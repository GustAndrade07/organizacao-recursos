package br.com.senac.recursos.controller.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Erros que a API não consegue provocar de propósito (falha inesperada, integridade fora das regras): a resposta é
 * genérica e nunca carrega SQL, mensagem interna nem nome de classe (AGENTS.md, seção 5). Teste de unidade isolado.
 */
class TratadorDeErrosTest {

    private final TratadorDeErros tratador = new TratadorDeErros();
    private final ServletWebRequest requisicao = new ServletWebRequest(new MockHttpServletRequest("GET", "/reservas"));

    @Test
    void erroInesperadoVira500GenericoSemDetalheInterno() {
        ResponseEntity<ProblemDetail> resposta = tratador.inesperado(
                new IllegalStateException("select * from usuario where senha_hash = 'x'"), requisicao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().getDetail()).isEqualTo("Ocorreu um erro inesperado. Tente de novo mais tarde.");
        assertThat(resposta.getBody().toString()).doesNotContain("select").doesNotContain("IllegalStateException");
        assertThat(resposta.getBody().getInstance()).hasToString("/reservas");
    }

    @Test
    void integridadeForaDasRegrasVira409SemSql() {
        ResponseEntity<ProblemDetail> resposta = tratador.integridade(new DataIntegrityViolationException(
                "could not execute statement", new SQLException("violates foreign key \"fk_x\"", "23503")), requisicao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().toString()).doesNotContain("foreign key").doesNotContain("statement");
    }
}
