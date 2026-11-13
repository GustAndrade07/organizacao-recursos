package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.senac.recursos.dto.response.UsuarioResponse;
import br.com.senac.recursos.enums.PerfilUsuario;
import br.com.senac.recursos.exception.LoginEmUsoException;
import br.com.senac.recursos.service.UsuarioService;
import br.com.senac.recursos.support.ComoUsuario;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tela 16 do inventário (docs/frontend/telas.md): usuários, prefixos {@code usuarios-} e {@code usuario-form-}, só do
 * Administrador. Contrato do template e o que o controller faz com a resposta do {@link UsuarioService}; o cadastro
 * contra o banco está em {@code TelasComBancoTest}.
 */
@ComoUsuario(PerfilUsuario.ADMINISTRADOR)
@WebMvcTest(UsuarioController.class)
class UsuariosPaginaTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioService usuarios;

    private Document pagina(String caminho) throws Exception {
        return Jsoup.parse(mvc.perform(get(caminho)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private Document enviar(int status, String... campos) throws Exception {
        var pedido = post("/admin/usuarios").with(csrf());
        for (int i = 0; i < campos.length; i += 2) {
            pedido.param(campos[i], campos[i + 1]);
        }
        return Jsoup.parse(mvc.perform(pedido).andExpect(status().is(status))
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    void listaMostraNomeLoginEPerfilPorExtensoEmOrdemDeNomeENuncaASenha() throws Exception {
        when(usuarios.listar()).thenReturn(List.of(
                new UsuarioResponse(2L, "Bruno Alves", "bruno@exemplo.com", PerfilUsuario.RESPONSAVEL),
                new UsuarioResponse(1L, "ana Lima", "ana.lima@exemplo.com", PerfilUsuario.SOLICITANTE)));

        Document doc = pagina("/admin/usuarios");

        assertThat(doc.title()).isEqualTo("Usuários · Organização de Recursos");
        assertThat(doc.select("h1")).hasSize(1);
        assertThat(doc.select("[data-testid^=usuarios-linha-]").eachAttr("data-testid"))
                .containsExactly("usuarios-linha-1", "usuarios-linha-2");
        assertThat(doc.selectFirst("[data-testid=usuarios-perfil-2]").text()).isEqualTo("Responsável");
        assertThat(doc.selectFirst("[data-testid=usuarios-tabela] caption").text()).isEqualTo("Usuários cadastrados");
        assertThat(doc.select("[data-testid=usuarios-tabela] td[data-rotulo]")).hasSize(4);
        assertThat(doc.html()).doesNotContainIgnoringCase("senha");
    }

    @Test
    void semUsuariosMostraOEstadoVazio() throws Exception {
        when(usuarios.listar()).thenReturn(List.of());

        Document doc = pagina("/admin/usuarios");

        assertThat(doc.selectFirst("[data-testid=usuarios-vazio] h2").text()).isEqualTo("Nenhum usuário cadastrado");
        assertThat(doc.select("[data-testid=usuarios-tabela]")).isEmpty();
    }

    @Test
    void formularioTemNomeCompletoEmailSenhaEPerfilSemRepetirSenha() throws Exception {
        Document doc = pagina("/admin/usuarios/novo");

        var form = doc.selectFirst("form[data-testid=usuario-form]");
        assertThat(form.attr("method")).isEqualToIgnoringCase("post");
        assertThat(form.attr("action")).isEqualTo("/admin/usuarios");
        assertThat(form.hasAttr("data-envio-unico")).isTrue();
        assertThat(form.select("input[name=_csrf]")).hasSize(1);
        for (String campo : List.of("nome", "login", "senha", "perfil")) {
            var entrada = form.selectFirst("[name=" + campo + "]");
            assertThat(entrada.hasAttr("required")).as(campo).isTrue();
            assertThat(doc.selectFirst("label[for=" + entrada.id() + "]")).as(campo).isNotNull();
        }
        var senha = form.selectFirst("input[name=senha]");
        assertThat(senha.attr("type")).isEqualTo("password");
        assertThat(senha.attr("autocomplete")).isEqualTo("new-password");
        // DEF-010: nome completo, e-mail e senha (mais o perfil, do Administrador); sem campo de repetir a senha
        assertThat(doc.selectFirst("label[for=usuario-form-nome]").text()).startsWith("Nome completo");
        var email = form.selectFirst("input[name=login]");
        assertThat(doc.selectFirst("label[for=" + email.id() + "]").text()).startsWith("E-mail");
        assertThat(email.attr("type")).isEqualTo("email");
        assertThat(email.attr("maxlength")).isEqualTo("254");
        assertThat(form.select("input[type=password]")).hasSize(1);
        assertThat(form.select("label").eachText()).noneMatch(texto -> texto.toLowerCase().contains("repit")
                || texto.toLowerCase().contains("confirm"));
        assertThat(doc.html()).doesNotContain("De 3 a 60 caracteres");
        assertThat(form.select("select[name=perfil] option").eachText())
                .containsExactly("Solicitante", "Responsável", "Administrador");
        assertThat(form.selectFirst("select[name=perfil] option[selected]").val()).isEqualTo("SOLICITANTE");
        assertThat(doc.select("script[src]").eachAttr("src")).contains("/js/usuario-form.js");
    }

    @Test
    void camposInvalidosVoltamComResumoMensagemJuntoAoCampoESemASenha() throws Exception {
        Document doc = enviar(400, "nome", " ", "login", "ana.lima", "senha", "curta", "perfil", "SOLICITANTE");

        var resumo = doc.selectFirst("[data-testid=usuario-form-erros]");
        assertThat(resumo.attr("role")).isEqualTo("alert");
        assertThat(resumo.select("li a").eachAttr("href"))
                .containsExactly("#usuario-form-nome", "#usuario-form-login", "#usuario-form-senha");
        assertThat(doc.selectFirst("[data-testid=usuario-form-nome-erro]").text()).isEqualTo("Informe o nome completo.");
        assertThat(doc.selectFirst("[data-testid=usuario-form-login-erro]").text())
                .isEqualTo("Informe um e-mail válido, como nome@dominio.com.");
        assertThat(doc.selectFirst("[data-testid=usuario-form-senha-erro]").text())
                .isEqualTo("A senha deve ter de 8 a 72 caracteres.");
        assertThat(doc.selectFirst("#usuario-form-senha").attr("aria-invalid")).isEqualTo("true");
        assertThat(doc.selectFirst("#usuario-form-login").val()).isEqualTo("ana.lima");
        assertThat(doc.selectFirst("#usuario-form-senha").val()).isEmpty();
        assertThat(doc.html()).doesNotContain("curta");
        verify(usuarios, never()).criar(any());
    }

    @Test
    void perfilDesconhecidoEhRecusadoNoCampo() throws Exception {
        Document doc = enviar(400, "nome", "Ana Lima", "login", "ana@exemplo.com", "senha", "senha-boa-123", "perfil", "ROOT");

        assertThat(doc.selectFirst("[data-testid=usuario-form-perfil-erro]").text()).isEqualTo("Informe o perfil.");
        verify(usuarios, never()).criar(any());
    }

    @Test
    void loginRepetidoViraErroNoCampoLogin() throws Exception {
        when(usuarios.criar(any())).thenThrow(new LoginEmUsoException("ana@exemplo.com"));

        Document doc = enviar(400, "nome", "Ana Lima", "login", "ana@exemplo.com", "senha", "senha-boa-123", "perfil", "RESPONSAVEL");

        assertThat(doc.selectFirst("[data-testid=usuario-form-login-erro]").text())
                .isEqualTo("Já existe um usuário com este e-mail.");
        assertThat(doc.selectFirst("select[name=perfil] option[selected]").val()).isEqualTo("RESPONSAVEL");
    }

    @Test
    void cadastroCertoVoltaParaAListaComAConfirmacao() throws Exception {
        when(usuarios.criar(any())).thenReturn(new UsuarioResponse(7L, "Ana Lima", "ana@exemplo.com", PerfilUsuario.SOLICITANTE));

        mvc.perform(post("/admin/usuarios").with(csrf()).param("nome", "Ana Lima").param("login", "ana@exemplo.com")
                        .param("senha", "senha-boa-123").param("perfil", "SOLICITANTE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/admin/usuarios"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("confirmacao", java.util.Map.of("titulo", "Usuário cadastrado",
                                "texto", "Ana Lima já pode entrar com o e-mail ana@exemplo.com.")));
    }

    @Test
    void emailEmMaiusculasEEspacosChegaAoServiceNormalizadoENomeComEspacosRepetidosTambem() throws Exception {
        when(usuarios.criar(any())).thenReturn(new UsuarioResponse(7L, "Ana Lima", "ana@exemplo.com", PerfilUsuario.SOLICITANTE));

        mvc.perform(post("/admin/usuarios").with(csrf()).param("nome", "  Ana   Lima ").param("login", "  ANA@Exemplo.COM ")
                        .param("senha", "senha-boa-123").param("perfil", "SOLICITANTE"))
                .andExpect(status().is3xxRedirection());

        var pedido = org.mockito.ArgumentCaptor.forClass(br.com.senac.recursos.dto.request.UsuarioRequest.class);
        verify(usuarios).criar(pedido.capture());
        assertThat(pedido.getValue().login()).isEqualTo("ana@exemplo.com");
        assertThat(pedido.getValue().nome()).isEqualTo("Ana Lima");
    }

    @Test
    void emojiNoNomeOuNoEmailVoltaComAMensagemDeEmojiNoCampo() throws Exception {
        Document doc = enviar(400, "nome", "Ana \uD83D\uDE00 Lima", "login", "ana\uD83D\uDE00@exemplo.com",
                "senha", "senha-boa-123", "perfil", "SOLICITANTE");

        assertThat(doc.selectFirst("[data-testid=usuario-form-nome-erro]").text()).isEqualTo("Não use emojis neste campo.");
        assertThat(doc.selectFirst("[data-testid=usuario-form-login-erro]").text()).isEqualTo("Não use emojis neste campo.");
        verify(usuarios, never()).criar(any());
    }

    @Test
    void nomeSemSobrenomeEEmailSemDominioSaoRecusadosNoCampo() throws Exception {
        Document doc = enviar(400, "nome", "Ana", "login", "ana@exemplo", "senha", "senha-boa-123", "perfil", "SOLICITANTE");

        assertThat(doc.selectFirst("[data-testid=usuario-form-nome-erro]").text()).startsWith("Informe o nome completo");
        assertThat(doc.selectFirst("[data-testid=usuario-form-login-erro]").text())
                .isEqualTo("Informe um e-mail válido, como nome@dominio.com.");
        verify(usuarios, never()).criar(any());
    }

    @Test
    @ComoUsuario(PerfilUsuario.RESPONSAVEL)
    void responsavelNaoAbreATelaDeUsuarios() throws Exception {
        mvc.perform(get("/admin/usuarios")).andExpect(status().isForbidden());
    }
}
