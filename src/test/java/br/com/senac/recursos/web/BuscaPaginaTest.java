package br.com.senac.recursos.web;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.support.ComoUsuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tela 4 do inventário (docs/frontend/telas.md): buscar disponibilidade, prefixo {@code busca-}, RF-03.
 * Critério de aceite: filtros de tipo de recurso, capacidade mínima, localização, data, hora de início e
 * hora de término. A lista de tipos e de locais vem do back-end (atributos {@code tipos} e {@code locais}).
 */
@ComoUsuario(PerfilUsuario.SOLICITANTE)
@WebMvcTest(BuscaController.class)
class BuscaPaginaTest {

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
        Document doc = pagina("/recursos/disponiveis");

        assertThat(doc.title()).isEqualTo("Buscar disponibilidade · Organização de Recursos");
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.selectFirst("main#conteudo h1[data-testid=busca-titulo]").text()).isEqualTo("Buscar disponibilidade");
    }

    @Test
    void formularioEnviaPorGetParaAPropriaRota() throws Exception {
        Document doc = pagina("/recursos/disponiveis");

        var form = doc.selectFirst("form[data-testid=busca-formulario]");
        assertThat(form.attr("method")).isEqualToIgnoringCase("get");
        assertThat(form.attr("action")).isEqualTo("/recursos/disponiveis");
        assertThat(form.selectFirst("button[type=submit][data-testid=busca-buscar]").text()).isEqualTo("Buscar recursos livres");
    }

    @Test
    void todoCampoTemRotuloVisivelLigadoPeloFor() throws Exception {
        Document doc = pagina("/recursos/disponiveis");

        var esperados = java.util.Map.of(
                "busca-tipo", "Tipo de recurso",
                "busca-capacidade", "Capacidade mínima",
                "busca-local", "Localização",
                "busca-data", "Data",
                "busca-inicio", "Hora de início",
                "busca-termino", "Hora de término");
        esperados.forEach((testid, rotulo) -> {
            var campo = doc.selectFirst("[data-testid=" + testid + "]");
            assertThat(campo).as(testid).isNotNull();
            assertThat(doc.selectFirst("label[for=" + campo.id() + "]").text()).as(testid).isEqualTo(rotulo);
        });
    }

    @Test
    void camposTemNomeETipoCertosParaOTecladoEOSeletorNativo() throws Exception {
        Document doc = pagina("/recursos/disponiveis");

        assertThat(doc.selectFirst("select[data-testid=busca-tipo]").attr("name")).isEqualTo("tipo");
        assertThat(doc.selectFirst("select[data-testid=busca-local]").attr("name")).isEqualTo("local");
        var capacidade = doc.selectFirst("input[data-testid=busca-capacidade]");
        assertThat(capacidade.attr("type")).isEqualTo("number");
        assertThat(capacidade.attr("name")).isEqualTo("capacidade");
        assertThat(capacidade.attr("min")).isEqualTo("1");
        assertThat(capacidade.attr("inputmode")).isEqualTo("numeric");
        assertThat(doc.selectFirst("input[data-testid=busca-data]").attr("type")).isEqualTo("date");
        assertThat(doc.selectFirst("input[data-testid=busca-data]").attr("name")).isEqualTo("data");
        assertThat(doc.selectFirst("input[data-testid=busca-inicio]").attr("type")).isEqualTo("time");
        assertThat(doc.selectFirst("input[data-testid=busca-inicio]").attr("name")).isEqualTo("inicio");
        assertThat(doc.selectFirst("input[data-testid=busca-termino]").attr("type")).isEqualTo("time");
        assertThat(doc.selectFirst("input[data-testid=busca-termino]").attr("name")).isEqualTo("termino");
    }

    @Test
    void dataEHorariosSaoObrigatoriosETipoELocalTemOpcaoPadrao() throws Exception {
        Document doc = pagina("/recursos/disponiveis");

        assertThat(doc.selectFirst("[data-testid=busca-data]").hasAttr("required")).isTrue();
        assertThat(doc.selectFirst("[data-testid=busca-inicio]").hasAttr("required")).isTrue();
        assertThat(doc.selectFirst("[data-testid=busca-termino]").hasAttr("required")).isTrue();
        assertThat(doc.selectFirst("select[data-testid=busca-tipo] option").text()).isEqualTo("Todos os tipos");
        assertThat(doc.selectFirst("select[data-testid=busca-tipo] option").val()).isEmpty();
        assertThat(doc.selectFirst("select[data-testid=busca-local] option").text()).isEqualTo("Qualquer local");
    }

    @Test
    void semBuscaAnteriorOsCamposVemVazios() throws Exception {
        Document doc = pagina("/recursos/disponiveis");

        assertThat(doc.selectFirst("[data-testid=busca-data]").val()).isEmpty();
        assertThat(doc.selectFirst("[data-testid=busca-inicio]").val()).isEmpty();
        assertThat(doc.selectFirst("[data-testid=busca-capacidade]").val()).isEmpty();
        assertThat(doc.select("[data-testid=resultado-titulo]")).isEmpty();
    }

    @Test
    void parametrosVoltamPreenchidosNoFormularioQuandoSeEditaABusca() throws Exception {
        Document doc = pagina("/recursos/disponiveis?capacidade=30&data=2026-11-12&inicio=14:00&termino=15:30&editar");

        assertThat(doc.selectFirst("[data-testid=busca-capacidade]").val()).isEqualTo("30");
        assertThat(doc.selectFirst("[data-testid=busca-data]").val()).isEqualTo("2026-11-12");
        assertThat(doc.selectFirst("[data-testid=busca-inicio]").val()).isEqualTo("14:00");
        assertThat(doc.selectFirst("[data-testid=busca-termino]").val()).isEqualTo("15:30");
    }

    @Test
    void campoVazioNoEnvioNaoQuebraAPagina() throws Exception {
        Document doc = pagina("/recursos/disponiveis?tipo=&capacidade=&local=&data=&inicio=&termino=");

        assertThat(doc.selectFirst("form[data-testid=busca-formulario]")).isNotNull();
    }
}
