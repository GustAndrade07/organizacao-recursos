package br.com.senac.recursos.banco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.sql.SQLException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de banco: PostgreSQL 16 de verdade (Testcontainers, nunca H2), subido uma vez por JVM, e a
 * aplicação inteira subindo contra ele com as migrações Flyway aplicadas do zero e {@code ddl-auto: validate}.
 *
 * <p>Docker é requisito (AGENTS.md, seção 8): não há {@code disabledWithoutDocker}; sem Docker os testes falham.
 * Os testes usam SQL direto (JdbcTemplate): entidades e serviços são do back-end.
 */
@SpringBootTest
abstract class BancoIntegracaoTest {

    // Contêiner único por JVM (padrão singleton): o contexto do Spring fica em cache entre as classes de teste e
    // não aponta para um contêiner já parado. O Ryuk do Testcontainers remove o contêiner ao fim da JVM.
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propriedadesDoBanco(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        // Os testes de concorrência disparam 20 pedidos de uma vez; com o pool padrão (10) metade esperaria
        // conexão e os pedidos não seriam simultâneos de verdade. Só vale para os testes.
        registro.add("spring.datasource.hikari.maximum-pool-size", () -> "30");
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    protected TransactionTemplate tx;

    @BeforeEach
    void criarTemplateDeTransacao() {
        tx = new TransactionTemplate(transactionManager);
    }

    /** Conta linhas direto no banco (nunca pela resposta de uma API). {@code tabela} é sempre literal do teste. */
    protected long contar(String tabela) {
        Long total = jdbc.queryForObject("select count(*) from " + tabela, Long.class);
        return total;
    }

    /** Executa a ação, exige que o banco a recuse e devolve o erro SQL (com SQLState e mensagem do PostgreSQL). */
    protected SQLException violacaoDe(ThrowingCallable acao) {
        Throwable erro = catchThrowable(acao);
        assertThat(erro).as("o banco deveria ter recusado a operação").isNotNull();
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql && sql.getSQLState() != null) {
                return sql;
            }
        }
        throw new AssertionError("a recusa não veio de um erro SQL do banco", erro);
    }
}
