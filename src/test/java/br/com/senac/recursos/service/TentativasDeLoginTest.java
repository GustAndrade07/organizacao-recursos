package br.com.senac.recursos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.senac.recursos.support.RelogioDeTeste;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Regras do contador do login (telas e API), sem servidor: janela de 15 minutos, bloqueio que não estende, login com
 * espaços e maiúsculas, limpeza da memória e valores de configuração. O caminho completo (POST /login com CSRF e
 * cookie) está em {@code LimiteDeTentativasDoLoginIntegracaoTest}.
 */
class TentativasDeLoginTest {

    private static final Duration QUINZE_MINUTOS = Duration.ofMinutes(15);

    private final RelogioDeTeste relogio = new RelogioDeTeste(Instant.parse("2026-11-01T12:00:00Z"));
    private final TentativasDeLogin tentativas = new TentativasDeLogin(relogio, 5, QUINZE_MINUTOS);

    @Test
    void quintaFalhaBloqueiaEAQuartaNao() {
        falhar("ana", 4);
        assertThat(tentativas.bloqueado("ana")).isFalse();

        falhar("ana", 1);
        assertThat(tentativas.bloqueado("ana")).isTrue();
    }

    @Test
    void bloqueioTerminaQuinzeMinutosDepoisDaQuintaFalhaENaoEstendeComTentativas() {
        falhar("ana", 5);

        relogio.avancar(Duration.ofMinutes(10));
        falhar("ana", 3);
        relogio.avancar(Duration.ofMinutes(5).minusNanos(1));
        assertThat(tentativas.bloqueado("ana")).isTrue();

        relogio.avancar(Duration.ofNanos(1));
        assertThat(tentativas.bloqueado("ana")).isFalse();
    }

    @Test
    void depoisDoBloqueioOContadorRecomecaDoZero() {
        falhar("ana", 5);
        relogio.avancar(QUINZE_MINUTOS);

        falhar("ana", 4);

        assertThat(tentativas.bloqueado("ana")).isFalse();
    }

    @Test
    void falhasForaDaJanelaDeslizanteNaoContam() {
        falhar("ana", 3);
        relogio.avancar(Duration.ofMinutes(10));
        falhar("ana", 1);
        relogio.avancar(Duration.ofMinutes(5));

        // as 3 primeiras completaram 15 minutos; sobra 1 + esta
        falhar("ana", 1);

        assertThat(tentativas.bloqueado("ana")).isFalse();
        falhar("ana", 3);
        assertThat(tentativas.bloqueado("ana")).isTrue();
    }

    @Test
    void entrarZeraOContador() {
        falhar("ana", 4);
        tentativas.entrou("ana");
        falhar("ana", 4);

        assertThat(tentativas.bloqueado("ana")).isFalse();
    }

    @Test
    void mesmoLoginComEspacosEMaiusculasContaJunto() {
        falhar("ana", 2);
        falhar("  ANA ", 2);
        falhar("Ana", 1);

        assertThat(tentativas.bloqueado("ana")).isTrue();
        assertThat(tentativas.bloqueado(" aNa")).isTrue();
        assertThat(tentativas.bloqueado("bia")).isFalse();
    }

    @Test
    void loginNuloContaComoVazio() {
        falhar(null, 5);

        assertThat(tentativas.bloqueado(null)).isTrue();
        assertThat(tentativas.bloqueado("")).isTrue();
    }

    /** Texto enorme não vira chave enorme: passa de 254 caracteres nenhum e-mail é válido, então todos dividem um balde. */
    @Test
    void chaveDeTextoEnormeEhLimitadaEOMapaNaoCresceComEle() {
        for (int i = 0; i < 5; i++) {
            tentativas.falhou("a".repeat(300) + i);
        }

        assertThat(tentativas.registrosNaMemoria()).isEqualTo(1);
        assertThat(tentativas.bloqueado("a".repeat(300) + "outro")).isTrue();
        assertThat(tentativas.bloqueado("a".repeat(100))).isFalse();
    }

    @Test
    void registrosVencidosSaemDaMemoriaQuandoOMapaPassaDoLimite() {
        for (int i = 0; i <= TentativasDeLogin.LIMPAR_ACIMA_DE; i++) {
            tentativas.falhou("inventado." + i);
        }
        falhar("bloqueado", 5);
        relogio.avancar(QUINZE_MINUTOS);

        tentativas.falhou("depois");

        // o que venceu saiu (inclusive o bloqueio que terminou); só fica a falha nova
        assertThat(tentativas.registrosNaMemoria()).isEqualTo(1);
    }

    @Test
    void enquantoNaoPassaDoLimiteNadaEApagado() {
        falhar("ana", 1);
        relogio.avancar(QUINZE_MINUTOS);

        tentativas.falhou("bia");

        assertThat(tentativas.registrosNaMemoria()).isEqualTo(2);
    }

    @Test
    void configuracaoInvalidaNaoSobe() {
        assertThatThrownBy(() -> new TentativasDeLogin(relogio, 0, QUINZE_MINUTOS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TentativasDeLogin(relogio, 5, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TentativasDeLogin(relogio, 5, Duration.ofMinutes(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TentativasDeLogin(relogio, 5, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void falhar(String login, int vezes) {
        for (int i = 0; i < vezes; i++) {
            tentativas.falhou(login);
        }
    }
}
