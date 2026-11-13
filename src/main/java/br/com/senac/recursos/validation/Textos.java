package br.com.senac.recursos.validation;

import java.util.regex.Pattern;

/** Regras de texto dos cadastros: emoji (DEF-005) e nome completo (proposta da spec "Login por e-mail"). */
public final class Textos {

    static final int NOME_MAXIMO = 120;

    /** Duas ou mais palavras separadas por um espaço; cada uma começa por letra e só tem letras, marcas, apóstrofo, ponto e hífen. */
    private static final Pattern NOME = Pattern.compile(
            "\\p{L}[\\p{L}\\p{M}'’.-]*( \\p{L}[\\p{L}\\p{M}'’.-]*)+");

    private static final Pattern ESPACOS = Pattern.compile("[\\s\\p{Z}]+");

    private Textos() {
    }

    /**
     * O texto tem emoji? Vale pictograma estendido, apresentação de emoji, indicador regional (bandeiras), seletor de
     * variação 16 e o keycap. Dígitos, {@code #} e {@code *} sozinhos não contam.
     */
    public static boolean contemEmoji(String texto) {
        return texto != null && texto.codePoints().anyMatch(Textos::ehEmoji);
    }

    private static boolean ehEmoji(int ponto) {
        return Character.isExtendedPictographic(ponto)
                || Character.isEmojiPresentation(ponto)
                || (ponto >= 0x1F1E6 && ponto <= 0x1F1FF)
                || ponto == 0xFE0F
                || ponto == 0x20E3;
    }

    /** Nome sem espaços nas pontas e com espaços repetidos reduzidos a um; nulo continua nulo. */
    public static String normalizarNome(String nome) {
        return nome == null ? null : ESPACOS.matcher(nome.strip()).replaceAll(" ");
    }

    /** O nome, já normalizado, é um nome completo (pelo menos duas palavras, até 120 caracteres)? */
    public static boolean nomeCompleto(String normalizado) {
        return normalizado != null && normalizado.length() <= NOME_MAXIMO && NOME.matcher(normalizado).matches();
    }
}
