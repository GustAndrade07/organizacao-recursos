package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * O SonarCloud lê o arquivo do template, não a página renderizada. Duas regras dele que um template
 * Thymeleaf quebra sem ninguém ver: id duplicado no mesmo arquivo (as duas variantes de um
 * {@code th:if} contam como duplicata) e campo sem {@code id} fixo ligado a um rótulo.
 */
class TemplatesAnalisaveisTest {

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private Document ler(Resource recurso) throws Exception {
        return Jsoup.parse(new String(recurso.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void nenhumTemplateRepeteUmIdFixoNoMesmoArquivo() throws Exception {
        List<String> repetidos = new ArrayList<>();
        for (Resource r : resolver.getResources("classpath:templates/**/*.html")) {
            Set<String> vistos = new HashSet<>();
            for (Element e : ler(r).select("[id]")) {
                if (!vistos.add(e.id())) {
                    repetidos.add(r.getFilename() + ": " + e.id());
                }
            }
        }
        assertThat(repetidos).isEmpty();
    }

    @Test
    void cadaCampoDoFragmentoCampoTemIdFixoLigadoAUmRotuloPorFor() throws Exception {
        Document doc = ler(resolver.getResource("classpath:templates/fragments/campo.html"));

        var campos = doc.select("input, textarea");
        assertThat(campos).isNotEmpty();
        for (Element campo : campos) {
            assertThat(campo.id()).as("id fixo de %s", campo.tagName()).isNotBlank();
            assertThat(doc.select("label[for=" + campo.id() + "]")).as("rótulo de #%s", campo.id()).hasSize(1);
        }
    }
}
