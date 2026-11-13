package br.com.senac.recursos.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.model.Recurso;
import br.com.senac.recursos.model.Usuario;
import br.com.senac.recursos.repository.RecursoRepository;
import br.com.senac.recursos.repository.UsuarioRepository;
import br.com.senac.recursos.service.TokenService;
import br.com.senac.recursos.support.RelogioDeTeste;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Passos dos cenários de `docs/testes/bdd-aprovacao-bloqueio.md`. Cada passo fala com a aplicação pela API, com o
 * token do perfil de quem age; o estado é conferido no banco ou na resposta, nunca por variável de memória do teste.
 */
public class AprovacaoBloqueioSteps {

    private static final String SENHA = "senha-de-teste-123";

    /** Dia das reservas de exemplo, no futuro em relação a {@link ConfiguracaoDoCucumber#AGORA}. */
    private static final String DIA = "2026-11-10T";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RelogioDeTeste relogio;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private RecursoRepository recursos;

    @Autowired
    private PasswordEncoder codificadorDeSenha;

    @Autowired
    private TokenService tokens;

    private final Map<String, String> tokenDe = new HashMap<>();
    private final Map<String, Long> recursoChamado = new HashMap<>();
    private Long reserva;
    private MockHttpServletResponse resposta;

    @Before
    public void bancoVazioERelogioNoInicio() {
        jdbc.execute("truncate table auditoria_reserva, tentativa_recusada, reserva_recurso, reserva, manutencao, "
                + "recurso, usuario restart identity cascade");
        relogio.ajustar(ConfiguracaoDoCucumber.AGORA);
        tokenDe.clear();
        recursoChamado.clear();
        reserva = null;
        resposta = null;
    }

    // ---------------------------------------------------------------- Dado

    @Dado("o sistema sem reservas")
    public void oSistemaSemReservas() {
        assertThat(contar("select count(*) from reserva")).isZero();
    }

    @Dado("um recurso restrito chamado {string}")
    public void umRecursoRestrito(String nome) {
        cadastrarRecurso(nome, true);
    }

    @Dado("um recurso comum chamado {string}")
    public void umRecursoComum(String nome) {
        cadastrarRecurso(nome, false);
    }

    @Dado("um Solicitante chamado {string}")
    public void umSolicitante(String login) {
        cadastrarUsuario(login, PerfilUsuario.SOLICITANTE);
    }

    @Dado("um Responsável chamado {string}")
    public void umResponsavel(String login) {
        cadastrarUsuario(login, PerfilUsuario.RESPONSAVEL);
    }

    @Dado("uma reserva de {string} no {string} das {word} às {word}")
    public void umaReservaNoRecurso(String login, String recurso, String inicio, String fim) throws Exception {
        criarReserva(login, recurso, inicio, fim);
    }

    @Dado("uma reserva de {string} na {string} das {word} às {word}")
    public void umaReservaNaSala(String login, String recurso, String inicio, String fim) throws Exception {
        criarReserva(login, recurso, inicio, fim);
    }

    @Dado("uma manutenção na {string} das {word} às {word}")
    public void umaManutencao(String recurso, String inicio, String fim) throws Exception {
        cadastrarUsuario("admin-da-manutencao", PerfilUsuario.ADMINISTRADOR);
        chamar(post("/api/recursos/" + recursoChamado.get(recurso) + "/manutencoes"), "admin-da-manutencao",
                "{\"inicio\":\"" + instante(inicio) + "\",\"fim\":\"" + instante(fim) + "\"}");
        assertThat(resposta.getStatus()).as("a manutenção precisa ter sido criada").isEqualTo(201);
    }

    /** Confere o estado como ponto de partida do cenário; não muda nada. */
    @Dado("a reserva está {string}")
    public void aReservaEsta(String estado) {
        assertThat(estadoNoBanco()).isEqualTo(estado);
    }

    // ---------------------------------------------------------------- Quando

    @Quando("{string} aprova a reserva")
    public void aprovaAReserva(String login) throws Exception {
        chamar(post("/api/reservas/" + reserva + "/aprovacao"), login, null);
        assertThat(resposta.getStatus()).as("a aprovação precisa ter dado certo").isEqualTo(200);
    }

    @Quando("{string} tenta aprovar a reserva")
    public void tentaAprovar(String login) throws Exception {
        chamar(post("/api/reservas/" + reserva + "/aprovacao"), login, null);
    }

    @Quando("{string} rejeita a reserva com o motivo {string}")
    public void rejeita(String login, String motivo) throws Exception {
        chamar(post("/api/reservas/" + reserva + "/rejeicao"), login, "{\"motivo\":\"" + motivo + "\"}");
    }

    @Quando("{string} tenta reservar a {string} das {word} às {word}")
    public void tentaReservar(String login, String recurso, String inicio, String fim) throws Exception {
        pedirReserva(login, recurso, inicio, fim);
    }

    @Quando("{string} registra a retirada")
    public void registraRetirada(String login) throws Exception {
        chamar(post("/api/reservas/" + reserva + "/retirada"), login, null);
    }

