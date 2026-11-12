package br.com.senac.recursos.controller;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.model.Recurso;
import br.com.senac.recursos.support.ApiIntegracaoTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * RF-06, RN-03 e RNF-03 de ponta a ponta: 20 pedidos HTTP simultâneos (servidor de verdade, porta aleatória) para o
 * mesmo horário → exatamente 1 × 201 e 19 × 409, nunca 500. A contagem é feita no banco. Repetido 5 vezes (plano,
 * Etapa 5). Fica vermelho se a constraint de exclusão for neutralizada (ver docs/testes/mutacao-manual.md, M1).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcorrenciaReservaApiTest extends ApiIntegracaoTest {

    private static final int PEDIDOS = 20;

    @LocalServerPort
    private int porta;

    @RepeatedTest(5)
    void vintePedidosParaAMesmaSalaGeramUmaReserva() throws Exception {
        Recurso sala = recurso(TipoRecurso.SALA, "Sala C237", false);

        Map<Integer, Long> respostas = dispararAoMesmoTempo(i -> corpoReserva(dia("10:00"), dia("12:00"),
                sala.getId()));

        assertThat(respostas).containsExactlyInAnyOrderEntriesOf(Map.of(201, 1L, 409, (long) PEDIDOS - 1));
        assertThat(contar("select count(*) from reserva_recurso where recurso_id = ?", sala.getId())).isOne();
        assertThat(contar("select count(*) from reserva")).isOne();
        assertThat(contar("select count(*) from tentativa_recusada")).isEqualTo(PEDIDOS - 1);
        assertThat(contar("select count(*) from auditoria_reserva")).isOne();
    }

    /** Agenda do professor (RN-02): cada pedido usa uma sala diferente; o que disputa é só o professor. */
    @RepeatedTest(5)
    void vintePedidosParaOMesmoProfessorEmSalasDiferentesGeramUmaReserva() throws Exception {
        Recurso professor = recurso(TipoRecurso.PROFESSOR, "Prof. Afonso", false);
        List<Long> salas = new ArrayList<>();
        for (int i = 0; i < PEDIDOS; i++) {
            salas.add(recurso(TipoRecurso.SALA, "Sala " + i, false).getId());
        }

        Map<Integer, Long> respostas = dispararAoMesmoTempo(i -> corpoReserva(dia("10:00"), dia("12:00"),
                salas.get(i), professor.getId()));

        assertThat(respostas).containsExactlyInAnyOrderEntriesOf(Map.of(201, 1L, 409, (long) PEDIDOS - 1));
        assertThat(contar("select count(*) from reserva_recurso where recurso_id = ?", professor.getId())).isOne();
        assertThat(contar("select count(*) from reserva")).isOne();
    }

    private Map<Integer, Long> dispararAoMesmoTempo(Function<Integer, String> corpo) throws Exception {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < PEDIDOS; i++) {
            tokens.add(token(usuario("sol" + i, PerfilUsuario.SOLICITANTE)));
        }
        HttpClient cliente = HttpClient.newHttpClient();
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(PEDIDOS);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (int i = 0; i < PEDIDOS; i++) {
                HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/reservas"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + tokens.get(i))
                        .POST(HttpRequest.BodyPublishers.ofString(corpo.apply(i)))
                        .build();
                futuros.add(executor.submit(() -> {
                    largada.await();
                    return cliente.send(requisicao, HttpResponse.BodyHandlers.discarding()).statusCode();
                }));
            }
            largada.countDown();
            List<Integer> status = new ArrayList<>();
            for (Future<Integer> futuro : futuros) {
                status.add(futuro.get(60, TimeUnit.SECONDS));
            }
            return status.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        } finally {
            executor.shutdownNow();
        }
    }
}
