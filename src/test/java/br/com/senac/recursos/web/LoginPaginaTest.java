package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tela 1 do inventário (docs/frontend/telas.md): login, prefixo {@code login-}, RF-01/RF-13.
 * Só a tela: o formulário envia {@code email} e {@code senha} por POST para {@code /login}; quem autentica é o
 * back-end (ADR-0001) e, se falhar, volta para {@code /login?erro} com o atributo de modelo {@code email} (flash).
 * Sessão expirada (401) chega em {@code /login?expirou}; {@code ?destino=} diz para onde voltar depois de entrar.
 * O estado "Entrando…" do botão é do {@code login.js}: aqui só se confere o que o servidor renderiza.
 */
@WebMvcTest(LoginController.class)
class LoginPaginaTest {

    @Autowired
    MockMvc mvc;

    private Document pagina(String caminho) throws Exception {
        return pagina(get(caminho));
    }

    private Document pagina(MockHttpServletRequestBuilder requisicao) throws Exception {
        String html = mvc.perform(requisicao)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Jsoup.parse(html);
    }

    private String lerRecurso(String caminho) throws IOException {
        return new String(new ClassPathResource(caminho).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
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

    @Test
    void semAtributoEmailOCampoVemVazio() throws Exception {
        Document doc = pagina("/login?erro");

        assertThat(doc.selectFirst("input[data-testid=login-email]").val()).isEmpty();
    }

    @Test
    void aposFalhaOEmailDigitadoVoltaPreenchido() throws Exception {
        Document doc = pagina(get("/login?erro").flashAttr("email", "ana.lima@escola.edu.br"));

        assertThat(doc.selectFirst("input[data-testid=login-email]").val()).isEqualTo("ana.lima@escola.edu.br");
        assertThat(doc.selectFirst("[data-testid=login-erro]")).isNotNull();
    }

    @Test
    void emailComHtmlPerigosoVoltaEscapadoNoCampo() throws Exception {
        String perigoso = "\"><script>alert(1)</script>@x.com";

        String html = mvc.perform(get("/login?erro").flashAttr("email", perigoso))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Document doc = Jsoup.parse(html);

        assertThat(html).doesNotContain("<script>alert(1)");
        assertThat(doc.selectFirst("input[data-testid=login-email]").val()).isEqualTo(perigoso);
    }

    @Test
    void asenhaNuncaVoltaNoHtml() throws Exception {
        String html = mvc.perform(get("/login?erro")
                        .flashAttr("email", "ana.lima@escola.edu.br")
                        .flashAttr("senha", "segredo-123")
                        .param("senha", "segredo-456"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Document doc = Jsoup.parse(html);

        assertThat(html).doesNotContain("segredo-123").doesNotContain("segredo-456");
        assertThat(doc.selectFirst("input[data-testid=login-senha]").hasAttr("value")).isFalse();
    }

    @Test
    void sessaoExpiradaMostraAvisoAnunciadoEFocavel() throws Exception {
        Document doc = pagina("/login?expirou");

        var aviso = doc.selectFirst("[data-testid=login-sessao-expirada]");
        assertThat(aviso).isNotNull();
        assertThat(aviso.hasClass("alerta")).isTrue();
        assertThat(aviso.hasClass("alerta--aviso")).isTrue();
        assertThat(aviso.attr("role")).isEqualTo("alert");
        assertThat(aviso.attr("tabindex")).isEqualTo("-1");
        assertThat(aviso.text()).isEqualTo("Sua sessão expirou. Entre novamente.");
        assertThat(doc.select("[data-testid=login-erro]")).isEmpty();
    }

    @Test
    void semOParametroExpirouNaoMostraOAvisoDeSessao() throws Exception {
        assertThat(pagina("/login").select("[data-testid=login-sessao-expirada]")).isEmpty();
        assertThat(pagina("/login?erro").select("[data-testid=login-sessao-expirada]")).isEmpty();
    }

    @Test
    void seVieremOsDoisAvisosSoOErroDeCredencialAparece() throws Exception {
        Document doc = pagina("/login?erro&expirou");

        assertThat(doc.select("[data-testid=login-erro]")).hasSize(1);
        assertThat(doc.select("[data-testid=login-sessao-expirada]")).isEmpty();
        assertThat(doc.select("main [role=alert]")).hasSize(1);
    }

    @Test
    void sessaoExpiradaTambemMantemOEmailQuandoOBackEndPassa() throws Exception {
        Document doc = pagina(get("/login?expirou").flashAttr("email", "ana.lima@escola.edu.br"));

        assertThat(doc.selectFirst("input[data-testid=login-email]").val()).isEqualTo("ana.lima@escola.edu.br");
    }

    @Test
    void destinoDeUmCaminhoDoSiteVaiNoFormularioComoCampoOculto() throws Exception {
        Document doc = pagina("/login?expirou&destino=/reservas/minhas?pagina=2");

        var campo = doc.selectFirst("form[data-testid=login-formulario] input[type=hidden][name=destino]");
        assertThat(campo).isNotNull();
        assertThat(campo.val()).isEqualTo("/reservas/minhas?pagina=2");
        assertThat(campo.attr("data-testid")).isEqualTo("login-destino");
    }

    @ParameterizedTest(name = "[{index}] destino inválido")
    @MethodSource("destinosInvalidos")
    void destinoQueNaoECaminhoDoSiteNaoVaiNoFormulario(String destino) throws Exception {
        Document doc = pagina(get("/login").param("destino", destino));

        assertThat(doc.select("input[name=destino]")).isEmpty();
    }

    static Stream<String> destinosInvalidos() {
        return Stream.of(
                "/\t/evil.com", "/\n/evil.com", "/\r/evil.com", "/\0/evil.com", "/\t\\evil.com",
                "/%2f/evil.com", "/%2F/evil.com", "/%5c/evil.com", "/%5C/evil.com", "/%252f/evil.com",
                "//evil.com", "///x", "/\\evil.com", "/a\\b", " //x", " /x", "/ x", "/\u00a0/evil.com",
                "https://x", "javascript:x", "reservas/minhas", "", "/" + "a".repeat(2000));
    }

    @ParameterizedTest(name = "[{index}] destino válido: {0}")
    @ValueSource(strings = {"/", "/reservas/minhas", "/reservas/minhas?a=1&b=2", "/recursos/disponiveis?tipo=SALA#topo"})
    void destinoQueECaminhoDoSiteVaiNoFormulario(String destino) throws Exception {
        Document doc = pagina(get("/login").param("destino", destino));

        assertThat(doc.selectFirst("input[type=hidden][name=destino]").val()).isEqualTo(destino);
    }

    @Test
    void semDestinoNaoHaCampoOculto() throws Exception {
        assertThat(pagina("/login").select("input[name=destino]")).isEmpty();
    }

    @Test
    void destinoComHtmlPerigosoNaoVaiNoFormulario() throws Exception {
        String html = mvc.perform(get("/login").param("destino", "/x\"><script>alert(1)</script>"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("<script>alert(1)");
        assertThat(Jsoup.parse(html).select("input[name=destino]")).isEmpty();
    }

    @Test
    void oServidorRenderizaOBotaoNoEstadoNormalComORotuloDoEnvio() throws Exception {
        Document doc = pagina("/login");

        var botao = doc.selectFirst("button[data-testid=login-entrar]");
        assertThat(botao.hasAttr("disabled")).isFalse();
        assertThat(botao.hasAttr("aria-busy")).isFalse();
        assertThat(botao.hasClass("botao--carregando")).isFalse();
        assertThat(botao.select(".carregando")).isEmpty();
        assertThat(botao.text()).isEqualTo("Entrar");
        assertThat(botao.attr("data-enviando")).isEqualTo("Entrando…");
    }

    @Test
    void scriptDaTelaMoveOFocoParaOAvisoDeSessaoExpirada() throws Exception {
        Document doc = pagina("/login?expirou");

        assertThat(doc.select("script[src]").eachAttr("src")).contains("/js/login.js");
        assertThat(lerRecurso("static/js/login.js")).contains("login-sessao-expirada");
    }

    @Test
    void scriptDesabilitaOBotaoNoEnvioSemImpedirOEnvio() throws Exception {
        String js = lerRecurso("static/js/login.js");

        assertThat(js).contains("addEventListener('submit'");
        assertThat(js).contains("aria-busy");
        assertThat(js).contains("botao--carregando");
        // o envio nunca pode ser cancelado por esta tela; sem JavaScript o formulário continua funcionando
        assertThat(js).doesNotContain("preventDefault");
    }

    @Test
    void scriptRestauraOBotaoPorTemporizadorDeSegurancaEEmQualquerPageshow() throws Exception {
        String js = lerRecurso("static/js/login.js");

        // Esc/Parar, rede caída ou resposta sem navegação não podem deixar o botão preso em "Entrando…"
        assertThat(js).contains("setTimeout").contains("30000").contains("clearTimeout");
        assertThat(js).contains("addEventListener('pageshow'");
        assertThat(js).doesNotContain("persisted");
        assertThat(js).contains("focus()");
    }
}
