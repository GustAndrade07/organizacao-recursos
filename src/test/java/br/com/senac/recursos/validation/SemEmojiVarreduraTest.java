package br.com.senac.recursos.validation;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.senac.recursos.dto.request.UsuarioRequest;
import br.com.senac.recursos.enums.PerfilUsuario;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * DEF-005 / regra 10 da seção 9 do AGENTS.md: os campos de texto de {@link UsuarioRequest} são descobertos por
 * reflexão, sem lista de nomes. Para cada campo {@code String} (menos a senha, que não é texto de cadastro: a regra
 * dela está em aberto no DEF-005) o teste põe um emoji num pedido válido e exige a recusa daquele campo. Um campo de
 * texto novo no cadastro entra na varredura sozinho e falha se esquecer o {@code @SemEmoji}.
 */
class SemEmojiVarreduraTest {

    private static final String EMOJI = "😀";

    private static Object[] pedidoValido() {
        return new Object[] {"Ana Souza", "ana@exemplo.com", "senha-de-teste-123", PerfilUsuario.SOLICITANTE};
    }

    @Test
    void todoCampoDeTextoDoCadastroDeUsuarioRecusaEmoji() throws Exception {
        RecordComponent[] componentes = UsuarioRequest.class.getRecordComponents();
        Constructor<UsuarioRequest> construtor = UsuarioRequest.class.getDeclaredConstructor(
                Arrays.stream(componentes).map(RecordComponent::getType).toArray(Class<?>[]::new));
        Object[] base = pedidoValido();
        assertThat(componentes).as("a ordem de pedidoValido() acompanha o record").hasSize(base.length);

        List<String> camposVarridos = new ArrayList<>();
        try (ValidatorFactory fabrica = Validation.buildDefaultValidatorFactory()) {
            Validator validador = fabrica.getValidator();
            assertThat(validador.validate(construtor.newInstance(base))).as("o pedido-base é válido").isEmpty();

            for (int i = 0; i < componentes.length; i++) {
                RecordComponent campo = componentes[i];
                if (campo.getType() != String.class || campo.getName().equals("senha")) {
                    continue;
                }
                camposVarridos.add(campo.getName());
                Object[] valores = base.clone();
                valores[i] = ((String) base[i]).replaceFirst("^(\\S{2})", "$1" + EMOJI);
                var violacoes = validador.validate(construtor.newInstance(valores));
                assertThat(violacoes).as("emoji em '%s'", campo.getName())
                        .anyMatch(v -> v.getPropertyPath().toString().equals(campo.getName())
                                && v.getMessage().equals("Não use emojis neste campo."));
            }
        }
        assertThat(camposVarridos).as("a varredura achou os campos de texto").contains("nome", "login");
    }
}
