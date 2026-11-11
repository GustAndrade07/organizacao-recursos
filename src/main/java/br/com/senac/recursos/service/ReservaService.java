package br.com.senac.recursos.service;

import br.com.senac.recursos.converter.ReservaConverter;
import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.dto.response.AuditoriaResponse;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.enums.EstadoReserva;
import br.com.senac.recursos.exception.AcessoNegadoException;
import br.com.senac.recursos.exception.ConflitoDeHorarioException;
import br.com.senac.recursos.exception.EntidadeNaoEncontradaException;
import br.com.senac.recursos.exception.RecursoEmManutencaoException;
import br.com.senac.recursos.exception.RequisicaoInvalidaException;
import br.com.senac.recursos.exception.ReservaJaIniciadaException;
import br.com.senac.recursos.exception.TransicaoInvalidaException;
import br.com.senac.recursos.model.Periodo;
import br.com.senac.recursos.model.Recurso;
import br.com.senac.recursos.model.Reserva;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.repository.ManutencaoRepository;
import br.com.senac.recursos.repository.RecursoRepository;
import br.com.senac.recursos.repository.ReservaRepository;
import br.com.senac.recursos.repository.UsuarioRepository;
import java.sql.SQLException;
import java.time.Clock;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reservas (RF-04 a RF-07, RF-09, RF-10): criar, alterar, cancelar, aprovar e as demais mudanças de estado.
 *
 * <h2>Como a reserva dupla é barrada (RN-02, RN-03, ADR-0002, ADR-0007)</h2>
 * O serviço NÃO consulta "está livre?" antes de gravar: entre a consulta e a gravação outro pedido poderia passar. Ele
 * grava, e a constraint de exclusão do banco recusa a sobreposição com SQLState {@code 23P01}, inclusive entre pedidos
 * simultâneos. Este serviço traduz {@code 23P01} em 409 e grava a tentativa recusada em transação própria (D2). Por isso
 * criar e alterar usam {@link TransactionTemplate}: a tradução precisa acontecer depois do rollback, fora dele.
 *
 * <h2>Quem pode o quê</h2>
 * O perfil é conferido pelo Spring Security (matriz de autorização). A posse é conferida aqui: só o dono altera ou
 * cancela a própria reserva; o Solicitante só enxerga as próprias; ninguém aprova a própria reserva (RN-06).
 */
@Service
public class ReservaService {

    private static final String VIOLACAO_DE_EXCLUSAO = "23P01";

    /** Estados em que o dono ainda pode mudar o período. */
    private static final Set<EstadoReserva> ALTERAVEIS = EnumSet.of(EstadoReserva.SOLICITADA, EstadoReserva.APROVADA);

    private final ReservaRepository reservas;
    private final RecursoRepository recursos;
    private final UsuarioRepository usuarios;
    private final ManutencaoRepository manutencoes;
    private final AuditoriaService auditoria;
    private final TentativaRecusadaService tentativas;
    private final ReservaConverter conversor;
    private final Clock relogio;
    private final TransactionTemplate transacao;

    @SuppressWarnings("java:S107") // dependências explícitas por construtor; agrupá-las esconderia o que o serviço usa
    public ReservaService(ReservaRepository reservas, RecursoRepository recursos, UsuarioRepository usuarios,
            ManutencaoRepository manutencoes, AuditoriaService auditoria, TentativaRecusadaService tentativas,
            ReservaConverter conversor, Clock relogio, PlatformTransactionManager gerenciadorDeTransacao) {
        this.reservas = reservas;
        this.recursos = recursos;
        this.usuarios = usuarios;
        this.manutencoes = manutencoes;
        this.auditoria = auditoria;
        this.tentativas = tentativas;
        this.conversor = conversor;
        this.relogio = relogio;
        this.transacao = new TransactionTemplate(gerenciadorDeTransacao);
    }

    // ---------------------------------------------------------------- criar e alterar (podem dar 23P01)

