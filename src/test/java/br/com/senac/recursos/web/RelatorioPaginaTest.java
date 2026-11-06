package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela 12 do inventário (docs/frontend/telas.md): relatório de utilização, prefixo {@code relatorio-},
 * RF-12 e RN-13. A tela valida o período (fim antes do início não gera relatório) e o back-end preenche
 * {@code relatorio} (recursos, professores, totalReservas, totalHoras e conflitos); sem ele a página mostra
 * só o filtro. Professor é quem pediu a reserva.
 */
@WebMvcTest({RelatorioController.class, RelatorioPaginaTest.PaginasDeTeste.class})
@Import(RelatorioPaginaTest.PaginasDeTeste.class)
class RelatorioPaginaTest {

    private static final String MENSAGEM_PERIODO = "A data de fim não pode ser anterior à data de início.";

    @Controller
    static class PaginasDeTeste {

        private static Map<String, Object> linha(String nome, int reservas, String horas) {
            return Map.of("nome", nome, "reservas", reservas, "horas", horas);
        }

        private static void filtro(Model model) {
            model.addAttribute("filtro", new FiltroRelatorio(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
        }

        @GetMapping("/teste/relatorio/completo")
        String completo(Model model) {
            filtro(model);
            model.addAttribute("relatorio", Map.of(
                    "recursos", List.of(
                            linha("Sala 203", 9, "18 h"),
                            linha("Laboratório de Química", 5, "12 h 30 min"),
                            linha("Projetor portátil P-07", 6, "9 h")),
                    "professores", List.of(
                            linha("Ana Lima", 8, "17 h"),
                            linha("Rafael Costa", 7, "13 h"),
                            linha("Laura Mendes", 5, "9 h 30 min")),
                    "totalReservas", 20,
                    "totalHoras", "39 h 30 min",
                    "conflitos", 7));
            return "relatorio";
        }

        @GetMapping("/teste/relatorio/uma-reserva")
        String umaReserva(Model model) {
            filtro(model);
            model.addAttribute("relatorio", Map.of(
                    "recursos", List.of(linha("Sala 203", 1, "2 h")),
                    "professores", List.of(linha("Ana Lima", 1, "2 h")),
                    "totalReservas", 1,
                    "totalHoras", "2 h",
                    "conflitos", 0));
            return "relatorio";
        }

        @GetMapping("/teste/relatorio/vazio")
        String vazio(Model model) {
            model.addAttribute("filtro", new FiltroRelatorio(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)));
            model.addAttribute("relatorio", Map.of(
                    "recursos", List.of(),
                    "professores", List.of(),
                    "totalReservas", 0,
                    "totalHoras", "0 h",
                    "conflitos", 0));
            return "relatorio";
        }

        @GetMapping("/teste/relatorio/com-erro")
        String comErro(Model model) {
            model.addAttribute("filtro", new FiltroRelatorio(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1)));
            model.addAttribute("erros", Map.of("fim", MENSAGEM_PERIODO));
            model.addAttribute("relatorio", Map.of(
                    "recursos", List.of(linha("Sala 203", 9, "18 h")),
                    "professores", List.of(linha("Ana Lima", 9, "18 h")),
                    "totalReservas", 9,
                    "totalHoras", "18 h",
                    "conflitos", 1));
            return "relatorio";
        }

