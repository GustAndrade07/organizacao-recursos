package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tela 13 do inventário (docs/frontend/telas.md): lista de recursos, prefixo {@code recursos-}, RF-02 e RF-08.
 * O back-end preenche {@code recursos} (id, nome, capacidade já formatada, local, patrimonio, competencias,
 * emManutencao, periodoManutencao, restrito) e, depois de salvar ou bloquear, {@code confirmacao}
 * (titulo, texto); {@code tipo} e {@code nome} vêm da URL. Sem {@code recursos} a página não mostra lista.
 */
@WebMvcTest(ListaRecursosTest.PaginasDeTeste.class)
@Import(ListaRecursosTest.PaginasDeTeste.class)
class ListaRecursosTest {

    @Controller
    static class PaginasDeTeste {

        private static Map<String, Object> sala(String id, String nome, String capacidade, String local, boolean restrito, String periodo) {
            Map<String, Object> recurso = new HashMap<>();
            recurso.put("id", id);
            recurso.put("nome", nome);
            recurso.put("capacidade", capacidade);
            recurso.put("local", local);
            recurso.put("restrito", restrito);
            recurso.put("emManutencao", periodo != null);
            recurso.put("periodoManutencao", periodo);
            return recurso;
        }

        private static List<Map<String, Object>> salas() {
            return List.of(
                    sala("1", "Sala 203", "40 lugares", "Bloco B, 2º andar", false, null),
                    sala("2", "Laboratório de Química", "25 lugares", "Bloco C, térreo", true, null),
                    sala("3", "Auditório Central", "120 lugares", "Bloco A, térreo", false, "10/11/2026 a 20/11/2026"));
        }

        private static void pagina(Model model, TipoRecurso tipo, String nome, List<Map<String, Object>> recursos) {
            model.addAttribute("tipo", tipo);
            model.addAttribute("nome", nome);
            if (recursos != null) {
                model.addAttribute("recursos", recursos);
            }
        }

        @GetMapping("/teste/recursos/salas")
        String listaDeSalas(Model model) {
            pagina(model, TipoRecurso.SALAS, null, salas());
            return "recursos";
        }

        @GetMapping("/teste/recursos/salas-busca")
        String salasComBusca(Model model) {
            pagina(model, TipoRecurso.SALAS, "audit", List.of(salas().get(2)));
            return "recursos";
        }

        @GetMapping("/teste/recursos/materiais")
        String listaDeMateriais(Model model) {
            Map<String, Object> projetor = sala("12", "Projetor portátil P-07", null, "Almoxarifado", false, null);
            projetor.put("patrimonio", "PAT-0042");
            Map<String, Object> notebook = sala("13", "Notebook N-03", null, "Biblioteca", true, "01/12/2026 a 05/12/2026");
            notebook.put("patrimonio", "PAT-0107");
            pagina(model, TipoRecurso.MATERIAIS, null, List.of(projetor, notebook));
            return "recursos";
        }

        @GetMapping("/teste/recursos/professores")
        String listaDeProfessores(Model model) {
            Map<String, Object> ana = sala("20", "Ana Lima", null, null, false, null);
            ana.put("competencias", "Química orgânica, segurança em laboratório");
            Map<String, Object> rafael = sala("21", "Rafael Costa", null, null, false, null);
            rafael.put("competencias", "Física experimental");
            pagina(model, TipoRecurso.PROFESSORES, null, List.of(ana, rafael));
            return "recursos";
        }

        @GetMapping("/teste/recursos/vazio")
        String vazio(Model model) {
            pagina(model, TipoRecurso.SALAS, null, List.of());
            return "recursos";
        }

        @GetMapping("/teste/recursos/vazio-materiais")
        String vazioDeMateriais(Model model) {
            pagina(model, TipoRecurso.MATERIAIS, null, List.of());
            return "recursos";
        }

        @GetMapping("/teste/recursos/vazio-professores")
        String vazioDeProfessores(Model model) {
            pagina(model, TipoRecurso.PROFESSORES, null, List.of());
            return "recursos";
        }

