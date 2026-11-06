package br.com.senac.recursos.web;

import java.time.LocalDate;

/** Período do relatório de utilização (tela 12): a página volta com o que o usuário já escolheu. */
public record FiltroRelatorio(LocalDate inicio, LocalDate fim) {
}
