package br.com.senac.recursos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.support.ApiIntegracaoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Cadastro de usuários pelo Administrador (DEF-010: o login é o e-mail). A senha só existe como hash e nunca volta na
 * resposta. Nome completo, e-mail e senha são obrigatórios; o perfil continua escolhido pelo Administrador.
 */
class UsuarioApiControllerTest extends ApiIntegracaoTest {

    private String admin;

    @BeforeEach
    void administrador() {
        admin = token(usuario("admin", PerfilUsuario.ADMINISTRADOR));
    }

    private static String corpo(String nome, String login, String senha, String perfil) {
        return "{\"nome\":" + texto(nome) + ",\"login\":" + texto(login) + ",\"senha\":" + texto(senha)
                + ",\"perfil\":" + texto(perfil) + "}";
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Test
    void administradorCadastraEASenhaFicaSoComoHashBcrypt() throws Exception {
        enviar(admin, "/api/usuarios", corpo("Bia Campos", "bia@exemplo.com", "senha-da-bia-1", "RESPONSAVEL"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/usuarios/2"))
                .andExpect(jsonPath("$.nome").value("Bia Campos"))
                .andExpect(jsonPath("$.login").value("bia@exemplo.com"))
                .andExpect(jsonPath("$.perfil").value("RESPONSAVEL"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        String hash = jdbc.queryForObject("select senha_hash from usuario where login = 'bia@exemplo.com'", String.class);
        assertThat(hash).startsWith("$2").doesNotContain("senha-da-bia-1");

        enviar(null, "/api/auth/login", "{\"login\":\"bia@exemplo.com\",\"senha\":\"senha-da-bia-1\"}")
                .andExpect(status().isOk());
    }

    @Test
    void emailComMaiusculasEEspacosEhGuardadoEmMinusculasENomeSemEspacosRepetidos() throws Exception {
        enviar(admin, "/api/usuarios", corpo("  Caio   Dias ", "  CAIO.Dias@Exemplo.COM ", "senha-do-caio-1", "SOLICITANTE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Caio Dias"))
                .andExpect(jsonPath("$.login").value("caio.dias@exemplo.com"));

        assertThat(contar("select count(*) from usuario where login = 'caio.dias@exemplo.com' and nome = 'Caio Dias'"))
                .isOne();
        enviar(null, "/api/auth/login", "{\"login\":\"Caio.Dias@exemplo.com\",\"senha\":\"senha-do-caio-1\"}")
                .andExpect(status().isOk());
    }

    @Test
    void emailRepetidoDa409MesmoComOutraCaixa() throws Exception {
        enviar(admin, "/api/usuarios", corpo("Outro Admin", "admin@exemplo.com", "senha-qualquer-1", "SOLICITANTE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:recursos:problema:login-em-uso"));
        enviar(admin, "/api/usuarios", corpo("Outro Admin", "ADMIN@Exemplo.com", "senha-qualquer-1", "SOLICITANTE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:recursos:problema:login-em-uso"));

        assertThat(contar("select count(*) from usuario")).isOne();
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

    @ParameterizedTest
    @ValueSource(strings = {
        "ana", "ana@", "@exemplo.com", "ana@exemplo", "ana@@exemplo.com", "ana@exemplo..com", "ana @exemplo.com",
        "ana@exemplo.com, bia@exemplo.com", "😀@exemplo.com", "ana@exemplo.com😀"
    })
    void emailForaDoFormatoDa400NoCampoLoginEnadaEhGravado(String email) throws Exception {
        enviar(admin, "/api/usuarios", corpo("Ana Souza", email, "senha-da-ana-1", "SOLICITANTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:recursos:problema:dados-invalidos"))
                .andExpect(jsonPath("$.erros[0].campo").value("login"));

        assertThat(contar("select count(*) from usuario")).isOne();
    }

    @Test
    void emailComMaisDe254CaracteresDa400() throws Exception {
        String longo = "a".repeat(64) + "@" + "b".repeat(63) + "." + "b".repeat(63) + "." + "b".repeat(57) + ".comx";
        assertThat(longo).hasSize(255);

        enviar(admin, "/api/usuarios", corpo("Ana Souza", longo, "senha-da-ana-1", "SOLICITANTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("login"));
    }

    @Test
    void semEmailOuSemNomeDa400ComAMensagemDoCampo() throws Exception {
        enviar(admin, "/api/usuarios", corpo("Ana Souza", "", "senha-da-ana-1", "SOLICITANTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='login')].mensagem").value("Informe o e-mail."));
        enviar(admin, "/api/usuarios", corpo(null, "ana@exemplo.com", "senha-da-ana-1", "SOLICITANTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo=='nome')].mensagem").value("Informe o nome completo."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ana", "Ana1 Souza", "Ana Souza 3", "Ana 😀 Souza", "<b>Ana</b> Souza", "Ana_Souza Lima"})
    void nomeSemSobrenomeComDigitoSimboloOuEmojiDa400NoCampoNome(String nome) throws Exception {
        enviar(admin, "/api/usuarios", corpo(nome, "ana@exemplo.com", "senha-da-ana-1", "SOLICITANTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("nome"));

        assertThat(contar("select count(*) from usuario")).isOne();
    }

    @Test
    void emojiNoNomeRecebeAMensagemDeEmoji() throws Exception {
        enviar(admin, "/api/usuarios", corpo("Ana 😀 Souza", "ana@exemplo.com", "senha-da-ana-1", "SOLICITANTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.length()").value(1))
                .andExpect(jsonPath("$.erros[0].mensagem").value("Não use emojis neste campo."));
    }

    @Test
    void perfilContinuaObrigatorioNoCadastro() throws Exception {
        enviar(admin, "/api/usuarios", corpo("Ana Souza", "ana@exemplo.com", "senha-da-ana-1", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("perfil"));
    }

    @Test
    void listaTodosEmOrdemDeCadastro() throws Exception {
        usuario("zeca", PerfilUsuario.SOLICITANTE);

        obter(admin, "/api/usuarios")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("admin@exemplo.com"))
                .andExpect(jsonPath("$[1].login").value("zeca@exemplo.com"));
    }
}
