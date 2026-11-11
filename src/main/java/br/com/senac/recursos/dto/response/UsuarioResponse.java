package br.com.senac.recursos.dto.response;

import br.com.senac.recursos.enums.PerfilUsuario;

/** Dados públicos do usuário: nunca a senha nem o hash. */
public record UsuarioResponse(Long id, String nome, String login, PerfilUsuario perfil) {
}
