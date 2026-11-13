package br.com.senac.recursos.web.sessao;

import br.com.senac.recursos.service.TentativasDeLogin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.senac.recursos.support.RelogioDeTeste;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * O provedor com limite, isolado: o provedor de senha é um dublê que aceita só a senha "certa" e conta quantas vezes
 * foi chamado. Prova que o bloqueado recebe a mesma exceção de uma senha errada e que a senha é conferida mesmo
 * bloqueado (o tempo de resposta não revela o bloqueio).
 */
class LoginComLimiteTest {

    private final RelogioDeTeste relogio = new RelogioDeTeste(Instant.parse("2026-11-01T12:00:00Z"));
    private final TentativasDeLogin tentativas = new TentativasDeLogin(relogio, 5, Duration.ofMinutes(15));
    private final SenhaDeTeste senhas = new SenhaDeTeste();
    private final LoginComLimite login = new LoginComLimite(senhas, tentativas);

    @Test
    void senhaCertaEntraEZeraOContador() {
        errar(4);

        Authentication resultado = login.authenticate(pedido("certa"));

        assertThat(resultado.isAuthenticated()).isTrue();
        errar(4);
        assertThat(tentativas.bloqueado("ana")).isFalse();
    }

    @Test
    void falhaContaParaOLoginDigitado() {
        errar(5);

        assertThat(tentativas.bloqueado("ana")).isTrue();
    }

    @Test
    void bloqueadoRecebeAMesmaExcecaoDeSenhaErradaMesmoComASenhaCertaEConfereASenhaDoMesmoJeito() {
        errar(5);
        int chamadasAntes = senhas.chamadas.get();

        assertThatThrownBy(() -> login.authenticate(pedido("certa")))
                .isExactlyInstanceOf(BadCredentialsException.class)
                .hasMessage("E-mail ou senha incorretos.");
        assertThatThrownBy(() -> login.authenticate(pedido("errada")))
                .isExactlyInstanceOf(BadCredentialsException.class)
                .hasMessage("E-mail ou senha incorretos.");

        assertThat(senhas.chamadas.get()).isEqualTo(chamadasAntes + 2);
    }

    @Test
    void suportaOMesmoTipoDePedidoQueOProvedorDeSenha() {
        assertThat(login.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
        assertThat(login.supports(TestingAuthenticationToken.class)).isFalse();
    }

    private void errar(int vezes) {
        for (int i = 0; i < vezes; i++) {
            assertThatThrownBy(() -> login.authenticate(pedido("errada")))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    private static Authentication pedido(String senha) {
        return UsernamePasswordAuthenticationToken.unauthenticated("ana", senha);
    }

    /** Dublê do DaoAuthenticationProvider: aceita só "certa", com a mesma mensagem da tela na recusa. */
    private static final class SenhaDeTeste implements AuthenticationProvider {

        final AtomicInteger chamadas = new AtomicInteger();

        @Override
        public Authentication authenticate(Authentication pedido) {
            chamadas.incrementAndGet();
            if (!"certa".equals(pedido.getCredentials())) {
                throw new BadCredentialsException("E-mail ou senha incorretos.");
            }
            return UsernamePasswordAuthenticationToken.authenticated(pedido.getName(), null, List.of());
        }

        @Override
        public boolean supports(Class<?> tipo) {
            return UsernamePasswordAuthenticationToken.class.isAssignableFrom(tipo);
        }
    }
}
