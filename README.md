# Organização de Recursos

Sistema web de reservas de salas, professores e materiais, sem conflito de horário.
Projeto de Qualidade de Software (Senac, 2026.2, Prof. Afonso) — o foco é a **evidência de qualidade**: testes, CI, cobertura, rastreabilidade, desempenho e defeitos.

**Grupo:** Gustavo (banco de dados) · Pedro (back-end) · Leandro (front-end)

## Stack
Java 21 · Spring Boot 3 · Maven Wrapper · Spring Security (JWT) · PostgreSQL 16 + Flyway · JUnit 5, Testcontainers, WireMock, Cucumber, Playwright · GitHub Actions, JaCoCo, SonarCloud, JMeter

## Pré-requisitos
- **JDK 21** (o `JAVA_HOME` precisa apontar para ele; conferir com `java -version`)
- **Docker** (PostgreSQL local e testes com Testcontainers)
- Git. O Maven não precisa estar instalado: o Maven Wrapper (`mvnw`) baixa o Maven sozinho.

## Como subir
```bash
git clone https://github.com/GustAndrade07/organizacao-recursos.git
cd organizacao-recursos
docker compose up -d          # PostgreSQL 16 em localhost:5432
./mvnw spring-boot:run        # aplicação em http://localhost:8080
```
Conferir que subiu: abrir http://localhost:8080/actuator/health (deve responder `{"status":"UP"}`).
No Windows (PowerShell) use `.\mvnw.cmd` no lugar de `./mvnw`.

### Primeiro acesso à API
O sistema é uma API REST (sem telas; ADR-0008). Para criar o primeiro Administrador, suba a aplicação com duas variáveis de ambiente (escolha a senha na hora, nunca a escreva no repositório):
```bash
ADMIN_LOGIN=admin ADMIN_SENHA='<sua senha>' ./mvnw spring-boot:run
curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
     -d '{"login":"admin","senha":"<sua senha>"}'     # devolve {"token": ...}
```
Mande o token em cada requisição: `Authorization: Bearer <token>`. Rotas e permissões: `docs/seguranca/matriz-autorizacao.md`. Fora do desenvolvimento, defina também `JWT_SECRET` (32 bytes ou mais); sem ele, os tokens deixam de valer quando a aplicação reinicia.

> Passo a passo ainda **não validado em clone limpo** por outra pessoa (etapa 6 do plano).

## Como testar
```bash
./mvnw -B verify              # testes + cobertura (JaCoCo: o build falha abaixo de 80% de linhas / 70% de branches)
```
Os testes de integração usam Testcontainers (PostgreSQL 16 de verdade) e precisam do Docker rodando. Sem Docker eles **falham** (não são ignorados), para o `verify` mostrar sempre `Skipped: 0`.
Relatório de cobertura: `target/site/jacoco/index.html`.

## Regras do dia a dia
- Nunca commitar direto na `main`: branch → pull request → revisão de outra pessoa → merge.
- Revisão em roda: Gustavo revisa Pedro, Pedro revisa Leandro, Leandro revisa Gustavo.
- TDD: commit do teste vermelho **antes** do que faz passar.
- Defeito achado: registrar em `docs/defeitos/` e só então corrigir, citando o defeito no commit.
- Nenhum token ou senha no repositório (SonarCloud usa o secret `SONAR_TOKEN`).

## Documentação
`AGENTS.md` (regras do grupo e da IA; todo PR atualiza o plano) · `docs/plano-execucao.md` · `docs/prd.md` · `docs/frontend/` (design system, tokens e inventário de telas) · `docs/rtm.md` · `docs/status-requisitos.md` · `docs/adr/` · `docs/defeitos/` · `docs/testes/` · `docs/performance/` · `perf/` (plano JMeter) · `docs/entrega/checklist.md` · `docs/prova-oral/`

## Qualidade e CI
- **CI:** GitHub Actions (`.github/workflows/ci.yml`) roda `./mvnw -B verify` em todo pull request e em push na `main`; os relatórios do Surefire e do JaCoCo ficam como artifact.
- **Cobertura:** JaCoCo com gate de 80% de linhas e 70% de branches no `verify`.
- **Análise estática:** SonarCloud, projeto [GustAndrade07_organizacao-recursos](https://sonarcloud.io/project/overview?id=GustAndrade07_organizacao-recursos). O token fica só no secret `SONAR_TOKEN` do GitHub.
- A `main` é protegida: só recebe código por pull request aprovado por outra pessoa, com o check `verify` verde.

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
