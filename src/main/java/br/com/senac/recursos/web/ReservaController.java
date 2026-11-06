package br.com.senac.recursos.web;

import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela 6 do inventário: nova reserva (RF-04 a RF-06). Só a tela: preenche o formulário com o que veio da
 * busca. Quem grava é o back-end, que deve responder a {@code POST /reservas} e, se recusar, voltar a esta
 * tela com {@code erros}, {@code conflito} ou {@code manutencao} no modelo (docs/frontend/README.md).
 * O atributo {@code recurso} (nome, local, restrito) também é do back-end.
 */
@Controller
public class ReservaController {

    @GetMapping("/reservas/nova")
    public String nova(
            @RequestParam(required = false) String recursoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime termino,
            Model model) {
        model.addAttribute("reserva", new FormularioReserva(recursoId, data, inicio, termino));
        return "reserva-nova";
    }
}
