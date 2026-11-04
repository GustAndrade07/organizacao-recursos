package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Regra de docs/frontend/README.md: nenhuma cor, espaçamento, raio ou fonte solta no CSS das telas.
 * Só {@code tokens.css} pode ter valores literais; o resto usa {@code var(--token)}.
 */
class CssSemValoresSoltosTest {

    private static final Pattern COMENTARIO = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern MEDIA = Pattern.compile("@media[^{]*");
    private static final Pattern COR_LITERAL = Pattern.compile("#[0-9a-fA-F]{3,8}\\b|\\b(?:rgb|rgba|hsl|hsla)\\(");
    private static final Pattern MEDIDA_LITERAL = Pattern.compile("(?<![\\w.-])\\d*\\.?\\d+(?:px|rem|em)\\b");
    private static final Pattern FONTE_SOLTA = Pattern.compile("(?:font-family|font-size|font-weight|line-height|letter-spacing)\\s*:");
    private static final Pattern USO_DE_TOKEN = Pattern.compile("var\\(\\s*(--[\\w-]+)");
    private static final Pattern DEFINICAO_DE_TOKEN = Pattern.compile("(--[\\w-]+)\\s*:");

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private String ler(Resource recurso) throws IOException {
        String css = new String(recurso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return COMENTARIO.matcher(css).replaceAll("");
    }

    private List<Resource> cssDasTelas() throws IOException {
        List<Resource> telas = new ArrayList<>();
        for (Resource r : resolver.getResources("classpath:static/css/*.css")) {
            if (!"tokens.css".equals(r.getFilename())) {
                telas.add(r);
            }
        }
        return telas;
    }

    @Test
    void existeUmCssDeTelasAlemDosTokens() throws IOException {
        assertThat(cssDasTelas()).extracting(Resource::getFilename).contains("app.css");
    }

    @Test
    void cssDasTelasNaoTemCorMedidaOuFonteSolta() throws IOException {
        List<String> violacoes = new ArrayList<>();
        for (Resource r : cssDasTelas()) {
            // media queries não aceitam var(): os números de breakpoint ficam liberados só ali
            String css = MEDIA.matcher(ler(r)).replaceAll("@media");
            achar(violacoes, r, "cor solta", COR_LITERAL, css);
            achar(violacoes, r, "medida solta", MEDIDA_LITERAL, css);
            achar(violacoes, r, "fonte solta", FONTE_SOLTA, css);
        }
        assertThat(violacoes).isEmpty();
    }

    @Test
    void todoTokenUsadoExisteEmTokensCss() throws IOException {
        Set<String> definidos = new HashSet<>();
        Matcher d = DEFINICAO_DE_TOKEN.matcher(ler(resolver.getResource("classpath:static/css/tokens.css")));
        while (d.find()) {
            definidos.add(d.group(1));
        }
        List<String> inexistentes = new ArrayList<>();
        for (Resource r : cssDasTelas()) {
            Matcher u = USO_DE_TOKEN.matcher(ler(r));
            while (u.find()) {
                if (!definidos.contains(u.group(1))) {
                    inexistentes.add(r.getFilename() + ": " + u.group(1));
                }
            }
        }
        assertThat(inexistentes).isEmpty();
    }

    private static void achar(List<String> violacoes, Resource r, String tipo, Pattern padrao, String css) {
        Matcher m = padrao.matcher(css);
        while (m.find()) {
            violacoes.add(r.getFilename() + ": " + tipo + " -> " + m.group());
        }
    }
}
