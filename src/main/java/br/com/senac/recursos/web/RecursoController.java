package br.com.senac.recursos.web;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Telas 13, 14 e 15 do inventário: lista de recursos, formulário de recurso e bloqueio por manutenção
 * (RF-02, RF-08). Só as telas: devolvem à view o que veio na URL. Quem lista, cadastra, edita e bloqueia
 * é o back-end, que deve responder a {@code POST /admin/recursos}, {@code POST /admin/recursos/{id}} e
 * {@code POST /admin/recursos/{id}/manutencao} e preencher o resto do modelo
 * ({@code recursos}, {@code confirmacao}, {@code recurso}, {@code erros}, {@code afetadas}); o contrato
 * está em docs/frontend/README.md.
 */
@Controller
public class RecursoController {

    @GetMapping("/admin/recursos")
    public String lista(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String nome,
            Model model) {
        model.addAttribute("tipo", TipoRecurso.da(tipo));
        model.addAttribute("nome", vazioParaNulo(nome));
        return "recursos";
    }

    /**
     * Também recebe o que já foi digitado: a troca de tipo recarrega a página por GET. Tipo desconhecido
     * ou {@code restrito} que não seja booleano respondem 400.
     */
    @GetMapping("/admin/recursos/novo")
    public String novo(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String capacidade,
            @RequestParam(required = false) String localizacao,
            @RequestParam(required = false) String patrimonio,
            @RequestParam(required = false) String competencias,
            @RequestParam(defaultValue = "false") boolean restrito,
            Model model) {
        model.addAttribute("tipo", TipoRecurso.exigir(tipo));
        model.addAttribute("recurso", new FormularioRecurso(null, nome, capacidade, localizacao, patrimonio,
                competencias, restrito));
        return "recurso-form";
    }

    /** O back-end sobrescreve {@code recurso} com os dados gravados e {@code tipo} com o tipo do recurso. */
    @GetMapping("/admin/recursos/{id}")
    public String editar(@PathVariable String id, @RequestParam(required = false) String tipo, Model model) {
        model.addAttribute("tipo", TipoRecurso.exigir(tipo));
        model.addAttribute("recurso", new FormularioRecurso(id, null, null, null, null, null, false));
        return "recurso-form";
    }

    /** O {@code tipo} só serve para a tela voltar à aba certa da lista. */
    @GetMapping("/admin/recursos/{id}/manutencao")
    public String manutencao(
            @PathVariable String id,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) String motivo,
            Model model) {
        model.addAttribute("tipo", TipoRecurso.exigir(tipo));
        model.addAttribute("bloqueio", new FormularioBloqueio(id, inicio, fim, motivo));
        return "manutencao";
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
