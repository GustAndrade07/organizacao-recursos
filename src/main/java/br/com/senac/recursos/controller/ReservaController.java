package br.com.senac.recursos.controller;

import br.com.senac.recursos.converter.PeriodoConverter;
import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.dto.request.MotivoRequest;
import br.com.senac.recursos.dto.request.PeriodoRequest;
import br.com.senac.recursos.dto.request.ReservaRequest;
import br.com.senac.recursos.dto.response.AuditoriaResponse;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.enums.EstadoReserva;
import br.com.senac.recursos.service.ReservaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reservas (RF-04 a RF-07, RF-09, RF-10). Cada mudança de estado é um {@code POST} num sub-recurso com nome de ação
 * ({@code /aprovacao}, {@code /cancelamento}...), e não um {@code PATCH} do estado: assim cada transição tem a sua linha
 * na matriz de autorização e o seu perfil. O corpo (motivo) é opcional.
 */
@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservas;
    private final PeriodoConverter periodos;

    public ReservaController(ReservaService reservas, PeriodoConverter periodos) {
        this.reservas = reservas;
        this.periodos = periodos;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> criar(@Valid @RequestBody ReservaRequest pedido,
            UsuarioAutenticado quem) {
        ReservaResponse criada = reservas.criar(pedido.recursoIds(), periodos.paraModelo(pedido), quem);
        return ResponseEntity.created(URI.create("/reservas/" + criada.id())).body(criada);
    }

    @GetMapping
    public List<ReservaResponse> listar(@RequestParam(required = false) EstadoReserva estado,
            UsuarioAutenticado quem) {
        return reservas.listar(estado, quem);
    }

    @GetMapping("/{id}")
    public ReservaResponse buscar(@PathVariable Long id, UsuarioAutenticado quem) {
        return reservas.buscar(id, quem);
    }

    @PutMapping("/{id}/periodo")
    public ReservaResponse alterarPeriodo(@PathVariable Long id, @Valid @RequestBody PeriodoRequest pedido,
            UsuarioAutenticado quem) {
        return reservas.alterarPeriodo(id, periodos.paraModelo(pedido), quem);
    }

    @PostMapping("/{id}/cancelamento")
    public ReservaResponse cancelar(@PathVariable Long id,
            @Valid @RequestBody(required = false) MotivoRequest pedido, UsuarioAutenticado quem) {
        return reservas.cancelar(id, motivo(pedido), quem);
    }

    @PostMapping("/{id}/aprovacao")
    public ReservaResponse aprovar(@PathVariable Long id,
            @Valid @RequestBody(required = false) MotivoRequest pedido, UsuarioAutenticado quem) {
        return reservas.aprovar(id, motivo(pedido), quem);
    }

    @PostMapping("/{id}/rejeicao")
    public ReservaResponse rejeitar(@PathVariable Long id,
            @Valid @RequestBody(required = false) MotivoRequest pedido, UsuarioAutenticado quem) {
        return reservas.rejeitar(id, motivo(pedido), quem);
    }

    @PostMapping("/{id}/retirada")
    public ReservaResponse registrarRetirada(@PathVariable Long id,
            @Valid @RequestBody(required = false) MotivoRequest pedido, UsuarioAutenticado quem) {
        return reservas.registrarRetirada(id, motivo(pedido), quem);
    }

    @PostMapping("/{id}/devolucao")
    public ReservaResponse registrarDevolucao(@PathVariable Long id,
            @Valid @RequestBody(required = false) MotivoRequest pedido, UsuarioAutenticado quem) {
        return reservas.registrarDevolucao(id, motivo(pedido), quem);
    }

    @PostMapping("/{id}/nao-comparecimento")
    public ReservaResponse registrarNaoComparecimento(@PathVariable Long id,
            @Valid @RequestBody(required = false) MotivoRequest pedido, UsuarioAutenticado quem) {
        return reservas.registrarNaoComparecimento(id, motivo(pedido), quem);
    }

    /** Histórico auditável (RF-10): só Responsável e Administrador (RN-11, matriz). */
    @GetMapping("/{id}/historico")
    public List<AuditoriaResponse> historico(@PathVariable Long id) {
        return reservas.historico(id);
    }

    private static String motivo(MotivoRequest pedido) {
        return pedido == null || pedido.motivo() == null || pedido.motivo().isBlank() ? null : pedido.motivo().trim();
    }
}
