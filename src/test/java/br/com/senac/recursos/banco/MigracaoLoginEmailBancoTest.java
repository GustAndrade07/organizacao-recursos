package br.com.senac.recursos.banco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/**
 * V2 (DEF-010, ADR-0010): o login passa a ser e-mail. Um banco de desenvolvimento antigo tem usuários sem {@code @}
 * ({@code admin}, {@code ana.souza}): a V2 <b>não inventa e-mail</b> e <b>não falha</b> por causa deles. Aqui o banco
 * sobe na V1, ganha usuários antigos, recebe a V2, e conferimos que (1) a migração passa, (2) os usuários antigos
 * continuam lá e (3) só as gravações novas passam a exigir e-mail (o CHECK é {@code NOT VALID}).
 */
class MigracaoLoginEmailBancoTest extends BancoIntegracaoTest {

    private static final String BANCO_ANTIGO = "migracao_v2_login_antigo";

    @Value("${spring.datasource.url}")
    private String urlDoBancoDoTeste;

    @Value("${spring.datasource.username}")
    private String usuarioDoBanco;

    @Value("${spring.datasource.password}")
    private String senhaDoBanco;

    private String urlDe(String banco) {
        return urlDoBancoDoTeste.replaceFirst("/[^/?]+(\\?|$)", "/" + banco + "$1");
    }

    private Connection conectar(String banco) throws SQLException {
        return DriverManager.getConnection(urlDe(banco), usuarioDoBanco, senhaDoBanco);
    }

    @Test
    void bancoAntigoComUsuariosSemArrobaMigraSemFalharEOsUsuariosAntigosFicam() throws Exception {
        jdbc.execute("drop database if exists " + BANCO_ANTIGO);
        jdbc.execute("create database " + BANCO_ANTIGO);

        Flyway.configure().dataSource(urlDe(BANCO_ANTIGO), usuarioDoBanco, senhaDoBanco)
                .locations("classpath:db/migration").target("1").load().migrate();
        try (Connection conexao = conectar(BANCO_ANTIGO); var comando = conexao.createStatement()) {
            comando.execute("insert into usuario (nome, login, senha_hash, perfil) values "
                    + "('Administrador', 'admin', 'hash-antigo', 'ADMINISTRADOR'), "
                    + "('Ana Souza', 'Ana.Souza', 'hash-antigo', 'SOLICITANTE')");
        }

        var resultado = Flyway.configure().dataSource(urlDe(BANCO_ANTIGO), usuarioDoBanco, senhaDoBanco)
                .locations("classpath:db/migration").load().migrate();

        assertThat(resultado.success).as("a V2 passa num banco com usuário antigo").isTrue();
        assertThat(resultado.migrationsExecuted).isEqualTo(1);
        try (Connection conexao = conectar(BANCO_ANTIGO); var comando = conexao.createStatement()) {
            var logins = comando.executeQuery("select string_agg(login, ',' order by id) from usuario");
            logins.next();
            assertThat(logins.getString(1)).as("nenhum e-mail foi inventado nem apagado").isEqualTo("admin,Ana.Souza");

            var validada = comando.executeQuery(
                    "select convalidated from pg_constraint where conname = 'usuario_login_e_email'");
            validada.next();
            assertThat(validada.getBoolean(1)).as("CHECK criado NOT VALID: só vale para gravações novas").isFalse();

            assertThatThrownBy(() -> comando.execute("insert into usuario (nome, login, senha_hash, perfil) values "
                    + "('Novo Usuario', 'sem-arroba', 'hash', 'SOLICITANTE')"))
                    .as("gravação nova sem @ é recusada")
                    .isInstanceOfSatisfying(SQLException.class, erro -> assertThat(erro.getSQLState()).isEqualTo("23514"));

            comando.execute("insert into usuario (nome, login, senha_hash, perfil) values "
                    + "('Novo Usuario', 'novo@exemplo.com', 'hash', 'SOLICITANTE')");
        } finally {
            jdbc.execute("drop database if exists " + BANCO_ANTIGO + " with (force)");
        }
    }

    @Test
    void v2FoiAplicadaNoBancoNovoDoTeste() {
        var versoes = jdbc.queryForList(
                "select version from flyway_schema_history where success and version is not null order by installed_rank",
                String.class);

        assertThat(versoes).containsExactly("1", "2");
    }
}
