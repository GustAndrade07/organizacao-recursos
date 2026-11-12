package br.com.senac.recursos.support;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.web.sessao.UsuarioDaSessao;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * Roda o teste com alguém logado pela tela (o mesmo {@link UsuarioDaSessao} que o {@code POST /login} põe na sessão),
 * para as rotas reais das telas passarem pela cadeia de segurança e o controller receber o usuário.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
@WithSecurityContext(factory = ComoUsuario.Fabrica.class)
public @interface ComoUsuario {

    PerfilUsuario value() default PerfilUsuario.SOLICITANTE;

    long id() default 1L;

    String nome() default "Laura Mendes";

    final class Fabrica implements WithSecurityContextFactory<ComoUsuario> {

        @Override
        public SecurityContext createSecurityContext(ComoUsuario anotacao) {
            UsuarioDaSessao usuario = new UsuarioDaSessao(anotacao.id(), anotacao.nome(),
                    anotacao.value().name().toLowerCase(), anotacao.value(), null);
            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(usuario, null,
                    usuario.getAuthorities()));
            return contexto;
        }
    }
}
