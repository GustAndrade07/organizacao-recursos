package br.com.senac.recursos.web;

import br.com.senac.recursos.dto.response.RecursoResponse;
import br.com.senac.recursos.dto.response.ReservaResponse;
import br.com.senac.recursos.enums.EstadoReserva;
import br.com.senac.recursos.enums.TipoRecurso;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Converte o que os services devolvem (instantes em UTC, enums) no que as telas mostram: data e hora no fuso da
 * instituição, nomes de estado por extenso (o contrato de {@code fragments/estado.html}) e textos prontos ("14:00 às
 * 15:30", "40 lugares"). É o único lugar que conhece o fuso das telas ({@code recursos.telas.fuso-horario}).
 */
@Component
public class FormatoDasTelas {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ZoneId fuso;
    private final Clock relogio;

    public FormatoDasTelas(@Value("${recursos.telas.fuso-horario:America/Sao_Paulo}") String fuso, Clock relogio) {
        this.fuso = ZoneId.of(fuso);
        this.relogio = relogio;
    }

    public ZoneId fuso() {
        return fuso;
    }

    public Instant agora() {
        return relogio.instant();
    }

    public Instant instante(LocalDate data, LocalTime hora) {
        return data.atTime(hora).atZone(fuso).toInstant();
    }

    public Instant instante(LocalDateTime dataHora) {
        return dataHora.atZone(fuso).toInstant();
    }

    public LocalDate data(Instant instante) {
        return instante.atZone(fuso).toLocalDate();
    }

    public LocalTime hora(Instant instante) {
        return instante.atZone(fuso).toLocalTime();
    }

    public LocalDateTime dataHora(Instant instante) {
        return instante.atZone(fuso).toLocalDateTime();
    }

    public String textoData(Instant instante) {
        return DATA.format(instante.atZone(fuso));
    }

    /** "14:00 às 15:30". */
    public String horario(Instant inicio, Instant fim) {
        return HORA.format(inicio.atZone(fuso)) + " às " + HORA.format(fim.atZone(fuso));
    }

    /** "10/11/2026 a 20/11/2026" (ou só a data, se começa e termina no mesmo dia). */
    public String periodoEmDias(Instant inicio, Instant fim) {
        String de = textoData(inicio);
        String ate = textoData(fim);
        return de.equals(ate) ? de : de + " a " + ate;
    }

    /** Nomes do contrato de {@code fragments/estado.html} (RN-05). */
    public static String estado(EstadoReserva estado) {
        return switch (estado) {
            case SOLICITADA -> "Solicitada";
            case APROVADA -> "Aprovada";
            case EM_USO -> "Em uso";
            case CONCLUIDA -> "Concluída";
            case REJEITADA -> "Rejeitada";
            case CANCELADA -> "Cancelada";
            case NAO_COMPARECEU -> "Não compareceu";
        };
    }

    public static String tipo(TipoRecurso tipo) {
        return switch (tipo) {
            case SALA -> "Sala";
            case MATERIAL -> "Material";
            case PROFESSOR -> "Professor";
        };
    }

    /** "40 lugares"; sem capacidade (material, professor), "Não se aplica". */
    public static String capacidade(Integer capacidade) {
        if (capacidade == null) {
            return "Não se aplica";
        }
        return capacidade == 1 ? "1 lugar" : capacidade + " lugares";
    }

    /** "18 h", "12 h 30 min", "0 h"; arredonda para o minuto mais próximo. */
    public static String horas(BigDecimal horas) {
        long minutos = horas == null ? 0
                : horas.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        long h = minutos / 60;
        long m = minutos % 60;
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }

    /** Os recursos da reserva, como a tela mostra: "Sala 203" ou "Sala 203, Prof. Ana". */
    public static String recursos(ReservaResponse reserva) {
        return reserva.recursos().stream().map(RecursoResponse::nome).collect(Collectors.joining(", "));
    }

    /** O horário de início já chegou (RN-07: a reserva começou). */
    public boolean iniciada(ReservaResponse reserva) {
        return !agora().isBefore(reserva.inicio());
    }

    /** O horário da reserva já terminou. */
    public boolean terminada(ReservaResponse reserva) {
        return !agora().isBefore(reserva.fim());
    }
}
