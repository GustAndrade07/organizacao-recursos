package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.senac.recursos.service.UsuarioService;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * CA-2 e CA-6 (Spec "Políticas do site"), com servidor de verdade e PostgreSQL em Testcontainers (só o CI roda: a
 * máquina de desenvolvimento não tem Docker). Prova o que {@code MatrizAutorizacaoTest} não prova (a célula {@code ok}
 * aceita até um 302): as três páginas respondem exatamente 200 para quem não entrou, sem {@code Location} e sem
 * {@code Set-Cookie}; o que a regra {@code permitAll} não cobre continua negado; e o conjunto de cookies que o sistema
 * emite é só o {@code JSESSIONID}, com {@code HttpOnly} e {@code SameSite=Lax} (a base de dispensar o banner de
 * consentimento, e o que a política de cookies afirma).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PoliticasPublicasIntegracaoTest {

    private static final String LOGIN = "admin.politicas";
    private static final String SENHA = "senha-de-teste-123";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @LocalServerPort
    int porta;

    @Autowired
    UsuarioService usuarios;

    @BeforeEach
    void administrador() {
        usuarios.garantirAdministrador("Administrador de Teste", LOGIN, SENHA);
    }

    /** Cliente novo, sem cookie nenhum: é a primeira visita de quem chega de fora. */
    private HttpClient visitante() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private HttpResponse<String> enviar(HttpClient cliente, String metodo, String caminho, String corpo) throws Exception {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .header("Accept", "text/html");
        if (corpo == null) {
            pedido.method(metodo, HttpRequest.BodyPublishers.noBody());
        } else {
            pedido.header("Content-Type", "application/x-www-form-urlencoded")
                    .method(metodo, HttpRequest.BodyPublishers.ofString(corpo));
        }
        return cliente.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private void entrar(HttpClient cliente) throws Exception {
        Document formulario = Jsoup.parse(enviar(cliente, "GET", "/login", null).body());
        String csrf = formulario.selectFirst("input[name=_csrf]").val();
        HttpResponse<String> resposta = enviar(cliente, "POST", "/login",
                "_csrf=" + codificar(csrf) + "&login=" + codificar(LOGIN) + "&senha=" + codificar(SENHA));
        assertThat(resposta.statusCode()).isEqualTo(302);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/privacidade", "/cookies", "/termos"})
    void semSessaoDevolve200SemRedirecionarNemCriarCookie(String caminho) throws Exception {
        HttpResponse<String> resposta = enviar(visitante(), "GET", caminho, null);

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.headers().firstValue("Location")).isEmpty();
        assertThat(resposta.headers().allValues("Set-Cookie")).as("página pública não cria sessão").isEmpty();
        assertThat(resposta.headers().firstValue("X-Frame-Options")).hasValue("DENY");
    }

    @Test
    void consultaNaUrlEIgnoradaMasOutrasGrafiasNaoCasam() throws Exception {
        assertThat(enviar(visitante(), "GET", "/privacidade?x=1", null).statusCode()).isEqualTo(200);

        for (String grafia : List.of("/privacidade/", "/privacidade.html", "/Privacidade")) {
            HttpResponse<String> resposta = enviar(visitante(), "GET", grafia, null);
            assertThat(resposta.statusCode()).as(grafia).isEqualTo(302);
            assertThat(resposta.headers().firstValue("Location").orElseThrow()).as(grafia).contains("/login");
        }
        // HEAD não casa a regra de GET: fica como o /login, que também devolve 302 (decisão documentada na Spec)
        assertThat(enviar(visitante(), "HEAD", "/privacidade", null).statusCode()).isEqualTo(302);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/privacidade", "/cookies", "/termos"})
    void postNasPaginasContinuaNegadoEMaisNuncaEDoisXXNemCincoXX(String caminho) throws Exception {
        // sem CSRF: o filtro de CSRF vem antes da autorização
        assertThat(enviar(visitante(), "POST", caminho, "x=1").statusCode()).isEqualTo(403);

        // com CSRF e sem sessão: negado pela regra final, que leva ao login
        HttpClient cliente = visitante();
        Document formulario = Jsoup.parse(enviar(cliente, "GET", "/login", null).body());
        String csrf = formulario.selectFirst("input[name=_csrf]").val();
        HttpResponse<String> comCsrf = enviar(cliente, "POST", caminho, "_csrf=" + codificar(csrf));
        assertThat(comCsrf.statusCode()).isEqualTo(302);
        assertThat(comCsrf.headers().firstValue("Location").orElseThrow()).contains("/login");

        // logado: 403
        HttpClient logado = visitante();
        entrar(logado);
        String csrfDoLogado = Jsoup.parse(enviar(logado, "GET", "/login", null).body())
                .selectFirst("input[name=_csrf]").val();
        assertThat(enviar(logado, "POST", caminho, "_csrf=" + codificar(csrfDoLogado)).statusCode()).isEqualTo(403);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/privacidade", "/cookies", "/termos"})
    void logadoVeAPaginaComOMenu(String caminho) throws Exception {
        HttpClient cliente = visitante();
        entrar(cliente);

        HttpResponse<String> resposta = enviar(cliente, "GET", caminho, null);
        Document doc = Jsoup.parse(resposta.body());

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(doc.selectFirst("[data-testid=menu-usuario]").text()).isEqualTo("Administrador de Teste");
    }

    /** CA-6: o conjunto de cookies emitidos é exatamente {JSESSIONID}, com HttpOnly e SameSite=Lax. */
    @Test
    void osUnicosCookiesDoSiteSaoOJsessionidComHttpOnlyESameSiteLax() throws Exception {
        List<String> cookies = new ArrayList<>();
        for (String caminho : List.of("/login", "/privacidade", "/cookies", "/termos", "/nao-existe")) {
            cookies.addAll(enviar(visitante(), "GET", caminho, null).headers().allValues("Set-Cookie"));
        }
        // login completo e logout
        HttpClient cliente = visitante();
        Document formulario = Jsoup.parse(enviar(cliente, "GET", "/login", null).body());
        String csrf = formulario.selectFirst("input[name=_csrf]").val();
        cookies.addAll(enviar(cliente, "POST", "/login",
                "_csrf=" + codificar(csrf) + "&login=" + codificar(LOGIN) + "&senha=" + codificar(SENHA))
                .headers().allValues("Set-Cookie"));

        assertThat(cookies).isNotEmpty();
        assertThat(cookies.stream().map(c -> c.substring(0, c.indexOf('=')))).containsOnly("JSESSIONID");
        assertThat(cookies).allSatisfy(c -> assertThat(c).containsIgnoringCase("HttpOnly")
                .containsIgnoringCase("SameSite=Lax"));
    }
}
