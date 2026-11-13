package br.com.senac.recursos.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.model.Recurso;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.repository.RecursoRepository;
import br.com.senac.recursos.repository.UsuarioRepository;
import br.com.senac.recursos.service.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de API: aplicação inteira (segurança, controllers, serviços, JPA) contra PostgreSQL 16 de verdade
 * (Testcontainers, nunca H2), com a V1 aplicada pelo Flyway. Requisições pelo MockMvc com token de cada perfil.
 *
 * <p>Contêiner único por JVM e relógio controlado ({@link RelogioDeTeste}, começa em {@link #AGORA}). Cada teste
 * começa com o banco vazio. Docker é requisito (AGENTS.md, seção 8): sem Docker o teste falha, não é ignorado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiIntegracaoTest.RelogioConfigDeTeste.class)
public abstract class ApiIntegracaoTest {

    /** "Agora" de todos os testes: as reservas de exemplo ficam em 10/11/2026, no futuro em relação a ele. */
    protected static final Instant AGORA = Instant.parse("2026-11-01T12:00:00Z");

    protected static final String SENHA = "senha-de-teste-123";

    protected static final Duration VALIDADE_DO_TOKEN = Duration.ofDays(30);

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("spring.datasource.hikari.maximum-pool-size", () -> "30");
        // Os testes avançam o relógio de 01/11 até 10/11; com a validade padrão (1 h) os tokens expirariam no meio.
        registro.add("recursos.seguranca.jwt.validade", VALIDADE_DO_TOKEN::toString);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RelogioConfigDeTeste {

        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste(AGORA);
        }
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected RelogioDeTeste relogio;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private RecursoRepository recursos;

    @Autowired
    private PasswordEncoder codificadorDeSenha;

    @Autowired
    private TokenService tokens;

    @BeforeEach
    void bancoVazioERelogioNoInicio() {
        jdbc.execute("truncate table auditoria_reserva, tentativa_recusada, reserva_recurso, reserva, manutencao, "
                + "recurso, usuario restart identity cascade");
        relogio.ajustar(AGORA);
    }

    // ---------------------------------------------------------------- dados

    /** O login é um e-mail (DEF-010): os testes dão só o apelido e o e-mail é {@code apelido@exemplo.com}. */
    protected static String email(String apelido) {
        return apelido + "@exemplo.com";
    }

    protected Usuario usuario(String apelido, PerfilUsuario perfil) {
        return usuarios.save(new Usuario("Usuário " + apelido, email(apelido), codificadorDeSenha.encode(SENHA), perfil));
    }

    protected String token(Usuario usuario) {
        return tokens.emitir(usuario).token();
    }

    protected Recurso recurso(TipoRecurso tipo, String nome, boolean restrito) {
        return recursos.save(new Recurso(tipo, nome, tipo == TipoRecurso.SALA ? 30 : null, restrito));
    }

    /** Instante em 10/11/2026 (UTC), ex.: {@code dia("10:00")}. */
    protected static String dia(String hora) {
        return "2026-11-10T" + hora + ":00Z";
    }

    protected static String corpoReserva(String inicio, String fim, long... recursoIds) {
        String ids = Arrays.stream(recursoIds).mapToObj(Long::toString).collect(Collectors.joining(","));
        return "{\"recursoIds\":[" + ids + "],\"inicio\":\"" + inicio + "\",\"fim\":\"" + fim + "\"}";
    }

    protected static String corpoPeriodo(String inicio, String fim) {
        return "{\"inicio\":\"" + inicio + "\",\"fim\":\"" + fim + "\"}";
    }

    /** Cria a reserva pela API e devolve o id; falha se não vier 201. */
    protected long reservar(String token, String inicio, String fim, long... recursoIds) throws Exception {
        MvcResult resultado = mvc.perform(comToken(post("/api/reservas"), token)
                        .content(corpoReserva(inicio, fim, recursoIds)))
                .andExpect(status().isCreated())
                .andReturn();
        return lerJson(resultado).get("id").asLong();
    }

    protected long contar(String sql, Object... argumentos) {
        Long total = jdbc.queryForObject(sql, Long.class, argumentos);
        return total == null ? 0 : total;
    }

    // ---------------------------------------------------------------- requisições

    protected MockHttpServletRequestBuilder comToken(MockHttpServletRequestBuilder requisicao, String token) {
        requisicao.contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return requisicao;
    }

    protected ResultActions obter(String token, String url) throws Exception {
        return mvc.perform(comToken(get(url), token));
    }

    protected ResultActions enviar(String token, String url, String corpo) throws Exception {
        MockHttpServletRequestBuilder requisicao = comToken(post(url), token);
        return mvc.perform(corpo == null ? requisicao : requisicao.content(corpo));
    }

    protected ResultActions substituir(String token, String url, String corpo) throws Exception {
        return mvc.perform(comToken(put(url), token).content(corpo));
    }

    protected JsonNode lerJson(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsString());
    }
}
