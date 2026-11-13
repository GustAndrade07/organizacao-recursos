package br.com.senac.recursos.validation;

import java.util.Locale;
import java.util.regex.Pattern;

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

    private static final Pattern PADRAO = Pattern.compile(
            "[a-z0-9_%+-]+(\\.[a-z0-9_%+-]+)*@([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}");

    private Emails() {
    }

    /** Sem espaços nas pontas e em minúsculas ({@code Locale.ROOT}); nulo continua nulo. */
    public static String normalizar(String valor) {
        return valor == null ? null : valor.strip().toLowerCase(Locale.ROOT);
    }

    /** O valor, já normalizado, tem o formato de um e-mail de login? */
    public static boolean valido(String normalizado) {
        return normalizado != null
                && normalizado.length() <= TAMANHO_MAXIMO
                && PADRAO.matcher(normalizado).matches()
                && normalizado.indexOf('@') <= PARTE_LOCAL_MAXIMA;
    }
}
