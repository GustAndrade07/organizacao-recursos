package br.com.senac.recursos.web.sessao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** O {@code POST /login} das telas procura o usuário pelo e-mail normalizado (minúsculas, sem espaços nas pontas). */
class UsuariosDaTelaTest {

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final UsuariosDaTela usuarios = new UsuariosDaTela(repositorio);

    @Test
    void emailComCaixaEEspacosDiferentesAcha() {
        when(repositorio.findByLogin("ana@exemplo.com")).thenReturn(
                Optional.of(new Usuario("Ana Souza", "ana@exemplo.com", "hash", PerfilUsuario.SOLICITANTE)));

        assertThat(usuarios.loadUserByUsername("  ANA@Exemplo.com ").getUsername()).isEqualTo("ana@exemplo.com");

        verify(repositorio).findByLogin("ana@exemplo.com");
    }

    @Test
    void emailInexistenteDaAMesmaMensagemDeSenhaErrada() {
        when(repositorio.findByLogin("ninguem@exemplo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarios.loadUserByUsername("ninguem@exemplo.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("E-mail ou senha incorretos.");
        assertThatThrownBy(() -> usuarios.loadUserByUsername(null)).isInstanceOf(UsernameNotFoundException.class);
    }
}
