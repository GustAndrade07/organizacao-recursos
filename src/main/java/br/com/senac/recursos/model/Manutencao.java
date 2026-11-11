package br.com.senac.recursos.model;

import br.com.senac.recursos.converter.PeriodoUserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

/** Tabela {@code manutencao} da V1: o recurso fica bloqueado no período (RN-04, RF-08). */
@Entity
@Table(name = "manutencao")
public class Manutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_id", nullable = false)
    private Recurso recurso;

    @Type(PeriodoUserType.class)
    @Column(name = "periodo", nullable = false, columnDefinition = "tstzrange")
    private Periodo periodo;

    /** Exigido pelo JPA. */
    protected Manutencao() {
    }

    public Manutencao(Recurso recurso, Periodo periodo) {
        this.recurso = recurso;
        this.periodo = periodo;
    }

    public Long getId() {
        return id;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public Periodo getPeriodo() {
        return periodo;
    }
}
