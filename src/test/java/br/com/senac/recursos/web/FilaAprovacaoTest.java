package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * Tela 9 do inventário (docs/frontend/telas.md): fila de aprovação, prefixo {@code aprovacao-}, RF-07 e RN-06.
 * O back-end preenche {@code pedidos} (id, solicitante, recurso, data e horario já formatados, estado como
 * texto e {@code proprio}); {@code rejeitando} (o pedido que está sendo rejeitado), {@code erros} (mapa, chave
 * {@code motivo}), {@code confirmacao} (titulo, reserva, estado) e {@code erroAprovacao} (403 da API).
 * A tela só mostra o que recebe; quem aprova e rejeita é o back-end, por POST.
 */
@WebMvcTest
@Import(FilaAprovacaoTest.PaginasDeTeste.class)
class FilaAprovacaoTest {

    @Controller
    static class PaginasDeTeste {

        private static Map<String, Object> pedido(String id, String solicitante, String recurso, String data, String horario, boolean proprio) {
            return Map.of("id", id, "solicitante", solicitante, "recurso", recurso, "data", data,
                    "horario", horario, "estado", "Solicitada", "proprio", proprio);
        }

        private static List<Map<String, Object>> fila() {
            return List.of(
                    pedido("1", "Laura Mendes", "Laboratório de Química", "09/11/2026", "08:00 às 10:00", false),
                    pedido("2", "Rafael Costa", "Projetor portátil P-07", "10/11/2026", "13:30 às 15:00", false),
                    pedido("3", "Carlos Souza", "Sala de reuniões 2", "11/11/2026", "14:00 às 16:00", true),
                    pedido("4", "Ana Lima", "Laboratório de Química", "12/11/2026", "14:00 às 15:30", false));
        }

        @GetMapping("/teste/aprovacoes/fila")
        String fila(Model model) {
            model.addAttribute("pedidos", fila());
            return "aprovacoes";
        }

        @GetMapping("/teste/aprovacoes/vazia")
        String vazia(Model model) {
            model.addAttribute("pedidos", List.of());
            return "aprovacoes";
        }

        @GetMapping("/teste/aprovacoes/rejeitando")
        String rejeitando(Model model) {
            model.addAttribute("pedidos", fila());
            model.addAttribute("rejeitando", fila().get(0));
            return "aprovacoes";
        }

        @GetMapping("/teste/aprovacoes/sem-motivo")
        String semMotivo(Model model) {
            model.addAttribute("pedidos", fila());
            model.addAttribute("rejeitando", fila().get(0));
            model.addAttribute("erros", Map.of("motivo", "Informe o motivo da rejeição."));
            return "aprovacoes";
        }

        @GetMapping("/teste/aprovacoes/aprovada")
        String aprovada(Model model) {
            model.addAttribute("pedidos", fila().subList(1, 4));
            model.addAttribute("confirmacao", Map.of("titulo", "Reserva aprovada",
                    "reserva", "Laboratório de Química, de Laura Mendes", "estado", "Aprovada"));
            return "aprovacoes";
        }

        @GetMapping("/teste/aprovacoes/rejeitada")
        String rejeitada(Model model) {
            model.addAttribute("pedidos", fila().subList(1, 4));
            model.addAttribute("confirmacao", Map.of("titulo", "Reserva rejeitada",
                    "reserva", "Laboratório de Química, de Laura Mendes", "estado", "Rejeitada"));
            return "aprovacoes";
        }

        @GetMapping("/teste/aprovacoes/erro-403")
        String erro403(Model model) {
            model.addAttribute("pedidos", fila());
            model.addAttribute("erroAprovacao", true);
            return "aprovacoes";
        }

