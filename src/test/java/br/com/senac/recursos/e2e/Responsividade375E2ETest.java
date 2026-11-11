package br.com.senac.recursos.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * RF-13 em 375 px, tela por tela (critérios gerais das Specs do Front-end no Notion): sem rolagem horizontal,
 * um único {@code h1}, tabelas viram cartões (cabeçalho só para leitor de tela, linhas que deixam de ser linhas de tabela) e todo controle tocável tem pelo menos 44 px
 * de altura (fora o que é só para leitor de tela). Cada tela deixa um print em {@code target/e2e/375}. As rotas reais mostram o estado vazio; as
 * {@code /e2e/...} mostram a mesma tela com dados ({@link TelasComDados}).
 */
class Responsividade375E2ETest extends E2ETeste {

    private static final int ALVO_DE_TOQUE = 44;

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "01-login,                      /login",
            "01-login-erro,                 /login?erro",
            "01-erro-401,                   /e2e/erro/401",
            "01-erro-403,                   /e2e/erro/403",
            "01-erro-404,                   /nao-existe",
            "01-erro-500,                   /e2e/erro/500",
            "02-busca-vazia,                /recursos/disponiveis",
            "02-busca-resultados,           /e2e/busca",
            "02-nova-reserva,               /reservas/nova?recursoId=7&data=2026-11-12&inicio=14:00&termino=15:30",
            "02-nova-reserva-restrito,      /e2e/reserva-nova/restrito",
            "02-nova-reserva-conflito,      /e2e/reserva-nova/conflito",
            "02-minhas-vazia,               /reservas/minhas",
            "02-minhas,                     /e2e/minhas",
            "02-detalhe,                    /e2e/reserva",
            "02-detalhe-alterar,            /e2e/reserva?acao=alterar",
            "02-detalhe-cancelar,           /e2e/reserva?acao=cancelar",
            "03-aprovacoes-vazia,           /aprovacoes",
            "03-aprovacoes,                 /e2e/aprovacoes",
            "03-aprovacoes-rejeitar,        /e2e/aprovacoes?rejeitar=1",
            "03-retirada-devolucao,         /e2e/retirada-devolucao",
            "03-retirada-nao-compareceu,    /e2e/retirada-devolucao?naoCompareceu=4",
            "04-historico,                  /e2e/historico",
            "04-relatorio-vazio,            /relatorios/utilizacao",
            "04-relatorio,                  /e2e/relatorio",
            "05-recursos-vazia,             /admin/recursos",
            "05-recursos,                   /e2e/recursos",
            "05-recurso-novo,               /admin/recursos/novo",
            "05-manutencao,                 /admin/recursos/9/manutencao"
    })
    void telaCabeEm375PxSemRolagemHorizontalComUmTituloEAlvosDeToque(String nome, String caminho) {
        abrir(caminho);

        semRolagemHorizontal();
        assertThat(pagina.locator("h1").count()).as("h1 em %s", caminho).isEqualTo(1);
        for (var tabela : pagina.locator("table.tabela").all()) {
            // Cartão: o cabeçalho continua no HTML para o leitor de tela, mas não ocupa espaço na tela
            assertThat(tabela.locator("thead").boundingBox().height).as("altura do cabeçalho da tabela em %s", caminho)
                    .isLessThanOrEqualTo(1.0);
            assertThat(tabela.locator("tbody tr").first().evaluate("e => getComputedStyle(e).display"))
                    .as("linha da tabela em %s (cartão, não linha)", caminho).isNotEqualTo("table-row");
        }
        assertThat(controlesMenoresQueOAlvoDeToque()).as("controles abaixo de %d px em %s", ALVO_DE_TOQUE, caminho).isEmpty();
        print(nome);
    }

    @SuppressWarnings("unchecked")
    private List<String> controlesMenoresQueOAlvoDeToque() {
        return (List<String>) pagina.evaluate("""
                (minimo) => Array.from(document.querySelectorAll(
                    'main button, main a.botao, main input:not([type=hidden]):not([type=checkbox]):not([type=radio]), '
                    + 'main select, main textarea, header button, header a.botao, dialog button, dialog a.botao'))
                  .filter(e => e.getClientRects().length > 0 && getComputedStyle(e).visibility !== 'hidden')
                  .filter(e => !e.closest('.so-leitor, [aria-hidden=true]'))
                  .filter(e => e.getBoundingClientRect().height < minimo)
                  .map(e => (e.getAttribute('data-testid') || e.tagName) + ' ' + Math.round(e.getBoundingClientRect().height) + 'px')
                """, ALVO_DE_TOQUE);
    }
}
