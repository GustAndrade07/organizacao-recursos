package br.com.senac.recursos.config;

import br.com.senac.recursos.converter.UsuarioAutenticadoConverter;
import br.com.senac.recursos.dto.UsuarioAutenticado;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permite que um controller receba {@link UsuarioAutenticado} como parâmetro, sem lidar com o token. Rotas que o
 * recebem são sempre autenticadas pela matriz; sem token, o Spring Security já respondeu 401 antes de chegar aqui.
 */
@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    private final UsuarioAutenticadoConverter conversor;

    public WebConfig(UsuarioAutenticadoConverter conversor) {
        this.conversor = conversor;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvedores) {
        resolvedores.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parametro) {
                return UsuarioAutenticado.class.equals(parametro.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parametro, ModelAndViewContainer modelo,
                    NativeWebRequest requisicao, WebDataBinderFactory fabrica) {
                Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
                if (autenticacao instanceof JwtAuthenticationToken token) {
                    return conversor.paraUsuario(token.getToken());
                }
                throw new IllegalStateException("Rota que exige usuário autenticado chamada sem token JWT.");
            }
        });
    }
}