        @GetMapping("/teste/recursos/vazio-busca")
        String vazioComBusca(Model model) {
            pagina(model, TipoRecurso.SALAS, "xyz", List.of());
            return "recursos";
        }

        @GetMapping("/teste/recursos/sem-lista")
        String semLista(Model model) {
            pagina(model, TipoRecurso.SALAS, null, null);
            return "recursos";
        }

        @GetMapping("/teste/recursos/salvo")
        String salvo(Model model) {
            pagina(model, TipoRecurso.SALAS, null, salas());
            model.addAttribute("confirmacao", Map.of("titulo", "Recurso salvo",
                    "texto", "Sala 203 foi atualizada. A lista abaixo já mostra as mudanças."));
            return "recursos";
        }

        @GetMapping("/teste/recursos/perigoso")
        String perigoso(Model model) {
            pagina(model, TipoRecurso.SALAS, "<b>x</b>", List.of(
                    sala("1", "<script>alert(1)</script>", "10 lugares", "<i>Bloco</i>", false, null)));
            model.addAttribute("confirmacao", Map.of("titulo", "<u>Salvo</u>", "texto", "<script>alert(2)</script>"));
            return "recursos";
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

    @Test
    void telaUsaOLayoutBaseComTituloEUmUnicoH1() throws Exception {
        Document doc = pagina("/teste/recursos/salas");

        assertThat(doc.title()).isEqualTo("Recursos · Organização de Recursos");
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.selectFirst("main#conteudo h1[data-testid=recursos-titulo]").text()).isEqualTo("Recursos");
    }

    @Test
    void cabecalhoTemOBotaoCadastrarQueLevaAoFormularioDoTipoAtual() throws Exception {
        Document doc = pagina("/teste/recursos/materiais");

        var cadastrar = doc.selectFirst("header a[data-testid=recursos-cadastrar]");
        assertThat(cadastrar.text()).isEqualTo("Cadastrar recurso");
        assertThat(cadastrar.attr("href")).isEqualTo("/admin/recursos/novo?tipo=materiais");
    }

    @Test
    void abasPorTipoSaoLinksEApenasAAtualTemAriaCurrent() throws Exception {
        Document doc = pagina("/teste/recursos/materiais");

        var abas = doc.selectFirst("nav[data-testid=recursos-abas]");
        assertThat(abas.attr("aria-label")).isEqualTo("Tipo de recurso");
        assertThat(abas.select("a.aba").eachText()).containsExactly("Salas", "Materiais", "Professores");
        assertThat(abas.select("a.aba").eachAttr("href")).containsExactly(
                "/admin/recursos?tipo=salas", "/admin/recursos?tipo=materiais", "/admin/recursos?tipo=professores");
        assertThat(abas.select("a[aria-current]")).hasSize(1);
        assertThat(abas.selectFirst("a[aria-current=page]").attr("data-testid")).isEqualTo("recursos-aba-materiais");
        assertThat(abas.selectFirst("a[data-testid=recursos-aba-salas]").hasAttr("aria-current")).isFalse();
    }

    @Test
    void buscaPorNomeEUmFormularioGetQueMantemOTipoEOTextoDigitado() throws Exception {
        Document doc = pagina("/teste/recursos/salas-busca");

        var form = doc.selectFirst("form[data-testid=recursos-busca]");
        assertThat(form.attr("method")).isEqualToIgnoringCase("get");
        assertThat(form.attr("action")).isEqualTo("/admin/recursos");
        assertThat(form.attr("role")).isEqualTo("search");
        assertThat(form.attr("aria-label")).isEqualTo("Buscar salas por nome");
        assertThat(form.selectFirst("input[type=hidden][name=tipo]").val()).isEqualTo("salas");
        var rotulo = form.selectFirst("label[for=recursos-nome]");
        assertThat(rotulo.text()).isEqualTo("Buscar por nome");
        var campo = form.selectFirst("input#recursos-nome");
        assertThat(campo.attr("type")).isEqualTo("search");
        assertThat(campo.attr("name")).isEqualTo("nome");
        assertThat(campo.val()).isEqualTo("audit");
        assertThat(campo.attr("data-testid")).isEqualTo("recursos-nome");
        assertThat(form.selectFirst("button[type=submit]").text()).isEqualTo("Buscar");
    }