    /**
     * Cria a reserva (RF-04). Recurso restrito nasce SOLICITADA e espera o Responsável (RF-07); sem recurso restrito,
     * nasce APROVADA (proposta do back-end, pendente de confirmação do grupo: D10 do plano).
     */
    public ReservaResponse criar(List<Long> recursoIds, Periodo periodo, UsuarioAutenticado quem) {
        List<Long> ids = semRepeticao(recursoIds);
        return traduzindoSobreposicao(quem.id(), ids, periodo, 0L, () -> {
            Usuario solicitante = usuario(quem.id());
            List<Recurso> pedidos = carregarRecursos(ids);
            exigirForaDeManutencao(ids, periodo);
            EstadoReserva inicial = pedidos.stream().anyMatch(Recurso::isRestrito)
                    ? EstadoReserva.SOLICITADA
                    : EstadoReserva.APROVADA;
            Reserva reserva = Reserva.nova(solicitante, pedidos, periodo, inicial, relogio.instant());
            reservas.saveAndFlush(reserva);
            auditoria.registrar(reserva, solicitante, null, inicial, inicial == EstadoReserva.SOLICITADA
                    ? "Reserva criada; recurso restrito aguarda aprovação."
                    : "Reserva criada; nenhum recurso restrito.");
            return conversor.paraResposta(reserva);
        });
    }

    /**
     * Troca o período de uma reserva (RF-04). Só o dono, só antes de começar (RN-07) e só em SOLICITADA ou APROVADA.
     * O estado não muda; a alteração entra no histórico com estado anterior igual ao novo.
     */
    public ReservaResponse alterarPeriodo(Long id, Periodo novo, UsuarioAutenticado quem) {
        Reserva atual = transacao.execute(status -> reservaParaLeitura(id));
        exigirDono(atual, quem);
        List<Long> ids = atual.recursoIds();
        return traduzindoSobreposicao(quem.id(), ids, novo, id, () -> {
            Reserva reserva = reservaParaAtualizar(id);
            exigirDono(reserva, quem);
            if (!ALTERAVEIS.contains(reserva.getEstado())) {
                throw new TransicaoInvalidaException("A reserva está " + reserva.getEstado()
                        + " e não pode mais ter o período alterado.");
            }
            exigirNaoIniciada(reserva);
            exigirForaDeManutencao(ids, novo);
            Periodo anterior = reserva.getPeriodo();
            reserva.alterarPeriodo(novo);
            reservas.flush();
            auditoria.registrar(reserva, usuario(quem.id()), reserva.getEstado(), reserva.getEstado(),
                    "Período alterado de " + anterior.comoTstzrange() + " para " + novo.comoTstzrange() + ".");
            return conversor.paraResposta(reserva);
        });
    }

    // ---------------------------------------------------------------- mudanças de estado (RN-05)

    /** Só o dono cancela, e só antes de a reserva começar (RN-07). Cancelar libera o horário (D5). */
    @Transactional
    public ReservaResponse cancelar(Long id, String motivo, UsuarioAutenticado quem) {
        Reserva reserva = reservaParaAtualizar(id);
        exigirDono(reserva, quem);
        exigirNaoIniciada(reserva);
        return mudarEstado(reserva, EstadoReserva.CANCELADA, motivo, quem);
    }

    /** Só Responsável (matriz) e nunca a própria reserva (RN-06). */
    @Transactional
    public ReservaResponse aprovar(Long id, String motivo, UsuarioAutenticado quem) {
        Reserva reserva = reservaParaAtualizar(id);
        if (reserva.pertenceA(quem.id())) {
            throw new AcessoNegadoException("Ninguém aprova a própria reserva.");
        }
        return mudarEstado(reserva, EstadoReserva.APROVADA, motivo, quem);
    }

    @Transactional
    public ReservaResponse rejeitar(Long id, String motivo, UsuarioAutenticado quem) {
        return mudarEstado(reservaParaAtualizar(id), EstadoReserva.REJEITADA, motivo, quem);
    }

    /** Retirada do material ou início do uso (RF-09): APROVADA para EM_USO. */
    @Transactional
    public ReservaResponse registrarRetirada(Long id, String motivo, UsuarioAutenticado quem) {
        return mudarEstado(reservaParaAtualizar(id), EstadoReserva.EM_USO, motivo, quem);
    }

    /** Devolução do material ou fim do uso (RF-09): EM_USO para CONCLUIDA. */
    @Transactional
    public ReservaResponse registrarDevolucao(Long id, String motivo, UsuarioAutenticado quem) {
        return mudarEstado(reservaParaAtualizar(id), EstadoReserva.CONCLUIDA, motivo, quem);
    }

    @Transactional
    public ReservaResponse registrarNaoComparecimento(Long id, String motivo, UsuarioAutenticado quem) {
        return mudarEstado(reservaParaAtualizar(id), EstadoReserva.NAO_COMPARECEU, motivo, quem);
    }

    // ---------------------------------------------------------------- consultas

