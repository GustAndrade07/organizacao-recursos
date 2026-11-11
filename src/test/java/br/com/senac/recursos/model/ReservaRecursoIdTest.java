package br.com.senac.recursos.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * A chave composta de {@code reserva_recurso} é comparada por valor: o Hibernate usa {@code equals}/{@code hashCode}
 * dela para achar a linha no contexto de persistência. Se a comparação falhar, a mesma linha vira duas entidades.
 */
class ReservaRecursoIdTest {

    private static ReservaRecursoId chave(Long reservaId, Long recursoId) throws ReflectiveOperationException {
        ReservaRecursoId chave = new ReservaRecursoId();
        definir(chave, "reservaId", reservaId);
        definir(chave, "recursoId", recursoId);
        return chave;
    }

    private static void definir(Object alvo, String campo, Object valor) throws ReflectiveOperationException {
        Field f = alvo.getClass().getDeclaredField(campo);
        f.setAccessible(true);
        f.set(alvo, valor);
    }

    @Test
    void mesmaReservaEMesmoRecursoSaoAMesmaChave() throws Exception {
        ReservaRecursoId a = chave(1L, 2L);
        ReservaRecursoId b = chave(1L, 2L);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isEqualTo(a);
        assertThat(a.getReservaId()).isEqualTo(1L);
        assertThat(a.getRecursoId()).isEqualTo(2L);
    }

    @Test
    void qualquerParteDiferenteEhOutraChave() throws Exception {
        ReservaRecursoId base = chave(1L, 2L);

        assertThat(base).isNotEqualTo(chave(1L, 3L))
                .isNotEqualTo(chave(9L, 2L))
                .isNotEqualTo(chave(null, 2L))
                .isNotEqualTo(null)
                .isNotEqualTo("1-2");
    }
}
