package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Estados da tela 6 (nova reserva): o back-end preenche {@code recurso} (nome, local, restrito) e, depois
 * de um POST recusado, {@code erros} (campo para mensagem), {@code conflito} (409 de horário) ou
 * {@code manutencao} (409 com inicio e fim). A tela só mostra o que recebe.
 */
@WebMvcTest(NovaReservaEstadosTest.PaginasDeTeste.class)
@Import(NovaReservaEstadosTest.PaginasDeTeste.class)
class NovaReservaEstadosTest {

    @Controller
    static class PaginasDeTeste {

        private static void base(Model model, boolean restrito, String nome) {
            model.addAttribute("reserva", new FormularioReserva("9", LocalDate.of(2026, 11, 12),
                    LocalTime.of(14, 0), LocalTime.of(15, 30)));
            model.addAttribute("recurso", Map.of("nome", nome, "local", "Bloco C, térreo", "restrito", restrito));
        }

        @GetMapping("/teste/reserva/livre")
        String livre(Model model) {
            base(model, false, "Sala 203");
            return "reserva-nova";
        }

        @GetMapping("/teste/reserva/restrito")
        String restrito(Model model) {
            base(model, true, "Laboratório de Química");
            return "reserva-nova";
        }

        @GetMapping("/teste/reserva/termino")
        String termino(Model model) {
            base(model, false, "Sala 203");
            model.addAttribute("erros", Map.of("termino", "O término precisa ser depois do início"));
            return "reserva-nova";
        }

        @GetMapping("/teste/reserva/dois-erros")
        String doisErros(Model model) {
            base(model, false, "Sala 203");
            Map<String, String> erros = new LinkedHashMap<>();
            erros.put("data", "Informe a data");
            erros.put("termino", "O término precisa ser depois do início");
            model.addAttribute("erros", erros);
            return "reserva-nova";
        }

        @GetMapping("/teste/reserva/conflito")
        String conflito(Model model) {
            base(model, false, "Sala 203");
            model.addAttribute("conflito", true);
            return "reserva-nova";
        }

        @GetMapping("/teste/reserva/manutencao")
        String manutencao(Model model) {
            base(model, false, "Laboratório de Química");
            model.addAttribute("manutencao", Map.of(
                    "inicio", LocalDateTime.of(2026, 11, 12, 8, 0),
                    "fim", LocalDateTime.of(2026, 11, 14, 18, 0)));
            return "reserva-nova";
        }

        @GetMapping("/teste/reserva/perigoso")
        String perigoso(Model model) {
            base(model, false, "<script>alert(1)</script>");
            return "reserva-nova";
        }
    }

    @Autowired
    MockMvc mvc;

    private Document pagina(String caminho) throws Exception {
        String html = mvc.perform(get(caminho))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Jsoup.parse(html);
    }

    @Test
    void mostraORecursoEscolhidoComLocalEUmCaminhoParaTroca() throws Exception {
        Document doc = pagina("/teste/reserva/livre");

        assertThat(doc.selectFirst("[data-testid=reserva-form-recurso]").text()).isEqualTo("Sala 203");
        assertThat(doc.selectFirst("[data-testid=reserva-form-local]").text()).isEqualTo("Bloco C, térreo");
        var trocar = doc.selectFirst("a[data-testid=reserva-form-trocar]");
        assertThat(trocar.text()).isEqualTo("Trocar recurso");
        assertThat(trocar.attr("href")).startsWith("/recursos/disponiveis?");
    }

    @Test
    void recursoLivreNaoMostraAvisoDeRestricao() throws Exception {
        assertThat(pagina("/teste/reserva/livre").select("[data-testid=reserva-form-aviso-restrito]")).isEmpty();
    }

    @Test
    void recursoRestritoAvisaAntesDeConfirmarQueFicaSolicitadaAteAprovacao() throws Exception {
        Document doc = pagina("/teste/reserva/restrito");

        var aviso = doc.selectFirst("[data-testid=reserva-form-aviso-restrito]");
        assertThat(aviso.text()).isEqualTo("Recurso restrito Sua reserva ficará Solicitada até um Responsável aprovar.");
        assertThat(aviso.selectFirst(".estado--solicitada").text()).isEqualTo("Solicitada");
    }

