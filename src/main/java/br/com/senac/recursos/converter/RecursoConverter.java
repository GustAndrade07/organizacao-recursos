package br.com.senac.recursos.converter;

import br.com.senac.recursos.dto.request.RecursoRequest;
import br.com.senac.recursos.dto.response.RecursoResponse;
import br.com.senac.recursos.model.Recurso;
import org.springframework.stereotype.Component;

@Component
public class RecursoConverter {

    public Recurso paraModelo(RecursoRequest pedido) {
        return new Recurso(pedido.tipo(), pedido.nome().trim(), pedido.capacidade(), pedido.restrito());
    }

    public RecursoResponse paraResposta(Recurso recurso) {
        return new RecursoResponse(recurso.getId(), recurso.getTipo(), recurso.getNome(), recurso.getCapacidade(),
                recurso.isRestrito());
    }
}
