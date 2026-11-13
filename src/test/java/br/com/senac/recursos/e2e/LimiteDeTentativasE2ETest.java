package br.com.senac.recursos.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.senac.recursos.dto.request.UsuarioRequest;
import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.support.RelogioDeTeste;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.RequestOptions;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * Limite de tentativas no login (ADR-0009, ressalva 1 do PR #48) no navegador, em 375 px: o que a pessoa vê quando o
 * login fica bloqueado, pelo formulário de verdade (CSRF, cookie, {@code login.js}).
 * <ul>
 *   <li>bloqueada, ela vê exatamente a tela de uma senha errada: o mesmo aviso, com foco, o login digitado e a senha
 *       em branco, sem nada que diga "bloqueado";</li>
 *   <li>passados 15 minutos no {@link RelogioDeTeste}, a senha certa entra;</li>
 *   <li>falhas pela API ({@code POST /api/auth/login}, pelo próprio navegador) bloqueiam a tela, porque o contador
 *       é um só.</li>
 * </ul>
 * Diferente das outras classes E2E, esta usa o relógio controlado no lugar do {@code Clock} da aplicação (contexto
 * próprio do Spring), para não esperar 15 minutos de verdade nem usar {@code Thread.sleep}. Cada teste usa um login
 * próprio, porque o contador vive enquanto o contexto viver.
 */
@Import(LimiteDeTentativasE2ETest.RelogioControlado.class)
class LimiteDeTentativasE2ETest extends E2ETeste {

    private static final Instant AGORA = Instant.parse("2026-11-01T12:00:00Z");
    private static final String SENHA_ERRADA = "senha-errada";
    private static final int LIMITE = 5;

    @TestConfiguration(proxyBeanMethods = false)
    static class RelogioControlado {

        @Bean
        @Primary
        RelogioDeTeste relogioDoE2E() {
            return new RelogioDeTeste(AGORA);
        }
    }

    @Autowired
    RelogioDeTeste relogio;

    @BeforeEach
    void relogioNoInicio() {
        relogio.ajustar(AGORA);
    }

    @Test
    void bloqueadoVeAMesmaTelaDeSenhaErradaMesmoComASenhaCerta() {
        String login = usuario("e2e-bloqueio-tela");
        errarPelaTela(login, LIMITE - 1);

        tentarPelaTela(login, SENHA_ERRADA);
        String telaDaSenhaErrada = testid("login-erro").innerText();
        String conteudoDaSenhaErrada = conteudoSemCampos();

        tentarPelaTela(login, SENHA);

        assertThat(caminhoAtual()).isEqualTo("/login?erro");
        assertThat(testid("login-erro")).isVisible();
        assertThat(testid("login-erro")).isFocused();
        assertThat(testid("login-erro")).hasText(telaDaSenhaErrada);
        assertThat(testid("login-usuario")).hasValue(login);
        assertThat(testid("login-senha")).hasValue("");
        assertThat(testid("menu-usuario")).hasCount(0);
        assertThat(conteudoSemCampos()).isEqualTo(conteudoDaSenhaErrada).doesNotContainIgnoringCase("bloque");
        semRolagemHorizontal();
        print("login-bloqueado");
    }

    @Test
    void depoisDeQuinzeMinutosASenhaCertaEntraPeloFormulario() {
        String login = usuario("e2e-bloqueio-quinze");
        errarPelaTela(login, LIMITE);

        relogio.avancar(Duration.ofMinutes(15).minusSeconds(1));
        tentarPelaTela(login, SENHA);
        assertThat(caminhoAtual()).isEqualTo("/login?erro");

        relogio.avancar(Duration.ofSeconds(1));
        tentarPelaTela(login, SENHA);

        assertThat(caminhoAtual()).doesNotStartWith("/login");
        assertThat(testid("menu-usuario")).hasText("Pessoa Limite");
    }

    @Test
    void falhasPelaApiBloqueiamOFormularioDoNavegador() {
        String login = usuario("e2e-bloqueio-api");
        for (int i = 0; i < LIMITE; i++) {
            APIResponse resposta = pagina.request().post(url("/api/auth/login"), RequestOptions.create()
                    .setHeader("Content-Type", "application/json")
                    .setData("{\"login\":\"" + login + "\",\"senha\":\"" + SENHA_ERRADA + "\"}"));
            assertThat(resposta.status()).isEqualTo(401);
        }

        tentarPelaTela(login, SENHA);

        assertThat(caminhoAtual()).isEqualTo("/login?erro");
        assertThat(testid("login-erro")).isVisible();
    }

    // ---------------------------------------------------------------- apoio

    private Locator testid(String id) {
        return pagina.getByTestId(id);
    }

    /** Cria o usuário e devolve o e-mail dele ({@code apelido@exemplo.com}). */
    private String usuario(String apelido) {
        String email = apelido + "@exemplo.com";
        if (!repositorioDeUsuarios.existsByLogin(email)) {
            usuarios.criar(new UsuarioRequest("Pessoa Limite", email, SENHA, PerfilUsuario.SOLICITANTE));
        }
        return email;
    }

    /** Preenche e envia o formulário como a pessoa faria, e espera a página seguinte. */
    private void tentarPelaTela(String login, String senha) {
        abrir("/login");
        testid("login-usuario").fill(login);
        testid("login-senha").fill(senha);
        pagina.waitForNavigation(() -> testid("login-entrar").click());
    }

    private void errarPelaTela(String login, int vezes) {
        for (int i = 0; i < vezes; i++) {
            tentarPelaTela(login, SENHA_ERRADA);
            assertThat(caminhoAtual()).isEqualTo("/login?erro");
        }
    }

    /** O texto da página do login (o que a pessoa lê), sem os valores dos campos. */
    private String conteudoSemCampos() {
        return pagina.locator("#conteudo-pagina").innerText();
    }
}
