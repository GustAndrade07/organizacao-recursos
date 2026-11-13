package br.com.senac.recursos.validation;

/** Esqueleto do TDD: regras de texto dos cadastros (emoji, nome completo). */
public final class Textos {

    private Textos() {
    }

    /** O texto tem emoji? */
    public static boolean contemEmoji(String texto) {
        return false;
    }

    /** Nome sem espaços nas pontas e com espaços repetidos reduzidos a um; nulo continua nulo. */
    public static String normalizarNome(String nome) {
        return nome;
    }

    /** O nome, já normalizado, é um nome completo (proposta da spec)? */
    public static boolean nomeCompleto(String normalizado) {
        return true;
    }
}
