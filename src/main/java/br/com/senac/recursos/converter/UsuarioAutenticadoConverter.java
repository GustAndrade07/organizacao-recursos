package br.com.senac.recursos.converter;

import br.com.senac.recursos.dto.UsuarioAutenticado;
import br.com.senac.recursos.enums.PerfilUsuario;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Token JWT já validado para {@link UsuarioAutenticado}. As claims são as que {@code service.TokenService} escreve:
 * {@code sub} (login), {@code uid} (id do usuário) e {@code perfil}.
 */
@Component
public class UsuarioAutenticadoConverter {

    public static final String CLAIM_ID = "uid";
    public static final String CLAIM_PERFIL = "perfil";

    public UsuarioAutenticado paraUsuario(Jwt token) {
        Number id = token.getClaim(CLAIM_ID);
        String perfil = token.getClaimAsString(CLAIM_PERFIL);
        return new UsuarioAutenticado(id.longValue(), token.getSubject(), PerfilUsuario.valueOf(perfil));
    }
}
