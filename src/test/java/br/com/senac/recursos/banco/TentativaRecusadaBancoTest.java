package br.com.senac.recursos.banco;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * D2 (ADR-0002): a tentativa recusada por sobreposição é gravada em transação própria, fora da transação que sofreu a
 * violação, para o "conflitos evitados" do RF-12 não perder dado. Aqui só o comportamento do banco e da transação;
 * a tradução em 409 é do serviço de reserva (Pedro).
 */
class TentativaRecusadaBancoTest extends BancoComDadosTest {

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
    void tentativaGravadaEmTransacaoPropriaSobreviveAoRollbackDoPedidoRecusado() {
        reservar(ana, "SOLICITADA", periodo("10:00:00", "12:00:00"), sala101);
        TransactionTemplate propria = new TransactionTemplate(transactionManager);
        propria.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        String pedido = periodo("11:00:00", "13:00:00");

        SQLException erro = violacaoDe(() -> tx.executeWithoutResult(status -> {
            Long reservaDoBruno = jdbc.queryForObject(
                    "insert into reserva (solicitante_id, estado, criada_em) "
                            + "values (?, 'SOLICITADA', '2026-11-01T09:00:00Z') returning id",
                    Long.class, bruno);
            try {
                jdbc.update(
                        "insert into reserva_recurso (reserva_id, estado, recurso_id, periodo) "
                                + "values (?, 'SOLICITADA', ?, ?::tstzrange)",
                        reservaDoBruno, sala101, pedido);
            } catch (DataAccessException violacao) {
                propria.executeWithoutResult(s -> registrarTentativaRecusada(sala101, bruno, pedido));
                throw violacao;
            }
        }));

        assertThat(erro.getSQLState()).isEqualTo("23P01");
        assertThat(contar("tentativa_recusada")).isEqualTo(1);
        // o pedido recusado não deixou reserva do Bruno; só a da Ana existe
        Long reservasDoBruno = jdbc.queryForObject(
                "select count(*) from reserva where solicitante_id = ?", Long.class, bruno);
        assertThat(reservasDoBruno).isZero();
        assertThat(contar("reserva")).isEqualTo(1);
    }

    @Test
    void tentativaGuardaQuemTentouQualRecursoOIntervaloPedidoEOMotivo() {
        registrarTentativaRecusada(sala101, bruno, periodo("11:00:00", "13:00:00"));

        var linha = jdbc.queryForMap(
                "select recurso_id, solicitante_id, lower(periodo_pedido) as inicio, upper(periodo_pedido) as fim, "
                        + "motivo, registrada_em from tentativa_recusada");

        assertThat(linha.get("recurso_id")).isEqualTo(sala101);
        assertThat(linha.get("solicitante_id")).isEqualTo(bruno);
        assertThat(linha.get("motivo")).isEqualTo("SOBREPOSICAO");
        assertThat(linha.get("inicio")).isNotNull();
        assertThat(linha.get("fim")).isNotNull();
        assertThat(linha.get("registrada_em")).isNotNull();
    }

    @Test
    void motivoForaDoCatalogoERecusadoPeloBanco() {
        SQLException erro = violacaoDe(() -> jdbc.update(
                "insert into tentativa_recusada (recurso_id, solicitante_id, periodo_pedido, motivo, registrada_em) "
                        + "values (?, ?, ?::tstzrange, 'QUALQUER COISA', '2026-11-01T09:00:00Z')",
                sala101, bruno, periodo("11:00:00", "13:00:00")));

        assertThat(erro.getSQLState()).isEqualTo("23514");
    }

    @Test
    void tentativaSemSolicitanteOuSemRecursoExistenteERecusadaPelaChaveEstrangeira() {
        SQLException erro = violacaoDe(() -> registrarTentativaRecusada(sala101, 999_999L, periodo("11:00:00", "13:00:00")));

        assertThat(erro.getSQLState()).isEqualTo("23503");
    }
}
