package br.com.senac.recursos.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.senac.recursos.enums.PerfilUsuario;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Cadastro (DEF-010): nome completo, e-mail e senha, mais o perfil. O pedido normaliza o que recebe (e-mail em
 * minúsculas e sem espaços nas pontas, nome sem espaços repetidos) para a API e a tela terem o mesmo comportamento,
 * e o {@code LoginRequest} faz o mesmo com o e-mail de entrada.
 */
class UsuarioRequestTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void encerrar() {
        fabrica.close();
    }

    private static Map<String, String> erros(UsuarioRequest pedido) {
        Set<ConstraintViolation<UsuarioRequest>> violacoes = validador.validate(pedido);
        return violacoes.stream().collect(Collectors.toMap(v -> v.getPropertyPath().toString(),
                ConstraintViolation::getMessage, (a, b) -> a + " | " + b));
    }

    @Test
    void pedidoCompletoEhValidoENormalizaEmailENome() {
        var pedido = new UsuarioRequest("  Ana   Souza ", "  ANA.Souza@Exemplo.COM ", "senha-de-teste-123",
                PerfilUsuario.SOLICITANTE);

        assertThat(pedido.nome()).isEqualTo("Ana Souza");
        assertThat(pedido.login()).isEqualTo("ana.souza@exemplo.com");
        assertThat(erros(pedido)).isEmpty();
    }

    @Test
    void camposObrigatoriosTemMensagemPropria() {
        var pedido = new UsuarioRequest("  ", null, null, null);

        assertThat(erros(pedido)).containsOnlyKeys("nome", "login", "senha", "perfil")
                .containsEntry("nome", "Informe o nome completo.")
                .containsEntry("login", "Informe o e-mail.")
                .containsEntry("senha", "Informe a senha.")
                .containsEntry("perfil", "Informe o perfil.");
    }

    @Test
    void senhaContinuaDe8a72CaracteresESemOutroCampoDeSenha() {
        assertThat(erros(new UsuarioRequest("Ana Souza", "ana@exemplo.com", "curta", PerfilUsuario.SOLICITANTE)))
                .containsOnlyKeys("senha").containsEntry("senha", "A senha deve ter de 8 a 72 caracteres.");
        assertThat(erros(new UsuarioRequest("Ana Souza", "ana@exemplo.com", "x".repeat(73), PerfilUsuario.SOLICITANTE)))
                .containsOnlyKeys("senha");
        assertThat(UsuarioRequest.class.getRecordComponents()).extracting(c -> c.getName())
                .containsExactly("nome", "login", "senha", "perfil");
    }

    @Test
    void nomeEEmailRuinsTemCadaUmASuaMensagem() {
        var pedido = new UsuarioRequest("Ana", "ana@exemplo", "senha-de-teste-123", PerfilUsuario.RESPONSAVEL);

        assertThat(erros(pedido))
                .containsEntry("login", "Informe um e-mail válido, como nome@dominio.com.")
                .hasEntrySatisfying("nome", mensagem -> assertThat(mensagem).startsWith("Informe o nome completo"));
    }

    @Test
    void toStringNaoMostraASenha() {
        var pedido = new UsuarioRequest("Ana Souza", "ana@exemplo.com", "senha-secreta-123", PerfilUsuario.SOLICITANTE);

        assertThat(pedido.toString()).doesNotContain("senha-secreta-123").contains("senha=***");
    }

    @Test
    void loginRequestNormalizaOEmailDeEntradaEMantemASenhaComoVeio() {
        var pedido = new LoginRequest("  ANA@Exemplo.com ", " Senha Com Espaço ");

        assertThat(pedido.login()).isEqualTo("ana@exemplo.com");
        assertThat(pedido.senha()).isEqualTo(" Senha Com Espaço ");
        assertThat(pedido.toString()).doesNotContain("Senha Com").contains("senha=***");
    }

    @Test
    void loginRequestSoExigePreenchimentoEFormatoRuimNaoViraErroDeValidacao() {
        var vazio = validador.validate(new LoginRequest("  ", ""));
        var semArroba = validador.validate(new LoginRequest("ana", "qualquer"));

        assertThat(vazio).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder("Informe o e-mail.", "Informe a senha.");
        // Formato errado dá o mesmo 401 de senha errada (AutenticacaoApiControllerTest), nunca um 400 que diferencie.
        assertThat(semArroba).isEmpty();
    }
}
