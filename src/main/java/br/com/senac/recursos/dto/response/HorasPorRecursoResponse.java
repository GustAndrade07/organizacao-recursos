package br.com.senac.recursos.dto.response;

import br.com.senac.recursos.enums.TipoRecurso;
import java.math.BigDecimal;

/** Linha "horas reservadas por recurso" do relatório de utilização (RF-12). */
public record HorasPorRecursoResponse(Long recursoId, String nome, TipoRecurso tipo, BigDecimal horas) {
}
