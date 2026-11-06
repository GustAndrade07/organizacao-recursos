package br.com.senac.recursos.web;

import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Telas 4 e 5 do inventário: buscar disponibilidade e resultados (RF-03), mesma rota e mesmo template.
 * Só a tela: devolve os filtros recebidos ao formulário. Quem consulta a disponibilidade é o back-end, que
 * preenche o atributo {@code resultados} (e {@code tipos} e {@code locais}, para as listas de escolha);
 * sem {@code resultados} a página mostra o formulário. O parâmetro {@code editar} serve para voltar ao
 * formulário com os filtros preenchidos.
 */
@Controller
public class BuscaController {

    @GetMapping("/recursos/disponiveis")
    public String buscar(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Integer capacidade,
            @RequestParam(required = false) String local,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime termino,
            Model model) {
        model.addAttribute("filtros", new FiltrosBusca(
                vazioParaNulo(tipo), capacidade, vazioParaNulo(local), data, inicio, termino));
        return "busca";
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
