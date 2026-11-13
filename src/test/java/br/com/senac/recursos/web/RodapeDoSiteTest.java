package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** CA-14: o ano do rodapé vem do {@code Clock} injetado, no fuso das telas (São Paulo), nunca do HTML. */
class RodapeDoSiteTest {

    private static RodapeDoSite comRelogioEm(String instanteComOffset) {
        Clock relogio = Clock.fixed(OffsetDateTime.parse(instanteComOffset).toInstant(), ZoneOffset.UTC);
        return new RodapeDoSite(new FormatoDasTelas("America/Sao_Paulo", relogio));
    }

    @Test
    void anoDoRelogioNoFusoDasTelas() {
        assertThat(comRelogioEm("2026-11-13T10:00:00-03:00").ano()).isEqualTo(2026);
    }

    @Test
    void ultimaHoraDoAnoEmSaoPauloAindaEOAnoAntigoMesmoJaSendoOutroAnoEmUtc() {
        // 23:30 de 31/12 em São Paulo já é 02:30 de 01/01 em UTC: o rodapé não pode virar de ano três horas antes.
        assertThat(comRelogioEm("2026-12-31T23:30:00-03:00").ano()).isEqualTo(2026);
    }

    @Test
    void primeiraHoraDoAnoNovoEmSaoPaulo() {
        assertThat(comRelogioEm("2027-01-01T00:30:00-03:00").ano()).isEqualTo(2027);
    }
}
