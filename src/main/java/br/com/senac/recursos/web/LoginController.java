package br.com.senac.recursos.web;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela de login (tela 1 do inventário). Só mostra o formulário: quem autentica é o back-end (ADR-0001), que
 * deve responder a {@code POST /login} e, em caso de falha, redirecionar para {@code /login?erro}.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam Map<String, String> parametros, Model model) {
        // basta o parâmetro existir (/login?erro); o valor não importa
        model.addAttribute("falhou", parametros.containsKey("erro"));
        return "login";
    }
}
