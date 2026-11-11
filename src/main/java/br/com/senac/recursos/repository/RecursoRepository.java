package br.com.senac.recursos.repository;

import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.model.Recurso;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    /** Consulta do cadastro (RF-02, RF-03): filtros opcionais por tipo e capacidade mínima. */
    @Query("""
            select r from Recurso r
            where (:tipo is null or r.tipo = :tipo)
              and (:capacidadeMinima is null or r.capacidade >= :capacidadeMinima)
            order by r.id""")
    List<Recurso> buscar(@Param("tipo") TipoRecurso tipo, @Param("capacidadeMinima") Integer capacidadeMinima);

    /**
     * Disponibilidade (RF-03): recursos sem reserva que ocupe o horário (estados da D5) e sem manutenção que
     * sobreponha o período. É uma consulta de leitura: a garantia contra reserva dupla continua sendo a constraint.
     *
     * @param periodo literal {@code tstzrange} de {@code model.Periodo#comoTstzrange()}
     */
    @Query(nativeQuery = true, value = """
            select r.* from recurso r
            where (cast(:tipo as text) is null or r.tipo = cast(:tipo as text))
              and (cast(:capacidadeMinima as integer) is null or r.capacidade >= cast(:capacidadeMinima as integer))
              and not exists (
                  select 1 from reserva_recurso rr
                  where rr.recurso_id = r.id
                    and rr.estado in ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA')
                    and rr.periodo && cast(:periodo as tstzrange))
              and not exists (
                  select 1 from manutencao m
                  where m.recurso_id = r.id
                    and m.periodo && cast(:periodo as tstzrange))
            order by r.id""")
    List<Recurso> buscarDisponiveis(@Param("tipo") String tipo, @Param("capacidadeMinima") Integer capacidadeMinima,
            @Param("periodo") String periodo);
}
