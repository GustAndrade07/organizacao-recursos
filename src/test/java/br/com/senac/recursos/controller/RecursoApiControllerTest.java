package br.com.senac.recursos.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.model.Recurso;
import br.com.senac.recursos.support.ApiIntegracaoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Cadastro e consulta (RF-02), disponibilidade (RF-03) e manutenção (RF-08). */
class RecursoApiControllerTest extends ApiIntegracaoTest {

    private String admin;
    private String solicitante;

    @BeforeEach
    void usuarios() {
        admin = token(usuario("admin", PerfilUsuario.ADMINISTRADOR));
        solicitante = token(usuario("sol", PerfilUsuario.SOLICITANTE));
    }

    @Test
    void administradorCadastraEAlteraSemMudarOTipo() throws Exception {
        enviar(admin, "/api/recursos", "{\"tipo\":\"SALA\",\"nome\":\" Sala C237 \",\"capacidade\":40,\"restrito\":false}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/recursos/1"))
                .andExpect(jsonPath("$.nome").value("Sala C237"));

        substituir(admin, "/api/recursos/1", "{\"nome\":\"Laboratório\",\"capacidade\":20,\"restrito\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SALA"))
                .andExpect(jsonPath("$.restrito").value(true));

        obter(solicitante, "/api/recursos/1").andExpect(status().isOk()).andExpect(jsonPath("$.capacidade").value(20));
    }

    @Test
    void capacidadeZeroOuTipoDesconhecidoDao400() throws Exception {
        enviar(admin, "/api/recursos", "{\"tipo\":\"SALA\",\"nome\":\"S\",\"capacidade\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("capacidade"));
        enviar(admin, "/api/recursos", "{\"tipo\":\"AUDITORIO\",\"nome\":\"S\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:recursos:problema:corpo-ilegivel"));
        obter(admin, "/api/recursos?tipo=AUDITORIO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:recursos:problema:parametro-invalido"));
        obter(admin, "/api/recursos?capacidadeMinima=-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("capacidadeMinima"));
    }

    @Test
    void recursoInexistenteDa404() throws Exception {
        obter(solicitante, "/api/recursos/99")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:recursos:problema:nao-encontrado"));
        substituir(admin, "/api/recursos/99", "{\"nome\":\"X\"}").andExpect(status().isNotFound());
        obter(admin, "/api/recursos/99/manutencoes").andExpect(status().isNotFound());
    }

    @Test
    void filtraPorTipoECapacidadeMinima() throws Exception {
        recurso(TipoRecurso.SALA, "Sala grande", false); // capacidade 30 (ver ApiIntegracaoTest)
        recurso(TipoRecurso.PROFESSOR, "Prof. Ana", false);
        recurso(TipoRecurso.MATERIAL, "Projetor", false);

        obter(solicitante, "/api/recursos?tipo=PROFESSOR").andExpect(jsonPath("$[*].nome", contains("Prof. Ana")));
        obter(solicitante, "/api/recursos?capacidadeMinima=30").andExpect(jsonPath("$[*].nome", contains("Sala grande")));
        obter(solicitante, "/api/recursos?capacidadeMinima=31").andExpect(jsonPath("$.length()").value(0));
        obter(solicitante, "/api/recursos").andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void disponiveisExcluiReservaQueOcupaEManutencaoMasNaoACancelada() throws Exception {
        Recurso ocupada = recurso(TipoRecurso.SALA, "Ocupada", false);
        Recurso emManutencao = recurso(TipoRecurso.SALA, "Em manutenção", false);
        Recurso cancelada = recurso(TipoRecurso.SALA, "Reserva cancelada", false);
        recurso(TipoRecurso.SALA, "Livre", false);
        reservar(solicitante, dia("10:00"), dia("11:00"), ocupada.getId());
        long reservaCancelada = reservar(solicitante, dia("10:00"), dia("11:00"), cancelada.getId());
        enviar(solicitante, "/api/reservas/" + reservaCancelada + "/cancelamento", null).andExpect(status().isOk());
        enviar(admin, "/api/recursos/" + emManutencao.getId() + "/manutencoes", corpoPeriodo(dia("09:00"), dia("12:00")))
                .andExpect(status().isCreated());

        obter(solicitante, "/api/recursos/disponiveis?inicio=" + dia("10:30") + "&fim=" + dia("10:45") + "&tipo=SALA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", contains("Reserva cancelada", "Livre")));
        // [início, fim): a partir das 11:00 a sala ocupada volta a aparecer
        obter(solicitante, "/api/recursos/disponiveis?inicio=" + dia("11:00") + "&fim=" + dia("11:30")
                        + "&capacidadeMinima=1")
                .andExpect(jsonPath("$[*].nome", contains("Ocupada", "Reserva cancelada", "Livre")));
    }

    @Test
    void disponiveisSemPeriodoOuComPeriodoInvertidoDa400() throws Exception {
        obter(solicitante, "/api/recursos/disponiveis?inicio=" + dia("10:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:recursos:problema:parametro-ausente"));
        obter(solicitante, "/api/recursos/disponiveis?inicio=" + dia("11:00") + "&fim=" + dia("10:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O término precisa ser depois do início."));
    }

    @Test
    void manutencaoECadastradaEListadaComPeriodoInvalidoRecusado() throws Exception {
        Recurso sala = recurso(TipoRecurso.SALA, "Sala", false);
        String url = "/api/recursos/" + sala.getId() + "/manutencoes";

        enviar(admin, url, "{\"inicio\":\"2026-11-10T07:00:00-03:00\",\"fim\":\"2026-11-10T09:00:00-03:00\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inicio").value(dia("10:00")))
                .andExpect(jsonPath("$.fim").value(dia("12:00")));
        enviar(admin, url, corpoPeriodo(dia("12:00"), dia("12:00"))).andExpect(status().isBadRequest());
        enviar(admin, url, "{\"inicio\":null}").andExpect(status().isBadRequest());

        obter(solicitante, url).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
}
