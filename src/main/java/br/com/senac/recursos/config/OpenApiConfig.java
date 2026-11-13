package br.com.senac.recursos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação da API (RF-14), gerada do próprio código pelo springdoc: {@code /v3/api-docs} (JSON) e
 * {@code /swagger-ui/index.html} (navegador).
 *
 * <p>As duas rotas são da biblioteca, não do pacote {@code controller}, então não entram nas tabelas de
 * {@code docs/seguranca/matriz-autorizacao.md} (o teste da matriz compara as rotas dos pacotes do projeto). A
 * permissão delas está escrita na seção "Rotas da documentação" do mesmo arquivo e conferida por
 * {@code OpenApiDocumentacaoTest}: quem não está autenticado vai para o {@code /login}.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI documentacaoDaApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Organização de Recursos — API")
                        .version("v1.0.0-rc1")
                        .description("""
                                Reserva de salas, professores e materiais sem conflito de horário.

                                Autenticação: `POST /api/auth/login` devolve um token JWT; mande-o em
                                `Authorization: Bearer <token>` nas demais rotas. Quem pode o quê está em
                                `docs/seguranca/matriz-autorizacao.md`.

                                Períodos são `[início, fim)` em ISO-8601 com deslocamento (decisão D1, ADR-0002):
                                uma reserva que termina exatamente quando outra começa é aceita.

                                Erros seguem `application/problem+json` (RFC 7807) e nunca trazem stack trace,
                                SQL nem nome de classe interna.""")
                        .license(new License().name("Projeto acadêmico — Senac, Qualidade de Software 2026.2")))
                .components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token devolvido por POST /api/auth/login.")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
