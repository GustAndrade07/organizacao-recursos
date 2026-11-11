package br.com.senac.recursos.dto;

import br.com.senac.recursos.enums.PerfilUsuario;

/**
 * Quem está fazendo a requisição, lido do token JWT já validado pelo Spring Security. Os serviços recebem este valor,
 * e não o token, para as regras de posse (RN-06, reserva de outro Solicitante) não dependerem da camada web.
 */
public record UsuarioAutenticado(Long id, String login, PerfilUsuario perfil) {

    /** Responsável e Administrador enxergam todas as reservas; o Solicitante só as próprias. */
    public boolean veTodasAsReservas() {
        return perfil == PerfilUsuario.RESPONSAVEL || perfil == PerfilUsuario.ADMINISTRADOR;
    }
}
