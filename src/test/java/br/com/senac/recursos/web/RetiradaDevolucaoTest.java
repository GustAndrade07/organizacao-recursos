package br.com.senac.recursos.web;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.support.ComoUsuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela 10 do inventário (docs/frontend/telas.md): retirada e devolução, prefixo {@code retirada-devolucao-},
 * RF-09, RN-05 e RN-08. O back-end preenche {@code reservas} (id, recurso, solicitante, data e horario já
 * formatados, estado como texto e {@code horarioPassou}); {@code naoCompareceu} (a reserva em confirmação) e
 * {@code confirmacao} (titulo, reserva, estado). O controller preenche {@code filtro}. A tela só oferece a
 * ação que o estado permite: Aprovada retira, Em uso devolve, qualquer outro estado fica sem ação.
 */
@ComoUsuario(PerfilUsuario.RESPONSAVEL)
@WebMvcTest({RetiradaDevolucaoController.class, RetiradaDevolucaoTest.PaginasDeTeste.class})
@Import(RetiradaDevolucaoTest.PaginasDeTeste.class)
class RetiradaDevolucaoTest {

    @Controller
    static class PaginasDeTeste {

        private static Map<String, Object> reserva(String id, String recurso, String solicitante, String data,
                String horario, String estado, boolean horarioPassou) {
            return Map.of("id", id, "recurso", recurso, "solicitante", solicitante, "data", data,
                    "horario", horario, "estado", estado, "horarioPassou", horarioPassou);
        }

        private static List<Map<String, Object>> lista() {
            return List.of(
                    reserva("1", "Projetor portátil P-07", "Ana Lima", "06/11/2026", "10:00 às 12:00", "Em uso", false),
                    reserva("2", "Sala 203", "Rafael Costa", "06/11/2026", "14:00 às 16:00", "Aprovada", false),
                    reserva("3", "Laboratório de Química", "Laura Mendes", "09/11/2026", "08:00 às 10:00", "Aprovada", false),
                    reserva("4", "Kit de microscópio M-02", "Beatriz Nogueira", "05/11/2026", "09:00 às 11:00", "Aprovada", true),
                    reserva("5", "Sala 203", "Ana Lima", "28/10/2026", "19:00 às 21:00", "Concluída", false));
        }

        private static void filtro(Model model) {
            model.addAttribute("filtro", "ativas");
        }

        @GetMapping("/teste/retirada-devolucao/lista")
        String lista(Model model) {
            filtro(model);
            model.addAttribute("reservas", lista());
            return "retirada-devolucao";
        }

        @GetMapping("/teste/retirada-devolucao/todos")
        String todos(Model model) {
            model.addAttribute("filtro", "todos");
            // horarioPassou verdadeiro em todo estado sem retirada: a etiqueta e o botão não podem vazar
            model.addAttribute("reservas", List.of(
                    reserva("1", "Sala 203", "Ana Lima", "06/11/2026", "10:00 às 12:00", "Solicitada", true),
                    reserva("2", "Sala 203", "Ana Lima", "06/11/2026", "10:00 às 12:00", "Aprovada", false),
                    reserva("3", "Sala 203", "Ana Lima", "06/11/2026", "10:00 às 12:00", "Em uso", false),
                    reserva("4", "Sala 203", "Ana Lima", "06/11/2026", "10:00 às 12:00", "Concluída", true),
                    reserva("5", "Sala 203", "Ana Lima", "06/11/2026", "10:00 às 12:00", "Rejeitada", true),
                    reserva("6", "Sala 203", "Ana Lima", "06/11/2026", "10:00 às 12:00", "Cancelada", true),
                    reserva("7", "Sala 203", "Ana Lima", "06/11/2026", "10:00 às 12:00", "Não compareceu", true)));
            return "retirada-devolucao";
        }

