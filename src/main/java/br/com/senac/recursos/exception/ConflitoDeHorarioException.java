package br.com.senac.recursos.exception;

import java.util.List;

/**
 * 409: o banco recusou a reserva por sobreposição (RN-02, RN-03), inclusive na disputa entre pedidos simultâneos.
 * Nasce da violação {@code 23P01} da constraint {@code reserva_recurso_sem_sobreposicao} (ADR-0002, ADR-0007).
 */
public class ConflitoDeHorarioException extends NegocioException {

    private static final long serialVersionUID = 1L;

    private final List<Long> recursoIds;

    public ConflitoDeHorarioException(List<Long> recursoIds, Throwable causa) {
        super("O horário pedido já está ocupado para pelo menos um dos recursos.", causa);
        this.recursoIds = List.copyOf(recursoIds);
    }

    public List<Long> recursoIds() {
        return recursoIds;
    }

    @Override
    public String tipo() {
        return "conflito-de-horario";
    }
}
