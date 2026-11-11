package br.com.senac.recursos.web;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Telas 6, 7 e 8 do inventário, do Solicitante (RF-04 a RF-06). Só as telas; quem lista, grava, altera e
 * cancela é o back-end (docs/frontend/README.md).
 * <ul>
 * <li>Tela 6, nova reserva: preenche o formulário com o que veio da busca. O back-end responde a
 * {@code POST /reservas} e, se recusar, volta a esta tela com {@code erros}, {@code conflito} ou
 * {@code manutencao} no modelo. O atributo {@code recurso} (nome, local, restrito) também é do back-end.</li>
 * <li>Tela 7, minhas reservas: devolve o filtro de estado escolhido (valor desconhecido vira {@code todos});
 * o back-end preenche {@code reservas}, já filtradas e ordenadas.</li>
 * <li>Tela 8, detalhe: devolve a ação pedida em {@code ?acao=alterar} ou {@code ?acao=cancelar} (outro valor é
 * ignorado). O back-end preenche {@code reserva} e responde a {@code POST /reservas/{id}} (alterar) e
 * {@code POST /reservas/{id}/cancelar}. A rota exige {@code {id}} numérico: outro valor responde 400.</li>
 * </ul>
 */
@Controller
public class ReservaController {

    private static final String TODOS = "todos";
    private static final Set<String> FILTROS = Set.of(TODOS, "solicitada", "aprovada", "em-uso", "concluida",
            "rejeitada", "cancelada", "nao-compareceu");
    private static final Set<String> ACOES = Set.of("alterar", "cancelar");

    @GetMapping("/reservas/nova")
    public String nova(
            @RequestParam(required = false) String recursoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime termino,
            Model model) {
        model.addAttribute("reserva", new FormularioReserva(recursoId, data, inicio, termino));
        return "reserva-nova";
    }

    @GetMapping("/reservas/minhas")
    public String minhas(@RequestParam(required = false) String estado, Model model) {
        model.addAttribute("filtro", estado != null && FILTROS.contains(estado) ? estado : TODOS);
        return "reservas-minhas";
    }

    @GetMapping("/reservas/{id}")
    public String detalhe(@PathVariable long id, @RequestParam(required = false) String acao, Model model) {
        if (acao != null && ACOES.contains(acao)) {
            model.addAttribute("acao", acao);
        }
        return "reserva-detalhe";
    }
}
