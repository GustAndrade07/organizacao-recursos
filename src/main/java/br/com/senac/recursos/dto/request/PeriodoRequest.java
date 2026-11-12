package br.com.senac.recursos.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * Período {@code [início, fim)} (D1) em ISO-8601 com deslocamento, ex.: {@code 2026-11-10T10:00:00-03:00}. Serve para
 * manutenção e para a alteração de uma reserva. "Término depois do início" (RN-01) é conferido ao virar
 * {@code model.Periodo}.
 */
public record PeriodoRequest(
        @NotNull(message = "Informe o início.") OffsetDateTime inicio,
        @NotNull(message = "Informe o término.") OffsetDateTime fim) {
}
