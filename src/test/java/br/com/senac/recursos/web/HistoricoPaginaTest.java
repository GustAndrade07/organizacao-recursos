package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela 11 do inventário (docs/frontend/telas.md): histórico da reserva, prefixo {@code historico-}, RF-10,
 * RN-09, RN-10 e RN-11. O back-end preenche {@code reserva} (recurso, solicitante, data, horario, estado) e
 * {@code eventos} (instante, de, para, usuario, motivo), do mais antigo ao mais recente. A tela é somente
 * leitura. O 403 do Solicitante (RN-11) é a página de erro 403 que o back-end devolve; não há tela própria.
 */
@WebMvcTest({HistoricoController.class, HistoricoPaginaTest.PaginasDeTeste.class})
@Import(HistoricoPaginaTest.PaginasDeTeste.class)
class HistoricoPaginaTest {

    @Controller
    static class PaginasDeTeste {

        private static Map<String, Object> reserva(String recurso, String estado) {
            return Map.of("recurso", recurso, "solicitante", "Ana Lima", "data", LocalDate.of(2026, 11, 12),
                    "horario", "14:00 às 15:30", "estado", estado);
        }

        private static Map<String, Object> evento(LocalDateTime instante, String de, String para, String usuario, String motivo) {
            Map<String, Object> evento = new HashMap<>();
            evento.put("instante", instante);
            evento.put("de", de);
            evento.put("para", para);
            evento.put("usuario", usuario);
            evento.put("motivo", motivo);
            return evento;
        }

        @GetMapping("/teste/historico/concluida")
        String concluida(Model model) {
            model.addAttribute("reserva", reserva("Laboratório de Química", "Concluída"));
            model.addAttribute("eventos", List.of(
                    evento(LocalDateTime.of(2026, 11, 6, 9, 12), null, "Solicitada", "Ana Lima", null),
                    evento(LocalDateTime.of(2026, 11, 6, 11, 40), "Solicitada", "Aprovada", "Carlos Souza", null),
                    evento(LocalDateTime.of(2026, 11, 12, 14, 5), "Aprovada", "Em uso", "Carlos Souza", ""),
                    evento(LocalDateTime.of(2026, 11, 12, 15, 32), "Em uso", "Concluída", "Carlos Souza", "   ")));
            return "historico";
        }

        @GetMapping("/teste/historico/rejeitada")
        String rejeitada(Model model) {
            model.addAttribute("reserva", reserva("Laboratório de Química", "Rejeitada"));
            model.addAttribute("eventos", List.of(
                    evento(LocalDateTime.of(2026, 11, 5, 16, 20), null, "Solicitada", "Ana Lima", null),
                    evento(LocalDateTime.of(2026, 11, 5, 17, 2), "Solicitada", "Rejeitada", "Carlos Souza",
                            "Laboratório ocupado por outra turma neste horário.")));
            return "historico";
        }

        @GetMapping("/teste/historico/estado")
        String estado(@RequestParam String nome, Model model) {
            model.addAttribute("reserva", reserva("Sala 203", nome));
            model.addAttribute("eventos", List.of(
                    evento(LocalDateTime.of(2026, 11, 6, 9, 12), null, "Solicitada", "Ana Lima", null),
                    evento(LocalDateTime.of(2026, 11, 6, 11, 40), "Solicitada", nome, "Carlos Souza", null)));
            return "historico";
        }

        @GetMapping("/teste/historico/voltar")
        String comVoltar(Model model) {
            model.addAttribute("reserva", reserva("Sala 203", "Aprovada"));
            model.addAttribute("eventos", List.of(
                    evento(LocalDateTime.of(2026, 11, 6, 9, 12), null, "Solicitada", "Ana Lima", null)));
            model.addAttribute("voltar", "/emprestimos");
            return "historico";
        }

        @GetMapping("/teste/historico/perigoso")
        String perigoso(Model model) {
            model.addAttribute("reserva", Map.of("recurso", "<script>alert(1)</script>", "solicitante", "<b>Ana</b>",
                    "data", LocalDate.of(2026, 11, 12), "horario", "<i>14:00</i>", "estado", "Aprovada"));
            model.addAttribute("eventos", List.of(
                    evento(LocalDateTime.of(2026, 11, 6, 9, 12), null, "Solicitada", "<u>Ana</u>", "<img src=x onerror=alert(1)>")));
            return "historico";
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
    void telaUsaOLayoutBaseComTituloEUmUnicoH1() throws Exception {
        Document doc = pagina("/teste/historico/concluida");

        assertThat(doc.title()).isEqualTo("Histórico da reserva · Organização de Recursos");
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.selectFirst("main#conteudo h1[data-testid=historico-titulo]").text()).isEqualTo("Histórico da reserva");
    }

