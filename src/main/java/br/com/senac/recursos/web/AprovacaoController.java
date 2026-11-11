package br.com.senac.recursos.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tela 9 do inventário: fila de aprovação (RF-07, RN-06). Só a tela: quem lista os pedidos Solicitados,
 * aprova e rejeita é o back-end, que preenche {@code pedidos}, {@code rejeitando}, {@code erros},
 * {@code confirmacao} e {@code erroAprovacao} e responde a {@code POST /aprovacoes/{id}/aprovar} e
 * {@code POST /aprovacoes/{id}/rejeitar} (docs/frontend/README.md). O parâmetro {@code rejeitar}, com o
 * id do pedido, abre o diálogo de rejeição: o back-end o lê e devolve o pedido em {@code rejeitando}.
 */
@Controller
public class AprovacaoController {

    @GetMapping("/aprovacoes")
    public String fila() {
        return "aprovacoes";
    }
}
