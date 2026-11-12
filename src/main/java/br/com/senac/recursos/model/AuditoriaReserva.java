package br.com.senac.recursos.model;

import br.com.senac.recursos.enums.EstadoReserva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/**
 * Tabela {@code auditoria_reserva} da V1 (RN-08, RN-09). {@link Immutable}: o Hibernate nunca gera UPDATE para ela, e
 * nenhuma rota da API altera ou apaga registro (RN-10). O instante vem do {@code Clock} injetado.
 */
@Entity
@Immutable
@Table(name = "auditoria_reserva")
public class AuditoriaReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "instante", nullable = false)
    private Instant instante;

    /** Nulo só no registro de criação da reserva. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private EstadoReserva estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_novo", nullable = false)
    private EstadoReserva estadoNovo;

    @Column(name = "motivo")
    private String motivo;

    /** Exigido pelo JPA. */
    protected AuditoriaReserva() {
    }

    public AuditoriaReserva(Reserva reserva, Usuario usuario, Instant instante, EstadoReserva estadoAnterior,
            EstadoReserva estadoNovo, String motivo) {
        this.reserva = reserva;
        this.usuario = usuario;
        this.instante = instante;
        this.estadoAnterior = estadoAnterior;
        this.estadoNovo = estadoNovo;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Instant getInstante() {
        return instante;
    }

    public EstadoReserva getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoReserva getEstadoNovo() {
        return estadoNovo;
    }

    public String getMotivo() {
        return motivo;
    }
}
