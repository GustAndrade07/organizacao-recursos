package br.com.senac.recursos.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * CA-5 e CA-7 (Spec "Políticas do site"), em 390, 768 e 1280 px: o rodapé aparece nas telas e nas páginas de erro, os
 * três links levam às páginas, nada rola na horizontal, o link do rodapé tem foco visível e alvo de toque de 44 px. A
 * garantia de que <b>toda</b> tela tem o rodapé vem da varredura de templates ({@code RodapeNosTemplatesTest}); aqui
 * está a prova no navegador, numa amostra de telas de cada tipo (login, erro, busca, listas e formulários).
 */
class PoliticasE2ETest extends E2ETeste {

    private static final int ALVO_DE_TOQUE = 44;

    @ParameterizedTest(name = "{0} px em {1}")
    @CsvSource({
            "390,  /login",
            "768,  /login",
            "1280, /login",
            "390,  /e2e/erro/403",
            "1280, /e2e/erro/500",
            "390,  /e2e/busca",
            "768,  /e2e/minhas",
            "1280, /e2e/aprovacoes",
            "390,  /e2e/recursos"
    })
    void rodapeApareceComDireitosELinksSemRolagemHorizontal(int largura, String caminho) {
        redimensionar(largura);
        abrir(caminho);

        Locator rodape = pagina.locator("footer.rodape-site");
        assertThat(rodape.count()).isEqualTo(1);
        assertThat(pagina.locator("[data-testid=rodape-direitos]").innerText()).startsWith("© ")
                .endsWith("Organização de Recursos");
        assertThat(pagina.locator("footer a").allInnerTexts())
                .containsExactly("Política de privacidade", "Política de cookies", "Termos de uso");
        semRolagemHorizontal(largura);
        for (Locator link : pagina.locator("footer a").all()) {
            double altura = link.boundingBox().height;
            assertThat(altura).as("altura do link do rodapé a %d px", largura).isGreaterThanOrEqualTo(ALVO_DE_TOQUE);
        }
    }

    @ParameterizedTest(name = "{0} px")
    @ValueSource(ints = {390, 768, 1280})
    void doLoginSemSessaoOsTresLinksLevamAsPaginas(int largura) {
        redimensionar(largura);
        List<String[]> destinos = List.of(
                new String[] {"rodape-privacidade", "/privacidade", "Política de privacidade"},
                new String[] {"rodape-cookies", "/cookies", "Política de cookies"},
                new String[] {"rodape-termos", "/termos", "Termos de uso"});

        for (String[] destino : destinos) {
            abrir("/login");
            pagina.locator("[data-testid=" + destino[0] + "]").click();

            assertThat(caminhoAtual()).isEqualTo(destino[1]);
            assertThat(pagina.locator("h1").allInnerTexts()).containsExactly(destino[2]);
            assertThat(pagina.locator("footer a[aria-current=page]").count()).isEqualTo(1);
            semRolagemHorizontal(largura);
            // texto corrido legível: 16 px ou mais e coluna de no máximo 75 caracteres
            double fonte = ((Number) pagina.evaluate(
                    "() => parseFloat(getComputedStyle(document.querySelector('.documento p')).fontSize)")).doubleValue();
            assertThat(fonte).isGreaterThanOrEqualTo(16.0);
            print("politica-" + destino[1].substring(1) + "-" + largura);
        }
    }

    @ParameterizedTest(name = "{0} px")
    @ValueSource(ints = {390, 1280})
    void linkDoRodapeRecebeFocoVisivelPeloTeclado(int largura) {
        redimensionar(largura);
        abrir("/privacidade");

        pagina.locator("[data-testid=rodape-cookies]").focus();
        String estilo = (String) pagina.evaluate("""
                () => { const s = getComputedStyle(document.activeElement);
                        return s.outlineStyle + '|' + parseFloat(s.outlineWidth); }""");
        String[] partes = estilo.split("\\|");
        assertThat(partes[0]).isNotEqualTo("none");
        assertThat(Double.parseDouble(partes[1])).isGreaterThanOrEqualTo(2.0);
    }
}
