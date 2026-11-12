package br.com.senac.recursos.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Pedido de reserva (RF-04): um ou mais recursos no mesmo período, ex.: a sala e o professor da aula (D6). Datas em
 * ISO-8601 com deslocamento; o período é {@code [início, fim)} (D1).
 */
public record ReservaRequest(
        @NotEmpty(message = "Informe pelo menos um recurso.")
        @Size(max = 10, message = "Uma reserva pode ter no máximo 10 recursos.")
        List<@NotNull(message = "Recurso sem identificador.") @Positive(message = "Identificador de recurso inválido.") Long> recursoIds,

        @NotNull(message = "Informe o início.") OffsetDateTime inicio,
        @NotNull(message = "Informe o término.") OffsetDateTime fim) {
}
