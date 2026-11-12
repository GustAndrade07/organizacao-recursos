package br.com.senac.recursos.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.senac.recursos.model.Periodo;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Leitura do texto que o PostgreSQL devolve para um tstzrange, nos fusos e precisões que ele pode imprimir. */
class PeriodoUserTypeTest {

    private static final Instant DEZ_HORAS = Instant.parse("2026-11-10T10:00:00Z");
    private static final Instant ONZE_HORAS = Instant.parse("2026-11-10T11:00:00Z");

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "[\"2026-11-10 10:00:00+00\",\"2026-11-10 11:00:00+00\")",
        "[\"2026-11-10 07:00:00-03\",\"2026-11-10 08:00:00-03\")",
        "[\"2026-11-10 15:30:00+05:30\",\"2026-11-10 16:30:00+05:30\")",
        "[2026-11-10T10:00:00Z,2026-11-10T11:00:00Z)"
    })
    void leOsFormatosQueOServidorImprime(String texto) {
        assertThat(PeriodoUserType.deTexto(texto)).isEqualTo(new Periodo(DEZ_HORAS, ONZE_HORAS));
    }

    @Test
    void leFracaoDeSegundoEDeslocamentoComSegundos() {
        Periodo periodo = PeriodoUserType.deTexto(
                "[\"1900-01-01 09:53:32.123456-03:06:28\",\"1900-01-01 10:53:32.5-03:06:28\")");
        assertThat(periodo.inicio()).isEqualTo(Instant.parse("1900-01-01T13:00:00.123456Z"));
        assertThat(periodo.fim()).isEqualTo(Instant.parse("1900-01-01T14:00:00.500Z"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "(\"2026-11-10 10:00:00+00\",\"2026-11-10 11:00:00+00\")",
        "[\"2026-11-10 10:00:00+00\",\"2026-11-10 11:00:00+00\"]",
        "[\"2026-11-10 10:00:00+00\",)",
        "[,\"2026-11-10 10:00:00+00\")",
        "[-infinity,\"2026-11-10 10:00:00+00\")",
        "empty",
        "[a,b,c)"
    })
    void recusaIntervaloQueAV1NaoGuarda(String texto) {
        assertThatThrownBy(() -> PeriodoUserType.deTexto(texto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void escritaELeituraSaoInversas() {
        PeriodoUserType tipo = new PeriodoUserType();
        Periodo original = new Periodo(DEZ_HORAS, ONZE_HORAS);
        assertThat(tipo.assemble(tipo.disassemble(original), null)).isEqualTo(original);
        assertThat(tipo.disassemble(null)).isNull();
        assertThat(tipo.assemble(null, null)).isNull();
        assertThat(tipo.deepCopy(original)).isSameAs(original);
        assertThat(tipo.isMutable()).isFalse();
        assertThat(tipo.equals(original, new Periodo(DEZ_HORAS, ONZE_HORAS))).isTrue();
        assertThat(tipo.hashCode(original)).isEqualTo(original.hashCode());
        assertThat(tipo.returnedClass()).isEqualTo(Periodo.class);
    }
}
