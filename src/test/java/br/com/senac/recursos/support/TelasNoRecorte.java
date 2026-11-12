package br.com.senac.recursos.support;

import br.com.senac.recursos.service.ManutencaoService;
import br.com.senac.recursos.service.RecursoService;
import br.com.senac.recursos.service.RelatorioService;
import br.com.senac.recursos.service.ReservaService;
import br.com.senac.recursos.service.UsuarioService;
import br.com.senac.recursos.web.FormatoDasTelas;
import br.com.senac.recursos.web.VisaoDasReservas;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockReset;

/**
 * O que os recortes {@code @WebMvcTest} das telas precisam desde que as telas chamam os services (D12, ADR-0009):
 * <ul>
 *   <li>os services são dublês do Mockito <b>sem nenhuma resposta programada</b>: listas vazias e nulos (o teste que
 *       precisa de uma resposta a programa e o Spring a desfaz depois de cada teste, {@code MockReset.AFTER}). Assim os
 *       testes de contrato de cada template continuam olhando só o HTML, com os dados que eles mesmos põem no modelo
 *       pelas rotas {@code /teste/...}. O que a tela faz com dado real é provado contra o banco em
 *       {@code TelasComBancoTest} e no E2E;</li>
 *   <li>as rotas {@code /teste/**}, que só existem nos testes, ficam abertas numa cadeia de segurança própria; as
 *       rotas reais continuam com a cadeia das telas (login e perfil), e o teste entra com {@link ComoUsuario}.</li>
 * </ul>
 * Registrado em {@code META-INF/spring/...AutoConfigureWebMvc.imports}; vale só para os recortes.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import({FormatoDasTelas.class, VisaoDasReservas.class})
public class TelasNoRecorte {

    @Bean
    ReservaService reservaService() {
        return Mockito.mock(ReservaService.class, MockReset.withSettings(MockReset.AFTER));
    }

    @Bean
    RecursoService recursoService() {
        return Mockito.mock(RecursoService.class, MockReset.withSettings(MockReset.AFTER));
    }

    @Bean
    ManutencaoService manutencaoService() {
        return Mockito.mock(ManutencaoService.class, MockReset.withSettings(MockReset.AFTER));
    }

    @Bean
    RelatorioService relatorioService() {
        return Mockito.mock(RelatorioService.class, MockReset.withSettings(MockReset.AFTER));
    }

    @Bean
    UsuarioService usuarioService() {
        return Mockito.mock(UsuarioService.class, MockReset.withSettings(MockReset.AFTER));
    }

    @Bean
    @Order(0)
    SecurityFilterChain rotasDeTesteAbertas(HttpSecurity http) throws Exception {
        http.securityMatcher("/teste/**").authorizeHttpRequests(rota -> rota.anyRequest().permitAll());
        return http.build();
    }
}
