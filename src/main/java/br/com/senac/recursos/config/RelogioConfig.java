package br.com.senac.recursos.config;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Único ponto onde o sistema lê a hora (AGENTS.md, seção 5): toda regra de tempo recebe este {@link Clock} injetado
 * (criação da reserva, RN-07, auditoria, tentativa recusada, expiração do token). Os testes trocam por um relógio fixo.
 */
@Configuration(proxyBeanMethods = false)
public class RelogioConfig {

    /** Em microssegundos, a precisão do {@code timestamptz}: o instante devolvido na resposta é o mesmo que fica gravado. */
    @Bean
    public Clock relogio() {
        return Clock.tick(Clock.systemUTC(), Duration.of(1, ChronoUnit.MICROS));
    }
}
