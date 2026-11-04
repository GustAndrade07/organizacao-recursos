package br.com.senac.recursos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Prova que a aplicação sobe contra um PostgreSQL de verdade (sem H2) e que o Flyway roda no start.
 * Sem Docker na máquina o teste é ignorado; no CI (GitHub Actions) o Docker existe e ele roda.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class AplicacaoSmokeTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void aplicacaoSobeEConversaComPostgresReal() {
        Integer resposta = jdbc.queryForObject("select 1", Integer.class);

        assertThat(resposta).isEqualTo(1);
    }
}
