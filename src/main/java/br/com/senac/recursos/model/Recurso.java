package br.com.senac.recursos.model;

import br.com.senac.recursos.enums.TipoRecurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tabela {@code recurso} da V1: sala, material ou professor (D6). {@code restrito} exige aprovação de um Responsável
 * (RF-07). Localização, patrimônio e competências ainda não existem no esquema (ficam para a V2, ver plano).
 */
@Entity
@Table(name = "recurso")
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoRecurso tipo;

    @Column(name = "nome", nullable = false)
    private String nome;

    /** Opcional; quando existe é positivo (constraint {@code recurso_capacidade_positiva}). */
    @Column(name = "capacidade")
    private Integer capacidade;

    @Column(name = "restrito", nullable = false)
    private boolean restrito;

    /** Exigido pelo JPA. */
    protected Recurso() {
    }

    public Recurso(TipoRecurso tipo, String nome, Integer capacidade, boolean restrito) {
        this.tipo = tipo;
        atualizar(nome, capacidade, restrito);
    }

    /** O tipo não muda depois do cadastro: trocar sala por professor mudaria o sentido das reservas existentes. */
    public final void atualizar(String nome, Integer capacidade, boolean restrito) {
        this.nome = nome;
        this.capacidade = capacidade;
        this.restrito = restrito;
    }

    public Long getId() {
        return id;
    }

    public TipoRecurso getTipo() {
        return tipo;
    }

    public String getNome() {
        return nome;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public boolean isRestrito() {
        return restrito;
    }
}
