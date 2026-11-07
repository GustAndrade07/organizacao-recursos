package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Regra única de "destino só pode ser caminho deste site" (login e páginas de erro). */
class CaminhoDoSiteTest {

    @ParameterizedTest(name = "[{index}] válido: {0}")
    @ValueSource(strings = {"/", "/reservas", "/reservas/minhas?a=1&b=2", "/recursos/disponiveis?tipo=SALA#topo",
            "/reservas/99", "/a/b/c.html", "/busca?q=sala%20203"})
    void aceitaCaminhoDoSite(String caminho) {
        assertThat(CaminhoDoSite.valido(caminho)).isTrue();
        assertThat(CaminhoDoSite.ouNulo(caminho)).isEqualTo(caminho);
    }

    @ParameterizedTest(name = "[{index}] inválido")
    @MethodSource("invalidos")
    void rejeitaTudoQueOutroSitePossaInterpretar(String caminho) {
        assertThat(CaminhoDoSite.valido(caminho)).isFalse();
        assertThat(CaminhoDoSite.ouNulo(caminho)).isNull();
    }

    static Stream<String> invalidos() {
        return Stream.of(
                null, "", " ", "reservas", "https://evil.com", "http://evil.com/x", "javascript:alert(1)", "data:text/html,x",
                "mailto:a@b.co", "evil.com/x",
                // começo: "//" e "/\" viram protocolo relativo
                "//evil.com", "///evil.com", "/\\evil.com", "/\\\\evil.com",
                // o navegador remove TAB, LF e CR dentro da URL: "/<TAB>/evil.com" vira "//evil.com"
                "/\t/evil.com", "/\n/evil.com", "/\r/evil.com", "/\t\\evil.com", "/\r\n/evil.com", "\t//evil.com",
                // caracteres de controle e espaços
                "/\0/evil.com", "/\u0001x", "/\u001fx", "/\u007fx", "/\u0085x", "/ x", " /x", "/x ", "/ /evil.com",
                "/ x", "/　x", "/​/evil.com",
                // barra invertida em qualquer posição
                "/a\\b", "/reservas\\..\\x",
                // barra e barra invertida codificadas, e dupla codificação
                "/%2f/evil.com", "/%2F/evil.com", "/%5c/evil.com", "/%5C/evil.com", "/%252f/evil.com", "/a%25b",
                // tamanho
                "/" + "a".repeat(2000),
                // não é URI válida
                "/x\"><script>", "/a b", "/<x>", "/{x}");
    }

    @Test
    void limiteDeTamanhoEDe2000Caracteres() {
        assertThat(CaminhoDoSite.valido("/" + "a".repeat(1999))).isTrue();
        assertThat(CaminhoDoSite.valido("/" + "a".repeat(2000))).isFalse();
    }
}
