package br.com.senac.recursos.dto.request;

import jakarta.validation.constraints.Size;

/** Motivo opcional de uma mudança de estado; vai para a auditoria (RN-09). O corpo inteiro é opcional. */
public record MotivoRequest(
        @Size(max = 500, message = "O motivo pode ter no máximo 500 caracteres.") String motivo) {
}
