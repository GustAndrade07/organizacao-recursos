package br.com.senac.recursos.service;

import br.com.senac.recursos.config.JwtPropriedades;
import br.com.senac.recursos.converter.UsuarioAutenticadoConverter;
import br.com.senac.recursos.dto.response.TokenResponse;
import br.com.senac.recursos.model.Usuario;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite o JWT do login (ADR-0001): HS256, com {@code sub} = login, {@code uid} = id do usuário e {@code perfil}. O
 * instante de emissão e a expiração vêm do {@link Clock} injetado.
 */
@Service
public class TokenService {

    private final JwtEncoder codificador;
    private final JwtPropriedades propriedades;
    private final Clock relogio;

    public TokenService(JwtEncoder codificador, JwtPropriedades propriedades, Clock relogio) {
        this.codificador = codificador;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    public TokenResponse emitir(Usuario usuario) {
        Instant agora = relogio.instant();
        Instant expiraEm = agora.plus(propriedades.validade());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propriedades.emissor())
                .subject(usuario.getLogin())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim(UsuarioAutenticadoConverter.CLAIM_ID, usuario.getId())
                .claim(UsuarioAutenticadoConverter.CLAIM_PERFIL, usuario.getPerfil().name())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = codificador.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", expiraEm);
    }
}
