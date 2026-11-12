package br.com.senac.recursos.support;

import br.com.senac.recursos.dto.response.RecursoResponse;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.enums.EstadoReserva;
import br.com.senac.recursos.enums.TipoRecurso;
import java.time.Instant;
import java.util.List;

/** Respostas de exemplo dos services, para os recortes de tela que precisam de uma rota real com dados. */
public final class Respostas {

    private Respostas() {
    }

    public static RecursoResponse sala(long id, String nome) {
        return new RecursoResponse(id, TipoRecurso.SALA, nome, 40, false);
    }

    public static RecursoResponse recurso(long id, TipoRecurso tipo, String nome, boolean restrito) {
        return new RecursoResponse(id, tipo, nome, tipo == TipoRecurso.SALA ? 40 : null, restrito);
    }

    /** Reserva de 12/11/2026, 14:00 às 15:30 em São Paulo (17:00 a 18:30 em UTC). */
    public static ReservaResponse reserva(long id, long solicitanteId, EstadoReserva estado, RecursoResponse recurso) {
        return new ReservaResponse(id, solicitanteId, estado, Instant.parse("2026-11-12T17:00:00Z"),
                Instant.parse("2026-11-12T18:30:00Z"), Instant.parse("2026-11-01T12:00:00Z"), List.of(recurso));
    }
}
