package br.com.senac.recursos.controller.handler;

import br.com.senac.recursos.exception.AcessoNegadoException;
import br.com.senac.recursos.exception.ConflitoDeHorarioException;
import br.com.senac.recursos.exception.CredenciaisInvalidasException;
import br.com.senac.recursos.exception.EntidadeNaoEncontradaException;
import br.com.senac.recursos.exception.LoginEmUsoException;
import br.com.senac.recursos.exception.NegocioException;
import br.com.senac.recursos.exception.RecursoEmManutencaoException;
import br.com.senac.recursos.exception.RequisicaoInvalidaException;
import br.com.senac.recursos.exception.ReservaJaIniciadaException;
import br.com.senac.recursos.exception.TransicaoInvalidaException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Todo erro da API sai como {@code ProblemDetail} (RFC 9457) com mensagem para o usuário. Regra (AGENTS.md, seção 5):
 * a resposta nunca traz stack trace, SQL, nome de classe interna nem o valor rejeitado (que poderia ser uma senha).
 * O detalhe técnico vai só para o log.
 */
@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TratadorDeErros.class);

    private static final String PREFIXO_TIPO = "urn:recursos:problema:";

    private static final Map<Class<? extends NegocioException>, HttpStatus> STATUS = Map.of(
            EntidadeNaoEncontradaException.class, HttpStatus.NOT_FOUND,
            RequisicaoInvalidaException.class, HttpStatus.BAD_REQUEST,
            ConflitoDeHorarioException.class, HttpStatus.CONFLICT,
            RecursoEmManutencaoException.class, HttpStatus.CONFLICT,
            TransicaoInvalidaException.class, HttpStatus.CONFLICT,
            ReservaJaIniciadaException.class, HttpStatus.CONFLICT,
            LoginEmUsoException.class, HttpStatus.CONFLICT,
            AcessoNegadoException.class, HttpStatus.FORBIDDEN,
            CredenciaisInvalidasException.class, HttpStatus.UNAUTHORIZED);

    @ExceptionHandler(NegocioException.class)
    ResponseEntity<ProblemDetail> negocio(NegocioException erro, WebRequest requisicao) {
        HttpStatus status = STATUS.getOrDefault(erro.getClass(), HttpStatus.BAD_REQUEST);
        ProblemDetail problema = problema(status, erro.tipo(), erro.getMessage(), requisicao);
        if (erro instanceof ConflitoDeHorarioException conflito) {
            problema.setProperty("recursos", conflito.recursoIds());
        } else if (erro instanceof RecursoEmManutencaoException manutencao) {
            problema.setProperty("recursos", manutencao.recursoIds());
        }
        ResponseEntity.BodyBuilder resposta = ResponseEntity.status(status);
        if (status == HttpStatus.UNAUTHORIZED) {
            resposta.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        return resposta.body(problema);
    }

    /** Violação de integridade que não virou regra de negócio (ex.: chave estrangeira): 409 sem citar o SQL. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integridade(DataIntegrityViolationException erro, WebRequest requisicao) {
        LOG.warn("Violação de integridade não tratada como regra de negócio", erro);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problema(HttpStatus.CONFLICT, "conflito-de-dados",
                "A operação conflita com dados já gravados.", requisicao));
    }

    /** Qualquer outro erro: 500 genérico. O detalhe fica só no log. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> inesperado(Exception erro, WebRequest requisicao) {
        LOG.error("Erro inesperado na API", erro);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problema(HttpStatus.INTERNAL_SERVER_ERROR,
                "erro-interno", "Ocorreu um erro inesperado. Tente de novo mais tarde.", requisicao));
    }

    /** Campo do corpo inválido: lista campo e mensagem, nunca o valor enviado. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException erro,
            @NonNull HttpHeaders cabecalhos, @NonNull HttpStatusCode status, @NonNull WebRequest requisicao) {
        List<Map<String, String>> campos = erro.getBindingResult().getFieldErrors().stream()
                .map(campo -> Map.of("campo", campo.getField(), "mensagem", mensagem(campo.getDefaultMessage())))
                .toList();
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "dados-invalidos",
                "Há campos inválidos na requisição.", requisicao);
        problema.setProperty("erros", campos);
        return ResponseEntity.badRequest().body(problema);
    }

    /** Parâmetro de URL ou de consulta inválido (ex.: capacidade mínima negativa). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @NonNull HandlerMethodValidationException erro, @NonNull HttpHeaders cabecalhos,
            @NonNull HttpStatusCode status, @NonNull WebRequest requisicao) {
        List<Map<String, String>> campos = erro.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(falha -> Map.of(
                                "campo", String.valueOf(resultado.getMethodParameter().getParameterName()),
                                "mensagem", mensagem(falha.getDefaultMessage()))))
                .toList();
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "dados-invalidos",
                "Há parâmetros inválidos na requisição.", requisicao);
        problema.setProperty("erros", campos);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(@NonNull HttpMessageNotReadableException erro,
            @NonNull HttpHeaders cabecalhos, @NonNull HttpStatusCode status, @NonNull WebRequest requisicao) {
        return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "corpo-ilegivel",
                "O corpo da requisição não é um JSON válido ou tem um valor em formato errado "
                        + "(datas em ISO-8601 com deslocamento, ex.: 2026-11-10T10:00:00-03:00).", requisicao));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(@NonNull TypeMismatchException erro,
            @NonNull HttpHeaders cabecalhos, @NonNull HttpStatusCode status, @NonNull WebRequest requisicao) {
        String parametro = erro.getPropertyName() == null ? "parâmetro" : erro.getPropertyName();
        return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "parametro-invalido",
                "O valor de '" + parametro + "' está em formato inválido.", requisicao));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            @NonNull MissingServletRequestParameterException erro, @NonNull HttpHeaders cabecalhos,
            @NonNull HttpStatusCode status, @NonNull WebRequest requisicao) {
        return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "parametro-ausente",
                "Informe o parâmetro '" + erro.getParameterName() + "'.", requisicao));
    }

    private static ProblemDetail problema(HttpStatus status, String tipo, String detalhe, WebRequest requisicao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setType(URI.create(PREFIXO_TIPO + tipo));
        if (requisicao instanceof ServletWebRequest servlet) {
            problema.setInstance(URI.create(servlet.getRequest().getRequestURI()));
        }
        return problema;
    }

    private static String mensagem(String texto) {
        return texto == null ? "valor inválido" : texto;
    }
}
