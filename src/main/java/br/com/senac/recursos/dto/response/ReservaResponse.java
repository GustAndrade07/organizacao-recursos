package br.com.senac.recursos.dto.response;

import br.com.senac.recursos.enums.EstadoReserva;
import java.time.Instant;
import java.util.List;

/** Reserva com o período {@code [inicio, fim)} e os recursos que ela ocupa. */
public record ReservaResponse(
        Long id,
        Long solicitanteId,
        EstadoReserva estado,
        Instant inicio,
        Instant fim,
        Instant criadaEm,
        List<RecursoResponse> recursos) {
}
