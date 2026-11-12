package br.com.senac.recursos.web.sessao;

import br.com.senac.recursos.web.CaminhoDoSite;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Quem abre uma tela sem estar logado vai para o login (o "401" da tela). Se o navegador mandou um cookie de sessão que
 * não vale mais, a tela de login diz "Sua sessão expirou" ({@code ?expirou}). Num {@code GET}, a página pedida vai em
 * {@code ?destino=}, só se for caminho deste site ({@link CaminhoDoSite}); depois do login o usuário volta para ela.
 */
public class PortaDeEntradaDasTelas implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest requisicao, HttpServletResponse resposta, AuthenticationException erro)
            throws IOException {
        StringBuilder url = new StringBuilder(requisicao.getContextPath()).append("/login");
        boolean sessaoExpirada = requisicao.getRequestedSessionId() != null && !requisicao.isRequestedSessionIdValid();
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
