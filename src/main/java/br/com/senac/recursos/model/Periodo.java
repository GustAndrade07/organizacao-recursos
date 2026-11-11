package br.com.senac.recursos.model;

import br.com.senac.recursos.exception.RequisicaoInvalidaException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Intervalo de tempo semiaberto {@code [inicio, fim)} (D1, ADR-0002): uma reserva que termina exatamente quando outra
 * começa não sobrepõe. É o espelho, em Java, do {@code tstzrange} da V1; a conversão fica em
 * {@code converter.PeriodoUserType}.
 *
 * <p>Os instantes são truncados em microssegundos, a precisão do {@code timestamptz} do PostgreSQL, para o valor lido
 * do banco ser igual ao gravado.
 *
 * @param inicio instante inicial, incluído
 * @param fim instante final, excluído; sempre depois do início (RN-01)
 */
public record Periodo(Instant inicio, Instant fim) {

    public Periodo {
        if (inicio == null || fim == null) {
            throw new RequisicaoInvalidaException("Início e término são obrigatórios.");
        }
        inicio = inicio.truncatedTo(ChronoUnit.MICROS);
        fim = fim.truncatedTo(ChronoUnit.MICROS);
        if (!fim.isAfter(inicio)) {
            throw new RequisicaoInvalidaException("O término precisa ser depois do início.");
        }
    }

    /** Sobreposição no sentido do operador {@code &&} do PostgreSQL para intervalos {@code [)}. */
    public boolean sobrepoe(Periodo outro) {
        Objects.requireNonNull(outro, "outro");
        return inicio.isBefore(outro.fim) && outro.inicio.isBefore(fim);
    }

    /** {@code true} se o instante já chegou ao início do período (RN-07: a reserva começou). */
    public boolean iniciadoEm(Instant agora) {
        return !agora.isBefore(inicio);
    }

    public Duration duracao() {
        return Duration.between(inicio, fim);
    }

    /** Literal aceito pelo PostgreSQL como {@code tstzrange}, ex.: {@code [2026-11-10T10:00:00Z,2026-11-10T11:00:00Z)}. */
    public String comoTstzrange() {
        return "[" + inicio + "," + fim + ")";
    }
}
