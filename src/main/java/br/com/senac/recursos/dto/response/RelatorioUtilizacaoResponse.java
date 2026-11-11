package br.com.senac.recursos.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Relatório de utilização (RF-12) na janela {@code [inicio, fim)}. As premissas das consultas (estados que contam,
 * período dos conflitos, recurso sem uso, quem é o professor) seguem pendentes de confirmação do grupo: D8 do plano.
 */
public record RelatorioUtilizacaoResponse(
        Instant inicio,
        Instant fim,
        List<HorasPorRecursoResponse> horasPorRecurso,
        List<CargaPorProfessorResponse> cargaPorProfessor,
        long conflitosEvitados) {
}
