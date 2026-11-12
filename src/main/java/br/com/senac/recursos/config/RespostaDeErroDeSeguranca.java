package br.com.senac.recursos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 401 (sem token, token expirado ou adulterado) e 403 (perfil sem permissão) no mesmo formato {@code ProblemDetail}
 * dos demais erros. A resposta nunca diz por que o token foi recusado, nem ecoa o token.
 */
@Component
public class RespostaDeErroDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper json;

    public RespostaDeErroDeSeguranca(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest requisicao, HttpServletResponse resposta, AuthenticationException erro)
            throws IOException {
        resposta.setHeader("WWW-Authenticate", "Bearer");
        escrever(requisicao, resposta, HttpStatus.UNAUTHORIZED, "nao-autenticado",
                "Faça login e envie o token no cabeçalho Authorization.");
    }

    @Override
    public void handle(HttpServletRequest requisicao, HttpServletResponse resposta, AccessDeniedException erro)
            throws IOException {
        escrever(requisicao, resposta, HttpStatus.FORBIDDEN, "acesso-negado",
                "O seu perfil não tem permissão para esta operação.");
    }

    private void escrever(HttpServletRequest requisicao, HttpServletResponse resposta, HttpStatus status, String tipo,
            String detalhe) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setType(URI.create("urn:recursos:problema:" + tipo));
        problema.setInstance(URI.create(requisicao.getRequestURI()));
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        json.writeValue(resposta.getOutputStream(), problema);
    }
}
