package br.com.senac.recursos.converter;

import br.com.senac.recursos.dto.request.PeriodoRequest;
import br.com.senac.recursos.dto.request.ReservaRequest;
import br.com.senac.recursos.model.Periodo;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

/**
 * Datas da requisição (ISO-8601 com deslocamento) para {@link Periodo} em UTC. O construtor de {@code Periodo} aplica
 * RN-01 (término depois do início) e devolve 400 se ela for ferida.
 */
@Component
public class PeriodoConverter {

    public Periodo paraModelo(PeriodoRequest pedido) {
        return paraModelo(pedido.inicio(), pedido.fim());
    }

    public Periodo paraModelo(ReservaRequest pedido) {
        return paraModelo(pedido.inicio(), pedido.fim());
    }

    public Periodo paraModelo(OffsetDateTime inicio, OffsetDateTime fim) {
        return new Periodo(inicio == null ? null : inicio.toInstant(), fim == null ? null : fim.toInstant());
    }
}
