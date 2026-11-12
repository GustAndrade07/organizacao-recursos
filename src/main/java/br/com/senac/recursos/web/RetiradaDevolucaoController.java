package br.com.senac.recursos.web;

import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.enums.EstadoReserva;
import br.com.senac.recursos.service.ReservaService;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Tela 10 do inventário: retirada e devolução (RF-09, RN-05, RN-08), só do Responsável (matriz), ligada ao
 * {@link ReservaService}. Filtro {@code ativas} (Aprovadas e em uso, o padrão), {@code aprovada}, {@code em-uso} ou
 * {@code todos}; valor desconhecido vira {@code ativas}. {@code horarioPassou} é verdadeiro quando o horário da
 * reserva já terminou: só então a tela oferece "Marcar como não compareceu" (o service não tem prazo, D10 (d)). A
 * transição que o estado não permite é recusada pelo service (409).
 */
@Controller
public class RetiradaDevolucaoController {

    private static final String PADRAO = "ativas";
    private static final Map<String, Set<EstadoReserva>> FILTROS = Map.of(
            PADRAO, EnumSet.of(EstadoReserva.APROVADA, EstadoReserva.EM_USO),
            "aprovada", EnumSet.of(EstadoReserva.APROVADA),
            "em-uso", EnumSet.of(EstadoReserva.EM_USO),
            "todos", EnumSet.allOf(EstadoReserva.class));
    private static final String VOLTAR = "redirect:/retirada-devolucao";

    private final ReservaService reservas;
    private final VisaoDasReservas visao;

    public RetiradaDevolucaoController(ReservaService reservas, VisaoDasReservas visao) {
        this.reservas = reservas;
        this.visao = visao;
    }

    @GetMapping("/retirada-devolucao")
    public String listar(@RequestParam(required = false) String estado,
            @RequestParam(required = false) Long naoCompareceu, UsuarioAutenticado quem, Model model) {
        String filtro = estado != null && FILTROS.containsKey(estado) ? estado : PADRAO;
        model.addAttribute("filtro", filtro);
        Map<Long, String> nomes = visao.nomes();
        Set<EstadoReserva> estados = FILTROS.get(filtro);
        List<Map<String, Object>> linhas = reservas.listar(null, quem).stream()
                .filter(reserva -> estados.contains(reserva.estado()))
                .sorted(Comparator.comparing(ReservaResponse::inicio))
                .map(reserva -> visao.linha(reserva, nomes))
                .toList();
        model.addAttribute("reservas", linhas);
        if (naoCompareceu != null) {
            linhas.stream().filter(linha -> naoCompareceu.equals(linha.get("id"))).findFirst()
                    .ifPresent(linha -> model.addAttribute("naoCompareceu", linha));
        }
        return "retirada-devolucao";
    }

    @PostMapping("/retirada-devolucao/{id}/retirar")
    public String retirar(@PathVariable long id, UsuarioAutenticado quem, RedirectAttributes redirecionamento) {
        return confirmar("Retirada registrada", reservas.registrarRetirada(id, null, quem), redirecionamento);
    }

    @PostMapping("/retirada-devolucao/{id}/devolver")
    public String devolver(@PathVariable long id, UsuarioAutenticado quem, RedirectAttributes redirecionamento) {
        return confirmar("Devolução registrada", reservas.registrarDevolucao(id, null, quem), redirecionamento);
    }

    @PostMapping("/retirada-devolucao/{id}/nao-compareceu")
    public String naoCompareceu(@PathVariable long id, UsuarioAutenticado quem, RedirectAttributes redirecionamento) {
        return confirmar("Não comparecimento registrado", reservas.registrarNaoComparecimento(id, null, quem),
                redirecionamento);
    }

    private String confirmar(String titulo, ReservaResponse reserva, RedirectAttributes redirecionamento) {
        redirecionamento.addFlashAttribute("confirmacao", visao.confirmacao(titulo,
                visao.rotulo(reserva, visao.nomes()), reserva.estado()));
        return VOLTAR;
    }
}