    /** O Solicitante só enxerga as próprias reservas; Responsável e Administrador, todas. */
    @Transactional(readOnly = true)
    public ReservaResponse buscar(Long id, UsuarioAutenticado quem) {
        Reserva reserva = reservaParaLeitura(id);
        if (!quem.veTodasAsReservas()) {
            exigirDono(reserva, quem);
        }
        return conversor.paraResposta(reserva);
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listar(EstadoReserva estado, UsuarioAutenticado quem) {
        List<Reserva> encontradas = quem.veTodasAsReservas()
                ? reservas.listar(estado)
                : reservas.listarDoSolicitante(quem.id(), estado);
        return encontradas.stream().map(conversor::paraResposta).toList();
    }

    /** Histórico da reserva (RF-10). Só Responsável e Administrador chegam aqui (RN-11, matriz). */
    @Transactional(readOnly = true)
    public List<AuditoriaResponse> historico(Long id) {
        if (!reservas.existsById(id)) {
            throw new EntidadeNaoEncontradaException("Reserva", id);
        }
        return auditoria.historico(id);
    }

    // ---------------------------------------------------------------- apoio

    private ReservaResponse mudarEstado(Reserva reserva, EstadoReserva destino, String motivo,
            UsuarioAutenticado quem) {
        EstadoReserva anterior = reserva.getEstado();
        reserva.mudarEstado(destino);
        reservas.flush();
        auditoria.registrar(reserva, usuario(quem.id()), anterior, destino, motivo);
        return conversor.paraResposta(reserva);
    }

    /**
     * Roda a gravação numa transação; se o banco recusar por sobreposição (23P01), a transação já desfeita, grava a
     * tentativa em transação própria e responde 409.
     */
    private ReservaResponse traduzindoSobreposicao(Long solicitanteId, List<Long> recursoIds, Periodo periodo,
            long reservaAlterada, Supplier<ReservaResponse> gravacao) {
        try {
            return transacao.execute(status -> gravacao.get());
        } catch (DataIntegrityViolationException erro) {
            if (!violouExclusao(erro)) {
                throw erro;
            }
            List<Long> ocupados = tentativas.registrarSobreposicao(solicitanteId, recursoIds, periodo,
                    reservaAlterada);
            throw new ConflitoDeHorarioException(ocupados, erro);
        }
    }

    static boolean violouExclusao(Throwable erro) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql && VIOLACAO_DE_EXCLUSAO.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static List<Long> semRepeticao(List<Long> recursoIds) {
        Set<Long> unicos = new LinkedHashSet<>(recursoIds);
        if (unicos.size() != recursoIds.size()) {
            throw new RequisicaoInvalidaException("O mesmo recurso foi informado mais de uma vez.");
        }
        return List.copyOf(unicos);
    }

    private List<Recurso> carregarRecursos(List<Long> ids) {
        List<Recurso> encontrados = recursos.findAllById(ids);
        if (encontrados.size() != ids.size()) {
            Set<Long> achados = new LinkedHashSet<>();
            encontrados.forEach(recurso -> achados.add(recurso.getId()));
            Long faltando = ids.stream().filter(id -> !achados.contains(id)).findFirst().orElseThrow();
            throw new EntidadeNaoEncontradaException("Recurso", faltando);
        }
        return encontrados;
    }

    /**
     * RN-04. Conferido no serviço, porque a V1 não tem constraint entre manutenção e reserva. É "consulta e depois
     * grava": uma manutenção criada entre esta consulta e o commit da reserva passa. Risco registrado no ADR-0008 e
     * na D13 do plano (aceitar ou proteger no banco), apontado na revisão do PR #44.
     */
    private void exigirForaDeManutencao(List<Long> recursoIds, Periodo periodo) {
        List<Long> emManutencao = manutencoes.recursosEmManutencao(recursoIds, periodo.comoTstzrange());
        if (!emManutencao.isEmpty()) {
            throw new RecursoEmManutencaoException(emManutencao);
        }
    }

    private void exigirNaoIniciada(Reserva reserva) {
        if (reserva.getPeriodo().iniciadoEm(relogio.instant())) {
            throw new ReservaJaIniciadaException();
        }
    }

    private static void exigirDono(Reserva reserva, UsuarioAutenticado quem) {
        if (!reserva.pertenceA(quem.id())) {
            throw new AcessoNegadoException("Só quem fez a reserva pode fazer esta operação.");
        }
    }

    private Usuario usuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário", id));
    }

    private Reserva reservaParaLeitura(Long id) {
        return reservas.findComRecursosById(id).orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva", id));
    }

    private Reserva reservaParaAtualizar(Long id) {
        return reservas.buscarParaAtualizar(id).orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva", id));
    }
}
