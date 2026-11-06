package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Tela 15 do inventário (docs/frontend/telas.md): bloqueio por manutenção, prefixo {@code manutencao-},
 * RF-08 e RN-04. O back-end preenche {@code recurso} (id, nome, tipo, local, emManutencao), {@code bloqueio}
 * (recursoId, inicio, fim, motivo), e, depois de um POST recusado, {@code erros} (campo para mensagem) ou
 * {@code afetadas} (reservas que caem no período: data, horario, solicitante, estado com o nome por extenso).
 */
@WebMvcTest(ManutencaoPaginaTest.PaginasDeTeste.class)
@Import(ManutencaoPaginaTest.PaginasDeTeste.class)
class ManutencaoPaginaTest {

    @Controller
    static class PaginasDeTeste {

        private static Map<String, Object> auditorio(boolean emManutencao) {
            Map<String, Object> recurso = new HashMap<>();
            recurso.put("id", "3");
            recurso.put("nome", "Auditório Central");
            recurso.put("tipo", "Sala, 120 lugares");
            recurso.put("local", "Bloco A, térreo");
            recurso.put("emManutencao", emManutencao);
            return recurso;
        }

        private static FormularioBloqueio bloqueio(LocalDateTime inicio, LocalDateTime fim) {
            return new FormularioBloqueio("3", inicio, fim, "Reforma do sistema de som");
        }

        private static String base(Model model, Map<String, Object> recurso, FormularioBloqueio bloqueio) {
            if (recurso != null) {
                model.addAttribute("recurso", recurso);
            }
            model.addAttribute("bloqueio", bloqueio);
            return "manutencao";
        }

        private static Map<String, Object> reserva(LocalDate data, String horario, String solicitante, String estado) {
            return Map.of("data", data, "horario", horario, "solicitante", solicitante, "estado", estado);
        }

        @GetMapping("/teste/manutencao/novo")
        String novo(Model model) {
            return base(model, auditorio(false),
                    new FormularioBloqueio("3", null, null, null));
        }

        @GetMapping("/teste/manutencao/preenchido")
        String preenchido(Model model) {
            return base(model, auditorio(false),
                    bloqueio(LocalDateTime.of(2026, 11, 10, 8, 0), LocalDateTime.of(2026, 11, 20, 18, 0)));
        }

        @GetMapping("/teste/manutencao/em-manutencao")
        String emManutencao(Model model) {
            return base(model, auditorio(true), new FormularioBloqueio("3", null, null, null));
        }

        @GetMapping("/teste/manutencao/sem-recurso")
        String semRecurso(Model model) {
            return base(model, null, new FormularioBloqueio("3", null, null, null));
        }

        @GetMapping("/teste/manutencao/termino")
        String termino(Model model) {
            model.addAttribute("erros", Map.of("fim", "O fim do bloqueio precisa ser depois do início."));
            return base(model, auditorio(false),
                    bloqueio(LocalDateTime.of(2026, 11, 20, 8, 0), LocalDateTime.of(2026, 11, 10, 18, 0)));
        }

        @GetMapping("/teste/manutencao/tres-erros")
        String tresErros(Model model) {
            Map<String, String> erros = new LinkedHashMap<>();
            erros.put("inicio", "Informe o início do bloqueio.");
            erros.put("fim", "Informe o fim do bloqueio.");
            erros.put("motivo", "Informe o motivo.");
            model.addAttribute("erros", erros);
            return base(model, auditorio(false), new FormularioBloqueio("3", null, null, ""));
        }

        @GetMapping("/teste/manutencao/afetadas")
        String afetadas(Model model) {
            model.addAttribute("afetadas", List.of(
                    reserva(LocalDate.of(2026, 11, 12), "14:00 às 16:00", "Beatriz Nogueira", "Aprovada"),
                    reserva(LocalDate.of(2026, 11, 18), "09:00 às 11:00", "Rafael Costa", "Solicitada")));
            return base(model, auditorio(false),
                    bloqueio(LocalDateTime.of(2026, 11, 10, 8, 0), LocalDateTime.of(2026, 11, 20, 18, 0)));
        }

        @GetMapping("/teste/manutencao/com-tipo")
        String comTipo(Model model) {
            model.addAttribute("tipo", TipoRecurso.MATERIAIS);
            return base(model, auditorio(false), new FormularioBloqueio("3", null, null, null));
        }

