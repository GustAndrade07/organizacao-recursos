package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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
 * Tela 2 do inventário (docs/frontend/telas.md): fragmento {@code layout.html} e menu (prefixo {@code menu-}).
 * O menu é montado a partir dos atributos de modelo {@code usuario} (nome, perfil) e {@code menu}
 * (id, rotulo, href, ativo); quem preenche esses atributos é o back-end (RF-01).
 */
@WebMvcTest(LayoutBaseTest.PaginasDeTeste.class)
@Import(LayoutBaseTest.PaginasDeTeste.class)
class LayoutBaseTest {

    @Controller
    static class PaginasDeTeste {

        @GetMapping("/teste/anonimo")
        String anonimo() {
            return "teste/pagina";
        }

        @GetMapping("/teste/solicitante")
        String solicitante(Model model) {
            model.addAttribute("usuario", Map.of("nome", "Ana Souza", "perfil", "Solicitante"));
            model.addAttribute("menu", List.of(
                    Map.of("id", "buscar", "rotulo", "Buscar recursos", "href", "/recursos/disponiveis", "ativo", true),
                    Map.of("id", "minhas-reservas", "rotulo", "Minhas reservas", "href", "/reservas/minhas", "ativo", false)));
            return "teste/pagina";
        }

        @GetMapping("/teste/responsavel")
        String responsavel(Model model) {
            model.addAttribute("usuario", Map.of("nome", "Carlos Lima", "perfil", "Responsável"));
            model.addAttribute("menu", List.of(
                    Map.of("id", "aprovacoes", "rotulo", "Aprovações", "href", "/aprovacoes", "ativo", false),
                    Map.of("id", "retirada-devolucao", "rotulo", "Retirada e devolução", "href", "/retirada-devolucao", "ativo", false),
                    Map.of("id", "relatorios", "rotulo", "Relatórios", "href", "/relatorios/utilizacao", "ativo", false)));
            return "teste/pagina";
        }

        @GetMapping("/teste/nome-perigoso")
        String nomePerigoso(Model model) {
            model.addAttribute("usuario", Map.of("nome", "<script>alert(1)</script>", "perfil", "Solicitante"));
            model.addAttribute("menu", List.of());
            return "teste/pagina";
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
    void cabecalhoDaPaginaImportaTokensEDefineIdiomaEViewport() throws Exception {
        Document doc = pagina("/teste/anonimo");

        assertThat(doc.selectFirst("html").attr("lang")).isEqualTo("pt-BR");
        assertThat(doc.selectFirst("meta[name=viewport]").attr("content")).contains("width=device-width");
        assertThat(doc.select("link[rel=stylesheet]").eachAttr("href")).containsExactly("/css/tokens.css", "/css/app.css");
        assertThat(doc.title()).isEqualTo("Página de teste · Organização de Recursos");
    }

    @Test
    void conteudoDaTelaEntraNoMainComLinkParaPularAoConteudo() throws Exception {
        Document doc = pagina("/teste/anonimo");

        assertThat(doc.selectFirst("main#conteudo [data-testid=teste-titulo]").text()).isEqualTo("Conteúdo de teste");
        assertThat(doc.selectFirst("[data-testid=menu-pular]").attr("href")).isEqualTo("#conteudo");
    }

    @Test
    void semUsuarioMostraSoAMarcaSemMenuNemSair() throws Exception {
        Document doc = pagina("/teste/anonimo");

        assertThat(doc.selectFirst("[data-testid=menu-marca]").text()).isEqualTo("Organização de Recursos");
        assertThat(doc.select("[data-testid=menu-abrir]")).isEmpty();
        assertThat(doc.select("[data-testid=menu-sair]")).isEmpty();
        assertThat(doc.select("[data-testid^=menu-item-]")).isEmpty();
    }

    @Test
    void solicitanteVeSeusItensComOAtivoMarcadoParaLeitoresDeTela() throws Exception {
        Document doc = pagina("/teste/solicitante");

        assertThat(doc.select("[data-testid^=menu-item-]").eachText()).containsExactly("Buscar recursos", "Minhas reservas");
        assertThat(doc.selectFirst("[data-testid=menu-item-buscar]").attr("href")).isEqualTo("/recursos/disponiveis");
        assertThat(doc.selectFirst("[data-testid=menu-item-buscar]").attr("aria-current")).isEqualTo("page");
        assertThat(doc.selectFirst("[data-testid=menu-item-minhas-reservas]").hasAttr("aria-current")).isFalse();
        assertThat(doc.selectFirst("[data-testid=menu-usuario]").text()).isEqualTo("Ana Souza");
        assertThat(doc.selectFirst("[data-testid=menu-perfil]").text()).isEqualTo("Solicitante");
    }

    @Test
    void responsavelNaoVeItensDeSolicitante() throws Exception {
        Document doc = pagina("/teste/responsavel");

        assertThat(doc.select("[data-testid^=menu-item-]").eachText()).containsExactly("Aprovações", "Retirada e devolução", "Relatórios");
        assertThat(doc.select("[data-testid=menu-item-buscar]")).isEmpty();
    }

    @Test
    void botaoDoMenuTemNomeAcessivelEControlaOPainel() throws Exception {
        Document doc = pagina("/teste/solicitante");

        var botao = doc.selectFirst("button[data-testid=menu-abrir]");
        assertThat(botao.attr("aria-label")).isEqualTo("Abrir menu");
        assertThat(botao.attr("aria-expanded")).isEqualTo("false");
        assertThat(botao.attr("aria-controls")).isEqualTo("menu-painel");
        assertThat(doc.select("#menu-painel [data-testid=menu-sair]")).hasSize(1);
    }

    @Test
    void sairEUmFormularioPostParaLogout() throws Exception {
        Document doc = pagina("/teste/solicitante");

        var form = doc.selectFirst("form:has([data-testid=menu-sair])");
        assertThat(form.attr("method")).isEqualToIgnoringCase("post");
        assertThat(form.attr("action")).isEqualTo("/logout");
        assertThat(doc.selectFirst("button[data-testid=menu-sair]").text()).isEqualTo("Sair");
    }

    @Test
    void nomeDoUsuarioEhEscapadoNoHtml() throws Exception {
        Document doc = pagina("/teste/nome-perigoso");

        assertThat(doc.select("script:containsData(alert(1))")).isEmpty();
        assertThat(doc.selectFirst("[data-testid=menu-usuario]").text()).isEqualTo("<script>alert(1)</script>");
    }
}
