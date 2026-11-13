package br.com.senac.recursos.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.web.sessao.UsuarioDaSessao;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.support.ApiIntegracaoTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Lê {@code docs/seguranca/matriz-autorizacao.md} e confere cada célula (Aula 12): as rotas {@code /api/**} com o token
 * de cada perfil, e as telas com o usuário de cada perfil na sessão, como o {@code POST /login} deixa (D12, opção (a)),
 * e todo {@code POST} de tela com o token CSRF. A matriz é a fonte: o teste não tem lista própria de permissões. Também
 * falha se houver rota no código fora da matriz.
 */
class MatrizAutorizacaoTest extends ApiIntegracaoTest {

    private static final Path MATRIZ = Path.of("docs", "seguranca", "matriz-autorizacao.md");

    /** Ordem das colunas de perfil na tabela; {@code null} é o anônimo. */
    private static final List<PerfilUsuario> COLUNAS = java.util.Arrays.asList(
            null, PerfilUsuario.SOLICITANTE, PerfilUsuario.RESPONSAVEL, PerfilUsuario.ADMINISTRADOR);

    private static final String JANELA = "?inicio=2026-11-10T00:00:00Z&fim=2026-11-11T00:00:00Z";

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeamentos;

    private final Map<PerfilUsuario, String> tokens = new EnumMap<>(PerfilUsuario.class);
    private final Map<PerfilUsuario, Usuario> usuarios = new EnumMap<>(PerfilUsuario.class);
    private final Map<PerfilUsuario, Long> reservaPropria = new EnumMap<>(PerfilUsuario.class);
    private long reservaDeOutro;
    private long salaId;

    record Linha(HttpMethod metodo, String rota, List<String> celulas) {
    }

    @BeforeEach
    void cenario() throws Exception {
        for (PerfilUsuario perfil : PerfilUsuario.values()) {
            usuarios.put(perfil, usuario(perfil.name().toLowerCase(), perfil));
            tokens.put(perfil, token(usuarios.get(perfil)));
        }
        String outro = token(usuario("outro", PerfilUsuario.SOLICITANTE));
        salaId = recurso(TipoRecurso.SALA, "Sala", false).getId();
        reservaPropria.put(PerfilUsuario.SOLICITANTE, reservar(tokens.get(PerfilUsuario.SOLICITANTE),
                dia("08:00"), dia("09:00"), salaId));
        reservaPropria.put(PerfilUsuario.RESPONSAVEL, reservar(tokens.get(PerfilUsuario.RESPONSAVEL),
                dia("09:00"), dia("10:00"), salaId));
        reservaPropria.put(PerfilUsuario.ADMINISTRADOR, reservar(tokens.get(PerfilUsuario.ADMINISTRADOR),
                dia("10:00"), dia("11:00"), salaId));
        reservaDeOutro = reservar(outro, dia("11:00"), dia("12:00"), salaId);
    }

    static List<Linha> linhasDaMatriz() throws IOException {
        List<Linha> linhas = new ArrayList<>();
        for (String texto : Files.readAllLines(MATRIZ, StandardCharsets.UTF_8)) {
            String[] colunas = texto.split("\\|");
            if (colunas.length < 7 || !colunas[1].trim().matches("GET|POST|PUT|PATCH|DELETE")) {
                continue;
            }
            linhas.add(new Linha(HttpMethod.valueOf(colunas[1].trim()), colunas[2].trim(),
                    List.of(colunas[3].trim(), colunas[4].trim(), colunas[5].trim(), colunas[6].trim())));
        }
        return linhas;
    }

    static Stream<Arguments> celulas() throws IOException {
        List<Arguments> casos = new ArrayList<>();
        for (Linha linha : linhasDaMatriz()) {
            for (int i = 0; i < COLUNAS.size(); i++) {
                String celula = linha.celulas().get(i);
                String perfil = COLUNAS.get(i) == null ? "ANONIMO" : COLUNAS.get(i).name();
                if (celula.equals("dono") || celula.equals("não-dono")) {
                    casos.add(Arguments.of(linha.metodo(), linha.rota(), perfil, celula, true));
                    casos.add(Arguments.of(linha.metodo(), linha.rota(), perfil, celula, false));
                } else {
                    casos.add(Arguments.of(linha.metodo(), linha.rota(), perfil, celula, false));
                }
            }
        }
        return casos.stream();
    }

    @ParameterizedTest(name = "{0} {1} como {2}: {3} (na própria reserva: {4})")
    @MethodSource("celulas")
    void cadaCelulaDaMatrizValeNaApi(HttpMethod metodo, String rota, String perfilNome, String celula,
            boolean naPropria) throws Exception {
        PerfilUsuario perfil = perfilNome.equals("ANONIMO") ? null : PerfilUsuario.valueOf(perfilNome);
        long reserva = naPropria ? reservaPropria.get(perfil) : reservaDeOutro;
        boolean tela = !rota.startsWith("/api/");
        boolean doRecurso = rota.startsWith("/api/recursos") || rota.startsWith("/admin/recursos");
        String url = rota.replace("{id}", Long.toString(doRecurso ? salaId : reserva))
                + (rota.equals("/api/recursos/disponiveis") || rota.startsWith("/api/relatorios") ? JANELA : "");

        MockHttpServletRequestBuilder requisicao;
        if (tela) {
            requisicao = request(metodo, url).with(csrf());
            if (perfil != null) {
                requisicao.with(authentication(sessaoDe(usuarios.get(perfil))));
            }
            camposDaTela(metodo + " " + rota).forEach(requisicao::param);
        } else {
            requisicao = comToken(request(metodo, url), perfil == null ? null : tokens.get(perfil));
            String corpo = corpo(metodo + " " + rota, perfil);
            if (corpo != null) {
                requisicao.content(corpo);
            }
        }
        MockHttpServletResponse resposta = mvc.perform(requisicao).andReturn().getResponse();
        int status = resposta.getStatus();
        String descricao = metodo + " " + url + " como " + perfilNome + " -> " + status + " " + resposta.getContentAsString();

        switch (celula) {
            case "401" -> assertThat(status).as(descricao).isEqualTo(401);
            case "login" -> assertThat(resposta.getRedirectedUrl()).as(descricao).isNotNull()
                    .matches(destino -> status == 302 && destino.contains("/login"));
            case "403" -> assertThat(status).as(descricao).isEqualTo(403);
            case "ok" -> assertThat(status).as(descricao).isNotIn(401, 403);
            case "dono" -> assertThat(status).as(descricao).matches(s -> naPropria ? s != 401 && s != 403 : s == 403);
            case "não-dono" -> assertThat(status).as(descricao).matches(s -> naPropria ? s == 403 : s != 401 && s != 403);
            default -> throw new AssertionError("Célula desconhecida na matriz: " + celula);
        }
    }

    /** Toda rota do código (API e telas) está na matriz, e toda linha da matriz existe no código (regra 13). */
    @Test
    void matrizCobreExatamenteAsRotasDoCodigo() throws IOException {
        Set<String> doCodigo = new TreeSet<>();
        mapeamentos.getHandlerMethods().forEach((info, metodo) -> {
            String pacote = metodo.getBeanType().getPackageName();
            if (pacote.equals("br.com.senac.recursos.controller") || pacote.equals("br.com.senac.recursos.web")) {
                info.getMethodsCondition().getMethods().forEach(verbo -> info.getPatternValues()
                        .forEach(rota -> doCodigo.add(verbo.name() + " " + rota)));
            }
        });
        Set<String> daMatriz = new TreeSet<>();
        linhasDaMatriz().forEach(linha -> daMatriz.add(linha.metodo().name() + " " + linha.rota()));

        assertThat(daMatriz).isEqualTo(doCodigo);
    }

    @Test
    void rotaForaDaMatrizEhNegada() throws Exception {
        assertThat(mvc.perform(comToken(request(HttpMethod.GET, "/api/nao-existe"), null)).andReturn()
                .getResponse().getStatus()).isEqualTo(401);
        assertThat(mvc.perform(comToken(request(HttpMethod.DELETE, "/api/reservas/1"),
                tokens.get(PerfilUsuario.ADMINISTRADOR))).andReturn().getResponse().getStatus()).isEqualTo(403);
    }

    /** O mesmo usuário que o {@code POST /login} põe na sessão. */
    private static Authentication sessaoDe(Usuario usuario) {
        UsuarioDaSessao daSessao = new UsuarioDaSessao(usuario.getId(), usuario.getNome(), usuario.getLogin(),
                usuario.getPerfil(), null);
        return UsernamePasswordAuthenticationToken.authenticated(daSessao, null, daSessao.getAuthorities());
    }

    /**
     * Campos válidos para os formulários de tela que gravam, para a resposta refletir a autorização e não um 400. As
     * datas são as do fuso das telas (São Paulo) e ficam em 10/11/2026, no futuro em relação ao relógio do teste.
     */
    private Map<String, String> camposDaTela(String chave) {
        return switch (chave) {
            case "POST /reservas" -> Map.of("recursoId", Long.toString(salaId), "data", "2026-11-10",
                    "inicio", "18:00", "termino", "19:00");
            case "POST /reservas/{id}" -> Map.of("data", "2026-11-10", "inicio", "19:00", "termino", "20:00");
            case "POST /aprovacoes/{id}/rejeitar" -> Map.of("motivo", "Sala em obra.");
            case "POST /admin/recursos" -> Map.of("tipo", "materiais", "nome", "Projetor");
            case "POST /admin/usuarios" -> Map.of("nome", "Nova Pessoa", "login", "nova.pessoa",
                    "senha", "senha-nova-123", "perfil", "SOLICITANTE");
            case "POST /admin/recursos/{id}" -> Map.of("nome", "Sala renomeada", "capacidade", "10");
            case "POST /admin/recursos/{id}/manutencao" -> Map.of("inicio", "2026-12-01T10:00",
                    "fim", "2026-12-01T12:00", "confirmar", "true");
            default -> Map.of();
        };
    }

    /** Corpo válido para cada rota de escrita, para a resposta refletir a autorização e não um 400. */
    private static String corpo(String chave, PerfilUsuario perfil) {
        return switch (chave) {
            case "POST /api/auth/login" -> "{\"login\":\"" + (perfil == null ? "solicitante" : perfil.name().toLowerCase())
                    + "@exemplo.com\",\"senha\":\"" + SENHA + "\"}";
            case "POST /api/usuarios" ->
                    "{\"nome\":\"Novo Usuario\",\"login\":\"novo@exemplo.com\",\"senha\":\"senha-nova-123\",\"perfil\":\"SOLICITANTE\"}";
            case "POST /api/recursos" -> "{\"tipo\":\"MATERIAL\",\"nome\":\"Projetor\",\"restrito\":false}";
            case "PUT /api/recursos/{id}" -> "{\"nome\":\"Sala renomeada\",\"capacidade\":10,\"restrito\":false}";
            case "POST /api/recursos/{id}/manutencoes" -> corpoPeriodo("2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z");
            case "POST /api/reservas" -> "{\"recursoIds\":[1],\"inicio\":\"" + dia("20:00") + "\",\"fim\":\""
                    + dia("21:00") + "\"}";
            case "PUT /api/reservas/{id}/periodo" -> corpoPeriodo(dia("22:00"), dia("23:00"));
            default -> null;
        };
    }
}
