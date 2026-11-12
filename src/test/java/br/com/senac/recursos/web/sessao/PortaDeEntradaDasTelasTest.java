package br.com.senac.recursos.web.sessao;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

/**
 * Para onde a tela manda quem não está logado (o "401" das telas, D12): o login, com {@code ?expirou} só quando o
 * navegador mandou um cookie de sessão que não vale mais, e com {@code ?destino=} só no {@code GET} e só para caminho
 * deste site.
 */
class PortaDeEntradaDasTelasTest {

    private final PortaDeEntradaDasTelas porta = new PortaDeEntradaDasTelas();

    private String redirecionamento(MockHttpServletRequest requisicao) throws Exception {
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        porta.commence(requisicao, resposta, new InsufficientAuthenticationException("sem sessão"));
        return resposta.getRedirectedUrl();
    }

    @Test
    void semCookieDeSessaoVaiAoLoginComODestino() throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/reservas/minhas");
        requisicao.setQueryString("estado=aprovada");

        assertThat(redirecionamento(requisicao)).isEqualTo("/login?destino=%2Freservas%2Fminhas%3Festado%3Daprovada");
    }

    @Test
    void cookieDeSessaoQueNaoValeMaisAvisaQueASessaoExpirou() throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/aprovacoes");
        requisicao.setCookies(new Cookie("JSESSIONID", "sessao-antiga"));
        requisicao.setRequestedSessionIdValid(false);

        assertThat(redirecionamento(requisicao)).isEqualTo("/login?expirou&destino=%2Faprovacoes");
    }

    @Test
    void cookieDeSessaoValidoNaoEhSessaoExpirada() {
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/aprovacoes");
        requisicao.setCookies(new Cookie("JSESSIONID", "sessao-viva"));
        requisicao.setRequestedSessionIdValid(true);

        assertThat(PortaDeEntradaDasTelas.sessaoVencida(requisicao)).isFalse();
    }

    @Test
    void postNaoLevaDestinoEDestinoDeOutroSiteNaoEntra() throws Exception {
        assertThat(redirecionamento(new MockHttpServletRequest("POST", "/reservas"))).isEqualTo("/login");
        assertThat(redirecionamento(new MockHttpServletRequest("GET", "//evil.example"))).isEqualTo("/login");
    }
}
