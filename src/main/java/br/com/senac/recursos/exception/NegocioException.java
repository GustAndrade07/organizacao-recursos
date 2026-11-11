package br.com.senac.recursos.exception;

/**
 * Base das exceções de regra de negócio. A mensagem é escrita para o usuário final (vai no {@code detail} do
 * {@code ProblemDetail}); nunca carrega SQL, stack trace ou nome de classe interna. A tradução em status HTTP fica em
 * um só lugar: {@code controller.handler.TratadorDeErros}.
 */
public abstract class NegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected NegocioException(String mensagem) {
        super(mensagem);
    }

    protected NegocioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    /** Identificador estável do tipo de problema, usado no {@code type} do ProblemDetail. */
    public abstract String tipo();
}
