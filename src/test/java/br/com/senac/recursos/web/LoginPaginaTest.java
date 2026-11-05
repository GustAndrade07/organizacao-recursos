package br.com.senac.recursos.web;

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
 * Tela 1 do inventário (docs/frontend/telas.md): login, prefixo {@code login-}, RF-01/RF-13.
 * Só a tela: o formulário envia {@code email} e {@code senha} por POST para {@code /login}; quem autentica é o
 * back-end (ADR-0001) e, se falhar, volta para {@code /login?erro}.
 */
@WebMvcTest(LoginController.class)
class LoginPaginaTest {

    @Autowired
    MockMvc mvc;

    private Document pagina(String caminho) throws Exception {
        String html = mvc.perform(get(caminho))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Jsoup.parse(html);
    }

    @Test
    void telaUsaOLayoutBaseComTituloEDescricao() throws Exception {
        Document doc = pagina("/login");

        assertThat(doc.title()).isEqualTo("Entrar · Organização de Recursos");
        assertThat(doc.select("link[rel=stylesheet]").eachAttr("href")).containsExactly("/css/tokens.css", "/css/app.css");
        assertThat(doc.selectFirst("main#conteudo h1[data-testid=login-titulo]").text()).isEqualTo("Entrar");
        assertThat(doc.selectFirst("[data-testid=login-descricao]").text())
                .isEqualTo("Use seu e-mail e senha para acessar o sistema de reservas.");
        assertThat(doc.select("h1")).hasSize(1);
    }

    @Test
    void visitanteNaoVeMenuNemSair() throws Exception {
        Document doc = pagina("/login");

        assertThat(doc.select("[data-testid=menu-abrir], [data-testid=menu-sair], [data-testid^=menu-item-]")).isEmpty();
        assertThat(doc.selectFirst("[data-testid=menu-marca]")).isNotNull();
    }

    @Test
    void formularioEnviaPorPostParaLogin() throws Exception {
        Document doc = pagina("/login");

        var form = doc.selectFirst("form[data-testid=login-formulario]");
        assertThat(form.attr("method")).isEqualToIgnoringCase("post");
        assertThat(form.attr("action")).isEqualTo("/login");
    }

    @Test
    void campoDeEmailTemRotuloVisivelTipoEAutocompletarParaGerenciadorDeSenhas() throws Exception {
        Document doc = pagina("/login");

        var campo = doc.selectFirst("input[data-testid=login-email]");
        assertThat(campo.attr("type")).isEqualTo("email");
        assertThat(campo.attr("name")).isEqualTo("email");
        assertThat(campo.attr("autocomplete")).isEqualTo("username");
        assertThat(campo.hasAttr("required")).isTrue();
        assertThat(doc.selectFirst("label[for=" + campo.id() + "]").text()).isEqualTo("E-mail");
    }

    @Test
    void campoDeSenhaTemRotuloVisivelEPermiteColarESalvarSenha() throws Exception {
        Document doc = pagina("/login");

        var campo = doc.selectFirst("input[data-testid=login-senha]");
        assertThat(campo.attr("type")).isEqualTo("password");
        assertThat(campo.attr("name")).isEqualTo("senha");
        assertThat(campo.attr("autocomplete")).isEqualTo("current-password");
        assertThat(campo.hasAttr("required")).isTrue();
        assertThat(campo.hasAttr("onpaste")).isFalse();
        assertThat(doc.selectFirst("label[for=" + campo.id() + "]").text()).isEqualTo("Senha");
    }

    @Test
    void botaoEntrarEnviaOFormulario() throws Exception {
        Document doc = pagina("/login");

        var botao = doc.selectFirst("form[data-testid=login-formulario] button[data-testid=login-entrar]");
        assertThat(botao.attr("type")).isEqualTo("submit");
        assertThat(botao.text()).isEqualTo("Entrar");
    }

    @Test
    void semErroNaoMostraOAviso() throws Exception {
        Document doc = pagina("/login");

        assertThat(doc.select("[data-testid=login-erro]")).isEmpty();
    }

    @Test
    void aposFalhaMostraAvisoAnunciadoSemDizerQualCampoEstaErrado() throws Exception {
        Document doc = pagina("/login?erro");

        var aviso = doc.selectFirst("[data-testid=login-erro]");
        assertThat(aviso.attr("role")).isEqualTo("alert");
        assertThat(aviso.text()).isEqualTo("E-mail ou senha incorretos. Confira os dados e tente novamente.");
        assertThat(doc.selectFirst("input[data-testid=login-email]")).isNotNull();
    }

    @Test
    void scriptDaTelaMoveOFocoParaOAvisoDeErro() throws Exception {
        Document doc = pagina("/login?erro");

        assertThat(doc.select("script[src]").eachAttr("src")).contains("/js/login.js");
        assertThat(doc.selectFirst("[data-testid=login-erro]").attr("tabindex")).isEqualTo("-1");
    }
}
