package br.com.senac.recursos.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.senac.recursos.dto.request.LoginRequest;
import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.exception.CredenciaisInvalidasException;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.repository.UsuarioRepository;
import br.com.senac.recursos.support.RelogioDeTeste;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Login da API (DEF-010) sem banco: só entra quem tem e-mail de login. Conta antiga com login que não é e-mail
 * (resto de banco de desenvolvimento, que a V2 deixa de propósito) recebe o mesmo 401 de senha errada, sem sequer ser
 * procurada, mesmo que a senha esteja certa.
 */
class AutenticacaoServiceTest {

    private UsuarioRepository repositorio;
    private PasswordEncoder codificador;
    private AutenticacaoService servico;

    @BeforeEach
    void montar() {
        repositorio = mock(UsuarioRepository.class);
        codificador = mock(PasswordEncoder.class);
        when(codificador.encode(any())).thenReturn("hash-falso");
        when(codificador.matches(any(), any())).thenReturn(true);
        TentativasDeLogin tentativas = new TentativasDeLogin(new RelogioDeTeste(Instant.parse("2026-11-01T12:00:00Z")),
                5, Duration.ofMinutes(15));
        servico = new AutenticacaoService(repositorio, codificador, mock(TokenService.class), tentativas);
    }

    @Test
    void loginQueNaoEEmailNaoEhProcuradoNemComSenhaCerta() {
        when(repositorio.findByLogin("admin")).thenReturn(
                Optional.of(new Usuario("Administrador", "admin", "hash", PerfilUsuario.ADMINISTRADOR)));

        assertThatThrownBy(() -> servico.entrar(new LoginRequest("admin", "senha-certa-123")))
                .isInstanceOf(CredenciaisInvalidasException.class);

        verify(repositorio, never()).findByLogin(any());
        verify(codificador).matches(any(), any());
    }
}