        @GetMapping("/teste/manutencao/afetada-uma")
        String afetadaUma(Model model) {
            model.addAttribute("afetadas", List.of(
                    reserva(LocalDate.of(2026, 11, 12), "14:00 às 16:00", "Beatriz Nogueira", "Em uso")));
            return base(model, auditorio(false),
                    bloqueio(LocalDateTime.of(2026, 11, 10, 8, 0), LocalDateTime.of(2026, 11, 20, 18, 0)));
        }

        @GetMapping("/teste/manutencao/afetada-desconhecida")
        String afetadaDesconhecida(Model model) {
            model.addAttribute("afetadas", List.of(
                    reserva(LocalDate.of(2026, 11, 12), "14:00 às 16:00", "Beatriz Nogueira", "Em análise"),
                    reserva(LocalDate.of(2026, 11, 13), "08:00 às 09:00", "Rafael Costa", "Não compareceu")));
            return base(model, auditorio(false),
                    bloqueio(LocalDateTime.of(2026, 11, 10, 8, 0), LocalDateTime.of(2026, 11, 20, 18, 0)));
        }

        @GetMapping("/teste/manutencao/afetadas-vazio")
        String afetadasVazio(Model model) {
            model.addAttribute("afetadas", List.of());
            return base(model, auditorio(false),
                    bloqueio(LocalDateTime.of(2026, 11, 10, 8, 0), LocalDateTime.of(2026, 11, 20, 18, 0)));
        }

        @GetMapping("/teste/manutencao/perigoso")
        String perigoso(Model model) {
            Map<String, Object> recurso = auditorio(false);
            recurso.put("nome", "<script>alert(1)</script>");
            recurso.put("local", "<i>Bloco</i>");
            model.addAttribute("erros", Map.of("motivo", "<b>erro</b>"));
            model.addAttribute("afetadas", List.of(
                    reserva(LocalDate.of(2026, 11, 12), "14:00 às 16:00", "<u>Fulano</u>", "Aprovada")));
            return base(model, recurso, new FormularioBloqueio("3", null, null, "</textarea><b>x</b>"));
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
        assertThat(label.text()).isEqualTo(rotulo + " (obrigatório)");
        assertThat(label.selectFirst(".campo__obrig").text()).isEqualTo("(obrigatório)");
    }

    @Test
    void telaUsaOLayoutBaseComTituloUnicoH1ELinkParaVoltarALista() throws Exception {
        Document doc = pagina("/teste/manutencao/novo");

        assertThat(doc.title()).isEqualTo("Bloquear para manutenção · Organização de Recursos");
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.selectFirst("main#conteudo h1[data-testid=manutencao-titulo]").text()).isEqualTo("Bloquear para manutenção");
        var voltar = doc.selectFirst("a[data-testid=manutencao-voltar]");
        assertThat(voltar.text()).isEqualTo("Voltar para a lista de recursos");
        assertThat(voltar.attr("href")).isEqualTo("/admin/recursos");
    }

    @Test
    void resumoMostraORecursoComTipoLocalESituacaoEscrita() throws Exception {
        Document doc = pagina("/teste/manutencao/novo");

        var resumo = doc.selectFirst("section[data-testid=manutencao-recurso]");
        assertThat(resumo.selectFirst("h2").text()).isEqualTo("Auditório Central");
        assertThat(resumo.attr("aria-labelledby")).isEqualTo(resumo.selectFirst("h2").id());
        assertThat(resumo.select("dt").eachText()).containsExactly("Tipo", "Localização");
        assertThat(resumo.select("dd").eachText()).containsExactly("Sala, 120 lugares", "Bloco A, térreo");
        var situacao = resumo.selectFirst(".situacao");
        assertThat(situacao.text()).isEqualTo("Ativo");
        assertThat(situacao.hasClass("situacao--ativo")).isTrue();
        assertThat(resumo.selectFirst(".cartao__linha .situacao")).isNotNull();
    }

    @Test
    void recursoJaEmManutencaoMostraASituacaoEscrita() throws Exception {
        var situacao = pagina("/teste/manutencao/em-manutencao").selectFirst("[data-testid=manutencao-recurso] .situacao");

        assertThat(situacao.text()).isEqualTo("Em manutenção");
        assertThat(situacao.hasClass("situacao--manutencao")).isTrue();
    }

    @Test
    void semORecursoNoModeloAPaginaMostraSoOFormulario() throws Exception {
        Document doc = pagina("/teste/manutencao/sem-recurso");

        assertThat(doc.select("[data-testid=manutencao-recurso]")).isEmpty();
        assertThat(doc.selectFirst("form[data-testid=manutencao-form]").attr("action")).isEqualTo("/admin/recursos/3/manutencao");
    }

