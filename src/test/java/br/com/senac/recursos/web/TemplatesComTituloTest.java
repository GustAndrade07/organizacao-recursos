package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * O SonarCloud analisa o arquivo do template, não a página renderizada, e reprova página sem
 * {@code <title>} (regra Web:PageWithoutTitleCheck). O título final vem do layout; o template
 * declara o mesmo título no {@code <head>} só para a análise estática enxergar.
 */
class TemplatesComTituloTest {

    private static final List<String> TEMPLATES_DE_TELA = List.of("busca", "reserva-nova", "fragments/estado", "historico", "relatorio", "login", "error/401",
            "recursos", "recurso-form", "manutencao", "fragments/campo",
            "aprovacoes", "retirada-devolucao", "fragments/confirmacao",
            "error/403", "error/404", "error/4xx", "error/500", "error/5xx", "fragments/erro");

    @Test
    void todoTemplateDeTelaDeclaraUmTitleNoHead() throws Exception {
        for (String nome : TEMPLATES_DE_TELA) {
            String html = new String(new ClassPathResource("templates/" + nome + ".html").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            var titulos = Jsoup.parse(html).select("html > head > title");
            assertThat(titulos).as(nome).hasSize(1);
            assertThat(titulos.first().text()).as(nome).isNotBlank();
            // Sonar Web:S5254: o elemento html declara o idioma da página
            assertThat(Jsoup.parse(html).select("html[lang]")).as(nome + " declara lang no html").hasSize(1);
        }
    }
}
