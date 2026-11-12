package br.com.senac.recursos.converter;

import br.com.senac.recursos.dto.response.UsuarioResponse;
import br.com.senac.recursos.model.Usuario;
import org.springframework.stereotype.Component;

/** {@link Usuario} para a resposta pública: o hash da senha nunca sai daqui. */
@Component
public class UsuarioConverter {

    public UsuarioResponse paraResposta(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getLogin(), usuario.getPerfil());
    }
}
