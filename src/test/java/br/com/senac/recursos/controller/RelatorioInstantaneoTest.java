package br.com.senac.recursos.controller;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.model.Recurso;
import br.com.senac.recursos.repository.RelatorioRepository;
import br.com.senac.recursos.support.ApiIntegracaoTest;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * RF-12: as três partes do relatório leem o mesmo instantâneo do banco ({@code RelatorioService}, REPEATABLE READ).
 *
 * <p>Uma reserva de outro pedido é gravada (e confirmada, em outra thread e outra transação) entre a consulta das horas
 * por recurso e a da carga por professor. Com o instantâneo único, as duas partes continuam concordando: o professor
 * aparece com 1 h nas duas. Sem ele (READ COMMITTED, o padrão do PostgreSQL), a carga já veria as 3 h. Achado na
 * mutação MR4 de {@code docs/testes/mutacao-manual.md}: sem este teste, tirar o isolamento não deixava nenhum teste
 * vermelho.
 */
class RelatorioInstantaneoTest extends ApiIntegracaoTest {

    private static final String JANELA_DIA_10 =
            "/api/relatorios/utilizacao?inicio=2026-11-10T00:00:00Z&fim=2026-11-11T00:00:00Z";

    @MockitoSpyBean
    private RelatorioRepository relatorio;

    @Test
    void reservaGravadaNoMeioDoRelatorioNaoEntraEmSoUmaDasPartes() throws Exception {
        String solicitante = token(usuario("sol", PerfilUsuario.SOLICITANTE));
        String responsavel = token(usuario("resp", PerfilUsuario.RESPONSAVEL));
        Recurso sala = recurso(TipoRecurso.SALA, "Sala C237", false);
        Recurso outraSala = recurso(TipoRecurso.SALA, "Sala D156", false);
        Recurso professor = recurso(TipoRecurso.PROFESSOR, "Prof. Afonso", false);
        reservar(solicitante, dia("10:00"), dia("11:00"), sala.getId(), professor.getId()); // professor: 1 h
        String horasDoProfessor = "$.horasPorRecurso[?(@.recursoId == " + professor.getId() + ")].horas";

        AtomicBoolean gravouNoMeio = new AtomicBoolean();
        doAnswer(consulta -> {
            Object horas = consulta.callRealMethod();
            if (gravouNoMeio.compareAndSet(false, true)) {
                // professor: + 2 h, confirmadas antes de a carga por professor ser lida
                CompletableFuture.runAsync(() -> {
                    try {
                        reservar(solicitante, dia("14:00"), dia("16:00"), outraSala.getId(), professor.getId());
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }).get(30, TimeUnit.SECONDS);
            }
            return horas;
        }).when(relatorio).horasPorRecurso(anyString());

        obter(responsavel, JANELA_DIA_10)
                .andExpect(status().isOk())
                .andExpect(jsonPath(horasDoProfessor, contains(valeHoras(1))))
                .andExpect(jsonPath("$.cargaPorProfessor.length()").value(1))
                .andExpect(jsonPath("$.cargaPorProfessor[0].horas", valeHoras(1)));

        // a reserva do meio foi mesmo gravada: o relatório seguinte já a vê nas duas partes (1 + 2 = 3 h)
        obter(responsavel, JANELA_DIA_10)
                .andExpect(jsonPath(horasDoProfessor, contains(valeHoras(3))))
                .andExpect(jsonPath("$.cargaPorProfessor[0].horas", valeHoras(3)));
    }

    /** Compara pelo valor: o JSONPath devolve {@code Double} ou {@code BigDecimal} conforme os dígitos do número. */
    private static Matcher<Object> valeHoras(int esperado) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(Object valor) {
                return valor instanceof Number && new BigDecimal(valor.toString()).compareTo(BigDecimal.valueOf(esperado)) == 0;
            }

            @Override
            public void describeTo(Description descricao) {
                descricao.appendText(esperado + " h");
            }
        };
    }
}
