package br.com.senac.recursos.web.sessao;

import br.com.senac.recursos.web.CaminhoDoSite;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

/**
 * O que acontece depois do {@code POST /login} (contrato em docs/frontend/README.md, "Login"):
 * <ul>
 *   <li>sucesso: vai para o {@code destino} do formulário, <b>revalidado</b> com {@link CaminhoDoSite} (o campo oculto
 *       é editável pelo cliente); sem destino válido, para {@code /}, que escolhe a tela inicial do perfil;</li>
 *   <li>falha: volta para {@code /login?erro}, repete o {@code destino} na URL e devolve o login digitado por atributo
 *       flash (nunca na URL). A senha nunca volta, e a mensagem não diz qual dos dois errou.</li>
 * </ul>
 */
public final class ResultadoDoLogin {

    static final String CAMPO_DESTINO = "destino";
    static final String CAMPO_LOGIN = "login";
    private static final SessionFlashMapManager FLASH = new SessionFlashMapManager();

    private ResultadoDoLogin() {
    }

    public static AuthenticationSuccessHandler sucesso() {
        return (HttpServletRequest requisicao, HttpServletResponse resposta, Authentication autenticacao) -> {
            String destino = CaminhoDoSite.ouNulo(requisicao.getParameter(CAMPO_DESTINO));
            resposta.sendRedirect(requisicao.getContextPath() + (destino == null ? "/" : destino));
        };
    }

    public static AuthenticationFailureHandler falha() {
        return (HttpServletRequest requisicao, HttpServletResponse resposta, AuthenticationException erro) ->
                redirecionarComErro(requisicao, resposta);
    }

    private static void redirecionarComErro(HttpServletRequest requisicao, HttpServletResponse resposta)
            throws IOException {
        String login = requisicao.getParameter(CAMPO_LOGIN);
        if (login != null && !login.isBlank()) {
            FlashMap flash = new FlashMap();
            flash.put(CAMPO_LOGIN, login.trim());
            flash.setTargetRequestPath(requisicao.getContextPath() + "/login");
            // O filtro de login roda antes do DispatcherServlet, então o gerenciador de flash é criado aqui; é o mesmo
            // tipo que o Spring MVC usa por padrão (guarda na sessão até o próximo GET /login).
            FLASH.saveOutputFlashMap(flash, requisicao, resposta);
        }
        String destino = CaminhoDoSite.ouNulo(requisicao.getParameter(CAMPO_DESTINO));
        String url = requisicao.getContextPath() + "/login?erro"
                + (destino == null ? "" : "&destino=" + URLEncoder.encode(destino, StandardCharsets.UTF_8));
        resposta.sendRedirect(url);
    }
}
