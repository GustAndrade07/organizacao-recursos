package br.com.senac.recursos.banco;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * RF-12 no nível do banco: as consultas do relatório de utilização (horas por recurso, carga por professor, conflitos
 * evitados). O SQL testado é o arquivo de {@code src/main/resources/db/consultas/}, o mesmo que o repositório do
 * back-end (Pedro) pode carregar; o teste não repete a consulta.
 *
 * <p>Todos os valores esperados foram calculados à mão no comentário de cada teste. A janela do relatório é um
 * intervalo semiaberto {@code [início, fim)} (D1) já pronto: a conversão das datas do endpoint em intervalo (fuso,
 * fim inclusivo) é do serviço e não é testada aqui.
 *
 * <p>Definições assumidas e ainda não confirmadas pelo grupo (PRD não decide): contam como uso os estados que ocupam
 * horário na D5 (SOLICITADA, APROVADA, EM_USO, CONCLUIDA); "conflitos evitados no período" = tentativas recusadas
 * por sobreposição com {@code registrada_em} dentro da janela; recurso sem uso no período não gera linha.
 */
class RelatorioUtilizacaoBancoTest extends BancoComDadosTest {

    private static final String HORAS_POR_RECURSO = "relatorio-horas-por-recurso.sql";
    private static final String CARGA_POR_PROFESSOR = "relatorio-carga-por-professor.sql";
    private static final String CONFLITOS_EVITADOS = "relatorio-conflitos-evitados.sql";

    /** Janela de 24 h: o dia 10/11/2026 inteiro, de 00:00 (incluso) a 11/11 00:00 (excluso). */
    private static final String DIA_10 = faixa("2026-11-10T00:00:00Z", "2026-11-11T00:00:00Z");

    private long ana;
    private long bruno;
    private long sala101;
    private long projetor;
    private long marta;
    private long paulo;

    @BeforeEach
    void cenarioBase() {
        ana = novoUsuario("ana");
        bruno = novoUsuario("bruno");
        sala101 = novoRecurso("SALA", "Sala 101");
        projetor = novoRecurso("MATERIAL", "Projetor");
        marta = novoRecurso("PROFESSOR", "Marta");
        paulo = novoRecurso("PROFESSOR", "Paulo");
    }

    @Test
    void horasPorRecursoSomamAsReservasDaJanela() {
        // Sala 101: 09:00-11:00 (2 h) + 14:00-15:30 (1,5 h) = 3,5 h. Projetor: 13:00-14:00 = 1 h.
        reservar(ana, "APROVADA", faixa("2026-11-10T09:00:00Z", "2026-11-10T11:00:00Z"), sala101);
        reservar(bruno, "CONCLUIDA", faixa("2026-11-10T14:00:00Z", "2026-11-10T15:30:00Z"), sala101);
        reservar(bruno, "SOLICITADA", faixa("2026-11-10T13:00:00Z", "2026-11-10T14:00:00Z"), projetor);

        Map<Long, BigDecimal> horas = horasPor(HORAS_POR_RECURSO, DIA_10);

        assertThat(horas).containsOnlyKeys(sala101, projetor);
        assertThat(horas.get(sala101)).isEqualByComparingTo("3.5");
        assertThat(horas.get(projetor)).isEqualByComparingTo("1");
    }

    @Test
    void periodoSemReservasNaoTrazLinhaNenhuma() {
        // Há reserva de 2 h no dia 10, mas a janela é o dia 01/12: nenhuma interseção, nenhuma linha, 0 conflito.
        reservar(ana, "APROVADA", faixa("2026-11-10T09:00:00Z", "2026-11-10T11:00:00Z"), sala101, marta);
        String dezembro = faixa("2026-12-01T00:00:00Z", "2026-12-02T00:00:00Z");

        assertThat(horasPor(HORAS_POR_RECURSO, dezembro)).isEmpty();
        assertThat(horasPor(CARGA_POR_PROFESSOR, dezembro)).isEmpty();
        assertThat(conflitosEvitados(dezembro)).isZero();
    }

    @Test
    void reservaQueCruzaABordaContaSoAParteDentroDaJanela() {
        // Projetor 09/11 22:00 a 10/11 02:00: são 4 h no total, mas só 00:00-02:00 = 2 h estão na janela.
        reservar(ana, "EM_USO", faixa("2026-11-09T22:00:00Z", "2026-11-10T02:00:00Z"), projetor);
        // Sala 101 10/11 23:00 a 11/11 03:00: só 23:00-24:00 = 1 h está na janela.
        reservar(bruno, "APROVADA", faixa("2026-11-10T23:00:00Z", "2026-11-11T03:00:00Z"), sala101);
        // Marta termina exatamente às 00:00 do dia 10 e Paulo começa exatamente às 00:00 do dia 11 (D1, [início, fim)):
        // não há interseção com a janela, então 0 h e nenhuma linha.
        reservar(ana, "CONCLUIDA", faixa("2026-11-09T20:00:00Z", "2026-11-10T00:00:00Z"), marta);
        reservar(bruno, "SOLICITADA", faixa("2026-11-11T00:00:00Z", "2026-11-11T02:00:00Z"), paulo);

        Map<Long, BigDecimal> horas = horasPor(HORAS_POR_RECURSO, DIA_10);

        assertThat(horas).containsOnlyKeys(projetor, sala101);
        assertThat(horas.get(projetor)).isEqualByComparingTo("2");
        assertThat(horas.get(sala101)).isEqualByComparingTo("1");
    }

