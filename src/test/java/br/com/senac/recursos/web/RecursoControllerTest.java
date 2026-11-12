package br.com.senac.recursos.web;

import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.support.ComoUsuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import static org.mockito.Mockito.when;

import br.com.senac.recursos.service.RecursoService;
import br.com.senac.recursos.support.Respostas;
import org.junit.jupiter.api.BeforeEach;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rotas reais das telas 13, 14 e 15 ({@link RecursoController}): só recebem os parâmetros e devolvem a
 * tela. Quem lista, grava e consulta reservas afetadas é o back-end, que preenche o resto do modelo; por
 * isso estes testes olham o que o controller coloca no modelo, não o que a página mostra sem o back-end.
 */
@ComoUsuario(PerfilUsuario.ADMINISTRADOR)
@WebMvcTest(RecursoController.class)
class RecursoControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    RecursoService recursos;

    @BeforeEach
    void recursosGravados() {
        when(recursos.buscar(7L)).thenReturn(Respostas.sala(7, "Sala 203"));
        when(recursos.buscar(20L)).thenReturn(Respostas.recurso(20,
                br.com.senac.recursos.enums.TipoRecurso.PROFESSOR, "Prof. Ana", false));
        when(recursos.buscar(3L)).thenReturn(Respostas.recurso(3,
                br.com.senac.recursos.enums.TipoRecurso.MATERIAL, "Projetor P-07", false));
    }

    private Document pagina(String caminho) throws Exception {
        String html = mvc.perform(get(caminho))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Jsoup.parse(html);
    }

    @Test
    void listaSemParametrosUsaSalasESemNome() throws Exception {
        mvc.perform(get("/admin/recursos"))
                .andExpect(status().isOk())
                .andExpect(view().name("recursos"))
                .andExpect(model().attribute("tipo", TipoRecurso.SALAS))
                .andExpect(model().attribute("nome", (Object) null));
    }

    @Test
    void listaLeOTipoEONomeDaUrl() throws Exception {
        mvc.perform(get("/admin/recursos?tipo=materiais&nome=proje"))
                .andExpect(view().name("recursos"))
                .andExpect(model().attribute("tipo", TipoRecurso.MATERIAIS))
                .andExpect(model().attribute("nome", "proje"));
    }

    @Test
    void listaComTipoDesconhecidoCaiEmSalas() throws Exception {
        mvc.perform(get("/admin/recursos?tipo=carros"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tipo", TipoRecurso.SALAS));
    }

    @Test
    void listaComNomeEmBrancoNaoTemNome() throws Exception {
        mvc.perform(get("/admin/recursos?nome=  "))
                .andExpect(model().attribute("nome", (Object) null));
    }

    @Test
    void novoRecursoAbreEmSalasPorPadraoEnoTipoPedido() throws Exception {
        mvc.perform(get("/admin/recursos/novo"))
                .andExpect(view().name("recurso-form"))
                .andExpect(model().attribute("tipo", TipoRecurso.SALAS))
                .andExpect(model().attribute("recurso", new FormularioRecurso(null, null, null, null, null, null, false)));
        mvc.perform(get("/admin/recursos/novo?tipo=professores"))
                .andExpect(model().attribute("tipo", TipoRecurso.PROFESSORES));
    }

    @Test
    void trocarOTipoLevaAoNovoOQueJaFoiDigitado() throws Exception {
        mvc.perform(get("/admin/recursos/novo?tipo=materiais&nome=Projetor&capacidade=3&localizacao=Almoxarifado&patrimonio=PAT-1&competencias=x&restrito=on"))
                .andExpect(model().attribute("recurso",
                        new FormularioRecurso(null, "Projetor", "3", "Almoxarifado", "PAT-1", "x", true)));
    }

    @Test
    void novoERecursoComTipoDesconhecidoOuRestritoInvalidoResponde400() throws Exception {
        mvc.perform(get("/admin/recursos/novo?tipo=carros")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/recursos/novo?restrito=abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/recursos/7?tipo=carros")).andExpect(status().isBadRequest());
    }

    /** Na edição o formulário vem com o que está gravado, e o tipo é o do recurso, não o da URL. */
    @Test
    void editarRecursoVemComOQueEstaGravadoEOTipoDoRecurso() throws Exception {
        mvc.perform(get("/admin/recursos/7"))
                .andExpect(view().name("recurso-form"))
                .andExpect(model().attribute("tipo", TipoRecurso.SALAS))
                .andExpect(model().attribute("recurso", new FormularioRecurso("7", "Sala 203", "40", null, null, null, false)));
        mvc.perform(get("/admin/recursos/20?tipo=salas"))
                .andExpect(model().attribute("tipo", TipoRecurso.PROFESSORES))
                .andExpect(model().attribute("recurso", new FormularioRecurso("20", "Prof. Ana", null, null, null, null, false)));
    }

    @Test
    void bloqueioLevaOIdOsValoresDaUrlEOTipoParaVoltar() throws Exception {
        mvc.perform(get("/admin/recursos/3/manutencao"))
                .andExpect(view().name("manutencao"))
                .andExpect(model().attribute("tipo", TipoRecurso.SALAS))
                .andExpect(model().attribute("bloqueio", new FormularioBloqueio("3", null, null, null)));
        mvc.perform(get("/admin/recursos/3/manutencao?tipo=materiais&inicio=2026-11-10T08:00&fim=2026-11-20T18:00&motivo=Reforma"))
                .andExpect(model().attribute("tipo", TipoRecurso.MATERIAIS))
                .andExpect(model().attribute("bloqueio", new FormularioBloqueio("3",
                        LocalDateTime.of(2026, 11, 10, 8, 0), LocalDateTime.of(2026, 11, 20, 18, 0), "Reforma")));
    }

    @Test
    void bloqueioComDataInvalidaOuTipoDesconhecidoResponde400() throws Exception {
        mvc.perform(get("/admin/recursos/3/manutencao?inicio=ontem")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/recursos/3/manutencao?tipo=carros")).andExpect(status().isBadRequest());
    }

    @Test
    void asTresRotasRenderizamAPaginaComOLayoutBase() throws Exception {
        assertThat(pagina("/admin/recursos?tipo=materiais").title()).isEqualTo("Recursos · Organização de Recursos");
        assertThat(pagina("/admin/recursos/novo?tipo=professores").title()).isEqualTo("Cadastrar recurso · Organização de Recursos");
        assertThat(pagina("/admin/recursos/7").title()).isEqualTo("Editar recurso · Organização de Recursos");
        assertThat(pagina("/admin/recursos/3/manutencao?tipo=materiais").selectFirst("a[data-testid=manutencao-voltar]").attr("href"))
                .isEqualTo("/admin/recursos?tipo=materiais");
    }
}
