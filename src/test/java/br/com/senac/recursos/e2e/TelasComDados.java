package br.com.senac.recursos.e2e;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import br.com.senac.recursos.web.FiltrosBusca;
import br.com.senac.recursos.web.FormularioReserva;
import br.com.senac.recursos.web.TipoRecurso;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Só do E2E: renderiza os templates reais com dados de exemplo em {@code /e2e/...}, no lugar do back-end que
 * ainda não existe. O modelo segue o contrato de cada tela em {@code docs/frontend/README.md}. Fica fora do
 * component scan ({@link TestComponent}) e entra só pelo {@code @Import} de {@link E2ETeste}.
 */
@TestComponent
@Controller
class TelasComDados {

    /**
     * As rotas {@code /e2e/**} só existem no teste e ficam abertas numa cadeia própria; as rotas reais das telas
     * continuam exigindo login (D12, ADR-0009), e os testes que as usam entram pela tela de login ({@code entrarComo}).
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class RotasDoE2EAbertas {

        @Bean
        @Order(0)
        SecurityFilterChain rotasDoE2E(HttpSecurity http) throws Exception {
            http.securityMatcher("/e2e/**").authorizeHttpRequests(rota -> rota.anyRequest().permitAll());
            return http.build();
        }
    }

    static final LocalDate DIA = LocalDate.of(2026, 11, 12);

    private static void solicitante(Model model, String ativo) {
        model.addAttribute("usuario", Map.of("nome", "Ana Lima", "perfil", "Solicitante"));
        model.addAttribute("menu", List.of(
                Map.of("id", "busca", "rotulo", "Buscar recursos", "href", "/recursos/disponiveis", "ativo", "busca".equals(ativo)),
                Map.of("id", "minhas", "rotulo", "Minhas reservas", "href", "/reservas/minhas", "ativo", "minhas".equals(ativo))));
    }

    private static void responsavel(Model model, String ativo) {
        model.addAttribute("usuario", Map.of("nome", "Carlos Souza", "perfil", "Responsável"));
        model.addAttribute("menu", List.of(
                Map.of("id", "aprovacoes", "rotulo", "Aprovações", "href", "/aprovacoes", "ativo", "aprovacoes".equals(ativo)),
                Map.of("id", "retirada", "rotulo", "Retirada e devolução", "href", "/retirada-devolucao", "ativo", "retirada".equals(ativo)),
                Map.of("id", "relatorios", "rotulo", "Relatórios", "href", "/relatorios/utilizacao", "ativo", "relatorios".equals(ativo))));
    }

    private static void administrador(Model model) {
        model.addAttribute("usuario", Map.of("nome", "Marta Reis", "perfil", "Administrador"));
        model.addAttribute("menu", List.of(
                Map.of("id", "recursos", "rotulo", "Recursos", "href", "/admin/recursos", "ativo", true),
                Map.of("id", "relatorios", "rotulo", "Relatórios", "href", "/relatorios/utilizacao", "ativo", false)));
    }

    // Seção 02: busca, nova reserva, minhas reservas e detalhe (Solicitante)

    @GetMapping("/e2e/busca")
    String busca(Model model) {
        solicitante(model, "busca");
        model.addAttribute("filtros", new FiltrosBusca("Sala", 30, null, DIA, LocalTime.of(14, 0), LocalTime.of(15, 30)));
        model.addAttribute("tipos", List.of("Sala", "Material"));
        model.addAttribute("locais", List.of("Bloco A", "Bloco B"));
        model.addAttribute("resultados", List.of(
                Map.of("id", 7, "nome", "Sala 203", "tipo", "Sala", "local", "Bloco B, 2º andar", "capacidade", "40 lugares", "restrito", false),
                Map.of("id", 9, "nome", "Laboratório de Química", "tipo", "Sala", "local", "Bloco A, térreo", "capacidade", "30 lugares", "restrito", true)));
        return "busca";
    }

    @GetMapping("/e2e/reserva-nova/conflito")
    String novaReservaComConflito(Model model) {
        solicitante(model, "busca");
        model.addAttribute("reserva", new FormularioReserva("7", DIA, LocalTime.of(14, 0), LocalTime.of(15, 30)));
        model.addAttribute("recurso", Map.of("nome", "Sala 203", "local", "Bloco B, 2º andar", "restrito", false));
        model.addAttribute("conflito", true);
        return "reserva-nova";
    }

    @GetMapping("/e2e/reserva-nova/restrito")
    String novaReservaRestrita(Model model) {
        solicitante(model, "busca");
        model.addAttribute("reserva", new FormularioReserva("9", DIA, LocalTime.of(14, 0), LocalTime.of(15, 30)));
        model.addAttribute("recurso", Map.of("nome", "Laboratório de Química", "local", "Bloco A, térreo", "restrito", true));
        return "reserva-nova";
    }

    private static Map<String, Object> minha(int id, String recurso, String estado, boolean iniciada) {
        return Map.of("id", id, "recurso", recurso, "local", "Bloco B, 2º andar", "data", "12/11/2026",
                "horario", "14:00 às 15:30", "estado", estado, "iniciada", iniciada);
    }

    @GetMapping("/e2e/minhas")
    String minhas(Model model) {
        solicitante(model, "minhas");
        model.addAttribute("filtro", "todos");
        model.addAttribute("reservas", List.of(
                minha(42, "Sala 203", "Aprovada", false),
                minha(43, "Laboratório de Química", "Solicitada", false),
                minha(44, "Projetor portátil P-07", "Em uso", true),
                minha(45, "Sala 101", "Concluída", true)));
        return "reservas-minhas";
    }

    @GetMapping("/e2e/reserva")
    String detalhe(@RequestParam(required = false) String acao, Model model) {
        solicitante(model, "minhas");
        Map<String, Object> reserva = new HashMap<>();
        reserva.put("id", 42);
        reserva.put("recurso", "Sala 203");
        reserva.put("local", "Bloco B, 2º andar");
        reserva.put("data", DIA);
        reserva.put("inicio", LocalTime.of(14, 0));
        reserva.put("termino", LocalTime.of(15, 30));
        reserva.put("estado", "Aprovada");
        reserva.put("iniciada", false);
        model.addAttribute("reserva", reserva);
        if (acao != null) {
            model.addAttribute("acao", acao);
        }
        return "reserva-detalhe";
    }

    // Seção 03: aprovação, retirada e devolução (Responsável)

    private static Map<String, Object> pedido(int id, String solicitante, String recurso, boolean proprio) {
        return Map.of("id", id, "solicitante", solicitante, "recurso", recurso, "data", "12/11/2026",
                "horario", "08:00 às 10:00", "estado", "Solicitada", "proprio", proprio);
    }

    @GetMapping("/e2e/aprovacoes")
    String aprovacoes(@RequestParam(required = false) Integer rejeitar, Model model) {
        responsavel(model, "aprovacoes");
        List<Map<String, Object>> pedidos = List.of(
                pedido(1, "Laura Mendes", "Laboratório de Química", false),
                pedido(2, "Carlos Souza", "Laboratório de Física", true));
        model.addAttribute("pedidos", pedidos);
        if (rejeitar != null) {
            model.addAttribute("rejeitando", pedidos.get(0));
        }
        return "aprovacoes";
    }

    private static Map<String, Object> emprestimo(int id, String recurso, String estado, boolean passou) {
        return Map.of("id", id, "recurso", recurso, "solicitante", "Beatriz Nogueira", "data", "05/11/2026",
                "horario", "09:00 às 11:00", "estado", estado, "horarioPassou", passou);
    }

    @GetMapping("/e2e/retirada-devolucao")
    String retiradaDevolucao(@RequestParam(required = false) Integer naoCompareceu, Model model) {
        responsavel(model, "retirada");
        model.addAttribute("filtro", "ativas");
        List<Map<String, Object>> reservas = List.of(
                emprestimo(2, "Sala 203", "Aprovada", false),
                emprestimo(4, "Kit de microscópio M-02", "Aprovada", true),
                emprestimo(1, "Projetor portátil P-07", "Em uso", false));
        model.addAttribute("reservas", reservas);
        if (naoCompareceu != null) {
            model.addAttribute("naoCompareceu", reservas.get(1));
        }
        return "retirada-devolucao";
    }

    // Seção 04: histórico e relatório

    @GetMapping("/e2e/historico")
    String historico(Model model) {
        responsavel(model, "retirada");
        model.addAttribute("reserva", Map.of("recurso", "Laboratório de Química", "solicitante", "Laura Mendes",
                "data", DIA, "horario", "08:00 às 10:00", "estado", "Aprovada"));
        Map<String, Object> criacao = new HashMap<>();
        criacao.put("instante", LocalDateTime.of(2026, 11, 6, 9, 12));
        criacao.put("de", null);
        criacao.put("para", "Solicitada");
        criacao.put("usuario", "Laura Mendes");
        criacao.put("motivo", null);
        model.addAttribute("eventos", List.of(criacao, Map.of("instante", LocalDateTime.of(2026, 11, 6, 14, 40),
                "de", "Solicitada", "para", "Aprovada", "usuario", "Carlos Souza", "motivo", "Aula prática da turma B")));
        return "historico";
    }

    @GetMapping("/e2e/relatorio")
    String relatorio(Model model) {
        responsavel(model, "relatorios");
        model.addAttribute("filtro", Map.of("inicio", LocalDate.of(2026, 11, 1), "fim", LocalDate.of(2026, 11, 30)));
        model.addAttribute("relatorio", Map.of(
                "recursos", List.of(Map.of("nome", "Laboratório de Química com bancada de segurança", "reservas", 6, "horas", "12 h"),
                        Map.of("nome", "Sala 203", "reservas", 4, "horas", "6 h 30 min")),
                "professores", List.of(Map.of("nome", "Laura Mendes", "reservas", 5, "horas", "10 h")),
                "totalReservas", 10, "totalHoras", "18 h 30 min", "conflitos", 3));
        return "relatorio";
    }

    // Seção 05: recursos (Administrador)

    @GetMapping("/e2e/recursos")
    String recursos(Model model) {
        administrador(model);
        model.addAttribute("tipo", TipoRecurso.SALAS);
        model.addAttribute("recursos", List.of(
                Map.of("id", 7, "nome", "Sala 203", "capacidade", "40 lugares", "local", "Bloco B, 2º andar", "patrimonio", "",
                        "competencias", "", "emManutencao", false, "periodoManutencao", "", "restrito", false),
                Map.of("id", 9, "nome", "Laboratório de Química", "capacidade", "30 lugares", "local", "Bloco A, térreo", "patrimonio", "",
                        "competencias", "", "emManutencao", true, "periodoManutencao", "10/11/2026 a 20/11/2026", "restrito", true)));
        return "recursos";
    }

    // Páginas de erro: o status passa pelo tratamento de erro real do Spring (templates/error)

    @GetMapping("/e2e/erro/{status}")
    String erro(@PathVariable int status) {
        throw new ResponseStatusException(HttpStatus.valueOf(status));
    }
}
