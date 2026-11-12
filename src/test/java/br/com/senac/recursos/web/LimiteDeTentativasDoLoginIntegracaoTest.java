package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.senac.recursos.service.UsuarioService;
import br.com.senac.recursos.support.RelogioDeTeste;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Limite de tentativas no login das telas (ressalva 1 do Gustavo no PR #48, ADR-0009), com servidor de verdade (porta
 * aleatória), PostgreSQL em Testcontainers, CSRF e cookie de sessão, como um navegador faria:
 * <ul>
 *   <li>5 falhas seguidas no mesmo login, dentro de 15 minutos, bloqueiam esse login por 15 minutos;</li>
 *   <li>durante o bloqueio, nem a senha certa entra, e a tentativa não estende o bloqueio;</li>
 *   <li>o login bloqueado recebe a mesma resposta de uma senha errada (a tela não revela o bloqueio);</li>
 *   <li>login certo zera o contador; falha mais velha que 15 minutos não conta;</li>
 *   <li>login inexistente também conta, para não diferenciar logins que existem dos que não existem.</li>
 * </ul>
 * O tempo passa pelo {@link RelogioDeTeste} no lugar do {@code Clock} da aplicação: nada de {@code Thread.sleep}
 * (regras 5 e 14 do {@code AGENTS.md}). Cada teste usa um login próprio, porque o contador vive enquanto o contexto
 * do Spring viver.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(LimiteDeTentativasDoLoginIntegracaoTest.RelogioControlado.class)
class LimiteDeTentativasDoLoginIntegracaoTest {

    private static final Instant AGORA = Instant.parse("2026-11-01T12:00:00Z");
    private static final String SENHA = "senha-de-teste-123";
    private static final String SENHA_ERRADA = "senha-errada";
    private static final int LIMITE = 5;
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @TestConfiguration(proxyBeanMethods = false)
    static class RelogioControlado {

        @Bean
        @Primary
        RelogioDeTeste relogioDoLogin() {
            return new RelogioDeTeste(AGORA);
        }
    }

    @LocalServerPort
    int porta;

    @Autowired
    UsuarioService usuarios;

    @Autowired
    RelogioDeTeste relogio;

    private HttpClient navegador = novoNavegador();

    @BeforeEach
    void relogioNoInicio() {
        relogio.ajustar(AGORA);
    }

    @Test
    void quatroFalhasAindaNaoBloqueiam() throws Exception {
        String login = usuario("quatro.falhas");
        errar(login, LIMITE - 1);

        assertEntrou(enviarLogin(login, SENHA));
    }

    @Test
    void cincoFalhasSeguidasBloqueiamEASextaTentativaComASenhaCertaFalha() throws Exception {
        String login = usuario("cinco.falhas");
        errar(login, LIMITE);

        assertRecusado(enviarLogin(login, SENHA));
    }

    /** O bloqueio conta do instante da 5ª falha; tentar no meio dele não empurra o fim (não estende). */
    @Test
    void depoisDeQuinzeMinutosASenhaCertaEntra() throws Exception {
        String login = usuario("quinze.minutos");
        errar(login, LIMITE);

        relogio.avancar(BLOQUEIO.minusSeconds(1));
        assertRecusado(enviarLogin(login, SENHA));

        relogio.avancar(Duration.ofSeconds(1));
        assertEntrou(enviarLogin(login, SENHA));
    }

    @Test
    void loginCertoZeraOContador() throws Exception {
        String login = usuario("zera.contador");
        errar(login, LIMITE - 1);
        assertEntrou(enviarLogin(login, SENHA));

        errar(login, LIMITE - 1);

        assertEntrou(enviarLogin(login, SENHA));
    }

    /** Só contam as falhas dos últimos 15 minutos: 4 antigas e 1 nova não bloqueiam. */
    @Test
    void falhaMaisVelhaQueQuinzeMinutosNaoConta() throws Exception {
        String login = usuario("falha.antiga");
        errar(login, LIMITE - 1);

        relogio.avancar(BLOQUEIO);
        errar(login, 1);

        assertEntrou(enviarLogin(login, SENHA));
    }

