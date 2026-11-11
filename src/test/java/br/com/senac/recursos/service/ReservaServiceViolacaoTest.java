package br.com.senac.recursos.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Só o SQLState 23P01 (violação da constraint de exclusão) vira 409 de conflito de horário; outra violação de
 * integridade segue como erro, para não ser contada como "conflito evitado" no relatório.
 */
class ReservaServiceViolacaoTest {

    @Test
    void reconhece23P01MesmoEmbrulhadoEmVariasCamadas() {
        SQLException exclusao = new SQLException("conflicting key value violates exclusion constraint", "23P01");
        Exception embrulhada = new DataIntegrityViolationException("x", new RuntimeException("hibernate", exclusao));

        assertThat(ReservaService.violouExclusao(embrulhada)).isTrue();
    }

    @Test
    void naoConfundeComChaveEstrangeiraNemComDeadlock() {
        assertThat(ReservaService.violouExclusao(new DataIntegrityViolationException("x",
                new SQLException("fk", "23503")))).isFalse();
        assertThat(ReservaService.violouExclusao(new DataIntegrityViolationException("x",
                new SQLException("deadlock", "40P01")))).isFalse();
        assertThat(ReservaService.violouExclusao(new DataIntegrityViolationException("sem causa"))).isFalse();
    }
}