        @GetMapping("/teste/retirada-devolucao/estado-desconhecido")
        String estadoDesconhecido(Model model) {
            filtro(model);
            Map<String, Object> semEstado = new HashMap<>(reserva("2", "Sala 203", "Ana Lima", "06/11/2026", "10:00 às 12:00", "x", true));
            semEstado.put("estado", null);
            model.addAttribute("reservas", List.of(
                    reserva("1", "Sala 203", "Ana Lima", "06/11/2026", "10:00 às 12:00", "Em análise", true),
                    semEstado,
                    reserva("3", "Sala 203", "Ana Lima", "06/11/2026", "10:00 às 12:00", "aprovada", true),
                    reserva("4", "Sala 203", "Ana Lima", "06/11/2026", "10:00 às 12:00", "EM USO", true)));
            return "retirada-devolucao";
        }

        @GetMapping("/teste/retirada-devolucao/vazia")
        String vazia(Model model) {
            filtro(model);
            model.addAttribute("reservas", List.of());
            return "retirada-devolucao";
        }

        @GetMapping("/teste/retirada-devolucao/passou-em-uso")
        String passouEmUso(Model model) {
            filtro(model);
            model.addAttribute("reservas", List.of(
                    reserva("8", "Sala 203", "Ana Lima", "05/11/2026", "09:00 às 11:00", "Em uso", true)));
            return "retirada-devolucao";
        }

        @GetMapping("/teste/retirada-devolucao/confirmando-nc")
        String confirmandoNaoCompareceu(Model model) {
            model.addAttribute("filtro", "aprovada");
            model.addAttribute("reservas", lista());
            model.addAttribute("naoCompareceu", lista().get(3));
            return "retirada-devolucao";
        }

        @GetMapping("/teste/retirada-devolucao/confirmando-nc-custom")
        String confirmandoNaoCompareceuCustom(@RequestParam String estado, @RequestParam boolean passou, Model model) {
            filtro(model);
            model.addAttribute("reservas", lista());
            model.addAttribute("naoCompareceu", reserva("4", "Kit de microscópio M-02", "Beatriz Nogueira", "05/11/2026",
                    "09:00 às 11:00", estado, passou));
            return "retirada-devolucao";
        }

        @GetMapping("/teste/retirada-devolucao/retirada-registrada")
        String retiradaRegistrada(Model model) {
            filtro(model);
            model.addAttribute("reservas", lista());
            model.addAttribute("confirmacao", Map.of("titulo", "Retirada registrada",
                    "reserva", "Sala 203, de Rafael Costa", "estado", "Em uso"));
            return "retirada-devolucao";
        }

        @GetMapping("/teste/retirada-devolucao/devolucao-registrada")
        String devolucaoRegistrada(Model model) {
            filtro(model);
            model.addAttribute("reservas", lista());
            model.addAttribute("confirmacao", Map.of("titulo", "Devolução registrada",
                    "reserva", "Projetor portátil P-07", "estado", "Concluída"));
            return "retirada-devolucao";
        }

        @GetMapping("/teste/retirada-devolucao/perigoso")
        String perigoso(Model model) {
            filtro(model);
            Map<String, Object> reserva = reserva("1", "<script>alert(1)</script>", "<img src=x onerror=alert(2)>",
                    "05/11/2026", "09:00 às 11:00", "Aprovada", true);
            model.addAttribute("reservas", List.of(reserva));
            model.addAttribute("naoCompareceu", reserva);
            model.addAttribute("confirmacao", Map.of("titulo", "<b>Retirada registrada</b>",
                    "reserva", "<script>alert(3)</script>", "estado", "Em uso"));
            return "retirada-devolucao";
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

    // Rota real: o controller devolve a view e o filtro escolhido

    @Test
    void rotaGetRetiradaDevolucaoUsaOLayoutBaseComTituloEUmUnicoH1() throws Exception {
        Document doc = pagina("/retirada-devolucao");

        assertThat(doc.title()).isEqualTo("Retirada e devolução · Organização de Recursos");
        assertThat(doc.select("title")).as("um só title, o do layout").hasSize(1);
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.selectFirst("main#conteudo h1[data-testid=retirada-devolucao-titulo]").text()).isEqualTo("Retirada e devolução");
        assertThat(doc.selectFirst("[data-testid=retirada-devolucao-vazio] h2").text())
                .isEqualTo("Nenhuma reserva aguardando retirada ou devolução");
    }

