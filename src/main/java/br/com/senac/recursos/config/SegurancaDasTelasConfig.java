package br.com.senac.recursos.config;

import br.com.senac.recursos.web.sessao.PortaDeEntradaDasTelas;
import br.com.senac.recursos.web.sessao.ResultadoDoLogin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadeia das telas Thymeleaf (D12, opção (a), ADR-0009): formulário em {@code /login} com sessão do Spring Security,
 * CSRF ligado e perfil conferido por rota. As regras de URL abaixo SÃO a tabela "Telas" de
 * {@code docs/seguranca/matriz-autorizacao.md}, conferida célula a célula por {@code MatrizAutorizacaoTest}; espelham
 * a tabela da API (quem não pode chamar a API também não vê a tela). A posse (dono da reserva, RN-06) continua no
 * serviço. Fora da lista, só com login (para a URL inexistente mostrar a página 404); rota nova que não esteja na
 * matriz faz o {@code MatrizAutorizacaoTest} falhar.
 *
 * <ul>
 *   <li>Sem sessão: redireciona para {@code /login} (com {@code ?expirou} se o cookie de sessão venceu e
 *       {@code ?destino=} com a página pedida).</li>
 *   <li>Perfil sem permissão: 403, a página {@code error/403.html}.</li>
 *   <li>{@code POST /logout} encerra a sessão, apaga o cookie e volta ao login.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class SegurancaDasTelasConfig {

    private static final String SOLICITANTE = "SOLICITANTE";
    private static final String RESPONSAVEL = "RESPONSAVEL";
    private static final String ADMINISTRADOR = "ADMINISTRADOR";

    @Bean
    @Order(2)
    SecurityFilterChain cadeiaDasTelas(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(rota -> rota
                        // páginas de erro, arquivos estáticos e o health check (fora da matriz)
                        .requestMatchers("/error", "/error/**", "/css/**", "/js/**", "/img/**", "/favicon.ico",
                                "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/login").permitAll()

                        .requestMatchers(HttpMethod.GET, "/").authenticated()

                        // Solicitante (e quem mais pode reservar pela API): busca, nova reserva, minhas e detalhe
                        .requestMatchers(HttpMethod.GET, "/recursos/disponiveis", "/reservas/nova", "/reservas/minhas")
                        .hasAnyRole(SOLICITANTE, RESPONSAVEL, ADMINISTRADOR)
                        .requestMatchers(HttpMethod.GET, "/reservas/*/historico").hasAnyRole(RESPONSAVEL, ADMINISTRADOR)
                        .requestMatchers(HttpMethod.GET, "/reservas/*").hasAnyRole(SOLICITANTE, RESPONSAVEL, ADMINISTRADOR)
                        .requestMatchers(HttpMethod.POST, "/reservas", "/reservas/*", "/reservas/*/cancelar")
                        .hasAnyRole(SOLICITANTE, RESPONSAVEL, ADMINISTRADOR)

                        // Responsável: aprovação, retirada e devolução
                        .requestMatchers(HttpMethod.GET, "/aprovacoes", "/retirada-devolucao").hasRole(RESPONSAVEL)
                        .requestMatchers(HttpMethod.POST, "/aprovacoes/*/aprovar", "/aprovacoes/*/rejeitar",
                                "/retirada-devolucao/*/retirar", "/retirada-devolucao/*/devolver",
                                "/retirada-devolucao/*/nao-compareceu").hasRole(RESPONSAVEL)

                        .requestMatchers(HttpMethod.GET, "/relatorios/utilizacao").hasAnyRole(RESPONSAVEL, ADMINISTRADOR)

                        // Administrador: cadastro de recursos e bloqueio por manutenção
                        .requestMatchers(HttpMethod.GET, "/admin/recursos", "/admin/recursos/*",
                                "/admin/recursos/*/manutencao").hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.POST, "/admin/recursos", "/admin/recursos/*",
                                "/admin/recursos/*/manutencao").hasRole(ADMINISTRADOR)

                        // Fora da lista: exige login. Com sessão, a URL que não existe mostra a página 404 (e não
                        // um 403 que esconderia o erro); rota nova esquecida aqui não chega à main, porque
                        // MatrizAutorizacaoTest falha quando há rota no código que não está na matriz.
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("login")
                        .passwordParameter("senha")
                        .successHandler(ResultadoDoLogin.sucesso())
                        .failureHandler(ResultadoDoLogin.falha())
                        .permitAll())
                .logout(saida -> saida
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(erro -> erro.authenticationEntryPoint(new PortaDeEntradaDasTelas()))
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
