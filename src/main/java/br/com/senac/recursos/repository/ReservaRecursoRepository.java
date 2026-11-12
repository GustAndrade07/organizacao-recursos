package br.com.senac.recursos.repository;

import br.com.senac.recursos.model.ReservaRecurso;
import br.com.senac.recursos.model.ReservaRecursoId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Só leitura: as linhas de {@code reserva_recurso} nascem e mudam pelo agregado {@code Reserva}. */
public interface ReservaRecursoRepository extends Repository<ReservaRecurso, ReservaRecursoId> {

    /**
     * Depois que o banco recusou um pedido (23P01), descobre quais recursos pedidos estão ocupados no período, para
     * gravar a tentativa recusada de cada um (D2).
     *
     * @param ignorarReservaId reserva que está sendo alterada (as próprias linhas não contam); {@code 0} na criação
     */
    @Query(nativeQuery = true, value = """
            select distinct rr.recurso_id from reserva_recurso rr
            where rr.recurso_id in (:recursoIds)
              and rr.reserva_id <> :ignorarReservaId
              and rr.estado in ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA')
              and rr.periodo && cast(:periodo as tstzrange)
            order by rr.recurso_id""")
    List<Long> recursosOcupados(@Param("recursoIds") Collection<Long> recursoIds, @Param("periodo") String periodo,
            @Param("ignorarReservaId") long ignorarReservaId);
}
