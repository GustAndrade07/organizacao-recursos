package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.model.Recurso;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.support.ApiIntegracaoTest;
import br.com.senac.recursos.web.sessao.UsuarioDaSessao;
import java.time.Instant;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * As telas ligadas aos services (D12, opção (a), ADR-0009), contra PostgreSQL real (Testcontainers, nunca H2): o
 * usuário entra na sessão como o {@code POST /login} deixa, a tela chama o mesmo service da API e o resultado é
 * conferido no HTML <b>e no banco</b>. Datas no fuso das telas (São Paulo, UTC-3): 10/11/2026 das 14:00 às 15:00 é
 * 17:00 a 18:00 em UTC, no futuro em relação ao relógio do teste (01/11/2026).
 */
class TelasComBancoTest extends ApiIntegracaoTest {

    private Usuario laura;
    private Usuario rafael;
    private Usuario responsavel;
    private Usuario administrador;
    private Recurso sala;
    private Recurso laboratorio;

    @BeforeEach
    void cenario() {
        laura = usuario("laura", PerfilUsuario.SOLICITANTE);
        rafael = usuario("rafael", PerfilUsuario.SOLICITANTE);
        responsavel = usuario("bruno", PerfilUsuario.RESPONSAVEL);
        administrador = usuario("admin", PerfilUsuario.ADMINISTRADOR);
        sala = recurso(TipoRecurso.SALA, "Sala 203", false);
        laboratorio = recurso(TipoRecurso.SALA, "Laboratório de Química", true);
    }

    // ---------------------------------------------------------------- busca e nova reserva (RF-03, RF-04)

    @Test
    void buscaMostraSoOQueEstaLivreNoHorario() throws Exception {
        reservarPelaTela(rafael, sala, "14:00", "15:00");

        Document doc = abrir(laura, "/recursos/disponiveis?data=2026-11-10&inicio=14:30&termino=15:30");

        assertThat(doc.select("[data-testid^=resultado-linha-]")).hasSize(1);
        assertThat(doc.selectFirst("[data-testid=resultado-nome-" + laboratorio.getId() + "]").text())
                .contains("Laboratório de Química").contains("Restrito");
        assertThat(doc.selectFirst("[data-testid=resultado-reservar-" + laboratorio.getId() + "]").attr("href"))
                .isEqualTo("/reservas/nova?recursoId=" + laboratorio.getId()
                        + "&data=2026-11-10&inicio=14:30&termino=15:30");
    }

