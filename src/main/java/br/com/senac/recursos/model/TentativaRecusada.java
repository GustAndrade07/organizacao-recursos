package br.com.senac.recursos.model;

import br.com.senac.recursos.converter.PeriodoUserType;
import br.com.senac.recursos.enums.MotivoTentativa;
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
import org.hibernate.annotations.Type;

/**
 * Tabela {@code tentativa_recusada} da V1 (D2): cada pedido recusado por sobreposição, gravado em transação própria
 * para sobreviver ao rollback do pedido. É a fonte dos "conflitos evitados" do relatório (RF-12).
 */
@Entity
@Immutable
@Table(name = "tentativa_recusada")
public class TentativaRecusada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_id", nullable = false)
    private Recurso recurso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @Type(PeriodoUserType.class)
    @Column(name = "periodo_pedido", nullable = false, columnDefinition = "tstzrange")
    private Periodo periodoPedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false)
    private MotivoTentativa motivo;

    @Column(name = "registrada_em", nullable = false)
    private Instant registradaEm;

    /** Exigido pelo JPA. */
    protected TentativaRecusada() {
    }

    public TentativaRecusada(Recurso recurso, Usuario solicitante, Periodo periodoPedido, MotivoTentativa motivo,
            Instant registradaEm) {
        this.recurso = recurso;
        this.solicitante = solicitante;
        this.periodoPedido = periodoPedido;
        this.motivo = motivo;
        this.registradaEm = registradaEm;
    }

    public Long getId() {
        return id;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public Periodo getPeriodoPedido() {
        return periodoPedido;
    }

    public MotivoTentativa getMotivo() {
        return motivo;
    }

    public Instant getRegistradaEm() {
        return registradaEm;
    }
}
