package br.com.senac.recursos.repository;

import br.com.senac.recursos.dto.response.CargaPorProfessorResponse;
import br.com.senac.recursos.dto.response.HorasPorRecursoResponse;
import br.com.senac.recursos.enums.TipoRecurso;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Consultas do relatório de utilização (RF-12). O SQL não fica aqui: é o de {@code db/consultas/}, escrito e testado
 * no nível do banco ({@code RelatorioUtilizacaoBancoTest}). Este repositório só carrega os arquivos e passa a janela
 * {@code [início, fim)} como único parâmetro.
 */
@Repository
public class RelatorioRepository {

    private final JdbcTemplate jdbc;
    private final String sqlHorasPorRecurso;
    private final String sqlCargaPorProfessor;
    private final String sqlConflitosEvitados;

    public RelatorioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.sqlHorasPorRecurso = carregar("db/consultas/relatorio-horas-por-recurso.sql");
        this.sqlCargaPorProfessor = carregar("db/consultas/relatorio-carga-por-professor.sql");
        this.sqlConflitosEvitados = carregar("db/consultas/relatorio-conflitos-evitados.sql");
    }

    /** @param janela literal {@code tstzrange} de {@code model.Periodo#comoTstzrange()} */
    public List<HorasPorRecursoResponse> horasPorRecurso(String janela) {
        return jdbc.query(sqlHorasPorRecurso, (rs, linha) -> new HorasPorRecursoResponse(
                rs.getLong("recurso_id"), rs.getString("nome"), TipoRecurso.valueOf(rs.getString("tipo")),
                rs.getBigDecimal("horas")), janela);
    }

    public List<CargaPorProfessorResponse> cargaPorProfessor(String janela) {
        return jdbc.query(sqlCargaPorProfessor, (rs, linha) -> new CargaPorProfessorResponse(
                rs.getLong("recurso_id"), rs.getString("nome"), rs.getBigDecimal("horas")), janela);
    }

    public long conflitosEvitados(String janela) {
        Long total = jdbc.queryForObject(sqlConflitosEvitados, Long.class, janela);
        return total == null ? 0 : total;
    }

    private static String carregar(String caminho) {
        try {
            return new ClassPathResource(caminho).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Consulta do relatório não encontrada: " + caminho, e);
        }
    }
}
