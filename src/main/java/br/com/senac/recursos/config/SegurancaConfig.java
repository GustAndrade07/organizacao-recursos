package br.com.senac.recursos.config;

import br.com.senac.recursos.converter.UsuarioAutenticadoConverter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * Autenticação por JWT (ADR-0001) e autorização por perfil. As regras de URL abaixo SÃO a matriz de autorização
 * ({@code docs/seguranca/matriz-autorizacao.md}); {@code MatrizAutorizacaoTest} lê a matriz e confere cada célula, e
 * falha se surgir rota que não está nela. Regra de posse (dono da reserva, RN-06) é do serviço.
 *
 * <p>Rota fora da lista é recusada ({@code denyAll}): esquecer de cadastrar uma rota fecha o acesso em vez de abrir.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtPropriedades.class, AdministradorInicialPropriedades.class})
public class SegurancaConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SegurancaConfig.class);

    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    private static final String ADMINISTRADOR = "ADMINISTRADOR";
    private static final String RESPONSAVEL = "RESPONSAVEL";

    @Bean
    SecurityFilterChain cadeiaDeSeguranca(HttpSecurity http, JwtAuthenticationConverter conversorDeToken,
            RespostaDeErroDeSeguranca respostaDeErro) throws Exception {
        http
                // API stateless: o token vai no cabeçalho Authorization, nunca em cookie; sem sessão não há o que
                // um site de terceiros forjar (CSRF). Justificativa do hotspot em docs/seguranca/matriz-autorizacao.md.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(rota -> rota
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()

                        .requestMatchers(HttpMethod.GET, "/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/usuarios").hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.POST, "/usuarios").hasRole(ADMINISTRADOR)

                        .requestMatchers(HttpMethod.GET, "/recursos", "/recursos/disponiveis", "/recursos/*",
                                "/recursos/*/manutencoes").authenticated()
                        .requestMatchers(HttpMethod.POST, "/recursos", "/recursos/*/manutencoes").hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.PUT, "/recursos/*").hasRole(ADMINISTRADOR)

                        .requestMatchers(HttpMethod.GET, "/reservas/*/historico").hasAnyRole(RESPONSAVEL, ADMINISTRADOR)
                        .requestMatchers(HttpMethod.GET, "/reservas", "/reservas/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/reservas", "/reservas/*/cancelamento").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/reservas/*/periodo").authenticated()
                        .requestMatchers(HttpMethod.POST, "/reservas/*/aprovacao", "/reservas/*/rejeicao",
                                "/reservas/*/retirada", "/reservas/*/devolucao", "/reservas/*/nao-comparecimento")
                        .hasRole(RESPONSAVEL)

                        .requestMatchers(HttpMethod.GET, "/relatorios/utilizacao").hasAnyRole(RESPONSAVEL, ADMINISTRADOR)

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
            new SecureRandom().nextBytes(aleatoria);
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
}
