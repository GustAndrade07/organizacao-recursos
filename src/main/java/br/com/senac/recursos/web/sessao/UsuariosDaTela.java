package br.com.senac.recursos.web.sessao;

import br.com.senac.recursos.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carrega o usuário do login da tela (RF-01). Mesma tabela e mesmo hash BCrypt da API. Login inexistente e senha
 * errada dão a mesma resposta ao usuário ({@code /login?erro}), e o {@code DaoAuthenticationProvider} confere um hash
 * também quando o login não existe, para o tempo de resposta não revelar quais logins existem.
 */
@Service
public class UsuariosDaTela implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public UsuariosDaTela(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) {
        return usuarios.findByLogin(login == null ? "" : login.trim())
                .map(UsuarioDaSessao::de)
                .orElseThrow(() -> new UsernameNotFoundException("Login ou senha incorretos."));
    }
}
