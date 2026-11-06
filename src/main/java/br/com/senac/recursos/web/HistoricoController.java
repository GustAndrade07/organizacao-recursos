package br.com.senac.recursos.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tela 11 do inventário: histórico da reserva (RF-10, RN-09, RN-10), somente leitura. Só a tela: quem lê a
 * auditoria é o back-end, que preenche {@code reserva} e {@code eventos} (e, se quiser, {@code voltar}); sem
 * {@code reserva} a página mostra só o título. O back-end também decide o acesso: Responsável e
 * Administrador veem; o Solicitante recebe 403 (RN-11), que é a página de erro que já existe.
 */
@Controller
public class HistoricoController {

    @GetMapping("/reservas/{id}/historico")
    public String historico() {
        return "historico";
    }
}
