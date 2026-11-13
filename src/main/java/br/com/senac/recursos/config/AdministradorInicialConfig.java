package br.com.senac.recursos.config;

import br.com.senac.recursos.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * No start, garante o primeiro Administrador a partir de {@code ADMIN_LOGIN} (um e-mail, DEF-010) e
 * {@code ADMIN_SENHA}. Sem as duas variáveis não faz nada; com um {@code ADMIN_LOGIN} que não é e-mail a subida
 * falha com mensagem clara. Nunca grava nem imprime a senha, nem o e-mail no log.
 */
@Configuration(proxyBeanMethods = false)
public class AdministradorInicialConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AdministradorInicialConfig.class);

    @Bean
    ApplicationRunner administradorInicial(AdministradorInicialPropriedades propriedades, UsuarioService usuarios) {
        return argumentos -> {
            if (!propriedades.configurado()) {
                return;
            }
            String nome = propriedades.nome() == null || propriedades.nome().isBlank()
                    ? "Administrador"
                    : propriedades.nome();
            if (usuarios.garantirAdministrador(nome, propriedades.login(), propriedades.senha())) {
                LOG.info("Administrador inicial criado.");
            }
        };
    }
}
