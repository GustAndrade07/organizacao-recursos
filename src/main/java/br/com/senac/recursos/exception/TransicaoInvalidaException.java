package br.com.senac.recursos.exception;

import br.com.senac.recursos.enums.EstadoReserva;

/** 409 com tipo próprio (D3): a máquina de estados (RN-05) não permite a transição pedida. */
public class TransicaoInvalidaException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public TransicaoInvalidaException(EstadoReserva atual, EstadoReserva destino) {
        super("A reserva está " + atual + " e não pode passar para " + destino + ".");
    }

    public TransicaoInvalidaException(String mensagem) {
        super(mensagem);
    }

    @Override
    public String tipo() {
        return "transicao-invalida";
    }
}