        @GetMapping("/teste/relatorio/perigoso")
        String perigoso(@RequestParam(defaultValue = "x") String x, Model model) {
            filtro(model);
            model.addAttribute("relatorio", Map.of(
                    "recursos", List.of(linha("<script>alert(1)</script>", 1, "<b>2 h</b>")),
                    "professores", List.of(linha("<i>Ana</i>", 1, "2 h")),
                    "totalReservas", 1,
                    "totalHoras", "<u>2 h</u>",
                    "conflitos", 0));
            return "relatorio";
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

    private String texto(String caminhoNoClasspath) throws IOException {
        return new String(new ClassPathResource(caminhoNoClasspath).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    // Filtro e período (rota real)

    @Test
    void telaUsaOLayoutBaseComTituloEUmUnicoH1() throws Exception {
        Document doc = pagina("/relatorios/utilizacao");

        assertThat(doc.title()).isEqualTo("Relatório de utilização · Organização de Recursos");
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.selectFirst("main#conteudo h1[data-testid=relatorio-titulo]").text()).isEqualTo("Relatório de utilização");
    }

    @Test
    void filtroEnviaPorGetParaAPropriaRotaComBotaoGerarRelatorio() throws Exception {
        Document doc = pagina("/relatorios/utilizacao");

        var form = doc.selectFirst("form[data-testid=relatorio-filtros]");
        assertThat(form.attr("method")).isEqualToIgnoringCase("get");
        assertThat(form.attr("action")).isEqualTo("/relatorios/utilizacao");
        assertThat(form.selectFirst("button[type=submit][data-testid=relatorio-gerar]").text()).isEqualTo("Gerar relatório");
    }

    @Test
    void camposDeDataTemRotuloVisivelLigadoPeloForENomeCerto() throws Exception {
        Document doc = pagina("/relatorios/utilizacao");

        var esperados = Map.of("relatorio-inicio", "Data de início", "relatorio-fim", "Data de fim");
        esperados.forEach((testid, rotulo) -> {
            var campo = doc.selectFirst("input[data-testid=" + testid + "]");
            assertThat(campo.attr("type")).as(testid).isEqualTo("date");
            assertThat(doc.selectFirst("label[for=" + campo.id() + "]").text()).as(testid).isEqualTo(rotulo);
        });
        assertThat(doc.selectFirst("input[data-testid=relatorio-inicio]").attr("name")).isEqualTo("inicio");
        assertThat(doc.selectFirst("input[data-testid=relatorio-fim]").attr("name")).isEqualTo("fim");
    }

    @Test
    void camposVoltamPreenchidosComOPeriodoEscolhido() throws Exception {
        Document doc = pagina("/relatorios/utilizacao?inicio=2026-10-01&fim=2026-10-31");

        assertThat(doc.selectFirst("input[data-testid=relatorio-inicio]").val()).isEqualTo("2026-10-01");
        assertThat(doc.selectFirst("input[data-testid=relatorio-fim]").val()).isEqualTo("2026-10-31");
    }

    @Test
    void semParametrosOsCamposVemVaziosEAPaginaMostraSoOFiltro() throws Exception {
        Document doc = pagina("/relatorios/utilizacao");

        assertThat(doc.selectFirst("input[data-testid=relatorio-inicio]").val()).isEmpty();
        assertThat(doc.selectFirst("input[data-testid=relatorio-fim]").val()).isEmpty();
        assertThat(doc.select("section.bloco, [data-testid=relatorio-vazio], table")).isEmpty();
        assertThat(doc.select("[data-testid=relatorio-erro-periodo]:not([hidden])")).isEmpty();
    }

    @Test
    void notaFixaAvisaQueReservaCanceladaNaoContaComoUso() throws Exception {
        for (String caminho : List.of("/relatorios/utilizacao", "/teste/relatorio/completo", "/teste/relatorio/vazio")) {
            var nota = pagina(caminho).selectFirst("[data-testid=relatorio-nota-canceladas]");
            assertThat(nota.hasClass("alerta--info")).as(caminho).isTrue();
            assertThat(nota.attr("role")).as(caminho).isEqualTo("note");
            assertThat(nota.selectFirst(".alerta__titulo").text()).as(caminho).isEqualTo("Reservas canceladas não contam como uso");
        }
    }

    @Test
    void fimAnteriorAoInicioNaoGeraRelatorioEMostraOErroJuntoAoCampo() throws Exception {
        Document doc = pagina("/relatorios/utilizacao?inicio=2026-10-31&fim=2026-10-01");

        var erro = doc.selectFirst("#relatorio-erro-periodo");
        assertThat(erro.attr("data-testid")).isEqualTo("relatorio-erro-periodo");
        assertThat(erro.attr("role")).isEqualTo("alert");
        assertThat(erro.hasAttr("hidden")).isFalse();
        assertThat(erro.text()).isEqualTo(MENSAGEM_PERIODO);
        var fim = doc.selectFirst("input[data-testid=relatorio-fim]");
        assertThat(fim.attr("aria-invalid")).isEqualTo("true");
        assertThat(fim.attr("aria-describedby")).isEqualTo("relatorio-erro-periodo");
        assertThat(fim.hasClass("campo__entrada--erro")).isTrue();
        assertThat(doc.selectFirst("input[data-testid=relatorio-inicio]").hasAttr("aria-invalid")).isFalse();
        assertThat(doc.select("section.bloco, [data-testid=relatorio-vazio], table")).isEmpty();
        assertThat(fim.val()).isEqualTo("2026-10-01");
    }

    @Test
    void mesmoDiaNoInicioENoFimEhPeriodoValido() throws Exception {
        Document doc = pagina("/relatorios/utilizacao?inicio=2026-10-05&fim=2026-10-05");

        assertThat(doc.select("[data-testid=relatorio-erro-periodo]:not([hidden])")).isEmpty();
        assertThat(doc.selectFirst("input[data-testid=relatorio-fim]").hasAttr("aria-invalid")).isFalse();
    }

    @Test
    void erroDoServidorImpedeOsBlocosMesmoQueOBackEndTenhaPreenchidoORelatorio() throws Exception {
        Document doc = pagina("/teste/relatorio/com-erro");

        assertThat(doc.selectFirst("[data-testid=relatorio-erro-periodo]").text()).isEqualTo(MENSAGEM_PERIODO);
        assertThat(doc.select("section.bloco, [data-testid=relatorio-vazio], table")).isEmpty();
    }

    @Test
    void paginaTrazOScriptQueBloqueiaOPeriodoInvalidoAntesDoEnvio() throws Exception {
        Document doc = pagina("/relatorios/utilizacao");

        assertThat(doc.select("script[src]").eachAttr("src")).contains("/js/relatorio-form.js");
        assertThat(texto("static/js/relatorio-form.js")).contains(MENSAGEM_PERIODO);
    }

    // Relatório (atributo preenchido pelo back-end)

    @Test
    void blocoDeHorasPorRecursoTemTabelaComTotalDeReservasEHoras() throws Exception {
        Document doc = pagina("/teste/relatorio/completo");

        var bloco = doc.selectFirst("section[aria-labelledby=relatorio-recursos-titulo]");
        assertThat(bloco.selectFirst("h2#relatorio-recursos-titulo").text()).isEqualTo("Horas reservadas por recurso");
        assertThat(bloco.selectFirst("[data-testid=relatorio-recursos-total]").text()).isEqualTo("Total: 20 reservas, 39 h 30 min");
        var tabela = bloco.selectFirst("table[data-testid=relatorio-recursos]");
        assertThat(tabela.selectFirst("caption").text()).isEqualTo("Horas reservadas por recurso");
        assertThat(tabela.select("thead th[scope=col]").eachText()).containsExactly("Recurso", "Reservas", "Horas");
        assertThat(tabela.select("tbody tr")).hasSize(3);
        var linha = tabela.selectFirst("tr[data-testid=relatorio-recursos-linha-2]");
        assertThat(linha.selectFirst("th[scope=row]").text()).isEqualTo("Laboratório de Química");
        assertThat(linha.select("td").eachText()).containsExactly("5", "12 h 30 min");
    }

    @Test
    void blocoDeCargaPorProfessorUsaOSolicitanteComTotal() throws Exception {
        Document doc = pagina("/teste/relatorio/completo");

        var bloco = doc.selectFirst("section[aria-labelledby=relatorio-professores-titulo]");
        assertThat(bloco.selectFirst("h2#relatorio-professores-titulo").text()).isEqualTo("Carga horária por professor");
        assertThat(bloco.selectFirst("[data-testid=relatorio-professores-total]").text()).isEqualTo("Total: 20 reservas, 39 h 30 min");
        var tabela = bloco.selectFirst("table[data-testid=relatorio-professores]");
        assertThat(tabela.select("thead th[scope=col]").eachText()).containsExactly("Professor", "Reservas", "Horas");
        assertThat(tabela.select("tbody th[scope=row]").eachText()).containsExactly("Ana Lima", "Rafael Costa", "Laura Mendes");
        assertThat(tabela.selectFirst("tr[data-testid=relatorio-professores-linha-3]").select("td").eachText())
                .containsExactly("5", "9 h 30 min");
    }

    @Test
    void conflitosEvitadosMostramUmNumeroComORotulo() throws Exception {
        Document doc = pagina("/teste/relatorio/completo");

        var bloco = doc.selectFirst("section[aria-labelledby=relatorio-conflitos-titulo]");
        assertThat(bloco.selectFirst("h2#relatorio-conflitos-titulo").text()).isEqualTo("Conflitos evitados no período");
        var metrica = bloco.selectFirst("[data-testid=relatorio-conflitos]");
        assertThat(metrica.selectFirst(".metrica__valor").text()).isEqualTo("7");
        assertThat(metrica.selectFirst(".metrica__rotulo").text())
                .isEqualTo("tentativas de reserva recusadas por sobreposição de horário");
    }

    @Test
    void tabelasTemCelulasRotuladasEColunasNumericasParaOsCartoesDoMobile() throws Exception {
        Document doc = pagina("/teste/relatorio/completo");

        for (String tabela : List.of("relatorio-recursos", "relatorio-professores")) {
            var linha = doc.selectFirst("table[data-testid=" + tabela + "] tbody tr");
            assertThat(linha.select("td[data-rotulo]").eachAttr("data-rotulo")).as(tabela).containsExactly("Reservas", "Horas");
            assertThat(linha.select("td.tabela__num")).as(tabela).hasSize(2);
            assertThat(linha.selectFirst("th").hasClass("tabela__nome")).as(tabela).isTrue();
            assertThat(doc.select("table[data-testid=" + tabela + "] thead th.tabela__num").eachText())
                    .as(tabela).containsExactly("Reservas", "Horas");
        }
    }

    @Test
    void totalNoSingularQuandoHaUmaSoReserva() throws Exception {
        Document doc = pagina("/teste/relatorio/uma-reserva");

        assertThat(doc.selectFirst("[data-testid=relatorio-recursos-total]").text()).isEqualTo("Total: 1 reserva, 2 h");
        assertThat(doc.selectFirst("[data-testid=relatorio-conflitos] .metrica__valor").text()).isEqualTo("0");
    }

    @Test
    void periodoSemReservasMostraSoNenhumaReservaNoPeriodoSemTabelasVazias() throws Exception {
        Document doc = pagina("/teste/relatorio/vazio");

        var vazio = doc.selectFirst("section[data-testid=relatorio-vazio]");
        assertThat(vazio.selectFirst("h2").text()).isEqualTo("Nenhuma reserva no período");
        assertThat(doc.select("table, section.bloco, [data-testid=relatorio-conflitos]")).isEmpty();
        assertThat(doc.selectFirst("input[data-testid=relatorio-inicio]").val()).isEqualTo("2026-07-01");
    }

    @Test
    void textoComHtmlEhEscapado() throws Exception {
        Document doc = pagina("/teste/relatorio/perigoso");

        assertThat(doc.select("script:containsData(alert), main b, main i, main u")).isEmpty();
        assertThat(doc.selectFirst("tr[data-testid=relatorio-recursos-linha-1] th").text()).isEqualTo("<script>alert(1)</script>");
        assertThat(doc.selectFirst("tr[data-testid=relatorio-recursos-linha-1] td[data-rotulo=Horas]").text()).isEqualTo("<b>2 h</b>");
        assertThat(doc.selectFirst("[data-testid=relatorio-professores-total]").text()).isEqualTo("Total: 1 reserva, <u>2 h</u>");
    }
}
