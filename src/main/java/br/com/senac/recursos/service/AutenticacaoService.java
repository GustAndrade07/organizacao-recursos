package br.com.senac.recursos.service;

import br.com.senac.recursos.dto.request.LoginRequest;
import br.com.senac.recursos.dto.response.TokenResponse;
import br.com.senac.recursos.exception.CredenciaisInvalidasException;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.repository.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login (RF-01): confere login e senha e devolve o token, com o limite de tentativas de {@link TentativasDeLogin}
 * (o mesmo contador do login das telas, ADR-0009).
 */
@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificadorDeSenha;
    private final TokenService tokens;
    private final TentativasDeLogin tentativas;
    private final String hashFalso;

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder codificadorDeSenha, TokenService tokens,
            TentativasDeLogin tentativas) {
        this.usuarios = usuarios;
        this.codificadorDeSenha = codificadorDeSenha;
        this.tokens = tokens;
        this.tentativas = tentativas;
        // Valor aleatório a cada subida: não é senha de ninguém e não fica escrito no código.
        this.hashFalso = codificadorDeSenha.encode(UUID.randomUUID().toString());
    }

    /**
     * Login inexistente, senha errada e login bloqueado dão a mesma resposta e gastam o mesmo tempo (o hash é
     * conferido nos três casos), para a API não revelar quais logins existem nem quais estão bloqueados. A falha conta
     * para o login digitado, exista ele ou não; a tentativa durante o bloqueio não conta (não estende o bloqueio); o
     * sucesso zera o contador.
     */
    @Transactional(readOnly = true)
    public TokenResponse entrar(LoginRequest pedido) {
        Optional<Usuario> usuario = usuarios.findByLogin(pedido.login());
        String hash = usuario.map(Usuario::getSenhaHash).orElse(hashFalso);
        boolean senhaConfere = codificadorDeSenha.matches(pedido.senha(), hash);
        if (tentativas.bloqueado(pedido.login())) {
            throw new CredenciaisInvalidasException();
        }
        if (usuario.isEmpty() || !senhaConfere) {
            tentativas.falhou(pedido.login());
            throw new CredenciaisInvalidasException();
        }
        tentativas.entrou(pedido.login());
        return tokens.emitir(usuario.get());
    }
}
