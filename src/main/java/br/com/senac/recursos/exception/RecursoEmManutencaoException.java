package br.com.senac.recursos.exception;

import java.util.List;

/** 409: pelo menos um recurso pedido está em manutenção no período (RN-04, RF-08). */
public class RecursoEmManutencaoException extends NegocioException {

    private static final long serialVersionUID = 1L;

    private final List<Long> recursoIds;

    public RecursoEmManutencaoException(List<Long> recursoIds) {
        super("Pelo menos um dos recursos está em manutenção no período pedido.");
        this.recursoIds = List.copyOf(recursoIds);
    }

    public List<Long> recursoIds() {
        return recursoIds;
    }

    @Override
    public String tipo() {
        return "recurso-em-manutencao";
    }
}
