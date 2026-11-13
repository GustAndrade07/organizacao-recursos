package br.com.senac.recursos.exception;

/**
 * 401 no login. A mesma mensagem para e-mail inexistente e senha errada, para a resposta não revelar quais e-mails
 * têm conta.
 */
public class CredenciaisInvalidasException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos.");
    }

    @Override
    public String tipo() {
        return "credenciais-invalidas";
    }
}