    @Test
    void semParametroOFiltroPadraoEAprovadasEEmUso() throws Exception {
        Document doc = pagina("/retirada-devolucao");

        assertThat(doc.select("#retirada-devolucao-estado option[selected]").eachAttr("value")).containsExactly("ativas");
    }

    @ParameterizedTest
    @CsvSource({
            "aprovada, aprovada",
            "em-uso, em-uso",
            "todos, todos",
            "ativas, ativas",
            "valor-invalido, ativas",
            "'', ativas"
    })
    void controllerDevolveOFiltroEscolhidoOuOPadrao(String parametro, String esperado) throws Exception {
        Document doc = pagina("/retirada-devolucao?estado=" + parametro);

        assertThat(doc.select("#retirada-devolucao-estado option[selected]").eachAttr("value")).containsExactly(esperado);
    }

    // Filtro por estado

    @Test
    void filtroEnviaPorGetParaAPropriaRotaComRotuloLigadoPeloFor() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/lista");

        var form = doc.selectFirst("form[data-testid=retirada-devolucao-filtros]");
        assertThat(form.attr("method")).isEqualToIgnoringCase("get");
        assertThat(form.attr("action")).isEqualTo("/retirada-devolucao");
        var seletor = form.selectFirst("select[data-testid=retirada-devolucao-estado]");
        assertThat(seletor.attr("name")).isEqualTo("estado");
        assertThat(form.selectFirst("label[for=" + seletor.id() + "]").text()).isEqualTo("Estado");
        assertThat(seletor.select("option").eachText()).containsExactly("Aprovadas e em uso", "Aprovada", "Em uso", "Todos os estados");
        assertThat(seletor.select("option").eachAttr("value")).containsExactly("ativas", "aprovada", "em-uso", "todos");
        assertThat(form.selectFirst("button[type=submit][data-testid=retirada-devolucao-filtrar]").text()).isEqualTo("Aplicar");
    }

    @Test
    void filtroMostraComoSelecionadoOQueVeioDoModelo() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/todos");

