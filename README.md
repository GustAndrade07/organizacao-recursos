# Organização de Recursos

Sistema web de reservas de salas, professores e materiais, sem conflito de horário.
Projeto de Qualidade de Software (Senac, 2026.2, Prof. Afonso) — o foco é a **evidência de qualidade**: testes, CI, cobertura, rastreabilidade, desempenho e defeitos.

**Grupo:** Gustavo (banco de dados) · Pedro (back-end) · Leandro (front-end)

## Stack
Java 21 · Spring Boot 3 · Maven Wrapper · Thymeleaf · Spring Security (JWT na API) · springdoc-openapi (RF-14) · PostgreSQL 16 + Flyway · JUnit 5, Testcontainers, Playwright, Cucumber · GitHub Actions, JaCoCo, SonarCloud · JMeter (carga, fora do `verify`)

- **Cucumber** (7.23.0, no `pom.xml` desde o PR #61, 13/11/2026): os cenários de `docs/testes/bdd-aprovacao-bloqueio.md` rodam no `verify` (`src/test/resources/features/aprovacao-bloqueio.feature`).
- **JMeter** (5.6.3, PR #56, 12/11/2026): **não** está no `pom.xml` nem no CI; roda à parte, em modo CLI, contra a aplicação local (`perf/README.md`). Plano em `perf/reservas.jmx`, resultados brutos em `perf/resultados/` e a leitura em `docs/performance/resultado-01.md`.
- Previsto e **ainda não presente** (conferido em 13/11/2026): **WireMock** (notificação externa, RF-11), que não está no `pom.xml` nem em `src/` — o RF-11 segue ausente.

## Pré-requisitos
- **JDK 21** (o `JAVA_HOME` precisa apontar para ele; conferir com `java -version`)
- **Docker** (PostgreSQL local e testes com Testcontainers)
- Git. O Maven não precisa estar instalado: o Maven Wrapper (`mvnw`) baixa o Maven sozinho.
- **Porta 5432 livre.** Se a máquina já tiver um PostgreSQL instalado (serviço do Windows, por exemplo), a aplicação fala com ele em vez do container e para no Flyway com "autenticação do tipo senha falhou para o usuário recursos" (achado no clone limpo de 13/11/2026, `docs/entrega/roteiro-clone-limpo.md`). Pare esse serviço ou suba o banco do projeto em outra porta, como em "Porta 5432 ocupada", abaixo.

## Como subir
```bash
git clone https://github.com/GustAndrade07/organizacao-recursos.git
cd organizacao-recursos
docker compose up -d          # PostgreSQL 16 em localhost:5432
ADMIN_LOGIN=admin@exemplo.com ADMIN_SENHA='<escolha uma senha>' ./mvnw spring-boot:run   # http://localhost:8080
```
Conferir que subiu: abrir http://localhost:8080/actuator/health (deve responder `{"status":"UP"}`).

**Dois cuidados** (achados no clone limpo do Pedro, 13/11/2026):
- **A ordem importa:** sem o `docker compose up -d` antes, não há banco, e o `spring-boot:run` para com um erro genérico do Maven (`MojoExecutionException`).
- **Troque `<escolha uma senha>` inteiro**, inclusive os sinais `<` e `>`, pela senha que você vai usar para entrar. Se colar o comando sem trocar, a senha do Administrador vira literalmente `<escolha uma senha>`. Subir de novo com outra `ADMIN_SENHA` não troca a senha de um Administrador que já existe: para recomeçar, `docker compose down -v`.

**No Windows** a forma `VAR=valor comando` da última linha é do bash e **não funciona** no PowerShell nem no cmd. Defina as variáveis antes e use `.\mvnw.cmd` no lugar de `./mvnw` (as variáveis valem só para aquela janela do terminal):
```powershell
# PowerShell
$env:ADMIN_LOGIN='admin@exemplo.com'; $env:ADMIN_SENHA='<escolha uma senha>'; .\mvnw.cmd spring-boot:run
```
```bat
:: cmd (Prompt de Comando), sem aspas em volta da senha
set ADMIN_LOGIN=admin@exemplo.com
set ADMIN_SENHA=<escolha uma senha>
mvnw.cmd spring-boot:run
```
O `ADMIN_LOGIN` é um **e-mail** (desde o PR #67): com um valor sem `@`, como `admin`, a aplicação **não sobe**. A forma do cmd **não foi testada**.

**Porta 5432 ocupada** (PostgreSQL já instalado na máquina): suba o container em outra porta e aponte a aplicação para ela com `DB_URL`, que o `application.yml` já aceita. Sem `DB_PORTA`, o container continua na 5432. O `verify` não usa essa porta: o Testcontainers sobe o próprio banco numa porta livre.
```powershell
# PowerShell
$env:DB_PORTA='5433'; docker compose up -d
$env:DB_URL='jdbc:postgresql://localhost:5433/recursos'
$env:ADMIN_LOGIN='admin@exemplo.com'; $env:ADMIN_SENHA='<escolha uma senha>'; .\mvnw.cmd spring-boot:run
```
```bash
# bash
DB_PORTA=5433 docker compose up -d
DB_URL=jdbc:postgresql://localhost:5433/recursos ADMIN_LOGIN=admin@exemplo.com ADMIN_SENHA='<escolha uma senha>' ./mvnw spring-boot:run
```

**Cookie de sessão das telas:** o cookie (`JSESSIONID`) sai sempre com `HttpOnly` e `SameSite=Lax`. O atributo `Secure` só é ligado com `SESSAO_COOKIE_SEGURO=true` (`server.servlet.session.cookie.secure` no `application.yml`), e isso só serve **com HTTPS**: com `Secure`, o navegador não devolve o cookie por HTTP e o login das telas deixa de funcionar. Por isso, em `http://localhost` (desenvolvimento local) a variável fica sem definir e o `Secure` fica **desligado**. Fora do desenvolvimento local, sirva a aplicação por HTTPS e defina `SESSAO_COOKIE_SEGURO=true`. Conferido em 13/11/2026 no `Set-Cookie` de `GET /login` (local, por HTTP): sem a variável, `HttpOnly; SameSite=Lax`; com ela, `Secure; HttpOnly; SameSite=Lax`. A aplicação atrás de HTTPS de verdade **não foi testada**.

**Ciclo de vida do cookie, conferido em 13/11/2026** (aplicação local na `main` `bf2584a`, por HTTP, com `curl`):

| Momento | O que acontece |
|---|---|
| Página de política (`/cookies`, `/privacidade`, `/termos`) sem sessão | Nenhum `Set-Cookie` |
| `GET /login` | Cria o `JSESSIONID` (`Path=/; HttpOnly; SameSite=Lax`, sem `Max-Age` nem `Expires`: é cookie de sessão do navegador) |
| `POST /login` com a senha certa | Troca o identificador do `JSESSIONID` (proteção contra fixação de sessão, padrão do Spring Security) |
| Navegação logada (início, recursos, reservas, aprovações, usuários, CSS, 404) | Nenhum cookie além do `JSESSIONID` |
| `POST /api/auth/login` | 200 e nenhum `Set-Cookie`: o JWT vai só no corpo e volta no cabeçalho `Authorization` |
| `POST /logout` | `JSESSIONID=; Max-Age=0`: o cookie é apagado; volta a `/login` |
| `JSESSIONID` de sessão que não existe mais | 302 para `/login?expirou&destino=...` |
| Tempo sem uso até a sessão vencer no servidor | **30 minutos**: `server.servlet.session.timeout` não está no `application.yml`, então vale o padrão do Spring Boot 3.5.16 (`Session.timeout = Duration.ofMinutes(30)`, lido no bytecode da classe) |

O JavaScript das telas não usa `localStorage`, `sessionStorage` nem `document.cookie` (0 ocorrências em `src/main/resources/static`). Os testes que guardam esse comportamento: `PoliticasPublicasIntegracaoTest` (só o `JSESSIONID` sai), `PaginaNaoEncontradaIntegracaoTest` (`HttpOnly` e `SameSite=Lax`), `PortaDeEntradaDasTelasTest` (`?expirou`) e `PoliticasPaginaTest`, todos verdes em 13/11/2026.

**Por que as duas variáveis:** o banco começa vazio e o cadastro de usuário exige um Administrador. `ADMIN_LOGIN` e `ADMIN_SENHA` criam o **primeiro Administrador** no start (`AdministradorInicialConfig`); sem elas a aplicação sobe, mas **ninguém consegue entrar** nem nas telas nem na API. Escolha a senha na hora e nunca a escreva no repositório. Esse mesmo login serve para o formulário em http://localhost:8080/login e para o `POST /api/auth/login`.

**O login é o e-mail** (minúsculas, até 254 caracteres, formato `nome@dominio.com`). O campo continua chamado `login` no formulário, no JSON da API e em `ADMIN_LOGIN`, mas o valor é o e-mail; o cadastro de usuário pede nome completo, e-mail, senha e perfil. Banco local criado antes dessa mudança (usuários sem `@`) não é convertido: recrie com `docker compose down -v` e suba de novo, ou peça ao Administrador que recadastre as pessoas.

### API REST e primeiro acesso
As telas ficam em `http://localhost:8080/login` e demais rotas do inventário; a API REST fica sob `/api/**` (ADR-0008). Com o Administrador inicial criado acima ("Como subir"), pegue o token:
```bash
curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
     -d '{"login":"admin@exemplo.com","senha":"<sua senha>"}'     # devolve {"token": ...}
```
Mande o token em cada requisição: `Authorization: Bearer <token>`. Rotas e permissões: `docs/seguranca/matriz-autorizacao.md`.

**Documentação da API (RF-14):** com a aplicação de pé e **depois de entrar pelo `/login`**, abra http://localhost:8080/swagger-ui/index.html (o JSON fica em `/v3/api-docs`). É gerada do próprio código pelo `springdoc-openapi`, e um teste (`OpenApiDocumentacaoTest`) falha se ela deixar de bater com a matriz de autorização. Fora do desenvolvimento, defina também `JWT_SECRET` (32 bytes ou mais); sem ele, os tokens deixam de valer quando a aplicação reinicia.

> Passo a passo **rodado em clone limpo pela IA em 13/11/2026**, no commit `9acbbcd`, num container novo: sobe, responde `UP`, grava reserva pela API e pela tela e recusa a sobreposição com 409 (`docs/entrega/roteiro-clone-limpo.md`, seção 4). A conferência da Aula 14 pede **uma pessoa que não escreveu o README**: o **Pedro** a fez em 13/11/2026, noite, no Windows, no commit `38cd0ce` (`verify` verde, `UP`, reserva gravada, dois cliques simultâneos na mesma sala → 1 reserva e 1 recusa 409), sem bloqueador (`docs/entrega/checklist.md`, "Terceira rodada").

## Como testar
```bash
./mvnw -B verify              # testes + cobertura (JaCoCo: o build falha abaixo de 80% de linhas / 70% de branches)
```
Os testes de integração usam Testcontainers (PostgreSQL 16 de verdade) e precisam do Docker rodando. Sem Docker eles **falham** (não são ignorados), para o `verify` mostrar sempre `Skipped: 0`.
Relatório de cobertura: `target/site/jacoco/index.html`.

## Banco de dados
- **Esquema só por migração Flyway** em `src/main/resources/db/migration` (hoje a `V1__nucleo_da_reserva.sql`); o Hibernate roda com `ddl-auto: validate` e nunca altera tabela ([ADR-0006](docs/adr/0006-flyway-dono-do-esquema.md)).
- **Migração já mergeada não se edita:** erro vira a próxima versão (`V2__...`). O CI barra PR que modifica, apaga ou renomeia uma migração existente.
- **Reserva dupla é barrada no próprio PostgreSQL** por uma constraint `EXCLUDE USING gist` sobre `tstzrange` (extensão `btree_gist`), com intervalo semiaberto `[início, fim)` ([ADR-0002](docs/adr/0002-anti-reserva-dupla.md), [ADR-0007](docs/adr/0007-modelagem-protecao-reserva-dupla.md)).
- Esquema consolidado para leitura: `docs/banco/schema.sql` (documentação, não migração). Consultas do relatório: `src/main/resources/db/consultas/`.
- Testes do banco em `src/test/java/br/com/senac/recursos/banco/` (PostgreSQL real via Testcontainers).
- Banco local que aplicou uma migração depois alterada: `docker compose down -v` apaga os dados e o próximo `up` recria do zero.

## Regras do dia a dia
- Nunca commitar direto na `main`: branch → pull request → revisão de outra pessoa → merge.
- Revisão em roda: Gustavo revisa Pedro, Pedro revisa Leandro, Leandro revisa Gustavo.
- TDD: commit do teste vermelho **antes** do que faz passar.
- Defeito achado: registrar em `docs/defeitos/` e só então corrigir, citando o defeito no commit.
- Nenhum token ou senha no repositório (SonarCloud usa o secret `SONAR_TOKEN`).

## Documentação
`AGENTS.md` (regras do grupo e da IA; todo PR atualiza o plano) · `docs/plano-execucao.md` · `docs/prd.md` · `docs/frontend/` (design system, tokens e inventário de telas) · `docs/rtm.md` · `docs/status-requisitos.md` · `docs/adr/` · `docs/defeitos/` · `docs/testes/` · `docs/performance/` · `perf/` (plano JMeter) · `docs/entrega/checklist.md` · `docs/prova-oral/`

## Qualidade e CI
- **CI:** GitHub Actions (`.github/workflows/ci.yml`) roda `./mvnw -B verify` em todo pull request e em push na `main`; os relatórios do Surefire e do JaCoCo ficam como artifact. Em pull request, um passo antes do build falha se uma migração `V<n>__*.sql` já existente for alterada (`.github/scripts/verificar-migracoes-imutaveis.sh`, com auto-teste que roda sempre).
- **Cobertura:** JaCoCo com gate de 80% de linhas e 70% de branches no `verify`.
- **Análise estática:** SonarCloud, projeto [GustAndrade07_organizacao-recursos](https://sonarcloud.io/project/overview?id=GustAndrade07_organizacao-recursos). O token fica só no secret `SONAR_TOKEN` do GitHub.
- A `main` é protegida: só recebe código por pull request aprovado por outra pessoa, com os checks `verify` e `SonarCloud Code Analysis` verdes e a branch atualizada.

## Histórico de CI vermelho proposital
Etapa 4 do plano: provar que o pipeline barra código quebrado e que ele se recupera. Feito no [PR #30](https://github.com/GustAndrade07/organizacao-recursos/pull/30) (branch `ci/run-vermelho-proposital`), em 07/11/2026.

| Run | Commit | Resultado | Link |
|---|---|---|---|
| Vermelho proposital | `22b68ae` (`test(red)`) | `failure` | [run #120, ID 42427577443](https://github.com/GustAndrade07/organizacao-recursos/actions/runs/42427577443) |
| Recuperação (verde) | `1e5cc0b` (`ci: remove as falhas propositais...`) | `success` | [run #121, ID 42428144208](https://github.com/GustAndrade07/organizacao-recursos/actions/runs/42428144208) |

**O que o run vermelho mostrou** (fonte: log e anotações do job `verify` do run #120):
- Duas falhas provocadas de propósito: o teste novo `CiVermelhoPropositalTest` (`1 + 1 == 3`) e a asserção do limite de 2000 caracteres invertida em `CaminhoDoSiteTest.limiteDeTamanhoEDe2000Caracteres`.
- O passo "Build, testes e cobertura" terminou com `Tests run: 428, Failures: 2, Errors: 0, Skipped: 0` e `BUILD FAILURE`; o job `verify` terminou em `failure` em 52 s.
- O passo de artifacts rodou mesmo com a falha (`if: always()`, passo `success`) e o passo do SonarCloud foi pulado (`skipped`), porque o `verify` falhou antes dele.

**O que o run verde mostrou** (fonte: jobs e check-runs do run #121 na API do GitHub): job `verify` em `success` em 1 min 48 s; passos "Build, testes e cobertura", artifacts e SonarCloud todos em `success`; checks `verify` e `SonarCloud Code Analysis` verdes no commit `1e5cc0b`. O total de testes no log do CI não foi lido neste run (log recolhido); o `verify` local do mesmo código deu 427 testes, 0 ignorados.

**Como foi feito:** o `ci.yml` só dispara em `pull_request` e em push na `main`, então o vermelho exigiu abrir PR (em rascunho, sem pedir revisão até o verde). O commit de correção removeu o teste novo e restaurou a asserção. Medições feitas só nesta máquina ou só neste run não se generalizam.