    /**
     * As 5 falhas acontecem quando o login ainda não existe; o usuário é criado depois, ainda dentro do bloqueio, e a
     * senha certa não entra. Prova que o contador não olha se o login existe.
     */
    @Test
    void loginInexistenteTambemBloqueia() throws Exception {
        String login = "ainda.nao.existe";
        errar(login, LIMITE);

        usuarios.garantirAdministrador("Usuário " + login, login, SENHA);

        assertRecusado(enviarLogin(login, SENHA));
    }

    /**
     * A 5ª tentativa é uma senha errada comum; a 6ª, com a senha certa, cai no bloqueio. As duas têm de dar a mesma
     * resposta e a mesma tela: o mesmo redirecionamento e a mesma página de {@code /login?erro}, com o login digitado
     * e o aviso genérico, sem nada que diga "bloqueado".
     */
    @Test
    void respostaDoLoginBloqueadoIgualADeSenhaErrada() throws Exception {
        String login = usuario("mesma.resposta");
        errar(login, LIMITE - 1);

        HttpResponse<String> senhaErrada = enviarLogin(login, SENHA_ERRADA);
        String telaDaSenhaErrada = telaDeErroSemCsrf();
        HttpResponse<String> bloqueado = enviarLogin(login, SENHA);
        String telaDoBloqueado = telaDeErroSemCsrf();

        assertThat(bloqueado.statusCode()).isEqualTo(senhaErrada.statusCode()).isEqualTo(302);
        assertThat(bloqueado.headers().firstValue("Location"))
                .isEqualTo(senhaErrada.headers().firstValue("Location"));
        assertThat(bloqueado.body()).isEqualTo(senhaErrada.body());
        assertThat(telaDoBloqueado).isEqualTo(telaDaSenhaErrada);
        assertThat(Jsoup.parse(telaDoBloqueado).text()).doesNotContainIgnoringCase("bloque");
    }

    // ---------------------------------------------------------------- apoio

    private String usuario(String login) {
        usuarios.garantirAdministrador("Usuário " + login, login, SENHA);
        return login;
    }

    private void errar(String login, int vezes) throws Exception {
        for (int i = 0; i < vezes; i++) {
            assertRecusado(enviarLogin(login, SENHA_ERRADA));
        }
    }

    private static void assertRecusado(HttpResponse<String> resposta) {
        assertThat(resposta.statusCode()).isEqualTo(302);
        assertThat(resposta.headers().firstValue("Location").orElseThrow()).endsWith("/login?erro");
    }

    /** Entrou; a próxima tentativa sai de um navegador novo, sem a sessão de quem já entrou. */
    private void assertEntrou(HttpResponse<String> resposta) {
        assertThat(resposta.statusCode()).isEqualTo(302);
        assertThat(resposta.headers().firstValue("Location").orElseThrow()).isEqualTo("http://localhost:" + porta + "/");
        navegador = novoNavegador();
    }

    private static HttpClient novoNavegador() {
        return HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** A tela que o navegador vê depois da falha, sem o token CSRF (que muda a cada página por desenho). */
    private String telaDeErroSemCsrf() throws Exception {
        Document tela = Jsoup.parse(abrir("/login?erro").body());
        assertThat(tela.selectFirst("[data-testid=login-erro]")).isNotNull();
        tela.select("input[name=_csrf], meta[name^=_csrf]").remove();
        return tela.html();
    }

    /** Abre o formulário (o CSRF vem nele, ligado à sessão do cookie) e envia como o navegador enviaria. */
    private HttpResponse<String> enviarLogin(String login, String senha) throws Exception {
        Document formulario = Jsoup.parse(abrir("/login").body());
        String csrf = formulario.selectFirst("input[name=_csrf]").val();
        String corpo = "_csrf=" + codificar(csrf) + "&login=" + codificar(login) + "&senha=" + codificar(senha);
        HttpRequest pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        return navegador.send(pedido, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> abrir(String caminho) throws Exception {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .header("Accept", "text/html")
                .GET()
                .build();
        return navegador.send(pedido, HttpResponse.BodyHandlers.ofString());
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
