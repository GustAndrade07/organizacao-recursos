package br.com.senac.recursos.web;

import br.com.senac.recursos.exception.AcessoNegadoException;
import br.com.senac.recursos.exception.EntidadeNaoEncontradaException;
import br.com.senac.recursos.exception.NegocioException;
import br.com.senac.recursos.exception.RequisicaoInvalidaException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Erros de regra que chegam às telas viram as páginas de erro que já existem ({@code templates/error}), com o menu de
 * quem está logado e sem stack trace nem mensagem interna: reserva de outra pessoa (403), reserva ou recurso que não
 * existe (404), pedido inválido (400) e transição que o estado não permite (409, página genérica 4xx). Os erros que a
 * tela sabe mostrar no próprio formulário (conflito de horário, manutenção, campo inválido) são tratados antes, no
 * controller. A API tem o seu tratador ({@code controller.handler.TratadorDeErros}), que responde {@code ProblemDetail}.
 */
@ControllerAdvice(basePackages = "br.com.senac.recursos.web")
class TratadorDeErrosDasTelas {

    @ExceptionHandler(NegocioException.class)
    void negocio(NegocioException erro, HttpServletResponse resposta) throws IOException {
        resposta.sendError(status(erro).value());
    }

    static HttpStatus status(NegocioException erro) {
        if (erro instanceof AcessoNegadoException) {
            return HttpStatus.FORBIDDEN;
        }
        if (erro instanceof EntidadeNaoEncontradaException) {
            return HttpStatus.NOT_FOUND;
        }
        if (erro instanceof RequisicaoInvalidaException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.CONFLICT;
    }
}
