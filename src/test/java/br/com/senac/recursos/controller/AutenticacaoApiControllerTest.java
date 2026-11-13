package br.com.senac.recursos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.support.ApiIntegracaoTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** RF-01 e ADR-0001: login, token e recusas (sem token, expirado, adulterado), sempre em ProblemDetail. */
class AutenticacaoApiControllerTest extends ApiIntegracaoTest {

    @Test
    void loginCertoDevolveTokenQueAbreAsRotasDoPerfil() throws Exception {
        usuario("ana", PerfilUsuario.SOLICITANTE);

        MvcResult resultado = enviar(null, "/api/auth/login", "{\"login\":\"ana@exemplo.com\",\"senha\":\"" + SENHA + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").value(AGORA.plus(VALIDADE_DO_TOKEN).toString()))
                .andReturn();
        String token = lerJson(resultado).get("token").asText();

        obter(token, "/api/usuarios/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.perfil").value("SOLICITANTE"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void senhaErradaELoginInexistenteDaoAMesmaResposta() throws Exception {
        usuario("ana", PerfilUsuario.SOLICITANTE);

        String senhaErrada = enviar(null, "/api/auth/login", "{\"login\":\"ana@exemplo.com\",\"senha\":\"outra-senha\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.type").value("urn:recursos:problema:credenciais-invalidas"))
                .andReturn().getResponse().getContentAsString();
        String loginInexistente = enviar(null, "/api/auth/login", "{\"login\":\"ninguem@exemplo.com\",\"senha\":\"outra-senha\"}")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(senhaErrada).isEqualTo(loginInexistente.replace("ninguem@exemplo.com", "ana@exemplo.com"));
        assertThat(senhaErrada).doesNotContain("outra-senha");
    }

    @Test
    void caixaEEspacosNoEmailNaoImportamNoLogin() throws Exception {
        usuario("ana", PerfilUsuario.SOLICITANTE);

        enviar(null, "/api/auth/login", "{\"login\":\"  ANA@Exemplo.COM \",\"senha\":\"" + SENHA + "\"}")
                .andExpect(status().isOk());
    }

    @Test
    void textoQueNaoEEmailNoLoginDaOMesmo401DeSenhaErradaSemRevelarFormato() throws Exception {
        usuario("ana", PerfilUsuario.SOLICITANTE);
        String senhaErrada = enviar(null, "/api/auth/login", "{\"login\":\"ana@exemplo.com\",\"senha\":\"outra-senha\"}")
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        String semArroba = enviar(null, "/api/auth/login", "{\"login\":\"ana\",\"senha\":\"outra-senha\"}")
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        assertThat(semArroba).isEqualTo(senhaErrada);
    }

    @Test
    void erroDeLoginFalaEmEmailESemRevelarSeAContaExiste() throws Exception {
        enviar(null, "/api/auth/login", "{\"login\":\"ninguem@exemplo.com\",\"senha\":\"outra-senha\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    void loginSemCamposDa400ComOsCamposESemEcoarValores() throws Exception {
        enviar(null, "/api/auth/login", "{\"login\":\"\",\"senha\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:recursos:problema:dados-invalidos"))
                .andExpect(jsonPath("$.erros.length()").value(2))
                .andExpect(jsonPath("$.erros[*].campo").value(org.hamcrest.Matchers.containsInAnyOrder("login", "senha")));
    }

    @Test
    void semTokenDa401EmProblemDetail() throws Exception {
        obter(null, "/api/usuarios/me")
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.type").value("urn:recursos:problema:nao-autenticado"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenExpiradoDa401SemDormir() throws Exception {
        String token = token(usuario("ana", PerfilUsuario.SOLICITANTE));
        obter(token, "/api/usuarios/me").andExpect(status().isOk());

        // No instante exato de "exp" o Spring ainda aceita; um segundo depois, recusa.
        relogio.avancar(VALIDADE_DO_TOKEN);
        obter(token, "/api/usuarios/me").andExpect(status().isOk());
        relogio.avancar(Duration.ofSeconds(1));
        obter(token, "/api/usuarios/me").andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAdulteradoDa401ESemEcoarOToken() throws Exception {
        Usuario ana = usuario("ana", PerfilUsuario.SOLICITANTE);
        String token = token(ana);
        String[] partes = token.split("\\.");
        // Troca o perfil no corpo do token sem refazer a assinatura.
        String corpoAdulterado = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(java.util.Base64.getUrlDecoder().decode(partes[1]))
                        .replace("SOLICITANTE", "ADMINISTRADOR").getBytes());
        String adulterado = partes[0] + "." + corpoAdulterado + "." + partes[2];

        String corpo = obter(adulterado, "/api/usuarios")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(corpo).doesNotContain(partes[2]).doesNotContainIgnoringCase("exception");
    }
}
