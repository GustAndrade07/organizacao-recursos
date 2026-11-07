package br.com.senac.recursos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Teste que falha de propósito (Etapa 4 do plano): serve para o run do GitHub Actions ficar vermelho e provar que
 * o pipeline barra código quebrado. Sai do repositório no commit seguinte, que traz o run verde.
 */
class CiVermelhoPropositalTest {

    @Test
    void falhaDePropositoParaVerOCiVermelho() {
        assertThat(1 + 1).as("falha de proposito: 1 + 1 nao e 3").isEqualTo(3);
    }
}
