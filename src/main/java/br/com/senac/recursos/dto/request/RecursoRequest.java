package br.com.senac.recursos.dto.request;

import br.com.senac.recursos.enums.TipoRecurso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Cadastro de recurso (RF-02). Capacidade é opcional e, quando vem, positiva (constraint da V1). */
public record RecursoRequest(
        @NotNull(message = "Informe o tipo: SALA, MATERIAL ou PROFESSOR.")
        TipoRecurso tipo,

        @NotBlank(message = "Informe o nome.")
        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
        String nome,

        @Positive(message = "A capacidade, quando informada, precisa ser maior que zero.")
        Integer capacidade,

        boolean restrito) {
}
