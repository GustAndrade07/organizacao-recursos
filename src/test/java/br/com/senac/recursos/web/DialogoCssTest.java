package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * O diálogo das telas 9 e 10 é um {@code <dialog>} com {@code display: flex} em {@code .dialogo}. Sem uma
 * regra para o diálogo fechado, esse {@code display} vence o {@code display: none} do navegador e o
 * diálogo apareceria sempre. O fundo do modal vem do {@code ::backdrop}.
 */
class DialogoCssTest {

    private String css() throws IOException {
        return new String(new ClassPathResource("static/css/app.css").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String regra(String seletor) throws IOException {
        Matcher m = Pattern.compile("(?m)^" + Pattern.quote(seletor) + "\\s*\\{([^}]*)\\}").matcher(css());
        assertThat(m.find()).as("regra %s", seletor).isTrue();
        return m.group(1);
    }

    @Test
    void dialogoFechadoNaoApareceMesmoComDisplayFlexNaClasse() throws IOException {
        assertThat(regra(".dialogo:not([open])")).contains("display: none");
    }

    @Test
    void dialogoFicaFixoNaJanelaEUsaSoTokens() throws IOException {
        assertThat(regra(".dialogo")).contains("position: fixed").contains("var(--shadow-overlay)").contains("var(--color-bg-surface)");
    }

    @Test
    void fundoDoModalEscureceAPaginaComToken() throws IOException {
        assertThat(regra(".dialogo::backdrop")).contains("var(--color-text-primary)");
    }
}
