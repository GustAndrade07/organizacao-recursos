package br.com.senac.recursos.web;

import br.com.senac.recursos.dto.response.RecursoResponse;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.model.Periodo;
import br.com.senac.recursos.service.RecursoService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Telas 4 e 5 do inventário: buscar disponibilidade e resultados (RF-03), mesma rota e mesmo template, ligadas ao
 * {@link RecursoService}. Com data, início e término (término depois do início) e sem {@code editar}, a página mostra
 * os recursos livres: sem reserva que ocupe o horário (D5) e sem manutenção. O resultado é informativo; quem garante
 * a reserva é a constraint do banco, na gravação. Sem os três campos, mostra o formulário.
 *
 * <p>O filtro de localização fica sem opções: o esquema ainda não guarda a localização do recurso (V2).
 */
@Controller
public class BuscaController {

    private static final List<String> TIPOS = Arrays.stream(TipoRecurso.values()).map(FormatoDasTelas::tipo).toList();

    /** Linha do resultado: {@code capacidade} já em texto ("40 lugares"). */
    public record Resultado(Long id, String nome, String tipo, String local, String capacidade, boolean restrito) {
    }

    private final RecursoService recursos;
    private final FormatoDasTelas formato;

    public BuscaController(RecursoService recursos, FormatoDasTelas formato) {
        this.recursos = recursos;
        this.formato = formato;
    }

    @GetMapping("/recursos/disponiveis")
    public String buscar(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Integer capacidade,
            @RequestParam(required = false) String local,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime termino,
            HttpServletRequest requisicao,
            Model model) {
        FiltrosBusca filtros = new FiltrosBusca(vazioParaNulo(tipo), capacidade, vazioParaNulo(local), data, inicio,
                termino);
        model.addAttribute("filtros", filtros);
        model.addAttribute("tipos", TIPOS);
        model.addAttribute("locais", List.of());
        if (!requisicao.getParameterMap().containsKey("editar") && data != null && inicio != null && termino != null && termino.isAfter(inicio)) {
            Periodo periodo = new Periodo(formato.instante(data, inicio), formato.instante(data, termino));
            Integer capacidadeMinima = capacidade != null && capacidade > 0 ? capacidade : null;
            model.addAttribute("resultados", recursos.disponiveis(periodo, tipoDe(filtros.tipo()), capacidadeMinima)
                    .stream().map(BuscaController::resultado).toList());
        }
        return "busca";
    }

    private static Resultado resultado(RecursoResponse recurso) {
        return new Resultado(recurso.id(), recurso.nome(), FormatoDasTelas.tipo(recurso.tipo()),
                VisaoDasReservas.SEM_LOCAL, FormatoDasTelas.capacidade(recurso.capacidade()), recurso.restrito());
    }

    /** O filtro mostra os nomes ("Sala"); nome desconhecido não filtra. */
    private static TipoRecurso tipoDe(String nome) {
        for (TipoRecurso tipo : TipoRecurso.values()) {
            if (FormatoDasTelas.tipo(tipo).equals(nome)) {
                return tipo;
            }
        }
        return null;
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
