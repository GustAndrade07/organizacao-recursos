package br.com.senac.recursos.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.dto.response.RecursoResponse;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.enums.PerfilUsuario;
import org.junit.jupiter.api.Test;

/**
 * Specs 03, 04 e 05 do Notion em 375 px: diálogos de rejeição e de não compareceu (modal, foco, Esc), envio
 * único da retirada, filtro do relatório e bloqueio por manutenção que barram fim antes do início. Esses são
 * os comportamentos de JavaScript que os testes de servidor não alcançam ({@code docs/frontend/README.md},
 * "Risco aberto" de {@code dialogo.js} e de {@code envio-unico.js}). Contra o back-end real: o pedido de recurso
 * restrito chega à fila do Responsável, é aprovado pela tela e o Solicitante vê o novo estado; o relatório e o
 * bloqueio entram pelo login de cada perfil.
 */
class ResponsavelEAdministradorE2ETest extends E2ETeste {

    private Locator testid(String id) {
        return pagina.getByTestId(id);
    }

    @Test
    void rejeitarAbreDialogoModalComFocoNoMotivoEEscVoltaAFila() {
        abrir("/e2e/aprovacoes?rejeitar=1");

        var dialogo = pagina.locator("dialog");
        assertThat(dialogo).isVisible();
        assertThat(dialogo.evaluate("d => d.matches(':modal')")).isEqualTo(true);
        assertThat(focado()).isEqualTo("aprovacao-motivo");

        pagina.keyboard().press("Escape");
        assertThat(dialogo).isHidden();
        pagina.waitForURL(url("/aprovacoes"));
        assertThat(focado()).isEqualTo("aprovacao-rejeitar-1");
    }

    @Test
    void rejeitarSemMotivoNaoEnvia() {
        var posts = segurarPosts("**/aprovacoes/1/rejeitar");
        abrir("/e2e/aprovacoes?rejeitar=1");

        testid("aprovacao-rejeitar-confirmar").click();

        assertThat(enviosDeFormulario()).isZero();
        assertThat(posts).isEmpty();
        assertThat(testid("aprovacao-motivo").evaluate("e => e.validity.valueMissing")).isEqualTo(true);
    }

    @Test
    void pedidoDoProprioResponsavelNaoTemAprovarNemRejeitar() {
        abrir("/e2e/aprovacoes");

        assertThat(testid("aprovacao-aprovar-1")).isVisible();
        assertThat(testid("aprovacao-aprovar-2")).hasCount(0);
        assertThat(testid("aprovacao-rejeitar-2")).hasCount(0);
    }

    @Test
    void naoCompareceuAbreConfirmacaoModalComFocoEmVoltarEEscMantemOFiltro() {
        abrir("/e2e/retirada-devolucao?naoCompareceu=4");

        var dialogo = testid("retirada-devolucao-nc-dialogo");
        assertThat(dialogo).isVisible();
        assertThat(dialogo.evaluate("d => d.matches(':modal')")).isEqualTo(true);
        assertThat(focado()).isEqualTo("retirada-devolucao-nc-voltar");

        pagina.keyboard().press("Escape");
        assertThat(dialogo).isHidden();
        pagina.waitForURL(url("/retirada-devolucao?estado=ativas"));
        assertThat(focado()).isEqualTo("retirada-devolucao-nao-compareceu-4");
    }