    @Test
    void tabelaDeSalasTemLegendaCabecalhosECelulasRotuladasParaOsCardsDoMobile() throws Exception {
        Document doc = pagina("/teste/recursos/salas");

        var tabela = doc.selectFirst("table[data-testid=recursos-tabela]");
        assertThat(tabela.selectFirst("caption").text()).isEqualTo("Salas cadastradas");
        assertThat(tabela.select("thead th[scope=col]").eachText())
                .containsExactly("Nome", "Capacidade", "Localização", "Situação", "Ações");
        assertThat(tabela.select("tbody tr")).hasSize(3);
        var linha = tabela.selectFirst("tr[data-testid=recursos-linha-1]");
        assertThat(linha.selectFirst("th[scope=row]").text()).isEqualTo("Sala 203");
        assertThat(linha.select("td[data-rotulo]").eachAttr("data-rotulo")).containsExactly("Capacidade", "Localização", "Situação");
        assertThat(linha.selectFirst("td[data-rotulo=Capacidade]").text()).isEqualTo("40 lugares");
        assertThat(linha.selectFirst("td[data-rotulo=Localização]").text()).isEqualTo("Bloco B, 2º andar");
    }

    @Test
    void situacaoAtivoEmManutencaoERestritoAparecemEscritos() throws Exception {
        Document doc = pagina("/teste/recursos/salas");

        var ativo = doc.selectFirst("[data-testid=recursos-situacao-1]");
        assertThat(ativo.text()).isEqualTo("Ativo");
        assertThat(ativo.hasClass("situacao--ativo")).isTrue();
        assertThat(doc.select("[data-testid=recursos-restrito-1], [data-testid=recursos-periodo-1]")).isEmpty();

        var restrito = doc.selectFirst("[data-testid=recursos-restrito-2]");
        assertThat(restrito.text()).isEqualTo("Restrito");
        assertThat(restrito.hasClass("etiqueta")).isTrue();
        assertThat(doc.selectFirst("[data-testid=recursos-situacao-2]").text()).isEqualTo("Ativo");

        var manutencao = doc.selectFirst("[data-testid=recursos-situacao-3]");
        assertThat(manutencao.text()).isEqualTo("Em manutenção");
        assertThat(manutencao.hasClass("situacao--manutencao")).isTrue();
        assertThat(doc.selectFirst("[data-testid=recursos-periodo-3]").text()).isEqualTo("10/11/2026 a 20/11/2026");
        assertThat(doc.select("[data-testid=recursos-situacao-3].situacao--ativo")).isEmpty();
    }

    @Test
    void cadaSalaTemEditarEManutencaoComNomeAcessivel() throws Exception {
        Document doc = pagina("/teste/recursos/salas");

        var editar = doc.selectFirst("a[data-testid=recursos-editar-2]");
        assertThat(editar.text()).isEqualTo("Editar");
        assertThat(editar.attr("aria-label")).isEqualTo("Editar Laboratório de Química");
        assertThat(editar.attr("href")).isEqualTo("/admin/recursos/2?tipo=salas");
        var manutencao = doc.selectFirst("a[data-testid=recursos-manutencao-2]");
        assertThat(manutencao.text()).isEqualTo("Manutenção");
        assertThat(manutencao.attr("aria-label")).isEqualTo("Manutenção de Laboratório de Química");
        assertThat(manutencao.attr("href")).isEqualTo("/admin/recursos/2/manutencao?tipo=salas");
    }

    @Test
    void editarEManutencaoLevamOTipoDaAbaParaATelaVoltarAMesmaAba() throws Exception {
        Document doc = pagina("/teste/recursos/materiais");

        assertThat(doc.selectFirst("a[data-testid=recursos-editar-12]").attr("href")).isEqualTo("/admin/recursos/12?tipo=materiais");
        assertThat(doc.selectFirst("a[data-testid=recursos-manutencao-12]").attr("href"))
                .isEqualTo("/admin/recursos/12/manutencao?tipo=materiais");
    }

