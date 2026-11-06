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
 * RN-05 no visual: cada um dos sete estados da reserva tem a sua classe {@code .estado--*} no CSS, com os
 * tokens {@code color/status/*} de fundo e de texto (docs/frontend/design-tokens.md). O nome do estado
 * escrito no badge é conferido nos testes de cada tela.
 */
class EstadosDaReservaCssTest {

    private static final List<String> ESTADOS = List.of(
            "solicitada", "aprovada", "em-uso", "concluida", "rejeitada", "cancelada", "nao-compareceu");

    private String css() throws IOException {
        return new String(new ClassPathResource("static/css/app.css").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void cadaEstadoTemClasseComOsTokensDeStatus() throws IOException {
        String css = css();
        for (String estado : ESTADOS) {
            Matcher regra = Pattern.compile("\\.estado--" + estado + "\\s*\\{([^}]*)\\}").matcher(css);
            assertThat(regra.find()).as("classe .estado--%s", estado).isTrue();
            assertThat(regra.group(1))
                    .as("tokens da classe .estado--%s", estado)
                    .contains("var(--color-status-" + estado + "-bg)")
                    .contains("var(--color-status-" + estado + "-fg)");
        }
    }

    @Test
    void regraBaseDoBadgeTemCorNeutraParaEstadoDesconhecido() throws IOException {
        Matcher regra = Pattern.compile("(?<![\\w-])\\.estado\\s*(?:,\\s*\\.situacao\\s*)?\\{([^}]*)\\}").matcher(css());

        // a regra base é dividida com .situacao (situação do recurso, telas de recursos)
        assertThat(regra.find()).as("regra base .estado").isTrue();
        assertThat(regra.group(1))
                .contains("background: var(--color-bg-subtle)")
                .contains("color: var(--color-text-secondary)");
    }
}