    @Test
    void registrarRetiradaEnviaUmaVezMesmoComCliqueDuplo() {
        var posts = segurarPosts("**/retirada-devolucao/**");
        abrir("/e2e/retirada-devolucao");
        var retirar = testid("retirada-devolucao-retirar-2");

        pagina.waitForRequest(r -> "POST".equals(r.method()), retirar::dblclick);

        assertThat(retirar).isDisabled();
        assertThat(retirar.locator("xpath=ancestor::form")).hasAttribute("aria-busy", "true");
        // O contrato é o POST: num clique duplo imediato o Chromium dispara dois "submit" antes de o envio-unico.js
        // travar o botão, mas só uma requisição sai (medido em 11/11/2026; ver docs/frontend/README.md)
        assertThat(enviosDeFormulario()).isBetween(1, 2);
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).url()).endsWith("/retirada-devolucao/2/retirar");
    }

    @Test
    void relatorioBarraFimAntesDoInicioNoNavegadorEDepoisFiltraPeloServidor() {
        entrarComo(PerfilUsuario.RESPONSAVEL);
        abrir("/relatorios/utilizacao");

        testid("relatorio-inicio").fill("2026-11-30");
        testid("relatorio-fim").fill("2026-11-01");
        testid("relatorio-gerar").click();
        assertThat(testid("relatorio-fim-erro")).hasText("A data de fim não pode ser anterior à data de início.");
        assertThat(testid("relatorio-fim")).hasAttribute("aria-invalid", "true");
        assertThat(focado()).isEqualTo("relatorio-fim");
        assertThat(caminhoAtual()).isEqualTo("/relatorios/utilizacao");
        print("04-relatorio-fim-antes-do-inicio");

        testid("relatorio-fim").fill("2026-11-30");
        testid("relatorio-inicio").fill("2026-11-01");
        testid("relatorio-gerar").click();
        pagina.waitForURL("**/relatorios/utilizacao?**");
        assertThat(caminhoAtual()).isEqualTo("/relatorios/utilizacao?inicio=2026-11-01&fim=2026-11-30");
        assertThat(testid("relatorio-fim-erro")).isHidden();
    }

    @Test
    void bloqueioPorManutencaoBarraFimIgualOuAntesDoInicio() {
        RecursoResponse sala = salaReal("Sala em obra", false);
        entrarComo(PerfilUsuario.ADMINISTRADOR);
        var posts = segurarPosts("**/admin/recursos/" + sala.id() + "/manutencao");
        abrir("/admin/recursos/" + sala.id() + "/manutencao");

        testid("manutencao-inicio").fill("2026-11-20T10:00");
        testid("manutencao-fim").fill("2026-11-20T10:00");
        testid("manutencao-motivo").fill("Troca do piso");
        testid("manutencao-bloquear").click();

        assertThat(testid("manutencao-fim-erro")).hasText("O fim do bloqueio precisa ser depois do início.");
        assertThat(focado()).isEqualTo("manutencao-fim");
        assertThat(enviosDeFormulario()).isZero();
        assertThat(posts).isEmpty();
        print("05-manutencao-fim-igual-ao-inicio");
    }

    @Test
    void historicoMostraALinhaDoTempoSemAcaoDeEditar() {
        abrir("/e2e/historico");

        assertThat(pagina.locator("[data-testid^=historico-evento-]").filter(
                new Locator.FilterOptions().setHasText("Carlos Souza"))).not().hasCount(0);
        assertThat(testid("historico-somente-leitura")).isVisible();
        assertThat(pagina.locator("main form, main button")).hasCount(0);
    }

    @Test
    void pedidoRealDeRecursoRestritoEhAprovadoNaFilaEOSolicitanteVeAprovada() {
        RecursoResponse laboratorio = salaReal("Laboratório restrito", true);
        UsuarioAutenticado ana = entrarComo(PerfilUsuario.SOLICITANTE);
        ReservaResponse pedido = reservaReal(ana, laboratorio, "10:00", "11:00");
        contexto.clearCookies();

        entrarComo(PerfilUsuario.RESPONSAVEL);
        abrir("/aprovacoes");
        var linha = testid("aprovacao-linha-" + pedido.id());
        assertThat(linha).containsText(laboratorio.nome());
        assertThat(linha).containsText("Ana Lima");
        semRolagemHorizontal();
        print("03-fila-real");
        pagina.waitForNavigation(() -> testid("aprovacao-aprovar-" + pedido.id()).click());
        pagina.waitForURL("**/aprovacoes");
        assertThat(pagina.getByRole(com.microsoft.playwright.options.AriaRole.STATUS)).containsText("Aprovada");
        assertThat(testid("aprovacao-linha-" + pedido.id())).hasCount(0);
        contexto.clearCookies();

        entrarComo(PerfilUsuario.SOLICITANTE);
        abrir("/reservas/" + pedido.id());
        assertThat(testid("reserva-estado")).hasText("Aprovada");
    }

    @Test
    void administradorCadastraUsuarioPelaTelaEOUsuarioNovoEntra() {
        entrarComo(PerfilUsuario.ADMINISTRADOR);
        String login = "e2e-novo-" + java.util.UUID.randomUUID().toString().substring(0, 6) + "@exemplo.com";

        abrir("/admin/usuarios/novo");
        testid("usuario-form-nome").fill("Pessoa Nova");
        testid("usuario-form-login").fill(login);
        testid("usuario-form-senha").fill("curta");
        pagina.waitForNavigation(() -> testid("usuario-form-salvar").click());
        assertThat(testid("usuario-form-senha-erro")).hasText("A senha deve ter de 8 a 72 caracteres.");
        assertThat(focado()).isEqualTo("usuario-form-erros");
        assertThat(testid("usuario-form-senha")).hasValue("");
        semRolagemHorizontal();
        print("05-usuario-erro");

        testid("usuario-form-senha").fill("senha-nova-123");
        testid("usuario-form-perfil").selectOption("RESPONSAVEL");
        pagina.waitForNavigation(() -> testid("usuario-form-salvar").click());
        assertThat(testid("usuarios-sucesso")).containsText("Pessoa Nova já pode entrar com o e-mail " + login);
        semRolagemHorizontal();
        print("05-usuarios-lista");
        contexto.clearCookies();

        abrir("/login");
        testid("login-usuario").fill(login);
        testid("login-senha").fill("senha-nova-123");
        pagina.waitForNavigation(() -> testid("login-entrar").click());
        assertThat(testid("menu-usuario")).hasText("Pessoa Nova");
        assertThat(testid("menu-perfil")).hasText("Responsável");
    }

    @Test
    void responsavelAbreOHistoricoPelaRetiradaEDevolucao() {
        RecursoResponse sala = salaReal("Sala com histórico", false);
        UsuarioAutenticado ana = entrarComo(PerfilUsuario.SOLICITANTE);
        ReservaResponse reserva = reservaReal(ana, sala, "16:00", "17:00");
        contexto.clearCookies();
        entrarComo(PerfilUsuario.RESPONSAVEL);

        abrir("/retirada-devolucao");
        pagina.waitForNavigation(() -> testid("retirada-devolucao-historico-" + reserva.id()).click());

        assertThat(caminhoAtual()).isEqualTo("/reservas/" + reserva.id() + "/historico");
        assertThat(testid("historico-evento-1-para")).hasText("Aprovada");
        semRolagemHorizontal();
    }
}
