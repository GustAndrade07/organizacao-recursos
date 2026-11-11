package br.com.senac.recursos.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do token (ADR-0001), em {@code recursos.seguranca.jwt}.
 *
 * @param segredo chave HS256 com pelo menos 32 bytes, só por variável de ambiente ({@code JWT_SECRET}); vazia, a
 *     aplicação gera uma chave aleatória a cada start
 * @param emissor valor da claim {@code iss}, conferido na validação
 * @param validade tempo de vida do token (padrão {@code PT1H}, proposta do back-end a confirmar com o grupo)
 */
@ConfigurationProperties("recursos.seguranca.jwt")
public record JwtPropriedades(String segredo, String emissor, Duration validade) {

    public JwtPropriedades {
        emissor = emissor == null || emissor.isBlank() ? "organizacao-recursos" : emissor;
        validade = validade == null ? Duration.ofHours(1) : validade;
        if (validade.isNegative() || validade.isZero()) {
            throw new IllegalStateException("recursos.seguranca.jwt.validade precisa ser positiva.");
        }
    }

    /** Não imprime o segredo em log. */
    @Override
    public String toString() {
        return "JwtPropriedades[segredo=***, emissor=" + emissor + ", validade=" + validade + "]";
    }
}