    @Test
    void rotaDoBackEndDevolveATelaSemReservaNemLinhaDoTempoAteOBackEndPreencher() throws Exception {
        Document doc = pagina("/reservas/5/historico");

        assertThat(doc.selectFirst("h1[data-testid=historico-titulo]").text()).isEqualTo("Histórico da reserva");
        assertThat(doc.select("[data-testid=historico-resumo], [data-testid=historico-linha-tempo]")).isEmpty();
    }

    @Test
    void voltarLevaAoInicioOuAoEnderecoQueOBackEndInformar() throws Exception {
        assertThat(pagina("/teste/historico/concluida").selectFirst("a[data-testid=historico-voltar]").attr("href")).isEqualTo("/");

        var voltar = pagina("/teste/historico/voltar").selectFirst("a[data-testid=historico-voltar]");
        assertThat(voltar.text()).isEqualTo("Voltar");
        assertThat(voltar.attr("href")).isEqualTo("/emprestimos");
    }

    @Test
    void resumoMostraRecursoEstadoAtualComONomeEscritoSolicitanteDataEHorario() throws Exception {
        Document doc = pagina("/teste/historico/concluida");

        var resumo = doc.selectFirst("section[data-testid=historico-resumo]");
        assertThat(resumo.selectFirst("h2.cartao__titulo").text()).isEqualTo("Laboratório de Química");
        assertThat(resumo.attr("aria-labelledby")).isEqualTo(resumo.selectFirst("h2").id());
        var estado = resumo.selectFirst("[data-testid=historico-estado-atual]");
        assertThat(estado.text()).isEqualTo("Concluída");
        assertThat(estado.hasClass("estado")).isTrue();
        assertThat(estado.hasClass("estado--concluida")).isTrue();
        assertThat(resumo.select("dl.lista-dados dt").eachText()).containsExactly("Solicitante", "Data", "Horário");
        assertThat(resumo.select("dl.lista-dados dd").eachText()).containsExactly("Ana Lima", "12/11/2026", "14:00 às 15:30");
    }

    @ParameterizedTest
    @CsvSource({
            "Solicitada, estado--solicitada",
            "Aprovada, estado--aprovada",
            "Em uso, estado--em-uso",
            "Concluída, estado--concluida",
            "Rejeitada, estado--rejeitada",
            "Cancelada, estado--cancelada",
            "Não compareceu, estado--nao-compareceu"})
    void cadaEstadoApareceEscritoNoBadgeComASuaClasse(String nome, String classe) throws Exception {
        Document doc = pagina("/teste/historico/estado?nome=" + nome);

        var atual = doc.selectFirst("[data-testid=historico-estado-atual]");
        assertThat(atual.text()).isEqualTo(nome);
        assertThat(atual.hasClass(classe)).isTrue();
        var novo = doc.selectFirst("[data-testid=historico-evento-2-para]");
        assertThat(novo.text()).isEqualTo(nome);
        assertThat(novo.hasClass(classe)).isTrue();
    }

    @Test
    void linhaDoTempoEhUmaListaOrdenadaNaOrdemEmQueOBackEndEnviou() throws Exception {
        Document doc = pagina("/teste/historico/concluida");

        var lista = doc.selectFirst("ol[data-testid=historico-linha-tempo]");
        assertThat(lista.hasClass("linha-tempo")).isTrue();
        assertThat(lista.select("> li.linha-tempo__item")).hasSize(4);
        assertThat(lista.select("> li").eachAttr("data-testid"))
                .containsExactly("historico-evento-1", "historico-evento-2", "historico-evento-3", "historico-evento-4");
        assertThat(lista.select("time").eachText()).containsExactly(
                "06/11/2026 às 09:12", "06/11/2026 às 11:40", "12/11/2026 às 14:05", "12/11/2026 às 15:32");
        var secao = doc.selectFirst("section[aria-labelledby=historico-linha-titulo]");
        assertThat(secao.selectFirst("h2#historico-linha-titulo").text()).isEqualTo("Mudanças de estado");
    }

