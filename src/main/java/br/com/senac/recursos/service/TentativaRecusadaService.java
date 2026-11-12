package br.com.senac.recursos.service;

import br.com.senac.recursos.enums.MotivoTentativa;
import br.com.senac.recursos.model.Periodo;
import br.com.senac.recursos.model.TentativaRecusada;
import br.com.senac.recursos.repository.RecursoRepository;
import br.com.senac.recursos.repository.ReservaRecursoRepository;
import br.com.senac.recursos.repository.TentativaRecusadaRepository;
import br.com.senac.recursos.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava o pedido recusado por sobreposição (D2), fonte dos "conflitos evitados" do relatório (RF-12).
 *
 * <p>Roda em transação própria ({@link Propagation#REQUIRES_NEW}): o pedido que falhou já sofreu rollback, e a
 * tentativa precisa sobreviver a ele.
 */
@Service
public class TentativaRecusadaService {

    private final TentativaRecusadaRepository tentativas;
    private final ReservaRecursoRepository ocupacoes;
    private final RecursoRepository recursos;
    private final UsuarioRepository usuarios;
    private final Clock relogio;

    public TentativaRecusadaService(TentativaRecusadaRepository tentativas, ReservaRecursoRepository ocupacoes,
            RecursoRepository recursos, UsuarioRepository usuarios, Clock relogio) {
        this.tentativas = tentativas;
        this.ocupacoes = ocupacoes;
        this.recursos = recursos;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    /**
     * Uma linha por recurso que estava ocupado. Se a reserva que causou o conflito já tiver sumido (cancelada entre a
     * recusa e esta consulta), grava para todos os recursos pedidos: o pedido foi recusado de qualquer forma.
     *
     * @param ignorarReservaId reserva sendo alterada, ou {@code 0} na criação
     * @return recursos para os quais a tentativa foi gravada (os ocupados)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> registrarSobreposicao(Long solicitanteId, List<Long> recursoIds, Periodo periodo,
            long ignorarReservaId) {
        List<Long> ocupados = ocupacoes.recursosOcupados(recursoIds, periodo.comoTstzrange(), ignorarReservaId);
        List<Long> alvo = ocupados.isEmpty() ? recursoIds : ocupados;
        Instant agora = relogio.instant();
        alvo.forEach(recursoId -> tentativas.save(new TentativaRecusada(recursos.getReferenceById(recursoId),
                usuarios.getReferenceById(solicitanteId), periodo, MotivoTentativa.SOBREPOSICAO, agora)));
        return alvo;
    }
}
