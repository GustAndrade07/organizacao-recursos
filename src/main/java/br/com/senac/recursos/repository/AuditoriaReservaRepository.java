package br.com.senac.recursos.repository;

import br.com.senac.recursos.model.AuditoriaReserva;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

/**
 * Só inclusão e leitura (RN-10): de propósito não estende {@code JpaRepository}, então não existe {@code delete} nem
 * {@code deleteAll} para alguém chamar por engano.
 */
public interface AuditoriaReservaRepository extends Repository<AuditoriaReserva, Long> {

    AuditoriaReserva save(AuditoriaReserva registro);

    @EntityGraph(attributePaths = {"usuario", "reserva"})
    List<AuditoriaReserva> findByReserva_IdOrderByInstanteAscIdAsc(Long reservaId);
}
