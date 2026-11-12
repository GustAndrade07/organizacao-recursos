package br.com.senac.recursos.web;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.web.sessao.UsuarioDaSessao;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Atributos de modelo que todas as telas (inclusive {@code /error}) recebem:
 * <ul>
 *   <li>{@code caminhoDoSite}: a regra de caminho do site; o Thymeleaf não deixa chamar método estático dentro de
 *       fragmento, então os templates usam {@code ${caminhoDoSite.valido(x)}};</li>
 *   <li>{@code usuario} ({@code nome}, {@code perfil}) e {@code menu} (itens do perfil), que o layout usa para montar a
 *       barra (docs/frontend/README.md, "Layout base e menu"). Sem sessão os dois ficam nulos e a barra mostra só a
 *       marca.</li>
 * </ul>
 */
@ControllerAdvice
class AtributosDasTelas {

    /** Usuário logado, como a barra mostra. */
    public record UsuarioDaBarra(String nome, String perfil) {
    }

    /** Item do menu; {@code ativo} marca a página atual ({@code aria-current="page"}). */
    public record ItemDoMenu(String id, String rotulo, String href, boolean ativo) {
    }

    private record Item(String id, String rotulo, String href) {
    }

    private static final Item BUSCAR = new Item("buscar", "Buscar recursos", "/recursos/disponiveis");
    private static final Item MINHAS = new Item("minhas", "Minhas reservas", "/reservas/minhas");
    private static final Item APROVACOES = new Item("aprovacoes", "Aprovações", "/aprovacoes");
    private static final Item RETIRADA = new Item("retirada", "Retirada e devolução", "/retirada-devolucao");
    private static final Item RELATORIOS = new Item("relatorios", "Relatórios", "/relatorios/utilizacao");
    private static final Item RECURSOS = new Item("recursos", "Recursos", "/admin/recursos");

    @ModelAttribute("caminhoDoSite")
    CaminhoDoSite caminhoDoSite() {
        return new CaminhoDoSite();
    }

    @ModelAttribute("usuario")
    UsuarioDaBarra usuario() {
        UsuarioDaSessao logado = logado();
        return logado == null ? null : new UsuarioDaBarra(logado.getNome(), nomeDoPerfil(logado.getPerfil()));
    }

    @ModelAttribute("menu")
    List<ItemDoMenu> menu(HttpServletRequest requisicao) {
        UsuarioDaSessao logado = logado();
        if (logado == null) {
            return List.of();
        }
        String atual = requisicao.getRequestURI().substring(requisicao.getContextPath().length());
        return itensDo(logado.getPerfil()).stream()
                .map(item -> new ItemDoMenu(item.id(), item.rotulo(), item.href(), atual.startsWith(item.href())))
                .toList();
    }

    /** Itens propostos por perfil em docs/frontend/README.md; cada um é uma rota que o perfil pode abrir (matriz). */
    static List<String> rotasDoMenu(PerfilUsuario perfil) {
        return itensDo(perfil).stream().map(Item::href).toList();
    }

    private static List<Item> itensDo(PerfilUsuario perfil) {
        return switch (perfil) {
            case SOLICITANTE -> List.of(BUSCAR, MINHAS);
            case RESPONSAVEL -> List.of(APROVACOES, RETIRADA, RELATORIOS);
            case ADMINISTRADOR -> List.of(RECURSOS, RELATORIOS);
        };
    }

    static String nomeDoPerfil(PerfilUsuario perfil) {
        return switch (perfil) {
            case SOLICITANTE -> "Solicitante";
            case RESPONSAVEL -> "Responsável";
            case ADMINISTRADOR -> "Administrador";
        };
    }

    private static UsuarioDaSessao logado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioDaSessao usuario ? usuario : null;
    }
}
