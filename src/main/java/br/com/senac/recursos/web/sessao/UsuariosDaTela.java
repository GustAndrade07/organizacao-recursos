package br.com.senac.recursos.web.sessao;

import br.com.senac.recursos.repository.UsuarioRepository;
import br.com.senac.recursos.validation.Emails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carrega o usuário pelo e-mail digitado na tela (RF-01, DEF-010), sem diferenciar maiúsculas nem espaços nas pontas. Mesma tabela e mesmo hash BCrypt da API. E-mail inexistente e senha
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
        // Login que não é e-mail (conta antiga de banco de desenvolvimento) nem é procurado: mesma resposta de senha errada.
        String email = Emails.normalizar(login);
        return (Emails.valido(email) ? usuarios.findByLogin(email) : java.util.Optional.<br.com.senac.recursos.model.Usuario>empty())
                .map(UsuarioDaSessao::de)
                .orElseThrow(() -> new UsernameNotFoundException("E-mail ou senha incorretos."));
    }
}
