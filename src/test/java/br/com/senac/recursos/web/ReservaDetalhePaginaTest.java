package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * Tela 8 do inventário (docs/frontend/telas.md): detalhe, alterar e cancelar, prefixo {@code reserva-}, RF-04,
 * RN-01, RN-05 e RN-07 (spec 02 do Notion). O back-end preenche {@code reserva} (id, recurso, local, data,
 * inicio, termino, estado, {@code iniciada} e motivo), e, depois de um POST, {@code formulario}, {@code erros},
 * {@code conflito}, {@code manutencao} ou {@code confirmacao}. O controller preenche {@code acao} com
 * {@code ?acao=alterar} ou {@code ?acao=cancelar}.
 */
@WebMvcTest({ReservaController.class, ReservaDetalhePaginaTest.PaginasDeTeste.class})
@Import(ReservaDetalhePaginaTest.PaginasDeTeste.class)
class ReservaDetalhePaginaTest {

    @Controller
    static class PaginasDeTeste {

        static Map<String, Object> reserva(String estado, boolean iniciada) {
            Map<String, Object> reserva = new HashMap<>();
            reserva.put("id", 42);
            reserva.put("recurso", "Sala 203");
            reserva.put("local", "Bloco B, 2º andar");
            reserva.put("data", LocalDate.of(2026, 11, 12));
            reserva.put("inicio", LocalTime.of(14, 0));
            reserva.put("termino", LocalTime.of(15, 30));
            reserva.put("estado", estado);
            reserva.put("iniciada", iniciada);
            return reserva;
        }

        @GetMapping("/teste/reserva")
        String detalhe(@RequestParam(defaultValue = "Aprovada") String estado,
                @RequestParam(defaultValue = "false") boolean iniciada,
                @RequestParam(required = false) String acao, Model model) {
            model.addAttribute("reserva", reserva(estado, iniciada));
            if (acao != null) {
                model.addAttribute("acao", acao);
            }
            return "reserva-detalhe";
        }

        @GetMapping("/teste/reserva/rejeitada")
        String rejeitada(@RequestParam(required = false) String motivo, Model model) {
            Map<String, Object> reserva = reserva("Rejeitada", false);
            reserva.put("motivo", motivo);
            model.addAttribute("reserva", reserva);
            return "reserva-detalhe";
        }

        @GetMapping("/teste/reserva/sem-iniciada")
        String semIniciada(Model model) {
            Map<String, Object> reserva = reserva("Aprovada", false);
            reserva.put("iniciada", null);
            model.addAttribute("reserva", reserva);
            return "reserva-detalhe";
        }

        @GetMapping("/teste/reserva/criada")
        String criada(Model model) {
            model.addAttribute("reserva", reserva("Solicitada", false));
            model.addAttribute("confirmacao", Map.of("titulo", "Reserva enviada", "reserva", "Sala 203", "estado", "Solicitada"));
            return "reserva-detalhe";
        }

        @GetMapping("/teste/reserva/alteracao-recusada")
        String alteracaoRecusada(Model model) {
            model.addAttribute("reserva", reserva("Aprovada", false));
            model.addAttribute("acao", "alterar");
            model.addAttribute("formulario", new FormularioReserva("7", LocalDate.of(2026, 11, 13), LocalTime.of(16, 0), LocalTime.of(15, 0)));
            Map<String, String> erros = new LinkedHashMap<>();
            erros.put("data", "Escolha uma data a partir de hoje");
            erros.put("termino", "O término precisa ser depois do início");
            erros.put("geral", "Não foi possível salvar agora");
            model.addAttribute("erros", erros);
            return "reserva-detalhe";
        }

        @GetMapping("/teste/reserva/conflito")
        String conflito(Model model) {
            model.addAttribute("reserva", reserva("Aprovada", false));
            model.addAttribute("acao", "alterar");
            model.addAttribute("conflito", true);
            return "reserva-detalhe";
        }

        @GetMapping("/teste/reserva/manutencao")
        String manutencao(Model model) {
            model.addAttribute("reserva", reserva("Aprovada", false));
            model.addAttribute("acao", "alterar");
            model.addAttribute("manutencao", Map.of("inicio", LocalDateTime.of(2026, 11, 12, 8, 0),
                    "fim", LocalDateTime.of(2026, 11, 14, 18, 0)));
            return "reserva-detalhe";
        }

