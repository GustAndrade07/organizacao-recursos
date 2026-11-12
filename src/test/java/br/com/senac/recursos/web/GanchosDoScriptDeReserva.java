package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.springframework.core.io.ClassPathResource;

/**
 * Confere que todo seletor e id que {@code reserva-form.js} procura existe na página renderizada. O script é
 * usado pela nova reserva (tela 6, prefixo {@code reserva-form}) e pela alteração no detalhe (tela 8, prefixo
 * {@code reserva-alterar}): o formulário diz o prefixo em {@code data-reserva-form}, e aqui o prefixo da página
 * substitui a variável {@code prefixo} do script antes de procurar os seletores.
 */
final class GanchosDoScriptDeReserva {

    private GanchosDoScriptDeReserva() {
    }

    static void conferir(Document doc) throws IOException {
        String js = new String(new ClassPathResource("static/js/reserva-form.js").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        var form = doc.selectFirst("form[data-reserva-form]");
        assertThat(form).as("formulário marcado com data-reserva-form").isNotNull();
        String prefixo = form.attr("data-reserva-form");
        assertThat(prefixo).isNotBlank();
        String expandido = js.replace("\"' + prefixo + '", "\"" + prefixo)
                .replace("getElementById(prefixo + '", "getElementById('" + prefixo);

        Matcher testids = Pattern.compile("\\[data-testid=\"([^\"]+)\"\\]").matcher(expandido);
        int achados = 0;
        while (testids.find()) {
            achados++;
            assertThat(doc.select("[data-testid=" + testids.group(1) + "]")).as(testids.group(1)).isNotEmpty();
        }
        Matcher ids = Pattern.compile("getElementById\\('([^']+)'\\)").matcher(expandido);
        while (ids.find()) {
            achados++;
            assertThat(doc.getElementById(ids.group(1))).as(ids.group(1)).isNotNull();
        }
        Matcher dados = Pattern.compile("\\[(data-resumo-[a-z]+)\\]").matcher(expandido);
        while (dados.find()) {
            achados++;
            assertThat(doc.select("[" + dados.group(1) + "]")).as(dados.group(1)).isNotEmpty();
        }
        assertThat(achados).isGreaterThanOrEqualTo(6);
        assertThat(expandido).doesNotContain("prefixo + '-", "'-inicio", "\"-inicio");
        assertThat(js).contains("O término precisa ser depois do início");
    }
}
