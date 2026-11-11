package br.com.senac.recursos.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Informe o login.") String login,
        @NotBlank(message = "Informe a senha.") String senha) {

    /** A senha nunca aparece em log nem em mensagem de erro. */
    @Override
    public String toString() {
        return "LoginRequest[login=" + login + ", senha=***]";
    }
}
