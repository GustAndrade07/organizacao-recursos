package br.com.senac.recursos.controller;

import br.com.senac.recursos.converter.PeriodoConverter;
import br.com.senac.recursos.dto.request.PeriodoRequest;
import br.com.senac.recursos.dto.request.RecursoAtualizacaoRequest;
import br.com.senac.recursos.dto.request.RecursoRequest;
import br.com.senac.recursos.dto.response.ManutencaoResponse;
import br.com.senac.recursos.dto.response.RecursoResponse;
import br.com.senac.recursos.enums.TipoRecurso;
import br.com.senac.recursos.service.ManutencaoService;
import br.com.senac.recursos.service.RecursoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Salas, materiais e professores (RF-02, RF-03) e manutenções (RF-08). Escrita só para Administrador (matriz). */
@RestController
@RequestMapping("/api/recursos")
public class RecursoApiController {

    private final RecursoService recursos;
    private final ManutencaoService manutencoes;
    private final PeriodoConverter periodos;

    public RecursoApiController(RecursoService recursos, ManutencaoService manutencoes, PeriodoConverter periodos) {
        this.recursos = recursos;
        this.manutencoes = manutencoes;
        this.periodos = periodos;
    }

    @GetMapping
    public List<RecursoResponse> listar(@RequestParam(required = false) TipoRecurso tipo,
            @RequestParam(required = false) @Positive Integer capacidadeMinima) {
        return recursos.listar(tipo, capacidadeMinima);
    }

    /** RF-03: recursos livres em {@code [inicio, fim)}, sem reserva que ocupe o horário e sem manutenção. */
    @GetMapping("/disponiveis")
    public List<RecursoResponse> disponiveis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim,
            @RequestParam(required = false) TipoRecurso tipo,
            @RequestParam(required = false) @Positive Integer capacidadeMinima) {
        return recursos.disponiveis(periodos.paraModelo(inicio, fim), tipo, capacidadeMinima);
    }

    @GetMapping("/{id}")
    public RecursoResponse buscar(@PathVariable Long id) {
        return recursos.buscar(id);
    }

    @PostMapping
    public ResponseEntity<RecursoResponse> criar(@Valid @RequestBody RecursoRequest pedido) {
        RecursoResponse criado = recursos.criar(pedido);
        return ResponseEntity.created(URI.create("/api/recursos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public RecursoResponse atualizar(@PathVariable Long id, @Valid @RequestBody RecursoAtualizacaoRequest pedido) {
        return recursos.atualizar(id, pedido);
    }

    @PostMapping("/{id}/manutencoes")
    public ResponseEntity<ManutencaoResponse> bloquear(@PathVariable Long id,
            @Valid @RequestBody PeriodoRequest pedido) {
        ManutencaoResponse criada = manutencoes.criar(id, periodos.paraModelo(pedido));
        return ResponseEntity.created(URI.create("/api/recursos/" + id + "/manutencoes/" + criada.id())).body(criada);
    }

    @GetMapping("/{id}/manutencoes")
    public List<ManutencaoResponse> manutencoes(@PathVariable Long id) {
        return manutencoes.listar(id);
    }
}
