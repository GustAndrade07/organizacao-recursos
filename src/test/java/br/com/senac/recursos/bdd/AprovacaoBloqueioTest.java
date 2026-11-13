package br.com.senac.recursos.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Roda os cenários de {@code src/test/resources/features/aprovacao-bloqueio.feature} no {@code verify}, com o nome
 * terminado em {@code Test} para o Surefire pegar. Um cenário vermelho quebra o build como qualquer outro teste.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "br.com.senac.recursos.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "summary")
class AprovacaoBloqueioTest {
}
