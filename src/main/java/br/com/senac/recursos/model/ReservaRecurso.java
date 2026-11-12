package br.com.senac.recursos.model;

import br.com.senac.recursos.converter.PeriodoUserType;
import br.com.senac.recursos.enums.EstadoReserva;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

/**
 * Tabela {@code reserva_recurso} da V1: uma linha por recurso ocupado pela reserva. É aqui que vive a proteção contra
 * reserva dupla (constraint {@code reserva_recurso_sem_sobreposicao}, ADR-0007).
 *
 * <p>{@code estado} é cópia do estado da reserva, exigida pela constraint parcial (D5, D6). A aplicação só o escreve
 * na inserção; depois, quem o mantém é a chave estrangeira composta {@code ON UPDATE CASCADE} (regra 8 do AGENTS.md:
 * nunca por coluna mantida à mão). Por isso {@code updatable = false}.
 */
@Entity
@Table(name = "reserva_recurso")
public class ReservaRecurso {

    @EmbeddedId
    private ReservaRecursoId id = new ReservaRecursoId();

    @MapsId("reservaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @MapsId("recursoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_id")
    private Recurso recurso;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, updatable = false)
    private EstadoReserva estado;

    @Type(PeriodoUserType.class)
    @Column(name = "periodo", nullable = false, columnDefinition = "tstzrange")
    private Periodo periodo;

    /** Exigido pelo JPA. */
    protected ReservaRecurso() {
    }

    ReservaRecurso(Reserva reserva, Recurso recurso, Periodo periodo) {
        this.reserva = reserva;
        this.recurso = recurso;
        this.estado = reserva.getEstado();
        this.periodo = periodo;
    }

    void alterarPeriodo(Periodo novo) {
        this.periodo = novo;
    }

    public ReservaRecursoId getId() {
        return id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public Periodo getPeriodo() {
        return periodo;
    }
}
