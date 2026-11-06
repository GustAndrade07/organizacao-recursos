package br.com.senac.recursos.web;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela 12 do inventário: relatório de utilização (RF-12, RN-13). Devolve o período ao filtro e recusa fim
 * anterior ao início com {@code erros}. Quem calcula é o back-end, que preenche {@code relatorio} e não deve
 * fazê-lo quando {@code erros} existir; sem {@code relatorio} a página mostra só o filtro. O acesso (só
 * Responsável e Administrador) também é do back-end.
 */
@Controller
public class RelatorioController {

    private static final String PERIODO_INVALIDO = "A data de fim não pode ser anterior à data de início.";

    @GetMapping("/relatorios/utilizacao")
    public String utilizacao(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Model model) {
        model.addAttribute("filtro", new FiltroRelatorio(inicio, fim));
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            model.addAttribute("erros", Map.of("fim", PERIODO_INVALIDO));
        }
        return "relatorio";
    }
}
