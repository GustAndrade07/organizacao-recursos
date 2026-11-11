package br.com.senac.recursos.converter;

import br.com.senac.recursos.dto.response.ManutencaoResponse;
import br.com.senac.recursos.model.Manutencao;
import org.springframework.stereotype.Component;

@Component
public class ManutencaoConverter {

    public ManutencaoResponse paraResposta(Manutencao manutencao) {
        return new ManutencaoResponse(manutencao.getId(), manutencao.getRecurso().getId(),
                manutencao.getPeriodo().inicio(), manutencao.getPeriodo().fim());
    }
}
