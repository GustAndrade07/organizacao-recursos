package br.com.senac.recursos.dto.response;

import java.math.BigDecimal;

/** Linha "carga horária alocada por professor" do relatório de utilização (RF-12; o professor é recurso, D6). */
public record CargaPorProfessorResponse(Long professorId, String nome, BigDecimal horas) {
}
