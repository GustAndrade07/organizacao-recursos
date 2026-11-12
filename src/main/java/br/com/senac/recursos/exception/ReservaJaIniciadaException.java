package br.com.senac.recursos.exception;

/** 409: reserva cujo horário já começou não pode ser cancelada nem alterada (RN-07). */
public class ReservaJaIniciadaException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public ReservaJaIniciadaException() {
        super("A reserva já começou e não pode mais ser cancelada nem alterada.");
    }

    @Override
    public String tipo() {
        return "reserva-ja-iniciada";
    }
}
