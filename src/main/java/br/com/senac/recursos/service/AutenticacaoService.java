package br.com.senac.recursos.service;

import br.com.senac.recursos.dto.request.LoginRequest;
import br.com.senac.recursos.dto.response.TokenResponse;
import br.com.senac.recursos.exception.CredenciaisInvalidasException;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.repository.UsuarioRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login (RF-01): confere login e senha e devolve o token. */
@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificadorDeSenha;
    private final TokenService tokens;
    private final String hashFalso;

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder codificadorDeSenha, TokenService tokens) {
        this.usuarios = usuarios;
        this.codificadorDeSenha = codificadorDeSenha;
        this.tokens = tokens;
        this.hashFalso = codificadorDeSenha.encode("senha-que-nao-pertence-a-ninguem");
    }

    /**
     * Login inexistente e senha errada dão a mesma resposta e gastam o mesmo tempo (o hash é conferido nos dois
     * casos), para a API não revelar quais logins existem.
     */
    @Transactional(readOnly = true)
    public TokenResponse entrar(LoginRequest pedido) {
        Optional<Usuario> usuario = usuarios.findByLogin(pedido.login());
        String hash = usuario.map(Usuario::getSenhaHash).orElse(hashFalso);
        boolean senhaConfere = codificadorDeSenha.matches(pedido.senha(), hash);
        if (usuario.isEmpty() || !senhaConfere) {
            throw new CredenciaisInvalidasException();
        }
        return tokens.emitir(usuario.get());
    }
}
