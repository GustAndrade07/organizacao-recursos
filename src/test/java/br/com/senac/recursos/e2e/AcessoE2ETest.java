package br.com.senac.recursos.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Spec 01 do Notion (acesso e navegação) em 375 px: avisos do login com foco, botão Entrar em "Entrando…"
 * durante o envio, menu recolhido no celular e páginas de erro 401, 403, 404 e 500 sem jargão técnico.
 * A autenticação ainda não existe (RF-01, back-end): o {@code POST /login} é segurado no navegador.
 */
class AcessoE2ETest extends E2ETeste {

    private Locator testid(String id) {
        return pagina.getByTestId(id);
    }

    @Test
    void credencialInvalidaMostraAvisoComFocoSemDizerQualCampoErrou() {
        abrir("/login?erro");

        assertThat(testid("login-erro")).isVisible();
        assertThat(testid("login-erro")).containsText("E-mail ou senha incorretos");
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
        testid("login-email").fill("ana.lima@senac.br");
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
        assertThat(posts.get(0).postData()).contains("email=ana.lima%40senac.br").contains("senha=");
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
            "404, /nao-existe,   Página não encontrada,                             /",
            "500, /e2e/erro/500, Algo deu errado,                                   /e2e/erro/500"
    })
    void paginaDeErroTemMensagemPropriaECaminhoParaContinuar(String codigo, String caminho, String titulo, String destino) {
        var resposta = pagina.navigate(url(caminho));

        assertThat(resposta.status()).isEqualTo(Integer.parseInt(codigo));
        assertThat(testid("erro-codigo")).containsText(codigo);
        assertThat(testid("erro-titulo")).containsText(titulo);
        assertThat(testid("erro-acao-principal")).hasAttribute("href", destino);
        String texto = pagina.locator("main").innerText();
        assertThat(texto).doesNotContain("Exception", "at br.com", "Whitelabel", "SQL", "trace");
    }
}
