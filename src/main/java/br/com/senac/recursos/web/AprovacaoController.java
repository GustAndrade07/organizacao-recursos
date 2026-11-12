package br.com.senac.recursos.web;

import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.enums.EstadoReserva;
import br.com.senac.recursos.exception.AcessoNegadoException;
import br.com.senac.recursos.service.ReservaService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Tela 9 do inventário: fila de aprovação (RF-07, RN-06), só do Responsável (matriz), ligada ao
 * {@link ReservaService}. Lista as reservas Solicitadas, as mais próximas primeiro; {@code ?rejeitar={id}} abre o
 * diálogo de rejeição. Aprovar a própria reserva é recusado pelo service (RN-06): a tela mostra o aviso
 * {@code erroAprovacao} com status 403 e não muda nada. Rejeitar exige motivo, que vai para a auditoria (RN-09).
 */
@Controller
public class AprovacaoController {

    private static final String VIEW = "aprovacoes";
    private static final int MOTIVO_MAXIMO = 500;

    private final ReservaService reservas;
    private final VisaoDasReservas visao;

    public AprovacaoController(ReservaService reservas, VisaoDasReservas visao) {
        this.reservas = reservas;
        this.visao = visao;
    }

    @GetMapping("/aprovacoes")
    public String fila(@RequestParam(required = false) Long rejeitar, UsuarioAutenticado quem, Model model) {
        preencher(model, quem, rejeitar);
        return VIEW;
    }

    @PostMapping("/aprovacoes/{id}/aprovar")
    public String aprovar(@PathVariable long id, UsuarioAutenticado quem, Model model,
            RedirectAttributes redirecionamento, HttpServletResponse resposta) {
        try {
            ReservaResponse aprovada = reservas.aprovar(id, null, quem);
            redirecionamento.addFlashAttribute("confirmacao", visao.confirmacao("Reserva aprovada",
                    visao.rotulo(aprovada, visao.nomes()), aprovada.estado()));
            return "redirect:/aprovacoes";
        } catch (AcessoNegadoException propria) {
            preencher(model, quem, null);
            model.addAttribute("erroAprovacao", true);
            resposta.setStatus(HttpStatus.FORBIDDEN.value());
            return VIEW;
        }
    }

    @PostMapping("/aprovacoes/{id}/rejeitar")
    public String rejeitar(@PathVariable long id, @RequestParam(required = false) String motivo,
            UsuarioAutenticado quem, Model model, RedirectAttributes redirecionamento, HttpServletResponse resposta) {
        String texto = motivo == null ? "" : motivo.trim();
        if (texto.isEmpty() || texto.length() > MOTIVO_MAXIMO) {
            preencher(model, quem, id);
            model.addAttribute("erros", Map.of("motivo", texto.isEmpty()
                    ? "Informe o motivo da rejeição."
                    : "O motivo pode ter no máximo " + MOTIVO_MAXIMO + " caracteres."));
            resposta.setStatus(HttpStatus.BAD_REQUEST.value());
            return VIEW;
        }
        ReservaResponse rejeitada = reservas.rejeitar(id, texto, quem);
        redirecionamento.addFlashAttribute("confirmacao", visao.confirmacao("Reserva rejeitada",
                visao.rotulo(rejeitada, visao.nomes()), rejeitada.estado()));
        return "redirect:/aprovacoes";
    }

    private void preencher(Model model, UsuarioAutenticado quem, Long rejeitar) {
        Map<Long, String> nomes = visao.nomes();
        List<Map<String, Object>> pedidos = reservas.listar(EstadoReserva.SOLICITADA, quem).stream()
                .sorted(Comparator.comparing(ReservaResponse::inicio))
                .map(reserva -> {
                    Map<String, Object> linha = visao.linha(reserva, nomes);
                    linha.put("proprio", quem.id().equals(reserva.solicitanteId()));
                    return linha;
                })
                .toList();
        model.addAttribute("pedidos", pedidos);
        if (rejeitar != null) {
            pedidos.stream().filter(pedido -> rejeitar.equals(pedido.get("id"))).findFirst()
                    .ifPresent(pedido -> model.addAttribute("rejeitando", pedido));
        }
    }
}
