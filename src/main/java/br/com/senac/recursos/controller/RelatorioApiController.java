package br.com.senac.recursos.controller;

import br.com.senac.recursos.converter.PeriodoConverter;
import br.com.senac.recursos.dto.response.RelatorioUtilizacaoResponse;
import br.com.senac.recursos.service.RelatorioService;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Relatório de utilização (RF-12). Só Responsável e Administrador: sem token 401, Solicitante 403 (RN-13). */
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioApiController {

    private final RelatorioService relatorios;
    private final PeriodoConverter periodos;

    public RelatorioApiController(RelatorioService relatorios, PeriodoConverter periodos) {
        this.relatorios = relatorios;
        this.periodos = periodos;
    }

    /** Janela {@code [inicio, fim)} em ISO-8601 com deslocamento, ex.: {@code 2026-11-10T00:00:00-03:00}. */
    @GetMapping("/utilizacao")
    public RelatorioUtilizacaoResponse utilizacao(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim) {
        return relatorios.utilizacao(periodos.paraModelo(inicio, fim));
    }
}
