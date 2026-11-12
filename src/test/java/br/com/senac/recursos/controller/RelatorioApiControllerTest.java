package br.com.senac.recursos.controller;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.model.Recurso;
import br.com.senac.recursos.support.ApiIntegracaoTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * RF-12 e RN-13 pela API. Os valores esperados são calculados à mão (conta no comentário), nunca copiados da saída.
 * Premissas das consultas pendentes de confirmação do grupo (D8 do plano): contam os estados da D5, conflitos pelo
 * instante em que foram registrados, recurso sem uso não aparece, professor = recurso do tipo PROFESSOR.
 */
class RelatorioApiControllerTest extends ApiIntegracaoTest {

    private static final String JANELA_DIA_10 =
            "/api/relatorios/utilizacao?inicio=2026-11-10T00:00:00Z&fim=2026-11-11T00:00:00Z";

    private String solicitante;
    private String responsavel;
    private String administrador;

    @BeforeEach
    void usuarios() {
        solicitante = token(usuario("sol", PerfilUsuario.SOLICITANTE));
        responsavel = token(usuario("resp", PerfilUsuario.RESPONSAVEL));
        administrador = token(usuario("admin", PerfilUsuario.ADMINISTRADOR));
    }

    @Test
    void valoresCalculadosAMaoComBordaCanceladaEConflito() throws Exception {
        Recurso sala = recurso(TipoRecurso.SALA, "Sala C237", false);           // id 1
        Recurso professor = recurso(TipoRecurso.PROFESSOR, "Prof. Afonso", false); // id 2
        Recurso outraSala = recurso(TipoRecurso.SALA, "Sala D156", false);      // id 3
        Recurso semUso = recurso(TipoRecurso.MATERIAL, "Projetor", false);      // id 4, não aparece

        reservar(solicitante, dia("10:00"), dia("12:00"), sala.getId());                          // sala: 2 h
        reservar(solicitante, "2026-11-10T23:00:00Z", "2026-11-11T01:00:00Z", sala.getId());      // sala: 1 h (só até 00:00)
        long cancelada = reservar(solicitante, dia("14:00"), dia("15:00"), sala.getId());         // não conta
        enviar(solicitante, "/api/reservas/" + cancelada + "/cancelamento", null).andExpect(status().isOk());
        reservar(solicitante, dia("14:00"), dia("15:30"), outraSala.getId(), professor.getId()); // 1,5 h cada

        // conflito registrado às 08:00 do dia 10 (dentro da janela) e outro em 01/11 (fora)
        relogio.ajustar(Instant.parse(dia("08:00")));
        enviar(solicitante, "/api/reservas", corpoReserva(dia("11:00"), dia("13:00"), sala.getId()))
                .andExpect(status().isConflict());
        relogio.ajustar(AGORA);
        enviar(solicitante, "/api/reservas", corpoReserva(dia("11:00"), dia("13:00"), sala.getId()))
                .andExpect(status().isConflict());

        // sala = 2 + 1 = 3 h; professor = 1,5 h; outra sala = 1,5 h; projetor sem linha; carga do professor = 1,5 h
        obter(responsavel, JANELA_DIA_10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value("2026-11-10T00:00:00Z"))
                .andExpect(jsonPath("$.horasPorRecurso.length()").value(3))
                .andExpect(jsonPath("$.horasPorRecurso[0].recursoId").value(sala.getId()))
                .andExpect(jsonPath("$.horasPorRecurso[0].horas", closeTo(3.0, 1e-9)))
                .andExpect(jsonPath("$.horasPorRecurso[1].recursoId").value(professor.getId()))
                .andExpect(jsonPath("$.horasPorRecurso[1].tipo").value("PROFESSOR"))
                .andExpect(jsonPath("$.horasPorRecurso[1].horas", closeTo(1.5, 1e-9)))
                .andExpect(jsonPath("$.horasPorRecurso[2].recursoId").value(outraSala.getId()))
                .andExpect(jsonPath("$.horasPorRecurso[2].horas", closeTo(1.5, 1e-9)))
                .andExpect(jsonPath("$.horasPorRecurso[?(@.recursoId == " + semUso.getId() + ")]").isEmpty())
                .andExpect(jsonPath("$.cargaPorProfessor.length()").value(1))
                .andExpect(jsonPath("$.cargaPorProfessor[0].professorId").value(professor.getId()))
                .andExpect(jsonPath("$.cargaPorProfessor[0].horas", closeTo(1.5, 1e-9)))
                .andExpect(jsonPath("$.conflitosEvitados").value(1));
    }

    @Test
    void periodoVazioDevolveListasVaziasEZeroConflitos() throws Exception {
        Recurso sala = recurso(TipoRecurso.SALA, "Sala", false);
        reservar(solicitante, dia("10:00"), dia("12:00"), sala.getId());

        obter(administrador, "/api/relatorios/utilizacao?inicio=2026-12-01T00:00:00Z&fim=2026-12-02T00:00:00Z")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasPorRecurso.length()").value(0))
                .andExpect(jsonPath("$.cargaPorProfessor.length()").value(0))
                .andExpect(jsonPath("$.conflitosEvitados").value(0));
    }

    @Test
    void semTokenDa401ESolicitanteDa403() throws Exception {
        obter(null, JANELA_DIA_10).andExpect(status().isUnauthorized());
        obter(solicitante, JANELA_DIA_10).andExpect(status().isForbidden());
    }

    @Test
    void janelaAusenteOuInvertidaDa400() throws Exception {
        obter(responsavel, "/api/relatorios/utilizacao?inicio=2026-11-10T00:00:00Z").andExpect(status().isBadRequest());
        obter(responsavel, "/api/relatorios/utilizacao?inicio=2026-11-11T00:00:00Z&fim=2026-11-10T00:00:00Z")
                .andExpect(status().isBadRequest());
        obter(responsavel, "/api/relatorios/utilizacao?inicio=ontem&fim=hoje")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:recursos:problema:parametro-invalido"));
    }
}
