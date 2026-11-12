package br.com.senac.recursos.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import br.com.senac.recursos.enums.PerfilUsuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Spec 01 do Notion (acesso e navegação) em 375 px: avisos do login com foco, botão Entrar em "Entrando…"
 * durante o envio, menu recolhido no celular e páginas de erro 401, 403, 404 e 500 sem jargão técnico.
 * O login de verdade (D12, opção (a)) está em {@code entrarComo} e nos testes de "entrar" abaixo; o teste do botão
 * "Entrando…" segura o {@code POST /login} no navegador para ver o estado intermediário.
 */
class AcessoE2ETest extends E2ETeste {

    private Locator testid(String id) {
        return pagina.getByTestId(id);
    }

    @Test
    void loginRealLevaDeVoltaAPaginaPedidaEMostraOMenuDoPerfil() {
        entrarComo(PerfilUsuario.RESPONSAVEL);
        contexto.clearCookies();

        abrir("/relatorios/utilizacao");
        pagina.waitForURL("**/login?destino=**");
        testid("login-usuario").fill("e2e-responsavel");
        testid("login-senha").fill(SENHA);
        pagina.waitForNavigation(() -> testid("login-entrar").click());
        pagina.waitForURL("**/relatorios/utilizacao");

        assertThat(testid("menu-usuario")).hasText("Carlos Souza");
        assertThat(testid("menu-perfil")).hasText("Responsável");
        assertThat(testid("relatorio-titulo")).isVisible();
    }

    @Test
    void loginRealComSenhaErradaVoltaComOAvisoEOLoginDigitado() {
        entrarComo(PerfilUsuario.SOLICITANTE);
        contexto.clearCookies();

        abrir("/login");
        testid("login-usuario").fill("e2e-solicitante");
        testid("login-senha").fill("senha-errada");
        pagina.waitForNavigation(() -> testid("login-entrar").click());
        pagina.waitForURL("**/login?erro");

        assertThat(testid("login-erro")).isVisible();
        assertThat(testid("login-usuario")).hasValue("e2e-solicitante");
        assertThat(testid("login-senha")).hasValue("");
    }

    @Test
    void sairEncerraASessaoEAsTelasVoltamAPedirLogin() {
        entrarComo(PerfilUsuario.SOLICITANTE);

        testid("menu-abrir").click();
        pagina.waitForNavigation(() -> testid("menu-sair").click());
        pagina.waitForURL("**/login");
        abrir("/reservas/minhas");

        assertThat(caminhoAtual()).startsWith("/login");
        assertThat(testid("login-formulario")).isVisible();
    }

    @Test
    void solicitanteRecebe403NaTelaDoResponsavel() {
        entrarComo(PerfilUsuario.SOLICITANTE);

        var resposta = pagina.navigate(url("/aprovacoes"));

        assertThat(resposta.status()).isEqualTo(403);
        assertThat(testid("erro-codigo")).containsText("403");
    }

    @Test
    void credencialInvalidaMostraAvisoComFocoSemDizerQualCampoErrou() {
        abrir("/login?erro");

        assertThat(testid("login-erro")).isVisible();
        assertThat(testid("login-erro")).containsText("Login ou senha incorretos");
        assertThat(focado()).isEqualTo("login-erro");
    }

    @Test
    void sessaoExpiradaMostraAvisoComFoco() {
        abrir("/login?expirou");

        assertThat(testid("login-sessao-expirada")).containsText("Sua sessão expirou. Entre novamente.");
        assertThat(focado()).isEqualTo("login-sessao-expirada");
    }

    @Test
    void entrarFicaDesabilitadoEmEntrandoDuranteOEnvioEEnviaUmaVezSo() {
        var posts = segurarPosts("**/login");
        abrir("/login");
        testid("login-usuario").fill("ana.lima");
        testid("login-senha").fill("segredo-de-teste");

        pagina.waitForRequest(r -> "POST".equals(r.method()), () -> testid("login-entrar").click());

        var entrar = testid("login-entrar");
        assertThat(entrar).isDisabled();
        assertThat(entrar).containsText("Entrando…");
        assertThat(entrar).hasAttribute("aria-busy", "true");
        print("01-login-entrando");
        entrar.click(new Locator.ClickOptions().setForce(true));
        assertThat(enviosDeFormulario()).isEqualTo(1);
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).postData()).contains("login=ana.lima").contains("senha=");
    }

    @Test
    void menuFicaRecolhidoNoCelularAbreNoBotaoEFechaComEsc() {
        abrir("/e2e/minhas");
        var botao = testid("menu-abrir");
        var item = testid("menu-item-busca");

        assertThat(botao).isVisible();
        assertThat(botao).hasAttribute("aria-expanded", "false");
        assertThat(item).isHidden();

        botao.click();
        assertThat(botao).hasAttribute("aria-expanded", "true");
        assertThat(botao).hasAttribute("aria-label", "Fechar menu");
        assertThat(item).isVisible();
        assertThat(testid("menu-item-minhas")).hasAttribute("aria-current", "page");
        assertThat(testid("menu-usuario")).hasText("Ana Lima");
        assertThat(testid("menu-sair")).isVisible();
        semRolagemHorizontal();
        print("01-menu-aberto");

        pagina.keyboard().press("Escape");
        assertThat(botao).hasAttribute("aria-expanded", "false");
        assertThat(item).isHidden();
        assertThat(focado()).isEqualTo("menu-abrir");
    }

    @Test
    void menuSoMostraOsItensDoPerfil() {
        abrir("/e2e/minhas");
        testid("menu-abrir").click();

        assertThat(pagina.locator("[data-testid^=menu-item-]")).hasCount(2);
        assertThat(testid("menu-item-aprovacoes")).hasCount(0);
        assertThat(testid("menu-item-recursos")).hasCount(0);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "401, /e2e/erro/401, Você precisa entrar,                               /login?expirou",
            "403, /e2e/erro/403, Você não tem permissão para acessar esta página,   /",
            "404, /reservas/999999, Página não encontrada,                             /",
            "500, /e2e/erro/500, Algo deu errado,                                   /e2e/erro/500"
    })
    void paginaDeErroTemMensagemPropriaECaminhoParaContinuar(String codigo, String caminho, String titulo, String destino) {
        // Logado: sem sessão as telas levam ao login; o 404 é de uma reserva que não existe
        entrarComo(PerfilUsuario.SOLICITANTE);
        var resposta = pagina.navigate(url(caminho));

        assertThat(resposta.status()).isEqualTo(Integer.parseInt(codigo));
        assertThat(testid("erro-codigo")).containsText(codigo);
        assertThat(testid("erro-titulo")).containsText(titulo);
        assertThat(testid("erro-acao-principal")).hasAttribute("href", destino);
        String texto = pagina.locator("main").innerText();
        assertThat(texto).doesNotContain("Exception", "at br.com", "Whitelabel", "SQL", "trace");
    }
}
