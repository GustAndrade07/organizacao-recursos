package br.com.senac.recursos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.model.Recurso;
import br.com.senac.recursos.support.ApiIntegracaoTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Regras da reserva pela API (caixa-preta): RN-01 a RN-08, D1, D3, D5 e D6. As contagens de "nenhuma reserva criada"
 * são feitas no banco, nunca pela resposta.
 */
class ReservaControllerTest extends ApiIntegracaoTest {

    private String solicitante;
    private String outroSolicitante;
    private String responsavel;
    private String administrador;
    private Recurso sala;
    private Recurso professor;
    private Recurso laboratorio;

    @BeforeEach
    void cenario() {
        solicitante = token(usuario("sol", PerfilUsuario.SOLICITANTE));
        outroSolicitante = token(usuario("sol2", PerfilUsuario.SOLICITANTE));
        responsavel = token(usuario("resp", PerfilUsuario.RESPONSAVEL));
        administrador = token(usuario("admin", PerfilUsuario.ADMINISTRADOR));
        sala = recurso(TipoRecurso.SALA, "Sala C237", false);
        professor = recurso(TipoRecurso.PROFESSOR, "Prof. Afonso", false);
        laboratorio = recurso(TipoRecurso.SALA, "Laboratório", true);
    }

    private long reservasGravadas() {
        return contar("select count(*) from reserva");
    }

    @Nested
    class Criar {

        @Test
        void salaComProfessorSemRestritoNasceAprovadaComAuditoria() throws Exception {
            enviar(solicitante, "/reservas", corpoReserva(dia("10:00"), dia("11:00"), professor.getId(), sala.getId()))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/reservas/1"))
                    .andExpect(jsonPath("$.estado").value("APROVADA"))
                    .andExpect(jsonPath("$.criadaEm").value("2026-11-01T12:00:00Z"))
                    .andExpect(jsonPath("$.recursos[*].id", contains(sala.getId().intValue(),
                            professor.getId().intValue())));

            // D6: uma linha por recurso, em ordem de recurso_id, com o estado copiado
            assertThat(jdbc.queryForList("select recurso_id from reserva_recurso order by recurso_id", Long.class))
                    .containsExactly(sala.getId(), professor.getId());
            assertThat(contar("select count(*) from auditoria_reserva where estado_anterior is null "
                    + "and estado_novo = 'APROVADA' and instante = ?", java.sql.Timestamp.from(AGORA))).isOne();
        }

