package br.com.senac.recursos.enums;

import static br.com.senac.recursos.enums.EstadoReserva.APROVADA;
import static br.com.senac.recursos.enums.EstadoReserva.CANCELADA;
import static br.com.senac.recursos.enums.EstadoReserva.CONCLUIDA;
import static br.com.senac.recursos.enums.EstadoReserva.EM_USO;
import static br.com.senac.recursos.enums.EstadoReserva.NAO_COMPARECEU;
import static br.com.senac.recursos.enums.EstadoReserva.REJEITADA;
import static br.com.senac.recursos.enums.EstadoReserva.SOLICITADA;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Máquina de estados (RN-05) e estados que ocupam o horário (D5), conferidos par a par. */
class EstadoReservaTest {

    /** Oráculo escrito à mão a partir do RN-05; não é copiado da implementação. */
    private static final Map<EstadoReserva, Set<EstadoReserva>> PERMITIDAS = Map.of(
            SOLICITADA, Set.of(APROVADA, REJEITADA, CANCELADA),
            APROVADA, Set.of(EM_USO, CANCELADA, NAO_COMPARECEU),
            EM_USO, Set.of(CONCLUIDA),
            CONCLUIDA, Set.of(),
            REJEITADA, Set.of(),
            CANCELADA, Set.of(),
            NAO_COMPARECEU, Set.of());

    static Stream<Arguments> todosOsPares() {
        return Stream.of(EstadoReserva.values()).flatMap(de -> Stream.of(EstadoReserva.values())
                .map(para -> Arguments.of(de, para, PERMITIDAS.get(de).contains(para))));
    }

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @MethodSource("todosOsPares")
    void cadaTransicaoSegueORn05(EstadoReserva de, EstadoReserva para, boolean permitida) {
        assertThat(de.podeIrPara(para)).isEqualTo(permitida);
    }

    @Test
    void concluidaNaoVoltaParaAprovada() {
        assertThat(CONCLUIDA.podeIrPara(APROVADA)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoReserva.class, names = {"SOLICITADA", "APROVADA", "EM_USO", "CONCLUIDA"})
    void estadosQueOcupamOHorarioSaoOsDaD5(EstadoReserva estado) {
        assertThat(estado.ocupaHorario()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoReserva.class, names = {"REJEITADA", "CANCELADA", "NAO_COMPARECEU"})
    void estadosQueLiberamOHorarioNaoOcupam(EstadoReserva estado) {
        assertThat(estado.ocupaHorario()).isFalse();
        assertThat(estado.finalizado()).isTrue();
    }

    @Test
    void conjuntoDePermitidasNaoPodeSerAlteradoPorQuemLe() {
        Set<EstadoReserva> proximos = SOLICITADA.proximosPermitidos();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> proximos.add(CONCLUIDA))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
