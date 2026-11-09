package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

/**
 * O SonarCloud analisa o arquivo do template, não a página renderizada, e reprova página sem
 * {@code <title>} (regra Web:PageWithoutTitleCheck, tipo Bug: rebaixa o Reliability Rating do código novo
 * e reprova o Quality Gate). O título final vem do layout; o template declara um título no {@code <head>}
 * só para a análise estática enxergar.
 *
 * <p>A verificação varre <b>todos</b> os arquivos de {@code src/main/resources/templates} (a pasta que o Sonar
 * analisa; os templates de teste ficam de fora), sem lista manual: a lista antiga
 * deixou de fora as páginas de erro 403, 404, 500, 4xx e 5xx e o fragmento de erro (DEF-001).
 */
class TemplatesComTituloTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    void todoTemplateDeclaraUmTitleNoHeadEOIdiomaNoHtml() throws Exception {
        List<Path> templates;
        try (Stream<Path> arquivos = Files.walk(TEMPLATES)) {
            templates = arquivos.filter(p -> p.toString().endsWith(".html")).sorted().toList();
        }
        assertThat(templates).as("templates encontrados").isNotEmpty();

        List<String> semTitle = new ArrayList<>();
        List<String> semLang = new ArrayList<>();
        for (Path template : templates) {
            String nome = TEMPLATES.relativize(template).toString();
            Document doc = Jsoup.parse(Files.readString(template, StandardCharsets.UTF_8));
            var titulos = doc.select("html > head > title");
            if (titulos.size() != 1 || titulos.first().text().isBlank()) {
                semTitle.add(nome);
            }
            // Sonar Web:S5254: o elemento html declara o idioma da página
            if (doc.select("html[lang]").size() != 1) {
                semLang.add(nome);
            }
        }

        assertThat(semTitle).as("templates sem exatamente um <title> preenchido no <head>").isEmpty();
        assertThat(semLang).as("templates sem lang no <html>").isEmpty();
    }
}
