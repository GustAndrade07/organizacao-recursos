package br.com.senac.recursos.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.dto.response.RecursoResponse;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.enums.PerfilUsuario;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Spec 02 do Notion (busca e reserva) em 375 px. Contra o back-end real (login pela tela, banco do Testcontainers):
 * o fluxo da busca até a reserva gravada, com o bloqueio de término antes do início (RN-01) e um só POST; o 409 de
 * horário ocupado (RN-02, RN-03) com o aviso e o foco; minhas reservas e o cancelamento. Com dados de exemplo
 * ({@code /e2e/...}): o aviso de recurso restrito, o diálogo de cancelar (modal, foco, Esc) e o envio único no clique
 * duplo, com o {@code POST} segurado no navegador.
 */
class ReservaE2ETest extends E2ETeste {

    private Locator testid(String id) {
        return pagina.getByTestId(id);
    }

    @Test
    void fluxoRealDaBuscaAteAReservaGravadaBloqueiaTerminoAntesDoInicioEEnviaUmaVez() {
        RecursoResponse sala = salaReal("Sala do fluxo", false);
        entrarComo(PerfilUsuario.SOLICITANTE);
        List<String> posts = new ArrayList<>();
        pagina.onRequest(pedido -> {
            if ("POST".equals(pedido.method()) && pedido.url().endsWith("/reservas")) {
                posts.add(pedido.postData());
            }
        });

        abrir("/recursos/disponiveis?data=" + DIA_REAL + "&inicio=14:00&termino=15:30");
        var reservar = testid("resultado-reservar-" + sala.id());
        assertThat(reservar).isVisible();
        semRolagemHorizontal();
        print("02-fluxo-1-resultados");

        reservar.click();
        pagina.waitForURL("**/reservas/nova?**");
        assertThat(caminhoAtual()).isEqualTo("/reservas/nova?recursoId=" + sala.id() + "&data=" + DIA_REAL
                + "&inicio=14:00&termino=15:30");
        assertThat(testid("reserva-form-data")).hasValue(DIA_REAL);
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
        pagina.waitForNavigation(() -> testid("reserva-form-confirmar").click());
        pagina.waitForURL("**/reservas/*");

        assertThat(testid("reserva-sucesso")).containsText("Reserva confirmada");
        assertThat(testid("reserva-recurso")).hasText(sala.nome());
        assertThat(pagina.locator("main")).containsText("14:00 às 15:30");
        semRolagemHorizontal();
        print("02-fluxo-3-reserva-confirmada");
        assertThat(posts).hasSize(1);
        // A cadeia das telas tem CSRF: o formulário manda o token antes dos campos
        assertThat(posts.get(0)).startsWith("_csrf=")
                .endsWith("&recursoId=" + sala.id() + "&data=" + DIA_REAL + "&inicio=14%3A00&termino=15%3A30");
    }

    @Test
    void conflitoRealDeHorarioExplicaOMotivoComFocoEOfereceVoltarABusca() {
        RecursoResponse sala = salaReal("Sala disputada", false);
        UsuarioAutenticado outro = entrarComo(PerfilUsuario.RESPONSAVEL);
        reservaReal(outro, sala, "14:00", "15:30");
        contexto.clearCookies();
        entrarComo(PerfilUsuario.SOLICITANTE);

        abrir("/reservas/nova?recursoId=" + sala.id() + "&data=" + DIA_REAL + "&inicio=14:30&termino=15:00");
        pagina.waitForNavigation(() -> testid("reserva-form-confirmar").click());
        pagina.waitForURL("**/reservas");

        var aviso = testid("reserva-form-erro-conflito");
        assertThat(aviso).isVisible();
        assertThat(aviso).containsText("Esse horário acabou de ser reservado ou se sobrepõe a outra reserva.");
        assertThat(focado()).isEqualTo("reserva-form-erro-conflito");
        semRolagemHorizontal();
        print("02-conflito-real");
        aviso.getByTestId("reserva-form-erro-voltar").click();
        pagina.waitForURL("**/recursos/disponiveis?**");
        assertThat(caminhoAtual()).contains("data=" + DIA_REAL).contains("inicio=14:30").contains("termino=15:00");
    }

    @Test
    void recursoRestritoAvisaAntesDeConfirmarQueFicaSolicitada() {
        abrir("/e2e/reserva-nova/restrito");

        assertThat(testid("reserva-form-aviso-restrito")).isVisible();
        assertThat(testid("reserva-form-aviso-restrito")).containsText("Solicitada");
        assertThat(testid("reserva-form-aviso-restrito")).containsText("até um Responsável aprovar");
    }

    @Test
    void minhasReservasReaisOferecemCancelarECancelarMudaOEstado() {
        RecursoResponse sala = salaReal("Sala minha", false);
        UsuarioAutenticado ana = entrarComo(PerfilUsuario.SOLICITANTE);
        ReservaResponse reserva = reservaReal(ana, sala, "08:00", "09:00");

        abrir("/reservas/minhas");
        assertThat(testid("minhas-estado-" + reserva.id())).hasText("Aprovada");
        assertThat(testid("minhas-alterar-" + reserva.id())).isVisible();
        semRolagemHorizontal();

        testid("minhas-cancelar-" + reserva.id()).click();
        pagina.waitForURL("**/reservas/" + reserva.id() + "?acao=cancelar");
        assertThat(testid("reserva-cancelar-dialogo")).isVisible();
        pagina.waitForNavigation(() -> testid("reserva-cancelar-confirmar").click());
        pagina.waitForURL(url("/reservas/" + reserva.id()));

        assertThat(testid("reserva-sucesso")).containsText("Reserva cancelada");
        assertThat(testid("reserva-sem-acao")).isVisible();
        print("02-cancelada-real");
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
        assertThat(posts.get(0).postData()).startsWith("_csrf=")
                .endsWith("&data=2026-11-13&inicio=16%3A00&termino=17%3A00");
    }
}
