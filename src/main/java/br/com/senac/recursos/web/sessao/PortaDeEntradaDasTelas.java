package br.com.senac.recursos.web.sessao;

import br.com.senac.recursos.web.CaminhoDoSite;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Quem abre uma tela sem estar logado vai para o login (o "401" da tela). Se o navegador mandou um cookie de sessão que
 * não vale mais, a tela de login diz "Sua sessão expirou" ({@code ?expirou}). Num {@code GET}, a página pedida vai em
 * {@code ?destino=}, só se for caminho deste site ({@link CaminhoDoSite}); depois do login o usuário volta para ela.
 */
public class PortaDeEntradaDasTelas implements AuthenticationEntryPoint {

    static final String COOKIE_DE_SESSAO = "JSESSIONID";

    @Override
    public void commence(HttpServletRequest requisicao, HttpServletResponse resposta, AuthenticationException erro)
            throws IOException {
        StringBuilder url = new StringBuilder(requisicao.getContextPath()).append("/login");
        boolean sessaoExpirada = sessaoVencida(requisicao);
        String separador = "?";
        if (sessaoExpirada) {
            url.append(separador).append("expirou");
            separador = "&";
        }
        String destino = destinoDe(requisicao);
        if (destino != null) {
            url.append(separador).append("destino=").append(URLEncoder.encode(destino, StandardCharsets.UTF_8));
        }
        resposta.sendRedirect(url.toString());
    }

    /**
     * O navegador mandou o cookie de sessão, mas a sessão não existe mais (venceu ou o servidor reiniciou). Lê o
     * cookie em vez de {@code getRequestedSessionId()}: o identificador nunca é usado, só a presença do cookie, para
     * escolher a mensagem da tela de login (regra java:S2254 do SonarCloud).
     */
    static boolean sessaoVencida(HttpServletRequest requisicao) {
        Cookie[] cookies = requisicao.getCookies();
        boolean mandouCookie = cookies != null
                && Arrays.stream(cookies).anyMatch(cookie -> COOKIE_DE_SESSAO.equals(cookie.getName()));
        return mandouCookie && !requisicao.isRequestedSessionIdValid();
    }

    static String destinoDe(HttpServletRequest requisicao) {
        if (!"GET".equals(requisicao.getMethod())) {
            return null;
        }
        String caminho = requisicao.getRequestURI().substring(requisicao.getContextPath().length());
        String consulta = requisicao.getQueryString();
        String destino = consulta == null ? caminho : caminho + "?" + consulta;
        return CaminhoDoSite.ouNulo(destino);
    }
}
