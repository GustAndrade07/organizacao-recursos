package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

/**
 * CA-4, CA-14 e CA-15 (Spec "Políticas do site"): o rodapé de página existe em <b>toda</b> tela porque todo template
 * de página usa o {@code layout.html}, e o layout o desenha. A varredura olha <b>todos</b> os arquivos de
 * {@code src/main/resources/templates}, sem lista manual (regra 10 do AGENTS.md); só {@code layout.html} e
 * {@code fragments/} ficam de fora, e isso é decidido pelo caminho. O comentário de uso do layout cita o
 * {@code th:replace} das páginas literalmente, por isso a conferência olha o atributo do elemento {@code html},
 * não o texto do arquivo.
 */
class RodapeNosTemplatesTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    private static Document ler(Path arquivo) throws Exception {
        return Jsoup.parse(Files.readString(arquivo, StandardCharsets.UTF_8));
    }

    private static List<Path> todosOsTemplates() throws Exception {
        try (Stream<Path> arquivos = Files.walk(TEMPLATES)) {
            return arquivos.filter(p -> p.toString().endsWith(".html")).sorted().toList();
        }
    }

    private static boolean ehPagina(Path arquivo) {
        Path relativo = TEMPLATES.relativize(arquivo);
        return !relativo.toString().equals("layout.html") && !relativo.startsWith("fragments");
    }

    @Test
    void todaPaginaUsaOLayoutQueTemORodape() throws Exception {
        List<Path> paginas = todosOsTemplates().stream().filter(RodapeNosTemplatesTest::ehPagina).toList();
        assertThat(paginas).as("páginas encontradas (o total não pode cair)").hasSizeGreaterThanOrEqualTo(20);

        List<String> foraDoLayout = new ArrayList<>();
        for (Path pagina : paginas) {
            String usa = ler(pagina).selectFirst("html").attr("th:replace");
            if (!usa.contains("layout :: pagina")) {
                foraDoLayout.add(TEMPLATES.relativize(pagina).toString());
            }
        }
        assertThat(foraDoLayout).as("páginas que não usam layout :: pagina").isEmpty();
    }

    @Test
    void layoutTemUmRodapeDePaginaForaDoCabecalhoEDoMenu() throws Exception {
        Document layout = ler(TEMPLATES.resolve("layout.html"));

        assertThat(layout.select("footer")).as("rodapés de página").hasSize(1);
        Element rodape = layout.selectFirst("footer");
        assertThat(rodape.parent().tagName()).as("o rodapé é filho direto do body").isEqualTo("body");
        assertThat(rodape.parents().stream().map(Element::tagName).toList()).doesNotContain("header", "nav", "main");
        // o "rodape" do menu (usuário e Sair) é outra coisa e continua dentro do menu
        assertThat(layout.select("nav#menu-painel .menu__rodape")).hasSize(1);
        assertThat(rodape.select(".menu__rodape")).isEmpty();
    }

    @Test
    void ordemDosLandmarksEPularHeaderMainFooter() throws Exception {
        Document layout = ler(TEMPLATES.resolve("layout.html"));

        List<String> ordem = layout.body().children().stream()
                .map(Element::tagName).filter(tag -> !tag.equals("script")).toList();
        assertThat(ordem).containsExactly("a", "header", "main", "footer");
        assertThat(layout.select("header")).hasSize(1);
        assertThat(layout.select("main")).hasSize(1);
    }

    @Test
    void rodapeTemOsTresLinksDasPoliticasENavComNomeProprio() throws Exception {
        Document layout = ler(TEMPLATES.resolve("layout.html"));
        Element rodape = layout.selectFirst("footer");

        Element nav = rodape.selectFirst("nav[aria-label]");
        assertThat(nav).as("nav dentro do rodapé").isNotNull();
        assertThat(nav.attr("aria-label")).isEqualTo("Políticas do site");
        // nenhum nav repete o rótulo de outro
        List<String> rotulos = layout.select("nav[aria-label]").eachAttr("aria-label");
        assertThat(rotulos).doesNotHaveDuplicates();

        assertThat(rodape.select("a").eachAttr("th:href")).containsExactly("@{/privacidade}", "@{/cookies}", "@{/termos}");
        assertThat(rodape.select("a").eachText()).containsExactly("Política de privacidade", "Política de cookies", "Termos de uso");
        assertThat(rodape.select("h1, h2, h3, h4, h5, h6, .botao")).as("rodapé sem título nem botão").isEmpty();
    }

    @Test
    void anoDoRodapeVemDoRelogioNuncaEscritoNoHtml() throws Exception {
        Document layout = ler(TEMPLATES.resolve("layout.html"));
        assertThat(layout.selectFirst("footer").html()).contains("${@rodapeDoSite.ano()}");

        Pattern anoFixo = Pattern.compile("©\\s*(?:&copy;)?\\s*\\d{4}");
        List<String> comAnoFixo = new ArrayList<>();
        for (Path template : todosOsTemplates()) {
            if (anoFixo.matcher(Files.readString(template, StandardCharsets.UTF_8)).find()) {
                comAnoFixo.add(TEMPLATES.relativize(template).toString());
            }
        }
        assertThat(comAnoFixo).as("templates com © e ano escritos à mão").isEmpty();
    }
}
