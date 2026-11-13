package br.com.senac.recursos.validation;

/** Esqueleto do TDD: o e-mail de login (ver spec "Login por e-mail"). */
public final class Emails {

    public static final int TAMANHO_MAXIMO = 254;

    private Emails() {
    }

    /** Sem espaços nas pontas e em minúsculas; nulo continua nulo. */
    public static String normalizar(String valor) {
        return valor;
    }

    /** O valor, já normalizado, tem o formato de um e-mail de login? */
    public static boolean valido(String normalizado) {
        return true;
    }
}
