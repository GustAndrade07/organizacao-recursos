package br.com.senac.recursos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.support.ApiIntegracaoTest;
import br.com.senac.recursos.web.sessao.UsuarioDaSessao;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MvcResult;

/**
 * RF-14: a documentação da API é gerada do próprio código (springdoc) e precisa descrever **as mesmas rotas** que a
 * matriz de autorização lista para a API — nem a mais (rota não documentada esconde comportamento), nem a menos
 * (documentação prometendo rota que não existe). É a mesma ideia da regra 13 do {@code AGENTS.md}: a lista é tirada
 * do código, nunca mantida à mão.
 */
class OpenApiDocumentacaoTest extends ApiIntegracaoTest {

    private static final Path MATRIZ = Path.of("docs", "seguranca", "matriz-autorizacao.md");

    private static final String DOC = "/v3/api-docs";

    /** Rotas da API na matriz, no formato {@code "GET /api/reservas"}. */
    private static Set<String> rotasDaApiNaMatriz() throws IOException {
        Set<String> rotas = new TreeSet<>();
        for (String linha : Files.readAllLines(MATRIZ, StandardCharsets.UTF_8)) {
            String[] colunas = linha.split("\\|");
            if (colunas.length >= 7 && colunas[1].trim().matches("GET|POST|PUT|PATCH|DELETE")
                    && colunas[2].trim().startsWith("/api/")) {
                rotas.add(colunas[1].trim() + " " + colunas[2].trim());
            }
        }
        return rotas;
    }

    private JsonNode documentacao() throws Exception {
        MvcResult resultado = mvc.perform(get(DOC).with(authentication(sessaoDe(
                        usuario("leitor-da-doc", PerfilUsuario.ADMINISTRADOR)))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(resultado.getResponse().getContentAsString());
    }

    @Test
    void documentacaoDescreveExatamenteAsRotasDaApiDaMatriz() throws Exception {
        JsonNode caminhos = documentacao().get("paths");

        Set<String> documentadas = new TreeSet<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = caminhos.fields(); it.hasNext();) {
            Map.Entry<String, JsonNode> caminho = it.next();
            caminho.getValue().fieldNames()
                    .forEachRemaining(verbo -> documentadas.add(verbo.toUpperCase() + " " + caminho.getKey()));
        }

        assertThat(documentadas).isEqualTo(rotasDaApiNaMatriz());
    }

    @Test
    void relatorioDeUtilizacaoTemResumoParametrosERespostasDocumentados() throws Exception {
        JsonNode relatorio = documentacao().get("paths").get("/api/relatorios/utilizacao").get("get");

        assertThat(relatorio.get("summary").asText()).contains("Utilização");
        assertThat(relatorio.get("description").asText()).contains("D8");
        assertThat(relatorio.get("parameters").findValuesAsText("name")).containsExactlyInAnyOrder("inicio", "fim");
        assertThat(relatorio.get("parameters")).allSatisfy(parametro ->
                assertThat(parametro.get("required").asBoolean()).isTrue());
        assertThat(relatorio.get("responses").fieldNames()).toIterable().containsExactlyInAnyOrder("200", "400",
                "401", "403");
    }

    /** O token JWT aparece como esquema de segurança, para quem lê a documentação saber como autenticar (RF-01). */
    @Test
    void documentacaoExplicaOTokenEOTituloDaApi() throws Exception {
        JsonNode doc = documentacao();

        assertThat(doc.get("info").get("title").asText()).isEqualTo("Organização de Recursos — API");
        JsonNode esquema = doc.get("components").get("securitySchemes").get("bearer-jwt");
        assertThat(esquema.get("scheme").asText()).isEqualTo("bearer");
        assertThat(esquema.get("bearerFormat").asText()).isEqualTo("JWT");
    }

    /** A documentação não é pública: sem sessão, o springdoc cai na mesma regra das telas e leva ao login. */
    @Test
    void semSessaoADocumentacaoLevaAoLogin() throws Exception {
        mvc.perform(get(DOC))
                .andExpect(status().isFound())
                .andExpect(resultado -> assertThat(resultado.getResponse().getRedirectedUrl()).contains("/login"));
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isFound())
                .andExpect(resultado -> assertThat(resultado.getResponse().getRedirectedUrl()).contains("/login"));
    }

    /** Nenhuma senha, token ou segredo do projeto vaza no JSON da documentação. */
    @Test
    void documentacaoNaoTrazSegredo() throws Exception {
        String texto = documentacao().toString().toLowerCase();

        assertThat(texto).doesNotContain(SENHA).doesNotContain("recursos_dev").doesNotContain("jwt_secret");
    }

    /** O mesmo usuário que o {@code POST /login} põe na sessão. */
    private static Authentication sessaoDe(Usuario usuario) {
        UsuarioDaSessao daSessao = new UsuarioDaSessao(usuario.getId(), usuario.getNome(), usuario.getLogin(),
                usuario.getPerfil(), null);
        return UsernamePasswordAuthenticationToken.authenticated(daSessao, null, daSessao.getAuthorities());
    }
}
