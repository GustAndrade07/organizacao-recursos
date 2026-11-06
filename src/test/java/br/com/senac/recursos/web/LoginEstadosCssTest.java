package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Estados novos do login (tela 1): botão em envio e contraste do botão desabilitado. */
class LoginEstadosCssTest {

    private static String ler(String caminho) throws IOException {
        return new String(new ClassPathResource(caminho).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Valor do token, seguindo var(--outro) até chegar ao hexadecimal. */
    private static String hexDoToken(String tokens, String nome) {
        Matcher m = Pattern.compile("(?m)^\\s*" + Pattern.quote(nome) + "\\s*:\\s*([^;]+);").matcher(tokens);
        assertThat(m.find()).as("token %s em tokens.css", nome).isTrue();
        String valor = m.group(1).trim();
        Matcher ref = Pattern.compile("var\\((--[\\w-]+)\\)").matcher(valor);
        return ref.matches() ? hexDoToken(tokens, ref.group(1)) : valor;
    }

    private static double luminancia(String hex) {
        double[] canais = new double[3];
        for (int i = 0; i < 3; i++) {
            double c = Integer.parseInt(hex.substring(1 + 2 * i, 3 + 2 * i), 16) / 255.0;
            canais[i] = c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * canais[0] + 0.7152 * canais[1] + 0.0722 * canais[2];
    }

    @Test
    void textoDoBotaoDesabilitadoTemContrasteDeAoMenos4e5() throws IOException {
        String css = ler("static/css/app.css");
        Matcher regra = Pattern.compile("\\.botao:disabled\\s*\\{([^}]*)\\}").matcher(css);
        assertThat(regra.find()).as("regra .botao:disabled").isTrue();
        String corpo = regra.group(1);
        Matcher texto = Pattern.compile("(?<![\\w-])color:\\s*var\\((--[\\w-]+)\\)").matcher(corpo);
        Matcher fundo = Pattern.compile("background:\\s*var\\((--[\\w-]+)\\)").matcher(corpo);
        assertThat(texto.find() && fundo.find()).isTrue();

        String tokens = ler("static/css/tokens.css");
        double a = luminancia(hexDoToken(tokens, texto.group(1)));
        double b = luminancia(hexDoToken(tokens, fundo.group(1)));
        double contraste = (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);

        assertThat(contraste).as("contraste do botão desabilitado").isGreaterThanOrEqualTo(4.5);
    }

    @Test
    void botaoEmEnvioEAnelDeCarregamentoTemRegraNoAppCss() throws IOException {
        String css = ler("static/css/app.css");

        assertThat(css).contains(".botao--carregando").contains(".carregando");
        // .botao:disabled já existe (tela 6): não pode ser repetido para esta tela
        assertThat(css.split("\\.botao:disabled\\s*\\{", -1)).hasSize(2);
    }
}
