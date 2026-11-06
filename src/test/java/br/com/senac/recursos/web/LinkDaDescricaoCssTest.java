package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * WCAG 2.5.8: um link dentro de {@code .pagina__descricao} (por exemplo "Voltar") tem alvo de toque de pelo
 * menos {@code --size-control-md} (44 px) na altura, sem valor solto no CSS.
 */
class LinkDaDescricaoCssTest {

    @Test
    void linkDaDescricaoTemAlvoDeToqueMinimo() throws IOException {
        String css = new String(new ClassPathResource("static/css/app.css").getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        Matcher regra = Pattern.compile("\\.pagina__descricao\\s+a\\s*\\{([^}]*)\\}").matcher(css);

        assertThat(regra.find()).as("regra .pagina__descricao a").isTrue();
        assertThat(regra.group(1))
                .contains("display: inline-flex")
                .contains("align-items: center")
                .contains("min-height: var(--size-control-md)");
    }
}
