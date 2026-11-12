package br.com.senac.recursos.web;

import br.com.senac.recursos.dto.UsuarioAutenticado;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * {@code GET /}: a marca da barra e o login sem destino levam aqui, e daqui cada perfil vai para a primeira tela do
 * seu menu (Solicitante: busca; Responsável: aprovações; Administrador: recursos).
 */
@Controller
public class InicioController {

    @GetMapping("/")
    public String inicio(UsuarioAutenticado quem) {
        return "redirect:" + AtributosDasTelas.rotasDoMenu(quem.perfil()).get(0);
    }
}