    @Test
    void formularioEnviaPorPostParaOBloqueioDoRecurso() throws Exception {
        Document doc = pagina("/teste/manutencao/novo");

        var form = doc.selectFirst("form[data-testid=manutencao-form]");
        assertThat(form.attr("method")).isEqualToIgnoringCase("post");
        assertThat(form.attr("action")).isEqualTo("/admin/recursos/3/manutencao");
        assertThat(form.id()).isEqualTo("manutencao-form");
        assertThat(form.hasAttr("novalidate")).isTrue();
        assertThat(form.selectFirst("button[type=submit][data-testid=manutencao-bloquear]").text()).isEqualTo("Bloquear recurso");
        var cancelar = form.selectFirst("a[data-testid=manutencao-cancelar]");
        assertThat(cancelar.text()).isEqualTo("Cancelar");
        assertThat(cancelar.attr("href")).isEqualTo("/admin/recursos");
    }

    @Test
    void inicioFimEMotivoSaoObrigatoriosComRotuloVisivelETipoCerto() throws Exception {
        Document doc = pagina("/teste/manutencao/novo");

        assertCampoObrigatorio(doc, "manutencao-inicio", "Início do bloqueio");
        assertCampoObrigatorio(doc, "manutencao-fim", "Fim do bloqueio");
        assertCampoObrigatorio(doc, "manutencao-motivo", "Motivo");
        assertThat(doc.getElementById("manutencao-inicio").attr("type")).isEqualTo("datetime-local");
        assertThat(doc.getElementById("manutencao-inicio").attr("name")).isEqualTo("inicio");
        assertThat(doc.getElementById("manutencao-fim").attr("type")).isEqualTo("datetime-local");
        assertThat(doc.getElementById("manutencao-fim").attr("name")).isEqualTo("fim");
        var motivo = doc.getElementById("manutencao-motivo");
        assertThat(motivo.tagName()).isEqualTo("textarea");
        assertThat(motivo.attr("name")).isEqualTo("motivo");
        assertThat(motivo.hasClass("campo__entrada--area")).isTrue();
    }

    @Test
    void formularioVemPreenchidoComOPeriodoEOMotivo() throws Exception {
        Document doc = pagina("/teste/manutencao/preenchido");

        assertThat(doc.getElementById("manutencao-inicio").val()).isEqualTo("2026-11-10T08:00");
        assertThat(doc.getElementById("manutencao-fim").val()).isEqualTo("2026-11-20T18:00");
        assertThat(doc.getElementById("manutencao-motivo").val()).isEqualTo("Reforma do sistema de som");
    }

    @Test
    void fimAntesDoInicioMostraErroJuntoAoCampoComRoleAlert() throws Exception {
        Document doc = pagina("/teste/manutencao/termino");

        var fim = doc.getElementById("manutencao-fim");
        assertThat(fim.attr("aria-invalid")).isEqualTo("true");
        assertThat(fim.attr("aria-describedby")).isEqualTo("manutencao-fim-erro");
        assertThat(fim.hasClass("campo__entrada--erro")).isTrue();
        var erro = doc.getElementById("manutencao-fim-erro");
        assertThat(erro.attr("role")).isEqualTo("alert");
        assertThat(erro.hasAttr("hidden")).isFalse();
        assertThat(erro.text()).isEqualTo("O fim do bloqueio precisa ser depois do início.");
        assertThat(erro.attr("data-testid")).isEqualTo("manutencao-fim-erro");
        assertThat(doc.getElementById("manutencao-inicio").hasAttr("aria-invalid")).isFalse();
    }

    @Test
    void semErroOCampoDeErroDoFimExisteEscondidoParaOScriptPreencher() throws Exception {
        Document doc = pagina("/teste/manutencao/novo");

        var erro = doc.getElementById("manutencao-fim-erro");
        assertThat(erro.hasAttr("hidden")).isTrue();
        assertThat(erro.attr("role")).isEqualTo("alert");
        assertThat(erro.text()).isEmpty();
        assertThat(doc.getElementById("manutencao-fim").hasAttr("aria-invalid")).isFalse();
        assertThat(doc.getElementById("manutencao-fim").hasAttr("aria-describedby")).isFalse();
        assertThat(doc.select("[data-testid=manutencao-afetadas]")).isEmpty();
    }

