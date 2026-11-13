# Status em 13/11/2026 (sexta, branch `docs/gustavo-pendencias-entrega`)

Só documentação, a partir da `main` (`bc40183`, merge do #62; CI run #251 com `verify` e `SonarCloud Code Analysis` verdes). Fecha as pendências do Gustavo levantadas depois do merge dos PRs #14 a #62. Nenhum código de produção, teste ou configuração mudou.

## O que muda
- **`README.md`** (Gustavo):
  - "Como subir": forma do **PowerShell** (`$env:ADMIN_LOGIN=...; $env:ADMIN_SENHA=...; .\mvnw.cmd spring-boot:run`) e do **cmd** (`set ...`). A forma `VAR=valor comando` é do bash e não funciona no Windows (achado da revisão do PR #60).
  - Nota do cookie de sessão: `Secure` só com HTTPS e `SESSAO_COOKIE_SEGURO=true`; em HTTP local fica desligado.
  - Stack: Cucumber 7.23.0 (PR #61), springdoc (RF-14) e JMeter 5.6.3 (PR #56, fora do `pom.xml` e do CI). WireMock continua "ainda não presente" e o RF-11 ausente.
- **`docs/seguranca/riscos-dependencias.md`** (Gustavo):
  - Seção 8.1 nova: CVE-2026-59276 conferido no advisory oficial (spring.io), no NVD e na OSV. **Não atinge**: os 4 componentes afetados (`DigestAuthenticationFilter`, `KeyBasedPersistenceTokenService`, codificadores Password4j, `InMemoryOAuth2AuthorizationService`) não são usados (0 ocorrências em `src/` e no `pom.xml`; Password4j e authorization server fora do `dependency:tree`). A 6.5.12 corrigida é só "Enterprise Support"; a correção aberta exige a 7.x.
  - Seção 9 nova: springdoc 2.8.17 (compile), `commons-lang3` 3.22.0 (override; o Boot gerencia a 3.17.0, com o CVE-2025-48924, CVSS 5,3), Cucumber 7.23.0 com o `cucumber-bom`, `junit-platform-suite` 1.12.2 e Playwright 1.63.0 (test). Escopo, motivo, CVE (OSV, pacote a pacote, inclusive transitivos) e fonte, com data.
- **`docs/avaliacao-arquitetural/ciclo-01.md`** (Gustavo): seção 8.2 nova com as medidas da `main` `bc40183`; RK9 continua **fechado**, reconferido com o Quality Gate OK deste commit; fonte 12 nova.
- **`docs/plano-execucao.md`**: lacunas 5 e 10 da seção 4.1 marcadas como resolvidas; notas novas nas caixas do README (Etapa 1), do SonarCloud (Etapa 4) e da tag (Etapa 6). **Nenhuma caixa foi marcada**: a do README depende do clone limpo humano (Pedro), e a do SonarCloud e a da tag dependem do commit da tag.
- `docs/status-requisitos.md` e a RTM não mudam: nenhum requisito mudou de estado.

## Verificado (13/11/2026, máquina do Gustavo: Windows 11, JDK 21, Docker Desktop)
- `.\mvnw.cmd -B verify` no `bc40183`: `Tests run: 1138, Failures: 0, Errors: 0, Skipped: 0`, "All coverage checks have been met", BUILD SUCCESS (19 min 24 s). JaCoCo (`target/site/jacoco/jacoco.csv`): 1479 de 1543 linhas (95,85%) e 422 de 519 branches (81,31%). O `verify` foi repetido na ponta da branch antes do commit; o resultado está no corpo do PR.
- SonarCloud, API pública, análise do `bc40183`: Quality Gate OK, 0 bugs, 0 vulnerabilidades, 0 hotspots (100% revisados), 1 code smell, duplicação 0%, 95,9% de linhas e 81,7% de branches.
- GitHub (API pública): run #251 (ID 43465315272) `success`; check-runs `verify` e `SonarCloud Code Analysis` em `success` no `bc40183`.
- README no Windows: a forma do PowerShell, com `docker compose up -d`, subiu a aplicação, criou o Administrador, respondeu `{"status":"UP"}` e o `POST /api/auth/login` deu 200. `Set-Cookie` de `GET /login`: sem a variável, `HttpOnly; SameSite=Lax`; com `SESSAO_COOKIE_SEGURO=true`, `Secure; HttpOnly; SameSite=Lax`. Senha gerada só para o teste, apagada depois; banco local removido (`docker compose down -v`).
- Dependências: `.\mvnw.cmd -B dependency:tree`; OSV (`api.osv.dev`), NVD e `maven-metadata.xml` do Maven Central lidos em 13/11/2026.

## Não verificado
- **Forma do cmd** do README: não rodada.
- **Aplicação atrás de HTTPS** com `Secure`: não testada (só o cabeçalho por HTTP local).
- **Painel "Dependency risks" do SonarCloud:** a API respondeu 401 sem login; não se sabe se o painel acusa algo das dependências novas. Pendência manual do Gustavo.
- **Contagem de testes no log do CI** (run #251): o log do Actions exige login; vale o `verify` local.
- **Diferença de branches** entre JaCoCo (81,31%) e Sonar (81,7%): não investigada.

## Spec do Notion "Specs do Banco de Dados" (aplicado em 13/11/2026)
Aplicado pelo navegador integrado, logado, com a autorização explícita do Gustavo na sessão (a primeira tentativa tinha sido bloqueada pela checagem de permissão). Conferido recarregando a página: os textos abaixo estão na spec e as caixas marcadas ficaram marcadas. Link: https://app.notion.com/p/Specs-do-Banco-de-Dados-4108bb977b7b81aabca5c5aae9b050ee

**Seção 1, "Situação hoje":** os dois itens com o estado da `main` (o que começava em "Testes de banco em ...", com os números de 12/11 em `2610d6a`, e o que começava em "Os PRs #48, #49, #50 e #51 ...") foram trocados pelo texto abaixo, em dois itens. Os dois últimos bullets da seção (`pom.xml`/Flyway e back-end do PR #44) continuam valendo e não foram apagados. Texto:

> Em 13/11/2026 a main está em bc40183 (merge do #62). Desde 12/11 entraram #52, #56 a #59, #60, #61 e #62. Nenhuma migração nova: db/migration continua só com a V1. verify local do #61: 1138 testes, 0 falhas, 0 ignorados. Cobertura medida em 9acbbcd: 95,8% de linhas e 81,3% de branches (refazer no commit da tag).

No bullet do back-end (PR #44), "ADR-0008, proposto" virou "ADR-0008, aceito no PR #58".

**Seção 5, "Testes do banco":** trocado "Falta o vermelho sem a proteção no nível da API (Pedro)." por:

> Mutação M1 na API (Pedro, 12/11/2026): sem a constraint, ConcorrenciaReservaApiTest fica com 10 de 10 vermelhos; com ela, 10 de 10 verdes. Medida só na máquina do Pedro.

**Seção 6, "Pontos em aberto":**
- Marcado como feito (com "Feito no PR #61.") "Atualizar a tabela de fronteiras de docs/testes/bdd-aprovacao-bloqueio.md com a D1" (PR #61).
- No item do custo da constraint GiST e da trava da D7, o texto depois dos dois-pontos foi trocado por: "Medido com a constraint e a trava ativas, só local: busca com p95 de 17 e 8 ms; criação sob disputa só 201 ou 409 (80 × 201 e 3920 × 409). O custo isolado da constraint continua não medido." (continua aberto).
- No item dos riscos aceitos que tocam o banco, trocado "que ainda não existe na main (plano, seção 4.1, lacuna 2)" por "mergeado no PR #59 (D13 é o risco 4 e a V2 é o risco 6)" e marcado como feito.
- No item da D13, depois de "depende da D11", acrescentado "(D11 decidida no PR #58)".
- No item "Arquitetura: levar os números ... e fechar o RK9", marcado como feito, com o texto "ciclo-01, seções 8.1 (2610d6a) e 8.2 (bc40183, 13/11/2026); RK9 fechado e reconferido".
- Continuam abertos: D8, a divergência com a tela 12 (seção 8) e a V2.

## Decisões do Gustavo registradas neste PR (13/11/2026)
- **Migração V2:** não entra antes da tag; fica como risco aceito (risco 6 de `docs/decisao-qualidade.md`). A V2 completa pode sair como `v1.0.0-rc2` antes da prova oral. Registrada na caixa das entidades (Etapa 3) do plano.
- **Declaração do Gustavo** escrita na seção 4 de `docs/decisao-qualidade.md`. O arquivo é do Leandro; a edição foi combinada com ele e mexe só na linha do Gustavo.
- **Commits com autor "Claude" na `main`:** não serão corrigidos. Reescrever a `main` exigiria force push (bloqueado pelo ruleset), mudaria hashes citados nos documentos e quebraria o histórico red → green. **Achado:** são **8**, não 7 — além dos 2 de 04/11 (`3ae747c`, `07606da`) e dos 5 do PR #52 (`cd14f8c`, `e86c777`, `f0777de`, `5c13534`, `eb6ad27`), há o `2c6c8f6` (09/11, merge de `origin/main` no PR #32). Conferido com `git log origin/main --author=noreply@anthropic.com`. A contagem "7" em `docs/decisao-qualidade.md` (seção 4) e em `docs/relatorio-qualidade.md` (seção 5) é do Leandro corrigir.

## Pendências manuais do Gustavo (fora deste PR)
- [ ] D4: registrar a decisão (texto a combinar; hoje o plano e a decisão de qualidade dizem "pergunta na Aula 14").
- [ ] SonarCloud (logado): marcar os riscos novos de dependência, se houver, trocar a justificativa do CVE-2026-59276 de "não verificado" para "não atinge" (seção 8.1) e conferir o gate da `main`.
- [ ] Tag anotada `v1.0.0-rc1` só depois do clone limpo humano (Pedro) e com CI e SonarCloud verdes no commit da tag.