        @Test
        void recursoRestritoNasceSolicitada() throws Exception {
            enviar(solicitante, "/reservas", corpoReserva(dia("10:00"), dia("11:00"), laboratorio.getId()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estado").value("SOLICITADA"));
        }

        @Test
        void terminoAntesDoInicioDa400ENaoGrava() throws Exception {
            enviar(solicitante, "/reservas", corpoReserva(dia("11:00"), dia("10:00"), sala.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("O término precisa ser depois do início."));
            assertThat(reservasGravadas()).isZero();
        }

        @Test
        void listaVaziaRecursoRepetidoEInexistenteSaoRecusados() throws Exception {
            enviar(solicitante, "/reservas", "{\"recursoIds\":[],\"inicio\":\"" + dia("10:00") + "\",\"fim\":\""
                    + dia("11:00") + "\"}").andExpect(status().isBadRequest());
            enviar(solicitante, "/reservas", corpoReserva(dia("10:00"), dia("11:00"), sala.getId(), sala.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("O mesmo recurso foi informado mais de uma vez."));
            enviar(solicitante, "/reservas", corpoReserva(dia("10:00"), dia("11:00"), sala.getId(), 999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Recurso 999 não encontrado(a)."));
            assertThat(reservasGravadas()).isZero();
        }

        @Test
        void dataSemDeslocamentoDa400Legivel() throws Exception {
            enviar(solicitante, "/reservas", "{\"recursoIds\":[1],\"inicio\":\"10/11/2026 10:00\",\"fim\":\"x\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:corpo-ilegivel"));
        }
    }

    @Nested
    class Sobreposicao {

        /** D1: [início, fim). Reserva existente 10:00 a 12:00. */
        @ParameterizedTest(name = "{0} a {1} -> {2}")
        @CsvSource({
            "09:00, 10:00, 201",
            "12:00, 13:00, 201",
            "09:00, 10:01, 409",
            "11:59, 13:00, 409",
            "10:30, 11:30, 409"
        })
        void fronteirasDaSala(String inicio, String fim, int esperado) throws Exception {
            reservar(outroSolicitante, dia("10:00"), dia("12:00"), sala.getId());

            enviar(solicitante, "/reservas", corpoReserva(dia(inicio), dia(fim), sala.getId()))
                    .andExpect(status().is(esperado));
        }

        @Test
        void sobreposicaoDa409GravaTentativaENaoGravaReserva() throws Exception {
            reservar(outroSolicitante, dia("10:00"), dia("12:00"), sala.getId());

            enviar(solicitante, "/reservas", corpoReserva(dia("11:00"), dia("13:00"), sala.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:conflito-de-horario"))
                    .andExpect(jsonPath("$.recursos[0]").value(sala.getId()));

            assertThat(reservasGravadas()).isOne();
            assertThat(contar("select count(*) from tentativa_recusada where recurso_id = ? "
                    + "and motivo = 'SOBREPOSICAO' and solicitante_id = 1", sala.getId())).isOne();
        }

        @Test
        void agendaDoProfessorTambemEhProtegidaMesmoEmOutraSala() throws Exception {
            Recurso outraSala = recurso(TipoRecurso.SALA, "Sala D156", false);
            reservar(outroSolicitante, dia("10:00"), dia("12:00"), sala.getId(), professor.getId());

            enviar(solicitante, "/reservas", corpoReserva(dia("11:00"), dia("12:00"), outraSala.getId(),
                    professor.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.recursos[0]").value(professor.getId()));

            // a tentativa é do professor, não da sala livre
            assertThat(jdbc.queryForList("select recurso_id from tentativa_recusada", Long.class))
                    .containsExactly(professor.getId());
            assertThat(contar("select count(*) from reserva_recurso where recurso_id = ?", outraSala.getId())).isZero();
        }

        @Test
        void cancelarLiberaOHorario() throws Exception {
            long primeira = reservar(outroSolicitante, dia("10:00"), dia("12:00"), sala.getId());
            enviar(outroSolicitante, "/reservas/" + primeira + "/cancelamento", "{\"motivo\":\"aula suspensa\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CANCELADA"));

            enviar(solicitante, "/reservas", corpoReserva(dia("10:00"), dia("12:00"), sala.getId()))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    class Manutencao {

        @ParameterizedTest(name = "{0} a {1} -> {2}")
        @CsvSource({
            "09:00, 10:00, 201",
            "12:00, 13:00, 201",
            "09:00, 10:01, 409",
            "11:59, 13:00, 409",
            "10:30, 11:30, 409"
        })
        void fronteirasDaManutencaoDe10a12(String inicio, String fim, int esperado) throws Exception {
            enviar(administrador, "/recursos/" + sala.getId() + "/manutencoes", corpoPeriodo(dia("10:00"), dia("12:00")))
                    .andExpect(status().isCreated());

            enviar(solicitante, "/reservas", corpoReserva(dia(inicio), dia(fim), sala.getId()))
                    .andExpect(status().is(esperado));
        }

        @Test
        void manutencaoDa409DeTipoProprioSemTentativaESemReserva() throws Exception {
            enviar(administrador, "/recursos/" + sala.getId() + "/manutencoes", corpoPeriodo(dia("10:00"), dia("12:00")))
                    .andExpect(status().isCreated());

            enviar(solicitante, "/reservas", corpoReserva(dia("10:30"), dia("11:30"), professor.getId(), sala.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:recurso-em-manutencao"))
                    .andExpect(jsonPath("$.recursos", contains(sala.getId().intValue())));

            assertThat(reservasGravadas()).isZero();
            assertThat(contar("select count(*) from tentativa_recusada")).isZero();
        }
    }

    @Nested
    class Alterar {

        @Test
        void donoMudaOPeriodoEAAlteracaoEntraNoHistorico() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId(), professor.getId());

            substituir(solicitante, "/reservas/" + id + "/periodo", corpoPeriodo(dia("14:00"), dia("15:00")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.inicio").value(dia("14:00")))
                    .andExpect(jsonPath("$.estado").value("APROVADA"));

            assertThat(contar("select count(*) from reserva_recurso where lower(periodo) = '2026-11-10T14:00:00Z'"))
                    .isEqualTo(2);
            obter(responsavel, "/reservas/" + id + "/historico")
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[1].estadoAnterior").value("APROVADA"))
                    .andExpect(jsonPath("$[1].estadoNovo").value("APROVADA"));
        }

        @Test
        void mudarParaHorarioOcupadoDa409MantemOAntigoEGravaTentativa() throws Exception {
            reservar(outroSolicitante, dia("14:00"), dia("15:00"), sala.getId());
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            substituir(solicitante, "/reservas/" + id + "/periodo", corpoPeriodo(dia("14:30"), dia("15:30")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:conflito-de-horario"));

            obter(solicitante, "/reservas/" + id).andExpect(jsonPath("$.inicio").value(dia("10:00")));
            assertThat(contar("select count(*) from tentativa_recusada")).isOne();
        }

        @Test
        void encostarNoProprioHorarioNaoConflitaConsigoMesma() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            substituir(solicitante, "/reservas/" + id + "/periodo", corpoPeriodo(dia("10:30"), dia("11:30")))
                    .andExpect(status().isOk());
        }

        @Test
        void outroSolicitanteRecebe403EAReservaFicaIntacta() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            substituir(outroSolicitante, "/reservas/" + id + "/periodo", corpoPeriodo(dia("14:00"), dia("15:00")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:acesso-negado"));

            obter(solicitante, "/reservas/" + id).andExpect(jsonPath("$.inicio").value(dia("10:00")));
        }

        @Test
        void reservaJaIniciadaNaoMudaDePeriodo() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            relogio.ajustar(Instant.parse(dia("10:00")));

            substituir(solicitante, "/reservas/" + id + "/periodo", corpoPeriodo(dia("14:00"), dia("15:00")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:reserva-ja-iniciada"));
        }

        @Test
        void reservaCanceladaNaoMudaDePeriodo() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            enviar(solicitante, "/reservas/" + id + "/cancelamento", null).andExpect(status().isOk());

            substituir(solicitante, "/reservas/" + id + "/periodo", corpoPeriodo(dia("14:00"), dia("15:00")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:transicao-invalida"));
        }

        @Test
        void periodoParaManutencaoDa409() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            enviar(administrador, "/recursos/" + sala.getId() + "/manutencoes", corpoPeriodo(dia("14:00"), dia("16:00")))
                    .andExpect(status().isCreated());

            substituir(solicitante, "/reservas/" + id + "/periodo", corpoPeriodo(dia("15:00"), dia("17:00")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:recurso-em-manutencao"));
        }

        @Test
        void reservaInexistenteDa404() throws Exception {
            substituir(solicitante, "/reservas/99/periodo", corpoPeriodo(dia("14:00"), dia("15:00")))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Cancelar {

        @Test
        void outroSolicitanteRecebe403EAReservaContinuaAprovada() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            enviar(outroSolicitante, "/reservas/" + id + "/cancelamento", null).andExpect(status().isForbidden());

            assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, id))
                    .isEqualTo("APROVADA");
        }

        /** RN-07: a partir do instante de início a reserva não pode mais ser cancelada. */
        @Test
        void reservaIniciadaNaoPodeSerCancelada() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            long mesmoHorario = reservar(solicitante, dia("10:00"), dia("11:00"), professor.getId());

            relogio.ajustar(Instant.parse("2026-11-10T09:59:59Z"));
            enviar(solicitante, "/reservas/" + mesmoHorario + "/cancelamento", null).andExpect(status().isOk());

            relogio.ajustar(Instant.parse(dia("10:00")));
            enviar(solicitante, "/reservas/" + id + "/cancelamento", null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:reserva-ja-iniciada"));
        }

        @Test
        void motivoLongoDemaisDa400() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            enviar(solicitante, "/reservas/" + id + "/cancelamento", "{\"motivo\":\"" + "x".repeat(501) + "\"}")
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Aprovacao {

        @Test
        void responsavelAprovaRestritoEHistoricoGuardaQuemEQuando() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), laboratorio.getId());
            relogio.ajustar(Instant.parse("2026-11-02T08:00:00Z"));

            enviar(responsavel, "/reservas/" + id + "/aprovacao", "{\"motivo\":\" turma confirmada \"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("APROVADA"));

            obter(administrador, "/reservas/" + id + "/historico")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].estadoAnterior").doesNotExist())
                    .andExpect(jsonPath("$[0].estadoNovo").value("SOLICITADA"))
                    .andExpect(jsonPath("$[1].usuarioLogin").value("resp"))
                    .andExpect(jsonPath("$[1].instante").value("2026-11-02T08:00:00Z"))
                    .andExpect(jsonPath("$[1].estadoAnterior").value("SOLICITADA"))
                    .andExpect(jsonPath("$[1].estadoNovo").value("APROVADA"))
                    .andExpect(jsonPath("$[1].motivo").value("turma confirmada"));
        }

        /** RN-06: ninguém aprova a própria reserva, nem um Responsável. */
        @Test
        void responsavelNaoAprovaAPropriaReserva() throws Exception {
            long id = reservar(responsavel, dia("10:00"), dia("11:00"), laboratorio.getId());

            enviar(responsavel, "/reservas/" + id + "/aprovacao", null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Ninguém aprova a própria reserva."));

            assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, id))
                    .isEqualTo("SOLICITADA");
            assertThat(contar("select count(*) from auditoria_reserva where reserva_id = ?", id)).isOne();
        }

        @Test
        void solicitanteNaoAprovaAPropriaReserva() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), laboratorio.getId());

            enviar(solicitante, "/reservas/" + id + "/aprovacao", null).andExpect(status().isForbidden());

            assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, id))
                    .isEqualTo("SOLICITADA");
        }

        @Test
        void rejeitarLiberaOHorario() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), laboratorio.getId());

            enviar(responsavel, "/reservas/" + id + "/rejeicao", "{\"motivo\":\"laboratório sem técnico\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("REJEITADA"));

            enviar(outroSolicitante, "/reservas", corpoReserva(dia("10:00"), dia("11:00"), laboratorio.getId()))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    class CicloDeUso {

        @Test
        void retiradaEDevolucaoLevamAteConcluidaComAuditoriaDeCadaPasso() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            enviar(responsavel, "/reservas/" + id + "/retirada", null).andExpect(jsonPath("$.estado").value("EM_USO"));
            enviar(responsavel, "/reservas/" + id + "/devolucao", null)
                    .andExpect(jsonPath("$.estado").value("CONCLUIDA"));

            assertThat(jdbc.queryForList("select estado_novo from auditoria_reserva where reserva_id = ? order by id",
                    String.class, id)).containsExactly("APROVADA", "EM_USO", "CONCLUIDA");
            // D5: CONCLUIDA continua ocupando o horário
            enviar(outroSolicitante, "/reservas", corpoReserva(dia("10:00"), dia("11:00"), sala.getId()))
                    .andExpect(status().isConflict());
        }

        /** D3: transição inválida é 409 com tipo próprio, diferente do conflito de horário. */
        @Test
        void concluidaNaoVoltaParaAprovadaEDevolucaoSemRetiradaEhRecusada() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            enviar(responsavel, "/reservas/" + id + "/devolucao", null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:transicao-invalida"));
            enviar(responsavel, "/reservas/" + id + "/retirada", null).andExpect(status().isOk());
            enviar(responsavel, "/reservas/" + id + "/devolucao", null).andExpect(status().isOk());
            enviar(responsavel, "/reservas/" + id + "/aprovacao", null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("A reserva está CONCLUIDA e não pode passar para APROVADA."));

            assertThat(contar("select count(*) from auditoria_reserva where reserva_id = ?", id)).isEqualTo(3);
        }

        @Test
        void naoComparecimentoLiberaOHorario() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            enviar(responsavel, "/reservas/" + id + "/nao-comparecimento", null)
                    .andExpect(jsonPath("$.estado").value("NAO_COMPARECEU"));

            enviar(outroSolicitante, "/reservas", corpoReserva(dia("10:00"), dia("11:00"), sala.getId()))
                    .andExpect(status().isCreated());
        }

        @Test
        void transicaoEmReservaInexistenteDa404() throws Exception {
            enviar(responsavel, "/reservas/42/retirada", null).andExpect(status().isNotFound());
        }
    }

    @Nested
    class Consulta {

        @Test
        void solicitanteSoEnxergaAsProprias() throws Exception {
            long minha = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            long deOutro = reservar(outroSolicitante, dia("12:00"), dia("13:00"), sala.getId());

            obter(solicitante, "/reservas").andExpect(jsonPath("$[*].id", contains((int) minha)));
            obter(solicitante, "/reservas/" + deOutro).andExpect(status().isForbidden());
            obter(solicitante, "/reservas/" + minha).andExpect(status().isOk());
        }

        @Test
        void responsavelEAdministradorEnxergamTodasEFiltramPorEstado() throws Exception {
            long aprovada = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            long solicitada = reservar(outroSolicitante, dia("12:00"), dia("13:00"), laboratorio.getId());

            obter(responsavel, "/reservas").andExpect(jsonPath("$[*].id", contains((int) solicitada, (int) aprovada)));
            obter(administrador, "/reservas?estado=SOLICITADA").andExpect(jsonPath("$[*].id", contains((int) solicitada)));
            obter(solicitante, "/reservas?estado=SOLICITADA").andExpect(jsonPath("$.length()").value(0));
            obter(responsavel, "/reservas/" + solicitada).andExpect(status().isOk());
            obter(responsavel, "/reservas/77").andExpect(status().isNotFound());
        }

        @Test
        void historicoDeReservaInexistenteDa404() throws Exception {
            obter(responsavel, "/reservas/77/historico").andExpect(status().isNotFound());
        }
    }
}
