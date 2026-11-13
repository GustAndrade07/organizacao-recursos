package br.com.senac.recursos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.support.ApiIntegracaoTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Limite de tentativas no {@code POST /api/auth/login} (RF-01, ADR-0009, ressalva 1 do PR #48 estendida à API): a
 * mesma regra e o <b>mesmo contador</b> do login das telas. 5 falhas do mesmo login em 15 minutos bloqueiam esse login
 * por 15 minutos, pela tela ou pela API; o bloqueado recebe o mesmo 401 de uma senha errada (não 429, que revelaria o
 * bloqueio e que o login existe). O tempo passa pelo {@code RelogioDeTeste} da base, sem {@code Thread.sleep}. Cada
 * teste usa um login próprio.
 */
class LimiteDeTentativasApiTest extends ApiIntegracaoTest {

    private static final String SENHA_ERRADA = "senha-errada";
    private static final int LIMITE = 5;
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

    @Test
    void quatroFalhasAindaNaoBloqueiam() throws Exception {
        usuario("api.quatro", PerfilUsuario.SOLICITANTE);
        errarNaApi("api.quatro", LIMITE - 1);

        entrarNaApi("api.quatro", SENHA).andExpect(status().isOk());
    }

    @Test
    void cincoFalhasBloqueiamEASenhaCertaRecebeOMesmo401DeSenhaErrada() throws Exception {
        usuario("api.cinco", PerfilUsuario.SOLICITANTE);
        errarNaApi("api.cinco", LIMITE - 1);
        String senhaErrada = entrarNaApi("api.cinco", SENHA_ERRADA)
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String bloqueado = entrarNaApi("api.cinco", SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andReturn().getResponse().getContentAsString();

        assertThat(bloqueado).isEqualTo(senhaErrada).doesNotContainIgnoringCase("bloque");
    }

    @Test
    void depoisDeQuinzeMinutosASenhaCertaEntra() throws Exception {
        usuario("api.quinze", PerfilUsuario.SOLICITANTE);
        errarNaApi("api.quinze", LIMITE);

        relogio.avancar(BLOQUEIO.minusSeconds(1));
        entrarNaApi("api.quinze", SENHA).andExpect(status().isUnauthorized());

        relogio.avancar(Duration.ofSeconds(1));
        entrarNaApi("api.quinze", SENHA).andExpect(status().isOk());
    }

    @Test
    void loginCertoZeraOContador() throws Exception {
        usuario("api.zera", PerfilUsuario.SOLICITANTE);
        errarNaApi("api.zera", LIMITE - 1);
        entrarNaApi("api.zera", SENHA).andExpect(status().isOk());

        errarNaApi("api.zera", LIMITE - 1);

        entrarNaApi("api.zera", SENHA).andExpect(status().isOk());
    }

    /** As 5 falhas acontecem antes de o login existir; criado depois, ainda no bloqueio, ele não entra. */
    @Test
    void loginInexistenteTambemBloqueia() throws Exception {
        errarNaApi("api.depois", LIMITE);

        usuario("api.depois", PerfilUsuario.SOLICITANTE);

        entrarNaApi("api.depois", SENHA).andExpect(status().isUnauthorized());
    }

    /** Um contador só: 3 falhas pela tela e 2 pela API bloqueiam o login nas duas portas. */
    @Test
    void falhasNaTelaENaApiSomamNoMesmoContador() throws Exception {
        usuario("api.junto", PerfilUsuario.SOLICITANTE);
        for (int i = 0; i < 3; i++) {
            entrarNaTela("api.junto", SENHA_ERRADA).andExpect(redirectedUrl("/login?erro"));
        }
        errarNaApi("api.junto", 2);

        entrarNaApi("api.junto", SENHA).andExpect(status().isUnauthorized());
        entrarNaTela("api.junto", SENHA).andExpect(redirectedUrl("/login?erro"));
    }

    /** O contrário: as falhas pela API também bloqueiam a tela. */
    @Test
    void falhasNaApiBloqueiamATela() throws Exception {
        usuario("api.tela", PerfilUsuario.SOLICITANTE);
        errarNaApi("api.tela", LIMITE);

        entrarNaTela("api.tela", SENHA).andExpect(redirectedUrl("/login?erro"));
    }

    /** O contador é por e-mail normalizado: "ANA@..." e "ana@..." são a mesma conta. */
    @Test
    void emailComCaixaDiferenteContaParaOMesmoLimite() throws Exception {
        usuario("api.caixa", PerfilUsuario.SOLICITANTE);
        String maiusculas = "{\"login\":\"API.CAIXA@EXEMPLO.COM\",\"senha\":\"" + SENHA_ERRADA + "\"}";
        for (int i = 0; i < LIMITE; i++) {
            enviar(null, "/api/auth/login", maiusculas).andExpect(status().isUnauthorized());
        }

        entrarNaApi("api.caixa", SENHA).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- apoio

    private ResultActions entrarNaApi(String login, String senha) throws Exception {
        return enviar(null, "/api/auth/login", "{\"login\":\"" + email(login) + "\",\"senha\":\"" + senha + "\"}");
    }

    private void errarNaApi(String login, int vezes) throws Exception {
        for (int i = 0; i < vezes; i++) {
            entrarNaApi(login, SENHA_ERRADA).andExpect(status().isUnauthorized());
        }
    }

    private ResultActions entrarNaTela(String login, String senha) throws Exception {
        return mvc.perform(post("/login").param("login", email(login)).param("senha", senha).with(csrf()));
    }
}
