package br.com.senac.recursos.web;

import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.dto.request.RecursoAtualizacaoRequest;
import br.com.senac.recursos.dto.request.RecursoRequest;
import br.com.senac.recursos.dto.response.ManutencaoResponse;
import br.com.senac.recursos.dto.response.RecursoResponse;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.model.Periodo;
import br.com.senac.recursos.service.ManutencaoService;
import br.com.senac.recursos.service.RecursoService;
import br.com.senac.recursos.service.ReservaService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
 * Telas 13, 14 e 15 do inventário, só do Administrador (matriz): lista de recursos, formulário de recurso e bloqueio
 * por manutenção (RF-02, RF-08), ligadas a {@link RecursoService} e {@link ManutencaoService}.
 *
 * <p><b>O que o esquema ainda não guarda:</b> localização, patrimônio, competências e o motivo do bloqueio (V2, ver
 * {@code model.Recurso}). A tela continua mostrando esses campos, mas o que for digitado neles não é gravado; a lista
 * mostra "Não informado". Por isso só nome e capacidade (sala) são obrigatórios aqui.
 *
 * <p>Bloqueio: antes de gravar, a tela lista as reservas que ocupam horário (D5) no período e pede confirmação; o
 * {@code POST} com {@code confirmar=true} recalcula a lista. O que acontece com essas reservas é a D11 (hoje o service
 * as mantém). Professor não é bloqueável (a lista não oferece o link e o {@code POST} é recusado).
 */
@Controller
public class RecursoController {

    private static final String VIEW_FORM = "recurso-form";
    private static final String VIEW_MANUTENCAO = "manutencao";
    private static final int NOME_MAXIMO = 120;

    private final RecursoService recursos;
    private final ManutencaoService manutencoes;
    private final ReservaService reservas;
    private final VisaoDasReservas visao;

    public RecursoController(RecursoService recursos, ManutencaoService manutencoes, ReservaService reservas,
            VisaoDasReservas visao) {
        this.recursos = recursos;
        this.manutencoes = manutencoes;
        this.reservas = reservas;
        this.visao = visao;
    }

    /** Linha da tela 13. {@code capacidade} e {@code periodoManutencao} já em texto. */
    public record Linha(Long id, String nome, String capacidade, String local, String patrimonio, String competencias,
            boolean emManutencao, String periodoManutencao, boolean restrito) {
    }

    // ---------------------------------------------------------------- tela 13

    @GetMapping("/admin/recursos")
    public String lista(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String nome,
            Model model) {
        TipoRecurso aba = TipoRecurso.da(tipo);
        String busca = vazioParaNulo(nome);
        model.addAttribute("tipo", aba);
        model.addAttribute("nome", busca);
        Instant agora = visao.formato().agora();
        model.addAttribute("recursos", recursos.listar(paraModelo(aba), null).stream()
                .filter(recurso -> busca == null || recurso.nome().toLowerCase(Locale.ROOT)
                        .contains(busca.trim().toLowerCase(Locale.ROOT)))
                .map(recurso -> linha(recurso, agora))
                .toList());
        return "recursos";
    }

    // ---------------------------------------------------------------- tela 14

    /**
     * Também recebe o que já foi digitado: a troca de tipo recarrega a página por GET. Tipo desconhecido ou
     * {@code restrito} que não seja booleano respondem 400.
     */
    @GetMapping("/admin/recursos/novo")
    public String novo(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String capacidade,
            @RequestParam(required = false) String localizacao,
            @RequestParam(required = false) String patrimonio,
            @RequestParam(required = false) String competencias,
            @RequestParam(defaultValue = "false") boolean restrito,
            Model model) {
        model.addAttribute("tipo", TipoRecurso.exigir(tipo));
        model.addAttribute("recurso", new FormularioRecurso(null, nome, capacidade, localizacao, patrimonio,
                competencias, restrito));
        return VIEW_FORM;
    }

    @GetMapping("/admin/recursos/{id}")
    public String editar(@PathVariable long id, @RequestParam(required = false) String tipo, Model model) {
        TipoRecurso.exigir(tipo);
        RecursoResponse recurso = recursos.buscar(id);
        model.addAttribute("tipo", daAba(recurso.tipo()));
        model.addAttribute("recurso", new FormularioRecurso(String.valueOf(recurso.id()), recurso.nome(),
                recurso.capacidade() == null ? null : String.valueOf(recurso.capacidade()), null, null, null,
                recurso.restrito()));
        return VIEW_FORM;
    }

