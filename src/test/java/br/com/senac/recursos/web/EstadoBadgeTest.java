package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * RN-05: o modelo traz só o nome por extenso em {@code estado} (String) e UM fragmento converte em classe
 * ({@code fragments/estado :: badge(estado, testid)}). A classe sai de uma tabela fechada com os sete nomes;
 * nome desconhecido fica só com a classe {@code estado} (cor neutra); nulo ou em branco não renderiza nada.
 */
@WebMvcTest(EstadoBadgeTest.PaginasDeTeste.class)
@Import(EstadoBadgeTest.PaginasDeTeste.class)
class EstadoBadgeTest {

    @Controller
    static class PaginasDeTeste {

        @GetMapping("/teste/estado")
        String estado(@RequestParam(required = false) String estado,
                      @RequestParam(required = false) String testid, Model model) {
            model.addAttribute("estado", estado);
            model.addAttribute("testid", testid);
            return "teste/estado";
        }
    }

    @Autowired
    MockMvc mvc;

    private Document pagina(String estado, String testid) throws Exception {
        MockHttpServletRequestBuilder pedido = get("/teste/estado");
        if (estado != null) {
            pedido = pedido.param("estado", estado);
        }
        if (testid != null) {
            pedido = pedido.param("testid", testid);
        }
        String html = mvc.perform(pedido).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return Jsoup.parse(html);
    }

    private Element badge(String estado) throws Exception {
        Element badge = pagina(estado, "estado-x").selectFirst("#caixa .estado");
        assertThat(badge).as("badge para %s", estado).isNotNull();
        return badge;
    }

    @ParameterizedTest
    @CsvSource({
            "Solicitada,solicitada",
            "Aprovada,aprovada",
            "Em uso,em-uso",
            "Concluída,concluida",
            "Rejeitada,rejeitada",
            "Cancelada,cancelada",
            "Não compareceu,nao-compareceu"})
    void osSeteNomesViramAClasseDoEstado(String nome, String sufixo) throws Exception {
        Element badge = badge(nome);

        assertThat(badge.tagName()).isEqualTo("span");
        assertThat(badge.classNames()).containsExactlyInAnyOrder("estado", "estado--" + sufixo);
        assertThat(badge.text()).isEqualTo(nome);
    }

    @ParameterizedTest
    @CsvSource({
            "SOLICITADA,solicitada",
            "EM USO,em-uso",
            "CONCLUÍDA,concluida",
            "NÃO COMPARECEU,nao-compareceu",
            "em uso,em-uso",
            "não compareceu,nao-compareceu"})
    void maiusculasEMinusculasNaoMudamAClasseENemOTexto(String nome, String sufixo) throws Exception {
        Element badge = badge(nome);

        assertThat(badge.classNames()).containsExactlyInAnyOrder("estado", "estado--" + sufixo);
        assertThat(badge.text()).isEqualTo(nome);
    }

    @ParameterizedTest
    @CsvSource({
            "Concluida,concluida",
            "concluida,concluida",
            "Nao compareceu,nao-compareceu",
            "NAO COMPARECEU,nao-compareceu"})
    void grafiaSemAcentoTambemEAceita(String nome, String sufixo) throws Exception {
        Element badge = badge(nome);

        assertThat(badge.classNames()).containsExactlyInAnyOrder("estado", "estado--" + sufixo);
        assertThat(badge.text()).isEqualTo(nome);
    }

    @Test
    void espacosNasPontasEDuplosNaoQuebramAClasse() throws Exception {
        assertThat(badge("  Em uso  ").classNames()).containsExactlyInAnyOrder("estado", "estado--em-uso");
        assertThat(badge("Em  uso").classNames()).containsExactlyInAnyOrder("estado", "estado--em-uso");
        assertThat(badge(" Não   compareceu ").classNames()).containsExactlyInAnyOrder("estado", "estado--nao-compareceu");
        assertThat(badge("\tAprovada\t").classNames()).containsExactlyInAnyOrder("estado", "estado--aprovada");
    }

