package br.com.senac.recursos.exception;

/**
 * 401 no login. A mesma mensagem para login inexistente e senha errada, para a resposta não revelar quais logins
 * existem.
 */
public class CredenciaisInvalidasException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public CredenciaisInvalidasException() {
        super("Login ou senha inválidos.");
    }

    @Override
    public String tipo() {
        return "credenciais-invalidas";
    }
}
