package br.com.senac.recursos.config;

import br.com.senac.recursos.converter.UsuarioAutenticadoConverter;
import br.com.senac.recursos.service.TentativasDeLogin;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Duas cadeias de segurança, porque o projeto tem dois públicos (ADR-0008):
 * <ol>
 *   <li><b>API REST ({@code /api/**})</b>: JWT (ADR-0001), stateless, autorização por perfil. As regras de URL abaixo SÃO
 *       a matriz de autorização ({@code docs/seguranca/matriz-autorizacao.md}); {@code MatrizAutorizacaoTest} lê a
 *       matriz e confere cada célula, e falha se surgir rota que não está nela. Rota da API fora da lista é recusada
 *       ({@code denyAll}). Regra de posse (dono da reserva, RN-06) é do serviço.</li>
 *   <li><b>Telas Thymeleaf (todo o resto)</b>: formulário em {@code /login} com sessão do Spring Security e perfil
 *       conferido por rota (D12, opção (a)). Fica em {@link SegurancaDasTelasConfig}.</li>
 * </ol>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtPropriedades.class, AdministradorInicialPropriedades.class})
public class SegurancaConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SegurancaConfig.class);

    private static final int TAMANHO_MINIMO_SEGREDO = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private static final String ADMINISTRADOR = "ADMINISTRADOR";
    private static final String RESPONSAVEL = "RESPONSAVEL";

    @Bean
    @Order(1)
    SecurityFilterChain cadeiaDaApi(HttpSecurity http, JwtAuthenticationConverter conversorDeToken,
            RespostaDeErroDeSeguranca respostaDeErro) throws Exception {
        http
                .securityMatcher("/api/**")
                // API stateless: o token vai no cabeçalho Authorization, nunca em cookie; sem sessão não há o que
                // um site de terceiros forjar (CSRF). Justificativa da S4502 em docs/seguranca/matriz-autorizacao.md.
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR java:S4502 - API stateless com JWT no cabeçalho
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(rota -> rota
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").hasRole(ADMINISTRADOR)

                        .requestMatchers(HttpMethod.GET, "/api/recursos", "/api/recursos/disponiveis",
                                "/api/recursos/*", "/api/recursos/*/manutencoes").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/recursos", "/api/recursos/*/manutencoes")
                        .hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.PUT, "/api/recursos/*").hasRole(ADMINISTRADOR)

                        .requestMatchers(HttpMethod.GET, "/api/reservas/*/historico")
                        .hasAnyRole(RESPONSAVEL, ADMINISTRADOR)
                        .requestMatchers(HttpMethod.GET, "/api/reservas", "/api/reservas/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reservas", "/api/reservas/*/cancelamento")
                        .authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/reservas/*/periodo").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reservas/*/aprovacao", "/api/reservas/*/rejeicao",
                                "/api/reservas/*/retirada", "/api/reservas/*/devolucao",
                                "/api/reservas/*/nao-comparecimento")
                        .hasRole(RESPONSAVEL)

                        .requestMatchers(HttpMethod.GET, "/api/relatorios/utilizacao")
                        .hasAnyRole(RESPONSAVEL, ADMINISTRADOR)

                        .anyRequest().denyAll())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeToken))
                        .authenticationEntryPoint(respostaDeErro)
                        .accessDeniedHandler(respostaDeErro))
                .exceptionHandling(erro -> erro
                        .authenticationEntryPoint(respostaDeErro)
                        .accessDeniedHandler(respostaDeErro));
        return http.build();
    }

    /** A claim {@code perfil} vira a autoridade {@code ROLE_<perfil>}. */
    @Bean
    JwtAuthenticationConverter conversorDeToken() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(UsuarioAutenticadoConverter.CLAIM_PERFIL);
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }

    @Bean
    SecretKey chaveDoToken(JwtPropriedades propriedades) {
        String segredo = propriedades.segredo();
        if (segredo == null || segredo.isBlank()) {
            LOG.warn("JWT_SECRET não definido: usando chave aleatória; os tokens deixam de valer quando a aplicação "
                    + "reinicia. Defina JWT_SECRET fora do ambiente de desenvolvimento.");
            byte[] aleatoria = new byte[TAMANHO_MINIMO_SEGREDO];
            ALEATORIO.nextBytes(aleatoria);
            return new SecretKeySpec(aleatoria, "HmacSHA256");
        }
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos " + TAMANHO_MINIMO_SEGREDO + " bytes.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder codificadorDeToken(SecretKey chave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    /**
     * Valida assinatura, emissor e validade com o {@link Clock} injetado e sem tolerância: o teste de token expirado
     * avança o relógio, sem {@code Thread.sleep}.
     */
    @Bean
    JwtDecoder decodificadorDeToken(SecretKey chave, JwtPropriedades propriedades, Clock relogio) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator validade = new JwtTimestampValidator(Duration.ZERO);
        validade.setClock(relogio);
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validade,
                new JwtIssuerValidator(propriedades.emissor())));
        return decodificador;
    }

    @Bean
    PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Contador de falhas de login (ADR-0009, ressalva 1 do PR #48), o mesmo para a API ({@code AutenticacaoService})
     * e para as telas ({@code SegurancaDasTelasConfig}): falhar numa porta conta na outra. Em memória desta instância.
     */
    @Bean
    TentativasDeLogin tentativasDeLogin(Clock relogio,
            @Value("${recursos.seguranca.login.tentativas:5}") int tentativas,
            @Value("${recursos.seguranca.login.bloqueio:PT15M}") Duration bloqueio) {
        return new TentativasDeLogin(relogio, tentativas, bloqueio);
    }
}
