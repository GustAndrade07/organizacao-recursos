package br.com.senac.recursos.support;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Tira os controllers da API ({@code br.com.senac.recursos.controller}) dos recortes {@code @WebMvcTest} das telas.
 *
 * <p>Um {@code @WebMvcTest} sem lista de controllers carrega todos os controllers do projeto; os da API dependem dos
 * serviços e do banco, que o recorte não tem, e o contexto não subia ({@code PaginasDeErroTest}, depois do merge com a
 * {@code main} no PR #44). Os testes de tela não usam a API, então o recorte fica sem ela, e os testes do Leandro não
 * precisam saber que a API existe. Registrado em {@code src/test/resources/META-INF/spring.factories}; vale só para
 * classes anotadas com {@code @WebMvcTest}. Ver ADR-0008.
 */
public class RecorteDasTelasSemApi implements ContextCustomizerFactory {

    static final String PACOTE_DA_API = "br.com.senac.recursos.controller.";

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> classeDeTeste, List<ContextConfigurationAttributes> atributos) {
        boolean recorteWeb = MergedAnnotations
                .from(classeDeTeste, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
                .isPresent(WebMvcTest.class);
        return recorteWeb ? new SemApi() : null;
    }

    /** Registra o filtro antes da varredura de componentes; o {@code @SpringBootApplication} consulta filtros assim. */
    static final class SemApi implements ContextCustomizer {

        @Override
        public void customizeContext(ConfigurableApplicationContext contexto, MergedContextConfiguration config) {
            contexto.getBeanFactory().registerSingleton("recorteDasTelasSemApi", new FiltroDaApi());
        }

        @Override
        public boolean equals(Object outro) {
            return outro instanceof SemApi;
        }

        @Override
        public int hashCode() {
            return Objects.hash(SemApi.class);
        }
    }

    static final class FiltroDaApi extends TypeExcludeFilter {

        @Override
        public boolean match(MetadataReader leitor, MetadataReaderFactory fabrica) {
            return leitor.getClassMetadata().getClassName().startsWith(PACOTE_DA_API);
        }

        @Override
        public boolean equals(Object outro) {
            return outro instanceof FiltroDaApi;
        }

        @Override
        public int hashCode() {
            return Objects.hash(FiltroDaApi.class);
        }
    }
}
