package br.com.senac.recursos.dto.response;

import br.com.senac.recursos.enums.TipoRecurso;

public record RecursoResponse(Long id, TipoRecurso tipo, String nome, Integer capacidade, boolean restrito) {
}
