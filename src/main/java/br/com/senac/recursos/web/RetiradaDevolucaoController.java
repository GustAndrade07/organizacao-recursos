package br.com.senac.recursos.web;

import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela 10 do inventário: retirada e devolução (RF-09, RN-05, RN-08). Só a tela: devolve o filtro de estado
 * escolhido ({@code ativas}, {@code aprovada}, {@code em-uso} ou {@code todos}; qualquer outro valor vira
 * {@code ativas}). Quem lista as reservas e registra retirada, devolução e não comparecimento é o
 * back-end, que preenche {@code reservas}, {@code naoCompareceu} e {@code confirmacao} e responde a
 * {@code POST /retirada-devolucao/{id}/retirar}, {@code .../devolver} e {@code .../nao-compareceu}
 * (docs/frontend/README.md). O parâmetro {@code naoCompareceu}, com o id da reserva, abre o diálogo de
 * confirmação: o back-end o lê e devolve a reserva em {@code naoCompareceu}.
 */
@Controller
public class RetiradaDevolucaoController {

    private static final String PADRAO = "ativas";
    private static final Set<String> FILTROS = Set.of(PADRAO, "aprovada", "em-uso", "todos");

    @GetMapping("/retirada-devolucao")
    public String listar(@RequestParam(required = false) String estado, Model model) {
        model.addAttribute("filtro", estado != null && FILTROS.contains(estado) ? estado : PADRAO);
        return "retirada-devolucao";
    }
}
