package br.com.senac.recursos.banco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Constraints de integridade da V1 que não são a de exclusão. Regra do TDD aplicada ao esquema: toda constraint
 * escrita na migração tem um teste que a recusa. SQLState: 23505 = único, 23503 = chave estrangeira, 23514 = check.
 */
class EsquemaBancoTest extends BancoComDadosTest {

    private long ana;
    private long sala101;

    @BeforeEach
    void cenarioBase() {
        ana = novoUsuario("ana");
        sala101 = novoRecurso("SALA", "Sala 101");
    }

    @Test
    void loginDeUsuarioEUnico() {
        SQLException erro = violacaoDe(() -> novoUsuario("ana"));

        assertThat(erro.getSQLState()).isEqualTo("23505");
    }

    @Test
    void perfilForaDosTresPerfisDoPrdERecusado() {
        SQLException erro = violacaoDe(() -> jdbc.update(
                "insert into usuario (nome, login, senha_hash, perfil) values ('X', 'x', 'hash-de-teste', 'VISITANTE')"));

        assertThat(erro.getSQLState()).isEqualTo("23514");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR"})
    void osTresPerfisDoPrdSaoAceitos(String perfil) {
        assertThatCode(() -> jdbc.update(
                "insert into usuario (nome, login, senha_hash, perfil) values ('X', ?, 'hash-de-teste', ?)",
                "login-" + perfil, perfil)).doesNotThrowAnyException();
    }

    @Test
    void tipoDeRecursoForaDeSalaMaterialOuProfessorERecusado() {
        SQLException erro = violacaoDe(() -> novoRecurso("VEICULO", "Van"));

        assertThat(erro.getSQLState()).isEqualTo("23514");
    }

    @Test
    void capacidadeZeroOuNegativaERecusada() {
        SQLException erro = violacaoDe(
                () -> jdbc.update("insert into recurso (tipo, nome, capacidade) values ('SALA', 'Sala 0', 0)"));

        assertThat(erro.getSQLState()).isEqualTo("23514");
    }

    @Test
    void recursoSemCapacidadeEAceitoPoisMaterialEProfessorNaoTemLotacao() {
        assertThatCode(() -> jdbc.update("insert into recurso (tipo, nome) values ('MATERIAL', 'Projetor')"))
                .doesNotThrowAnyException();
    }

    @Test
    void manutencaoAceitaPeriodoSemiabertoParaRecursoExistente() {
        assertThatCode(() -> jdbc.update(
                "insert into manutencao (recurso_id, periodo) values (?, ?::tstzrange)",
                sala101, periodo("10:00:00", "12:00:00"))).doesNotThrowAnyException();
    }

    @Test
    void manutencaoComPeriodoForaDoPadraoSemiabertoERecusada() {
        SQLException erro = violacaoDe(() -> jdbc.update(
                "insert into manutencao (recurso_id, periodo) values (?, ?::tstzrange)",
                sala101, "[2026-11-10T10:00:00Z,2026-11-10T12:00:00Z]"));

        assertThat(erro.getSQLState()).isEqualTo("23514");
    }

    @Test
    void manutencaoDeRecursoInexistenteERecusada() {
        SQLException erro = violacaoDe(() -> jdbc.update(
                "insert into manutencao (recurso_id, periodo) values (999999, ?::tstzrange)",
                periodo("10:00:00", "12:00:00")));

        assertThat(erro.getSQLState()).isEqualTo("23503");
    }

    @Test
    void tentativaRecusadaComPeriodoPedidoForaDoPadraoSemiabertoERecusada() {
        SQLException erro = violacaoDe(() -> registrarTentativaRecusada(
                sala101, ana, "(2026-11-10T10:00:00Z,2026-11-10T12:00:00Z)"));

        assertThat(erro.getSQLState()).isEqualTo("23514");
    }

    @Test
    void recursoComReservaNaoPodeSerApagadoParaNaoPerderOHistorico() {
        reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);

        SQLException erro = violacaoDe(() -> jdbc.update("delete from recurso where id = ?", sala101));

        assertThat(erro.getSQLState()).isEqualTo("23503");
    }

    @Test
    void usuarioComReservaNaoPodeSerApagado() {
        reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);

        SQLException erro = violacaoDe(() -> jdbc.update("delete from usuario where id = ?", ana));

        assertThat(erro.getSQLState()).isEqualTo("23503");
    }

    @Test
    void auditoriaGuardaReservaUsuarioInstanteEstadoAnteriorEstadoNovoEMotivo() {
        long reserva = reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);

        assertThatCode(() -> jdbc.update(
                "insert into auditoria_reserva (reserva_id, usuario_id, instante, estado_anterior, estado_novo, motivo) "
                        + "values (?, ?, '2026-11-02T09:00:00Z', 'SOLICITADA', 'APROVADA', 'aprovado pelo responsavel')",
                reserva, ana)).doesNotThrowAnyException();

        assertThat(contar("auditoria_reserva")).isEqualTo(1);
    }

    @Test
    void primeiroRegistroDaReservaNaoTemEstadoAnterior() {
        long reserva = reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);

        assertThatCode(() -> jdbc.update(
                "insert into auditoria_reserva (reserva_id, usuario_id, instante, estado_anterior, estado_novo) "
                        + "values (?, ?, '2026-11-01T09:00:00Z', null, 'SOLICITADA')",
                reserva, ana)).doesNotThrowAnyException();
    }

    @Test
    void auditoriaComEstadoForaDaMaquinaDeEstadosERecusada() {
        long reserva = reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);

        SQLException erro = violacaoDe(() -> jdbc.update(
                "insert into auditoria_reserva (reserva_id, usuario_id, instante, estado_anterior, estado_novo) "
                        + "values (?, ?, '2026-11-02T09:00:00Z', 'SOLICITADA', 'TALVEZ')",
                reserva, ana));

        assertThat(erro.getSQLState()).isEqualTo("23514");
    }

    @Test
    void auditoriaDeReservaInexistenteERecusada() {
        SQLException erro = violacaoDe(() -> jdbc.update(
                "insert into auditoria_reserva (reserva_id, usuario_id, instante, estado_anterior, estado_novo) "
                        + "values (999999, ?, '2026-11-02T09:00:00Z', null, 'SOLICITADA')",
                ana));

        assertThat(erro.getSQLState()).isEqualTo("23503");
    }
}
