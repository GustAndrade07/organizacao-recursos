package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Tela 3 do inventário (docs/frontend/telas.md): páginas de erro 401 / 403 / 404 / 500, prefixo {@code erro-}.
 * Usa o {@code /error} do próprio Spring, que escolhe o template {@code error/<status>} (RF-13: mensagem clara
 * e um caminho para continuar).
 */
@WebMvcTest
class PaginasDeErroTest {

    @Autowired
    MockMvc mvc;

    private MvcResult erro(int status, String urlOriginal) throws Exception {
        return mvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr("jakarta.servlet.error.status_code", status)
                        .requestAttr("jakarta.servlet.error.request_uri", urlOriginal))
                .andReturn();
    }

    private Document pagina(int status, String urlOriginal) throws Exception {
        return Jsoup.parse(erro(status, urlOriginal).getResponse().getContentAsString());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            401 | Você precisa entrar                                | Sua sessão expirou ou você ainda não entrou. Entre com seu e-mail e senha para continuar.        | Entrar           | /login | aviso
            403 | Você não tem permissão para acessar esta página    | Seu perfil não permite esta ação. Se você precisa deste acesso, fale com o administrador.        | Voltar ao início | /      | aviso
            404 | Página não encontrada                              | O endereço não existe ou foi movido. Confira o link ou volte ao início.                          | Voltar ao início | /      | info
            """)
    void paginaDeErroMostraCodigoTituloMensagemEUmCaminhoParaContinuar(
            int status, String titulo, String mensagem, String acao, String href, String tom) throws Exception {
        MvcResult resultado = erro(status, "/reservas/99");
        Document doc = Jsoup.parse(resultado.getResponse().getContentAsString());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(status);
        assertThat(doc.selectFirst("[data-testid=erro-codigo]").text()).isEqualTo("Erro " + status);
        assertThat(doc.selectFirst("[data-testid=erro-codigo]").hasClass("erro__codigo--" + tom)).isTrue();
        assertThat(doc.selectFirst("h1[data-testid=erro-titulo]").text()).isEqualTo(titulo);
        assertThat(doc.selectFirst("[data-testid=erro-mensagem]").text()).isEqualTo(mensagem);
        assertThat(doc.selectFirst("a[data-testid=erro-acao-principal]").text()).isEqualTo(acao);
        assertThat(doc.selectFirst("a[data-testid=erro-acao-principal]").attr("href")).isEqualTo(href);
        assertThat(doc.select("[data-testid=erro-acao-secundaria]")).isEmpty();
    }

    @Test
    void erro500OfereceTentarNovamenteNaUrlOriginalEVoltarAoInicio() throws Exception {
        MvcResult resultado = erro(500, "/reservas/99");
        Document doc = Jsoup.parse(resultado.getResponse().getContentAsString());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(500);
        assertThat(doc.selectFirst("[data-testid=erro-codigo]").text()).isEqualTo("Erro 500");
        assertThat(doc.selectFirst("[data-testid=erro-codigo]").hasClass("erro__codigo--perigo")).isTrue();
        assertThat(doc.selectFirst("[data-testid=erro-titulo]").text()).isEqualTo("Algo deu errado do nosso lado");
        assertThat(doc.selectFirst("[data-testid=erro-acao-principal]").text()).isEqualTo("Tentar novamente");
        assertThat(doc.selectFirst("[data-testid=erro-acao-principal]").attr("href")).isEqualTo("/reservas/99");
        assertThat(doc.selectFirst("[data-testid=erro-acao-secundaria]").text()).isEqualTo("Voltar ao início");
        assertThat(doc.selectFirst("[data-testid=erro-acao-secundaria]").attr("href")).isEqualTo("/");
    }

    @Test
    void tentarNovamenteNuncaApontaParaOutroSite() throws Exception {
        Document doc = pagina(500, "//outro-site.example/x");

        assertThat(doc.selectFirst("[data-testid=erro-acao-principal]").attr("href")).isEqualTo("/");
    }

    @Test
    void paginaDeErroUsaOLayoutBaseSemMenuParaVisitante() throws Exception {
        Document doc = pagina(404, "/nada");

        assertThat(doc.title()).isEqualTo("Página não encontrada · Organização de Recursos");
        assertThat(doc.select("link[rel=stylesheet]").eachAttr("href")).containsExactly("/css/tokens.css", "/css/app.css");
        assertThat(doc.selectFirst("main#conteudo [data-testid=erro-titulo]")).isNotNull();
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.select("[data-testid=menu-abrir]")).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            400 | Não foi possível atender o pedido | aviso
            503 | Algo deu errado do nosso lado     | perigo
            """)
    void outrosStatusCaemNoModeloGenericoDaFaixaEmPortugues(int status, String titulo, String tom) throws Exception {
        MvcResult resultado = erro(status, "/qualquer");
        Document doc = Jsoup.parse(resultado.getResponse().getContentAsString());

        assertThat(resultado.getResponse().getStatus()).isEqualTo(status);
        assertThat(doc.selectFirst("[data-testid=erro-codigo]").text()).isEqualTo("Erro " + status);
        assertThat(doc.selectFirst("[data-testid=erro-codigo]").hasClass("erro__codigo--" + tom)).isTrue();
        assertThat(doc.selectFirst("[data-testid=erro-titulo]").text()).isEqualTo(titulo);
        assertThat(doc.text()).doesNotContain("Whitelabel");
    }
}
