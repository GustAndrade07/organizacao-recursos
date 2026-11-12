package br.com.senac.recursos.service;

import br.com.senac.recursos.converter.AuditoriaConverter;
import br.com.senac.recursos.dto.response.AuditoriaResponse;
import br.com.senac.recursos.enums.EstadoReserva;
import br.com.senac.recursos.model.AuditoriaReserva;
import br.com.senac.recursos.model.Reserva;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.repository.AuditoriaReservaRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Histórico auditável (RF-10, RN-08 a RN-12). */
@Service
public class AuditoriaService {

    private final AuditoriaReservaRepository registros;
    private final AuditoriaConverter conversor;
    private final Clock relogio;

    public AuditoriaService(AuditoriaReservaRepository registros, AuditoriaConverter conversor, Clock relogio) {
        this.registros = registros;
        this.conversor = conversor;
        this.relogio = relogio;
    }

    /**
     * Grava o registro na MESMA transação da mudança de estado (RN-12): {@link Propagation#MANDATORY} faz a chamada
     * falhar se não houver transação aberta, então não existe caminho que grave auditoria solta nem mudança sem
     * auditoria.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Reserva reserva, Usuario usuario, EstadoReserva anterior, EstadoReserva novo,
            String motivo) {
        registros.save(new AuditoriaReserva(reserva, usuario, relogio.instant(), anterior, novo, motivo));
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> historico(Long reservaId) {
        return registros.findByReserva_IdOrderByInstanteAscIdAsc(reservaId).stream()
                .map(conversor::paraResposta).toList();
    }
}
