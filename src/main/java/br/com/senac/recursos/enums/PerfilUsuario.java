package br.com.senac.recursos.enums;

/**
 * Perfis de acesso (RF-01). Os nomes são os mesmos da constraint {@code usuario_perfil_valido} da V1 e viajam no
 * token JWT como a claim {@code perfil}; o Spring Security os enxerga como {@code ROLE_<nome>}.
 */
public enum PerfilUsuario {

    /** Professor ou coordenador: consulta disponibilidade e cria, altera e cancela as próprias reservas. */
    SOLICITANTE,

    /** Aprova recurso restrito, registra retirada e devolução, consulta histórico e relatório. */
    RESPONSAVEL,

    /** Mantém recursos, usuários e manutenções; consulta histórico e relatório. */
    ADMINISTRADOR
}
