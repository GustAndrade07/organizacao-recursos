package br.com.senac.recursos.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Regra única para aceitar um destino ({@code ?destino=} do login, URL original das páginas de erro): só vale
 * caminho deste site. Quem olha só o começo da string aceita {@code "/<TAB>/evil.com"}, que o navegador lê como
 * {@code //evil.com}. Por isso a regra é completa:
 * <ul>
 *   <li>começa com {@code /}, e o segundo caractere não é {@code /} nem {@code \};</li>
 *   <li>não tem {@code \} em posição nenhuma;</li>
 *   <li>não tem caractere de controle, espaço (inclusive Unicode) nem caractere invisível de formatação;</li>
 *   <li>não tem {@code %2f}, {@code %5c} nem {@code %25} (barras codificadas e dupla codificação);</li>
 *   <li>tem no máximo {@value #TAMANHO_MAXIMO} caracteres;</li>
 *   <li>é uma URI válida, sem esquema nem autoridade.</li>
 * </ul>
 * O back-end deve usar a mesma regra ao receber o destino no {@code POST /login}.
 */
public final class CaminhoDoSite {

    static final int TAMANHO_MAXIMO = 2000;

    /** Só para os templates (ver {@link AtributosDasTelas}); a regra em si é estática. */
    CaminhoDoSite() {
    }

    /** {@code true} só se {@code caminho} é um caminho deste site, sem como virar outro endereço. */
    public static boolean valido(String caminho) {
        if (caminho == null || caminho.isEmpty() || caminho.length() > TAMANHO_MAXIMO || caminho.charAt(0) != '/') {
            return false;
        }
        if (caminho.length() > 1 && (caminho.charAt(1) == '/' || caminho.charAt(1) == '\\')) {
            return false;
        }
        for (int i = 0; i < caminho.length(); i++) {
            char c = caminho.charAt(i);
            if (c == '\\' || c <= 0x1F || c == 0x7F || Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                return false;
            }
            int tipo = Character.getType(c);
            if (tipo == Character.CONTROL || tipo == Character.FORMAT) {
                return false;
            }
        }
        String minusculo = caminho.toLowerCase(Locale.ROOT);
        if (minusculo.contains("%2f") || minusculo.contains("%5c") || minusculo.contains("%25")) {
            return false;
        }
        try {
            URI uri = new URI(caminho);
            return uri.getScheme() == null && uri.getRawAuthority() == null && uri.getHost() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /** O próprio caminho, se for válido; senão {@code null}. */
    public static String ouNulo(String caminho) {
        return valido(caminho) ? caminho : null;
    }
}
