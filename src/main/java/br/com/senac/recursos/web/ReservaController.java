package br.com.senac.recursos.web;

import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.dto.response.AuditoriaResponse;
import br.com.senac.recursos.dto.response.RecursoResponse;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.enums.EstadoReserva;
import br.com.senac.recursos.exception.ConflitoDeHorarioException;
import br.com.senac.recursos.exception.RecursoEmManutencaoException;
import br.com.senac.recursos.exception.RequisicaoInvalidaException;
import br.com.senac.recursos.model.Periodo;
import br.com.senac.recursos.service.RecursoService;
import br.com.senac.recursos.service.ReservaService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Telas 6, 7 e 8 do inventário, do Solicitante (RF-04 a RF-06), ligadas ao {@link ReservaService} (D12, opção (a)). A
 * regra continua toda no service e no banco: a tela só converte data e hora do formulário (fuso das telas) no período
 * {@code [início, fim)} e mostra o resultado.
 * <ul>
 * <li>Tela 6, nova reserva: {@code GET /reservas/nova} mostra o recurso escolhido na busca; {@code POST /reservas}
 * grava e leva ao detalhe com o aviso de confirmação. O 409 do banco (RN-02, RN-03) volta ao formulário com
 * {@code conflito}; a manutenção (RN-04), com {@code manutencao}; término não posterior ao início (RN-01), com
 * {@code erros}.</li>
 * <li>Tela 7, minhas reservas: só as do usuário logado (mesmo para Responsável e Administrador, que pela API veem
 * todas), filtradas pelo estado e com as próximas primeiro.</li>
 * <li>Tela 8, detalhe: {@code ?acao=alterar} abre o formulário e {@code ?acao=cancelar} a confirmação (outro valor é
 * ignorado). Reserva de outra pessoa responde 403 para o Solicitante (posse no service).</li>
 * </ul>
 */
@Controller
public class ReservaController {

    private static final String TODOS = "todos";
    private static final Map<String, EstadoReserva> FILTROS = Map.of(
            "solicitada", EstadoReserva.SOLICITADA, "aprovada", EstadoReserva.APROVADA,
            "em-uso", EstadoReserva.EM_USO, "concluida", EstadoReserva.CONCLUIDA,
            "rejeitada", EstadoReserva.REJEITADA, "cancelada", EstadoReserva.CANCELADA,
            "nao-compareceu", EstadoReserva.NAO_COMPARECEU);
    private static final Set<String> ACOES = Set.of("alterar", "cancelar");

    private static final String VIEW_NOVA = "reserva-nova";
    private static final String VIEW_DETALHE = "reserva-detalhe";
    private static final String CAMPO_TERMINO = "termino";

    private final ReservaService reservas;
    private final RecursoService recursos;
    private final VisaoDasReservas visao;

    public ReservaController(ReservaService reservas, RecursoService recursos, VisaoDasReservas visao) {
        this.reservas = reservas;
        this.recursos = recursos;
        this.visao = visao;
    }

    // ---------------------------------------------------------------- tela 6

    @GetMapping("/reservas/nova")
    public String nova(
            @RequestParam(required = false) String recursoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime termino,
            Model model) {
        model.addAttribute("reserva", new FormularioReserva(recursoId, data, inicio, termino));
        Long id = idOuNulo(recursoId);
        if (id != null) {
            model.addAttribute("recurso", recursoDaTela(recursos.buscar(id)));
        }
        return VIEW_NOVA;
    }

