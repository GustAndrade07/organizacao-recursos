package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rotas reais das telas 13, 14 e 15 ({@link RecursoController}): só recebem os parâmetros e devolvem a
 * tela. Quem lista, grava e consulta reservas afetadas é o back-end, que preenche o resto do modelo.
 */
@WebMvcTest(RecursoController.class)
class RecursoControllerTest {

    @Autowired
    MockMvc mvc;

    private Document pagina(String caminho) throws Exception {
        String html = mvc.perform(get(caminho))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Jsoup.parse(html);
    }

    @Test
    void listaSemParametrosMostraAbaSalasSemListaEnquantoOBackEndNaoPreenche() throws Exception {
        Document doc = pagina("/admin/recursos");

        assertThat(doc.title()).isEqualTo("Recursos · Organização de Recursos");
        assertThat(doc.selectFirst("a[aria-current=page]").attr("data-testid")).isEqualTo("recursos-aba-salas");
        assertThat(doc.select("table, [data-testid=recursos-vazio]")).isEmpty();
    }

    @Test
    void listaLeOTipoEONomeDaUrl() throws Exception {
        Document doc = pagina("/admin/recursos?tipo=materiais&nome=proje");

        assertThat(doc.selectFirst("a[aria-current=page]").attr("data-testid")).isEqualTo("recursos-aba-materiais");
        assertThat(doc.getElementById("recursos-nome").val()).isEqualTo("proje");
        assertThat(doc.selectFirst("input[name=tipo]").val()).isEqualTo("materiais");
    }

    @Test
    void tipoDesconhecidoCaiEmSalas() throws Exception {
        Document doc = pagina("/admin/recursos?tipo=carros");

        assertThat(doc.selectFirst("a[aria-current=page]").attr("data-testid")).isEqualTo("recursos-aba-salas");
    }

    @Test
    void nomeEmBrancoNaoPreencheOCampoDeBusca() throws Exception {
        assertThat(pagina("/admin/recursos?nome=%20%20").getElementById("recursos-nome").val()).isEmpty();
    }

    @Test
    void novoRecursoAbreNoTipoPedidoEAceitaSalaPorPadrao() throws Exception {
        assertThat(pagina("/admin/recursos/novo").selectFirst("select#recurso-form-tipo option[selected]").text()).isEqualTo("Sala");
        Document doc = pagina("/admin/recursos/novo?tipo=professores");

        assertThat(doc.title()).isEqualTo("Cadastrar recurso · Organização de Recursos");
        assertThat(doc.selectFirst("select#recurso-form-tipo option[selected]").text()).isEqualTo("Professor");
        assertThat(doc.getElementById("recurso-form-competencias")).isNotNull();
    }

    @Test
    void trocarOTipoLevaAoNovoOQueJaFoiDigitado() throws Exception {
        Document doc = pagina("/admin/recursos/novo?tipo=materiais&nome=Projetor&localizacao=Almoxarifado&patrimonio=PAT-1&restrito=on");

        assertThat(doc.getElementById("recurso-form-nome").val()).isEqualTo("Projetor");
        assertThat(doc.getElementById("recurso-form-localizacao").val()).isEqualTo("Almoxarifado");
        assertThat(doc.getElementById("recurso-form-patrimonio").val()).isEqualTo("PAT-1");
        assertThat(doc.getElementById("recurso-form-restrito").hasAttr("checked")).isTrue();
    }

    @Test
    void editarRecursoAbreOFormularioDeEdicaoComOIdDaRota() throws Exception {
        Document doc = pagina("/admin/recursos/7");

        assertThat(doc.title()).isEqualTo("Editar recurso · Organização de Recursos");
        assertThat(doc.selectFirst("form[data-testid=recurso-form]").attr("action")).isEqualTo("/admin/recursos/7");
        assertThat(doc.select("select")).isEmpty();
    }

    @Test
    void bloqueioAbreVazioOuPreenchidoComOQueVeioNaUrl() throws Exception {
        Document vazio = pagina("/admin/recursos/3/manutencao");
        assertThat(vazio.title()).isEqualTo("Bloquear para manutenção · Organização de Recursos");
        assertThat(vazio.selectFirst("form[data-testid=manutencao-form]").attr("action")).isEqualTo("/admin/recursos/3/manutencao");
        assertThat(vazio.getElementById("manutencao-inicio").val()).isEmpty();

        Document preenchido = pagina("/admin/recursos/3/manutencao?inicio=2026-11-10T08:00&fim=2026-11-20T18:00&motivo=Reforma");
        assertThat(preenchido.getElementById("manutencao-inicio").val()).isEqualTo("2026-11-10T08:00");
        assertThat(preenchido.getElementById("manutencao-fim").val()).isEqualTo("2026-11-20T18:00");
        assertThat(preenchido.getElementById("manutencao-motivo").val()).isEqualTo("Reforma");
    }
}
