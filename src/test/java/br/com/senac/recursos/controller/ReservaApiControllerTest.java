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
import java.sql.Timestamp;
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
class ReservaApiControllerTest extends ApiIntegracaoTest {

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
            enviar(solicitante, "/api/reservas", corpoReserva(dia("10:00"), dia("11:00"), professor.getId(), sala.getId()))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/reservas/1"))
                    .andExpect(jsonPath("$.estado").value("APROVADA"))
                    .andExpect(jsonPath("$.criadaEm").value("2026-11-01T12:00:00Z"))
                    .andExpect(jsonPath("$.recursos[*].id", contains(sala.getId().intValue(),
                            professor.getId().intValue())));

            // D6: uma linha por recurso, em ordem de recurso_id, com o estado copiado
            assertThat(jdbc.queryForList("select recurso_id from reserva_recurso order by recurso_id", Long.class))
                    .containsExactly(sala.getId(), professor.getId());
            assertThat(contar("select count(*) from auditoria_reserva where estado_anterior is null "
                    + "and estado_novo = 'APROVADA' and instante = ?", Timestamp.from(AGORA))).isOne();
        }

        @Test
        void recursoRestritoNasceSolicitada() throws Exception {
            enviar(solicitante, "/api/reservas", corpoReserva(dia("10:00"), dia("11:00"), laboratorio.getId()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estado").value("SOLICITADA"));
        }

        @Test
        void terminoAntesDoInicioDa400ENaoGrava() throws Exception {
            enviar(solicitante, "/api/reservas", corpoReserva(dia("11:00"), dia("10:00"), sala.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("O término precisa ser depois do início."));
            assertThat(reservasGravadas()).isZero();
        }

        @Test
        void listaVaziaRecursoRepetidoEInexistenteSaoRecusados() throws Exception {
            enviar(solicitante, "/api/reservas", "{\"recursoIds\":[],\"inicio\":\"" + dia("10:00") + "\",\"fim\":\""
                    + dia("11:00") + "\"}").andExpect(status().isBadRequest());
            enviar(solicitante, "/api/reservas", corpoReserva(dia("10:00"), dia("11:00"), sala.getId(), sala.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("O mesmo recurso foi informado mais de uma vez."));
            enviar(solicitante, "/api/reservas", corpoReserva(dia("10:00"), dia("11:00"), sala.getId(), 999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Recurso 999 não encontrado(a)."));
            assertThat(reservasGravadas()).isZero();
        }

        @Test
        void dataSemDeslocamentoDa400Legivel() throws Exception {
            enviar(solicitante, "/api/reservas", "{\"recursoIds\":[1],\"inicio\":\"10/11/2026 10:00\",\"fim\":\"x\"}")
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

            enviar(solicitante, "/api/reservas", corpoReserva(dia(inicio), dia(fim), sala.getId()))
                    .andExpect(status().is(esperado));
        }

        @Test
        void sobreposicaoDa409GravaTentativaENaoGravaReserva() throws Exception {
            reservar(outroSolicitante, dia("10:00"), dia("12:00"), sala.getId());

            enviar(solicitante, "/api/reservas", corpoReserva(dia("11:00"), dia("13:00"), sala.getId()))
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

            enviar(solicitante, "/api/reservas", corpoReserva(dia("11:00"), dia("12:00"), outraSala.getId(),
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
            enviar(outroSolicitante, "/api/reservas/" + primeira + "/cancelamento", "{\"motivo\":\"aula suspensa\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CANCELADA"));

            enviar(solicitante, "/api/reservas", corpoReserva(dia("10:00"), dia("12:00"), sala.getId()))
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
            enviar(administrador, "/api/recursos/" + sala.getId() + "/manutencoes", corpoPeriodo(dia("10:00"), dia("12:00")))
                    .andExpect(status().isCreated());

            enviar(solicitante, "/api/reservas", corpoReserva(dia(inicio), dia(fim), sala.getId()))
                    .andExpect(status().is(esperado));
        }

        @Test
        void manutencaoDa409DeTipoProprioSemTentativaESemReserva() throws Exception {
            enviar(administrador, "/api/recursos/" + sala.getId() + "/manutencoes", corpoPeriodo(dia("10:00"), dia("12:00")))
                    .andExpect(status().isCreated());

            enviar(solicitante, "/api/reservas", corpoReserva(dia("10:30"), dia("11:30"), professor.getId(), sala.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:recurso-em-manutencao"))
                    .andExpect(jsonPath("$.recursos", contains(sala.getId().intValue())));

            assertThat(reservasGravadas()).isZero();
            assertThat(contar("select count(*) from tentativa_recusada")).isZero();
        }

        /**
         * D11 (decidida em 12/11/2026, "fica como está no código"): a manutenção é gravada mesmo com reserva no período;
         * a reserva que já existia continua como estava (estado e histórico) e só as novas são recusadas (RN-04).
         */
        @Test
        void manutencaoSobreReservaExistenteMantemAReservaEBloqueiaSoAsNovas() throws Exception {
            long existente = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            enviar(administrador, "/api/recursos/" + sala.getId() + "/manutencoes", corpoPeriodo(dia("09:00"), dia("12:00")))
                    .andExpect(status().isCreated());

            obter(solicitante, "/api/reservas/" + existente)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("APROVADA"))
                    .andExpect(jsonPath("$.inicio").value(dia("10:00")))
                    .andExpect(jsonPath("$.fim").value(dia("11:00")));
            assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, existente))
                    .isEqualTo("APROVADA");
            assertThat(contar("select count(*) from auditoria_reserva where reserva_id = ?", existente)).isOne();

            enviar(outroSolicitante, "/api/reservas", corpoReserva(dia("11:00"), dia("12:00"), sala.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:recurso-em-manutencao"));
            assertThat(reservasGravadas()).isOne();
        }
    }

    @Nested
    class Alterar {

        @Test
        void donoMudaOPeriodoEAAlteracaoEntraNoHistorico() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId(), professor.getId());

            substituir(solicitante, "/api/reservas/" + id + "/periodo", corpoPeriodo(dia("14:00"), dia("15:00")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.inicio").value(dia("14:00")))
                    .andExpect(jsonPath("$.estado").value("APROVADA"));

            assertThat(contar("select count(*) from reserva_recurso where lower(periodo) = '2026-11-10T14:00:00Z'"))
                    .isEqualTo(2);
            obter(responsavel, "/api/reservas/" + id + "/historico")
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[1].estadoAnterior").value("APROVADA"))
                    .andExpect(jsonPath("$[1].estadoNovo").value("APROVADA"));
        }

        @Test
        void mudarParaHorarioOcupadoDa409MantemOAntigoEGravaTentativa() throws Exception {
            reservar(outroSolicitante, dia("14:00"), dia("15:00"), sala.getId());
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            substituir(solicitante, "/api/reservas/" + id + "/periodo", corpoPeriodo(dia("14:30"), dia("15:30")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:conflito-de-horario"));

            obter(solicitante, "/api/reservas/" + id).andExpect(jsonPath("$.inicio").value(dia("10:00")));
            assertThat(contar("select count(*) from tentativa_recusada")).isOne();
        }

        @Test
        void encostarNoProprioHorarioNaoConflitaConsigoMesma() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            substituir(solicitante, "/api/reservas/" + id + "/periodo", corpoPeriodo(dia("10:30"), dia("11:30")))
                    .andExpect(status().isOk());
        }

        @Test
        void outroSolicitanteRecebe403EAReservaFicaIntacta() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            substituir(outroSolicitante, "/api/reservas/" + id + "/periodo", corpoPeriodo(dia("14:00"), dia("15:00")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:acesso-negado"));

            obter(solicitante, "/api/reservas/" + id).andExpect(jsonPath("$.inicio").value(dia("10:00")));
        }

        @Test
        void reservaJaIniciadaNaoMudaDePeriodo() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            relogio.ajustar(Instant.parse(dia("10:00")));

            substituir(solicitante, "/api/reservas/" + id + "/periodo", corpoPeriodo(dia("14:00"), dia("15:00")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:reserva-ja-iniciada"));
        }

        @Test
        void reservaCanceladaNaoMudaDePeriodo() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            enviar(solicitante, "/api/reservas/" + id + "/cancelamento", null).andExpect(status().isOk());

            substituir(solicitante, "/api/reservas/" + id + "/periodo", corpoPeriodo(dia("14:00"), dia("15:00")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:transicao-invalida"));
        }

        @Test
        void periodoParaManutencaoDa409() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            enviar(administrador, "/api/recursos/" + sala.getId() + "/manutencoes", corpoPeriodo(dia("14:00"), dia("16:00")))
                    .andExpect(status().isCreated());

            substituir(solicitante, "/api/reservas/" + id + "/periodo", corpoPeriodo(dia("15:00"), dia("17:00")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:recurso-em-manutencao"));
        }

        @Test
        void reservaInexistenteDa404() throws Exception {
            substituir(solicitante, "/api/reservas/99/periodo", corpoPeriodo(dia("14:00"), dia("15:00")))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Cancelar {

        @Test
        void outroSolicitanteRecebe403EAReservaContinuaAprovada() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            enviar(outroSolicitante, "/api/reservas/" + id + "/cancelamento", null).andExpect(status().isForbidden());

            assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, id))
                    .isEqualTo("APROVADA");
        }

        /** RN-07: a partir do instante de início a reserva não pode mais ser cancelada. */
        @Test
        void reservaIniciadaNaoPodeSerCancelada() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            long mesmoHorario = reservar(solicitante, dia("10:00"), dia("11:00"), professor.getId());

            relogio.ajustar(Instant.parse("2026-11-10T09:59:59Z"));
            enviar(solicitante, "/api/reservas/" + mesmoHorario + "/cancelamento", null).andExpect(status().isOk());

            relogio.ajustar(Instant.parse(dia("10:00")));
            enviar(solicitante, "/api/reservas/" + id + "/cancelamento", null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:reserva-ja-iniciada"));
        }

        @Test
        void motivoLongoDemaisDa400() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            enviar(solicitante, "/api/reservas/" + id + "/cancelamento", "{\"motivo\":\"" + "x".repeat(501) + "\"}")
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Aprovacao {

        @Test
        void responsavelAprovaRestritoEHistoricoGuardaQuemEQuando() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), laboratorio.getId());
            relogio.ajustar(Instant.parse("2026-11-02T08:00:00Z"));

            enviar(responsavel, "/api/reservas/" + id + "/aprovacao", "{\"motivo\":\" turma confirmada \"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("APROVADA"));

            obter(administrador, "/api/reservas/" + id + "/historico")
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

            enviar(responsavel, "/api/reservas/" + id + "/aprovacao", null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Ninguém aprova a própria reserva."));

            assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, id))
                    .isEqualTo("SOLICITADA");
            assertThat(contar("select count(*) from auditoria_reserva where reserva_id = ?", id)).isOne();
        }

        @Test
        void solicitanteNaoAprovaAPropriaReserva() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), laboratorio.getId());

            enviar(solicitante, "/api/reservas/" + id + "/aprovacao", null).andExpect(status().isForbidden());

            assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, id))
                    .isEqualTo("SOLICITADA");
        }

        @Test
        void rejeitarLiberaOHorario() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), laboratorio.getId());

            enviar(responsavel, "/api/reservas/" + id + "/rejeicao", "{\"motivo\":\"laboratório sem técnico\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("REJEITADA"));

            enviar(outroSolicitante, "/api/reservas", corpoReserva(dia("10:00"), dia("11:00"), laboratorio.getId()))
                    .andExpect(status().isCreated());
        }

        /**
         * D10 (c), decidida em 12/11/2026: a RN-06 só proíbe aprovar a própria reserva; rejeitar a própria é permitido
         * ao Responsável (equivale a desistir), e a rejeição entra no histórico com ele como autor.
         */
        @Test
        void responsavelPodeRejeitarAPropriaReserva() throws Exception {
            long id = reservar(responsavel, dia("10:00"), dia("11:00"), laboratorio.getId());

            enviar(responsavel, "/api/reservas/" + id + "/rejeicao", "{\"motivo\":\"não vou mais usar\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("REJEITADA"));

            assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, id))
                    .isEqualTo("REJEITADA");
            obter(administrador, "/api/reservas/" + id + "/historico")
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[1].usuarioLogin").value("resp"))
                    .andExpect(jsonPath("$[1].estadoAnterior").value("SOLICITADA"))
                    .andExpect(jsonPath("$[1].estadoNovo").value("REJEITADA"))
                    .andExpect(jsonPath("$[1].motivo").value("não vou mais usar"));
        }
    }

    /**
     * D10 (d), decidida em 12/11/2026: não há limite de horário para registrar retirada, devolução e "não compareceu".
     * O relógio é fixo (regra 14 do AGENTS.md); a reserva é de 10/11/2026, das 10:00 às 11:00 (UTC).
     */
    @Nested
    class SemLimiteDeHorarioNoCicloDeUso {

        @ParameterizedTest(name = "retirada em {0}")
        @CsvSource({
            "2026-11-01T12:00:00Z", // nove dias antes do início
            "2026-11-10T10:30:00Z", // durante
            "2026-11-10T18:00:00Z"  // depois do fim
        })
        void retiradaEDevolucaoSaoAceitasEmQualquerHorario(String instante) throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            relogio.ajustar(Instant.parse(instante));

            enviar(responsavel, "/api/reservas/" + id + "/retirada", null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("EM_USO"));
            enviar(responsavel, "/api/reservas/" + id + "/devolucao", null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CONCLUIDA"));

            assertThat(jdbc.queryForList("select instante from auditoria_reserva where reserva_id = ? order by id",
                    Timestamp.class, id).stream().skip(1).map(Timestamp::toInstant))
                    .containsExactly(Instant.parse(instante), Instant.parse(instante));
        }

        @ParameterizedTest(name = "não compareceu em {0}")
        @CsvSource({
            "2026-11-01T12:00:00Z", // antes do início
            "2026-11-10T10:30:00Z", // durante
            "2026-11-12T08:00:00Z"  // dois dias depois do fim
        })
        void naoComparecimentoEhAceitoEmQualquerHorario(String instante) throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            relogio.ajustar(Instant.parse(instante));

            enviar(responsavel, "/api/reservas/" + id + "/nao-comparecimento", null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("NAO_COMPARECEU"));

            assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, id))
                    .isEqualTo("NAO_COMPARECEU");
        }
    }

    @Nested
    class CicloDeUso {

        @Test
        void retiradaEDevolucaoLevamAteConcluidaComAuditoriaDeCadaPasso() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            enviar(responsavel, "/api/reservas/" + id + "/retirada", null).andExpect(jsonPath("$.estado").value("EM_USO"));
            enviar(responsavel, "/api/reservas/" + id + "/devolucao", null)
                    .andExpect(jsonPath("$.estado").value("CONCLUIDA"));

            assertThat(jdbc.queryForList("select estado_novo from auditoria_reserva where reserva_id = ? order by id",
                    String.class, id)).containsExactly("APROVADA", "EM_USO", "CONCLUIDA");
            // D5: CONCLUIDA continua ocupando o horário
            enviar(outroSolicitante, "/api/reservas", corpoReserva(dia("10:00"), dia("11:00"), sala.getId()))
                    .andExpect(status().isConflict());
        }

        /** D3: transição inválida é 409 com tipo próprio, diferente do conflito de horário. */
        @Test
        void concluidaNaoVoltaParaAprovadaEDevolucaoSemRetiradaEhRecusada() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            enviar(responsavel, "/api/reservas/" + id + "/devolucao", null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("urn:recursos:problema:transicao-invalida"));
            enviar(responsavel, "/api/reservas/" + id + "/retirada", null).andExpect(status().isOk());
            enviar(responsavel, "/api/reservas/" + id + "/devolucao", null).andExpect(status().isOk());
            enviar(responsavel, "/api/reservas/" + id + "/aprovacao", null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("A reserva está CONCLUIDA e não pode passar para APROVADA."));

            assertThat(contar("select count(*) from auditoria_reserva where reserva_id = ?", id)).isEqualTo(3);
        }

        @Test
        void naoComparecimentoLiberaOHorario() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());

            enviar(responsavel, "/api/reservas/" + id + "/nao-comparecimento", null)
                    .andExpect(jsonPath("$.estado").value("NAO_COMPARECEU"));

            enviar(outroSolicitante, "/api/reservas", corpoReserva(dia("10:00"), dia("11:00"), sala.getId()))
                    .andExpect(status().isCreated());
        }

        @Test
        void transicaoEmReservaInexistenteDa404() throws Exception {
            enviar(responsavel, "/api/reservas/42/retirada", null).andExpect(status().isNotFound());
        }
    }

    /**
     * RN-08 e RN-09: toda mudança de estado gera um registro com quem, quando (pelo {@code Clock}), estado anterior,
     * estado novo e motivo. Criar, aprovar, alterar, retirar e devolver já são conferidos nas classes acima; aqui ficam
     * cancelar, rejeitar e "não compareceu", e a transição recusada, que não pode deixar registro.
     */
    @Nested
    class AuditoriaDeCadaTransicao {

        private long registrosDe(long reserva) {
            return contar("select count(*) from auditoria_reserva where reserva_id = ?", reserva);
        }

        @Test
        void cancelamentoEntraNoHistoricoComQuemQuandoEMotivo() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            relogio.ajustar(Instant.parse("2026-11-05T09:00:00Z"));

            enviar(solicitante, "/api/reservas/" + id + "/cancelamento", "{\"motivo\":\" aula suspensa \"}")
                    .andExpect(status().isOk());

            obter(responsavel, "/api/reservas/" + id + "/historico")
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[1].usuarioLogin").value("sol"))
                    .andExpect(jsonPath("$[1].instante").value("2026-11-05T09:00:00Z"))
                    .andExpect(jsonPath("$[1].estadoAnterior").value("APROVADA"))
                    .andExpect(jsonPath("$[1].estadoNovo").value("CANCELADA"))
                    .andExpect(jsonPath("$[1].motivo").value("aula suspensa"));
            assertThat(registrosDe(id)).isEqualTo(2);
        }

        @Test
        void naoComparecimentoEntraNoHistoricoComQuemQuandoEMotivo() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            relogio.ajustar(Instant.parse("2026-11-10T10:20:00Z"));

            enviar(responsavel, "/api/reservas/" + id + "/nao-comparecimento", "{\"motivo\":\"ninguém veio buscar\"}")
                    .andExpect(status().isOk());

            obter(administrador, "/api/reservas/" + id + "/historico")
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[1].usuarioLogin").value("resp"))
                    .andExpect(jsonPath("$[1].instante").value("2026-11-10T10:20:00Z"))
                    .andExpect(jsonPath("$[1].estadoAnterior").value("APROVADA"))
                    .andExpect(jsonPath("$[1].estadoNovo").value("NAO_COMPARECEU"))
                    .andExpect(jsonPath("$[1].motivo").value("ninguém veio buscar"));
            assertThat(registrosDe(id)).isEqualTo(2);
        }

        @Test
        void rejeicaoEntraNoHistoricoComQuemEMotivo() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), laboratorio.getId());

            enviar(responsavel, "/api/reservas/" + id + "/rejeicao", "{\"motivo\":\"laboratório sem técnico\"}")
                    .andExpect(status().isOk());

            obter(responsavel, "/api/reservas/" + id + "/historico")
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[1].usuarioLogin").value("resp"))
                    .andExpect(jsonPath("$[1].instante").value("2026-11-01T12:00:00Z"))
                    .andExpect(jsonPath("$[1].estadoAnterior").value("SOLICITADA"))
                    .andExpect(jsonPath("$[1].estadoNovo").value("REJEITADA"))
                    .andExpect(jsonPath("$[1].motivo").value("laboratório sem técnico"));
        }

        /** Transição recusada (D3) não muda o estado, então não pode deixar registro (contagem no banco). */
        @Test
        void transicaoRecusadaNaoGravaAuditoria() throws Exception {
            long id = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            enviar(solicitante, "/api/reservas/" + id + "/cancelamento", null).andExpect(status().isOk());

            enviar(solicitante, "/api/reservas/" + id + "/cancelamento", null).andExpect(status().isConflict());
            enviar(responsavel, "/api/reservas/" + id + "/nao-comparecimento", null).andExpect(status().isConflict());

            assertThat(registrosDe(id)).isEqualTo(2);
        }
    }

    @Nested
    class Consulta {

        @Test
        void solicitanteSoEnxergaAsProprias() throws Exception {
            long minha = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            long deOutro = reservar(outroSolicitante, dia("12:00"), dia("13:00"), sala.getId());

            obter(solicitante, "/api/reservas").andExpect(jsonPath("$[*].id", contains((int) minha)));
            obter(solicitante, "/api/reservas/" + deOutro).andExpect(status().isForbidden());
            obter(solicitante, "/api/reservas/" + minha).andExpect(status().isOk());
        }

        @Test
        void responsavelEAdministradorEnxergamTodasEFiltramPorEstado() throws Exception {
            long aprovada = reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId());
            long solicitada = reservar(outroSolicitante, dia("12:00"), dia("13:00"), laboratorio.getId());

            obter(responsavel, "/api/reservas").andExpect(jsonPath("$[*].id", contains((int) solicitada, (int) aprovada)));
            obter(administrador, "/api/reservas?estado=SOLICITADA").andExpect(jsonPath("$[*].id", contains((int) solicitada)));
            obter(solicitante, "/api/reservas?estado=SOLICITADA").andExpect(jsonPath("$.length()").value(0));
            obter(responsavel, "/api/reservas/" + solicitada).andExpect(status().isOk());
            obter(responsavel, "/api/reservas/77").andExpect(status().isNotFound());
        }

        @Test
        void historicoDeReservaInexistenteDa404() throws Exception {
            obter(responsavel, "/api/reservas/77/historico").andExpect(status().isNotFound());
        }
    }
}
