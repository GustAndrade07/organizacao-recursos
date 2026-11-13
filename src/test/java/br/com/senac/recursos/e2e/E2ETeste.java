package br.com.senac.recursos.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Route;
import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.dto.request.RecursoRequest;
import br.com.senac.recursos.dto.request.UsuarioRequest;
import br.com.senac.recursos.dto.response.RecursoResponse;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.model.Periodo;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.repository.UsuarioRepository;
import br.com.senac.recursos.service.RecursoService;
import br.com.senac.recursos.service.ReservaService;
import br.com.senac.recursos.service.UsuarioService;
import br.com.senac.recursos.web.FormatoDasTelas;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes E2E (ADR-0004, RF-13): a aplicação real sobe numa porta aleatória contra PostgreSQL em
 * Testcontainers (nunca H2) e o Chromium do Playwright, sem tela, abre as páginas em <b>375 px</b> de largura
 * (celular, {@code isMobile}). Desde a ligação das telas com o back-end (D12, ADR-0009) há dois tipos de teste:
 * <ul>
 *   <li><b>contra o back-end real</b>: entram pela tela de login ({@link #entrarComo}), usam dados gravados no
 *       banco ({@link #salaReal}, {@link #reservaReal}) e conferem o resultado na tela;</li>
 *   <li><b>de layout e comportamento do navegador</b> (375 px, foco, diálogo, envio único): {@link TelasComDados}
 *       (só do teste, fora do component scan) renderiza os templates com dados de exemplo em {@code /e2e/...},
 *       rotas abertas só no teste; quando o teste só quer medir o envio, o {@code POST} é interceptado no
 *       navegador ({@link #segurarPosts(String)}), contado e respondido com 204.</li>
 * </ul>
 *
 * <p>Esperas só pelo próprio Playwright (auto-wait e asserções), nunca {@code Thread.sleep}. Os prints de
 * evidência vão para {@code target/e2e/375}. O navegador vem do Playwright: no CI o passo "Navegador do E2E" do
 * {@code ci.yml} instala o Chromium; numa máquina sem ele o Playwright baixa na primeira execução.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TelasComDados.class, TelasComDados.RotasDoE2EAbertas.class})
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

    /** Senha dos usuários do E2E; só existe no banco do Testcontainers. */
    static final String SENHA = "senha-do-e2e-123";

    /** Dia das reservas reais do E2E: no futuro, porque o E2E usa o relógio de verdade. */
    static final String DIA_REAL = "2027-03-10";

    @LocalServerPort
    int porta;

    @Autowired
    UsuarioService usuarios;

    @Autowired
    UsuarioRepository repositorioDeUsuarios;

    @Autowired
    RecursoService recursos;

    @Autowired
    ReservaService reservas;

    @Autowired
    FormatoDasTelas formato;

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

    /**
     * Entra pela tela de login, como a pessoa faria (D12, opção (a)): o usuário {@code e2e-<perfil>} é criado na
     * primeira vez. Volta com a sessão no contexto do navegador e a página na tela inicial do perfil.
     */
    UsuarioAutenticado entrarComo(PerfilUsuario perfil) {
        String login = "e2e-" + perfil.name().toLowerCase() + "@exemplo.com";
        if (!repositorioDeUsuarios.existsByLogin(login)) {
            usuarios.criar(new UsuarioRequest(nomeDe(perfil), login, SENHA, perfil));
        }
        abrir("/login");
        pagina.getByTestId("login-usuario").fill(login);
        pagina.getByTestId("login-senha").fill(SENHA);
        pagina.waitForNavigation(() -> pagina.getByTestId("login-entrar").click());
        assertThat(pagina.getByTestId("menu-usuario").textContent()).isEqualTo(nomeDe(perfil));
        Usuario usuario = repositorioDeUsuarios.findByLogin(login).orElseThrow();
        return new UsuarioAutenticado(usuario.getId(), login, perfil);
    }

    static String nomeDe(PerfilUsuario perfil) {
        return switch (perfil) {
            case SOLICITANTE -> "Ana Lima";
            case RESPONSAVEL -> "Carlos Souza";
            case ADMINISTRADOR -> "Marta Reis";
        };
    }

    /** Sala gravada no banco, com nome único (o banco do E2E é um só para todas as classes). */
    RecursoResponse salaReal(String nome, boolean restrito) {
        return recursos.criar(new RecursoRequest(TipoRecurso.SALA, nome + " " + UUID.randomUUID().toString()
                .substring(0, 8), 30, restrito));
    }

    /** Reserva gravada pelo service (como se outra tela ou a API tivesse feito), no {@link #DIA_REAL}. */
    ReservaResponse reservaReal(UsuarioAutenticado quem, RecursoResponse recurso, String inicio, String termino) {
        LocalDate dia = LocalDate.parse(DIA_REAL);
        return reservas.criar(List.of(recurso.id()), new Periodo(formato.instante(dia, LocalTime.parse(inicio)),
                formato.instante(dia, LocalTime.parse(termino))), quem);
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
        semRolagemHorizontal(LARGURA);
    }

    void semRolagemHorizontal(int largura) {
        int larguraDoDocumento = ((Number) pagina.evaluate("() => document.documentElement.scrollWidth")).intValue();
        assertThat(larguraDoDocumento).as("largura do documento em %s a %d px", caminhoAtual(), largura)
                .isLessThanOrEqualTo(largura);
    }

    /** Muda a largura da janela só neste teste (os testes de 375 px continuam como estão). Chamar antes de {@link #abrir}. */
    void redimensionar(int largura) {
        pagina.setViewportSize(largura, ALTURA);
    }

    /**
     * Print de evidência da página inteira. Antes de capturar, espera dois quadros de animação: no CI (run
     * 43093990942, 11/11/2026) o Chromium respondeu "Page.captureScreenshot: Unable to capture screenshot" quando o
     * print foi pedido antes de a página ter um quadro desenhado. Se essa falha aparecer mesmo assim, tenta uma vez
     * mais depois de outros dois quadros. O print é evidência: as asserções da tela vêm antes dele e não mudam.
     */
    void print(String nome) {
        var opcoes = new Page.ScreenshotOptions().setPath(PRINTS.resolve(nome + ".png")).setFullPage(true);
        esperarQuadroDesenhado();
        try {
            pagina.screenshot(opcoes);
        } catch (PlaywrightException erro) {
            if (erro.getMessage() == null || !erro.getMessage().contains("Unable to capture screenshot")) {
                throw erro;
            }
            esperarQuadroDesenhado();
            pagina.screenshot(opcoes);
        }
    }

    private void esperarQuadroDesenhado() {
        pagina.evaluate("() => new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r)))");
    }

    String focado() {
        return (String) pagina.evaluate("() => document.activeElement && document.activeElement.getAttribute('data-testid')");
    }
}
