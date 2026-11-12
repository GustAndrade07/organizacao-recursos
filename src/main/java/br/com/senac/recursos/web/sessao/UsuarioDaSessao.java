package br.com.senac.recursos.web.sessao;

import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.model.Usuario;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Quem entrou pela tela de login (D12, opção (a): sessão do Spring Security). Fica na sessão HTTP, então guarda só o
 * necessário: id, nome, login e perfil. O hash da senha só existe durante a autenticação e é apagado logo depois
 * ({@link #eraseCredentials()}, chamado pelo Spring Security).
 */
public final class UsuarioDaSessao implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String nome;
    private final String login;
    private final PerfilUsuario perfil;
    private String senhaHash;

    public UsuarioDaSessao(Long id, String nome, String login, PerfilUsuario perfil, String senhaHash) {
        this.id = id;
        this.nome = nome;
        this.login = login;
        this.perfil = perfil;
        this.senhaHash = senhaHash;
    }

    static UsuarioDaSessao de(Usuario usuario) {
        return new UsuarioDaSessao(usuario.getId(), usuario.getNome(), usuario.getLogin(), usuario.getPerfil(),
                usuario.getSenhaHash());
    }

    /** O mesmo valor que a API lê do token: os services não sabem se a chamada veio da tela ou da API. */
    public UsuarioAutenticado comoUsuarioAutenticado() {
        return new UsuarioAutenticado(id, login, perfil);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public void eraseCredentials() {
        senhaHash = null;
    }
}