    @Test
    void errosDoServidorAparecemJuntoDeCadaCampo() throws Exception {
        Document doc = pagina("/teste/manutencao/tres-erros");

        assertThat(doc.getElementById("manutencao-inicio-erro").text()).isEqualTo("Informe o início do bloqueio.");
        assertThat(doc.getElementById("manutencao-fim-erro").text()).isEqualTo("Informe o fim do bloqueio.");
        assertThat(doc.getElementById("manutencao-motivo-erro").text()).isEqualTo("Informe o motivo.");
        assertThat(doc.getElementById("manutencao-inicio").attr("aria-describedby")).isEqualTo("manutencao-inicio-erro");
        assertThat(doc.getElementById("manutencao-motivo").attr("aria-describedby")).isEqualTo("manutencao-motivo-erro");
        assertThat(doc.getElementById("manutencao-motivo").attr("aria-invalid")).isEqualTo("true");
        assertThat(doc.select(".campo__erro:not([hidden])")).hasSize(3);
    }

    @Test
    void reservasAfetadasViramAvisoComListaDataSolicitanteEEstadoEscrito() throws Exception {
        Document doc = pagina("/teste/manutencao/afetadas");

        var aviso = doc.selectFirst("[data-testid=manutencao-afetadas]");
        assertThat(aviso.attr("role")).isEqualTo("alert");
        assertThat(aviso.attr("tabindex")).isEqualTo("-1");
        assertThat(aviso.hasClass("alerta--aviso")).isTrue();
        assertThat(aviso.selectFirst(".alerta__titulo").text()).isEqualTo("2 reservas caem no período do bloqueio");
        var itens = aviso.select("ul[data-testid=manutencao-afetadas-lista] > li");
        assertThat(itens).hasSize(2);
        assertThat(itens.get(0).attr("data-testid")).isEqualTo("manutencao-afetada-1");
        assertThat(itens.get(0).selectFirst("span").text()).isEqualTo("12/11/2026, 14:00 às 16:00, Beatriz Nogueira");
        assertThat(itens.get(0).selectFirst(".estado").text()).isEqualTo("Aprovada");
        assertThat(itens.get(0).selectFirst(".estado").hasClass("estado--aprovada")).isTrue();
        assertThat(itens.get(1).selectFirst("span").text()).isEqualTo("18/11/2026, 09:00 às 11:00, Rafael Costa");
        assertThat(itens.get(1).selectFirst(".estado").text()).isEqualTo("Solicitada");
        assertThat(itens.get(1).selectFirst(".estado").hasClass("estado--solicitada")).isTrue();
    }

    @Test
    void oBadgeDoEstadoVemDoFragmentoComumEDeixaSoOTextoQuandoOEstadoENovo() throws Exception {
        Document doc = pagina("/teste/manutencao/afetada-desconhecida");

        var novo = doc.selectFirst("[data-testid=manutencao-afetada-1] .estado");
        assertThat(novo.text()).isEqualTo("Em análise");
        assertThat(novo.classNames()).containsExactly("estado");
        var naoCompareceu = doc.selectFirst("[data-testid=manutencao-afetada-2] .estado");
        assertThat(naoCompareceu.hasClass("estado--nao-compareceu")).isTrue();
        assertThat(doc.select("[data-testid=manutencao-afetada-estado]")).hasSize(2);
    }

    @Test
    void oFormularioEnviaUmaVezSoInclusiveAoConfirmarOBloqueio() throws Exception {
        assertThat(pagina("/teste/manutencao/novo").getElementById("manutencao-form").hasAttr("data-envio-unico")).isTrue();
        Document afetadas = pagina("/teste/manutencao/afetadas");
        var form = afetadas.getElementById("manutencao-form");
        assertThat(form.hasAttr("data-envio-unico")).isTrue();
        assertThat(afetadas.selectFirst("button[data-testid=manutencao-confirmar]").attr("form")).isEqualTo(form.id());
    }

    @Test
    void umaReservaAfetadaFalaNoSingular() throws Exception {
        Document doc = pagina("/teste/manutencao/afetada-uma");

        assertThat(doc.selectFirst("[data-testid=manutencao-afetadas] .alerta__titulo").text())
                .isEqualTo("1 reserva cai no período do bloqueio");
        assertThat(doc.selectFirst("[data-testid=manutencao-afetada-1] .estado").hasClass("estado--em-uso")).isTrue();
    }