    @Test
    void erroDeTerminoApareceJuntoAoCampoEEmResumoNoTopo() throws Exception {
        Document doc = pagina("/teste/reserva/termino");

        var resumo = doc.selectFirst("[data-testid=reserva-form-erro-resumo]");
        assertThat(resumo.hasAttr("hidden")).isFalse();
        assertThat(resumo.attr("role")).isEqualTo("alert");
        assertThat(resumo.selectFirst(".alerta__titulo").text()).isEqualTo("Corrija 1 campo para continuar");
        var link = resumo.selectFirst("li a");
        assertThat(link.attr("href")).isEqualTo("#reserva-form-termino");
        assertThat(link.text()).isEqualTo("Hora de término: O término precisa ser depois do início");

        var campo = doc.selectFirst("input[data-testid=reserva-form-termino]");
        assertThat(campo.attr("aria-invalid")).isEqualTo("true");
        assertThat(campo.attr("aria-describedby")).isEqualTo("reserva-form-erro-termino");
        assertThat(campo.hasClass("campo__entrada--erro")).isTrue();
        var mensagem = doc.selectFirst("#reserva-form-erro-termino");
        assertThat(mensagem.hasAttr("hidden")).isFalse();
        assertThat(mensagem.text()).isEqualTo("O término precisa ser depois do início");
    }

    @Test
    void resumoContaOsCamposComErroNoPlural() throws Exception {
        Document doc = pagina("/teste/reserva/dois-erros");

        assertThat(doc.selectFirst("[data-testid=reserva-form-erro-resumo] .alerta__titulo").text())
                .isEqualTo("Corrija 2 campos para continuar");
        assertThat(doc.select("[data-testid=reserva-form-erro-resumo] li")).hasSize(2);
        assertThat(doc.selectFirst("input[data-testid=reserva-form-data]").attr("aria-invalid")).isEqualTo("true");
        assertThat(doc.selectFirst("input[data-testid=reserva-form-inicio]").hasAttr("aria-invalid")).isFalse();
    }

    @Test
    void conflitoDeHorarioExplicaOMotivoEOfereceVoltarAABusca() throws Exception {
        Document doc = pagina("/teste/reserva/conflito");

        var alerta = doc.selectFirst("[data-testid=reserva-form-erro-conflito]");
        assertThat(alerta.attr("role")).isEqualTo("alert");
        assertThat(alerta.selectFirst(".alerta__titulo").text()).isEqualTo("Não foi possível reservar");
        assertThat(alerta.selectFirst(".alerta__texto").text())
                .isEqualTo("Esse horário acabou de ser reservado ou se sobrepõe a outra reserva.");
        var voltar = alerta.selectFirst("a[data-testid=reserva-form-erro-voltar]");
        assertThat(voltar.text()).isEqualTo("Voltar à busca");
        assertThat(voltar.attr("href")).startsWith("/recursos/disponiveis?").contains("editar");
    }

    @Test
    void manutencaoMostraOPeriodoDoBloqueio() throws Exception {
        Document doc = pagina("/teste/reserva/manutencao");

        var alerta = doc.selectFirst("[data-testid=reserva-form-erro-manutencao]");
        assertThat(alerta.attr("role")).isEqualTo("alert");
        assertThat(alerta.selectFirst(".alerta__titulo").text()).isEqualTo("Recurso em manutenção");
        assertThat(alerta.selectFirst(".alerta__texto").text())
                .isEqualTo("O Laboratório de Química está em manutenção de 12/11/2026, 08:00 até 14/11/2026, 18:00."
                        + " Escolha outro período ou outro recurso.");
        assertThat(alerta.selectFirst("a[data-testid=reserva-form-erro-voltar]")).isNotNull();
        assertThat(doc.select("[data-testid=reserva-form-erro-conflito]")).isEmpty();
    }

    @Test
    void semErroNenhumAvisoDeFalhaApareceNoTopo() throws Exception {
        Document doc = pagina("/teste/reserva/livre");

        assertThat(doc.select("[data-testid=reserva-form-erro-conflito], [data-testid=reserva-form-erro-manutencao]")).isEmpty();
    }

    @Test
    void nomeDeRecursoComHtmlEhEscapado() throws Exception {
        Document doc = pagina("/teste/reserva/perigoso");

        assertThat(doc.select("script:containsData(alert)")).isEmpty();
        assertThat(doc.selectFirst("[data-testid=reserva-form-recurso]").text()).isEqualTo("<script>alert(1)</script>");
    }
}