    @Test
    void dataEHoraVaiNoElementoTimeComDatetimeLegivelPorMaquina() throws Exception {
        Document doc = pagina("/teste/historico/concluida");

        var data = doc.selectFirst("time[data-testid=historico-evento-1-data]");
        assertThat(data.attr("datetime")).isEqualTo("2026-11-06T09:12");
        assertThat(data.text()).isEqualTo("06/11/2026 às 09:12");
        assertThat(doc.selectFirst("time[data-testid=historico-evento-4-data]").attr("datetime")).isEqualTo("2026-11-12T15:32");
    }

    @Test
    void criacaoDaReservaNaoTemEstadoAnterior() throws Exception {
        Document doc = pagina("/teste/historico/concluida");

        assertThat(doc.selectFirst("[data-testid=historico-evento-1-de]").text()).isEqualTo("Sem estado anterior");
        assertThat(doc.select("[data-testid=historico-evento-1-de].estado")).isEmpty();
        var para = doc.selectFirst("[data-testid=historico-evento-1-para]");
        assertThat(para.text()).isEqualTo("Solicitada");
        assertThat(para.hasClass("estado--solicitada")).isTrue();
    }

    @Test
    void cadaMudancaMostraEstadoAnteriorENovoEscritosPorExtenso() throws Exception {
        Document doc = pagina("/teste/historico/concluida");

        var troca = doc.selectFirst("[data-testid=historico-evento-3] .linha-tempo__troca");
        assertThat(troca.text()).isEqualTo("de Aprovada para Em uso");
        assertThat(doc.selectFirst("[data-testid=historico-evento-3-de]").hasClass("estado--aprovada")).isTrue();
        assertThat(doc.selectFirst("[data-testid=historico-evento-3-para]").hasClass("estado--em-uso")).isTrue();
        assertThat(doc.selectFirst("[data-testid=historico-evento-4-de]").text()).isEqualTo("Em uso");
        assertThat(doc.selectFirst("[data-testid=historico-evento-4-para]").text()).isEqualTo("Concluída");
    }

    @Test
    void cadaMudancaMostraUsuarioEMotivoComRotulos() throws Exception {
        Document doc = pagina("/teste/historico/rejeitada");

        var evento = doc.selectFirst("[data-testid=historico-evento-2]");
        assertThat(evento.select("dl.lista-dados dt").eachText()).containsExactly("Usuário", "Motivo");
        assertThat(evento.selectFirst("[data-testid=historico-evento-2-usuario]").text()).isEqualTo("Carlos Souza");
        assertThat(evento.selectFirst("[data-testid=historico-evento-2-motivo]").text())
                .isEqualTo("Laboratório ocupado por outra turma neste horário.");
        assertThat(doc.selectFirst("[data-testid=historico-evento-1-usuario]").text()).isEqualTo("Ana Lima");
        assertThat(doc.selectFirst("[data-testid=historico-estado-atual]").text()).isEqualTo("Rejeitada");
    }

    @Test
    void semMotivoNemEspacoEmBrancoMostraNaoInformado() throws Exception {
        Document doc = pagina("/teste/historico/concluida");

        for (int n = 1; n <= 4; n++) {
            assertThat(doc.selectFirst("[data-testid=historico-evento-" + n + "-motivo]").text())
                    .as("motivo do evento %d", n).isEqualTo("Não informado");
        }
    }

    @Test
    void ehSomenteLeituraSemFormularioBotaoCampoOuLinkDeEdicao() throws Exception {
        Document doc = pagina("/teste/historico/concluida");

        assertThat(doc.select("main form, main button, main input, main textarea, main select")).isEmpty();
        assertThat(doc.select("main a").eachAttr("data-testid")).containsExactly("historico-voltar");
        assertThat(doc.selectFirst("p.nota-leitura[data-testid=historico-somente-leitura]").text())
                .isEqualTo("Os registros são somente leitura: não podem ser alterados nem apagados.");
    }

    @Test
    void textoComHtmlEhEscapado() throws Exception {
        Document doc = pagina("/teste/historico/perigoso");

        assertThat(doc.select("script:containsData(alert), img[onerror], main b, main i, main u")).isEmpty();
        assertThat(doc.selectFirst("section[data-testid=historico-resumo] h2").text()).isEqualTo("<script>alert(1)</script>");
        assertThat(doc.selectFirst("[data-testid=historico-evento-1-usuario]").text()).isEqualTo("<u>Ana</u>");
        assertThat(doc.selectFirst("[data-testid=historico-evento-1-motivo]").text()).isEqualTo("<img src=x onerror=alert(1)>");
    }
}