    @PostMapping("/admin/recursos")
    public String cadastrar(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String capacidade,
            @RequestParam(required = false) String localizacao,
            @RequestParam(required = false) String patrimonio,
            @RequestParam(required = false) String competencias,
            @RequestParam(defaultValue = "false") boolean restrito,
            Model model, RedirectAttributes redirecionamento, HttpServletResponse resposta) {
        TipoRecurso aba = TipoRecurso.exigir(tipo);
        boolean podeSerRestrito = aba != TipoRecurso.PROFESSORES && restrito;
        FormularioRecurso formulario = new FormularioRecurso(null, nome, capacidade, localizacao, patrimonio,
                competencias, podeSerRestrito);
        Map<String, String> erros = new LinkedHashMap<>();
        Integer lugares = validar(aba, nome, capacidade, erros);
        if (!erros.isEmpty()) {
            return recusar(model, aba, formulario, erros, resposta);
        }
        RecursoResponse criado = recursos.criar(new RecursoRequest(paraModelo(aba), nome.trim(), lugares,
                podeSerRestrito));
        return voltarParaLista(aba, "Recurso cadastrado", criado.nome() + " foi cadastrado.", redirecionamento);
    }

    /** Na edição o tipo do formulário é ignorado: vale o tipo gravado. */
    @PostMapping("/admin/recursos/{id}")
    public String atualizar(@PathVariable long id,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String capacidade,
            @RequestParam(required = false) String localizacao,
            @RequestParam(required = false) String patrimonio,
            @RequestParam(required = false) String competencias,
            @RequestParam(defaultValue = "false") boolean restrito,
            Model model, RedirectAttributes redirecionamento, HttpServletResponse resposta) {
        RecursoResponse atual = recursos.buscar(id);
        TipoRecurso aba = daAba(atual.tipo());
        boolean podeSerRestrito = aba != TipoRecurso.PROFESSORES && restrito;
        FormularioRecurso formulario = new FormularioRecurso(String.valueOf(id), nome, capacidade, localizacao,
                patrimonio, competencias, podeSerRestrito);
        Map<String, String> erros = new LinkedHashMap<>();
        Integer lugares = validar(aba, nome, capacidade, erros);
        if (!erros.isEmpty()) {
            return recusar(model, aba, formulario, erros, resposta);
        }
        RecursoResponse salvo = recursos.atualizar(id, new RecursoAtualizacaoRequest(nome.trim(),
                aba == TipoRecurso.SALAS ? lugares : atual.capacidade(), podeSerRestrito));
        return voltarParaLista(aba, "Recurso atualizado", salvo.nome() + " foi atualizado.", redirecionamento);
    }

    // ---------------------------------------------------------------- tela 15

