# Organização de Recursos

Sistema web de reservas de salas, professores e materiais, sem conflito de horário.
Projeto de Qualidade de Software (Senac, 2026.2, Prof. Afonso) — o foco é a **evidência de qualidade**: testes, CI, cobertura, rastreabilidade, desempenho e defeitos.

**Grupo:** Gustavo (banco de dados) · Pedro (back-end) · Leandro (front-end)

## Stack
Java 21 · Spring Boot 3 · Maven Wrapper · Thymeleaf · PostgreSQL 16 + Flyway · JUnit 5, Testcontainers, WireMock, Cucumber, Playwright · GitHub Actions, JaCoCo, SonarCloud, JMeter

## Como subir
```bash
docker compose up -d          # PostgreSQL
./mvnw spring-boot:run        # aplicação em http://localhost:8080
```
> Preencher/validar passo a passo quando o projeto Spring existir (precisa funcionar em clone limpo).

## Como testar
```bash
./mvnw -B verify              # testes + cobertura (JaCoCo 80% linhas / 70% branches)
```

## Regras do dia a dia
- Nunca commitar direto na `main`: branch → pull request → revisão de outra pessoa → merge.
- Revisão em roda: Gustavo revisa Pedro, Pedro revisa Leandro, Leandro revisa Gustavo.
- TDD: commit do teste vermelho **antes** do que faz passar.
- Defeito achado: registrar em `docs/defeitos/` e só então corrigir, citando o defeito no commit.
- Nenhum token ou senha no repositório (SonarCloud usa o secret `SONAR_TOKEN`).

## Documentação
`docs/prd.md` · `docs/rtm.md` · `docs/status-requisitos.md` · `docs/adr/` · `docs/defeitos/` · `docs/testes/` · `docs/performance/` · `perf/` (plano JMeter) · `docs/entrega/checklist.md` · `docs/prova-oral/`

## Histórico de CI vermelho proposital
(anotar aqui o CI vermelho proposital e a recuperação)
