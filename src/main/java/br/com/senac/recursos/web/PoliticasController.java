package br.com.senac.recursos.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Telas 17 a 19 do inventário: política de privacidade, política de cookies e termos de uso. Páginas públicas
 * ({@code permitAll} só para estes três {@code GET}, em {@code SegurancaDasTelasConfig}; linhas na matriz de
 * autorização), de texto fixo nos templates: sem modelo, sem banco e sem entrada do usuário.
 */
@Controller
public class PoliticasController {

    @GetMapping("/privacidade")
    public String privacidade() {
        return "politica-privacidade";
    }

    @GetMapping("/cookies")
    public String cookies() {
        return "politica-cookies";
    }

    @GetMapping("/termos")
    public String termos() {
        return "termos-de-uso";
    }
}
