# Status — 13/11/2026 — `feat/openapi-rf14`

**Quem:** feito pelo Leandro (com IA) na área do Pedro (back-end), com o OK do Leandro; **Pedro revisa**. Mesmo combinado do PR #52.

> Este arquivo de status é de uma das partes do PR `test/qualidade-testes-e-openapi-1311`, que junta cinco assuntos da área do Pedro num PR só (combinado com o Leandro em 13/11/2026, para não serem cinco revisões no dia da entrega). O `verify` da ponta do PR tem **1138 testes, 0 falhas, 0 erros, 0 ignorados**.

**Lacuna fechada:** a 8 da tabela "Lacunas para a entrega" (RF-14, OpenAPI).

## O que mudou
- `pom.xml`: `springdoc-openapi-starter-webmvc-ui` 2.8.17 (linha do springdoc para Spring Boot 3.5).
- `config/OpenApiConfig.java` (novo): título, versão, descrição (autenticação, intervalo `[início, fim)` da D1, erros em `application/problem+json`) e o esquema de segurança `bearer-jwt`.
- `controller/RelatorioApiController.java`: `@Operation`, `@Parameter` e `@ApiResponses` (200, 400, 401, 403) no `GET /api/relatorios/utilizacao`, dizendo também que as premissas da D8 seguem pendentes.
- `config/SegurancaDasTelasConfig.java`: `/v3/api-docs`, `/swagger-ui.html` e `/swagger-ui/**` exigem sessão.
- `docs/seguranca/matriz-autorizacao.md`: seção nova **"Rotas da documentação"**.
- `OpenApiDocumentacaoTest` (novo), `README.md`, `docs/plano-execucao.md`.

## A decisão que este PR tomou (e por que não é decisão de grupo)
As rotas do Swagger **exigem login**. Não é um ponto da seção 9 do plano (nenhuma decisão D trata disso) e não muda regra de negócio; é a escolha conservadora: não abrir ao anônimo o mapa da API. Está escrita na matriz, com o que mudar se o grupo preferir deixar aberto — e o teste fica vermelho até alguém mudar as duas pontas, de propósito.

**Por que as rotas do springdoc não viraram linha da matriz:** `MatrizAutorizacaoTest.matrizCobreExatamenteAsRotasDoCodigo` compara a matriz com as rotas dos pacotes `controller` e `web`. As do springdoc são do pacote `org.springdoc`, então uma linha nova na tabela quebraria a comparação sem acrescentar segurança. Ficaram numa seção própria, em texto, com teste próprio.

## Verificado
- `./mvnw -B test -Dtest=OpenApiDocumentacaoTest`: **5 testes, 0 falhas, 0 erros, 0 ignorados**, 13,06 s (13/11/2026, container Linux desta sessão).
  - a documentação descreve **exatamente** as rotas `/api/**` da matriz (23 rotas), comparando conjunto com conjunto;
  - o relatório tem resumo, os dois parâmetros obrigatórios e as quatro respostas;
  - título e esquema `bearer-jwt` presentes;
  - sem sessão, `/v3/api-docs` e `/swagger-ui/index.html` levam ao `/login`;
  - o JSON não contém senha de teste, `recursos_dev` nem `jwt_secret`.
- `./mvnw -B verify` completo nesta branch: **1121 testes, 0 falhas, 0 erros, 0 ignorados**, `BUILD SUCCESS`, "All coverage checks have been met" (1116 na `main` + os 5 testes da documentação).

## Não verificado
- Swagger UI aberto num navegador de verdade (o teste lê o JSON e o redirecionamento, não a página).
- Run do CI e efeito do springdoc no tempo do build no CI.
- Se o SonarCloud aponta algo na dependência nova (`docs/seguranca/riscos-dependencias.md` é do Gustavo).
- `docs/status-requisitos.md` e `docs/rtm.md`: atualizados depois do merge do PR #57 na `main`; RF-14 passa a `feito`, citando `OpenApiDocumentacaoTest`.