        @GetMapping("/teste/reserva/perigoso")
        String perigoso(@RequestParam String acao, Model model) {
            Map<String, Object> reserva = reserva("Aprovada", false);
            reserva.put("recurso", "<script>alert(1)</script>");
            reserva.put("local", "<img src=x onerror=alert(2)>");
            model.addAttribute("reserva", reserva);
            model.addAttribute("acao", acao);
            return "reserva-detalhe";
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
    void rotaGetDetalheUsaOLayoutBaseEMostraSoTituloEVoltarSemReserva() throws Exception {
        Document doc = pagina("/reservas/42");

        assertThat(doc.title()).isEqualTo("Detalhe da reserva · Organização de Recursos");
        assertThat(doc.select("title")).hasSize(1);
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.selectFirst("main#conteudo h1[data-testid=reserva-titulo]").text()).isEqualTo("Detalhe da reserva");
        var voltar = doc.selectFirst("a[data-testid=reserva-voltar]");
        assertThat(voltar.text()).isEqualTo("Voltar para minhas reservas");
        assertThat(voltar.attr("href")).isEqualTo("/reservas/minhas");
        assertThat(doc.select("[data-testid=reserva-resumo], main form, dialog")).isEmpty();
    }

    @Test
    void rotaExigeIdNumerico() throws Exception {
        mvc.perform(get("/reservas/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void rotasLiteraisNaoSaoConfundidasComOId() throws Exception {
        assertThat(pagina("/reservas/minhas").selectFirst("h1").text()).isEqualTo("Minhas reservas");
        assertThat(pagina("/reservas/nova").selectFirst("h1").text()).isEqualTo("Nova reserva");
    }

    @ParameterizedTest
    @CsvSource({"alterar", "cancelar", "apagar", "''"})
    void acaoSemReservaNoModeloNaoMostraFormularioNemDialogo(String acao) throws Exception {
        Document doc = pagina("/reservas/42?acao=" + acao);

        assertThat(doc.select("main form, dialog, script[src*=dialogo], script[src*=reserva-form]")).isEmpty();
    }

    // Resumo

    @Test
    void resumoMostraRecursoEstadoLocalDataEHorarioFormatados() throws Exception {
        Document doc = pagina("/teste/reserva");

        var resumo = doc.selectFirst("section.cartao[data-testid=reserva-resumo]");
        assertThat(doc.getElementById(resumo.attr("aria-labelledby")).text()).isEqualTo("Sala 203");
        assertThat(resumo.selectFirst("h2[data-testid=reserva-recurso]").text()).isEqualTo("Sala 203");
        var badge = resumo.selectFirst("[data-testid=reserva-estado]");
        assertThat(badge.text()).isEqualTo("Aprovada");
        assertThat(badge.hasClass("estado--aprovada")).isTrue();
        assertThat(resumo.select("dl.lista-dados dt").eachText()).containsExactly("Local", "Data", "Horário");
        assertThat(resumo.selectFirst("[data-testid=reserva-local]").text()).isEqualTo("Bloco B, 2º andar");
        assertThat(resumo.selectFirst("[data-testid=reserva-data]").text()).isEqualTo("12/11/2026");
        assertThat(resumo.selectFirst("[data-testid=reserva-horario]").text()).isEqualTo("14:00 às 15:30");
    }

    @Test
    void solicitadaAvisaQueAguardaAprovacao() throws Exception {
        Document doc = pagina("/teste/reserva?estado=Solicitada");

        assertThat(doc.selectFirst("[data-testid=reserva-aguardando]").text()).isEqualTo("Aguardando a aprovação de um Responsável.");
        assertThat(pagina("/teste/reserva").select("[data-testid=reserva-aguardando]")).isEmpty();
    }

    @Test
    void rejeitadaMostraOMotivoDaRejeicao() throws Exception {
        Document doc = pagina("/teste/reserva/rejeitada?motivo=Laboratório reservado para a banca");

        assertThat(doc.select("dl.lista-dados dt").eachText()).containsExactly("Local", "Data", "Horário", "Motivo da rejeição");
        assertThat(doc.selectFirst("[data-testid=reserva-motivo]").text()).isEqualTo("Laboratório reservado para a banca");
    }

    @ParameterizedTest
    @CsvSource({"/teste/reserva/rejeitada", "'/teste/reserva/rejeitada?motivo=   '"})
    void rejeitadaSemMotivoNaoMostraALinhaDoMotivo(String caminho) throws Exception {
        Document doc = pagina(caminho);

        assertThat(doc.select("[data-testid=reserva-motivo]")).isEmpty();
        assertThat(doc.select("dl.lista-dados dt").eachText()).containsExactly("Local", "Data", "Horário");
    }

    // Ações só quando o estado permite (RN-05, RN-07)

    @ParameterizedTest
    @CsvSource({"Solicitada", "Aprovada"})
    void solicitadaOuAprovadaQueNaoComecouOfereceAlterarECancelar(String estado) throws Exception {
        Document doc = pagina("/teste/reserva?estado=" + estado);

        var alterar = doc.selectFirst("a[data-testid=reserva-alterar]");
        assertThat(alterar.text()).isEqualTo("Alterar data ou horário");
        assertThat(alterar.attr("href")).isEqualTo("/reservas/42?acao=alterar");
        var cancelar = doc.selectFirst("a[data-testid=reserva-cancelar]");
        assertThat(cancelar.text()).isEqualTo("Cancelar reserva");
        assertThat(cancelar.attr("href")).isEqualTo("/reservas/42?acao=cancelar");
        assertThat(cancelar.hasClass("botao--perigo")).isTrue();
        assertThat(doc.select("[data-testid=reserva-sem-acao], main form, dialog")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "Em uso, true", "Concluída, true", "Rejeitada, false", "Cancelada, false", "Não compareceu, true",
            "Aprovada, true", "Solicitada, true", "aprovada, false", "Em análise, false"
    })
    void reservaIniciadaEmEstadoFinalOuForaDoContratoNaoOfereceNenhumaAcao(String estado, boolean iniciada) throws Exception {
        for (String acao : new String[] {"", "&acao=alterar", "&acao=cancelar"}) {
            Document doc = pagina("/teste/reserva?estado=" + estado + "&iniciada=" + iniciada + acao);

            assertThat(doc.select("[data-testid=reserva-alterar], [data-testid=reserva-cancelar], main form, dialog, script[src*=dialogo], script[src*=reserva-form]"))
                    .as("ações em %s, iniciada %s, %s", estado, iniciada, acao).isEmpty();
            assertThat(doc.selectFirst("[data-testid=reserva-sem-acao]").text())
                    .isEqualTo("Esta reserva não pode mais ser alterada nem cancelada.");
        }
    }

    @Test
    void iniciadaAusenteNoModeloContaComoIniciada() throws Exception {
        Document doc = pagina("/teste/reserva/sem-iniciada");

        assertThat(doc.select("[data-testid=reserva-alterar], [data-testid=reserva-cancelar]")).isEmpty();
        assertThat(doc.select("[data-testid=reserva-sem-acao]")).hasSize(1);
    }

    // Alterar

    @Test
    void alterarMostraFormularioPreenchidoComAReservaEEnviaPorPost() throws Exception {
        Document doc = pagina("/teste/reserva?acao=alterar");

        var secao = doc.selectFirst("section.cartao[data-testid=reserva-alterar-secao]");
        assertThat(doc.getElementById(secao.attr("aria-labelledby")).text()).isEqualTo("Alterar data ou horário");
        var form = secao.selectFirst("form[data-testid=reserva-alterar]");
        assertThat(form.attr("method")).isEqualToIgnoringCase("post");
        assertThat(form.attr("action")).isEqualTo("/reservas/42");
        assertThat(form.attr("data-reserva-form")).as("prefixo lido por reserva-form.js").isEqualTo("reserva-alterar");
        assertThat(form.selectFirst("input#reserva-alterar-data[type=date][name=data][required]").val()).isEqualTo("2026-11-12");
        assertThat(form.selectFirst("input#reserva-alterar-inicio[type=time][name=inicio][required]").val()).isEqualTo("14:00");
        assertThat(form.selectFirst("input#reserva-alterar-termino[type=time][name=termino][required]").val()).isEqualTo("15:30");
        for (String campo : new String[] {"data", "inicio", "termino"}) {
            assertThat(form.select("label[for=reserva-alterar-" + campo + "]")).as("rótulo de %s", campo).hasSize(1);
        }
        assertThat(form.select("label").eachText()).containsExactly("Data", "Hora de início", "Hora de término");
        var salvar = form.selectFirst("button[type=submit][data-testid=reserva-alterar-confirmar]");
        assertThat(salvar.text()).isEqualTo("Salvar alterações");
        assertThat(salvar.attr("data-enviando")).isEqualTo("Salvando…");
        var voltar = form.selectFirst("a[data-testid=reserva-alterar-voltar]");
        assertThat(voltar.text()).isEqualTo("Voltar sem alterar");
        assertThat(voltar.attr("href")).isEqualTo("/reservas/42");
        assertThat(doc.select("script[src='/js/reserva-form.js'][defer]")).hasSize(1);
        assertThat(doc.selectFirst("[data-testid=reserva-alterar-erro-resumo]").hasAttr("hidden")).isTrue();
    }

    @Test
    void todoSeletorEIdCitadoPeloScriptExisteNaAlteracao() throws Exception {
        GanchosDoScriptDeReserva.conferir(pagina("/teste/reserva?acao=alterar"));
    }

    @Test
    void comAlterarAbertoOsBotoesDoResumoSomemParaNaoDuplicarAAcao() throws Exception {
        Document doc = pagina("/teste/reserva?acao=alterar");

        assertThat(doc.select("a[data-testid=reserva-alterar], a[data-testid=reserva-cancelar], dialog")).isEmpty();
    }

    @Test
    void alteracaoRecusadaMantemOQueOUsuarioDigitouEMostraErrosNoCampoENoResumo() throws Exception {
        Document doc = pagina("/teste/reserva/alteracao-recusada");

        assertThat(doc.selectFirst("#reserva-alterar-data").val()).isEqualTo("2026-11-13");
        assertThat(doc.selectFirst("#reserva-alterar-inicio").val()).isEqualTo("16:00");
        assertThat(doc.selectFirst("#reserva-alterar-termino").val()).isEqualTo("15:00");

        var resumo = doc.selectFirst("[data-testid=reserva-alterar-erro-resumo]");
        assertThat(resumo.hasAttr("hidden")).isFalse();
        assertThat(resumo.attr("role")).isEqualTo("alert");
        assertThat(resumo.selectFirst(".alerta__titulo").text()).isEqualTo("Corrija 3 campos para continuar");
        assertThat(resumo.select("li").eachText()).containsExactly("Data: escolha uma data a partir de hoje",
                "Hora de término: o término precisa ser depois do início", "Não foi possível salvar agora");
        assertThat(resumo.select("a").eachAttr("href")).containsExactly("#reserva-alterar-data", "#reserva-alterar-termino");

        var termino = doc.selectFirst("#reserva-alterar-termino");
        assertThat(termino.attr("aria-invalid")).isEqualTo("true");
        assertThat(termino.attr("aria-describedby")).isEqualTo("reserva-alterar-erro-termino");
        assertThat(termino.hasClass("campo__entrada--erro")).isTrue();
        var erro = doc.getElementById("reserva-alterar-erro-termino");
        assertThat(erro.hasAttr("hidden")).isFalse();
        assertThat(erro.text()).isEqualTo("O término precisa ser depois do início");
        var inicio = doc.selectFirst("#reserva-alterar-inicio");
        assertThat(inicio.hasAttr("aria-invalid")).isFalse();
        assertThat(doc.getElementById("reserva-alterar-erro-inicio").hasAttr("hidden")).isTrue();
    }

    @Test
    void conflitoNaAlteracaoExplicaOMotivo() throws Exception {
        Document doc = pagina("/teste/reserva/conflito");

        var aviso = doc.selectFirst("[data-testid=reserva-alterar-erro-conflito]");
        assertThat(aviso.attr("role")).isEqualTo("alert");
        assertThat(aviso.selectFirst(".alerta__titulo").text()).isEqualTo("Não foi possível alterar");
        assertThat(aviso.selectFirst(".alerta__texto").text())
                .isEqualTo("Esse horário acabou de ser reservado ou se sobrepõe a outra reserva. Escolha outro horário.");
        assertThat(doc.select("[data-testid=reserva-alterar-erro-manutencao]")).isEmpty();
        assertThat(doc.select("form[data-testid=reserva-alterar]")).hasSize(1);
    }

    @Test
    void manutencaoNaAlteracaoMostraOPeriodo() throws Exception {
        Document doc = pagina("/teste/reserva/manutencao");

        var aviso = doc.selectFirst("[data-testid=reserva-alterar-erro-manutencao]");
        assertThat(aviso.attr("role")).isEqualTo("alert");
        assertThat(aviso.selectFirst(".alerta__texto").text()).isEqualTo(
                "O recurso Sala 203 está em manutenção de 12/11/2026, 08:00 até 14/11/2026, 18:00. Escolha outro período.");
        assertThat(doc.select("[data-testid=reserva-alterar-erro-conflito]")).isEmpty();
    }

    // Cancelar

    @Test
    void cancelarAbreDialogoDeConfirmacaoQueAvisaQueNaoPodeSerDesfeito() throws Exception {
        Document doc = pagina("/teste/reserva?acao=cancelar");

        var dialogo = doc.selectFirst("dialog.dialogo[data-testid=reserva-cancelar-dialogo]");
        assertThat(dialogo.hasAttr("open")).isTrue();
        assertThat(dialogo.attr("role")).isEqualTo("alertdialog");
        assertThat(doc.getElementById(dialogo.attr("aria-labelledby")).text()).isEqualTo("Cancelar a reserva?");
        var descricao = doc.getElementById(dialogo.attr("aria-describedby"));
        assertThat(descricao.text()).isEqualTo(
                "Sala 203, 12/11/2026, das 14:00 às 15:30. A reserva passa para Cancelada e esta ação não pode ser desfeita.");
        assertThat(descricao.selectFirst(".estado--cancelada").text()).isEqualTo("Cancelada");
        assertThat(doc.select("script[src='/js/dialogo.js'][defer]")).hasSize(1);
    }

    @Test
    void confirmarEnviaPostUmaVezSoEVoltarFechaSemCancelar() throws Exception {
        Document doc = pagina("/teste/reserva?acao=cancelar");

        var form = doc.selectFirst("dialog[data-testid=reserva-cancelar-dialogo] form");
        assertThat(form.attr("method")).isEqualToIgnoringCase("post");
        assertThat(form.attr("action")).isEqualTo("/reservas/42/cancelar");
        assertThat(form.hasAttr("data-envio-unico")).isTrue();
        assertThat(form.select("button[type=submit]")).hasSize(1);
        var confirmar = form.selectFirst("button[data-testid=reserva-cancelar-confirmar]");
        assertThat(confirmar.text()).isEqualTo("Sim, cancelar reserva");
        assertThat(confirmar.hasClass("botao--perigo")).isTrue();
        assertThat(confirmar.hasAttr("autofocus")).isFalse();
        var voltar = form.selectFirst("a[data-testid=reserva-cancelar-voltar]");
        assertThat(voltar.text()).isEqualTo("Voltar");
        assertThat(voltar.attr("href")).isEqualTo("/reservas/42");
        assertThat(voltar.hasAttr("autofocus")).as("o foco começa no gesto seguro").isTrue();
        assertThat(voltar.hasAttr("data-fechar")).isTrue();
    }

    @Test
    void dialogoGuardaOGatilhoQueOAbriuParaDevolverOFocoAoFechar() throws Exception {
        Document doc = pagina("/teste/reserva?acao=cancelar");

        var dialogo = doc.selectFirst("dialog[data-testid=reserva-cancelar-dialogo]");
        assertThat(dialogo.attr("data-origem")).isEqualTo("reserva-cancelar");
        assertThat(doc.select("[data-testid=" + dialogo.attr("data-origem") + "]")).hasSize(1);
    }

    @Test
    void dialogoReaproveitaAsClassesComuns() throws Exception {
        Document doc = pagina("/teste/reserva?acao=cancelar");

        assertThat(doc.selectFirst("#reserva-cancelar-titulo").hasClass("cartao__titulo")).isTrue();
        assertThat(doc.selectFirst("#reserva-cancelar-texto").hasClass("pagina__descricao")).isTrue();
        assertThat(doc.selectFirst("dialog form > div:has(a[data-fechar])").hasClass("formulario__acoes")).isTrue();
    }

    // Confirmação depois de criar, alterar ou cancelar

    @Test
    void reservaCriadaMostraAvisoComOEstadoAtual() throws Exception {
        Document doc = pagina("/teste/reserva/criada");

        var aviso = doc.selectFirst("div[data-testid=reserva-sucesso]");
        assertThat(aviso.attr("role")).isEqualTo("status");
        assertThat(aviso.selectFirst(".alerta__titulo").text()).isEqualTo("Reserva enviada");
        assertThat(aviso.selectFirst(".alerta__texto").text()).isEqualTo("Sala 203, agora está Solicitada. A mudança ficou registrada.");
        assertThat(aviso.selectFirst(".estado--solicitada")).isNotNull();
        assertThat(pagina("/teste/reserva").select("[role=status]")).isEmpty();
    }

    // Segurança

    @ParameterizedTest
    @CsvSource({"cancelar", "alterar"})
    void textoComHtmlEhEscapado(String acao) throws Exception {
        Document doc = pagina("/teste/reserva/perigoso?acao=" + acao);

        assertThat(doc.select("script:containsData(alert), img[onerror]")).isEmpty();
        assertThat(doc.selectFirst("[data-testid=reserva-recurso]").text()).isEqualTo("<script>alert(1)</script>");
        assertThat(doc.selectFirst("[data-testid=reserva-local]").text()).isEqualTo("<img src=x onerror=alert(2)>");
    }

    @Test
    void textoComHtmlEhEscapadoNoDialogo() throws Exception {
        Document doc = pagina("/teste/reserva/perigoso?acao=cancelar");

        assertThat(doc.selectFirst("#reserva-cancelar-texto").text()).startsWith("<script>alert(1)</script>, 12/11/2026");
    }
}
