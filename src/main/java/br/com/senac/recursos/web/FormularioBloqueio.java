package br.com.senac.recursos.web;

import java.time.LocalDateTime;

/**
 * Dados do formulário de bloqueio por manutenção (tela 15). Vêm da URL (ao voltar para ajustar o período)
 * e voltam preenchidos se o back-end recusar o envio.
 */
public record FormularioBloqueio(String recursoId, LocalDateTime inicio, LocalDateTime fim, String motivo) {
}