        @GetMapping("/teste/aprovacoes/perigoso")
        String perigoso(Model model) {
            Map<String, Object> pedido = pedido("1", "<img src=x onerror=alert(1)>", "<script>alert(2)</script>",
                    "09/11/2026", "08:00 às 10:00", false);
            model.addAttribute("pedidos", List.of(pedido));
            model.addAttribute("rejeitando", pedido);
            model.addAttribute("confirmacao", Map.of("titulo", "<b>Reserva aprovada</b>",
                    "reserva", "<script>alert(3)</script>", "estado", "Aprovada"));
            return "aprovacoes";
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

    // Rota real: o controller só devolve a view; quem preenche o modelo é o back-end

    @Test
    void rotaGetAprovacoesUsaOLayoutBaseComTituloEUmUnicoH1() throws Exception {
        Document doc = pagina("/aprovacoes");

        assertThat(doc.title()).isEqualTo("Fila de aprovação · Organização de Recursos");
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.selectFirst("main#conteudo h1[data-testid=aprovacao-titulo]").text()).isEqualTo("Fila de aprovação");
    }

    @Test
    void semPedidosNoModeloMostraAFilaVazia() throws Exception {
        Document doc = pagina("/aprovacoes");

        assertThat(doc.selectFirst("[data-testid=aprovacao-vazio] h2").text()).isEqualTo("Nenhum pedido aguardando aprovação");
        assertThat(doc.select("[data-testid=aprovacao-tabela]")).isEmpty();
    }

    // Fila

    @Test
    void filaTemTabelaComLegendaCabecalhosECelulasRotuladasParaOsCardsDoMobile() throws Exception {
        Document doc = pagina("/teste/aprovacoes/fila");

        var tabela = doc.selectFirst("table.tabela[data-testid=aprovacao-tabela]");
        assertThat(tabela.selectFirst("caption").text()).isEqualTo("Pedidos aguardando aprovação, os mais próximos do horário primeiro");
        assertThat(tabela.select("thead th[scope=col]").eachText())
                .containsExactly("Solicitante", "Recurso", "Data", "Horário", "Estado", "Ações");
        assertThat(tabela.select("tbody tr").eachAttr("data-testid"))
                .containsExactly("aprovacao-linha-1", "aprovacao-linha-2", "aprovacao-linha-3", "aprovacao-linha-4");
        var linha = tabela.selectFirst("tr[data-testid=aprovacao-linha-1]");
        assertThat(linha.selectFirst("th[scope=row]").text()).isEqualTo("Laura Mendes");
        assertThat(linha.select("td[data-rotulo]").eachAttr("data-rotulo")).containsExactly("Recurso", "Data", "Horário", "Estado");
        assertThat(linha.select("td[data-rotulo]").eachText()).containsExactly("Laboratório de Química", "09/11/2026", "08:00 às 10:00", "Solicitada");
    }

    @Test
    void estadoDoPedidoApareceEscritoNoBadge() throws Exception {
        Document doc = pagina("/teste/aprovacoes/fila");

        var estado = doc.selectFirst("[data-testid=aprovacao-estado-1]");
        assertThat(estado.text()).isEqualTo("Solicitada");
        assertThat(estado.hasClass("estado")).isTrue();
        assertThat(estado.hasClass("estado--solicitada")).isTrue();
    }

    @Test
    void aprovarEhUmFormularioPostComNomeAcessivelQueDizAAcaoORecursoEOSolicitante() throws Exception {
        Document doc = pagina("/teste/aprovacoes/fila");

        var botao = doc.selectFirst("button[data-testid=aprovacao-aprovar-1]");
        assertThat(botao.attr("type")).isEqualTo("submit");
        assertThat(botao.text()).isEqualTo("Aprovar");
        assertThat(botao.attr("aria-label")).isEqualTo("Aprovar reserva de Laura Mendes para Laboratório de Química");
        Element form = botao.closest("form");
        assertThat(form.attr("method")).isEqualToIgnoringCase("post");
        assertThat(form.attr("action")).isEqualTo("/aprovacoes/1/aprovar");
    }

