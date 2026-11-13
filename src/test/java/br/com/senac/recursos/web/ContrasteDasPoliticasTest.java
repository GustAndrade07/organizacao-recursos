package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * CA-10 (Spec "Políticas do site"): rodapé e texto das políticas usam só {@code text-primary}, {@code text-secondary}
 * e {@code text-brand}, sobre {@code bg-page} ou {@code bg-surface}, e cada par passa de 4,5:1. {@code text-muted} e
 * {@code text-disabled} ficam de fora (o primeiro reprova sobre {@code bg-subtle}, o segundo sobre qualquer fundo). O
 * rodapé herda o fundo do {@code body}, por isso os dois fundos possíveis são conferidos. Também trava o alvo de toque
 * do link do rodapé (44 px) e o teto da coluna de leitura (75 caracteres).
 */
class ContrasteDasPoliticasTest {

    private static final List<String> TEXTOS_PERMITIDOS = List.of("--color-text-primary", "--color-text-secondary",
            "--color-text-brand");
    private static final List<String> FUNDOS_POSSIVEIS = List.of("--color-bg-page", "--color-bg-surface");

    private static String ler(String caminho) throws IOException {
        return new String(new ClassPathResource(caminho).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String regra(String css, String seletor) {
        Matcher m = Pattern.compile("(?m)^" + Pattern.quote(seletor) + "\\s*\\{([^}]*)\\}").matcher(css);
        assertThat(m.find()).as("regra %s em app.css", seletor).isTrue();
        return m.group(1);
    }

    private static String hexDoToken(String tokens, String nome) {
        Matcher m = Pattern.compile("(?m)^\\s*" + Pattern.quote(nome) + "\\s*:\\s*([^;]+);").matcher(tokens);
        assertThat(m.find()).as("token %s em tokens.css", nome).isTrue();
        String valor = m.group(1).trim();
        Matcher ref = Pattern.compile("var\\((--[\\w-]+)\\)").matcher(valor);
        return ref.matches() ? hexDoToken(tokens, ref.group(1)) : valor;
    }

    private static double luminancia(String hex) {
        double[] canais = new double[3];
        for (int i = 0; i < 3; i++) {
            double c = Integer.parseInt(hex.substring(1 + 2 * i, 3 + 2 * i), 16) / 255.0;
            canais[i] = c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * canais[0] + 0.7152 * canais[1] + 0.0722 * canais[2];
    }

    private static double razao(String tokens, String texto, String fundo) {
        double a = luminancia(hexDoToken(tokens, texto));
        double b = luminancia(hexDoToken(tokens, fundo));
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    @Test
    void textoDoRodapeEDoDocumentoUsaSoOsTokensPermitidosComContrasteDeAoMenos4e5() throws IOException {
        String css = ler("static/css/app.css");
        String tokens = ler("static/css/tokens.css");

        for (String seletor : List.of(".rodape-site", ".rodape-site a", ".documento", ".documento a")) {
            Matcher cor = Pattern.compile("(?<![\\w-])color:\\s*var\\((--[\\w-]+)\\)").matcher(regra(css, seletor));
            if (!cor.find()) {
                continue; // herda a cor do pai, que também é conferido
            }
            assertThat(TEXTOS_PERMITIDOS).as("cor de %s", seletor).contains(cor.group(1));
            for (String fundo : FUNDOS_POSSIVEIS) {
                assertThat(razao(tokens, cor.group(1), fundo)).as("%s sobre %s", seletor, fundo)
                        .isGreaterThanOrEqualTo(4.5);
            }
        }
        // o texto herdado do body (text-primary) também passa sobre os dois fundos
        for (String fundo : FUNDOS_POSSIVEIS) {
            assertThat(razao(tokens, "--color-text-primary", fundo)).isGreaterThanOrEqualTo(4.5);
        }
    }

    @Test
    void linkDoRodapeTemAlvoDeToqueDe44PxSemRemoverOSublinhado() throws IOException {
        String link = regra(ler("static/css/app.css"), ".rodape-site a");

        assertThat(link).contains("min-height: var(--size-control-md)");
        assertThat(link).doesNotContain("text-decoration: none");
    }

    @Test
    void colunaDeLeituraTemNoMaximo75Caracteres() throws IOException {
        Matcher largura = Pattern.compile("max-width:\\s*(\\d+)ch").matcher(regra(ler("static/css/app.css"), ".documento"));

        assertThat(largura.find()).as("max-width em ch na regra .documento").isTrue();
        assertThat(Integer.parseInt(largura.group(1))).isLessThanOrEqualTo(75);
    }
}