    @Quando("{string} tenta registrar a devolução sem a retirada")
    public void devolucaoSemRetirada(String login) throws Exception {
        chamar(post("/api/reservas/" + reserva + "/devolucao"), login, null);
    }

    @Quando("{string} registra o não comparecimento")
    public void naoComparecimento(String login) throws Exception {
        chamar(post("/api/reservas/" + reserva + "/nao-comparecimento"), login, null);
    }

    // ---------------------------------------------------------------- Então

    @Então("a reserva fica {string}")
    public void aReservaFica(String estado) {
        assertThat(estadoNoBanco()).isEqualTo(estado);
    }

    @Então("a reserva continua {string}")
    public void aReservaContinua(String estado) {
        assertThat(estadoNoBanco()).isEqualTo(estado);
    }

    @Então("a resposta é {int}")
    public void aRespostaE(int status) throws Exception {
        assertThat(resposta.getStatus()).as(resposta.getContentAsString()).isEqualTo(status);
    }

    @Então("o tipo do problema é {string}")
    public void oTipoDoProblema(String tipo) throws Exception {
        assertThat(corpo().get("type").asText()).isEqualTo(tipo);
    }

    @Então("o sistema tem {int} reservas gravadas")
    public void reservasGravadas(int quantas) {
        assertThat(contar("select count(*) from reserva")).isEqualTo(quantas);
    }

    @Então("nenhuma tentativa recusada foi gravada")
    public void nenhumaTentativa() {
        assertThat(contar("select count(*) from tentativa_recusada")).isZero();
    }

    @Então("o histórico da reserva tem {int} registros, o último feito por {string}")
    public void oHistorico(int quantos, String login) throws Exception {
        cadastrarUsuario("auditor", PerfilUsuario.ADMINISTRADOR);
        chamar(get("/api/reservas/" + reserva + "/historico"), "auditor", null);
        JsonNode historico = corpo();
        assertThat(historico).hasSize(quantos);
        assertThat(historico.get(quantos - 1).get("usuarioLogin").asText()).isEqualTo(login);
    }

    @Então("depois da devolução a reserva fica {string}")
    public void depoisDaDevolucao(String estado) throws Exception {
        chamar(post("/api/reservas/" + reserva + "/devolucao"), "bruno", null);
        assertThat(resposta.getStatus()).isEqualTo(200);
        assertThat(estadoNoBanco()).isEqualTo(estado);
    }

    @Então("{string} consegue reservar o {string} das {word} às {word}")
    public void consegueReservarO(String login, String recurso, String inicio, String fim) throws Exception {
        pedirReserva(login, recurso, inicio, fim);
        assertThat(resposta.getStatus()).as(resposta.getContentAsString()).isEqualTo(201);
    }

    @Então("{string} consegue reservar a {string} das {word} às {word}")
    public void consegueReservarA(String login, String recurso, String inicio, String fim) throws Exception {
        consegueReservarO(login, recurso, inicio, fim);
    }

    // ---------------------------------------------------------------- apoio

    private void cadastrarRecurso(String nome, boolean restrito) {
        Recurso novo = recursos.save(new Recurso(TipoRecurso.SALA, nome, 30, restrito));
        recursoChamado.put(nome, novo.getId());
    }

    private void cadastrarUsuario(String login, PerfilUsuario perfil) {
        if (tokenDe.containsKey(login)) {
            return;
        }
        Usuario novo = usuarios.save(new Usuario("Usuário " + login, login, codificadorDeSenha.encode(SENHA), perfil));
        tokenDe.put(login, tokens.emitir(novo).token());
    }

    private void criarReserva(String login, String recurso, String inicio, String fim) throws Exception {
        pedirReserva(login, recurso, inicio, fim);
        assertThat(resposta.getStatus()).as(resposta.getContentAsString()).isEqualTo(201);
        reserva = corpo().get("id").asLong();
    }

    private void pedirReserva(String login, String recurso, String inicio, String fim) throws Exception {
        chamar(post("/api/reservas"), login, "{\"recursoIds\":[" + recursoChamado.get(recurso) + "],\"inicio\":\""
                + instante(inicio) + "\",\"fim\":\"" + instante(fim) + "\"}");
    }

    private void chamar(MockHttpServletRequestBuilder requisicao, String login, String corpo) throws Exception {
        requisicao.contentType(MediaType.APPLICATION_JSON);
        String token = tokenDe.get(login);
        assertThat(token).as("usuário %s não foi cadastrado no cenário", login).isNotNull();
        requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (corpo != null) {
            requisicao.content(corpo);
        }
        resposta = mvc.perform(requisicao).andReturn().getResponse();
    }

    private JsonNode corpo() throws Exception {
        return json.readTree(resposta.getContentAsString());
    }

    private String estadoNoBanco() {
        return jdbc.queryForObject("select estado from reserva where id = ?", String.class, reserva);
    }

    private long contar(String sql) {
        Long total = jdbc.queryForObject(sql, Long.class);
        return total == null ? 0 : total;
    }

    /** {@code "10:00"} e {@code "11:59:59"} viram instantes do dia das reservas de exemplo, em UTC. */
    private static String instante(String hora) {
        return DIA + (hora.length() == 5 ? hora + ":00" : hora) + "Z";
    }
}
