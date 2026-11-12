package br.com.senac.recursos.converter;

import br.com.senac.recursos.dto.response.AuditoriaResponse;
import br.com.senac.recursos.model.AuditoriaReserva;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaConverter {

    public AuditoriaResponse paraResposta(AuditoriaReserva registro) {
        return new AuditoriaResponse(registro.getId(), registro.getReserva().getId(), registro.getUsuario().getId(),
                registro.getUsuario().getLogin(), registro.getInstante(), registro.getEstadoAnterior(),
                registro.getEstadoNovo(), registro.getMotivo());
    }
}
