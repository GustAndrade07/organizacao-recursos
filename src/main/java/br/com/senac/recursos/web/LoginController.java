package br.com.senac.recursos.web;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela de login (tela 1 do inventário). Só mostra o formulário: quem autentica é o back-end (ADR-0001), que
 * deve responder a {@code POST /login} e, em caso de falha, redirecionar para {@code /login?erro}, devolvendo o
 * e-mail digitado no atributo de modelo {@code email} (por exemplo, atributo flash). Sessão expirada chega em
 * {@code /login?expirou}. {@code ?destino=} guarda a página anterior, que a tela repassa no POST (só se for {@link CaminhoDoSite}); quem decide
 * o redirecionamento é o back-end.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam Map<String, String> parametros, Model model) {
        // basta o parâmetro existir (/login?erro); o valor não importa
        model.addAttribute("falhou", parametros.containsKey("erro"));
        model.addAttribute("expirou", parametros.containsKey("expirou"));
        // só vai ao modelo o destino que é caminho deste site (ou nulo)
        model.addAttribute("destino", CaminhoDoSite.ouNulo(parametros.get("destino")));
        return "login";
    }
}
