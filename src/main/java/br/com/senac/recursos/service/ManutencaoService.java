package br.com.senac.recursos.service;

import br.com.senac.recursos.converter.ManutencaoConverter;
import br.com.senac.recursos.dto.response.ManutencaoResponse;
import br.com.senac.recursos.model.Manutencao;
import br.com.senac.recursos.model.Periodo;
import br.com.senac.recursos.model.Recurso;
import br.com.senac.recursos.repository.ManutencaoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bloqueio de recurso por manutenção (RF-08). A partir daqui, nenhuma reserva nova sobrepõe o período (RN-04, conferido
 * em {@link ReservaService}). O que acontece com reservas que já existiam no período é decisão pendente do grupo (D11
 * do plano): hoje elas continuam como estão.
 */
@Service
public class ManutencaoService {

    private final ManutencaoRepository manutencoes;
    private final RecursoService recursos;
    private final ManutencaoConverter conversor;

    public ManutencaoService(ManutencaoRepository manutencoes, RecursoService recursos, ManutencaoConverter conversor) {
        this.manutencoes = manutencoes;
        this.recursos = recursos;
        this.conversor = conversor;
    }

    @Transactional
    public ManutencaoResponse criar(Long recursoId, Periodo periodo) {
        Recurso recurso = recursos.buscarEntidade(recursoId);
        return conversor.paraResposta(manutencoes.save(new Manutencao(recurso, periodo)));
    }

    @Transactional(readOnly = true)
    public List<ManutencaoResponse> listar(Long recursoId) {
        recursos.buscarEntidade(recursoId);
        return manutencoes.findByRecurso_IdOrderByIdAsc(recursoId).stream().map(conversor::paraResposta).toList();
    }
}
