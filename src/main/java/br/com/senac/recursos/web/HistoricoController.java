package br.com.senac.recursos.web;

import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.dto.response.AuditoriaResponse;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.service.ReservaService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Tela 11 do inventário: histórico da reserva (RF-10, RN-09, RN-10), somente leitura, ligada ao
 * {@link ReservaService}. Só Responsável e Administrador chegam aqui (RN-11, matriz); o Solicitante recebe 403. A
 * rota exige {@code {id}} numérico (outro valor responde 400) e reserva inexistente responde 404. Os eventos vêm do
 * mais antigo ao mais recente, como a auditoria os grava. O link "Voltar" usa a lista fixa abaixo, nunca um parâmetro.
 */
@Controller
public class HistoricoController {

    private final ReservaService reservas;
    private final VisaoDasReservas visao;

    public HistoricoController(ReservaService reservas, VisaoDasReservas visao) {
        this.reservas = reservas;
        this.visao = visao;
    }

    /** Linha do tempo: {@code de} é nulo na criação da reserva. */
    public record Evento(java.time.LocalDateTime instante, String de, String para, String usuario, String motivo) {
    }

    @GetMapping("/reservas/{id}/historico")
    public String historico(@PathVariable long id, UsuarioAutenticado quem, Model model) {
        ReservaResponse reserva = reservas.buscar(id, quem);
        Map<Long, String> nomes = visao.nomes();
        FormatoDasTelas formato = visao.formato();
        Map<String, Object> resumo = new HashMap<>();
        resumo.put("recurso", FormatoDasTelas.recursos(reserva));
        resumo.put("solicitante", visao.nomeDe(reserva.solicitanteId(), nomes));
        resumo.put("data", formato.data(reserva.inicio()));
        resumo.put("horario", formato.horario(reserva.inicio(), reserva.fim()));
        resumo.put("estado", FormatoDasTelas.estado(reserva.estado()));
        model.addAttribute("reserva", resumo);

        List<AuditoriaResponse> registros = reservas.historico(id);
        model.addAttribute("eventos", registros.stream().map(registro -> new Evento(
                formato.dataHora(registro.instante()),
                registro.estadoAnterior() == null ? null : FormatoDasTelas.estado(registro.estadoAnterior()),
                FormatoDasTelas.estado(registro.estadoNovo()),
                visao.nomeDe(registro.usuarioId(), nomes),
                registro.motivo())).toList());
        model.addAttribute("voltar", voltarPara(quem));
        return "historico";
    }

    private static String voltarPara(UsuarioAutenticado quem) {
        return switch (quem.perfil()) {
            case RESPONSAVEL -> "/retirada-devolucao?estado=todos";
            case ADMINISTRADOR -> "/relatorios/utilizacao";
            case SOLICITANTE -> "/reservas/minhas";
        };
    }
}