    @Test
    void abaMateriaisSegueOPadraoDeSalasComPatrimonioEmVezDeCapacidade() throws Exception {
        Document doc = pagina("/teste/recursos/materiais");

        var tabela = doc.selectFirst("table[data-testid=recursos-tabela]");
        assertThat(tabela.selectFirst("caption").text()).isEqualTo("Materiais cadastrados");
        assertThat(tabela.select("thead th[scope=col]").eachText())
                .containsExactly("Nome", "Patrimônio", "Localização", "Situação", "Ações");
        var linha = tabela.selectFirst("tr[data-testid=recursos-linha-12]");
        assertThat(linha.selectFirst("th[scope=row]").text()).isEqualTo("Projetor portátil P-07");
        assertThat(linha.select("td[data-rotulo]").eachAttr("data-rotulo")).containsExactly("Patrimônio", "Localização", "Situação");
        assertThat(linha.selectFirst("td[data-rotulo=Patrimônio]").text()).isEqualTo("PAT-0042");
        assertThat(linha.selectFirst("td[data-rotulo=Localização]").text()).isEqualTo("Almoxarifado");
        assertThat(doc.selectFirst("[data-testid=recursos-restrito-13]").text()).isEqualTo("Restrito");
        assertThat(doc.selectFirst("[data-testid=recursos-situacao-13]").text()).isEqualTo("Em manutenção");
        assertThat(doc.selectFirst("a[data-testid=recursos-manutencao-12]")).isNotNull();
        assertThat(doc.selectFirst("form[data-testid=recursos-busca]").attr("aria-label")).isEqualTo("Buscar materiais por nome");
    }

    @Test
    void professoresSoTemEditarPoisNaoSaoReservaveisPorEnquanto() throws Exception {
        Document doc = pagina("/teste/recursos/professores");

        var tabela = doc.selectFirst("table[data-testid=recursos-tabela]");
        assertThat(tabela.selectFirst("caption").text()).isEqualTo("Professores cadastrados");
        assertThat(tabela.select("thead th[scope=col]").eachText()).containsExactly("Nome", "Competências", "Situação", "Ações");
        var linha = tabela.selectFirst("tr[data-testid=recursos-linha-20]");
        assertThat(linha.selectFirst("th[scope=row]").text()).isEqualTo("Ana Lima");
        assertThat(linha.selectFirst("td[data-rotulo=Competências]").text()).isEqualTo("Química orgânica, segurança em laboratório");
        assertThat(linha.selectFirst("[data-testid=recursos-situacao-20]").text()).isEqualTo("Ativo");
        var editar = linha.selectFirst("a[data-testid=recursos-editar-20]");
        assertThat(editar.attr("href")).isEqualTo("/admin/recursos/20?tipo=professores");
        assertThat(editar.attr("aria-label")).isEqualTo("Editar Ana Lima");
        assertThat(doc.select("a[data-testid^=recursos-manutencao-]")).isEmpty();
        assertThat(doc.select("[data-testid^=recursos-restrito-]")).isEmpty();
        assertThat(doc.selectFirst("a[data-testid=recursos-cadastrar]").attr("href")).isEqualTo("/admin/recursos/novo?tipo=professores");
    }

    @Test
    void listaVaziaDizOTipoEOferecePrimeiroCadastro() throws Exception {
        Document doc = pagina("/teste/recursos/vazio");

        var vazio = doc.selectFirst("section[data-testid=recursos-vazio]");
        assertThat(vazio.selectFirst("h2").text()).isEqualTo("Nenhuma sala cadastrada");
        assertThat(vazio.attr("aria-labelledby")).isEqualTo(vazio.selectFirst("h2").id());
        var botao = vazio.selectFirst("a[data-testid=recursos-cadastrar-primeiro]");
        assertThat(botao.text()).isEqualTo("Cadastrar o primeiro recurso");
        assertThat(botao.attr("href")).isEqualTo("/admin/recursos/novo?tipo=salas");
        assertThat(doc.select("table")).isEmpty();
    }