    @Test
    void rejeitarEhUmLinkParaODialogoDirigidoPeloServidor() throws Exception {
        Document doc = pagina("/teste/aprovacoes/fila");

        var link = doc.selectFirst("a[data-testid=aprovacao-rejeitar-2]");
        assertThat(link.text()).isEqualTo("Rejeitar");
        assertThat(link.attr("href")).isEqualTo("/aprovacoes?rejeitar=2");
        assertThat(link.attr("aria-label")).isEqualTo("Rejeitar reserva de Rafael Costa para Projetor portátil P-07");
    }

    @Test
    void pedidoDoProprioResponsavelNaoTemBotoesENaoPodeSerDecidido() throws Exception {
        Document doc = pagina("/teste/aprovacoes/fila");

        var linha = doc.selectFirst("tr[data-testid=aprovacao-linha-3]");
        assertThat(linha.select("button, a, form")).isEmpty();
        assertThat(linha.selectFirst("[data-testid=aprovacao-propria-3]").text()).isEqualTo("Você não pode decidir a sua própria reserva.");
        assertThat(linha.selectFirst("th[scope=row]").text()).isEqualTo("Carlos Souza (você)");
        assertThat(doc.select("[data-testid=aprovacao-aprovar-3], [data-testid=aprovacao-rejeitar-3]")).isEmpty();
    }

    @Test
    void soOsPedidosDeOutrasPessoasTemBotoesEAvisoSoNoProprio() throws Exception {
        Document doc = pagina("/teste/aprovacoes/fila");

        assertThat(doc.select("button[data-testid^=aprovacao-aprovar-]")).hasSize(3);
        assertThat(doc.select("a[data-testid^=aprovacao-rejeitar-]")).hasSize(3);
        assertThat(doc.select("[data-testid^=aprovacao-propria-]")).hasSize(1);
        assertThat(doc.select("th[scope=row] .tabela__detalhe").eachText()).containsExactly("(você)");
    }

    @Test
    void semDecisaoRecenteNaoMostraAvisoNemDialogo() throws Exception {
        Document doc = pagina("/teste/aprovacoes/fila");

        assertThat(doc.select("[role=status], [role=alert], dialog, [data-testid=aprovacao-sucesso], [data-testid=aprovacao-erro-403]")).isEmpty();
        assertThat(doc.select("script[src*=dialogo]")).isEmpty();
    }

    // Fila vazia

    @Test
    void filaVaziaExplicaOQueAcontecerEEscondeATabela() throws Exception {
        Document doc = pagina("/teste/aprovacoes/vazia");

        var vazio = doc.selectFirst("section[data-testid=aprovacao-vazio]");
        assertThat(vazio.attr("aria-labelledby")).isEqualTo("aprovacao-vazio-titulo");
        assertThat(vazio.selectFirst("h2#aprovacao-vazio-titulo").text()).isEqualTo("Nenhum pedido aguardando aprovação");
        assertThat(vazio.selectFirst("p").text()).isEqualTo("Quando alguém pedir um recurso restrito, o pedido aparece aqui.");
        assertThat(doc.select("[data-testid=aprovacao-tabela]")).isEmpty();
        assertThat(doc.selectFirst("h1[data-testid=aprovacao-titulo]")).isNotNull();
    }

    // Rejeitar (diálogo dirigido pelo servidor)

    @Test
    void rejeitandoAbreODialogoComTituloEDescricaoLigadosPorAria() throws Exception {
        Document doc = pagina("/teste/aprovacoes/rejeitando");

        var dialogo = doc.selectFirst("dialog.dialogo[data-testid=aprovacao-rejeitar-dialogo]");
        assertThat(dialogo.hasAttr("open")).as("aberto no HTML, para funcionar sem JavaScript").isTrue();
        assertThat(dialogo.hasAttr("role")).as("dialog já é o papel implícito de <dialog>").isFalse();
        assertThat(doc.getElementById(dialogo.attr("aria-labelledby")).text()).isEqualTo("Rejeitar o pedido de Laura Mendes?");
        var descricao = doc.getElementById(dialogo.attr("aria-describedby"));
        assertThat(descricao.text()).isEqualTo("Laboratório de Química, 09/11/2026, das 08:00 às 10:00. A reserva passa para Rejeitada.");
        assertThat(descricao.selectFirst(".estado--rejeitada").text()).isEqualTo("Rejeitada");
    }

