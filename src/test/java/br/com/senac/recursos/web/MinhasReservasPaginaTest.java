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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tela 7 do inventário (docs/frontend/telas.md): minhas reservas, prefixo {@code minhas-}, RF-04, RN-05 e RN-07
 * (spec 02 do Notion, "Minhas reservas e detalhe"). O back-end preenche {@code reservas} (id, recurso, local,
 * data e horario já formatados, estado como texto e {@code iniciada}), já filtradas e com as próximas primeiro;
 * o controller preenche {@code filtro}. Alterar e Cancelar só aparecem para Solicitada ou Aprovada que ainda
 * não começou.
 */
@WebMvcTest({ReservaController.class, MinhasReservasPaginaTest.PaginasDeTeste.class})
@Import(MinhasReservasPaginaTest.PaginasDeTeste.class)
class MinhasReservasPaginaTest {

    @Controller
    static class PaginasDeTeste {

        private static Map<String, Object> reserva(String id, String recurso, String estado, boolean iniciada) {
            return Map.of("id", id, "recurso", recurso, "local", "Bloco B, 2º andar", "data", "12/11/2026",
                    "horario", "14:00 às 15:30", "estado", estado, "iniciada", iniciada);
        }

        @GetMapping("/teste/minhas/lista")
        String lista(Model model) {
            model.addAttribute("filtro", "todos");
            model.addAttribute("reservas", List.of(
                    reserva("1", "Sala 203", "Solicitada", false),
                    reserva("2", "Projetor portátil P-07", "Aprovada", false),
                    reserva("3", "Laboratório de Química", "Em uso", true),
                    reserva("4", "Sala 101", "Concluída", true),
                    reserva("5", "Sala 101", "Rejeitada", false),
                    reserva("6", "Sala 101", "Cancelada", false),
                    reserva("7", "Sala 101", "Não compareceu", true),
                    reserva("8", "Sala 204", "Aprovada", true)));
            return "reservas-minhas";
        }

        @GetMapping("/teste/minhas/estado-fora-do-contrato")
        String estadoForaDoContrato(Model model) {
            model.addAttribute("filtro", "todos");
            Map<String, Object> semIniciada = new HashMap<>(reserva("3", "Sala 203", "Aprovada", false));
            semIniciada.put("iniciada", null);
            model.addAttribute("reservas", List.of(
                    reserva("1", "Sala 203", "aprovada", false),
                    reserva("2", "Sala 203", "Em análise", false),
                    semIniciada));
            return "reservas-minhas";
        }

        @GetMapping("/teste/minhas/vazia-filtrada")
        String vaziaFiltrada(Model model) {
            model.addAttribute("filtro", "cancelada");
            model.addAttribute("reservas", List.of());
            return "reservas-minhas";
        }

        @GetMapping("/teste/minhas/perigoso")
        String perigoso(Model model) {
            model.addAttribute("filtro", "todos");
            model.addAttribute("reservas", List.of(reserva("1", "<script>alert(1)</script>", "Aprovada", false)));
            return "reservas-minhas";
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

    // Rota real

    @Test
    void rotaGetMinhasReservasUsaOLayoutBaseComTituloEUmUnicoH1() throws Exception {
        Document doc = pagina("/reservas/minhas");

        assertThat(doc.title()).isEqualTo("Minhas reservas · Organização de Recursos");
        assertThat(doc.select("title")).as("um só title, o do layout").hasSize(1);
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.selectFirst("main#conteudo h1[data-testid=minhas-titulo]").text()).isEqualTo("Minhas reservas");
        assertThat(doc.selectFirst("main .pagina__descricao").text()).isEqualTo("As reservas mais próximas aparecem primeiro.");
    }

    @ParameterizedTest
    @CsvSource({
            "solicitada, solicitada",
            "aprovada, aprovada",
            "em-uso, em-uso",
            "concluida, concluida",
            "rejeitada, rejeitada",
            "cancelada, cancelada",
            "nao-compareceu, nao-compareceu",
            "todos, todos",
            "valor-invalido, todos",
            "'', todos"
    })
    void controllerDevolveOFiltroEscolhidoOuTodos(String parametro, String esperado) throws Exception {
        Document doc = pagina("/reservas/minhas?estado=" + parametro);

        assertThat(doc.select("#minhas-estado option[selected]").eachAttr("value")).containsExactly(esperado);
    }

    @Test
    void semParametroOFiltroETodosOsEstados() throws Exception {
        Document doc = pagina("/reservas/minhas");

        assertThat(doc.select("#minhas-estado option[selected]").eachText()).containsExactly("Todos os estados");
    }

    // Filtro por estado

