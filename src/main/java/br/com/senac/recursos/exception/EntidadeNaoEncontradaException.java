package br.com.senac.recursos.exception;

/** 404: o identificador pedido não existe. */
public class EntidadeNaoEncontradaException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public EntidadeNaoEncontradaException(String entidade, Object id) {
        super(entidade + " " + id + " não encontrado(a).");
    }

    @Override
    public String tipo() {
        return "nao-encontrado";
    }
}