    @PostMapping("/reservas")
    public String criar(
            @RequestParam(required = false) String recursoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime termino,
            UsuarioAutenticado quem, Model model, RedirectAttributes redirecionamento, HttpServletResponse resposta) {
        Long id = idOuNulo(recursoId);
        if (id == null) {
            throw new RequisicaoInvalidaException("Escolha o recurso na busca.");
        }
        RecursoResponse recurso = recursos.buscar(id);
        model.addAttribute("reserva", new FormularioReserva(recursoId, data, inicio, termino));
        model.addAttribute("recurso", recursoDaTela(recurso));

        Map<String, String> erros = errosDoPeriodo(data, inicio, termino);
        if (!erros.isEmpty()) {
            return recusar(model, erros, resposta, HttpStatus.BAD_REQUEST);
        }
        Instant de = visao.formato().instante(data, inicio);
        Instant ate = visao.formato().instante(data, termino);
        try {
            ReservaResponse criada = reservas.criar(List.of(id), new Periodo(de, ate), quem);
            String titulo = criada.estado() == EstadoReserva.SOLICITADA ? "Pedido de reserva enviado" : "Reserva confirmada";
            redirecionamento.addFlashAttribute("confirmacao", visao.confirmacao(titulo,
                    rotuloDoHorario(criada), criada.estado()));
            return "redirect:/reservas/" + criada.id();
        } catch (ConflitoDeHorarioException conflito) {
            model.addAttribute("conflito", true);
            resposta.setStatus(HttpStatus.CONFLICT.value());
            return VIEW_NOVA;
        } catch (RecursoEmManutencaoException emManutencao) {
            visao.manutencao(emManutencao.recursoIds(), de, ate).ifPresent(m -> model.addAttribute("manutencao", m));
            resposta.setStatus(HttpStatus.CONFLICT.value());
            return VIEW_NOVA;
        }
    }

    // ---------------------------------------------------------------- tela 7

    @GetMapping("/reservas/minhas")
    public String minhas(@RequestParam(required = false) String estado, UsuarioAutenticado quem, Model model) {
        String filtro = estado != null && FILTROS.containsKey(estado) ? estado : TODOS;
        model.addAttribute("filtro", filtro);
        Map<Long, String> nomes = visao.nomes();
        model.addAttribute("reservas", reservas.listar(FILTROS.get(filtro), quem).stream()
                .filter(reserva -> quem.id().equals(reserva.solicitanteId()))
                .sorted(visao.proximasPrimeiro())
                .map(reserva -> visao.linha(reserva, nomes))
                .toList());
        return "reservas-minhas";
    }

    // ---------------------------------------------------------------- tela 8

    @GetMapping("/reservas/{id}")
    public String detalhe(@PathVariable long id, @RequestParam(required = false) String acao,
            UsuarioAutenticado quem, Model model) {
        if (acao != null && ACOES.contains(acao)) {
            model.addAttribute("acao", acao);
        }
        model.addAttribute("reserva", detalheDaTela(reservas.buscar(id, quem), quem));
        return VIEW_DETALHE;
    }

    @PostMapping("/reservas/{id}")
    public String alterar(@PathVariable long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime termino,
            UsuarioAutenticado quem, Model model, RedirectAttributes redirecionamento, HttpServletResponse resposta) {
        ReservaResponse atual = reservas.buscar(id, quem);
        model.addAttribute("acao", "alterar");
        model.addAttribute("reserva", detalheDaTela(atual, quem));
        model.addAttribute("formulario", new FormularioReserva(null, data, inicio, termino));

        Map<String, String> erros = errosDoPeriodo(data, inicio, termino);
        if (!erros.isEmpty()) {
            return recusarAlteracao(model, erros, resposta, HttpStatus.BAD_REQUEST);
        }
        Instant de = visao.formato().instante(data, inicio);
        Instant ate = visao.formato().instante(data, termino);
        try {
            ReservaResponse alterada = reservas.alterarPeriodo(id, new Periodo(de, ate), quem);
            redirecionamento.addFlashAttribute("confirmacao", visao.confirmacao("Reserva alterada",
                    rotuloDoHorario(alterada), alterada.estado()));
            return "redirect:/reservas/" + id;
        } catch (ConflitoDeHorarioException conflito) {
            model.addAttribute("conflito", true);
            resposta.setStatus(HttpStatus.CONFLICT.value());
            return VIEW_DETALHE;
        } catch (RecursoEmManutencaoException emManutencao) {
            visao.manutencao(emManutencao.recursoIds(), de, ate).ifPresent(m -> model.addAttribute("manutencao", m));
            resposta.setStatus(HttpStatus.CONFLICT.value());
            return VIEW_DETALHE;
        }
    }

