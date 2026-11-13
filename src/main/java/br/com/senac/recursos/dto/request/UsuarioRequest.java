package br.com.senac.recursos.dto.request;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.validation.EmailDeLogin;
import br.com.senac.recursos.validation.Emails;
import br.com.senac.recursos.validation.NomeCompleto;
import br.com.senac.recursos.validation.SemEmoji;
import br.com.senac.recursos.validation.Textos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de usuário pelo Administrador (DEF-010): nome completo, e-mail (que é o login; o campo continua se
 * chamando {@code login}) e senha, mais o perfil. Sem campo de repetir a senha. O pedido normaliza o que recebe: o
 * e-mail fica em minúsculas e sem espaços nas pontas, o nome sem espaços repetidos; assim a API e a tela têm o mesmo
 * comportamento. O limite de 72 caracteres da senha é o do BCrypt.
 */
public record UsuarioRequest(
        @NotBlank(message = "Informe o nome completo.")
        @SemEmoji
        @NomeCompleto
        String nome,

        @NotBlank(message = "Informe o e-mail.")
        @SemEmoji
        @EmailDeLogin
        String login,

        @NotBlank(message = "Informe a senha.")
        @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
        String senha,

        @NotNull(message = "Informe o perfil.")
        PerfilUsuario perfil) {

    public UsuarioRequest {
        nome = Textos.normalizarNome(nome);
        login = Emails.normalizar(login);
    }

    /** A senha nunca aparece em log nem em mensagem de erro. */
    @Override
    public String toString() {
        return "UsuarioRequest[nome=" + nome + ", login=" + login + ", senha=***, perfil=" + perfil + "]";
    }
}
