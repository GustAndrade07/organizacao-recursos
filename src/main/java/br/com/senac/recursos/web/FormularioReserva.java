package br.com.senac.recursos.web;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Dados do formulário de nova reserva (tela 6). Vêm da busca pela URL e voltam preenchidos se o
 * back-end recusar o envio.
 */
public record FormularioReserva(String recursoId, LocalDate data, LocalTime inicio, LocalTime termino) {
}
