package br.com.senac.recursos.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Esqueleto da tela 11 (histórico da reserva); a implementação vem no próximo commit. */
@Controller
public class HistoricoController {

    @GetMapping("/reservas/{id}/historico")
    public String historico() {
        return "historico";
    }
}
