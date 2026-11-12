package br.com.senac.recursos.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Máquina de estados da reserva (RN-05):
 *
 * <pre>
 * SOLICITADA ──► APROVADA ──► EM_USO ──► CONCLUIDA
 *     │             │
 *     ├► REJEITADA  ├► CANCELADA
 *     └► CANCELADA  └► NAO_COMPARECEU
 * </pre>
 *
 * <p>{@link #ocupaHorario()} segue a D5: só SOLICITADA, APROVADA, EM_USO e CONCLUIDA ocupam o horário (são os estados
 * do predicado da constraint {@code reserva_recurso_sem_sobreposicao}); os demais liberam. Estados finais não têm
 * saída. Transição fora deste mapa responde 409 (D3).
 */
public enum EstadoReserva {

    SOLICITADA(true),
    APROVADA(true),
    EM_USO(true),
    CONCLUIDA(true),
    REJEITADA(false),
    CANCELADA(false),
    NAO_COMPARECEU(false);

    private final boolean ocupaHorario;

    EstadoReserva(boolean ocupaHorario) {
        this.ocupaHorario = ocupaHorario;
    }

    /** D5: estado que conta na constraint de exclusão e no relatório de utilização. */
    public boolean ocupaHorario() {
        return ocupaHorario;
    }

    /** Estados para os quais esta reserva pode ir; vazio nos estados finais. */
    public Set<EstadoReserva> proximosPermitidos() {
        Set<EstadoReserva> proximos = switch (this) {
            case SOLICITADA -> EnumSet.of(APROVADA, REJEITADA, CANCELADA);
            case APROVADA -> EnumSet.of(EM_USO, CANCELADA, NAO_COMPARECEU);
            case EM_USO -> EnumSet.of(CONCLUIDA);
            case CONCLUIDA, REJEITADA, CANCELADA, NAO_COMPARECEU -> EnumSet.noneOf(EstadoReserva.class);
        };
        return Collections.unmodifiableSet(proximos);
    }

    public boolean podeIrPara(EstadoReserva destino) {
        return proximosPermitidos().contains(destino);
    }

    public boolean finalizado() {
        return proximosPermitidos().isEmpty();
    }
}
