package br.com.senac.recursos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Chave primária composta de {@code reserva_recurso}: {@code (reserva_id, recurso_id)}. */
@Embeddable
public class ReservaRecursoId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "reserva_id")
    private Long reservaId;

    @Column(name = "recurso_id")
    private Long recursoId;

    /** Exigido pelo JPA; os valores chegam pelo {@code @MapsId} de {@link ReservaRecurso}. */
    protected ReservaRecursoId() {
    }

    public Long getReservaId() {
        return reservaId;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    @Override
    public boolean equals(Object outro) {
        return this == outro || (outro instanceof ReservaRecursoId o
                && Objects.equals(reservaId, o.reservaId) && Objects.equals(recursoId, o.recursoId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservaId, recursoId);
    }
}
