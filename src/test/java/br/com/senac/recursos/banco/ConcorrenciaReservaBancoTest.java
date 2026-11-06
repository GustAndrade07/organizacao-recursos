package br.com.senac.recursos.banco;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.RepeatedTest;

/**
 * RN-03 e RF-06 no nível do banco: 20 pedidos simultâneos para o mesmo intervalo geram 1 aceito e 19 recusados pela
 * constraint, com a contagem feita no banco (AGENTS.md, seção 5). Repetido 5 vezes, para o recurso e para o professor.
 * Sem {@code Thread.sleep}: a largada é um {@link CountDownLatch}. A tradução em 409 é do serviço (Pedro).
 */
class ConcorrenciaReservaBancoTest extends BancoComDadosTest {

    private static final int PEDIDOS = 20;
    private static final int RODADAS = 5;
    private static final int LIMITE_EM_SEGUNDOS = 60;
    private static final String VIOLACAO_DE_EXCLUSAO = "23P01";

    /** Uma ação que roda numa thread; se o banco a recusar, a exceção sobe e o SQLState dela é o resultado. */
    private interface Pedido {
        void executar(int numero) throws Exception;
    }

    @RepeatedTest(RODADAS)
    void vintePedidosSimultaneosParaOMesmoRecursoGeramUmaReservaEDezenoveRecusas() throws Exception {
        long sala = novoRecurso("SALA", "Sala 101");
        long[] solicitantes = solicitantes();
        String intervalo = periodo("10:00:00", "12:00:00");

        List<String> resultados = dispararSimultaneos(numero -> {
            reservar(solicitantes[numero], "SOLICITADA", intervalo, sala);
        }, numero -> registrarTentativaRecusada(sala, solicitantes[numero], intervalo));

        verificar(resultados);
        assertThat(contarOcupacoes("recurso_id", sala)).isEqualTo(1);
        assertThat(contar("reserva")).isEqualTo(1);
        assertThat(contar("tentativa_recusada")).isEqualTo(PEDIDOS - 1);
    }

    @RepeatedTest(RODADAS)
    void vintePedidosSimultaneosParaAAgendaDoMesmoProfessorGeramUmaReservaEDezenoveRecusas() throws Exception {
        long professor = novoRecurso("PROFESSOR", "Profa. Marta");
        long[] solicitantes = solicitantes();
        long[] salas = new long[PEDIDOS];
        for (int i = 0; i < PEDIDOS; i++) {
            salas[i] = novoRecurso("SALA", "Sala " + (200 + i)); // salas diferentes: só o professor disputa
        }
        String intervalo = periodo("10:00:00", "12:00:00");

        List<String> resultados = dispararSimultaneos(numero -> {
            reservar(solicitantes[numero], "SOLICITADA", intervalo, salas[numero], professor);
        }, numero -> registrarTentativaRecusada(professor, solicitantes[numero], intervalo));

        verificar(resultados);
        assertThat(contarOcupacoes("recurso_id", professor)).isEqualTo(1);
        assertThat(contar("reserva")).isEqualTo(1);
        // a reserva aceita ocupa a sala dela e o professor; as 19 recusadas não deixaram sala nenhuma
        assertThat(contar("reserva_recurso")).isEqualTo(2);
        assertThat(contar("tentativa_recusada")).isEqualTo(PEDIDOS - 1);
    }

    private long[] solicitantes() {
        long[] ids = new long[PEDIDOS];
        for (int i = 0; i < PEDIDOS; i++) {
            ids[i] = novoUsuario("solicitante" + i);
        }
        return ids;
    }

    private long contarOcupacoes(String coluna, long id) {
        // "coluna" é literal fixo dos testes acima
        Long total = jdbc.queryForObject("select count(*) from reserva_recurso where " + coluna + " = ?", Long.class, id);
        return total;
    }

    private void verificar(List<String> resultados) {
        long aceitos = resultados.stream().filter(r -> r == null).count();
        long recusadosPelaConstraint = resultados.stream().filter(VIOLACAO_DE_EXCLUSAO::equals).count();
        List<String> outros = resultados.stream().filter(r -> r != null && !VIOLACAO_DE_EXCLUSAO.equals(r)).toList();

        assertThat(resultados).hasSize(PEDIDOS);
        assertThat(outros).as("nenhum pedido pode terminar de outro jeito (SQLState diferente de 23P01)").isEmpty();
        assertThat(aceitos).as("pedidos aceitos").isEqualTo(1);
        assertThat(recusadosPelaConstraint).as("pedidos recusados pela constraint").isEqualTo(PEDIDOS - 1);
    }

    /**
     * Prepara {@code PEDIDOS} threads, espera todas ficarem prontas e solta todas juntas. Se o pedido for recusado
     * pelo banco, a transação dele já foi desfeita e a tentativa é gravada em transação própria (D2).
     */
    private List<String> dispararSimultaneos(Pedido pedido, Pedido registrarTentativa) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(PEDIDOS);
        CountDownLatch prontas = new CountDownLatch(PEDIDOS);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<String>> futuros = new ArrayList<>();
        try {
            for (int i = 0; i < PEDIDOS; i++) {
                final int numero = i;
                Callable<String> tarefa = () -> {
                    prontas.countDown();
                    largada.await();
                    try {
                        pedido.executar(numero);
                        return null;
                    } catch (Exception erro) {
                        SQLException sql = violacaoDe(() -> {
                            throw erro;
                        });
                        if (VIOLACAO_DE_EXCLUSAO.equals(sql.getSQLState())) {
                            registrarTentativa.executar(numero);
                        }
                        return sql.getSQLState();
                    }
                };
                futuros.add(pool.submit(tarefa));
            }
            assertThat(prontas.await(LIMITE_EM_SEGUNDOS, TimeUnit.SECONDS)).as("threads prontas").isTrue();
            largada.countDown();

            List<String> resultados = new ArrayList<>();
            for (Future<String> futuro : futuros) {
                resultados.add(obter(futuro));
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    private String obter(Future<String> futuro) throws InterruptedException, ExecutionException, TimeoutException {
        return futuro.get(LIMITE_EM_SEGUNDOS, TimeUnit.SECONDS);
    }
}
