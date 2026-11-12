package br.com.senac.recursos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Alteração de recurso. O tipo não muda depois do cadastro. */
public record RecursoAtualizacaoRequest(
        @NotBlank(message = "Informe o nome.")
        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
        String nome,

        @Positive(message = "A capacidade, quando informada, precisa ser maior que zero.")
        Integer capacidade,

        boolean restrito) {
}