    /** O {@code tipo} só serve para a tela voltar à aba certa da lista. */
    @GetMapping("/admin/recursos/{id}/manutencao")
    public String manutencao(
            @PathVariable long id,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) String motivo,
            Model model) {
        model.addAttribute("tipo", TipoRecurso.exigir(tipo));
        model.addAttribute("recurso", resumo(recursos.buscar(id)));
        model.addAttribute("bloqueio", new FormularioBloqueio(String.valueOf(id), inicio, fim, motivo));
        return VIEW_MANUTENCAO;
    }

    @PostMapping("/admin/recursos/{id}/manutencao")
    public String bloquear(
            @PathVariable long id,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) String motivo,
            @RequestParam(defaultValue = "false") boolean confirmar,
            UsuarioAutenticado quem, Model model, RedirectAttributes redirecionamento, HttpServletResponse resposta) {
        TipoRecurso aba = TipoRecurso.exigir(tipo);
        RecursoResponse recurso = recursos.buscar(id);
        model.addAttribute("tipo", aba);
        model.addAttribute("recurso", resumo(recurso));
        model.addAttribute("bloqueio", new FormularioBloqueio(String.valueOf(id), inicio, fim, motivo));

        Map<String, String> erros = new LinkedHashMap<>();
        if (recurso.tipo() == br.com.senac.recursos.enums.TipoRecurso.PROFESSOR) {
            erros.put("geral", "Professor não é bloqueado por manutenção.");
        }
        if (inicio == null) {
            erros.put("inicio", "Informe o início do bloqueio.");
        }
        if (fim == null) {
            erros.put("fim", "Informe o fim do bloqueio.");
        } else if (inicio != null && !fim.isAfter(inicio)) {
            erros.put("fim", "O fim do bloqueio precisa ser depois do início.");
        }
        if (!erros.isEmpty()) {
            model.addAttribute("erros", erros);
            resposta.setStatus(HttpStatus.BAD_REQUEST.value());
            return VIEW_MANUTENCAO;
        }
        Instant de = visao.formato().instante(inicio);
        Instant ate = visao.formato().instante(fim);
        List<Map<String, Object>> afetadas = afetadas(id, de, ate, quem);
        if (!afetadas.isEmpty() && !confirmar) {
            model.addAttribute("afetadas", afetadas);
            return VIEW_MANUTENCAO;
        }
        manutencoes.criar(id, new Periodo(de, ate));
        return voltarParaLista(aba, "Bloqueio registrado", recurso.nome() + " fica em manutenção de "
                + visao.formato().periodoEmDias(de, ate) + ".", redirecionamento);
    }

    // ---------------------------------------------------------------- apoio

    private Linha linha(RecursoResponse recurso, Instant agora) {
        Optional<ManutencaoResponse> atual = recurso.tipo() == br.com.senac.recursos.enums.TipoRecurso.PROFESSOR
                ? Optional.empty()
                : manutencoes.listar(recurso.id()).stream()
                        .filter(m -> !agora.isBefore(m.inicio()) && agora.isBefore(m.fim()))
                        .findFirst();
        return new Linha(recurso.id(), recurso.nome(), FormatoDasTelas.capacidade(recurso.capacidade()),
                VisaoDasReservas.SEM_LOCAL, VisaoDasReservas.SEM_LOCAL, VisaoDasReservas.SEM_LOCAL,
                atual.isPresent(),
                atual.map(m -> visao.formato().periodoEmDias(m.inicio(), m.fim())).orElse(null),
                recurso.restrito());
    }

    private Map<String, Object> resumo(RecursoResponse recurso) {
        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("id", recurso.id());
        resumo.put("nome", recurso.nome());
        String tipo = FormatoDasTelas.tipo(recurso.tipo());
        resumo.put("tipo", recurso.capacidade() == null ? tipo
                : tipo + ", " + FormatoDasTelas.capacidade(recurso.capacidade()));
        resumo.put("local", VisaoDasReservas.SEM_LOCAL);
        Instant agora = visao.formato().agora();
        resumo.put("emManutencao", manutencoes.listar(recurso.id()).stream()
                .anyMatch(m -> !agora.isBefore(m.inicio()) && agora.isBefore(m.fim())));
        return resumo;
    }

    /** Reservas que ocupam horário (D5) neste recurso e cruzam o período do bloqueio. */
    private List<Map<String, Object>> afetadas(long recursoId, Instant de, Instant ate, UsuarioAutenticado quem) {
        Map<Long, String> nomes = visao.nomes();
        FormatoDasTelas formato = visao.formato();
        return reservas.listar(null, quem).stream()
                .filter(reserva -> reserva.estado().ocupaHorario())
                .filter(reserva -> reserva.recursos().stream().anyMatch(r -> r.id() == recursoId))
                .filter(reserva -> reserva.inicio().isBefore(ate) && de.isBefore(reserva.fim()))
                .sorted(java.util.Comparator.comparing(ReservaResponse::inicio))
                .map(reserva -> {
                    Map<String, Object> linha = new LinkedHashMap<>();
                    linha.put("data", formato.data(reserva.inicio()));
                    linha.put("horario", formato.horario(reserva.inicio(), reserva.fim()));
                    linha.put("solicitante", visao.nomeDe(reserva.solicitanteId(), nomes));
                    linha.put("estado", FormatoDasTelas.estado(reserva.estado()));
                    return linha;
                })
                .toList();
    }

    /** Nome obrigatório (até 120 caracteres); sala exige capacidade inteira positiva. Devolve a capacidade lida. */
    private static Integer validar(TipoRecurso aba, String nome, String capacidade, Map<String, String> erros) {
        if (nome == null || nome.isBlank()) {
            erros.put("nome", "Informe o nome.");
        } else if (nome.trim().length() > NOME_MAXIMO) {
            erros.put("nome", "O nome pode ter no máximo " + NOME_MAXIMO + " caracteres.");
        }
        if (aba != TipoRecurso.SALAS) {
            return null;
        }
        if (capacidade == null || capacidade.isBlank()) {
            erros.put("capacidade", "Informe a capacidade.");
            return null;
        }
        try {
            int lugares = Integer.parseInt(capacidade.trim());
            if (lugares > 0) {
                return lugares;
            }
        } catch (NumberFormatException naoNumero) {
            // cai na mensagem abaixo
        }
        erros.put("capacidade", "A capacidade precisa ser um número inteiro maior que zero.");
        return null;
    }

    private static String recusar(Model model, TipoRecurso aba, FormularioRecurso formulario,
            Map<String, String> erros, HttpServletResponse resposta) {
        model.addAttribute("tipo", aba);
        model.addAttribute("recurso", formulario);
        model.addAttribute("erros", erros);
        resposta.setStatus(HttpStatus.BAD_REQUEST.value());
        return VIEW_FORM;
    }

    private static String voltarParaLista(TipoRecurso aba, String titulo, String texto,
            RedirectAttributes redirecionamento) {
        redirecionamento.addFlashAttribute("confirmacao", Map.of("titulo", titulo, "texto", texto));
        redirecionamento.addAttribute("tipo", aba.getChave());
        return "redirect:/admin/recursos";
    }

    private static br.com.senac.recursos.enums.TipoRecurso paraModelo(TipoRecurso aba) {
        return switch (aba) {
            case SALAS -> br.com.senac.recursos.enums.TipoRecurso.SALA;
            case MATERIAIS -> br.com.senac.recursos.enums.TipoRecurso.MATERIAL;
            case PROFESSORES -> br.com.senac.recursos.enums.TipoRecurso.PROFESSOR;
        };
    }

    private static TipoRecurso daAba(br.com.senac.recursos.enums.TipoRecurso tipo) {
        return switch (tipo) {
            case SALA -> TipoRecurso.SALAS;
            case MATERIAL -> TipoRecurso.MATERIAIS;
            case PROFESSOR -> TipoRecurso.PROFESSORES;
        };
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
