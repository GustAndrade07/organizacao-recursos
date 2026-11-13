package br.com.senac.recursos.bdd;

import br.com.senac.recursos.support.RelogioDeTeste;
import io.cucumber.spring.CucumberContextConfiguration;
import java.time.Instant;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Contexto dos cenários BDD: a aplicação inteira (segurança, controllers, serviços, JPA) contra um PostgreSQL 16 de
 * verdade (Testcontainers, nunca H2), com a V1 aplicada pelo Flyway, e {@link RelogioDeTeste} no lugar do relógio do
 * sistema (AGENTS.md, seção 5: nada de {@code Instant.now()} solto nem {@code Thread.sleep}).
 *
 * <p>O contêiner é o mesmo dos testes de API ({@code ApiIntegracaoTest}) só em espírito: cada um sobe o seu, porque o
 * Cucumber tem contexto próprio. Docker é requisito (AGENTS.md, seção 8): sem Docker o cenário falha, não é ignorado.
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@Import(ConfiguracaoDoCucumber.RelogioDoBdd.class)
public class ConfiguracaoDoCucumber {

    /** "Agora" dos cenários; as reservas de exemplo ficam em 10/11/2026, no futuro em relação a ele. */
    public static final Instant AGORA = Instant.parse("2026-11-01T12:00:00Z");

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        // Os cenários andam de 01/11 até 10/11; com a validade padrão (1 h) o token expiraria no meio.
        registro.add("recursos.seguranca.jwt.validade", () -> "P30D");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RelogioDoBdd {

        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste(AGORA);
        }
    }
}