    @Test
    void novaReservaGravaNoBancoNoFusoDasTelasELevaAoDetalheComAConfirmacao() throws Exception {
        MvcResult resultado = enviar(laura, "/reservas", Map.of("recursoId", id(sala), "data", "2026-11-10",
                "inicio", "14:00", "termino", "15:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("confirmacao"))
                .andReturn();

        long reserva = contar("select id from reserva");
        assertThat(resultado.getResponse().getRedirectedUrl()).isEqualTo("/reservas/" + reserva);
        assertThat(contar("select count(*) from reserva_recurso where reserva_id = ? and periodo = "
                + "'[2026-11-10 17:00:00+00,2026-11-10 18:00:00+00)'::tstzrange", reserva)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, reserva))
                .isEqualTo("APROVADA");

        Document detalhe = abrir(laura, "/reservas/" + reserva);
        assertThat(detalhe.selectFirst("[data-testid=reserva-recurso]").text()).isEqualTo("Sala 203");
        assertThat(detalhe.text()).contains("10/11/2026").contains("14:00 às 15:00").contains("Aprovada");
    }

    @Test
    void horarioOcupadoVoltaAoFormularioComOAvisoDeConflitoEONaoGravaASegunda() throws Exception {
        reservarPelaTela(rafael, sala, "14:00", "15:00");

        String html = enviar(laura, "/reservas", Map.of("recursoId", id(sala), "data", "2026-11-10",
                "inicio", "14:30", "termino", "15:30"))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        assertThat(Jsoup.parse(html).selectFirst("[data-testid=reserva-form-erro-conflito]")).isNotNull();
        assertThat(contar("select count(*) from reserva")).isEqualTo(1);
        assertThat(contar("select count(*) from tentativa_recusada")).isEqualTo(1);
    }

    @Test
    void recursoEmManutencaoVoltaComOPeriodoDaManutencao() throws Exception {
        enviar(administrador, "/admin/recursos/" + id(sala) + "/manutencao", Map.of("inicio", "2026-11-10T08:00",
                "fim", "2026-11-10T18:00")).andExpect(status().is3xxRedirection());

        String html = enviar(laura, "/reservas", Map.of("recursoId", id(sala), "data", "2026-11-10",
                "inicio", "14:00", "termino", "15:00"))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        assertThat(Jsoup.parse(html).selectFirst("[data-testid=reserva-form-erro-manutencao]").text())
                .contains("10/11/2026, 08:00").contains("10/11/2026, 18:00");
        assertThat(contar("select count(*) from reserva")).isZero();
    }

    @Test
    void terminoAntesDoInicioVoltaComOErroNoCampoSemChamarOBanco() throws Exception {
        String html = enviar(laura, "/reservas", Map.of("recursoId", id(sala), "data", "2026-11-10",
                "inicio", "15:00", "termino", "14:00"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(Jsoup.parse(html).selectFirst("[data-testid=reserva-form-erro-resumo]").text())
                .contains("término precisa ser depois do início");
        assertThat(contar("select count(*) from reserva")).isZero();
    }

    // ---------------------------------------------------------------- minhas reservas e detalhe (RF-04, RN-07)

    @Test
    void minhasReservasMostraSoAsDoUsuarioLogadoMesmoParaQuemVeTodas() throws Exception {
        long daLaura = reservarPelaTela(laura, sala, "14:00", "15:00");
        long doBruno = reservarPelaTela(responsavel, laboratorio, "14:00", "15:00");

        assertThat(abrir(laura, "/reservas/minhas").select("[data-testid^=minhas-linha-]").eachAttr("data-testid"))
                .containsExactly("minhas-linha-" + daLaura);
        assertThat(abrir(responsavel, "/reservas/minhas").select("[data-testid^=minhas-linha-]")
                .eachAttr("data-testid")).containsExactly("minhas-linha-" + doBruno);
        assertThat(abrir(laura, "/reservas/minhas?estado=cancelada").select("[data-testid=minhas-vazio-filtro]"))
                .hasSize(1);
    }

    @Test
    void solicitanteNaoAbreAReservaDeOutraPessoa() throws Exception {
        long doRafael = reservarPelaTela(rafael, sala, "14:00", "15:00");

        mvc.perform(get("/reservas/" + doRafael).with(sessao(laura))).andExpect(status().isForbidden());
        mvc.perform(post("/reservas/" + doRafael + "/cancelar").with(sessao(laura)).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, doRafael))
                .isEqualTo("APROVADA");
    }

    @Test
    void alterarTrocaOPeriodoNoBancoECancelarLiberaOHorario() throws Exception {
        long reserva = reservarPelaTela(laura, sala, "14:00", "15:00");

        enviar(laura, "/reservas/" + reserva, Map.of("data", "2026-11-11", "inicio", "09:00", "termino", "10:00"))
                .andExpect(redirectedUrl("/reservas/" + reserva))
                .andExpect(flash().attributeExists("confirmacao"));
        assertThat(contar("select count(*) from reserva_recurso where reserva_id = ? and periodo = "
                + "'[2026-11-11 12:00:00+00,2026-11-11 13:00:00+00)'::tstzrange", reserva)).isEqualTo(1);

        enviar(laura, "/reservas/" + reserva + "/cancelar", Map.of()).andExpect(redirectedUrl("/reservas/" + reserva));
        assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, reserva))
                .isEqualTo("CANCELADA");
        // D5: cancelada não ocupa o horário, então outra pessoa consegue reservar o mesmo período
        reservarPelaTela(rafael, sala, "2026-11-11", "09:00", "10:00");
    }

    @Test
    void reservaQueJaComecouNaoOfereceAlterarNemCancelar() throws Exception {
        long reserva = reservarPelaTela(laura, sala, "14:00", "15:00");
        relogio.ajustar(Instant.parse("2026-11-10T17:30:00Z"));

        Document doc = abrir(laura, "/reservas/" + reserva);

        assertThat(doc.select("[data-testid=reserva-alterar], [data-testid=reserva-cancelar]")).isEmpty();
        assertThat(doc.selectFirst("[data-testid=reserva-sem-acao]")).isNotNull();
    }

    // ---------------------------------------------------------------- aprovação (RF-07, RN-06)

