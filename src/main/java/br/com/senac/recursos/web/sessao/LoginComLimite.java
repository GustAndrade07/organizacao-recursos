package br.com.senac.recursos.web.sessao;

import br.com.senac.recursos.service.TentativasDeLogin;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Autenticação do {@code POST /login} das telas com o limite de {@link TentativasDeLogin}. Envolve o provedor que
 * confere login e senha (o {@code DaoAuthenticationProvider} com {@link UsuariosDaTela} e BCrypt):
 * <ul>
 *   <li>login bloqueado: a senha é conferida do mesmo jeito (o tempo de resposta não revela o bloqueio), o resultado é
 *       descartado e sai a mesma {@link BadCredentialsException} de uma senha errada, que leva a {@code /login?erro};
 *       a tentativa não conta nem estende o bloqueio;</li>
 *   <li>falha: conta para o login digitado, exista ele ou não;</li>
 *   <li>sucesso: zera o contador.</li>
 * </ul>
 * Tem de ser o único provedor de um {@code ProviderManager} sem pai: com o gerenciador global como pai, o Spring
 * tentaria de novo no pai depois da recusa, e a senha certa entraria durante o bloqueio.
 */
public class LoginComLimite implements AuthenticationProvider {

    private static final String MENSAGEM = "Login ou senha incorretos.";

    private final AuthenticationProvider senhas;
    private final TentativasDeLogin tentativas;

    public LoginComLimite(AuthenticationProvider senhas, TentativasDeLogin tentativas) {
        this.senhas = senhas;
        this.tentativas = tentativas;
    }

    @Override
    public Authentication authenticate(Authentication pedido) {
        String login = pedido.getName();
        if (tentativas.bloqueado(login)) {
            conferirSoPeloTempo(pedido);
            throw new BadCredentialsException(MENSAGEM);
        }
        try {
            Authentication autenticado = senhas.authenticate(pedido);
            tentativas.entrou(login);
            return autenticado;
        } catch (AuthenticationException falha) {
            tentativas.falhou(login);
            throw falha;
        }
    }

    @Override
    public boolean supports(Class<?> tipo) {
        return senhas.supports(tipo);
    }

    private void conferirSoPeloTempo(Authentication pedido) {
        try {
            senhas.authenticate(pedido);
        } catch (AuthenticationException ignorada) {
            // o resultado não importa: o login está bloqueado de qualquer jeito
        }
    }
}
