package br.com.senac.recursos.enums;

/**
 * Tipos de recurso reservável (RF-02). O professor também é um recurso (D6, ADR-0007): assim a mesma constraint de
 * exclusão protege a agenda da sala, do material e do docente (RN-02). Nomes iguais aos da constraint
 * {@code recurso_tipo_valido} da V1.
 */
public enum TipoRecurso {
    SALA,
    MATERIAL,
    PROFESSOR
}
