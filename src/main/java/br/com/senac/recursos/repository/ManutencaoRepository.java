package br.com.senac.recursos.repository;

import br.com.senac.recursos.model.Manutencao;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {

    List<Manutencao> findByRecurso_IdOrderByIdAsc(Long recursoId);

    /**
     * Quais dos recursos têm manutenção que sobrepõe o período (RN-04). Usa o operador {@code &&} do PostgreSQL, que
     * respeita o intervalo semiaberto (D1): terminar exatamente quando a manutenção começa não sobrepõe.
     */
    @Query(nativeQuery = true, value = """
            select distinct m.recurso_id from manutencao m
            where m.recurso_id in (:recursoIds)
              and m.periodo && cast(:periodo as tstzrange)
            order by m.recurso_id""")
    List<Long> recursosEmManutencao(@Param("recursoIds") Collection<Long> recursoIds,
            @Param("periodo") String periodo);
}
