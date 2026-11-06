package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tela 14 do inventário (docs/frontend/telas.md): formulário de recurso, prefixo {@code recurso-form-}, RF-02.
 * O atributo {@code tipo} escolhe os campos; {@code recurso} traz o que preencher (com {@code id} é a edição,
 * que o back-end fornece); {@code erros} (campo para mensagem) vem depois de um POST recusado.
 */
@WebMvcTest(RecursoFormTest.PaginasDeTeste.class)
@Import(RecursoFormTest.PaginasDeTeste.class)
class RecursoFormTest {

    @Controller
    static class PaginasDeTeste {

        private static FormularioRecurso vazio() {
            return new FormularioRecurso(null, null, null, null, null, null, false);
        }

        private static String formulario(Model model, TipoRecurso tipo, FormularioRecurso recurso, Map<String, String> erros) {
            model.addAttribute("tipo", tipo);
            model.addAttribute("recurso", recurso);
            if (erros != null) {
                model.addAttribute("erros", erros);
            }
            return "recurso-form";
        }

        @GetMapping("/teste/recurso-form/sala")
        String sala(Model model) {
            return formulario(model, TipoRecurso.SALAS, vazio(), null);
        }

        @GetMapping("/teste/recurso-form/material")
        String material(Model model) {
            return formulario(model, TipoRecurso.MATERIAIS, vazio(), null);
        }

        @GetMapping("/teste/recurso-form/professor")
        String professor(Model model) {
            return formulario(model, TipoRecurso.PROFESSORES, vazio(), null);
        }

        @GetMapping("/teste/recurso-form/erros")
        String erros(Model model) {
            Map<String, String> erros = new LinkedHashMap<>();
            erros.put("nome", "Informe o nome do recurso.");
            erros.put("capacidade", "Informe uma capacidade maior que zero.");
            erros.put("localizacao", "Informe a localização.");
            return formulario(model, TipoRecurso.SALAS, new FormularioRecurso(null, "", "0", "", null, null, false), erros);
        }

        @GetMapping("/teste/recurso-form/um-erro")
        String umErro(Model model) {
            return formulario(model, TipoRecurso.PROFESSORES, vazio(), Map.of("competencias", "Informe ao menos uma competência."));
        }

        @GetMapping("/teste/recurso-form/erro-geral")
        String erroGeral(Model model) {
            return formulario(model, TipoRecurso.SALAS, vazio(), Map.of("geral", "Já existe um recurso com esse nome."));
        }

        @GetMapping("/teste/recurso-form/novo-preenchido")
        String novoPreenchido(Model model) {
            return formulario(model, TipoRecurso.MATERIAIS,
                    new FormularioRecurso(null, "Projetor P-07", null, "Almoxarifado", "PAT-0042", null, true), null);
        }

        @GetMapping("/teste/recurso-form/edicao-sala")
        String edicaoSala(Model model) {
            return formulario(model, TipoRecurso.SALAS,
                    new FormularioRecurso("7", "Laboratório de Química", "25", "Bloco C, térreo", null, null, true), null);
        }

        @GetMapping("/teste/recurso-form/edicao-professor")
        String edicaoProfessor(Model model) {
            return formulario(model, TipoRecurso.PROFESSORES,
                    new FormularioRecurso("20", "Ana Lima", null, null, null, "Química orgânica, segurança", false), null);
        }

        @GetMapping("/teste/recurso-form/perigoso")
        String perigoso(Model model) {
            return formulario(model, TipoRecurso.PROFESSORES,
                    new FormularioRecurso(null, "\"><script>alert(1)</script>", null, null, null, "</textarea><b>x</b>", false),
                    Map.of("nome", "<i>erro</i>"));
        }
    }

    @Autowired
    MockMvc mvc;

    private Document pagina(String caminho) throws Exception {
        String html = mvc.perform(get(caminho))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Jsoup.parse(html);
    }

    private static void assertCampoObrigatorio(Document doc, String id, String rotulo) {
        Element campo = doc.getElementById(id);
        assertThat(campo).as("campo %s", id).isNotNull();
        assertThat(campo.hasAttr("required")).as("required em %s", id).isTrue();
        assertThat(campo.attr("aria-required")).as("aria-required em %s", id).isEqualTo("true");
        Element label = doc.selectFirst("label[for=" + id + "]");
        assertThat(label).as("rótulo de %s", id).isNotNull();
        assertThat(label.selectFirst(".campo__obrig").text()).isEqualTo("(obrigatório)");
        assertThat(label.text()).isEqualTo(rotulo + " (obrigatório)");
    }

