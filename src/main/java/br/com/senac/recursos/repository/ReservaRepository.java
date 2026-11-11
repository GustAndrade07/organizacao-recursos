package br.com.senac.recursos.repository;

import br.com.senac.recursos.enums.EstadoReserva;
import br.com.senac.recursos.model.Reserva;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @EntityGraph(attributePaths = {"solicitante", "recursos", "recursos.recurso"})
    Optional<Reserva> findComRecursosById(Long id);

    /**
     * Carrega a reserva com {@code SELECT ... FOR UPDATE}: duas mudanças de estado simultâneas na mesma reserva (ex.:
     * dois Responsáveis aprovando) entram em fila, e a segunda já enxerga o estado novo e recebe 409.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> buscarParaAtualizar(@Param("id") Long id);

    @EntityGraph(attributePaths = {"solicitante", "recursos", "recursos.recurso"})
    @Query("""
            select r from Reserva r
            where (:estado is null or r.estado = :estado)
            order by r.id desc""")
    List<Reserva> listar(@Param("estado") EstadoReserva estado);

    @EntityGraph(attributePaths = {"solicitante", "recursos", "recursos.recurso"})
    @Query("""
            select r from Reserva r
            where r.solicitante.id = :solicitanteId
              and (:estado is null or r.estado = :estado)
            order by r.id desc""")
    List<Reserva> listarDoSolicitante(@Param("solicitanteId") Long solicitanteId,
            @Param("estado") EstadoReserva estado);
}
