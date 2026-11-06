package br.com.senac.recursos.banco;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;

/** Base dos testes que gravam dados: limpa as tabelas antes de cada teste e oferece SQL direto de apoio. */
abstract class BancoComDadosTest extends BancoIntegracaoTest {

    /** Mesmo dia em todos os testes; só as horas mudam. Nenhum teste lê o relógio da máquina. */
    private static final String DIA = "2026-11-10";

    private static final OffsetDateTime CRIADA_EM = OffsetDateTime.parse("2026-11-01T09:00:00Z");

    @BeforeEach
    void limparDados() {
        jdbc.execute("truncate table auditoria_reserva, tentativa_recusada, reserva_recurso, reserva, manutencao, "
                + "recurso, usuario restart identity cascade");
    }

    /** Intervalo semiaberto {@code [início, fim)} (D1) no formato de literal do tstzrange. */
    protected static String periodo(String inicio, String fim) {
        return "[" + DIA + "T" + inicio + "Z," + DIA + "T" + fim + "Z)";
    }

    protected long novoUsuario(String login) {
        Long id = jdbc.queryForObject(
                "insert into usuario (nome, login, senha_hash, perfil) values (?, ?, 'hash-de-teste', 'SOLICITANTE') "
                        + "returning id",
                Long.class, "Usuario " + login, login);
        return id;
    }

    /** {@code tipo}: SALA, MATERIAL ou PROFESSOR (o professor também é recurso, RN-02). */
    protected long novoRecurso(String tipo, String nome) {
        Long id = jdbc.queryForObject(
                "insert into recurso (tipo, nome) values (?, ?) returning id", Long.class, tipo, nome);
        return id;
    }

    /**
     * Cria a reserva e as linhas de ocupação (uma por recurso) numa única transação, como o serviço fará: se uma
     * linha violar a constraint, a transação inteira desfaz.
     */
    protected long reservar(long solicitanteId, String estado, String periodo, long... recursos) {
        Long id = tx.execute(status -> {
            Long reservaId = jdbc.queryForObject(
                    "insert into reserva (solicitante_id, estado, criada_em) values (?, ?, ?) returning id",
                    Long.class, solicitanteId, estado, CRIADA_EM);
            for (long recurso : recursos) {
                jdbc.update(
                        "insert into reserva_recurso (reserva_id, estado, recurso_id, periodo) "
                                + "values (?, ?, ?, ?::tstzrange)",
                        reservaId, estado, recurso, periodo);
            }
            return reservaId;
        });
        return id;
    }

    protected void registrarTentativaRecusada(long recursoId, long solicitanteId, String periodoPedido) {
        jdbc.update(
                "insert into tentativa_recusada (recurso_id, solicitante_id, periodo_pedido, motivo, registrada_em) "
                        + "values (?, ?, ?::tstzrange, 'SOBREPOSICAO', ?)",
                recursoId, solicitanteId, periodoPedido, CRIADA_EM);
    }
}
