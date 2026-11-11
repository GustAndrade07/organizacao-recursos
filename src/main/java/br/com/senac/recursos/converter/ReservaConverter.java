package br.com.senac.recursos.converter;

import br.com.senac.recursos.dto.response.RecursoResponse;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.model.Periodo;
import br.com.senac.recursos.model.Reserva;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReservaConverter {

    private final RecursoConverter recursoConverter;

    public ReservaConverter(RecursoConverter recursoConverter) {
        this.recursoConverter = recursoConverter;
    }

    public ReservaResponse paraResposta(Reserva reserva) {
        Periodo periodo = reserva.getPeriodo();
        List<RecursoResponse> recursos = reserva.getRecursos().stream()
                .map(linha -> recursoConverter.paraResposta(linha.getRecurso()))
                .toList();
        return new ReservaResponse(reserva.getId(), reserva.getSolicitante().getId(), reserva.getEstado(),
                periodo.inicio(), periodo.fim(), reserva.getCriadaEm(), recursos);
    }
}
