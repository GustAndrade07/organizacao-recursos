# Organização de Recursos

Sistema web de reservas de salas, professores e materiais, sem conflito de horário.
Projeto de Qualidade de Software (Senac, 2026.2, Prof. Afonso) — o foco é a **evidência de qualidade**: testes, CI, cobertura, rastreabilidade, desempenho e defeitos.

**Grupo:** Gustavo (banco de dados) · Pedro (back-end) · Leandro (front-end)

## Stack
Java 21 · Spring Boot 3 · Maven Wrapper · Thymeleaf · PostgreSQL 16 + Flyway · JUnit 5, Testcontainers, WireMock, Cucumber, Playwright · GitHub Actions, JaCoCo, SonarCloud, JMeter

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

> Passo a passo ainda **não validado em clone limpo** por outra pessoa (etapa 6 do plano).

## Como testar
```bash
./mvnw -B verify              # testes + cobertura (JaCoCo: o build falha abaixo de 80% de linhas / 70% de branches)
```
Os testes de integração usam Testcontainers e precisam do Docker rodando. Sem Docker eles são ignorados (não falham), e o CI os executa.
Relatório de cobertura: `target/site/jacoco/index.html`.

## Regras do dia a dia
- Nunca commitar direto na `main`: branch → pull request → revisão de outra pessoa → merge.
- Revisão em roda: Gustavo revisa Pedro, Pedro revisa Leandro, Leandro revisa Gustavo.
- TDD: commit do teste vermelho **antes** do que faz passar.
- Defeito achado: registrar em `docs/defeitos/` e só então corrigir, citando o defeito no commit.
- Nenhum token ou senha no repositório (SonarCloud usa o secret `SONAR_TOKEN`).

## Documentação
`docs/prd.md` · `docs/frontend/` (design system, tokens e inventário de telas) · `docs/rtm.md` · `docs/status-requisitos.md` · `docs/adr/` · `docs/defeitos/` · `docs/testes/` · `docs/performance/` · `perf/` (plano JMeter) · `docs/entrega/checklist.md` · `docs/prova-oral/`

## Histórico de CI vermelho proposital
(anotar aqui o CI vermelho proposital e a recuperação)
