package br.com.senac.recursos.service;

import br.com.senac.recursos.converter.RecursoConverter;
import br.com.senac.recursos.dto.request.RecursoAtualizacaoRequest;
import br.com.senac.recursos.dto.request.RecursoRequest;
import br.com.senac.recursos.dto.response.RecursoResponse;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.exception.EntidadeNaoEncontradaException;
import br.com.senac.recursos.model.Periodo;
import br.com.senac.recursos.model.Recurso;
import br.com.senac.recursos.repository.RecursoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro e consulta de salas, materiais e professores (RF-02) e disponibilidade (RF-03). */
@Service
public class RecursoService {

    private final RecursoRepository recursos;
    private final RecursoConverter conversor;

    public RecursoService(RecursoRepository recursos, RecursoConverter conversor) {
        this.recursos = recursos;
        this.conversor = conversor;
    }

    @Transactional
    public RecursoResponse criar(RecursoRequest pedido) {
        return conversor.paraResposta(recursos.save(conversor.paraModelo(pedido)));
    }

    @Transactional
    public RecursoResponse atualizar(Long id, RecursoAtualizacaoRequest pedido) {
        Recurso recurso = buscarEntidade(id);
        recurso.atualizar(pedido.nome().trim(), pedido.capacidade(), pedido.restrito());
        return conversor.paraResposta(recurso);
    }

    @Transactional(readOnly = true)
    public RecursoResponse buscar(Long id) {
        return conversor.paraResposta(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<RecursoResponse> listar(TipoRecurso tipo, Integer capacidadeMinima) {
        return recursos.buscar(tipo, capacidadeMinima).stream().map(conversor::paraResposta).toList();
    }

    /**
     * Recursos livres no período: sem reserva que ocupe o horário (D5) e sem manutenção. Resultado informativo; quem
     * garante a reserva é a constraint do banco, no momento da gravação.
     */
    @Transactional(readOnly = true)
    public List<RecursoResponse> disponiveis(Periodo periodo, TipoRecurso tipo, Integer capacidadeMinima) {
        return recursos.buscarDisponiveis(tipo == null ? null : tipo.name(), capacidadeMinima, periodo.comoTstzrange())
                .stream().map(conversor::paraResposta).toList();
    }

    Recurso buscarEntidade(Long id) {
        return recursos.findById(id).orElseThrow(() -> new EntidadeNaoEncontradaException("Recurso", id));
    }
}
