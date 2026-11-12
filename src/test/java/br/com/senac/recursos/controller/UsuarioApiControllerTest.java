package br.com.senac.recursos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.support.ApiIntegracaoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Cadastro de usuários pelo Administrador. A senha só existe como hash e nunca volta na resposta. */
class UsuarioApiControllerTest extends ApiIntegracaoTest {

    private String admin;

    @BeforeEach
    void administrador() {
        admin = token(usuario("admin", PerfilUsuario.ADMINISTRADOR));
    }

    @Test
    void administradorCadastraEASenhaFicaSoComoHashBcrypt() throws Exception {
        enviar(admin, "/api/usuarios",
                "{\"nome\":\"Bia\",\"login\":\"bia\",\"senha\":\"senha-da-bia-1\",\"perfil\":\"RESPONSAVEL\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/usuarios/2"))
                .andExpect(jsonPath("$.login").value("bia"))
                .andExpect(jsonPath("$.perfil").value("RESPONSAVEL"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        String hash = jdbc.queryForObject("select senha_hash from usuario where login = 'bia'", String.class);
        assertThat(hash).startsWith("$2").doesNotContain("senha-da-bia-1");

        enviar(null, "/api/auth/login", "{\"login\":\"bia\",\"senha\":\"senha-da-bia-1\"}").andExpect(status().isOk());
    }

    @Test
    void loginRepetidoDa409() throws Exception {
        enviar(admin, "/api/usuarios",
                "{\"nome\":\"Outro\",\"login\":\"admin\",\"senha\":\"senha-qualquer-1\",\"perfil\":\"SOLICITANTE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:recursos:problema:login-em-uso"));
    }

    @Test
    void dadosInvalidosDao400SemEcoarASenha() throws Exception {
        String corpo = enviar(admin, "/api/usuarios",
                        "{\"nome\":\"\",\"login\":\"A B\",\"senha\":\"curta\",\"perfil\":null}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.length()").value(4))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).doesNotContain("curta").doesNotContain("at br.com").doesNotContainIgnoringCase("exception");
    }

    @Test
    void listaTodosEmOrdemDeCadastro() throws Exception {
        usuario("zeca", PerfilUsuario.SOLICITANTE);

        obter(admin, "/api/usuarios")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("admin"))
                .andExpect(jsonPath("$[1].login").value("zeca"));
    }
}
