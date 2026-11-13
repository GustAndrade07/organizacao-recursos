package br.com.senac.recursos.validation;

import java.util.Locale;

/**
 * O e-mail de login (DEF-010): guardado em minúsculas, sem espaços, até {@link #TAMANHO_MAXIMO} caracteres, num formato
 * simples e estrito (um {@code @}, parte local de até 64 caracteres, domínio com ponto, só ASCII). Não aceita
 * comentário, aspas, IP entre colchetes nem domínio internacionalizado de propósito: é uma regra de cadastro, não um
 * analisador de RFC 5322. A mesma regra está no CHECK {@code usuario_login_e_email} da V2; o teste
 * {@code EsquemaBancoTest} confere que as duas respondem igual.
 */
public final class Emails {

    public static final int TAMANHO_MAXIMO = 254;
    private static final int PARTE_LOCAL_MAXIMA = 64;

    private static final int ROTULO_MAXIMO = 63;

    private Emails() {
    }

    /** Sem espaços nas pontas e em minúsculas ({@code Locale.ROOT}); nulo continua nulo. */
    public static String normalizar(String valor) {
        return valor == null ? null : valor.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * O valor, já normalizado, tem o formato de um e-mail de login? Conferido caractere a caractere, sem expressão
     * regular com repetição (a regra S5998 do Sonar aponta o risco de estouro de pilha nelas). Mesma regra do
     * {@code CHECK} da V2: parte local de {@code [a-z0-9_%+-]} separada por pontos (sem ponto no começo, no fim nem
     * repetido), um só {@code @}, domínio de rótulos separados por ponto ({@code [a-z0-9-]}, de 1 a 63 caracteres,
     * começando e terminando em letra ou dígito) e último rótulo só de letras, de 2 a 63.
     */
    public static boolean valido(String normalizado) {
        if (normalizado == null || normalizado.length() > TAMANHO_MAXIMO) {
            return false;
        }
        int arroba = normalizado.indexOf('@');
        if (arroba < 1 || arroba > PARTE_LOCAL_MAXIMA || normalizado.indexOf('@', arroba + 1) >= 0) {
            return false;
        }
        return parteLocalValida(normalizado.substring(0, arroba)) && dominioValido(normalizado.substring(arroba + 1));
    }

    private static boolean parteLocalValida(String local) {
        for (String parte : local.split("[.]", -1)) {
            if (parte.isEmpty() || !parte.chars().allMatch(c -> letraOuDigito(c) || "_%+-".indexOf(c) >= 0)) {
                return false;
            }
        }
        return true;
    }

    private static boolean dominioValido(String dominio) {
        String[] rotulos = dominio.split("[.]", -1);
        if (rotulos.length < 2) {
            return false;
        }
        for (int i = 0; i < rotulos.length - 1; i++) {
            if (!rotuloValido(rotulos[i])) {
                return false;
            }
        }
        String ultimo = rotulos[rotulos.length - 1];
        return ultimo.length() >= 2 && ultimo.length() <= ROTULO_MAXIMO && ultimo.chars().allMatch(Emails::letra);
    }

    private static boolean rotuloValido(String rotulo) {
        int tamanho = rotulo.length();
        return tamanho >= 1 && tamanho <= ROTULO_MAXIMO
                && letraOuDigito(rotulo.charAt(0)) && letraOuDigito(rotulo.charAt(tamanho - 1))
                && rotulo.chars().allMatch(c -> letraOuDigito(c) || c == '-');
    }

    private static boolean letra(int c) {
        return c >= 'a' && c <= 'z';
    }

    private static boolean letraOuDigito(int c) {
        return letra(c) || (c >= '0' && c <= '9');
    }
}
