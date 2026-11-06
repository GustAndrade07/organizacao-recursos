package br.com.senac.recursos.web;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Filtros da busca de disponibilidade (tela 4). Todos opcionais: o formulário volta preenchido com o que
 * o usuário já escolheu, e o link de reserva leva data e horários adiante.
 */
public record FiltrosBusca(String tipo, Integer capacidade, String local, LocalDate data, LocalTime inicio, LocalTime termino) {
}
