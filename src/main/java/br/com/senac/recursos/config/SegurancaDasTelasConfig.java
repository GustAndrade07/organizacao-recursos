package br.com.senac.recursos.config;

import br.com.senac.recursos.service.TentativasDeLogin;
import br.com.senac.recursos.web.sessao.LoginComLimite;
import br.com.senac.recursos.web.sessao.PortaDeEntradaDasTelas;
import br.com.senac.recursos.web.sessao.ResultadoDoLogin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadeia das telas Thymeleaf (D12, opção (a), ADR-0009): formulário em {@code /login} com sessão do Spring Security,
 * CSRF ligado e perfil conferido por rota. As regras de URL abaixo SÃO a tabela "Telas" de
 * {@code docs/seguranca/matriz-autorizacao.md}, conferida célula a célula por {@code MatrizAutorizacaoTest}; espelham
 * a tabela da API (quem não pode chamar a API também não vê a tela). A posse (dono da reserva, RN-06) continua no
 * serviço. Rota de tela fora da lista é negada ({@code denyAll}, regra 13 do {@code AGENTS.md}): esquecer uma rota
 * fecha o acesso em vez de abrir.
 *
 * <ul>
 *   <li>Sem sessão: redireciona para {@code /login} (com {@code ?expirou} se o cookie de sessão venceu e
 *       {@code ?destino=} com a página pedida).</li>
 *   <li>Perfil sem permissão: 403, a página {@code error/403.html}.</li>
 *   <li>{@code POST /logout} encerra a sessão, apaga o cookie e volta ao login.</li>
 *   <li>{@code POST /login} com limite de tentativas ({@link TentativasDeLogin}, ressalva 1 do PR #48): 5 falhas do
 *       mesmo login em 15 minutos bloqueiam esse login por 15 minutos, com a mesma resposta de uma senha errada. O
 *       contador é o mesmo do login da API (bean em {@code SegurancaConfig}).</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class SegurancaDasTelasConfig {

    private static final String SOLICITANTE = "SOLICITANTE";
    private static final String RESPONSAVEL = "RESPONSAVEL";
    private static final String ADMINISTRADOR = "ADMINISTRADOR";

    @Bean
    @Order(2)
    SecurityFilterChain cadeiaDasTelas(HttpSecurity http, ObjectProvider<UserDetailsService> usuarios,
            PasswordEncoder senhas, TentativasDeLogin tentativas) throws Exception {
        // Buscado só no POST /login: os recortes @WebMvcTest não têm o UsuariosDaTela (entram com @ComoUsuario).
        UserDetailsService usuariosDaTela = login -> usuarios.getObject().loadUserByUsername(login);
        DaoAuthenticationProvider loginESenha = new DaoAuthenticationProvider(usuariosDaTela);
        loginESenha.setPasswordEncoder(senhas);
        http
                // Gerenciador próprio e sem pai: com o global como pai, a recusa por bloqueio seria tentada de novo
                // no pai, e a senha certa entraria durante o bloqueio (ver LoginComLimite).
                .authenticationManager(new ProviderManager(new LoginComLimite(loginESenha, tentativas)))
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
                        // Administrador: usuários (tela 16, como GET e POST /api/usuarios)
                        .requestMatchers(HttpMethod.GET, "/admin/usuarios", "/admin/usuarios/novo").hasRole(ADMINISTRADOR)
                        .requestMatchers(HttpMethod.POST, "/admin/usuarios").hasRole(ADMINISTRADOR)

                        // Fora da lista: negada (regra 13 do AGENTS.md), como na API. Sem sessão leva ao login;
                        // com sessão, 403. Esquecer uma rota aqui fecha o acesso em vez de abrir.
                        .anyRequest().denyAll())
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
