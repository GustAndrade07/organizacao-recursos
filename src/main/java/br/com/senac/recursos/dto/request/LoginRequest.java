package br.com.senac.recursos.dto.request;

import br.com.senac.recursos.validation.Emails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada (DEF-010): e-mail e senha. Só se exige preenchimento: e-mail fora do formato dá o mesmo 401 de senha errada,
 * para a resposta não ensinar nada sobre quem tem conta. O e-mail é normalizado (minúsculas, sem espaços nas pontas);
 * a senha vai como veio. Valor enorme é recusado com 400 antes de virar chave do contador de tentativas (não depende de
 * existir conta, então não revela nada).
 */
public record LoginRequest(
        @NotBlank(message = "Informe o e-mail.")
        @Size(max = Emails.TAMANHO_MAXIMO, message = "O e-mail pode ter no máximo 254 caracteres.")
        String login,

        @NotBlank(message = "Informe a senha.")
        @Size(max = 1024, message = "A senha pode ter no máximo 1024 caracteres.")
        String senha) {

    public LoginRequest {
        login = Emails.normalizar(login);
    }

    /** A senha nunca aparece em log nem em mensagem de erro. */
    @Override
    public String toString() {
        return "LoginRequest[login=" + login + ", senha=***]";
    }
}
