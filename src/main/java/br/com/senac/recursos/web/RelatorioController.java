package br.com.senac.recursos.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Esqueleto da tela 12 (relatório de utilização); a implementação vem no próximo commit. */
@Controller
public class RelatorioController {

    @GetMapping("/relatorios/utilizacao")
    public String utilizacao() {
        return "relatorio";
    }
}
