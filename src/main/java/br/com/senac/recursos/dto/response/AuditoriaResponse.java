package br.com.senac.recursos.dto.response;

import br.com.senac.recursos.enums.EstadoReserva;
import java.time.Instant;

/** Um registro do histórico da reserva (RN-09): quem, quando, de qual estado para qual e por quê. */
public record AuditoriaResponse(
        Long id,
        Long reservaId,
        Long usuarioId,
        String usuarioLogin,
        Instant instante,
        EstadoReserva estadoAnterior,
        EstadoReserva estadoNovo,
        String motivo) {
}