    @Test
    void textoDoBadgeEhExatamenteOQueVeioDoModelo() throws Exception {
        String html = mvc.perform(get("/teste/estado").param("estado", "  Em uso ").param("testid", "t"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(">  Em uso <");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Pendente", "Em análise", "Expirada", "uso", "Em uso agora", "solicitada2", "SOLICITADO", "em-uso", "estado--aprovada"})
    void estadoDesconhecidoFicaSoComAClasseEstadoMasContinuaVisivel(String nome) throws Exception {
        Element badge = badge(nome);

        assertThat(badge.classNames()).containsExactly("estado");
        assertThat(badge.attr("class")).isEqualTo("estado");
        assertThat(badge.text()).isEqualTo(nome);
    }

    @Test
    void asUnicasClassesPossiveisSaoEstadoOuUmDosSeteSufixos() throws Exception {
        for (String nome : new String[] {"Em uso", "  Em  uso ", "Pendente", "Não compareceu", "Concluída", "Concluida", "x y z"}) {
            assertThat(badge(nome).attr("class")).matches(
                    "estado( estado--(solicitada|aprovada|em-uso|concluida|rejeitada|cancelada|nao-compareceu))?");
        }
    }

    @Test
    void estadoNuloNaoRenderizaNada() throws Exception {
        Document doc = pagina(null, "estado-x");

        assertThat(doc.select("#caixa .estado")).isEmpty();
        assertThat(doc.select("[data-testid=estado-x]")).isEmpty();
        assertThat(doc.selectFirst("#caixa").children()).isEmpty();
        assertThat(doc.selectFirst("#caixa").text()).isBlank();
        assertThat(doc.html()).doesNotContain("estado--null").doesNotContain("estado--");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", " \t "})
    void estadoEmBrancoNaoRenderizaNada(String branco) throws Exception {
        Document doc = pagina(branco, "estado-x");

        assertThat(doc.select("#caixa .estado")).isEmpty();
        assertThat(doc.select("[data-testid=estado-x]")).isEmpty();
        assertThat(doc.selectFirst("#caixa").children()).isEmpty();
        assertThat(doc.selectFirst("#caixa").text()).isBlank();
    }

    @Test
    void htmlPerigosoEscapadoESemClasseDeEstado() throws Exception {
        String perigoso = "<script>alert(1)</script><b onclick=\"x\">";
        MockHttpServletRequestBuilder pedido = get("/teste/estado").param("estado", perigoso).param("testid", "estado-x");
        String html = mvc.perform(pedido).andReturn().getResponse().getContentAsString();
        Document doc = Jsoup.parse(html);

        assertThat(doc.select("script:containsData(alert(1))")).isEmpty();
        assertThat(doc.select("#caixa b")).isEmpty();
        assertThat(doc.selectFirst("#caixa .estado").text()).isEqualTo(perigoso);
        assertThat(doc.selectFirst("#caixa .estado").classNames()).containsExactly("estado");
        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void oTestidPassadoSaiNoAtributo() throws Exception {
        Element badge = pagina("Aprovada", "aprovacao-estado").selectFirst("#caixa .estado");

        assertThat(badge.attr("data-testid")).isEqualTo("aprovacao-estado");
    }

    @Test
    void semTestidOAtributoNaoApareceNemVazio() throws Exception {
        Element nulo = pagina("Aprovada", null).selectFirst("#caixa .estado");
        Element vazio = pagina("Aprovada", "").selectFirst("#caixa .estado");

        assertThat(nulo.hasAttr("data-testid")).isFalse();
        assertThat(vazio.hasAttr("data-testid")).isFalse();
    }

    @Test
    void testidComCaractereEspecialEhEscapado() throws Exception {
        String html = mvc.perform(get("/teste/estado").param("estado", "Aprovada").param("testid", "a\"><i>"))
                .andReturn().getResponse().getContentAsString();
        Document doc = Jsoup.parse(html);

        assertThat(doc.select("#caixa i")).isEmpty();
        assertThat(doc.selectFirst("#caixa .estado").attr("data-testid")).isEqualTo("a\"><i>");
    }
}