    @Test
    void comReservasAfetadasOAvisoOferecePrimeiroConfirmarOuVoltarEAjustar() throws Exception {
        Document doc = pagina("/teste/manutencao/afetadas");

        var aviso = doc.selectFirst("[data-testid=manutencao-afetadas]");
        var confirmar = aviso.selectFirst("button[data-testid=manutencao-confirmar]");
        assertThat(confirmar.text()).isEqualTo("Confirmar bloqueio");
        assertThat(confirmar.attr("type")).isEqualTo("submit");
        assertThat(confirmar.attr("form")).isEqualTo("manutencao-form");
        assertThat(confirmar.attr("name")).isEqualTo("confirmar");
        assertThat(confirmar.attr("value")).isEqualTo("true");
        assertThat(confirmar.hasClass("botao--perigo")).isTrue();
        var ajustar = aviso.selectFirst("a[data-testid=manutencao-ajustar]");
        assertThat(ajustar.text()).isEqualTo("Voltar e ajustar o período");
        assertThat(ajustar.attr("href")).startsWith("/admin/recursos/3/manutencao?")
                .contains("inicio=2026-11-10T08:00").contains("fim=2026-11-20T18:00").contains("motivo=");
        assertThat(doc.getElementById("manutencao-form")).isNotNull();
        assertThat(doc.select("button[data-testid=manutencao-bloquear]")).isEmpty();
    }

    @Test
    void comReservasAfetadasInicioFimEMotivoSaoSomenteLeituraParaNaoBurlarOAviso() throws Exception {
        Document doc = pagina("/teste/manutencao/afetadas");

        for (String id : new String[] {"manutencao-inicio", "manutencao-fim", "manutencao-motivo"}) {
            assertThat(doc.getElementById(id).hasAttr("readonly")).as("readonly em %s", id).isTrue();
            assertThat(doc.getElementById(id).hasAttr("required")).as("required em %s", id).isTrue();
        }
    }

    @Test
    void semReservasAfetadasOsCamposSaoEditaveis() throws Exception {
        Document doc = pagina("/teste/manutencao/preenchido");

        assertThat(doc.select("#manutencao-inicio[readonly], #manutencao-fim[readonly], #manutencao-motivo[readonly]")).isEmpty();
    }

    @Test
    void comReservasAfetadasOPrimeiroBotaoDeEnvioDoFormularioEOConfirmarEOAvisoVemAntesDele() throws Exception {
        Document doc = pagina("/teste/manutencao/afetadas");

        var form = doc.getElementById("manutencao-form");
        var botoes = doc.select("button[type=submit]").stream()
                .filter(b -> form.equals(b.closest("form")) || "manutencao-form".equals(b.attr("form")))
                .toList();
        assertThat(botoes).isNotEmpty();
        assertThat(botoes.get(0).attr("data-testid")).isEqualTo("manutencao-confirmar");
        var aviso = doc.selectFirst("[data-testid=manutencao-afetadas]");
        assertThat(aviso.elementSiblingIndex()).isLessThan(form.elementSiblingIndex());
    }

    @Test
    void voltarECancelarLevamParaAAbaDoTipoQuandoOBackEndInforma() throws Exception {
        Document doc = pagina("/teste/manutencao/com-tipo");

        assertThat(doc.selectFirst("a[data-testid=manutencao-voltar]").attr("href")).isEqualTo("/admin/recursos?tipo=materiais");
        assertThat(doc.selectFirst("a[data-testid=manutencao-cancelar]").attr("href")).isEqualTo("/admin/recursos?tipo=materiais");
    }

    @Test
    void listaVaziaDeAfetadasNaoMostraAviso() throws Exception {
        Document doc = pagina("/teste/manutencao/afetadas-vazio");

        assertThat(doc.select("[data-testid=manutencao-afetadas]")).isEmpty();
        assertThat(doc.selectFirst("button[data-testid=manutencao-bloquear]")).isNotNull();
    }

    @Test
    void textoComHtmlPerigosoEEscapado() throws Exception {
        Document doc = pagina("/teste/manutencao/perigoso");

        assertThat(doc.select("main script:not([src]), main b, main i, main u")).isEmpty();
        assertThat(doc.selectFirst("[data-testid=manutencao-recurso] h2").text()).isEqualTo("<script>alert(1)</script>");
        assertThat(doc.selectFirst("[data-testid=manutencao-recurso] dd:last-of-type").text()).isEqualTo("<i>Bloco</i>");
        assertThat(doc.getElementById("manutencao-motivo").val()).isEqualTo("</textarea><b>x</b>");
        assertThat(doc.getElementById("manutencao-motivo-erro").text()).isEqualTo("<b>erro</b>");
        assertThat(doc.selectFirst("[data-testid=manutencao-afetada-1] span").text()).contains("<u>Fulano</u>");
    }

    /** Só confere que o script é carregado; o comportamento dele fica para o E2E de 375 px (RF-13). */
    @Test
    void carregaOScriptDaTelaComDefer() throws Exception {
        Document doc = pagina("/teste/manutencao/novo");

        assertThat(doc.select("script[src='/js/manutencao-form.js'][defer]")).hasSize(1);
    }
}