    @PostMapping("/reservas/{id}/cancelar")
    public String cancelar(@PathVariable long id, UsuarioAutenticado quem, RedirectAttributes redirecionamento) {
        ReservaResponse cancelada = reservas.cancelar(id, null, quem);
        redirecionamento.addFlashAttribute("confirmacao", visao.confirmacao("Reserva cancelada",
                rotuloDoHorario(cancelada), cancelada.estado()));
        return "redirect:/reservas/" + id;
    }

    // ---------------------------------------------------------------- apoio

    /** RN-01 antes de chamar o service, com o texto junto ao campo; o service confere de novo ({@link Periodo}). */
    private static Map<String, String> errosDoPeriodo(LocalDate data, LocalTime inicio, LocalTime termino) {
        Map<String, String> erros = new LinkedHashMap<>();
        if (data == null) {
            erros.put("data", "Informe a data.");
        }
        if (inicio == null) {
            erros.put("inicio", "Informe o horário de início.");
        }
        if (termino == null) {
            erros.put(CAMPO_TERMINO, "Informe o horário de término.");
        } else if (inicio != null && !termino.isAfter(inicio)) {
            erros.put(CAMPO_TERMINO, "O término precisa ser depois do início.");
        }
        return erros;
    }

    private static String recusar(Model model, Map<String, String> erros, HttpServletResponse resposta,
            HttpStatus status) {
        model.addAttribute("erros", erros);
        resposta.setStatus(status.value());
        return VIEW_NOVA;
    }

    private static String recusarAlteracao(Model model, Map<String, String> erros, HttpServletResponse resposta,
            HttpStatus status) {
        model.addAttribute("erros", erros);
        resposta.setStatus(status.value());
        return VIEW_DETALHE;
    }

    private static Long idOuNulo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException naoNumerico) {
            throw new RequisicaoInvalidaException("Recurso inválido.");
        }
    }

    private static Map<String, Object> recursoDaTela(RecursoResponse recurso) {
        return Map.of("nome", recurso.nome(), "local", VisaoDasReservas.SEM_LOCAL, "restrito", recurso.restrito());
    }

    /** "Sala 203, 12/11/2026, das 14:00 às 15:30". */
    private String rotuloDoHorario(ReservaResponse reserva) {
        return FormatoDasTelas.recursos(reserva) + ", " + visao.formato().textoData(reserva.inicio()) + ", das "
                + visao.formato().horario(reserva.inicio(), reserva.fim());
    }

    private Map<String, Object> detalheDaTela(ReservaResponse reserva, UsuarioAutenticado quem) {
        FormatoDasTelas formato = visao.formato();
        Map<String, Object> detalhe = new LinkedHashMap<>();
        detalhe.put("id", reserva.id());
        detalhe.put("recurso", FormatoDasTelas.recursos(reserva));
        detalhe.put("local", VisaoDasReservas.SEM_LOCAL);
        detalhe.put("data", formato.data(reserva.inicio()));
        detalhe.put("inicio", formato.hora(reserva.inicio()));
        detalhe.put("termino", formato.hora(reserva.fim()));
        detalhe.put("estado", FormatoDasTelas.estado(reserva.estado()));
        detalhe.put("iniciada", formato.iniciada(reserva));
        // Alterar e cancelar são só do dono (posse no service); quem vê a reserva de outra pessoa não recebe os botões
        detalhe.put("dono", quem.id().equals(reserva.solicitanteId()));
        if (reserva.estado() == EstadoReserva.REJEITADA) {
            detalhe.put("motivo", motivoDaRejeicao(reserva.id()));
        }
        return detalhe;
    }

    /** O motivo que o Responsável escreveu ao rejeitar (RN-09), lido do histórico da própria reserva. */
    private String motivoDaRejeicao(Long id) {
        List<AuditoriaResponse> eventos = reservas.historico(id);
        for (int i = eventos.size() - 1; i >= 0; i--) {
            if (eventos.get(i).estadoNovo() == EstadoReserva.REJEITADA) {
                return eventos.get(i).motivo();
            }
        }
        return null;
    }
}
