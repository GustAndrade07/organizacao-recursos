package br.com.senac.recursos.dto.request;

import br.com.senac.recursos.enums.PerfilUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cadastro de usuário pelo Administrador. O limite de 72 caracteres da senha é o do BCrypt. */
public record UsuarioRequest(
        @NotBlank(message = "Informe o nome.")
        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
        String nome,

        @NotBlank(message = "Informe o login.")
        @Pattern(regexp = "[a-z0-9._-]{3,60}",
                message = "O login deve ter de 3 a 60 caracteres: letras minúsculas, números, ponto, hífen ou sublinhado.")
        String login,

        @NotBlank(message = "Informe a senha.")
        @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
        String senha,

        @NotNull(message = "Informe o perfil.")
        PerfilUsuario perfil) {

    /** A senha nunca aparece em log nem em mensagem de erro. */
    @Override
    public String toString() {
        return "UsuarioRequest[nome=" + nome + ", login=" + login + ", senha=***, perfil=" + perfil + "]";
    }
}
