package br.com.senac.recursos.banco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.sql.SQLException;
import java.util.List;
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

    // ---------------------------------------------------------------- login é e-mail (V2, DEF-010)

    private SQLException inserirLogin(String login) {
        return violacaoDe(() -> jdbc.update(
                "insert into usuario (nome, login, senha_hash, perfil) values ('Ana Souza', ?, 'hash-de-teste', 'SOLICITANTE')",
                login));
    }

    @Test
    void emailValidoEhAceitoComoLogin() {
        assertThatCode(() -> jdbc.update(
                "insert into usuario (nome, login, senha_hash, perfil) values ('Ana Souza', ?, 'hash-de-teste', 'SOLICITANTE')",
                "ana.souza+reservas@sub.exemplo.com.br")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ana", "admin", "ana.souza", "ana@", "@exemplo.com", "ana@exemplo", "ana@@exemplo.com", "ana@exemplo..com",
        "ana @exemplo.com", "ana@exemplo.com ", " ana@exemplo.com", "ana@-exemplo.com", "ana@exemplo.com.",
        ".ana@exemplo.com", "ana..x@exemplo.com", "ANA@EXEMPLO.COM", "Ana@exemplo.com", "ana@exemplo.com\n"
    })
    void loginQueNaoEEmailEmMinusculasEhRecusadoPeloBanco(String login) {
        SQLException erro = inserirLogin(login);

        assertThat(erro.getSQLState()).as("'" + login + "'").isEqualTo("23514");
        assertThat(erro.getMessage()).contains("usuario_login_e_email");
    }

    @Test
    void emailComMaisDe254CaracteresOuParteLocalComMaisDe64ESaoRecusados() {
        String dominio189 = "b".repeat(63) + "." + "b".repeat(63) + "." + "b".repeat(57) + ".com";
        String com254 = "a".repeat(64) + "@" + dominio189;

        assertThatCode(() -> jdbc.update(
                "insert into usuario (nome, login, senha_hash, perfil) values ('Ana Souza', ?, 'hash-de-teste', 'SOLICITANTE')",
                com254)).doesNotThrowAnyException();
        assertThat(inserirLogin(com254 + "m").getSQLState()).isEqualTo("23514");
        assertThat(inserirLogin("a".repeat(65) + "@exemplo.com").getSQLState()).isEqualTo("23514");
    }

    @Test
    void emailJaCadastradoNaoRepeteEOIndiceUnicoEmLowerBarraOutraCaixa() {
        // "ana@exemplo.com" já existe (cenarioBase). A mesma conta com outra caixa já cai no CHECK; para provar o
        // índice em lower(login) sozinho, uma transação tira o CHECK, insere "ANA@exemplo.com" e é desfeita no erro
        // (DDL é transacional no PostgreSQL).
        SQLException erro = violacaoDe(() -> tx.executeWithoutResult(status -> {
            jdbc.execute("alter table usuario drop constraint usuario_login_e_email");
            jdbc.update("insert into usuario (nome, login, senha_hash, perfil) values "
                    + "('Ana Souza', 'ANA@exemplo.com', 'hash-de-teste', 'SOLICITANTE')");
        }));

        assertThat(erro.getSQLState()).isEqualTo("23505");
        assertThat(erro.getMessage()).contains("usuario_login_minusculo_uq");
        // a transação foi desfeita: o CHECK continua no banco
        assertThat(inserirLogin("sem-arroba").getSQLState()).isEqualTo("23514");
        assertThat(inserirLogin("ana@exemplo.com").getSQLState()).isEqualTo("23505");
    }

    /** O formato do Java ({@code Emails.valido}) e o CHECK da V2 dão a mesma resposta para cada amostra. */
    @Test
    void asRegrasDeFormatoDoJavaEDoBancoConcordamParaCadaAmostra() {
        List<String> amostras = List.of("bia@exemplo.com", "bia.souza+x@sub.exemplo.com.br", "a_b-c%d@ex-emplo.com",
                "bia", "bia@exemplo", "bia@exemplo..com", ".bia@exemplo.com", "bia..x@exemplo.com", "bia@-exemplo.com",
                "bia@exemplo-.com", "bia@exemplo.c", "bia@exe mplo.com", "a".repeat(65) + "@exemplo.com",
                "bia@" + "b".repeat(64) + ".com", "BIA@exemplo.com", "bia@exemplo.com.", "bia@exemplo.c0m",
                "bia@\u00e7.com", "bia@exemplo.com\n");
        for (String amostra : amostras) {
            boolean bancoAceita;
            try {
                jdbc.update("insert into usuario (nome, login, senha_hash, perfil) values "
                        + "('Bia Souza', ?, 'hash-de-teste', 'SOLICITANTE')", amostra);
                bancoAceita = true;
                jdbc.update("delete from usuario where login = ?", amostra);
            } catch (org.springframework.dao.DataIntegrityViolationException recusa) {
                String estado = ((java.sql.SQLException) recusa.getCause()).getSQLState();
                assertThat(estado).as("a amostra '" + amostra + "' só pode cair no CHECK (23514) ou na UNIQUE (23505)")
                        .isIn("23514", "23505");
                bancoAceita = !"23514".equals(estado);
            }
            assertThat(bancoAceita).as("banco e Java sobre '" + amostra + "'")
                    .isEqualTo(br.com.senac.recursos.validation.Emails.valido(amostra));
        }
    }

    @Test
    void perfilForaDosTresPerfisDoPrdERecusado() {
        SQLException erro = violacaoDe(() -> jdbc.update(
                "insert into usuario (nome, login, senha_hash, perfil) values ('X', 'x@exemplo.com', 'hash-de-teste', 'VISITANTE')"));

        assertThat(erro.getSQLState()).isEqualTo("23514");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR"})
    void osTresPerfisDoPrdSaoAceitos(String perfil) {
        assertThatCode(() -> jdbc.update(
                "insert into usuario (nome, login, senha_hash, perfil) values ('X', ?, 'hash-de-teste', ?)",
                "login-" + perfil.toLowerCase() + "@exemplo.com", perfil)).doesNotThrowAnyException();
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
