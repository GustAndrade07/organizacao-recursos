package br.com.senac.recursos.controller;

import br.com.senac.recursos.dto.request.LoginRequest;
import br.com.senac.recursos.dto.response.TokenResponse;
import br.com.senac.recursos.service.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login (RF-01, ADR-0001). Única rota de negócio aberta sem token. */
@RestController
@RequestMapping("/auth")
public class AutenticacaoController {

    private final AutenticacaoService autenticacao;

    public AutenticacaoController(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
    }

    @PostMapping("/login")
    public TokenResponse entrar(@Valid @RequestBody LoginRequest pedido) {
        return autenticacao.entrar(pedido);
    }
}
