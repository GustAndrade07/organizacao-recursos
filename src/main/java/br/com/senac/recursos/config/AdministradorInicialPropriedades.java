package br.com.senac.recursos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Primeiro administrador ({@code recursos.administrador-inicial}). Sem ele ninguém consegue cadastrar o primeiro
 * usuário, porque o cadastro exige um Administrador. Vem só de variável de ambiente ({@code ADMIN_LOGIN},
 * {@code ADMIN_SENHA}); sem as duas, nada é criado.
 */
@ConfigurationProperties("recursos.administrador-inicial")
public record AdministradorInicialPropriedades(String login, String senha, String nome) {

    public boolean configurado() {
        return login != null && !login.isBlank() && senha != null && !senha.isBlank();
    }

    /** Não imprime a senha em log. */
    @Override
    public String toString() {
        return "AdministradorInicialPropriedades[login=" + login + ", senha=***, nome=" + nome + "]";
    }
}
