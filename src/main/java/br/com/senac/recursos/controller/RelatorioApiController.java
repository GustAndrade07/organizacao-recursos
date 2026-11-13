package br.com.senac.recursos.controller;

import br.com.senac.recursos.converter.PeriodoConverter;
import br.com.senac.recursos.dto.response.RelatorioUtilizacaoResponse;
import br.com.senac.recursos.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Relatório de utilização (RF-12). Só Responsável e Administrador: sem token 401, Solicitante 403 (RN-13). */
@RestController
@RequestMapping("/api/relatorios")
@Tag(name = "Relatórios", description = "Relatório de utilização (RF-12)")
public class RelatorioApiController {

    private final RelatorioService relatorios;
    private final PeriodoConverter periodos;

    public RelatorioApiController(RelatorioService relatorios, PeriodoConverter periodos) {
        this.relatorios = relatorios;
        this.periodos = periodos;
    }

    /** Janela {@code [inicio, fim)} em ISO-8601 com deslocamento, ex.: {@code 2026-11-10T00:00:00-03:00}. */
    @Operation(summary = "Utilização por recurso e por professor na janela pedida",
            description = "Devolve, para a janela [inicio, fim): horas por recurso, carga por professor e o total "
                    + "de conflitos evitados (tentativas recusadas por sobreposição).\n\n"
                    + "Só Responsável e Administrador (RN-13). O fim da janela não entra (decisão D1, ADR-0002) e "
                    + "reserva cancelada não conta.\n\n"
                    + "As premissas das consultas (quais estados contam como uso, o que é o \"período\" dos "
                    + "conflitos evitados, recurso sem uso e quem é o \"professor\") seguem **pendentes de decisão "
                    + "do grupo**: D8 de `docs/plano-execucao.md`.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relatório da janela"),
        @ApiResponse(responseCode = "400", description = "Janela ausente, mal formada ou com fim antes do início",
                content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(responseCode = "401", description = "Sem token ou token inválido",
                content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(responseCode = "403", description = "Solicitante não acessa o relatório (RN-13)",
                content = @Content(mediaType = "application/problem+json"))
    })
    @GetMapping("/utilizacao")
    public RelatorioUtilizacaoResponse utilizacao(
            @Parameter(description = "Início da janela, incluído. Ex.: 2026-11-10T00:00:00-03:00", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @Parameter(description = "Fim da janela, não incluído (D1). Ex.: 2026-11-11T00:00:00-03:00",
                    required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim) {
        return relatorios.utilizacao(periodos.paraModelo(inicio, fim));
    }
}
