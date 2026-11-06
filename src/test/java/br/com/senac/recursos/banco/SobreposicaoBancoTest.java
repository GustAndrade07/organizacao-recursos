package br.com.senac.recursos.banco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * RN-02, RN-03, RF-05 e RF-06 no nível do banco (ADR-0002): a constraint {@code EXCLUDE USING gist} recusa
 * sobreposição de recurso e de professor, com intervalo semiaberto (D1) e só para os estados que ocupam o horário (D5).
 *
 * <p>SQLState do PostgreSQL: 23P01 = violação de exclusão, 23503 = chave estrangeira, 23514 = check, 22000 = dado inválido.
 */
class SobreposicaoBancoTest extends BancoComDadosTest {

    private static final String VIOLACAO_DE_EXCLUSAO = "23P01";

    private long ana;
    private long sala101;

    @BeforeEach
    void cenarioBase() {
        ana = novoUsuario("ana");
        sala101 = novoRecurso("SALA", "Sala 101");
    }

    @Test
    void sobreposicaoNoMesmoRecursoERecusadaPeloBancoEODesfazTudo() {
        reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);

        SQLException erro = violacaoDe(() -> reservar(ana, "SOLICITADA", periodo("11:00:00", "13:00:00"), sala101));

        assertThat(erro.getSQLState()).isEqualTo(VIOLACAO_DE_EXCLUSAO);
        assertThat(erro.getMessage()).contains("reserva_recurso_sem_sobreposicao");
        // a segunda transação inteira foi desfeita: sobrou só a primeira reserva
        assertThat(contar("reserva")).isEqualTo(1);
        assertThat(contar("reserva_recurso")).isEqualTo(1);
    }

    @Test
    void recursosDiferentesNoMesmoHorarioConvivem() {
        long sala102 = novoRecurso("SALA", "Sala 102");
        reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);

        assertThatCode(() -> reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala102))
                .doesNotThrowAnyException();

        assertThat(contar("reserva_recurso")).isEqualTo(2);
    }

    @Test
    void sobreposicaoNaAgendaDoProfessorERecusadaMesmoComSalaDiferente() {
        long sala102 = novoRecurso("SALA", "Sala 102");
        long marta = novoRecurso("PROFESSOR", "Profa. Marta");
        reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101, marta);

        SQLException erro = violacaoDe(
                () -> reservar(ana, "SOLICITADA", periodo("11:00:00", "13:00:00"), sala102, marta));

        assertThat(erro.getSQLState()).isEqualTo(VIOLACAO_DE_EXCLUSAO);
        // a sala 102 estava livre, mas a reserva é uma só: nada dela pode ter ficado
        assertThat(contar("reserva_recurso")).isEqualTo(2);
        Long linhasDaSala102 = jdbc.queryForObject(
                "select count(*) from reserva_recurso where recurso_id = ?", Long.class, sala102);
        assertThat(linhasDaSala102).isZero();
    }

    /** D1: intervalo semiaberto [início, fim). Existente sempre 10:00 a 12:00. */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "termina exatamente no início da existente,  08:00:00, 10:00:00, true",
        "começa exatamente no fim da existente,      12:00:00, 13:00:00, true",
        "cruza 1 segundo no fim da existente,        11:59:59, 13:00:00, false",
        "cruza 1 segundo no início da existente,     08:00:00, 10:00:01, false",
        "mesmo intervalo,                            10:00:00, 12:00:00, false",
        "contido na existente,                       10:30:00, 11:30:00, false",
        "contém a existente,                         09:00:00, 13:00:00, false"
    })
    void fronteirasDoIntervaloSemiaberto(String caso, String inicio, String fim, boolean aceita) {
        reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);

        if (aceita) {
            assertThatCode(() -> reservar(ana, "SOLICITADA", periodo(inicio, fim), sala101))
                    .as(caso).doesNotThrowAnyException();
            assertThat(contar("reserva_recurso")).isEqualTo(2);
        } else {
            assertThat(violacaoDe(() -> reservar(ana, "SOLICITADA", periodo(inicio, fim), sala101)).getSQLState())
                    .as(caso).isEqualTo(VIOLACAO_DE_EXCLUSAO);
            assertThat(contar("reserva_recurso")).isEqualTo(1);
        }
    }

    /** D5: estados que liberam o horário. */
    @ParameterizedTest
    @ValueSource(strings = {"CANCELADA", "REJEITADA", "NAO_COMPARECEU"})
    void estadoQueLiberaOHorarioPermiteNovaReservaNoMesmoIntervalo(String estadoLiberador) {
        reservar(ana, estadoLiberador, periodo("10:00:00", "12:00:00"), sala101);

        assertThatCode(() -> reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101))
                .doesNotThrowAnyException();

        assertThat(contar("reserva_recurso")).isEqualTo(2);
    }

    /** D5: estados que ocupam o horário. */
    @ParameterizedTest
    @ValueSource(strings = {"SOLICITADA", "APROVADA", "EM_USO", "CONCLUIDA"})
    void estadoQueOcupaOHorarioBloqueiaNovaReservaNoMesmoIntervalo(String estadoOcupante) {
        reservar(ana, estadoOcupante, periodo("10:00:00", "12:00:00"), sala101);

        SQLException erro = violacaoDe(() -> reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101));

        assertThat(erro.getSQLState()).isEqualTo(VIOLACAO_DE_EXCLUSAO);
    }

    @Test
    void cancelarReservaNaoViolaAConstraintELiberaOHorario() {
        long reserva = reservar(ana, "APROVADA", periodo("10:00:00", "12:00:00"), sala101);

        assertThatCode(() -> jdbc.update("update reserva set estado = 'CANCELADA' where id = ?", reserva))
                .doesNotThrowAnyException();

        String estadoNaOcupacao = jdbc.queryForObject(
                "select estado from reserva_recurso where reserva_id = ?", String.class, reserva);
        assertThat(estadoNaOcupacao).isEqualTo("CANCELADA");
        assertThatCode(() -> reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101))
                .doesNotThrowAnyException();
    }

    @Test
    void transicoesEntreEstadosQueOcupamNaoViolamAConstraint() {
        long reserva = reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);

        for (String proximo : new String[] {"APROVADA", "EM_USO", "CONCLUIDA"}) {
            assertThatCode(() -> jdbc.update("update reserva set estado = ? where id = ?", proximo, reserva))
                    .as("transição para " + proximo).doesNotThrowAnyException();
        }

        String estadoNaOcupacao = jdbc.queryForObject(
                "select estado from reserva_recurso where reserva_id = ?", String.class, reserva);
        assertThat(estadoNaOcupacao).isEqualTo("CONCLUIDA");
    }

    /** Hoje o RN-05 não prevê voltar de estado final para ativo; se um dia previr, o banco recusa se houver conflito. */
    @Test
    void reativarReservaCanceladaComHorarioJaOcupadoERecusadoPeloBanco() {
        long cancelada = reservar(ana, "CANCELADA", periodo("10:00:00", "12:00:00"), sala101);
        reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);

        SQLException erro = violacaoDe(
                () -> jdbc.update("update reserva set estado = 'APROVADA' where id = ?", cancelada));

        assertThat(erro.getSQLState()).isEqualTo(VIOLACAO_DE_EXCLUSAO);
    }

    @Test
    void estadoDaOcupacaoDiferenteDoEstadoDaReservaERecusadoPeloBanco() {
        long reserva = reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);
        long sala102 = novoRecurso("SALA", "Sala 102");

        SQLException erro = violacaoDe(() -> jdbc.update(
                "insert into reserva_recurso (reserva_id, estado, recurso_id, periodo) "
                        + "values (?, 'APROVADA', ?, ?::tstzrange)",
                reserva, sala102, periodo("14:00:00", "15:00:00")));

        assertThat(erro.getSQLState()).isEqualTo("23503");
    }

    /** D1 no banco: só {@code [início, fim)}, finito e não vazio. */
    @ParameterizedTest
    @ValueSource(strings = {
        "(2026-11-10T10:00:00Z,2026-11-10T12:00:00Z)",
        "[2026-11-10T10:00:00Z,2026-11-10T12:00:00Z]",
        "(2026-11-10T10:00:00Z,2026-11-10T12:00:00Z]",
        "[2026-11-10T10:00:00Z,2026-11-10T10:00:00Z)",
        "[2026-11-10T10:00:00Z,)",
        "[,2026-11-10T12:00:00Z)",
        "empty"
    })
    void periodoForaDoPadraoSemiabertoFinitoENaoVazioERecusado(String periodoInvalido) {
        SQLException erro = violacaoDe(() -> reservar(ana, "SOLICITADA", periodoInvalido, sala101));

        assertThat(erro.getSQLState()).isEqualTo("23514");
    }

    @Test
    void periodoComTerminoAntesDoInicioERecusadoPeloBanco() {
        SQLException erro = violacaoDe(
                () -> reservar(ana, "SOLICITADA", periodo("12:00:00", "10:00:00"), sala101));

        assertThat(erro.getSQLState()).isEqualTo("22000");
    }

    @Test
    void estadoForaDaMaquinaDeEstadosERecusadoPeloBanco() {
        SQLException erro = violacaoDe(() -> reservar(ana, "TALVEZ", periodo("10:00:00", "12:00:00"), sala101));

        assertThat(erro.getSQLState()).isEqualTo("23514");
    }
}