        assertThat(doc.select("#retirada-devolucao-estado option[selected]").eachText()).containsExactly("Todos os estados");
    }

    // Lista

    @Test
    void listaTemTabelaComLegendaCabecalhosECelulasRotuladasParaOsCardsDoMobile() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/lista");

        var tabela = doc.selectFirst("table.tabela[data-testid=retirada-devolucao-tabela]");
        assertThat(tabela.selectFirst("caption").text()).isNotBlank();
        assertThat(tabela.select("thead th[scope=col]").eachText())
                .containsExactly("Recurso", "Solicitante", "Data", "Horário", "Estado", "Ações");
        assertThat(tabela.select("tbody tr").eachAttr("data-testid"))
                .containsExactly("retirada-devolucao-linha-1", "retirada-devolucao-linha-2", "retirada-devolucao-linha-3",
                        "retirada-devolucao-linha-4", "retirada-devolucao-linha-5");
        var linha = tabela.selectFirst("tr[data-testid=retirada-devolucao-linha-2]");
        assertThat(linha.selectFirst("th[scope=row]").text()).isEqualTo("Sala 203");
        assertThat(linha.select("td[data-rotulo]").eachAttr("data-rotulo")).containsExactly("Solicitante", "Data", "Horário", "Estado");
        assertThat(linha.select("td[data-rotulo]").eachText()).containsExactly("Rafael Costa", "06/11/2026", "14:00 às 16:00", "Aprovada");
    }

    @ParameterizedTest
    @CsvSource({
            "1, Solicitada, solicitada",
            "2, Aprovada, aprovada",
            "3, Em uso, em-uso",
            "4, Concluída, concluida",
            "5, Rejeitada, rejeitada",
            "6, Cancelada, cancelada",
            "7, Não compareceu, nao-compareceu"
    })
    void cadaEstadoApareceEscritoNoBadgeComASuaClasse(String id, String nome, String sufixo) throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/todos");

        var badge = doc.selectFirst("[data-testid=retirada-devolucao-estado-" + id + "]");
        assertThat(badge.text()).isEqualTo(nome);
        assertThat(badge.hasClass("estado")).isTrue();
        assertThat(badge.hasClass("estado--" + sufixo)).isTrue();
    }

    // Ações por estado (RN-05)

    @Test
    void aprovadaOfereceRegistrarRetiradaPorPostComNomeAcessivel() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/lista");

        var botao = doc.selectFirst("button[data-testid=retirada-devolucao-retirar-2]");
        assertThat(botao.text()).isEqualTo("Registrar retirada");
        assertThat(botao.attr("type")).isEqualTo("submit");
        assertThat(botao.attr("aria-label")).isEqualTo("Registrar retirada de Sala 203 por Rafael Costa");
        assertThat(botao.closest("form").attr("method")).isEqualToIgnoringCase("post");
        assertThat(botao.closest("form").attr("action")).isEqualTo("/retirada-devolucao/2/retirar");
        assertThat(doc.select("[data-testid=retirada-devolucao-devolver-2]")).isEmpty();
    }

    @Test
    void emUsoOfereceRegistrarDevolucaoPorPostENaoOferecePrimeiraRetirada() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/lista");

        var botao = doc.selectFirst("button[data-testid=retirada-devolucao-devolver-1]");
        assertThat(botao.text()).isEqualTo("Registrar devolução");
        assertThat(botao.attr("aria-label")).isEqualTo("Registrar devolução de Projetor portátil P-07 por Ana Lima");
        assertThat(botao.closest("form").attr("method")).isEqualToIgnoringCase("post");
        assertThat(botao.closest("form").attr("action")).isEqualTo("/retirada-devolucao/1/devolver");
        assertThat(doc.select("[data-testid=retirada-devolucao-retirar-1]")).isEmpty();
    }

    @Test
    void estadoSemTransicaoMostraSemAcaoDisponivelEnenhumBotao() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/lista");

        var linha = doc.selectFirst("tr[data-testid=retirada-devolucao-linha-5]");
        assertThat(linha.selectFirst("[data-testid=retirada-devolucao-sem-acao-5]").text()).isEqualTo("Sem ação disponível");
        assertThat(linha.select("button, a, form")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"1, Solicitada", "4, Concluída", "5, Rejeitada", "6, Cancelada", "7, Não compareceu"})
    void transicaoInvalidaNuncaEOferecida(String id, String estado) throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/todos");

        var linha = doc.selectFirst("tr[data-testid=retirada-devolucao-linha-" + id + "]");
        assertThat(linha.selectFirst("[data-testid=retirada-devolucao-estado-" + id + "]").text()).isEqualTo(estado);
        assertThat(linha.select("button, a, form")).as("ações do estado %s", estado).isEmpty();
        assertThat(linha.selectFirst(".tabela__nota").text()).isEqualTo("Sem ação disponível");
        assertThat(linha.select(".etiqueta")).as("etiqueta no estado %s", estado).isEmpty();
        assertThat(linha.select("[data-testid^=retirada-devolucao-nao-compareceu-], [data-testid^=retirada-devolucao-vencida-]"))
                .as("não compareceu no estado %s", estado).isEmpty();
    }

    @Test
    void estadoDesconhecidoOuNuloNaoOfereceAcaoENemEtiquetaEOBadgeFicaNeutro() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/estado-desconhecido");

        var desconhecido = doc.selectFirst("tr[data-testid=retirada-devolucao-linha-1]");
        assertThat(desconhecido.selectFirst("[data-testid=retirada-devolucao-sem-acao-1]").text()).isEqualTo("Sem ação disponível");
        var badge = desconhecido.selectFirst("[data-testid=retirada-devolucao-estado-1]");
        assertThat(badge.text()).isEqualTo("Em análise");
        assertThat(badge.classNames()).containsExactly("estado");
        var nulo = doc.selectFirst("tr[data-testid=retirada-devolucao-linha-2]");
        assertThat(nulo.selectFirst("[data-testid=retirada-devolucao-sem-acao-2]").text()).isEqualTo("Sem ação disponível");
        assertThat(nulo.select(".estado")).isEmpty();
        assertThat(doc.select("table button, [data-testid^=retirada-devolucao-nao-compareceu-], .etiqueta")).isEmpty();
    }

    @Test
    void decisaoDeAcaoComparaONomeExatoEDiferenciaMaiusculas() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/estado-desconhecido");

        assertThat(doc.select("[data-testid=retirada-devolucao-retirar-3], [data-testid=retirada-devolucao-devolver-4]")).isEmpty();
        assertThat(doc.selectFirst("[data-testid=retirada-devolucao-sem-acao-3]").text()).isEqualTo("Sem ação disponível");
        assertThat(doc.selectFirst("[data-testid=retirada-devolucao-sem-acao-4]").text()).isEqualTo("Sem ação disponível");
    }

    @Test
    void soOsFormulariosDeAcaoEnviamUmaVezSo() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/confirmando-nc");

        var forms = doc.select("main form[method=post]");
        assertThat(forms).hasSize(5);
        for (var form : forms) {
            assertThat(form.hasAttr("data-envio-unico")).as("form %s", form.attr("action")).isTrue();
            assertThat(form.select("button[type=submit]")).as("botão de envio de %s", form.attr("action")).hasSize(1);
        }
        assertThat(doc.selectFirst("form[data-testid=retirada-devolucao-filtros]").hasAttr("data-envio-unico")).isFalse();
    }

    @Test
    void dialogoReaproveitaAsClassesComunsEmVezDeRegrasProprias() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/confirmando-nc");

        assertThat(doc.selectFirst("#retirada-devolucao-nc-titulo").hasClass("cartao__titulo")).isTrue();
        assertThat(doc.selectFirst("#retirada-devolucao-nc-texto").hasClass("pagina__descricao")).isTrue();
        assertThat(doc.selectFirst("dialog form > div:has(a[data-fechar])").hasClass("formulario__acoes")).isTrue();
        assertThat(doc.select("[class*=dialogo__]")).isEmpty();
    }

    // Horário já passou (RN-08)

    @Test
    void aprovadaComHorarioPassadoMostraEtiquetaERetiradaEMarcarComoNaoCompareceu() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/lista");

        var linha = doc.selectFirst("tr[data-testid=retirada-devolucao-linha-4]");
        var etiqueta = linha.selectFirst("td[data-rotulo=Horário] .etiqueta[data-testid=retirada-devolucao-vencida-4]");
        assertThat(etiqueta.text()).isEqualTo("Horário já passou");
        assertThat(linha.select("button[data-testid=retirada-devolucao-retirar-4]")).hasSize(1);
        var nc = linha.selectFirst("a[data-testid=retirada-devolucao-nao-compareceu-4]");
        assertThat(nc.text()).isEqualTo("Marcar como não compareceu");
        assertThat(nc.attr("aria-label")).isEqualTo("Marcar como não compareceu a reserva de Kit de microscópio M-02 por Beatriz Nogueira");
        assertThat(nc.attr("href")).isEqualTo("/retirada-devolucao?estado=ativas&naoCompareceu=4");
    }

    @Test
    void aprovadaNoPrazoNaoMostraEtiquetaNemMarcarComoNaoCompareceu() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/lista");

        assertThat(doc.select("[data-testid=retirada-devolucao-vencida-2], [data-testid=retirada-devolucao-nao-compareceu-2]")).isEmpty();
        assertThat(doc.select("[data-testid^=retirada-devolucao-nao-compareceu-]")).hasSize(1);
        assertThat(doc.select(".etiqueta")).hasSize(1);
    }

    @Test
    void emUsoNuncaEMarcadaComoNaoCompareceuMesmoComHorarioPassado() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/passou-em-uso");

        assertThat(doc.select(".etiqueta, [data-testid^=retirada-devolucao-nao-compareceu-]")).isEmpty();
        assertThat(doc.select("[data-testid=retirada-devolucao-devolver-8]")).hasSize(1);
    }

    @Test
    void semNaoCompareceuNoModeloNaoMostraDialogoNemCarregaOJavaScript() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/lista");

        assertThat(doc.select("dialog, [role=alertdialog], script[src*=dialogo]")).isEmpty();
    }

    @Test
    void naoCompareceuAbreDialogoDeConfirmacaoComPapelAlertdialog() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/confirmando-nc");

        var dialogo = doc.selectFirst("dialog.dialogo[data-testid=retirada-devolucao-nc-dialogo]");
        assertThat(dialogo.hasAttr("open")).isTrue();
        assertThat(dialogo.attr("role")).isEqualTo("alertdialog");
        assertThat(doc.getElementById(dialogo.attr("aria-labelledby")).text()).isEqualTo("Marcar como não compareceu?");
        var descricao = doc.getElementById(dialogo.attr("aria-describedby"));
        assertThat(descricao.text()).isEqualTo(
                "Kit de microscópio M-02, de Beatriz Nogueira, 05/11/2026, das 09:00 às 11:00. A reserva passa para Não compareceu e a mudança fica registrada.");
        assertThat(descricao.selectFirst(".estado--nao-compareceu").text()).isEqualTo("Não compareceu");
        assertThat(doc.select("script[src='/js/dialogo.js'][defer]")).hasSize(1);
    }

    @Test
    void dialogoGuardaOGatilhoQueOAbriuParaDevolverOFocoAoFechar() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/confirmando-nc");

        var dialogo = doc.selectFirst("dialog[data-testid=retirada-devolucao-nc-dialogo]");
        assertThat(dialogo.attr("data-origem")).isEqualTo("retirada-devolucao-nao-compareceu-4");
        assertThat(doc.select("[data-testid=" + dialogo.attr("data-origem") + "]")).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource({
            "Em uso, true",
            "Concluída, true",
            "Solicitada, true",
            "Não compareceu, true",
            "Aprovada, false"
    })
    void naoCompareceuSoAbreOdialogoParaAprovadaComHorarioPassado(String estado, boolean passou) throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/confirmando-nc-custom?estado=" + estado + "&passou=" + passou);

        assertThat(doc.select("dialog, [role=alertdialog]")).as("diálogo com estado %s e horarioPassou %s", estado, passou).isEmpty();
        assertThat(doc.select("script[src*=dialogo]")).isEmpty();
    }

    @Test
    void naoCompareceuAprovadaComHorarioPassadoAbreODialogo() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/confirmando-nc-custom?estado=Aprovada&passou=true");

        assertThat(doc.select("dialog[data-testid=retirada-devolucao-nc-dialogo]")).hasSize(1);
        assertThat(doc.select("script[src='/js/dialogo.js']")).hasSize(1);
    }

    @Test
    void confirmarEnviaPostParaNaoCompareceuEVoltarFechaODialogoMantendoOFiltro() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/confirmando-nc");

        var form = doc.selectFirst("dialog[data-testid=retirada-devolucao-nc-dialogo] form");
        assertThat(form.attr("method")).isEqualToIgnoringCase("post");
        assertThat(form.attr("action")).isEqualTo("/retirada-devolucao/4/nao-compareceu");
        var confirmar = form.selectFirst("button[type=submit][data-testid=retirada-devolucao-nc-confirmar]");
        assertThat(confirmar.text()).isEqualTo("Sim, marcar como não compareceu");
        assertThat(confirmar.hasClass("botao--perigo")).isTrue();
        var voltar = form.selectFirst("a[data-testid=retirada-devolucao-nc-voltar]");
        assertThat(voltar.text()).isEqualTo("Voltar");
        assertThat(voltar.attr("href")).isEqualTo("/retirada-devolucao?estado=aprovada");
        assertThat(voltar.hasAttr("autofocus")).as("o foco começa no gesto seguro").isTrue();
        assertThat(confirmar.hasAttr("autofocus")).isFalse();
    }

    // Confirmação com o novo estado

    @Test
    void retiradaRegistradaMostraAvisoComRoleStatusENovoEstadoEmUso() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/retirada-registrada");

        var aviso = doc.selectFirst("div[data-testid=retirada-devolucao-sucesso]");
        assertThat(aviso.attr("role")).isEqualTo("status");
        assertThat(aviso.hasClass("alerta--sucesso")).isTrue();
        assertThat(aviso.selectFirst(".alerta__titulo").text()).isEqualTo("Retirada registrada");
        assertThat(aviso.selectFirst(".alerta__texto").text())
                .isEqualTo("Sala 203, de Rafael Costa, agora está Em uso. A mudança ficou registrada.");
        assertThat(aviso.selectFirst(".estado--em-uso").text()).isEqualTo("Em uso");
    }

    @Test
    void devolucaoRegistradaMostraAvisoComNovoEstadoConcluida() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/devolucao-registrada");

        var aviso = doc.selectFirst("div[data-testid=retirada-devolucao-sucesso]");
        assertThat(aviso.attr("role")).isEqualTo("status");
        assertThat(aviso.selectFirst(".alerta__titulo").text()).isEqualTo("Devolução registrada");
        assertThat(aviso.selectFirst(".estado--concluida").text()).isEqualTo("Concluída");
    }

    @Test
    void semConfirmacaoNaoHaAvisoDeStatus() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/lista");

        assertThat(doc.select("[role=status], [data-testid=retirada-devolucao-sucesso]")).isEmpty();
    }

    // Estado vazio

    @Test
    void semReservasMostraMensagemVaziaMasMantemOFiltro() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/vazia");

        var vazio = doc.selectFirst("section[data-testid=retirada-devolucao-vazio]");
        assertThat(vazio.attr("aria-labelledby")).isEqualTo("retirada-devolucao-vazio-titulo");
        assertThat(vazio.selectFirst("h2#retirada-devolucao-vazio-titulo").text()).isEqualTo("Nenhuma reserva aguardando retirada ou devolução");
        assertThat(vazio.selectFirst("p").text()).isEqualTo("Reservas aprovadas e em uso aparecem aqui.");
        assertThat(doc.select("[data-testid=retirada-devolucao-tabela]")).isEmpty();
        assertThat(doc.select("form[data-testid=retirada-devolucao-filtros]")).hasSize(1);
    }

    // Segurança

    @Test
    void textoComHtmlEhEscapadoNaTabelaNoDialogoENoAviso() throws Exception {
        Document doc = pagina("/teste/retirada-devolucao/perigoso");

        assertThat(doc.select("script:containsData(alert), img[onerror], b")).isEmpty();
        assertThat(doc.selectFirst("tr[data-testid=retirada-devolucao-linha-1] th").text()).isEqualTo("<script>alert(1)</script>");
        assertThat(doc.selectFirst("td[data-rotulo=Solicitante]").text()).isEqualTo("<img src=x onerror=alert(2)>");
        assertThat(doc.selectFirst("button[data-testid=retirada-devolucao-retirar-1]").attr("aria-label"))
                .isEqualTo("Registrar retirada de <script>alert(1)</script> por <img src=x onerror=alert(2)>");
        assertThat(doc.selectFirst("a[data-testid=retirada-devolucao-nao-compareceu-1]").attr("aria-label"))
                .isEqualTo("Marcar como não compareceu a reserva de <script>alert(1)</script> por <img src=x onerror=alert(2)>");
        assertThat(doc.selectFirst("#retirada-devolucao-nc-texto").text()).startsWith("<script>alert(1)</script>, de <img src=x onerror=alert(2)>,");
        assertThat(doc.selectFirst("[data-testid=retirada-devolucao-sucesso] .alerta__titulo").text()).isEqualTo("<b>Retirada registrada</b>");
        assertThat(doc.selectFirst("[data-testid=retirada-devolucao-sucesso] .alerta__texto").text()).startsWith("<script>alert(3)</script>, agora está");
    }
}
