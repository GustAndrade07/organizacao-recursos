package br.com.senac.recursos.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

/**
 * Specs 03, 04 e 05 do Notion em 375 px: diálogos de rejeição e de não compareceu (modal, foco, Esc), envio
 * único da retirada, filtro do relatório e bloqueio por manutenção que barram fim antes do início. Esses são
 * os comportamentos de JavaScript que os testes de servidor não alcançam ({@code docs/frontend/README.md},
 * "Risco aberto" de {@code dialogo.js} e de {@code envio-unico.js}).
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
        var posts = segurarPosts("**/admin/recursos/9/manutencao");
        abrir("/admin/recursos/9/manutencao");

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
}
