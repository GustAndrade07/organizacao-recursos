package br.com.senac.recursos.model;

import br.com.senac.recursos.enums.EstadoReserva;
import br.com.senac.recursos.exception.TransicaoInvalidaException;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Agregado da reserva: a linha de {@code reserva} e uma linha de {@code reserva_recurso} por recurso ocupado
 * (sala, material e professor, D6). Todas as linhas têm o mesmo período.
 *
 * <p>Regras que vivem aqui, e não no serviço:
 * <ul>
 *   <li>as linhas são criadas em ordem crescente de {@code recurso_id} (ADR-0007), para duas reservas com recursos em
 *       comum não se travarem em ordem oposta;</li>
 *   <li>toda mudança de estado passa por {@link #mudarEstado}, que aplica a máquina de estados (RN-05). O estado das
 *       linhas de {@code reserva_recurso} acompanha sozinho, pela chave estrangeira composta {@code ON UPDATE CASCADE}
 *       da V1.</li>
 * </ul>
 */
@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoReserva estado;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.PERSIST)
    @OrderBy("id.recursoId ASC")
    private List<ReservaRecurso> recursos = new ArrayList<>();

    /** Exigido pelo JPA. */
    protected Reserva() {
    }

    private Reserva(Usuario solicitante, EstadoReserva estadoInicial, Instant criadaEm) {
        this.solicitante = solicitante;
        this.estado = estadoInicial;
        this.criadaEm = criadaEm;
    }

    /**
     * Monta uma reserva nova, ainda não gravada.
     *
     * @param recursos recursos distintos e já carregados; a ordem de entrada não importa
     */
    public static Reserva nova(Usuario solicitante, Collection<Recurso> recursos, Periodo periodo,
            EstadoReserva estadoInicial, Instant criadaEm) {
        Reserva reserva = new Reserva(solicitante, estadoInicial, criadaEm);
        recursos.stream()
                .sorted(Comparator.comparing(Recurso::getId))
                .forEach(recurso -> reserva.recursos.add(new ReservaRecurso(reserva, recurso, periodo)));
        return reserva;
    }

    /** Aplica a máquina de estados (RN-05); transição fora do mapa vira 409 (D3). */
    public void mudarEstado(EstadoReserva destino) {
        if (!estado.podeIrPara(destino)) {
            throw new TransicaoInvalidaException(estado, destino);
        }
        this.estado = destino;
    }

    /** Troca o período de todas as linhas (RF-04). A constraint de exclusão confere a sobreposição no flush. */
    public void alterarPeriodo(Periodo novo) {
        recursos.forEach(linha -> linha.alterarPeriodo(novo));
    }

    public Periodo getPeriodo() {
        return recursos.get(0).getPeriodo();
    }

    public boolean pertenceA(Long usuarioId) {
        return solicitante.getId().equals(usuarioId);
    }

    public boolean temRecursoRestrito() {
        return recursos.stream().anyMatch(linha -> linha.getRecurso().isRestrito());
    }

    public List<Long> recursoIds() {
        return recursos.stream().map(linha -> linha.getRecurso().getId()).toList();
    }

    public Long getId() {
        return id;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public List<ReservaRecurso> getRecursos() {
        return Collections.unmodifiableList(recursos);
    }
}
