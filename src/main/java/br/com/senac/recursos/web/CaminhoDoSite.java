package br.com.senac.recursos.web;

/** Regra antiga (só o começo da string), mantida só até o commit de correção. */
public final class CaminhoDoSite {

    private CaminhoDoSite() {
    }

    public static boolean valido(String caminho) {
        return caminho != null && caminho.startsWith("/") && !caminho.startsWith("//");
    }

    public static String ouNulo(String caminho) {
        return valido(caminho) ? caminho : null;
    }
}
