package br.com.senac.recursos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.support.ApiIntegracaoTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.context.TestPropertySource;

/** O primeiro Administrador nasce das variáveis de ambiente, uma vez só, e consegue entrar. */
@TestPropertySource(properties = {
    "recursos.administrador-inicial.login=Chefe@Exemplo.com",
    "recursos.administrador-inicial.senha=senha-do-chefe-123",
    "recursos.administrador-inicial.nome="
})
class AdministradorInicialTest extends ApiIntegracaoTest {

    @Autowired
    private ApplicationRunner administradorInicial;

    @Test
    void criaUmaVezSoEOAdministradorConsegueEntrar() throws Exception {
        administradorInicial.run(new DefaultApplicationArguments());
        administradorInicial.run(new DefaultApplicationArguments());

        assertThat(contar("select count(*) from usuario where login = 'chefe@exemplo.com' and perfil = 'ADMINISTRADOR' "
                + "and nome = 'Administrador'")).isOne();
        enviar(null, "/api/auth/login", "{\"login\":\"chefe@exemplo.com\",\"senha\":\"senha-do-chefe-123\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void semAsVariaveisNaoCriaNinguem() throws Exception {
        AdministradorInicialPropriedades vazias = new AdministradorInicialPropriedades("", null, null);
        assertThat(vazias.configurado()).isFalse();
        assertThat(vazias.toString()).doesNotContain("null,").contains("senha=***");
        assertThat(new AdministradorInicialPropriedades("x", "", null).configurado()).isFalse();
    }
}