    @Test
    void dialogoDeRejeicaoEnviaOMotivoPorPostParaORejeitarDoPedido() throws Exception {
        Document doc = pagina("/teste/aprovacoes/rejeitando");

        var form = doc.selectFirst("dialog[data-testid=aprovacao-rejeitar-dialogo] form");
        assertThat(form.attr("method")).isEqualToIgnoringCase("post");
        assertThat(form.attr("action")).isEqualTo("/aprovacoes/1/rejeitar");
        var confirmar = form.selectFirst("button[type=submit][data-testid=aprovacao-rejeitar-confirmar]");
        assertThat(confirmar.text()).isEqualTo("Rejeitar pedido");
        assertThat(confirmar.hasClass("botao--perigo")).isTrue();
        var voltar = form.selectFirst("a[data-testid=aprovacao-rejeitar-voltar]");
        assertThat(voltar.text()).isEqualTo("Voltar");
        assertThat(voltar.attr("href")).isEqualTo("/aprovacoes");
    }

    @Test
    void motivoEhObrigatorioTemRotuloLigadoPeloForEFocoInicial() throws Exception {
        Document doc = pagina("/teste/aprovacoes/rejeitando");

        var campo = doc.selectFirst("textarea[data-testid=aprovacao-motivo]");
        assertThat(campo.attr("name")).isEqualTo("motivo");
        assertThat(campo.hasAttr("required")).isTrue();
        assertThat(campo.attr("aria-required")).isEqualTo("true");
        assertThat(campo.hasAttr("autofocus")).isTrue();
        assertThat(campo.hasClass("campo__entrada--area")).isTrue();
        var rotulo = doc.selectFirst("label[for=" + campo.id() + "]");
        assertThat(rotulo.text()).isEqualTo("Motivo da rejeição (obrigatório)");
        assertThat(doc.getElementById(campo.attr("aria-describedby")).text()).isEqualTo("O motivo fica registrado na reserva.");
        assertThat(doc.select("[data-testid=aprovacao-motivo-erro], [aria-invalid]")).isEmpty();
    }

    @Test
    void rejeitarSemMotivoMostraOErroJuntoAoCampoComRoleAlert() throws Exception {
        Document doc = pagina("/teste/aprovacoes/sem-motivo");

        var campo = doc.selectFirst("textarea[data-testid=aprovacao-motivo]");
        var erro = doc.selectFirst("p[data-testid=aprovacao-motivo-erro]");
        assertThat(erro.text()).isEqualTo("Informe o motivo da rejeição.");
        assertThat(erro.attr("role")).isEqualTo("alert");
        assertThat(erro.hasClass("campo__erro")).isTrue();
        assertThat(campo.attr("aria-invalid")).isEqualTo("true");
        assertThat(campo.attr("aria-describedby")).isEqualTo(erro.id());
        assertThat(campo.hasClass("campo__entrada--erro")).isTrue();
        assertThat(campo.hasAttr("autofocus")).isTrue();
        assertThat(erro.parent()).isSameAs(campo.parent());
        assertThat(doc.select("dialog[open]")).hasSize(1);
    }

    @Test
    void comDialogoACarregaOJavaScriptQueAbreOModal() throws Exception {
        Document doc = pagina("/teste/aprovacoes/rejeitando");

        assertThat(doc.select("script[src='/js/dialogo.js'][defer]")).hasSize(1);
    }

    // Decisão registrada

