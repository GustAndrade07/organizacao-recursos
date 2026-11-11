package br.com.senac.recursos.converter;

import br.com.senac.recursos.model.Periodo;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Objects;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * Converte {@link Periodo} de e para a coluna {@code tstzrange} do PostgreSQL, que o Hibernate não conhece.
 *
 * <p>Escrita: o literal {@code [início,fim)} em ISO-8601 vai como {@link Types#OTHER}; o driver o envia sem tipo e o
 * PostgreSQL o interpreta como {@code tstzrange} pelo contexto da coluna. Leitura: o driver devolve o texto do
 * intervalo no formato do servidor, ex.: {@code ["2026-11-10 10:00:00+00","2026-11-10 11:00:00+00")}, com o
 * deslocamento do fuso da sessão (que pode vir como {@code -03}, {@code +05:30} ou {@code -03:06:28}).
 *
 * <p>Só aceita o formato que a V1 permite guardar: limite inferior fechado e superior aberto, sem infinito e sem
 * intervalo vazio (constraints {@code *_periodo_semiaberto}).
 */
public class PeriodoUserType implements UserType<Periodo> {

    /** {@code 2026-11-10 10:00:00.123456-03:00}: data, espaço, hora com fração opcional e deslocamento opcional em minutos e segundos. */
    private static final DateTimeFormatter FORMATO_POSTGRES = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .appendOffset("+HH:mm:ss", "Z")
            .toFormatter();

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<Periodo> returnedClass() {
        return Periodo.class;
    }

    @Override
    public boolean equals(Periodo x, Periodo y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Periodo x) {
        return Objects.hashCode(x);
    }

    @Override
    public Periodo nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        Object valor = rs.getObject(position);
        return valor == null ? null : deTexto(valor.toString());
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Periodo value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setObject(index, value.comoTstzrange(), Types.OTHER);
        }
    }

    @Override
    public Periodo deepCopy(Periodo value) {
        return value; // record imutável
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Periodo value) {
        return value == null ? null : value.comoTstzrange();
    }

    @Override
    public Periodo assemble(Serializable cached, Object owner) {
        return cached == null ? null : deTexto(cached.toString());
    }

    /**
     * Lê o texto de um {@code tstzrange} (como o servidor o imprime, ou como {@link Periodo#comoTstzrange()} o
     * escreve).
     *
     * @throws IllegalArgumentException se o intervalo não for {@code [início, fim)} com os dois limites finitos
     */
    public static Periodo deTexto(String texto) {
        String valor = texto.trim();
        if (valor.length() < 5 || valor.charAt(0) != '[' || valor.charAt(valor.length() - 1) != ')') {
            throw new IllegalArgumentException("Intervalo fora do formato [início,fim): " + valor);
        }
        String[] limites = valor.substring(1, valor.length() - 1).split(",", -1);
        if (limites.length != 2) {
            throw new IllegalArgumentException("Intervalo fora do formato [início,fim): " + valor);
        }
        return new Periodo(instante(limites[0]), instante(limites[1]));
    }

    private static Instant instante(String limite) {
        String semAspas = limite.trim().replace("\"", "");
        if (semAspas.isEmpty() || semAspas.contains("infinity")) {
            throw new IllegalArgumentException("Limite de intervalo ausente ou infinito: " + limite);
        }
        if (semAspas.indexOf('T') > 0) {
            return OffsetDateTime.parse(semAspas).toInstant();
        }
        return OffsetDateTime.parse(semAspas, FORMATO_POSTGRES).toInstant();
    }
}
