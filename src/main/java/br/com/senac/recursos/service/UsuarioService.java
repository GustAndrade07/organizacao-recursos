package br.com.senac.recursos.service;

import br.com.senac.recursos.converter.UsuarioConverter;
import br.com.senac.recursos.dto.request.UsuarioRequest;
import br.com.senac.recursos.dto.response.UsuarioResponse;
import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.exception.EntidadeNaoEncontradaException;
import br.com.senac.recursos.exception.LoginEmUsoException;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.repository.UsuarioRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro de usuários pelo Administrador. A senha só é guardada como hash BCrypt. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificadorDeSenha;
    private final UsuarioConverter conversor;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder codificadorDeSenha, UsuarioConverter conversor) {
        this.usuarios = usuarios;
        this.codificadorDeSenha = codificadorDeSenha;
        this.conversor = conversor;
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest pedido) {
        if (usuarios.existsByLogin(pedido.login())) {
            throw new LoginEmUsoException(pedido.login());
        }
        Usuario usuario = new Usuario(pedido.nome().trim(), pedido.login(),
                codificadorDeSenha.encode(pedido.senha()), pedido.perfil());
        try {
            return conversor.paraResposta(usuarios.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException corrida) {
            // Dois cadastros simultâneos com o mesmo login: o UNIQUE do banco decide.
            throw new LoginEmUsoException(pedido.login());
        }
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarios.findAllByOrderByIdAsc().stream().map(conversor::paraResposta).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return usuarios.findById(id).map(conversor::paraResposta)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário", id));
    }

    /**
     * Cria o primeiro Administrador se ainda não existir usuário com esse login. Idempotente: rodar de novo não muda
     * nada, nem a senha de quem já existe.
     *
     * @return {@code true} se criou
     */
    @Transactional
    public boolean garantirAdministrador(String nome, String login, String senha) {
        if (usuarios.existsByLogin(login)) {
            return false;
        }
        usuarios.save(new Usuario(nome, login, codificadorDeSenha.encode(senha), PerfilUsuario.ADMINISTRADOR));
        return true;
    }
}