    @Test
    void filtroEnviaPorGetParaAPropriaRotaComRotuloLigadoPeloFor() throws Exception {
        Document doc = pagina("/teste/minhas/lista");

        var form = doc.selectFirst("form[data-testid=minhas-filtros]");
        assertThat(form.attr("method")).isEqualToIgnoringCase("get");
        assertThat(form.attr("action")).isEqualTo("/reservas/minhas");
        assertThat(form.hasAttr("data-envio-unico")).as("filtro é GET, pode repetir").isFalse();
        var seletor = form.selectFirst("select[data-testid=minhas-estado]");
        assertThat(seletor.attr("name")).isEqualTo("estado");
        assertThat(form.selectFirst("label[for=" + seletor.id() + "]").text()).isEqualTo("Estado");
        assertThat(seletor.select("option").eachText()).containsExactly("Todos os estados", "Solicitada", "Aprovada",
                "Em uso", "Concluída", "Rejeitada", "Cancelada", "Não compareceu");
        assertThat(seletor.select("option").eachAttr("value")).containsExactly("todos", "solicitada", "aprovada",
                "em-uso", "concluida", "rejeitada", "cancelada", "nao-compareceu");
        assertThat(form.selectFirst("button[type=submit][data-testid=minhas-filtrar]").text()).isEqualTo("Aplicar");
    }

    // Lista

    @Test
    void listaTemTabelaComLegendaCabecalhosECelulasRotuladasParaOsCardsDoMobile() throws Exception {
        Document doc = pagina("/teste/minhas/lista");

        var tabela = doc.selectFirst("table.tabela[data-testid=minhas-tabela]");
        assertThat(tabela.selectFirst("caption").text()).isNotBlank();
        assertThat(tabela.select("thead th[scope=col]").eachText())
                .containsExactly("Recurso", "Local", "Data", "Horário", "Estado", "Ações");
        var linha = tabela.selectFirst("tr[data-testid=minhas-linha-2]");
        assertThat(linha.selectFirst("th[scope=row]").text()).isEqualTo("Projetor portátil P-07");
        assertThat(linha.select("td[data-rotulo]").eachAttr("data-rotulo")).containsExactly("Local", "Data", "Horário", "Estado");
        assertThat(linha.select("td[data-rotulo]").eachText())
                .containsExactly("Bloco B, 2º andar", "12/11/2026", "14:00 às 15:30", "Aprovada");
    }

    @Test
    void listaMantemAOrdemQueOBackEndMandou() throws Exception {
        Document doc = pagina("/teste/minhas/lista");

        assertThat(doc.select("tbody tr").eachAttr("data-testid")).containsExactly("minhas-linha-1", "minhas-linha-2",
                "minhas-linha-3", "minhas-linha-4", "minhas-linha-5", "minhas-linha-6", "minhas-linha-7", "minhas-linha-8");
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
        Document doc = pagina("/teste/minhas/lista");

        var badge = doc.selectFirst("[data-testid=minhas-estado-" + id + "]");
        assertThat(badge.text()).isEqualTo(nome);
        assertThat(badge.hasClass("estado")).isTrue();
        assertThat(badge.hasClass("estado--" + sufixo)).isTrue();
    }

    @Test
    void todaLinhaLevaAoDetalheComNomeAcessivel() throws Exception {
        Document doc = pagina("/teste/minhas/lista");

        assertThat(doc.select("a[data-testid^=minhas-detalhe-]")).hasSize(8);
        var detalhe = doc.selectFirst("a[data-testid=minhas-detalhe-3]");
        assertThat(detalhe.text()).isEqualTo("Ver detalhes");
        assertThat(detalhe.attr("href")).isEqualTo("/reservas/3");
        assertThat(detalhe.attr("aria-label")).isEqualTo("Ver detalhes da reserva de Laboratório de Química em 12/11/2026");
    }

    // Alterar e Cancelar só quando o estado permite (RN-05, RN-07)

    @ParameterizedTest
    @CsvSource({"1, Sala 203", "2, Projetor portátil P-07"})
    void solicitadaOuAprovadaQueNaoComecouOfereceAlterarECancelar(String id, String recurso) throws Exception {
        Document doc = pagina("/teste/minhas/lista");

        var alterar = doc.selectFirst("a[data-testid=minhas-alterar-" + id + "]");
        assertThat(alterar.text()).isEqualTo("Alterar");
        assertThat(alterar.attr("href")).isEqualTo("/reservas/" + id + "?acao=alterar");
        assertThat(alterar.attr("aria-label")).isEqualTo("Alterar a reserva de " + recurso + " em 12/11/2026");
        var cancelar = doc.selectFirst("a[data-testid=minhas-cancelar-" + id + "]");
        assertThat(cancelar.text()).isEqualTo("Cancelar");
        assertThat(cancelar.attr("href")).as("cancelar abre a confirmação, não cancela direto").isEqualTo("/reservas/" + id + "?acao=cancelar");
        assertThat(cancelar.hasClass("botao--perigo")).isTrue();
        assertThat(cancelar.attr("aria-label")).isEqualTo("Cancelar a reserva de " + recurso + " em 12/11/2026");
    }

