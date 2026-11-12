package br.com.senac.recursos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Validações de configuração que impedem a aplicação de subir num estado inseguro. */
class ConfiguracaoTest {

    private final SegurancaConfig seguranca = new SegurancaConfig();

    @Test
    void segredoCurtoDemaisImpedeOStart() {
        JwtPropriedades curto = new JwtPropriedades("curto", null, null);
        assertThatThrownBy(() -> seguranca.chaveDoToken(curto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void semSegredoGeraChaveAleatoriaDiferenteACadaStart() {
        JwtPropriedades vazio = new JwtPropriedades("", null, null);
        assertThat(seguranca.chaveDoToken(vazio).getEncoded())
                .hasSize(32)
                .isNotEqualTo(seguranca.chaveDoToken(vazio).getEncoded());
    }

    @Test
    void segredoValidoViraChaveHmac() {
        JwtPropriedades valido = new JwtPropriedades("x".repeat(32), "emissor", Duration.ofMinutes(5));
        assertThat(seguranca.chaveDoToken(valido).getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    void padroesEValidadeNaoPositivaRecusada() {
        JwtPropriedades padrao = new JwtPropriedades(null, " ", null);
        assertThat(padrao.emissor()).isEqualTo("organizacao-recursos");
        assertThat(padrao.validade()).isEqualTo(Duration.ofHours(1));
        assertThat(padrao.toString()).contains("segredo=***");
        assertThatThrownBy(() -> new JwtPropriedades("s", "e", Duration.ZERO)).isInstanceOf(IllegalStateException.class);
    }
}
