package br.com.senac.recursos.banco;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * ADR-0006: o Flyway é o único dono do esquema. Aqui as migrações rodam num banco vazio (contêiner novo), a
 * aplicação sobe com {@code ddl-auto: validate} e o catálogo prova que a proteção contra reserva dupla existe.
 */
class MigracoesBancoTest extends BancoIntegracaoTest {

    @Autowired
    Environment ambiente;

    @Test
    void todasAsMigracoesForamAplicadasComSucessoNumBancoVazio() {
        Long falhas = jdbc.queryForObject("select count(*) from flyway_schema_history where success = false", Long.class);
        List<String> versoes = jdbc.queryForList(
                "select version from flyway_schema_history where success and version is not null order by installed_rank",
                String.class);

        assertThat(falhas).isZero();
        assertThat(versoes).contains("1", "2");
    }

    @Test
    void hibernateSoConfereOEsquemaEntaoOContextoSobeEmModoValidate() {
        // O contexto já subiu (a classe é @SpringBootTest); aqui se trava a configuração que torna isso um teste.
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "usuario", "recurso", "manutencao", "reserva", "reserva_recurso", "tentativa_recusada", "auditoria_reserva"
    })
    void tabelasDoNucleoDaReservaExistem(String tabela) {
        Long existe = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' and table_name = ?",
                Long.class, tabela);

        assertThat(existe).isEqualTo(1L);
    }

    @Test
    void extensaoBtreeGistEstaInstaladaNoCatalogo() {
        Long instalada = jdbc.queryForObject(
                "select count(*) from pg_extension where extname = 'btree_gist'", Long.class);

        assertThat(instalada).isEqualTo(1L);
    }

    @Test
    void constraintDeExclusaoDeReservaRecursoExisteNoCatalogoEEParcialPeloEstado() {
        List<String> definicoes = jdbc.queryForList(
                "select pg_get_constraintdef(oid) from pg_constraint "
                        + "where contype = 'x' and conname = 'reserva_recurso_sem_sobreposicao' "
                        + "and conrelid = 'reserva_recurso'::regclass",
                String.class);

        assertThat(definicoes).hasSize(1);
        assertThat(definicoes.get(0))
                .contains("EXCLUDE USING gist")
                .contains("recurso_id WITH =")
                .contains("periodo WITH &&")
                .contains("WHERE");
    }
}
