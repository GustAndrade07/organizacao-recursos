package br.com.senac.recursos.banco;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * RN-12: a auditoria é gravada na mesma transação da mudança de estado; um rollback não deixa auditoria órfã (nem
 * mudança de estado sem auditoria). Aqui só o comportamento do banco e da transação, com SQL direto; quem decide
 * quando auditar e qual transição é válida é o serviço de reserva (Pedro).
 *
 * <p>As falhas são provocadas pelo próprio banco, uma em cada direção: a mudança de estado que viola a exclusão
 * (23P01) depois de a auditoria já ter sido gravada, e a auditoria recusada (23503) depois de o estado já ter mudado.
 * Reativar uma reserva cancelada num horário ocupado é só um jeito de o banco falhar; não é regra de negócio.
 */
class AuditoriaBancoTest extends BancoComDadosTest {

    private static final OffsetDateTime INSTANTE = OffsetDateTime.parse("2026-11-02T08:00:00Z");

    private long ana;
    private long bruno;
    private long sala101;

    @BeforeEach
    void cenarioBase() {
        ana = novoUsuario("ana");
        bruno = novoUsuario("bruno");
        sala101 = novoRecurso("SALA", "Sala 101");
    }

    @Test
    void commitGravaAMudancaDeEstadoEAAuditoriaJuntas() {
        long reserva = reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);

        tx.executeWithoutResult(status -> {
            mudarEstado(reserva, "CANCELADA");
            gravarAuditoria(reserva, ana, "SOLICITADA", "CANCELADA");
        });

        assertThat(estadoDaReserva(reserva)).isEqualTo("CANCELADA");
        assertThat(estadoDaOcupacao(reserva)).isEqualTo("CANCELADA");
        assertThat(auditoriasDa(reserva)).isEqualTo(1);
    }

    @Test
    void mudancaDeEstadoRecusadaPeloBancoDesfazAAuditoriaJaGravada() {
        reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);
        long canceladaDoBruno = reservar(bruno, "CANCELADA", periodo("11:00:00", "13:00:00"), sala101);

        SQLException erro = violacaoDe(() -> tx.executeWithoutResult(status -> {
            gravarAuditoria(canceladaDoBruno, bruno, "CANCELADA", "SOLICITADA");
            // reativar ocupa de novo o horário [11h, 13h), que a reserva da Ana já ocupa: o banco recusa
            mudarEstado(canceladaDoBruno, "SOLICITADA");
        }));

        assertThat(erro.getSQLState()).isEqualTo("23P01");
        assertThat(auditoriasDa(canceladaDoBruno)).isZero();
        assertThat(contar("auditoria_reserva")).isZero();
        assertThat(estadoDaReserva(canceladaDoBruno)).isEqualTo("CANCELADA");
        assertThat(estadoDaOcupacao(canceladaDoBruno)).isEqualTo("CANCELADA");
    }

    @Test
    void auditoriaRecusadaPeloBancoDesfazAMudancaDeEstadoJaFeita() {
        long reserva = reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);
        long usuarioInexistente = 999_999L;

        SQLException erro = violacaoDe(() -> tx.executeWithoutResult(status -> {
            mudarEstado(reserva, "CANCELADA");
            gravarAuditoria(reserva, usuarioInexistente, "SOLICITADA", "CANCELADA");
        }));

        assertThat(erro.getSQLState()).isEqualTo("23503");
        assertThat(estadoDaReserva(reserva)).isEqualTo("SOLICITADA");
        assertThat(estadoDaOcupacao(reserva)).isEqualTo("SOLICITADA");
        assertThat(contar("auditoria_reserva")).isZero();
    }

    private void mudarEstado(long reservaId, String novoEstado) {
        jdbc.update("update reserva set estado = ? where id = ?", novoEstado, reservaId);
    }

    /** O instante é sempre o fixo do teste: no banco não há {@code now()} (a hora vem do Clock da aplicação). */
    private void gravarAuditoria(long reservaId, long usuarioId, String anterior, String novo) {
        jdbc.update(
                "insert into auditoria_reserva (reserva_id, usuario_id, instante, estado_anterior, estado_novo, motivo) "
                        + "values (?, ?, ?, ?, ?, 'teste de rollback')",
                reservaId, usuarioId, INSTANTE, anterior, novo);
    }

    private String estadoDaReserva(long reservaId) {
        return jdbc.queryForObject("select estado from reserva where id = ?", String.class, reservaId);
    }

    private String estadoDaOcupacao(long reservaId) {
        return jdbc.queryForObject("select estado from reserva_recurso where reserva_id = ?", String.class, reservaId);
    }

    private long auditoriasDa(long reservaId) {
        Long total = jdbc.queryForObject(
                "select count(*) from auditoria_reserva where reserva_id = ?", Long.class, reservaId);
        return total;
    }
}
