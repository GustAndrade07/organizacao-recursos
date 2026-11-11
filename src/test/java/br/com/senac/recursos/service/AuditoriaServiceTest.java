package br.com.senac.recursos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.senac.recursos.enums.EstadoReserva;
import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.model.Reserva;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.repository.ReservaRepository;
import br.com.senac.recursos.support.ApiIntegracaoTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** RN-12 no serviço: auditoria só existe dentro da transação da mudança de estado, e some junto com ela. */
class AuditoriaServiceTest extends ApiIntegracaoTest {

    @Autowired
    private AuditoriaService auditoria;

    @Autowired
    private ReservaRepository reservas;

    @Autowired
    private PlatformTransactionManager transacoes;

    @Test
    void registrarForaDeTransacaoEhRecusadoENaoGravaNada() throws Exception {
        Usuario sol = usuario("sol", PerfilUsuario.SOLICITANTE);
        long id = reservar(token(sol), dia("10:00"), dia("11:00"),
                recurso(TipoRecurso.SALA, "Sala", false).getId());
        Reserva reserva = reservas.findById(id).orElseThrow();

        assertThatThrownBy(() -> auditoria.registrar(reserva, sol, EstadoReserva.APROVADA, EstadoReserva.CANCELADA,
                "sem transação")).isInstanceOf(IllegalTransactionStateException.class);

        assertThat(contar("select count(*) from auditoria_reserva where reserva_id = ?", id)).isOne();
    }

    @Test
    void rollbackDaMudancaDeEstadoLevaAAuditoriaJunto() throws Exception {
        Usuario sol = usuario("sol", PerfilUsuario.SOLICITANTE);
        long id = reservar(token(sol), dia("10:00"), dia("11:00"),
                recurso(TipoRecurso.SALA, "Sala", false).getId());
        TransactionTemplate tx = new TransactionTemplate(transacoes);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            Reserva reserva = reservas.findById(id).orElseThrow();
            reserva.mudarEstado(EstadoReserva.CANCELADA);
            reservas.flush();
            auditoria.registrar(reserva, sol, EstadoReserva.APROVADA, EstadoReserva.CANCELADA, "vai desfazer");
            throw new IllegalStateException("falha depois de gravar estado e auditoria");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("select estado from reserva where id = ?", String.class, id))
                .isEqualTo("APROVADA");
        assertThat(contar("select count(*) from auditoria_reserva where reserva_id = ?", id)).isOne();
    }
}
