package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.support.ComoUsuario;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Telas 17 a 19 do inventário (docs/frontend/telas.md): política de privacidade, política de cookies e termos de uso,
 * páginas públicas (prefixos {@code politica-} e {@code rodape-politicas}). Cobre CA-1, CA-8, CA-11 e CA-15 da Spec
 * "Políticas do site": 200 sem sessão, um {@code h1}, {@code title}, {@code lang}, sem script nem recurso externo
 * além do que o layout já tem, títulos em ordem e o aviso de rascunho presente se, e só se, há pendência.
 */
@WebMvcTest
class PoliticasPaginaTest {

    private static final int LIMITE_DO_HTML = 30 * 1024;

    @Autowired
    MockMvc mvc;

    private MvcResult abrir(String caminho) throws Exception {
        return mvc.perform(get(caminho)).andExpect(status().isOk()).andReturn();
    }

    private Document pagina(String caminho) throws Exception {
        return Jsoup.parse(abrir(caminho).getResponse().getContentAsString());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            /privacidade | Política de privacidade | rodape-privacidade
            /cookies     | Política de cookies     | rodape-cookies
            /termos      | Termos de uso           | rodape-termos
            """)
    void paginaPublicaTemUmH1TitleEIdiomaESemCriarSessao(String caminho, String titulo, String linkDoRodape)
            throws Exception {
        MvcResult resultado = abrir(caminho);
        Document doc = Jsoup.parse(resultado.getResponse().getContentAsString());

        assertThat(resultado.getRequest().getSession(false)).as("página pública não cria sessão").isNull();
        assertThat(doc.selectFirst("html").attr("lang")).isEqualTo("pt-BR");
        assertThat(doc.title()).isEqualTo(titulo + " · Organização de Recursos");
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.selectFirst("main#conteudo article.documento h1[data-testid=politica-titulo]").text())
                .isEqualTo(titulo);
        assertThat(doc.selectFirst("[data-testid=politica-atualizacao]").text())
                .matches("Última atualização: \\d{2}/\\d{2}/\\d{4}");
        // o link da própria página, no rodapé, marca a página atual
        Element atual = doc.selectFirst("footer a[data-testid=" + linkDoRodape + "]");
        assertThat(atual.attr("aria-current")).isEqualTo("page");
        assertThat(doc.select("footer a[aria-current]")).hasSize(1);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"/privacidade", "/cookies", "/termos"})
    @ComoUsuario(PerfilUsuario.SOLICITANTE)
    void logadoVeAPaginaComOMenuDoPerfil(String caminho) throws Exception {
        Document doc = pagina(caminho);

        assertThat(doc.selectFirst("[data-testid=menu-usuario]").text()).isEqualTo("Laura Mendes");
        assertThat(doc.select("footer a")).hasSize(3);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"/privacidade", "/cookies", "/termos"})
    void htmlPequenoESemScriptNemRecursoExternoAlemDoLayout(String caminho) throws Exception {
        MvcResult resultado = abrir(caminho);
        Document doc = Jsoup.parse(resultado.getResponse().getContentAsString());

        assertThat(resultado.getResponse().getContentAsByteArray().length).as("bytes UTF-8 do HTML")
                .isLessThanOrEqualTo(LIMITE_DO_HTML);
        // os mesmos scripts do layout, nenhum a mais
        assertThat(doc.select("script[src]").eachAttr("src")).containsExactly("/js/menu.js", "/js/envio-unico.js");
        assertThat(doc.select("script:not([src])")).hasSize(1);
        List<String> externos = new ArrayList<>();
        doc.select("[src], [href], [action]").forEach(e -> {
            for (String atributo : List.of("src", "href", "action")) {
                String valor = e.attr(atributo);
                if (!valor.isEmpty() && !valor.startsWith("/") && !valor.startsWith("#")) {
                    externos.add(atributo + "=" + valor);
                }
            }
        });
        assertThat(externos).as("src, href e action que não são do próprio site").isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"/privacidade", "/cookies", "/termos"})
    void titulosComecamEmH1ENuncaPulamNivelERodapeNaoTemTitulo(String caminho) throws Exception {
        Document doc = pagina(caminho);

        List<Integer> niveis = doc.select("h1, h2, h3, h4, h5, h6").stream()
                .map(h -> Integer.parseInt(h.tagName().substring(1))).toList();
        assertThat(niveis).isNotEmpty();
        assertThat(niveis.get(0)).isEqualTo(1);
        for (int i = 1; i < niveis.size(); i++) {
            assertThat(niveis.get(i)).as("nível do título %d depois de %d", niveis.get(i), niveis.get(i - 1))
                    .isLessThanOrEqualTo(niveis.get(i - 1) + 1);
        }
        assertThat(doc.select("footer h1, footer h2, footer h3, footer h4, footer h5, footer h6")).isEmpty();
    }

    /** CA-8: toda pendência é um elemento marcado, e o aviso de rascunho aparece se, e só se, houver pendência. */
    @ParameterizedTest(name = "{0}")
    @CsvSource({"/privacidade", "/cookies", "/termos"})
    void avisoDeRascunhoApareceSeESoSeHaPendencia(String caminho) throws Exception {
        Document doc = pagina(caminho);
        Element documento = doc.selectFirst("article.documento");

        boolean haPendencia = !documento.select("[data-pendente]").isEmpty();
        assertThat(!doc.select("[data-testid=politica-rascunho]").isEmpty())
                .as("aviso de rascunho presente ⇔ há data-pendente").isEqualTo(haPendencia);

        // "a definir" solto, fora de um elemento marcado, é pendência escondida
        Pattern aDefinir = Pattern.compile("a definir", Pattern.CASE_INSENSITIVE);
        List<String> soltos = new ArrayList<>();
        documento.getAllElements().stream()
                .filter(e -> e.ownText() != null && aDefinir.matcher(e.ownText()).find())
                .filter(e -> e.closest("[data-pendente]") == null)
                .forEach(e -> soltos.add(e.ownText()));
        assertThat(soltos).as("\"a definir\" fora de [data-pendente]").isEmpty();
        assertThat(documento.text()).as("marcador {{...}} esquecido").doesNotContain("{{");
    }

    /** CA-6 (metade do texto): a política de cookies descreve exatamente o conjunto de cookies que o sistema emite. */
    @Test
    void politicaDeCookiesDescreveSoOJsessionid() throws Exception {
        Document doc = pagina("/cookies");

        List<String> nomes = doc.select("[data-cookie] h3").eachText();
        assertThat(nomes).containsExactly("JSESSIONID");
        assertThat(doc.selectFirst("[data-cookie]").select("dt").eachText())
                .contains("Para que serve", "Quanto dura", "Atributos");
        assertThat(doc.selectFirst("[data-cookie] dl").text()).contains("HttpOnly", "SameSite=Lax");
    }

    @Test
    void politicaDePrivacidadeTrataOEmailComoDadoPessoalENaoPrometeApagarSozinha() throws Exception {
        String texto = pagina("/privacidade").selectFirst("article.documento").text();

        assertThat(texto).contains("e-mail").contains("dado pessoal");
        assertThat(texto).contains("Lei 13.709/2018").contains("art. 18");
        assertThat(texto).contains("registro de auditoria");
    }
}
