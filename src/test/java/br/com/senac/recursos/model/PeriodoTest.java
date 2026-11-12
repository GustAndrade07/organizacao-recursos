package br.com.senac.recursos.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.senac.recursos.exception.RequisicaoInvalidaException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Intervalo semiaberto [início, fim) (D1) e RN-01 (término depois do início). */
class PeriodoTest {

    private static Periodo periodo(String inicio, String fim) {
        return new Periodo(Instant.parse("2026-11-10T" + inicio + "Z"), Instant.parse("2026-11-10T" + fim + "Z"));
    }

    @Test
    void terminoIgualAoInicioFereRn01() {
        assertThatThrownBy(() -> periodo("10:00:00", "10:00:00"))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("O término precisa ser depois do início.");
    }

    @Test
    void terminoAntesDoInicioFereRn01() {
        assertThatThrownBy(() -> periodo("10:00:01", "10:00:00")).isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    void umMicrossegundoDeDuracaoEhValido() {
        assertThat(new Periodo(Instant.parse("2026-11-10T10:00:00Z"), Instant.parse("2026-11-10T10:00:00.000001Z"))
                .duracao()).isEqualTo(Duration.ofNanos(1000));
    }

    @Test
    void datasAusentesSaoRecusadas() {
        Instant agora = Instant.parse("2026-11-10T10:00:00Z");
        assertThatThrownBy(() -> new Periodo(null, agora)).isInstanceOf(RequisicaoInvalidaException.class);
        assertThatThrownBy(() -> new Periodo(agora, null)).isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    void nanossegundosSaoTruncadosParaAPrecisaoDoBanco() {
        Periodo p = new Periodo(Instant.parse("2026-11-10T10:00:00.0000019Z"), Instant.parse("2026-11-10T11:00:00Z"));
        assertThat(p.inicio()).isEqualTo(Instant.parse("2026-11-10T10:00:00.000001Z"));
    }

    /** Manutenção 10:00 a 12:00 (tabela de fronteiras de docs/testes/bdd-aprovacao-bloqueio.md, trinca -1, 0, +1). */
    @ParameterizedTest(name = "{0} a {1} sobrepõe 10:00-12:00? {2}")
    @CsvSource({
        "09:00:00, 10:00:00, false",
        "12:00:00, 13:00:00, false",
        "09:00:00, 10:00:01, true",
        "11:59:59, 13:00:00, true",
        "10:30:00, 11:30:00, true",
        "08:00:00, 14:00:00, true"
    })
    void fronteirasDoIntervaloSemiaberto(String inicio, String fim, boolean sobrepoe) {
        Periodo manutencao = periodo("10:00:00", "12:00:00");
        assertThat(periodo(inicio, fim).sobrepoe(manutencao)).isEqualTo(sobrepoe);
        assertThat(manutencao.sobrepoe(periodo(inicio, fim))).isEqualTo(sobrepoe);
    }

    @Test
    void iniciadoAPartirDoInstanteDeInicio() {
        Periodo p = periodo("10:00:00", "11:00:00");
        assertThat(p.iniciadoEm(Instant.parse("2026-11-10T09:59:59Z"))).isFalse();
        assertThat(p.iniciadoEm(Instant.parse("2026-11-10T10:00:00Z"))).isTrue();
    }

    @Test
    void literalDoTstzrange() {
        assertThat(periodo("10:00:00", "11:00:00").comoTstzrange())
                .isEqualTo("[2026-11-10T10:00:00Z,2026-11-10T11:00:00Z)");
    }
}
