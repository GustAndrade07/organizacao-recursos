package br.com.senac.recursos.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Route;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes E2E (ADR-0004, RF-13): a aplicação real sobe numa porta aleatória contra PostgreSQL em
 * Testcontainers (nunca H2) e o Chromium do Playwright, sem tela, abre as páginas em <b>375 px</b> de largura
 * (celular, {@code isMobile}). As rotas reais das telas mostram só o estado vazio, porque o back-end ainda não
 * existe; por isso {@link TelasComDados} (só do teste, fora do component scan) renderiza os mesmos templates com
 * dados de exemplo em {@code /e2e/...}. Os {@code POST} que o back-end ainda não atende são interceptados no
 * navegador ({@link #segurarPosts(String)}), contados e respondidos com 204, sem conteúdo.
 *
 * <p>Esperas só pelo próprio Playwright (auto-wait e asserções), nunca {@code Thread.sleep}. Os prints de
 * evidência vão para {@code target/e2e/375}. O navegador vem do Playwright: no CI o passo "Navegador do E2E" do
 * {@code ci.yml} instala o Chromium; numa máquina sem ele o Playwright baixa na primeira execução.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TelasComDados.class)
abstract class E2ETeste {

    static final int LARGURA = 375;
    static final int ALTURA = 812;
    static final Path PRINTS = Path.of("target", "e2e", String.valueOf(LARGURA));

    /** Um container para todas as classes E2E: o contexto do Spring também é um só (mesma configuração). */
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    private static Playwright playwright;
    private static Browser navegador;

    @LocalServerPort
    int porta;

    BrowserContext contexto;
    Page pagina;

    @BeforeAll
    static void abrirNavegador() {
        playwright = Playwright.create();
        navegador = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void fecharNavegador() {
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void abrirContexto() {
        contexto = navegador.newContext(new Browser.NewContextOptions()
                .setViewportSize(LARGURA, ALTURA)
                .setDeviceScaleFactor(1)
                .setIsMobile(true)
                .setHasTouch(true)
                .setLocale("pt-BR"));
        contexto.setDefaultTimeout(10_000);
        // Conta os envios de formulário que de fato saíram (o "submit" não cancelado por um script de validação)
        contexto.addInitScript("""
                window.__envios = 0;
                document.addEventListener('submit', function (e) {
                  setTimeout(function () { if (!e.defaultPrevented) { window.__envios++; } }, 0);
                });
                """);
        pagina = contexto.newPage();
    }

    @AfterEach
    void fecharContexto() {
        contexto.close();
    }

    String url(String caminho) {
        return "http://localhost:" + porta + caminho;
    }

    Page abrir(String caminho) {
        pagina.navigate(url(caminho));
        return pagina;
    }

    /** Caminho e consulta da página atual, sem o "http://localhost:porta". */
    String caminhoAtual() {
        return pagina.url().substring(url("").length());
    }

    int enviosDeFormulario() {
        // O contador é atualizado no ciclo seguinte ao "submit"; o evaluate roda depois dele na fila do navegador
        pagina.evaluate("() => new Promise(r => setTimeout(r, 0))");
        return ((Number) pagina.evaluate("() => window.__envios")).intValue();
    }

    /**
     * Intercepta os POST para as URLs do padrão no lugar do back-end, que ainda não existe, e responde
     * {@code 204 No Content}: o navegador fica na mesma página (no estado de "enviando") e a lista devolvida
     * guarda cada pedido que saiu dele.
     */
    List<Request> segurarPosts(String padrao) {
        List<Request> posts = new ArrayList<>();
        pagina.route(padrao, (Route rota) -> {
            if ("POST".equals(rota.request().method())) {
                posts.add(rota.request());
                rota.fulfill(new Route.FulfillOptions().setStatus(204));
            } else {
                rota.resume();
            }
        });
        return posts;
    }

    void semRolagemHorizontal() {
        int larguraDoDocumento = ((Number) pagina.evaluate("() => document.documentElement.scrollWidth")).intValue();
        assertThat(larguraDoDocumento).as("largura do documento em %s", caminhoAtual()).isLessThanOrEqualTo(LARGURA);
    }

    void print(String nome) {
        pagina.screenshot(new Page.ScreenshotOptions().setPath(PRINTS.resolve(nome + ".png")).setFullPage(true));
    }

    String focado() {
        return (String) pagina.evaluate("() => document.activeElement && document.activeElement.getAttribute('data-testid')");
    }
}