    @Test
    void listaVaziaUsaONomeDoTipoNoGeneroCerto() throws Exception {
        assertThat(pagina("/teste/recursos/vazio-materiais").selectFirst("[data-testid=recursos-vazio] h2").text())
                .isEqualTo("Nenhum material cadastrado");
        assertThat(pagina("/teste/recursos/vazio-professores").selectFirst("[data-testid=recursos-vazio] h2").text())
                .isEqualTo("Nenhum professor cadastrado");
    }

    @Test
    void buscaSemResultadoNaoMandaCadastrarOPrimeiroRecurso() throws Exception {
        Document doc = pagina("/teste/recursos/vazio-busca");

        var vazio = doc.selectFirst("section[data-testid=recursos-vazio]");
        assertThat(vazio.selectFirst("h2").text()).isEqualTo("Nenhum resultado para “xyz”");
        assertThat(doc.select("[data-testid=recursos-cadastrar-primeiro]")).isEmpty();
        var limpar = vazio.selectFirst("a[data-testid=recursos-limpar-busca]");
        assertThat(limpar.text()).isEqualTo("Limpar busca");
        assertThat(limpar.attr("href")).isEqualTo("/admin/recursos?tipo=salas");
    }

    @Test
    void semOAtributoRecursosAPaginaNaoMostraListaNemEstadoVazio() throws Exception {
        Document doc = pagina("/teste/recursos/sem-lista");

        assertThat(doc.selectFirst("h1[data-testid=recursos-titulo]")).isNotNull();
        assertThat(doc.selectFirst("form[data-testid=recursos-busca]")).isNotNull();
        assertThat(doc.select("table, [data-testid=recursos-vazio]")).isEmpty();
    }

    @Test
    void confirmacaoDeRecursoSalvoEUmStatusNoTopoAntesDoTitulo() throws Exception {
        Document doc = pagina("/teste/recursos/salvo");

        var aviso = doc.selectFirst("[data-testid=recursos-sucesso]");
        assertThat(aviso.attr("role")).isEqualTo("status");
        assertThat(aviso.hasClass("alerta--sucesso")).isTrue();
        assertThat(aviso.selectFirst(".alerta__titulo").text()).isEqualTo("Recurso salvo");
        assertThat(aviso.selectFirst(".alerta__texto").text()).isEqualTo("Sala 203 foi atualizada. A lista abaixo já mostra as mudanças.");
        assertThat(aviso.elementSiblingIndex()).isLessThan(doc.selectFirst("header.pagina__cabecalho").elementSiblingIndex());
    }

    @Test
    void semConfirmacaoNaoHaAvisoDeSucesso() throws Exception {
        assertThat(pagina("/teste/recursos/salas").select("[data-testid=recursos-sucesso], [role=status]")).isEmpty();
    }

    @Test
    void textoComHtmlPerigosoEEscapado() throws Exception {
        Document doc = pagina("/teste/recursos/perigoso");

        assertThat(doc.select("main script")).isEmpty();
        assertThat(doc.select("main b, main i, main u")).isEmpty();
        assertThat(doc.selectFirst("tr[data-testid=recursos-linha-1] th[scope=row]").text()).isEqualTo("<script>alert(1)</script>");
        assertThat(doc.selectFirst("td[data-rotulo=Localização]").text()).isEqualTo("<i>Bloco</i>");
        assertThat(doc.selectFirst("a[data-testid=recursos-editar-1]").attr("aria-label")).isEqualTo("Editar <script>alert(1)</script>");
        assertThat(doc.selectFirst("input#recursos-nome").val()).isEqualTo("<b>x</b>");
        assertThat(doc.selectFirst("[data-testid=recursos-sucesso] .alerta__texto").text()).isEqualTo("<script>alert(2)</script>");
    }
}
