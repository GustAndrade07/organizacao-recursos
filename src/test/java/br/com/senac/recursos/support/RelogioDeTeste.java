package br.com.senac.recursos.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Relógio que o teste controla: as regras de tempo (RN-07, expiração do token, instante da auditoria) ficam
 * determinísticas e nenhum teste usa {@code Thread.sleep} (AGENTS.md, seção 5).
 */
public class RelogioDeTeste extends Clock {

    private volatile Instant agora;

    public RelogioDeTeste(Instant inicio) {
        this.agora = inicio;
    }

    public void ajustar(Instant instante) {
        this.agora = instante;
    }

    public void avancar(Duration duracao) {
        this.agora = agora.plus(duracao);
    }

    @Override
    public Instant instant() {
        return agora;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }
}
