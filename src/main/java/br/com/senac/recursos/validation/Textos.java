package br.com.senac.recursos.validation;

import java.util.regex.Pattern;

/** Regras de texto dos cadastros: emoji (DEF-005) e nome completo (proposta da spec "Login por e-mail"). */
public final class Textos {

    static final int NOME_MAXIMO = 120;

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

    /**
     * O nome, já normalizado, é um nome completo (pelo menos duas palavras, até 120 caracteres)? Conferido palavra a
     * palavra, sem expressão regular com repetição (regra S5998 do Sonar): palavras separadas por um espaço, cada uma
     * começando por letra e só com letras, marcas (acentos soltos), apóstrofo, ponto e hífen.
     */
    public static boolean nomeCompleto(String normalizado) {
        if (normalizado == null || normalizado.length() > NOME_MAXIMO) {
            return false;
        }
        String[] palavras = normalizado.split(" ", -1);
        if (palavras.length < 2) {
            return false;
        }
        for (String palavra : palavras) {
            if (!palavraDeNome(palavra)) {
                return false;
            }
        }
        return true;
    }

    private static boolean palavraDeNome(String palavra) {
        if (palavra.isEmpty() || !Character.isLetter(palavra.codePointAt(0))) {
            return false;
        }
        return palavra.codePoints().allMatch(c -> Character.isLetter(c) || marca(c) || "'’.-".indexOf(c) >= 0);
    }

    private static boolean marca(int ponto) {
        int tipo = Character.getType(ponto);
        return tipo == Character.NON_SPACING_MARK || tipo == Character.COMBINING_SPACING_MARK
                || tipo == Character.ENCLOSING_MARK;
    }
}