    @ParameterizedTest
    @CsvSource({"3, Em uso", "4, Concluída", "5, Rejeitada", "6, Cancelada", "7, Não compareceu", "8, Aprovada"})
    void reservaIniciadaOuEmEstadoFinalNaoOfereceAlterarNemCancelar(String id, String estado) throws Exception {
        Document doc = pagina("/teste/minhas/lista");

        var linha = doc.selectFirst("tr[data-testid=minhas-linha-" + id + "]");
        assertThat(linha.selectFirst("[data-testid=minhas-estado-" + id + "]").text()).isEqualTo(estado);
        assertThat(linha.select("[data-testid^=minhas-alterar-], [data-testid^=minhas-cancelar-]"))
                .as("ações na reserva %s (%s)", id, estado).isEmpty();
        assertThat(linha.select("a").eachText()).containsExactly("Ver detalhes");
    }

    @Test
    void estadoForaDoContratoOuIniciadaAusenteNaoOfereceAlterarNemCancelar() throws Exception {
        Document doc = pagina("/teste/minhas/estado-fora-do-contrato");

        assertThat(doc.select("[data-testid^=minhas-alterar-], [data-testid^=minhas-cancelar-]")).isEmpty();
        assertThat(doc.selectFirst("[data-testid=minhas-estado-2]").classNames()).containsExactly("estado");
        assertThat(doc.select("a[data-testid^=minhas-detalhe-]")).hasSize(3);
    }

    @Test
    void listaNaoTemFormularioQueMudeEstado() throws Exception {
        Document doc = pagina("/teste/minhas/lista");

        assertThat(doc.select("main form[method=post], main button:not([data-testid=minhas-filtrar])")).isEmpty();
    }

    // Estados vazios

    @Test
    void semReservasMostraConviteParaBuscarRecursos() throws Exception {
        Document doc = pagina("/reservas/minhas");

        var vazio = doc.selectFirst("section[data-testid=minhas-vazio]");
        assertThat(vazio.attr("aria-labelledby")).isEqualTo("minhas-vazio-titulo");
        assertThat(vazio.selectFirst("h2#minhas-vazio-titulo").text()).isEqualTo("Você ainda não tem reservas");
        var buscar = vazio.selectFirst("a[data-testid=minhas-vazio-buscar]");
        assertThat(buscar.text()).isEqualTo("Buscar recursos");
        assertThat(buscar.attr("href")).isEqualTo("/recursos/disponiveis");
        assertThat(doc.select("[data-testid=minhas-tabela], [data-testid=minhas-vazio-filtro]")).isEmpty();
        assertThat(doc.select("form[data-testid=minhas-filtros]")).hasSize(1);
    }

    @Test
    void filtroSemResultadoOfereceVerTodasEmVezDoConvite() throws Exception {
        Document doc = pagina("/teste/minhas/vazia-filtrada");

        var vazio = doc.selectFirst("section[data-testid=minhas-vazio-filtro]");
        assertThat(vazio.attr("aria-labelledby")).isEqualTo("minhas-vazio-filtro-titulo");
        assertThat(vazio.selectFirst("h2").text()).isEqualTo("Nenhuma reserva com esse estado");
        var todas = vazio.selectFirst("a[data-testid=minhas-vazio-todas]");
        assertThat(todas.text()).isEqualTo("Ver todas as reservas");
        assertThat(todas.attr("href")).isEqualTo("/reservas/minhas");
        assertThat(doc.select("[data-testid=minhas-vazio], [data-testid=minhas-tabela]")).isEmpty();
        assertThat(doc.select("#minhas-estado option[selected]").eachAttr("value")).containsExactly("cancelada");
    }

    // Segurança

    @Test
    void textoComHtmlEhEscapadoNaTabelaENosNomesAcessiveis() throws Exception {
        Document doc = pagina("/teste/minhas/perigoso");

        assertThat(doc.select("script:containsData(alert)")).isEmpty();
        assertThat(doc.selectFirst("tr[data-testid=minhas-linha-1] th").text()).isEqualTo("<script>alert(1)</script>");
        assertThat(doc.selectFirst("a[data-testid=minhas-cancelar-1]").attr("aria-label"))
                .isEqualTo("Cancelar a reserva de <script>alert(1)</script> em 12/11/2026");
    }
}
