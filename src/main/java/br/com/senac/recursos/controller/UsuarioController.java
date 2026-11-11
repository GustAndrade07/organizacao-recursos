package br.com.senac.recursos.controller;

import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.dto.request.UsuarioRequest;
import br.com.senac.recursos.dto.response.UsuarioResponse;
import br.com.senac.recursos.service.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Usuários. Cadastro e listagem só para Administrador (matriz); {@code /usuarios/me} para qualquer autenticado. */
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest pedido) {
        UsuarioResponse criado = usuarios.criar(pedido);
        return ResponseEntity.created(URI.create("/usuarios/" + criado.id())).body(criado);
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarios.listar();
    }

    @GetMapping("/me")
    public UsuarioResponse eu(UsuarioAutenticado quem) {
        return usuarios.buscar(quem.id());
    }
}
