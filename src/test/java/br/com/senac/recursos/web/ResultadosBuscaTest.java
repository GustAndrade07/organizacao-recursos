package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tela 5 do inventário (docs/frontend/telas.md): resultados da busca, prefixo {@code resultado-}, RF-03.
 * Mesma rota e mesmo template da tela 4. O back-end preenche {@code resultados} (lista de objetos com
 * {@code id}, {@code nome}, {@code tipo}, {@code local}, {@code capacidade} já formatada e {@code restrito});
 * enquanto o atributo não existir, a página mostra só o formulário.
 */
@WebMvcTest(ResultadosBuscaTest.PaginasDeTeste.class)
@Import(ResultadosBuscaTest.PaginasDeTeste.class)
class ResultadosBuscaTest {

    @Controller
    static class PaginasDeTeste {

        private static Map<String, Object> recurso(String id, String nome, String tipo, String local, String capacidade, boolean restrito) {
            return Map.of("id", id, "nome", nome, "tipo", tipo, "local", local, "capacidade", capacidade, "restrito", restrito);
        }

        private static void filtros(Model model) {
            model.addAttribute("filtros", new FiltrosBusca("Sala", 30, "Bloco B",
                    LocalDate.of(2026, 11, 12), LocalTime.of(14, 0), LocalTime.of(15, 30)));
        }

        @GetMapping("/teste/resultados")
        String comResultados(Model model) {
            filtros(model);
            model.addAttribute("resultados", List.of(
                    recurso("7", "Sala 203", "Sala", "Bloco B, 2º andar", "40 lugares", false),
                    recurso("9", "Laboratório de Química", "Sala", "Bloco C, térreo", "24 lugares", true),
                    recurso("12", "Projetor portátil P-07", "Material", "Almoxarifado", "1 unidade", false)));
            return "busca";
        }

        @GetMapping("/teste/resultados-um")
        String umResultado(Model model) {
            filtros(model);
            model.addAttribute("resultados", List.of(recurso("7", "Sala 203", "Sala", "Bloco B", "40 lugares", false)));
            return "busca";
        }

        @GetMapping("/teste/resultados-vazio")
        String vazio(Model model) {
            filtros(model);
            model.addAttribute("resultados", List.of());
            return "busca";
        }

        @GetMapping("/teste/resultados-perigoso")
        String perigoso(Model model) {
            filtros(model);
            model.addAttribute("resultados", List.of(recurso("1", "<script>alert(1)</script>", "Sala", "Bloco A", "10 lugares", false)));
            return "busca";
        }
    }

    @Autowired
    MockMvc mvc;

    private Document pagina(String caminho) throws Exception {
        String html = mvc.perform(get(caminho))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Jsoup.parse(html);
    }

    @Test
    void comResultadosMostraTituloContagemEResumoSemOFormulario() throws Exception {
        Document doc = pagina("/teste/resultados");

        assertThat(doc.title()).isEqualTo("Recursos livres · Organização de Recursos");
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.selectFirst("h1[data-testid=resultado-titulo]").text()).isEqualTo("Recursos livres");
        assertThat(doc.selectFirst("[data-testid=resultado-contagem]").text()).isEqualTo("3 recursos livres no horário escolhido.");
        assertThat(doc.selectFirst("[data-testid=resultado-resumo]").text())
                .contains("12/11/2026").contains("14:00 às 15:30").contains("Sala").contains("Bloco B");
        assertThat(doc.select("[data-testid=busca-formulario]")).isEmpty();
    }

    @Test
    void contagemNoSingularQuandoHaUmSoRecurso() throws Exception {
        Document doc = pagina("/teste/resultados-um");

        assertThat(doc.selectFirst("[data-testid=resultado-contagem]").text()).isEqualTo("1 recurso livre no horário escolhido.");
    }

    @Test
    void tabelaTemLegendaCabecalhosECelulasRotuladasParaOsCardsDoMobile() throws Exception {
        Document doc = pagina("/teste/resultados");

        var tabela = doc.selectFirst("table[data-testid=resultado-tabela]");
        assertThat(tabela.selectFirst("caption").text()).isEqualTo("Recursos livres no horário escolhido");
        assertThat(tabela.select("thead th[scope=col]").eachText()).containsExactly("Nome", "Tipo", "Local", "Capacidade", "Ação");
        var linha = tabela.selectFirst("tr[data-testid=resultado-linha-7]");
        assertThat(linha.selectFirst("th[scope=row]").text()).isEqualTo("Sala 203");
        assertThat(linha.select("td[data-rotulo]").eachAttr("data-rotulo")).containsExactly("Tipo", "Local", "Capacidade");
        assertThat(linha.select("td[data-rotulo]").eachText()).containsExactly("Sala", "Bloco B, 2º andar", "40 lugares");
        assertThat(tabela.select("tbody tr")).hasSize(3);
    }

    @Test
    void botaoReservarLevaOHorarioEscolhidoParaANovaReserva() throws Exception {
        Document doc = pagina("/teste/resultados");

        var reservar = doc.selectFirst("a[data-testid=resultado-reservar-7]");
        assertThat(reservar.text()).isEqualTo("Reservar");
        assertThat(reservar.attr("aria-label")).isEqualTo("Reservar Sala 203");
        assertThat(reservar.attr("href")).startsWith("/reservas/nova?")
                .contains("recursoId=7").contains("data=2026-11-12").contains("inicio=14:00").contains("termino=15:30");
    }

    @Test
    void recursoRestritoTemEtiquetaComTextoENaoSoCor() throws Exception {
        Document doc = pagina("/teste/resultados");

        assertThat(doc.selectFirst("[data-testid=resultado-nome-9] .etiqueta").text()).isEqualTo("Restrito");
        assertThat(doc.select("[data-testid=resultado-nome-7] .etiqueta")).isEmpty();
    }

    @Test
    void alterarBuscaVoltaAoFormularioComOsFiltrosPreenchidos() throws Exception {
        Document doc = pagina("/teste/resultados");

        var alterar = doc.selectFirst("a[data-testid=resultado-alterar-busca]");
        assertThat(alterar.text()).isEqualTo("Alterar busca");
        assertThat(alterar.attr("href")).startsWith("/recursos/disponiveis?")
                .contains("tipo=Sala").contains("capacidade=30").contains("local=Bloco%20B")
                .contains("data=2026-11-12").contains("inicio=14:00").contains("termino=15:30").contains("editar");
    }

    @Test
    void semResultadosMostraMensagemComSugestoesEOCaminhoParaAlterarOsFiltros() throws Exception {
        Document doc = pagina("/teste/resultados-vazio");

        var vazio = doc.selectFirst("section[data-testid=resultado-vazio]");
        assertThat(vazio.selectFirst("h2").text()).isEqualTo("Nenhum recurso livre nesse horário");
        assertThat(vazio.select("li")).hasSize(3);
        assertThat(vazio.selectFirst("a[data-testid=resultado-vazio-alterar]").text()).isEqualTo("Alterar filtros");
        assertThat(doc.selectFirst("[data-testid=resultado-contagem]").text()).isEqualTo("0 recursos livres no horário escolhido.");
        assertThat(doc.select("[data-testid=resultado-tabela]")).isEmpty();
    }

    @Test
    void nomeDeRecursoComHtmlEhEscapado() throws Exception {
        Document doc = pagina("/teste/resultados-perigoso");

        assertThat(doc.select("script:containsData(alert)")).isEmpty();
        assertThat(doc.selectFirst("[data-testid=resultado-nome-1]").text()).isEqualTo("<script>alert(1)</script>");
    }
}