    @Test
    void reservaCanceladaRejeitadaOuQueNaoCompareceuNaoContaComoUso() {
        // Mesmo horário 09:00-11:00 da Sala 101: a aprovada conta 2 h; as outras três liberam o horário (D5) e valem 0.
        reservar(ana, "CANCELADA", faixa("2026-11-10T09:00:00Z", "2026-11-10T11:00:00Z"), sala101);
        reservar(bruno, "REJEITADA", faixa("2026-11-10T09:00:00Z", "2026-11-10T11:00:00Z"), sala101);
        reservar(ana, "NAO_COMPARECEU", faixa("2026-11-10T09:00:00Z", "2026-11-10T11:00:00Z"), sala101);
        reservar(bruno, "APROVADA", faixa("2026-11-10T09:00:00Z", "2026-11-10T11:00:00Z"), sala101);
        // Projetor só tem reserva cancelada: não aparece.
        reservar(ana, "CANCELADA", faixa("2026-11-10T13:00:00Z", "2026-11-10T14:00:00Z"), projetor);

        Map<Long, BigDecimal> horas = horasPor(HORAS_POR_RECURSO, DIA_10);

        assertThat(horas).containsOnlyKeys(sala101);
        assertThat(horas.get(sala101)).isEqualByComparingTo("2");
    }

    @Test
    void cargaPorProfessorSomaSoOsRecursosDoTipoProfessor() {
        // Marta: 09:00-11:00 (2 h, junto com a Sala 101) + 13:00-14:00 (1 h) = 3 h.
        // Paulo: só uma reserva cancelada, 0 h, não aparece. A Sala 101 não entra na carga de professor.
        reservar(ana, "APROVADA", faixa("2026-11-10T09:00:00Z", "2026-11-10T11:00:00Z"), sala101, marta);
        reservar(bruno, "SOLICITADA", faixa("2026-11-10T13:00:00Z", "2026-11-10T14:00:00Z"), marta);
        reservar(ana, "CANCELADA", faixa("2026-11-10T09:00:00Z", "2026-11-10T11:00:00Z"), paulo);

        Map<Long, BigDecimal> carga = horasPor(CARGA_POR_PROFESSOR, DIA_10);

        assertThat(carga).containsOnlyKeys(marta);
        assertThat(carga.get(marta)).isEqualByComparingTo("3");
    }

    @Test
    void conflitosEvitadosContamTentativasPorSobreposicaoRegistradasNaJanela() {
        // Janela [10/11 00:00, 11/11 00:00). Dentro: 10/11 08:00 e 10/11 23:59:59 = 2.
        // Fora: 09/11 23:59:59 (antes) e 11/11 00:00:00 (o fim é excluso, D1) = não contam.
        String pedido = faixa("2026-11-12T10:00:00Z", "2026-11-12T12:00:00Z");
        tentativaRegistradaEm(sala101, bruno, pedido, "2026-11-10T08:00:00Z");
        tentativaRegistradaEm(sala101, ana, pedido, "2026-11-10T23:59:59Z");
        tentativaRegistradaEm(projetor, ana, pedido, "2026-11-09T23:59:59Z");
        tentativaRegistradaEm(marta, bruno, pedido, "2026-11-11T00:00:00Z");

        assertThat(conflitosEvitados(DIA_10)).isEqualTo(2);
    }

    private static String faixa(String inicio, String fim) {
        return "[" + inicio + "," + fim + ")";
    }

    /** Lê o SQL do relatório de {@code src/main/resources/db/consultas/}; arquivo ausente faz o teste falhar. */
    private static String sql(String arquivo) {
        try (var entrada = new ClassPathResource("db/consultas/" + arquivo).getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException erro) {
            throw new UncheckedIOException("consulta do relatório não encontrada: " + arquivo, erro);
        }
    }

    /** Executa a consulta de horas com a janela como único parâmetro e devolve recurso -> horas. */
    private Map<Long, BigDecimal> horasPor(String arquivo, String janela) {
        Map<Long, BigDecimal> horas = new LinkedHashMap<>();
        for (Map<String, Object> linha : jdbc.queryForList(sql(arquivo), janela)) {
            horas.put((Long) linha.get("recurso_id"), (BigDecimal) linha.get("horas"));
        }
        return horas;
    }

    private long conflitosEvitados(String janela) {
        Long total = jdbc.queryForObject(sql(CONFLITOS_EVITADOS), Long.class, janela);
        return total;
    }

    private void tentativaRegistradaEm(long recursoId, long solicitanteId, String periodoPedido, String registradaEm) {
        jdbc.update(
                "insert into tentativa_recusada (recurso_id, solicitante_id, periodo_pedido, motivo, registrada_em) "
                        + "values (?, ?, ?::tstzrange, 'SOBREPOSICAO', ?)",
                recursoId, solicitanteId, periodoPedido, OffsetDateTime.parse(registradaEm));
    }
}
