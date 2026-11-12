package br.com.senac.recursos.dto.response;

import java.time.Instant;

/** Manutenção de um recurso no período {@code [inicio, fim)}. */
public record ManutencaoResponse(Long id, Long recursoId, Instant inicio, Instant fim) {
}
