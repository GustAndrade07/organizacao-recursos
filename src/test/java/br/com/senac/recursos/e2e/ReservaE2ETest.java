package br.com.senac.recursos.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

/**
 * Spec 02 do Notion (busca e reserva) em 375 px: o fluxo da busca até o envio da reserva, o bloqueio de término
 * antes do início (RN-01), o aviso de conflito (409), minhas reservas e o detalhe com alterar e cancelar.
 * Gravar, alterar e cancelar são do back-end, que ainda não existe: os {@code POST} são segurados no navegador
 * e o teste confere o que saiu dele (uma vez só e com os campos certos).
 */
class ReservaE2ETest extends E2ETeste {

    private Locator testid(String id) {
        return pagina.getByTestId(id);
    }

    @Test
    void fluxoDaBuscaAteOEnvioDaReservaBloqueiaTerminoAntesDoInicioEEnviaUmaVez() {
        var posts = segurarPosts("**/reservas");
        abrir("/e2e/busca");
        assertThat(testid("resultado-reservar-7")).isVisible();
        print("02-fluxo-1-resultados");

        testid("resultado-reservar-7").click();
        pagina.waitForURL("**/reservas/nova?**");
        assertThat(caminhoAtual()).isEqualTo("/reservas/nova?recursoId=7&data=2026-11-12&inicio=14:00&termino=15:30");
        assertThat(testid("reserva-form-data")).hasValue("2026-11-12");
        assertThat(testid("reserva-form-inicio")).hasValue("14:00");
        assertThat(testid("reserva-form-termino")).hasValue("15:30");

        testid("reserva-form-termino").fill("13:00");
        testid("reserva-form-confirmar").click();
        assertThat(testid("reserva-form-erro-termino")).hasText("O término precisa ser depois do início");
        assertThat(testid("reserva-form-erro-resumo")).isVisible();
        assertThat(testid("reserva-form-termino")).hasAttribute("aria-invalid", "true");
        assertThat(focado()).isEqualTo("reserva-form-erro-resumo");
        assertThat(enviosDeFormulario()).as("RN-01: nada sai do navegador").isZero();
        semRolagemHorizontal();
        print("02-fluxo-2-termino-antes-do-inicio");

        testid("reserva-form-termino").fill("15:30");
        assertThat(testid("reserva-form-erro-resumo")).isHidden();
        pagina.waitForRequest(r -> "POST".equals(r.method()), () -> testid("reserva-form-confirmar").click());
        assertThat(testid("reserva-form-confirmar")).isDisabled();
        assertThat(testid("reserva-form-confirmar")).hasText("Enviando…");
        print("02-fluxo-3-enviando");

        // O contrato é o POST: num clique duplo imediato o Chromium dispara dois "submit" antes de o envio-unico.js
        // travar o botão, mas só uma requisição sai (medido em 11/11/2026; ver docs/frontend/README.md)
        assertThat(enviosDeFormulario()).isBetween(1, 2);
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).postData()).isEqualTo("recursoId=7&data=2026-11-12&inicio=14%3A00&termino=15%3A30");
    }

    @Test
    void conflitoDeHorarioExplicaOMotivoComFocoEOfereceVoltarABusca() {
        abrir("/e2e/reserva-nova/conflito");

        var aviso = testid("reserva-form-erro-conflito");
        assertThat(aviso).isVisible();
        assertThat(aviso).containsText("Esse horário acabou de ser reservado ou se sobrepõe a outra reserva.");
        assertThat(focado()).isEqualTo("reserva-form-erro-conflito");
        aviso.getByTestId("reserva-form-erro-voltar").click();
        pagina.waitForURL("**/recursos/disponiveis?**");
        assertThat(caminhoAtual()).contains("data=2026-11-12").contains("inicio=14:00").contains("termino=15:30");
    }

    @Test
    void recursoRestritoAvisaAntesDeConfirmarQueFicaSolicitada() {
        abrir("/e2e/reserva-nova/restrito");

        assertThat(testid("reserva-form-aviso-restrito")).isVisible();
        assertThat(testid("reserva-form-aviso-restrito")).containsText("Solicitada");
        assertThat(testid("reserva-form-aviso-restrito")).containsText("até um Responsável aprovar");
    }

    @Test
    void minhasReservasMostraCartoesEOfereceAlterarECancelarSoQuandoPode() {
        abrir("/e2e/minhas");

        assertThat(testid("minhas-estado-42")).hasText("Aprovada");
        assertThat(testid("minhas-alterar-42")).isVisible();
        assertThat(testid("minhas-cancelar-43")).isVisible();
        assertThat(testid("minhas-alterar-44")).hasCount(0);
        assertThat(testid("minhas-cancelar-45")).hasCount(0);

        testid("minhas-cancelar-42").click();
        pagina.waitForURL("**/reservas/42?acao=cancelar");
    }

    @Test
    void cancelarAbreConfirmacaoModalComFocoEmVoltarEEscFechaSemCancelar() {
        abrir("/e2e/reserva?acao=cancelar");

        var dialogo = testid("reserva-cancelar-dialogo");
        assertThat(dialogo).isVisible();
        assertThat(dialogo.evaluate("d => d.matches(':modal')")).isEqualTo(true);
        assertThat(focado()).isEqualTo("reserva-cancelar-voltar");
        assertThat(dialogo).containsText("esta ação não pode ser desfeita");
        semRolagemHorizontal();

        pagina.keyboard().press("Escape");
        assertThat(dialogo).isHidden();
        pagina.waitForURL(url("/reservas/42"));
        assertThat(focado()).isEqualTo("reserva-cancelar");
    }

    @Test
    void confirmarCancelamentoEnviaUmaVezMesmoComCliqueDuplo() {
        var posts = segurarPosts("**/reservas/42/cancelar");
        abrir("/e2e/reserva?acao=cancelar");

        pagina.waitForRequest(r -> "POST".equals(r.method()), () -> testid("reserva-cancelar-confirmar").dblclick());

        assertThat(testid("reserva-cancelar-confirmar")).isDisabled();
        // O contrato é o POST: num clique duplo imediato o Chromium dispara dois "submit" antes de o envio-unico.js
        // travar o botão, mas só uma requisição sai (medido em 11/11/2026; ver docs/frontend/README.md)
        assertThat(enviosDeFormulario()).isBetween(1, 2);
        assertThat(posts).hasSize(1);
    }

    @Test
    void alterarBloqueiaTerminoAntesDoInicioEEnviaANovaDataUmaVez() {
        var posts = segurarPosts("**/reservas/42");
        abrir("/e2e/reserva?acao=alterar");
        assertThat(testid("reserva-alterar-data")).hasValue("2026-11-12");

        testid("reserva-alterar-inicio").fill("16:00");
        testid("reserva-alterar-termino").fill("15:00");
        testid("reserva-alterar-confirmar").click();
        assertThat(testid("reserva-alterar-erro-termino")).hasText("O término precisa ser depois do início");
        assertThat(focado()).isEqualTo("reserva-alterar-erro-resumo");
        assertThat(enviosDeFormulario()).isZero();

        testid("reserva-alterar-data").fill("2026-11-13");
        testid("reserva-alterar-termino").fill("17:00");
        pagina.waitForRequest(r -> "POST".equals(r.method()), () -> testid("reserva-alterar-confirmar").click());
        assertThat(testid("reserva-alterar-confirmar")).hasText("Salvando…");
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).postData()).isEqualTo("data=2026-11-13&inicio=16%3A00&termino=17%3A00");
    }
}
