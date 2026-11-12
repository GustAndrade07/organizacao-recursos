package br.com.senac.recursos.exception;

/** 409: já existe usuário com o login pedido (constraint UNIQUE de {@code usuario.login}). */
public class LoginEmUsoException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public LoginEmUsoException(String login) {
        super("O login '" + login + "' já está em uso.");
    }

    @Override
    public String tipo() {
        return "login-em-uso";
    }
}
