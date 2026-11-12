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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Com servidor de verdade (porta aleatória) e PostgreSQL em Testcontainers: o caminho completo do container até o
 * template, inclusive o login da tela (D12, opção (a)), com CSRF, cookie de sessão e redirecionamento.
 * <ul>
 *   <li>sem sessão, uma URL que não existe leva ao login, com a URL pedida em {@code ?destino=};</li>
 *   <li>depois do login, a mesma URL devolve a página 404 do sistema (com o menu de quem entrou), e não a página
 *       padrão do Spring.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaginaNaoEncontradaIntegracaoTest {

    private static final String LOGIN = "admin.teste";
    private static final String SENHA = "senha-de-teste-123";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @LocalServerPort
    int porta;

    @Autowired
    UsuarioService usuarios;

    private final HttpClient navegador = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @BeforeEach
    void administrador() {
        usuarios.garantirAdministrador("Administrador de Teste", LOGIN, SENHA);
    }

    @Test
    void semSessaoUrlInexistenteLevaAoLoginComODestino() throws Exception {
        HttpResponse<String> resposta = abrir("/nao-existe");

        assertThat(resposta.statusCode()).isEqualTo(302);
        assertThat(resposta.headers().firstValue("Location").orElseThrow()).endsWith("/login?destino=%2Fnao-existe");
    }

    @Test
    void logadoUrlInexistenteDevolve404ComAPaginaDoSistema() throws Exception {
        entrar();

        HttpResponse<String> resposta = abrir("/nao-existe");
        Document doc = Jsoup.parse(resposta.body());

        assertThat(resposta.statusCode()).isEqualTo(404);
        assertThat(doc.selectFirst("[data-testid=erro-codigo]").text()).isEqualTo("Erro 404");
        assertThat(doc.selectFirst("[data-testid=erro-titulo]").text()).isEqualTo("Página não encontrada");
        assertThat(doc.selectFirst("a[data-testid=erro-acao-principal]").attr("href")).isEqualTo("/");
        assertThat(doc.selectFirst("[data-testid=menu-usuario]").text()).isEqualTo("Administrador de Teste");
        assertThat(doc.text()).doesNotContain("Whitelabel");
    }

    @Test
    void loginComSenhaErradaVoltaAoFormularioComOAvisoEOLoginDigitado() throws Exception {
        HttpResponse<String> resposta = enviarLogin(LOGIN, "senha-errada", null);

        assertThat(resposta.statusCode()).isEqualTo(302);
        assertThat(resposta.headers().firstValue("Location").orElseThrow()).endsWith("/login?erro");
        Document doc = Jsoup.parse(abrir("/login?erro").body());
        assertThat(doc.selectFirst("[data-testid=login-erro]")).isNotNull();
        assertThat(doc.selectFirst("[data-testid=login-usuario]").val()).isEqualTo(LOGIN);
        assertThat(doc.html()).doesNotContain("senha-errada");
    }

    @Test
    void loginLevaAoDestinoSoSeForCaminhoDesteSite() throws Exception {
        assertThat(enviarLogin(LOGIN, SENHA, "/relatorios/utilizacao").headers().firstValue("Location").orElseThrow())
                .endsWith("/relatorios/utilizacao");
        assertThat(enviarLogin(LOGIN, SENHA, "//evil.example").headers().firstValue("Location").orElseThrow())
                .isEqualTo("http://localhost:" + porta + "/");
    }

    private void entrar() throws Exception {
        assertThat(enviarLogin(LOGIN, SENHA, null).statusCode()).isEqualTo(302);
    }

    /** Abre o formulário (o CSRF vem nele, ligado à sessão do cookie) e envia como o navegador enviaria. */
    private HttpResponse<String> enviarLogin(String login, String senha, String destino) throws Exception {
        Document formulario = Jsoup.parse(abrir("/login").body());
        String csrf = formulario.selectFirst("input[name=_csrf]").val();
        String corpo = "_csrf=" + codificar(csrf) + "&login=" + codificar(login) + "&senha=" + codificar(senha)
                + (destino == null ? "" : "&destino=" + codificar(destino));
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
