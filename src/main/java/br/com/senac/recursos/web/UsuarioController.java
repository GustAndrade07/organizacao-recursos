package br.com.senac.recursos.web;

import br.com.senac.recursos.dto.request.UsuarioRequest;
import br.com.senac.recursos.dto.response.UsuarioResponse;
import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.exception.LoginEmUsoException;
import br.com.senac.recursos.service.UsuarioService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Tela 16 do inventário: usuários, só do Administrador (matriz), ligada ao {@link UsuarioService}, o mesmo de
 * {@code POST /api/usuarios}. Lista quem tem acesso e cadastra um usuário novo com nome completo, e-mail (o login, DEF-010), senha e perfil.
 *
 * <p>As regras dos campos são as do {@link UsuarioRequest} da API (as mesmas mensagens), conferidas aqui pelo
 * {@link Validator} antes de chamar o service; e-mail repetido (sem diferenciar maiúsculas) vira erro no campo. A senha nunca volta ao formulário
 * nem aparece na lista: o service só guarda o hash BCrypt. O service ainda não tem alterar nem desativar usuário, então
 * a tela também não tem.
 */
@Controller
public class UsuarioController {

    private static final String VIEW_FORM = "usuario-form";
    private static final List<String> ORDEM_DOS_CAMPOS = List.of("nome", "login", "senha", "perfil");

    private final UsuarioService usuarios;
    private final Validator validador;

    public UsuarioController(UsuarioService usuarios, Validator validador) {
        this.usuarios = usuarios;
        this.validador = validador;
    }

    /** Linha da lista: o perfil já por extenso. */
    public record Linha(Long id, String nome, String login, String perfil) {
    }

    /** O que o formulário devolve depois de uma recusa (nunca a senha). */
    public record Formulario(String nome, String login, String perfil) {
    }

    /** Opção do seletor de perfil. */
    public record Perfil(String valor, String rotulo) {
    }

    private static final List<Perfil> PERFIS = List.of(PerfilUsuario.values()).stream()
            .map(perfil -> new Perfil(perfil.name(), AtributosDasTelas.nomeDoPerfil(perfil)))
            .toList();

    @GetMapping("/admin/usuarios")
    public String lista(Model model) {
        model.addAttribute("usuarios", usuarios.listar().stream()
                .sorted(Comparator.comparing((UsuarioResponse u) -> u.nome().toLowerCase(Locale.ROOT)))
                .map(u -> new Linha(u.id(), u.nome(), u.login(), AtributosDasTelas.nomeDoPerfil(u.perfil())))
                .toList());
        return "usuarios";
    }

    @GetMapping("/admin/usuarios/novo")
    public String novo(Model model) {
        model.addAttribute("usuario", new Formulario(null, null, PerfilUsuario.SOLICITANTE.name()));
        model.addAttribute("perfis", PERFIS);
        return VIEW_FORM;
    }

    @PostMapping("/admin/usuarios")
    public String cadastrar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String login,
            @RequestParam(required = false) String senha,
            @RequestParam(required = false) String perfil,
            Model model, RedirectAttributes redirecionamento, HttpServletResponse resposta) {
        // O UsuarioRequest normaliza (e-mail em minúsculas e sem espaços nas pontas, nome sem espaços repetidos): é o
        // mesmo pedido da API, e o formulário devolve o que foi normalizado.
        UsuarioRequest pedido = new UsuarioRequest(nome, login, senha, perfilDe(perfil));
        Formulario formulario = new Formulario(pedido.nome(), pedido.login(), perfil);

        Map<String, String> erros = erros(validador.validate(pedido));
        if (erros.isEmpty()) {
            try {
                UsuarioResponse criado = usuarios.criar(pedido);
                redirecionamento.addFlashAttribute("confirmacao", Map.of("titulo", "Usuário cadastrado",
                        "texto", criado.nome() + " já pode entrar com o e-mail " + criado.login() + "."));
                return "redirect:/admin/usuarios";
            } catch (LoginEmUsoException repetido) {
                erros.put("login", "Já existe um usuário com este e-mail.");
            }
        }
        model.addAttribute("usuario", formulario);
        model.addAttribute("perfis", PERFIS);
        model.addAttribute("erros", erros);
        resposta.setStatus(HttpStatus.BAD_REQUEST.value());
        return VIEW_FORM;
    }

    /** Uma mensagem por campo, na ordem do formulário (a primeira violação de cada campo). */
    private static Map<String, String> erros(Set<ConstraintViolation<UsuarioRequest>> violacoes) {
        Map<String, String> porCampo = new LinkedHashMap<>();
        violacoes.stream()
                .sorted(Comparator.comparing((ConstraintViolation<UsuarioRequest> v) -> v.getMessage()))
                .forEach(v -> porCampo.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        Map<String, String> ordenado = new LinkedHashMap<>();
        ORDEM_DOS_CAMPOS.stream().filter(porCampo::containsKey).forEach(campo -> ordenado.put(campo, porCampo.get(campo)));
        return ordenado;
    }

    /** Perfil desconhecido vira nulo, e a validação ("Informe o perfil.") recusa. */
    private static PerfilUsuario perfilDe(String valor) {
        for (PerfilUsuario perfil : PerfilUsuario.values()) {
            if (perfil.name().equals(valor)) {
                return perfil;
            }
        }
        return null;
    }
}
