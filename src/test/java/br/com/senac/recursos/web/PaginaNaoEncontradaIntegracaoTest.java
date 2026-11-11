package br.com.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Com servidor de verdade (porta aleatória): uma URL que não existe tem que devolver a página 404 do sistema, e não a
 * página padrão do Spring. Prova o caminho completo do container até o template.
 *
 * <p>Desde o back-end (PR #44) a aplicação não sobe sem banco (serviços e repositórios JPA), então o teste usa
 * PostgreSQL real em Testcontainers, como os E2E, em vez de excluir a configuração do banco.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaginaNaoEncontradaIntegracaoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    TestRestTemplate http;

    @Test
    void urlInexistenteDevolve404ComAPaginaDoSistema() {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setAccept(java.util.List.of(MediaType.TEXT_HTML));

        ResponseEntity<String> resposta = http.exchange("/nao-existe", HttpMethod.GET, new HttpEntity<>(cabecalhos), String.class);
        Document doc = Jsoup.parse(resposta.getBody());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(doc.selectFirst("[data-testid=erro-codigo]").text()).isEqualTo("Erro 404");
        assertThat(doc.selectFirst("[data-testid=erro-titulo]").text()).isEqualTo("Página não encontrada");
        assertThat(doc.selectFirst("a[data-testid=erro-acao-principal]").attr("href")).isEqualTo("/");
        assertThat(doc.text()).doesNotContain("Whitelabel");
    }
}