    @Test
    void aprovadaMostraAvisoVerdeComONovoEstadoDaReserva() throws Exception {
        Document doc = pagina("/teste/aprovacoes/aprovada");

        var aviso = doc.selectFirst("div[data-testid=aprovacao-sucesso]");
        assertThat(aviso.attr("role")).isEqualTo("status");
        assertThat(aviso.hasClass("alerta--sucesso")).isTrue();
        assertThat(aviso.selectFirst(".alerta__titulo").text()).isEqualTo("Reserva aprovada");
        assertThat(aviso.selectFirst(".alerta__texto").text())
                .isEqualTo("Laboratório de Química, de Laura Mendes, agora está Aprovada. A mudança ficou registrada.");
        assertThat(aviso.selectFirst(".estado--aprovada").text()).isEqualTo("Aprovada");
        assertThat(doc.select("[data-testid=aprovacao-tabela]")).hasSize(1);
    }

    @Test
    void rejeitadaUsaOMesmoAvisoComOBadgeRejeitada() throws Exception {
        Document doc = pagina("/teste/aprovacoes/rejeitada");

        var aviso = doc.selectFirst("div[data-testid=aprovacao-sucesso]");
        assertThat(aviso.selectFirst(".alerta__titulo").text()).isEqualTo("Reserva rejeitada");
        assertThat(aviso.selectFirst(".estado--rejeitada").text()).isEqualTo("Rejeitada");
    }

    @Test
    void avisoDeDecisaoVemAntesDoTituloDaPagina() throws Exception {
        Document doc = pagina("/teste/aprovacoes/aprovada");

        var aviso = doc.selectFirst("[data-testid=aprovacao-sucesso]");
        var titulo = doc.selectFirst("h1[data-testid=aprovacao-titulo]");
        assertThat(doc.select("main *").indexOf(aviso)).isLessThan(doc.select("main *").indexOf(titulo));
    }

    // Erro 403 da API

    @Test
    void erro403MostraAvisoDeAlertaEMantemAFilaEOEstadoInalterado() throws Exception {
        Document doc = pagina("/teste/aprovacoes/erro-403");

        var erro = doc.selectFirst("div[data-testid=aprovacao-erro-403]");
        assertThat(erro.attr("role")).isEqualTo("alert");
        assertThat(erro.attr("tabindex")).isEqualTo("-1");
        assertThat(erro.hasClass("alerta--perigo")).isTrue();
        assertThat(erro.selectFirst(".alerta__titulo").text()).isEqualTo("Você não pode aprovar esta reserva");
        assertThat(erro.selectFirst(".alerta__texto").text())
                .isEqualTo("O estado da reserva não mudou. Se você precisa decidir este pedido, fale com o administrador.");
        assertThat(doc.select("tbody tr")).hasSize(4);
        assertThat(doc.select(".estado--solicitada")).hasSize(4);
        assertThat(doc.select("[data-testid=aprovacao-sucesso]")).isEmpty();
    }

    // Segurança

    @Test
    void textoComHtmlEhEscapadoNaTabelaNoDialogoENoAviso() throws Exception {
        Document doc = pagina("/teste/aprovacoes/perigoso");

        assertThat(doc.select("script:containsData(alert), img[onerror], b")).isEmpty();
        assertThat(doc.selectFirst("tr[data-testid=aprovacao-linha-1] th").text()).isEqualTo("<img src=x onerror=alert(1)>");
        assertThat(doc.selectFirst("[data-testid=aprovacao-linha-1] td[data-rotulo=Recurso]").text()).isEqualTo("<script>alert(2)</script>");
        assertThat(doc.selectFirst("#aprovacao-rejeitar-titulo").text()).isEqualTo("Rejeitar o pedido de <img src=x onerror=alert(1)>?");
        assertThat(doc.selectFirst("[data-testid=aprovacao-sucesso] .alerta__titulo").text()).isEqualTo("<b>Reserva aprovada</b>");
        assertThat(doc.selectFirst("[data-testid=aprovacao-sucesso] .alerta__texto").text()).startsWith("<script>alert(3)</script>, agora está");
    }
}