    @Test
    void recursoRestritoEsperaAprovacaoEOResponsavelAprovaNaFila() throws Exception {
        long reserva = reservarPelaTela(laura, laboratorio, "14:00", "15:00");
        assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, reserva))
                .isEqualTo("SOLICITADA");

        Document fila = abrir(responsavel, "/aprovacoes");
        assertThat(fila.selectFirst("[data-testid=aprovacao-linha-" + reserva + "]").text())
                .contains("Laboratório de Química").contains("Usuário laura");

        enviar(responsavel, "/aprovacoes/" + reserva + "/aprovar", Map.of())
                .andExpect(redirectedUrl("/aprovacoes"))
                .andExpect(flash().attributeExists("confirmacao"));
        assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, reserva))
                .isEqualTo("APROVADA");
        assertThat(contar("select count(*) from auditoria_reserva where reserva_id = ? and estado_novo = 'APROVADA'",
                reserva)).isEqualTo(1);
    }

    @Test
    void responsavelNaoAprovaAPropriaReservaETelaMostraOAviso() throws Exception {
        long propria = reservarPelaTela(responsavel, laboratorio, "14:00", "15:00");

        String html = enviar(responsavel, "/aprovacoes/" + propria + "/aprovar", Map.of())
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        assertThat(Jsoup.parse(html).select("[data-testid=aprovacao-erro-403]")).hasSize(1);
        assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, propria))
                .isEqualTo("SOLICITADA");
    }

    @Test
    void rejeitarExigeMotivoEOMotivoApareceParaQuemPediu() throws Exception {
        long reserva = reservarPelaTela(laura, laboratorio, "14:00", "15:00");

        String semMotivo = enviar(responsavel, "/aprovacoes/" + reserva + "/rejeitar", Map.of("motivo", "  "))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(Jsoup.parse(semMotivo).selectFirst("[data-testid=aprovacao-motivo-erro]").text())
                .isEqualTo("Informe o motivo da rejeição.");
        assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, reserva))
                .isEqualTo("SOLICITADA");

        enviar(responsavel, "/aprovacoes/" + reserva + "/rejeitar", Map.of("motivo", "Laboratório em reforma."))
                .andExpect(redirectedUrl("/aprovacoes"));
        assertThat(abrir(laura, "/reservas/" + reserva).text()).contains("Rejeitada").contains("Laboratório em reforma.");
    }

    // ---------------------------------------------------------------- retirada, devolução e histórico (RF-09, RF-10)

    @Test
    void retiradaEDevolucaoMudamOEstadoEOHistoricoMostraCadaPasso() throws Exception {
        long reserva = reservarPelaTela(laura, sala, "14:00", "15:00");

        assertThat(abrir(responsavel, "/retirada-devolucao").text()).contains("Sala 203");
        enviar(responsavel, "/retirada-devolucao/" + reserva + "/retirar", Map.of())
                .andExpect(redirectedUrl("/retirada-devolucao"));
        enviar(responsavel, "/retirada-devolucao/" + reserva + "/devolver", Map.of())
                .andExpect(redirectedUrl("/retirada-devolucao"));
        assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, reserva))
                .isEqualTo("CONCLUIDA");

        Document historico = abrir(responsavel, "/reservas/" + reserva + "/historico");
        assertThat(historico.select("[data-testid$=-para]").eachText())
                .containsExactly("Aprovada", "Em uso", "Concluída");
        assertThat(historico.text()).contains("Usuário bruno").contains("Usuário laura");
    }

    @Test
    void devolverReservaQueNaoEstaEmUsoRespondeConflitoSemMudarNada() throws Exception {
        long reserva = reservarPelaTela(laura, sala, "14:00", "15:00");

        enviar(responsavel, "/retirada-devolucao/" + reserva + "/devolver", Map.of()).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, reserva))
                .isEqualTo("APROVADA");
    }

    @Test
    void naoCompareceuSoEOferecidoDepoisQueOHorarioTermina() throws Exception {
        long reserva = reservarPelaTela(laura, sala, "14:00", "15:00");
        String dialogo = "/retirada-devolucao?naoCompareceu=" + reserva;

        assertThat(abrir(responsavel, dialogo).select("dialog")).isEmpty();
        relogio.ajustar(Instant.parse("2026-11-10T18:00:00Z"));
        assertThat(abrir(responsavel, dialogo).select("dialog")).hasSize(1);
    }

    // ---------------------------------------------------------------- relatório (RF-12)

    @Test
    void relatorioContaReservasEHorasNoPeriodoDoFusoDasTelas() throws Exception {
        reservarPelaTela(laura, sala, "14:00", "15:30");
        reservarPelaTela(rafael, sala, "16:00", "17:00");
        long cancelada = reservarPelaTela(rafael, laboratorio, "08:00", "09:00");
        enviar(rafael, "/reservas/" + cancelada + "/cancelar", Map.of());

        Document doc = abrir(responsavel, "/relatorios/utilizacao?inicio=2026-11-10&fim=2026-11-10");

        assertThat(doc.selectFirst("[data-testid=relatorio-recursos-total]").text())
                .isEqualTo("Total: 2 reservas, 2 h 30 min");
        assertThat(doc.selectFirst("[data-testid=relatorio-recursos-linha-1]").text())
                .contains("Sala 203").contains("2").contains("2 h 30 min");
        assertThat(doc.select("[data-testid^=relatorio-recursos-linha-]")).hasSize(1);
    }

    // ---------------------------------------------------------------- recursos e manutenção (RF-02, RF-08)

    @Test
    void administradorCadastraEEditaRecurso() throws Exception {
        enviar(administrador, "/admin/recursos", Map.of("tipo", "salas", "nome", "Auditório", "capacidade", "120",
                "restrito", "true"))
                .andExpect(redirectedUrl("/admin/recursos?tipo=salas"))
                .andExpect(flash().attributeExists("confirmacao"));
        long auditorio = contar("select id from recurso where nome = 'Auditório'");
        assertThat(contar("select count(*) from recurso where id = ? and capacidade = 120 and restrito", auditorio))
                .isEqualTo(1);

        enviar(administrador, "/admin/recursos/" + auditorio, Map.of("nome", "Auditório Central", "capacidade", "150"))
                .andExpect(redirectedUrl("/admin/recursos?tipo=salas"));
        assertThat(contar("select count(*) from recurso where id = ? and nome = 'Auditório Central' "
                + "and capacidade = 150 and not restrito", auditorio)).isEqualTo(1);

        Document lista = abrir(administrador, "/admin/recursos?tipo=salas&nome=central");
        assertThat(lista.select("[data-testid^=recursos-linha-]").eachAttr("data-testid"))
                .containsExactly("recursos-linha-" + auditorio);
    }

    @Test
    void cadastroSemCapacidadeVoltaComOErroENaoGrava() throws Exception {
        String html = enviar(administrador, "/admin/recursos", Map.of("tipo", "salas", "nome", "Sala sem número"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(Jsoup.parse(html).text()).contains("Informe a capacidade.");
        assertThat(contar("select count(*) from recurso where nome = 'Sala sem número'")).isZero();
    }

    @Test
    void bloqueioComReservaNoPeriodoPedeConfirmacaoAntesDeGravar() throws Exception {
        reservarPelaTela(laura, sala, "14:00", "15:00");
        Map<String, String> periodo = Map.of("inicio", "2026-11-10T08:00", "fim", "2026-11-10T18:00");

        String aviso = enviar(administrador, "/admin/recursos/" + id(sala) + "/manutencao", periodo)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(Jsoup.parse(aviso).selectFirst("[data-testid=manutencao-afetadas]").text())
                .contains("Usuário laura").contains("14:00 às 15:00");
        assertThat(contar("select count(*) from manutencao")).isZero();

        Map<String, String> confirmado = new java.util.HashMap<>(periodo);
        confirmado.put("confirmar", "true");
        enviar(administrador, "/admin/recursos/" + id(sala) + "/manutencao", confirmado)
                .andExpect(redirectedUrl("/admin/recursos?tipo=salas"));
        assertThat(contar("select count(*) from manutencao where recurso_id = ? and periodo = "
                + "'[2026-11-10 11:00:00+00,2026-11-10 21:00:00+00)'::tstzrange", sala.getId())).isEqualTo(1);
        // D11 (comportamento atual): a reserva que já existia continua
        assertThat(contar("select count(*) from reserva where estado = 'APROVADA'")).isEqualTo(1);
    }

    // ---------------------------------------------------------------- usuários (tela 16) e histórico

    @Test
    void administradorCadastraUsuarioComSenhaSoEmHashEOUsuarioNovoEntra() throws Exception {
        enviar(administrador, "/admin/usuarios", Map.of("nome", "Carla Dias", "login", "Carla.Dias@Exemplo.com",
                "senha", "senha-da-carla-1", "perfil", "RESPONSAVEL"))
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attributeExists("confirmacao"));

        String hash = jdbc.queryForObject("select senha_hash from usuario where login = 'carla.dias@exemplo.com'", String.class);
        assertThat(hash).startsWith("$2").doesNotContain("senha-da-carla-1");
        assertThat(jdbc.queryForObject("select perfil from usuario where login = 'carla.dias@exemplo.com'", String.class))
                .isEqualTo("RESPONSAVEL");
        assertThat(abrir(administrador, "/admin/usuarios").text()).contains("Carla Dias").contains("carla.dias@exemplo.com");
        // O login da tela aceita a usuária nova (mesma tabela e mesmo BCrypt)
        mvc.perform(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders
                        .formLogin("/login").userParameter("login").passwordParam("senha")
                        .user("  CARLA.dias@Exemplo.com ").password("senha-da-carla-1"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginRepetidoNaoCriaSegundoUsuario() throws Exception {
        String html = enviar(administrador, "/admin/usuarios", Map.of("nome", "Outra Laura", "login", "LAURA@exemplo.com",
                "senha", "senha-qualquer-1", "perfil", "SOLICITANTE"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(Jsoup.parse(html).selectFirst("[data-testid=usuario-form-login-erro]").text())
                .isEqualTo("Já existe um usuário com este e-mail.");
        assertThat(contar("select count(*) from usuario where login = 'laura@exemplo.com'")).isEqualTo(1);
    }

    @Test
    void historicoTemLinkNoDetalheENaRetiradaSoParaQuemPodeVer() throws Exception {
        long reserva = reservarPelaTela(laura, sala, "14:00", "15:00");

        assertThat(abrir(laura, "/reservas/" + reserva).select("[data-testid=reserva-historico]")).isEmpty();
        assertThat(abrir(responsavel, "/reservas/" + reserva).selectFirst("[data-testid=reserva-historico]")
                .attr("href")).isEqualTo("/reservas/" + reserva + "/historico");
        assertThat(abrir(responsavel, "/retirada-devolucao").selectFirst(
                "[data-testid=retirada-devolucao-historico-" + reserva + "]").attr("href"))
                .isEqualTo("/reservas/" + reserva + "/historico");
        mvc.perform(get("/reservas/" + reserva + "/historico").with(sessao(laura))).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- menu e início (RF-01)

    @Test
    void inicioLevaCadaPerfilParaATelaDoSeuMenu() throws Exception {
        mvc.perform(get("/").with(sessao(laura))).andExpect(redirectedUrl("/recursos/disponiveis"));
        mvc.perform(get("/").with(sessao(responsavel))).andExpect(redirectedUrl("/aprovacoes"));
        mvc.perform(get("/").with(sessao(administrador))).andExpect(redirectedUrl("/admin/recursos"));
    }

    @Test
    void menuMostraONomeEOPerfilDeQuemEntrouEMarcaAPaginaAtual() throws Exception {
        Document doc = abrir(responsavel, "/retirada-devolucao");

        assertThat(doc.selectFirst("[data-testid=menu-usuario]").text()).isEqualTo("Usuário bruno");
        assertThat(doc.selectFirst("[data-testid=menu-perfil]").text()).isEqualTo("Responsável");
        assertThat(doc.select("[data-testid^=menu-item-]").eachText())
                .containsExactly("Aprovações", "Retirada e devolução", "Relatórios");
        assertThat(doc.selectFirst("[data-testid=menu-item-retirada]").attr("aria-current")).isEqualTo("page");
    }

    // ---------------------------------------------------------------- apoio

    private long reservarPelaTela(Usuario quem, Recurso recurso, String inicio, String termino) throws Exception {
        return reservarPelaTela(quem, recurso, "2026-11-10", inicio, termino);
    }

    private long reservarPelaTela(Usuario quem, Recurso recurso, String data, String inicio, String termino)
            throws Exception {
        String destino = enviar(quem, "/reservas", Map.of("recursoId", id(recurso), "data", data, "inicio", inicio,
                "termino", termino))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        return Long.parseLong(destino.substring("/reservas/".length()));
    }

    private Document abrir(Usuario quem, String url) throws Exception {
        String html = mvc.perform(get(url).with(sessao(quem)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Jsoup.parse(html);
    }

    private ResultActions enviar(Usuario quem, String url, Map<String, String> campos) throws Exception {
        MockHttpServletRequestBuilder pedido = post(url).with(sessao(quem)).with(csrf());
        campos.forEach(pedido::param);
        return mvc.perform(pedido);
    }

    private static RequestPostProcessor sessao(Usuario usuario) {
        UsuarioDaSessao daSessao = new UsuarioDaSessao(usuario.getId(), usuario.getNome(), usuario.getLogin(),
                usuario.getPerfil(), null);
        return authentication(UsernamePasswordAuthenticationToken.authenticated(daSessao, null,
                daSessao.getAuthorities()));
    }

    private static String id(Recurso recurso) {
        return Long.toString(recurso.getId());
    }
}