    @Test
    void novoUsaOLayoutBaseComTituloUnicoH1ELinkParaVoltarALista() throws Exception {
        Document doc = pagina("/teste/recurso-form/sala");

        assertThat(doc.title()).isEqualTo("Cadastrar recurso · Organização de Recursos");
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.selectFirst("main#conteudo h1[data-testid=recurso-form-titulo]").text()).isEqualTo("Cadastrar recurso");
        var voltar = doc.selectFirst("a[data-testid=recurso-form-voltar]");
        assertThat(voltar.text()).isEqualTo("Voltar para a lista de recursos");
        assertThat(voltar.attr("href")).isEqualTo("/admin/recursos?tipo=salas");
    }

    @Test
    void novoEnviaPorPostParaAdminRecursosEOferecePostIgualParaSalvarOuCancelar() throws Exception {
        Document doc = pagina("/teste/recurso-form/sala");

        var form = doc.selectFirst("form[data-testid=recurso-form]");
        assertThat(form.attr("method")).isEqualToIgnoringCase("post");
        assertThat(form.attr("action")).isEqualTo("/admin/recursos");
        assertThat(form.hasAttr("novalidate")).isTrue();
        assertThat(form.selectFirst("button[data-testid=recurso-form-salvar]").text()).isEqualTo("Salvar recurso");
        var cancelar = form.selectFirst("a[data-testid=recurso-form-cancelar]");
        assertThat(cancelar.text()).isEqualTo("Cancelar");
        assertThat(cancelar.attr("href")).isEqualTo("/admin/recursos?tipo=salas");
    }

    @Test
    void seletorDeTipoTemTresOpcoesComRotuloEMarcaOTipoAtual() throws Exception {
        Document doc = pagina("/teste/recurso-form/material");

        assertThat(doc.selectFirst("label[for=recurso-form-tipo]").text()).isEqualTo("Tipo de recurso");
        var tipo = doc.selectFirst("select#recurso-form-tipo");
        assertThat(tipo.attr("name")).isEqualTo("tipo");
        assertThat(tipo.attr("data-testid")).isEqualTo("recurso-form-tipo");
        assertThat(tipo.select("option").eachText()).containsExactly("Sala", "Material", "Professor");
        assertThat(tipo.select("option").eachAttr("value")).containsExactly("salas", "materiais", "professores");
        assertThat(tipo.selectFirst("option[selected]").text()).isEqualTo("Material");
    }

    @Test
    void trocarOTipoFuncionaSemJavaScriptComUmBotaoQueRecarregaPorGet() throws Exception {
        Document doc = pagina("/teste/recurso-form/sala");

        var botao = doc.selectFirst("button[data-testid=recurso-form-trocar-tipo]");
        assertThat(botao.text()).isEqualTo("Atualizar campos");
        assertThat(botao.attr("type")).isEqualTo("submit");
        assertThat(botao.attr("formmethod")).isEqualTo("get");
        assertThat(botao.attr("formaction")).isEqualTo("/admin/recursos/novo");
        assertThat(botao.hasAttr("formnovalidate")).isFalse();
    }

    @Test
    void formularioDeSalaTemNomeCapacidadeELocalizacaoObrigatorios() throws Exception {
        Document doc = pagina("/teste/recurso-form/sala");

        assertCampoObrigatorio(doc, "recurso-form-nome", "Nome do recurso");
        assertCampoObrigatorio(doc, "recurso-form-capacidade", "Capacidade (lugares)");
        assertCampoObrigatorio(doc, "recurso-form-localizacao", "Localização");
        assertThat(doc.getElementById("recurso-form-capacidade").attr("type")).isEqualTo("number");
        assertThat(doc.getElementById("recurso-form-localizacao").attr("name")).isEqualTo("localizacao");
        assertThat(doc.select("#recurso-form-patrimonio, #recurso-form-competencias")).isEmpty();
    }

    @Test
    void formularioDeMaterialTemNomePatrimonioELocalizacaoObrigatorios() throws Exception {
        Document doc = pagina("/teste/recurso-form/material");

        assertCampoObrigatorio(doc, "recurso-form-nome", "Nome do recurso");
        assertCampoObrigatorio(doc, "recurso-form-patrimonio", "Número de patrimônio");
        assertCampoObrigatorio(doc, "recurso-form-localizacao", "Localização");
        assertThat(doc.getElementById("recurso-form-patrimonio").attr("name")).isEqualTo("patrimonio");
        assertThat(doc.select("#recurso-form-capacidade, #recurso-form-competencias")).isEmpty();
    }

    @Test
    void formularioDeProfessorTemNomeECompetenciasESemOpcaoDeRestrito() throws Exception {
        Document doc = pagina("/teste/recurso-form/professor");

        assertCampoObrigatorio(doc, "recurso-form-nome", "Nome do professor");
        assertCampoObrigatorio(doc, "recurso-form-competencias", "Competências");
        var competencias = doc.getElementById("recurso-form-competencias");
        assertThat(competencias.tagName()).isEqualTo("textarea");
        assertThat(competencias.hasClass("campo__entrada--area")).isTrue();
        assertThat(competencias.attr("aria-describedby")).isEqualTo("recurso-form-competencias-ajuda");
        assertThat(doc.getElementById("recurso-form-competencias-ajuda").text()).isEqualTo("Separe as competências por vírgula.");
        assertThat(doc.select("#recurso-form-capacidade, #recurso-form-patrimonio, #recurso-form-localizacao")).isEmpty();
        assertThat(doc.select("#recurso-form-restrito, [data-testid=recurso-form-restrito]")).isEmpty();
    }

    @Test
    void recursoRestritoEUmaCaixaDeSelecaoQueExplicaOEstadoSolicitada() throws Exception {
        for (String caminho : new String[] {"/teste/recurso-form/sala", "/teste/recurso-form/material"}) {
            Document doc = pagina(caminho);

            var caixa = doc.selectFirst("input#recurso-form-restrito");
            assertThat(caixa.attr("type")).isEqualTo("checkbox");
            assertThat(caixa.attr("name")).isEqualTo("restrito");
            assertThat(caixa.hasAttr("checked")).isFalse();
            assertThat(doc.selectFirst("label[for=recurso-form-restrito]").text()).isEqualTo("Recurso restrito (exige aprovação)");
            var ajuda = doc.getElementById(caixa.attr("aria-describedby"));
            assertThat(ajuda.text()).isEqualTo("Quem reservar este recurso fica com a reserva Solicitada até um Responsável aprovar.");
            assertThat(ajuda.selectFirst(".estado--solicitada").text()).isEqualTo("Solicitada");
        }
    }

    @Test
    void erroDeCampoApareceJuntoAoCampoEEmResumoComLinksNoTopo() throws Exception {
        Document doc = pagina("/teste/recurso-form/erros");

        var resumo = doc.selectFirst("[data-testid=recurso-form-erros]");
        assertThat(resumo.attr("role")).isEqualTo("alert");
        assertThat(resumo.attr("tabindex")).isEqualTo("-1");
        assertThat(resumo.selectFirst(".alerta__titulo").text()).isEqualTo("Não foi possível salvar o recurso");
        assertThat(resumo.selectFirst(".alerta__texto").text()).isEqualTo("Corrija os 3 campos destacados:");
        assertThat(resumo.select("li a").eachAttr("href"))
                .containsExactly("#recurso-form-nome", "#recurso-form-capacidade", "#recurso-form-localizacao");
        assertThat(resumo.select("li a").eachText()).containsExactly("Nome do recurso", "Capacidade (lugares)", "Localização");

        var nome = doc.getElementById("recurso-form-nome");
        assertThat(nome.attr("aria-invalid")).isEqualTo("true");
        assertThat(nome.attr("aria-describedby")).isEqualTo("recurso-form-nome-erro");
        assertThat(nome.hasClass("campo__entrada--erro")).isTrue();
        var mensagem = doc.getElementById("recurso-form-nome-erro");
        assertThat(mensagem.text()).isEqualTo("Informe o nome do recurso.");
        assertThat(mensagem.attr("data-testid")).isEqualTo("recurso-form-nome-erro");
        assertThat(mensagem.hasClass("campo__erro")).isTrue();
        assertThat(doc.getElementById("recurso-form-capacidade-erro").text()).isEqualTo("Informe uma capacidade maior que zero.");
        assertThat(doc.getElementById("recurso-form-capacidade").val()).isEqualTo("0");
    }

    @Test
    void semErrosNaoHaResumoNemMarcaDeCampoInvalido() throws Exception {
        Document doc = pagina("/teste/recurso-form/sala");

        assertThat(doc.select("[role=alert], .campo__erro, [aria-invalid]")).isEmpty();
    }

    @Test
    void resumoComUmErroFalaNoSingularETemOLinkDoCampoDoTipo() throws Exception {
        Document doc = pagina("/teste/recurso-form/um-erro");

        var resumo = doc.selectFirst("[data-testid=recurso-form-erros]");
        assertThat(resumo.selectFirst(".alerta__texto").text()).isEqualTo("Corrija o campo destacado:");
        assertThat(resumo.selectFirst("li a").attr("href")).isEqualTo("#recurso-form-competencias");
        assertThat(resumo.selectFirst("li a").text()).isEqualTo("Competências");
        assertThat(doc.getElementById("recurso-form-competencias").attr("aria-describedby"))
                .contains("recurso-form-competencias-erro").contains("recurso-form-competencias-ajuda");
    }

    @Test
    void erroSemCampoAparecePeloTextoNoResumoSemLink() throws Exception {
        Document doc = pagina("/teste/recurso-form/erro-geral");

        var item = doc.selectFirst("[data-testid=recurso-form-erros] li");
        assertThat(item.text()).isEqualTo("Já existe um recurso com esse nome.");
        assertThat(item.select("a")).isEmpty();
    }

    @Test
    void edicaoMudaTituloEBotaoEEnviaParaOIdDoRecurso() throws Exception {
        Document doc = pagina("/teste/recurso-form/edicao-sala");

        assertThat(doc.title()).isEqualTo("Editar recurso · Organização de Recursos");
        assertThat(doc.selectFirst("h1[data-testid=recurso-form-titulo]").text()).isEqualTo("Editar recurso");
        var form = doc.selectFirst("form[data-testid=recurso-form]");
        assertThat(form.attr("method")).isEqualToIgnoringCase("post");
        assertThat(form.attr("action")).isEqualTo("/admin/recursos/7");
        assertThat(form.selectFirst("button[data-testid=recurso-form-salvar]").text()).isEqualTo("Salvar alterações");
    }

    @Test
    void edicaoNaoMudaOTipoEVemPreenchida() throws Exception {
        Document doc = pagina("/teste/recurso-form/edicao-sala");

        assertThat(doc.select("select")).isEmpty();
        assertThat(doc.select("[data-testid=recurso-form-trocar-tipo]")).isEmpty();
        assertThat(doc.selectFirst("input[type=hidden][name=tipo]").val()).isEqualTo("salas");
        var tipo = doc.selectFirst("input#recurso-form-tipo");
        assertThat(tipo.val()).isEqualTo("Sala");
        assertThat(tipo.hasAttr("disabled")).isTrue();
        assertThat(doc.selectFirst("label[for=recurso-form-tipo]").text()).isEqualTo("Tipo de recurso");
        assertThat(doc.getElementById("recurso-form-nome").val()).isEqualTo("Laboratório de Química");
        assertThat(doc.getElementById("recurso-form-capacidade").val()).isEqualTo("25");
        assertThat(doc.getElementById("recurso-form-localizacao").val()).isEqualTo("Bloco C, térreo");
        assertThat(doc.getElementById("recurso-form-restrito").hasAttr("checked")).isTrue();
        assertThat(doc.selectFirst("a[data-testid=recurso-form-voltar]").attr("href")).isEqualTo("/admin/recursos?tipo=salas");
    }

    @Test
    void edicaoDeProfessorPreencheCompetenciasNaAreaDeTexto() throws Exception {
        Document doc = pagina("/teste/recurso-form/edicao-professor");

        assertThat(doc.getElementById("recurso-form-competencias").val()).isEqualTo("Química orgânica, segurança");
        assertThat(doc.selectFirst("form[data-testid=recurso-form]").attr("action")).isEqualTo("/admin/recursos/20");
        assertThat(doc.selectFirst("input[type=hidden][name=tipo]").val()).isEqualTo("professores");
    }

    @Test
    void novoVemPreenchidoQuandoATelaRecarregaPorTrocaDeTipo() throws Exception {
        Document doc = pagina("/teste/recurso-form/novo-preenchido");

        assertThat(doc.getElementById("recurso-form-nome").val()).isEqualTo("Projetor P-07");
        assertThat(doc.getElementById("recurso-form-patrimonio").val()).isEqualTo("PAT-0042");
        assertThat(doc.getElementById("recurso-form-localizacao").val()).isEqualTo("Almoxarifado");
        assertThat(doc.getElementById("recurso-form-restrito").hasAttr("checked")).isTrue();
        assertThat(doc.select("select#recurso-form-tipo")).hasSize(1);
    }

    @Test
    void textoDigitadoEMensagensComHtmlPerigosoSaoEscapados() throws Exception {
        Document doc = pagina("/teste/recurso-form/perigoso");

        assertThat(doc.select("main script:not([src]), main b, main i")).isEmpty();
        assertThat(doc.getElementById("recurso-form-nome").val()).isEqualTo("\"><script>alert(1)</script>");
        assertThat(doc.getElementById("recurso-form-competencias").val()).isEqualTo("</textarea><b>x</b>");
        assertThat(doc.getElementById("recurso-form-nome-erro").text()).isEqualTo("<i>erro</i>");
    }

    @Test
    void scriptDaTelaTrocaOTipoSozinhoEATelaFuncionaSemEle() throws Exception {
        Document doc = pagina("/teste/recurso-form/sala");

        assertThat(doc.select("script[src='/js/recurso-form.js'][defer]")).hasSize(1);
    }
}
