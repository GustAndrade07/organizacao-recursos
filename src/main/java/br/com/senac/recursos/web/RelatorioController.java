package br.com.senac.recursos.web;

import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.dto.response.CargaPorProfessorResponse;
import br.com.senac.recursos.dto.response.HorasPorRecursoResponse;
import br.com.senac.recursos.dto.response.RelatorioUtilizacaoResponse;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.model.Periodo;
import br.com.senac.recursos.service.RelatorioService;
import br.com.senac.recursos.service.ReservaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela 12 do inventário: relatório de utilização (RF-12, RN-13), só Responsável e Administrador (matriz), ligada ao
 * {@link RelatorioService}. O período escolhido vai de 00:00 do início até 00:00 do dia seguinte ao fim, no fuso das
 * telas ({@code [início, fim)}, D1). Fim anterior ao início volta com {@code erros} e não calcula.
 *
 * <p>As horas e os conflitos são os do service (as consultas de {@code db/consultas/}, premissas pendentes na D8). A
 * contagem de reservas por linha não vem do service: a tela conta as reservas que ocupam horário (D5) e cruzam o
 * período, a mesma regra das consultas. "Professor" é o recurso do tipo professor (D6).
 */
@Controller
public class RelatorioController {

    private static final String PERIODO_INVALIDO = "A data de fim não pode ser anterior à data de início.";

    private final RelatorioService relatorios;
    private final ReservaService reservas;
    private final FormatoDasTelas formato;

    public RelatorioController(RelatorioService relatorios, ReservaService reservas, FormatoDasTelas formato) {
        this.relatorios = relatorios;
        this.reservas = reservas;
        this.formato = formato;
    }

    /** Linha das tabelas do relatório: {@code horas} já em texto ("12 h 30 min"). */
    public record Linha(String nome, long reservas, String horas) {
    }

    public record Relatorio(List<Linha> recursos, List<Linha> professores, long totalReservas, String totalHoras,
            long conflitos) {
    }

    @GetMapping("/relatorios/utilizacao")
    public String utilizacao(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            UsuarioAutenticado quem, Model model) {
        model.addAttribute("filtro", new FiltroRelatorio(inicio, fim));
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            model.addAttribute("erros", Map.of("fim", PERIODO_INVALIDO));
        } else if (inicio != null && fim != null) {
            Instant de = inicio.atStartOfDay(formato.fuso()).toInstant();
            Instant ate = fim.plusDays(1).atStartOfDay(formato.fuso()).toInstant();
            model.addAttribute("relatorio", montar(relatorios.utilizacao(new Periodo(de, ate)), de, ate, quem));
        }
        return "relatorio";
    }

    private Relatorio montar(RelatorioUtilizacaoResponse dados, Instant de, Instant ate, UsuarioAutenticado quem) {
        List<ReservaResponse> noPeriodo = reservas.listar(null, quem).stream()
                .filter(reserva -> reserva.estado().ocupaHorario())
                .filter(reserva -> reserva.inicio().isBefore(ate) && de.isBefore(reserva.fim()))
                .toList();
        Map<Long, Long> porRecurso = new HashMap<>();
        noPeriodo.forEach(reserva -> reserva.recursos()
                .forEach(recurso -> porRecurso.merge(recurso.id(), 1L, Long::sum)));

        List<Linha> recursos = dados.horasPorRecurso().stream()
                .map((HorasPorRecursoResponse linha) -> new Linha(linha.nome(),
                        porRecurso.getOrDefault(linha.recursoId(), 0L), FormatoDasTelas.horas(linha.horas())))
                .toList();
        List<Linha> professores = dados.cargaPorProfessor().stream()
                .map((CargaPorProfessorResponse linha) -> new Linha(linha.nome(),
                        porRecurso.getOrDefault(linha.professorId(), 0L), FormatoDasTelas.horas(linha.horas())))
                .toList();
        BigDecimal total = dados.horasPorRecurso().stream().map(HorasPorRecursoResponse::horas)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Relatorio(recursos, professores, noPeriodo.size(), FormatoDasTelas.horas(total),
                dados.conflitosEvitados());
    }
}
