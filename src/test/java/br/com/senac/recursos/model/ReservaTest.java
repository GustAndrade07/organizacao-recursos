package br.com.senac.recursos.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.senac.recursos.enums.EstadoReserva;
import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.exception.TransicaoInvalidaException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regras que vivem no agregado: ordem das linhas (ADR-0007), máquina de estados (RN-05) e recurso restrito. */
class ReservaTest {

    private static final Periodo PERIODO = new Periodo(Instant.parse("2026-11-10T10:00:00Z"),
            Instant.parse("2026-11-10T11:00:00Z"));

    private static <T> T comId(T entidade, long id) throws ReflectiveOperationException {
        Field campo = entidade.getClass().getDeclaredField("id");
        campo.setAccessible(true);
        campo.set(entidade, id);
        return entidade;
    }

    @Test
    void linhasSaemEmOrdemCrescenteDeRecursoMesmoQuandoPedidasForaDeOrdem() throws Exception {
        Recurso professor = comId(new Recurso(TipoRecurso.PROFESSOR, "Prof", null, false), 9);
        Recurso sala = comId(new Recurso(TipoRecurso.SALA, "Sala", 30, false), 2);
        Recurso projetor = comId(new Recurso(TipoRecurso.MATERIAL, "Projetor", null, false), 5);

        Reserva reserva = Reserva.nova(solicitante(1), List.of(professor, sala, projetor), PERIODO,
                EstadoReserva.APROVADA, Instant.EPOCH);

        assertThat(reserva.recursoIds()).containsExactly(2L, 5L, 9L);
        assertThat(reserva.getPeriodo()).isEqualTo(PERIODO);
    }

    @Test
    void transicaoForaDoMapaEhRecusadaEOEstadoNaoMuda() throws Exception {
        Reserva reserva = Reserva.nova(solicitante(1), List.of(comId(new Recurso(TipoRecurso.SALA, "S", 1, false), 1)),
                PERIODO, EstadoReserva.SOLICITADA, Instant.EPOCH);

        assertThatThrownBy(() -> reserva.mudarEstado(EstadoReserva.CONCLUIDA))
                .isInstanceOf(TransicaoInvalidaException.class)
                .hasMessageContaining("SOLICITADA").hasMessageContaining("CONCLUIDA");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.SOLICITADA);

        reserva.mudarEstado(EstadoReserva.APROVADA);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.APROVADA);
    }

    @Test
    void reservaComUmRecursoRestritoExigeAprovacao() throws Exception {
        Recurso livre = comId(new Recurso(TipoRecurso.SALA, "Sala", 30, false), 1);
        Recurso restrito = comId(new Recurso(TipoRecurso.SALA, "Laboratório", 20, true), 2);

        assertThat(Reserva.nova(solicitante(1), List.of(livre), PERIODO, EstadoReserva.APROVADA, Instant.EPOCH)
                .temRecursoRestrito()).isFalse();
        assertThat(Reserva.nova(solicitante(1), List.of(livre, restrito), PERIODO, EstadoReserva.SOLICITADA,
                Instant.EPOCH).temRecursoRestrito()).isTrue();
    }

    @Test
    void alterarPeriodoMudaTodasAsLinhas() throws Exception {
        Reserva reserva = Reserva.nova(solicitante(1), List.of(
                        comId(new Recurso(TipoRecurso.SALA, "S", 1, false), 1),
                        comId(new Recurso(TipoRecurso.PROFESSOR, "P", null, false), 2)),
                PERIODO, EstadoReserva.APROVADA, Instant.EPOCH);
        Periodo novo = new Periodo(Instant.parse("2026-11-10T14:00:00Z"), Instant.parse("2026-11-10T15:00:00Z"));

        reserva.alterarPeriodo(novo);

        assertThat(reserva.getRecursos()).allSatisfy(linha -> assertThat(linha.getPeriodo()).isEqualTo(novo));
    }

    @Test
    void donoEhQuemSolicitou() throws Exception {
        Reserva reserva = Reserva.nova(solicitante(7), List.of(comId(new Recurso(TipoRecurso.SALA, "S", 1, false), 1)),
                PERIODO, EstadoReserva.APROVADA, Instant.EPOCH);

        assertThat(reserva.pertenceA(7L)).isTrue();
        assertThat(reserva.pertenceA(8L)).isFalse();
    }

    private static Usuario solicitante(long id) throws ReflectiveOperationException {
        return comId(new Usuario("Sol", "sol" + id, "hash", PerfilUsuario.SOLICITANTE), id);
    }
}
