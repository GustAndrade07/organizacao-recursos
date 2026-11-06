package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tela 6 do inventário (docs/frontend/telas.md): nova reserva, prefixo {@code reserva-form-},
 * RF-04, RF-05, RF-06. O formulário já vem preenchido com o recurso e o horário escolhidos na busca
 * e envia por POST para {@code /reservas}; quem grava a reserva é o back-end.
 */
@WebMvcTest(ReservaController.class)
class NovaReservaPaginaTest {

    private static final String ROTA = "/reservas/nova?recursoId=9&data=2026-11-12&inicio=14:00&termino=15:30";

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
        Document doc = pagina(ROTA);

        assertThat(doc.title()).isEqualTo("Nova reserva · Organização de Recursos");
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.selectFirst("main#conteudo h1[data-testid=reserva-form-titulo]").text()).isEqualTo("Nova reserva");
    }

    @Test
    void formularioEnviaPorPostParaReservasLevandoOIdDoRecurso() throws Exception {
        Document doc = pagina(ROTA);

        var form = doc.selectFirst("form[data-testid=reserva-form]");
        assertThat(form.attr("method")).isEqualToIgnoringCase("post");
        assertThat(form.attr("action")).isEqualTo("/reservas");
        var recurso = form.selectFirst("input[type=hidden][name=recursoId]");
        assertThat(recurso.val()).isEqualTo("9");
    }

    @Test
    void formularioVemPreenchidoComOQueFoiEscolhidoNaBusca() throws Exception {
        Document doc = pagina(ROTA);

        assertThat(doc.selectFirst("input[data-testid=reserva-form-data]").val()).isEqualTo("2026-11-12");
        assertThat(doc.selectFirst("input[data-testid=reserva-form-inicio]").val()).isEqualTo("14:00");
        assertThat(doc.selectFirst("input[data-testid=reserva-form-termino]").val()).isEqualTo("15:30");
    }

    @Test
    void camposTemRotuloVisivelTipoNomeEObrigatoriedade() throws Exception {
        Document doc = pagina(ROTA);

        var esperados = java.util.Map.of(
                "reserva-form-data", new String[] {"Data", "date", "data"},
                "reserva-form-inicio", new String[] {"Hora de início", "time", "inicio"},
                "reserva-form-termino", new String[] {"Hora de término", "time", "termino"});
        esperados.forEach((testid, esperado) -> {
            var campo = doc.selectFirst("input[data-testid=" + testid + "]");
            assertThat(campo.attr("type")).as(testid).isEqualTo(esperado[1]);
            assertThat(campo.attr("name")).as(testid).isEqualTo(esperado[2]);
            assertThat(campo.hasAttr("required")).as(testid).isTrue();
            assertThat(doc.selectFirst("label[for=" + campo.id() + "]").text()).as(testid).isEqualTo(esperado[0]);
        });
    }

    @Test
    void botaoConfirmarEnviaOFormularioEHaUmCaminhoDeVoltaParaABusca() throws Exception {
        Document doc = pagina(ROTA);

        var confirmar = doc.selectFirst("form[data-testid=reserva-form] button[data-testid=reserva-form-confirmar]");
        assertThat(confirmar.attr("type")).isEqualTo("submit");
        assertThat(confirmar.text()).isEqualTo("Confirmar reserva");
        var voltar = doc.selectFirst("a[data-testid=reserva-form-voltar]");
        assertThat(voltar.text()).isEqualTo("Voltar à busca");
        assertThat(voltar.attr("href")).startsWith("/recursos/disponiveis?")
                .contains("data=2026-11-12").contains("inicio=14:00").contains("termino=15:30").contains("editar");
    }

    @Test
    void scriptBloqueiaTerminoAteriorAoInicioEEvitaEnvioDuplicado() throws Exception {
        Document doc = pagina(ROTA);

        assertThat(doc.select("script[src]").eachAttr("src")).contains("/js/reserva-form.js");
        var form = doc.selectFirst("form[data-testid=reserva-form]");
        assertThat(form.hasAttr("novalidate")).isFalse();
        var resumo = doc.selectFirst("[data-testid=reserva-form-erro-resumo]");
        assertThat(resumo.attr("role")).isEqualTo("alert");
        assertThat(resumo.hasAttr("hidden")).isTrue();
        var erroTermino = doc.selectFirst("[data-testid=reserva-form-erro-termino]");
        assertThat(erroTermino.hasAttr("hidden")).isTrue();
        assertThat(erroTermino.text()).isEmpty();
    }

    @Test
    void semParametrosAPaginaAindaAbreComOFormularioVazio() throws Exception {
        Document doc = pagina("/reservas/nova");

        assertThat(doc.selectFirst("input[data-testid=reserva-form-data]").val()).isEmpty();
        assertThat(doc.selectFirst("input[type=hidden][name=recursoId]").val()).isEmpty();
    }

    @Test
    void ganchosQueOScriptUsaEstaoNoHtml() throws Exception {
        Document doc = pagina(ROTA);

        var resumo = doc.selectFirst("[data-testid=reserva-form-erro-resumo]");
        assertThat(resumo.attr("tabindex")).isEqualTo("-1");
        assertThat(resumo.select("[data-resumo-titulo]")).hasSize(1);
        assertThat(resumo.select("[data-resumo-lista]")).hasSize(1);
        assertThat(doc.selectFirst("[data-testid=reserva-form-confirmar]").attr("data-enviando")).isEqualTo("Enviando…");
        assertThat(doc.select("#reserva-form-erro-termino")).hasSize(1);
    }

    @Test
    void todoSeletorEIdCitadoPeloScriptExisteNaPaginaERenderizada() throws Exception {
        String js = new String(new ClassPathResource("static/js/reserva-form.js").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Document doc = pagina(ROTA);

        Matcher testids = Pattern.compile("\\[data-testid=\"([^\"]+)\"\\]").matcher(js);
        int achados = 0;
        while (testids.find()) {
            achados++;
            assertThat(doc.select("[data-testid=" + testids.group(1) + "]")).as(testids.group(1)).isNotEmpty();
        }
        Matcher ids = Pattern.compile("getElementById\\('([^']+)'\\)").matcher(js);
        while (ids.find()) {
            achados++;
            assertThat(doc.getElementById(ids.group(1))).as(ids.group(1)).isNotNull();
        }
        Matcher dados = Pattern.compile("\\[(data-resumo-[a-z]+)\\]").matcher(js);
        while (dados.find()) {
            achados++;
            assertThat(doc.select("[" + dados.group(1) + "]")).as(dados.group(1)).isNotEmpty();
        }
        assertThat(achados).isGreaterThanOrEqualTo(6);
        assertThat(js).contains("O término precisa ser depois do início");
    }

    @Test
    void dataOuHoraMalFormadaNaUrlViraErro400() throws Exception {
        mvc.perform(get("/reservas/nova?data=abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/reservas/nova?inicio=25:99")).andExpect(status().isBadRequest());
    }

    @Test
    void recursoIdComHtmlNaUrlEhEscapadoNoCampoOculto() throws Exception {
        String html = mvc.perform(get("/reservas/nova").param("recursoId", "\"><script>alert(1)</script>"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Document doc = Jsoup.parse(html);

        assertThat(doc.select("script:containsData(alert)")).isEmpty();
        assertThat(doc.selectFirst("input[type=hidden][name=recursoId]").val()).isEqualTo("\"><script>alert(1)</script>");
    }

    @Test
    void todoElementoDeTesteDaTelaUsaOPrefixoSemRepetir() throws Exception {
        Document doc = pagina(ROTA);

        var ids = doc.select("main [data-testid]").eachAttr("data-testid");
        assertThat(ids).isNotEmpty().allMatch(id -> id.equals("reserva-form") || id.startsWith("reserva-form-")).doesNotHaveDuplicates();
    }
}
