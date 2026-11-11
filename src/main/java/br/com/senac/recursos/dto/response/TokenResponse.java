package br.com.senac.recursos.dto.response;

import java.time.Instant;

/** Resposta do login (ADR-0001): o token vai no cabeçalho {@code Authorization: Bearer <token>} de cada requisição. */
public record TokenResponse(String token, String tipo, Instant expiraEm) {
}
