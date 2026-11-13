package br.com.senac.recursos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.senac.recursos.converter.UsuarioConverter;
import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Administrador inicial (R7 da spec de login por e-mail): o login é e-mail, validado com a mesma regra do cadastro. */
class UsuarioServiceTest {

    private UsuarioRepository repositorio;
    private UsuarioService servico;

    @BeforeEach
    void montar() {
        repositorio = mock(UsuarioRepository.class);
        PasswordEncoder codificador = mock(PasswordEncoder.class);
        when(codificador.encode(any())).thenReturn("hash-de-teste");
        servico = new UsuarioService(repositorio, codificador, new UsuarioConverter());
    }

    @Test
    void administradorInicialGuardaOEmailNormalizado() {
        boolean criou = servico.garantirAdministrador("Administrador", "  Chefe@Exemplo.COM ", "senha-do-chefe-123");

        assertThat(criou).isTrue();
        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).save(salvo.capture());
        assertThat(salvo.getValue().getLogin()).isEqualTo("chefe@exemplo.com");
        assertThat(salvo.getValue().getPerfil()).isEqualTo(PerfilUsuario.ADMINISTRADOR);
    }

    @Test
    void administradorInicialJaExistenteNaoMudaNada() {
        when(repositorio.existsByLogin("chefe@exemplo.com")).thenReturn(true);

        assertThat(servico.garantirAdministrador("Administrador", "CHEFE@exemplo.com", "outra-senha-123")).isFalse();

        verify(repositorio, never()).save(any());
    }

    @Test
    void adminLoginQueNaoEEmailInterrompeAsubidaComMensagemClaraSemEcoarASenha() {
        assertThatThrownBy(() -> servico.garantirAdministrador("Administrador", "admin", "senha-do-chefe-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ADMIN_LOGIN")
                .hasMessageContaining("e-mail")
                .hasMessageNotContaining("senha-do-chefe-123");

        verify(repositorio, never()).save(any());
    }

    @Test
    void adminLoginComEmojiOuSemDominioTambemEhRecusado() {
        assertThatThrownBy(() -> servico.garantirAdministrador("Administrador", "chefe@exemplo", "senha-do-chefe-123"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servico.garantirAdministrador("Administrador", "chefe😀@exemplo.com",
                "senha-do-chefe-123")).isInstanceOf(IllegalArgumentException.class);
    }
}
