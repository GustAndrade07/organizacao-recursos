# Riscos de dependências (SonarCloud "Dependency risks")

> Fonte dos números: painel "Dependency risks" do SonarCloud, projeto `GustAndrade07_organizacao-recursos`, branch `main`, lido em 05/11/2026 (análise do commit `9494c09`, de 00:57 UTC). Classificação "Critical/High" = **CVSS** informado pelo SonarCloud; o SonarCloud classifica o **impacto** de todos como "Medium" porque a alcançabilidade não foi analisada ("Reachability: Not analyzed").
> Meta do professor: **0 bugs ou vulnerabilidades críticas**. O Quality Gate (Passed) **não** olha estes riscos; por isso o tratamento é manual e registrado aqui.
> O que não foi medido está escrito **"não medido"**.

## 1. Situação antes da correção (main, 05/11/2026)
48 riscos em 122 dependências: **8 Critical**, 10 High, 15 Medium, 4 Low e 11 sem nota.

| Biblioteca (versão) | Riscos | Critical | High | Escopo |
|---|---|---|---|---|
| `org.apache.tomcat.embed:tomcat-embed-core` 10.1.55 | 9 | 6 | 1 | compile |
| `org.springframework:spring-webmvc` 6.2.19 | 5 | 2 | 0 | compile |
| `com.fasterxml.jackson.core:jackson-databind` 2.21.4 | 8 | 0 | 3 | compile |
| `com.fasterxml.jackson.core:jackson-core` 2.21.4 | 3 | 0 | 3 | compile |
| `org.postgresql:postgresql` 42.7.11 | 3 | 0 | 1 | runtime |
| `org.springframework:spring-web` 6.2.19 | 4 | 0 | 0 | compile |
| `org.jsoup:jsoup` 1.21.1 | 2 | 0 | 1 | test |
| `org.glassfish.jaxb:jaxb-core` 4.0.9 | 1 | 0 | 1 | runtime |
| `org.apache.commons:commons-compress` 1.24.0 | 2 | 0 | 0 | test |
| `ch.qos.logback:logback-classic` / `logback-core` 1.5.34 | 1 / 1 | 0 | 0 | compile |
| `org.springframework:spring-expression` 6.2.19 | 2 | 0 | 0 | compile |
| `org.springframework:spring-beans` 6.2.19 | 1 | 0 | 0 | compile |
| `org.springframework.data:spring-data-jpa` 3.5.13 | 1 | 0 | 0 | compile |
| `org.apache.logging.log4j:log4j-api` 2.24.3 | 1 | 0 | 0 | compile |
| `net.minidev:json-smart` 2.5.2 | 1 | 0 | 0 | test |
| `io.micrometer:micrometer-core` 1.15.12 | 1 | 0 | 0 | compile |
| `org.hdrhistogram:HdrHistogram` 2.2.2 / `org.latencyutils:LatencyUtils` 2.0.3 | 1 / 1 | 0 | 0 | runtime |

## 2. O que foi feito (PR `fix/dependencias-seguras`)
Versões sobrescritas por propriedade no `pom.xml` (o Spring Boot 3.5.16 gerencia versões mais antigas), cada uma na versão mais recente publicada no Maven Central em 05/11/2026 dentro da mesma linha:

| Propriedade | Antes | Depois | Observação |
|---|---|---|---|
| `tomcat.version` | 10.1.55 | **10.1.61** | O SonarCloud indica 10.1.59 como "versão mais próxima sem vulnerabilidade conhecida" para o CVE-2026-65637 (corrigido a partir da 10.1.58 segundo a descrição do CVE). Deve resolver os 6 críticos do Tomcat (confirmação só na reanálise: não medido). |
| `jackson-bom.version` | 2.21.4 | 2.22.4 | Salto de minor; teste de regressão é a suíte. |
| `postgresql.version` | 42.7.11 | 42.7.14 | Patch. |
| `logback.version` | 1.5.34 | 1.5.38 | Patch. |
| `log4j2.version` | 2.24.3 | 2.25.5 | Minor. |
| `jsoup.version` (teste) | 1.21.1 | 1.22.2 | Só usado em testes de tela. |

Também foi incluído `src/main/resources` em `sonar.sources`, para o SonarCloud analisar os templates Thymeleaf (aviso "templates not indexed").

**Verificado:** `./mvnw -B verify` com BUILD SUCCESS, 30 testes, 0 ignorados; `dependency:tree` mostra as versões novas resolvidas (Tomcat 10.1.61, Jackson 2.22.4, PostgreSQL 42.7.14, Logback 1.5.38, log4j-api 2.25.5, jsoup 1.22.2).
**Não medido:** quantos dos 48 riscos somem. Só dá para saber depois do merge, com a nova análise da `main` no SonarCloud. Registrar o resultado aqui quando existir.

## 3. Riscos aceitos, com justificativa
Spring Framework 6.2.19 é a **última** da linha 6.2 usada pelo Spring Boot 3.5; a correção só existe a partir do Spring Framework **7.0.9** (SonarCloud: "nearest version without any known vulnerabilities" para o `spring-webmvc`), isto é, Spring Boot 4. Migrar de linha a poucos dias da entrega é um risco maior que o benefício, então os riscos abaixo ficam **abertos e justificados**.

| Risco | CVSS | O que o CVE descreve (SonarCloud) | Por que não nos atinge | Responsável | Revisão |
|---|---|---|---|---|---|
| CVE-2026-47884, `spring-webmvc` 6.2.19 | 9.8 Critical | SSRF e RCE no uso de `XsltView` em app Spring MVC com mapeamento `/**` que renderiza view sem nome explícito | O projeto usa **Thymeleaf**; não há `XsltView` nem XSLT | Gustavo | Se o Boot 3.5 publicar Spring Framework 6.2.20 ou superior, ou ao migrar para Boot 4 |
| CVE-2026-47890, `spring-webmvc` 6.2.19 | 9.8 Critical | Corrupção do fluxo em Server-Sent Events (SSE) com fragmentos de view | O projeto **não usa SSE** | Gustavo | idem |

Demais riscos do Spring Framework 6.2.19 (`spring-web`, `spring-beans`, `spring-expression`, `spring-webmvc`), do `spring-data-jpa` 3.5.13 e do `micrometer-core` 1.15.12: **sem versão mais nova na mesma linha** no Maven Central em 05/11/2026; avaliação caso a caso **não feita** (não medido o impacto real). A reanálise e a leitura de cada um fica como pendência abaixo.

## 4. Revisão de 11/11/2026 (PR `fix/riscos-dependencias-1111`)
- **Painel depois do merge do PR #12** (lido logado pelo Gustavo em 09/11/2026, branch `main`): **23 riscos em 123 dependências**; os 2 críticos (CVSS 9,8) do `spring-webmvc` 6.2.19 da seção 3 continuam abertos, e o `jsoup` 1.22.2 (teste) apareceu com um risco novo de CVSS 8,7. A lista completa por severidade não foi copiada (não medido aqui).
- **`jsoup.version` 1.22.2 → 1.24.1**, a última publicada no Maven Central em 11/11/2026. Só é usado em testes de tela. `.\mvnw.cmd -B verify` na branch: 510 testes, 0 falhas, 0 ignorados, BUILD SUCCESS (só nesta máquina). Se o risco do jsoup some: **não medido** até a reanálise da `main` no painel (que exige login).
- **Spring Framework conferido de novo no Maven Central em 11/11/2026:** a linha 6.2 continua parando em 6.2.19 e o Spring Boot 3.5 em 3.5.16. Sem correção na mesma linha, a seção 3 continua valendo. `grep` em `src/` em 11/11/2026: nenhum `XsltView`, `SseEmitter`, `ResponseBodyEmitter` nem `text/event-stream`, o que sustenta o "por que não nos atinge" dos dois críticos.

## 5. Pendências
- [x] Depois do merge deste PR, reabrir o painel (logado) e anotar aqui quantos riscos restam e quais são Critical e High. — feito em 11/11/2026, seção 6.
- [x] No SonarCloud, marcar os dois críticos do Spring como **Accepted** com o motivo da seção 3 (ação manual do Gustavo no painel). — feito em 11/11/2026, seção 6.
- [ ] Reler os riscos restantes que não têm correção e decidir (corrigir, aceitar com motivo ou migrar de linha). Primeiro da fila: o `jaxb-core` 4.0.9 (CVSS 8,3, High), único High aberto.
- [ ] Perguntar ao professor se "0 vulnerabilidades críticas" se mede pelo CVSS das dependências ou pelo Quality Gate/issues. — sem resposta até 11/11/2026; ver a seção 6 e a D4 da seção 9 do plano.

## 6. Painel e D4 em 11/11/2026, noite (Gustavo, logado no SonarCloud)
- **Dependency risks (branch `main`, commit `8b18dd9`):** antes da marcação, 21 riscos em 123 dependências, com 2 Critical (os da seção 3). Depois de marcar os 2 como **Accepted**: **19 riscos abertos, 0 Critical, 1 High** (`org.glassfish.jaxb:jaxb-core` 4.0.9, CVE-2026-92877, CVSS 8,3, escopo runtime, transitivo). O risco do `jsoup` não aparece mais na lista depois do PR #38.
- **Descrição dos CVEs conferida no painel antes de aceitar:** o título do SonarCloud muda (CVE-2026-47884 aparece como "Path Traversal", CWE-22; CVE-2026-47890 como "CRLF Injection", CWE-93), mas a descrição continua sendo a da seção 3 (`XsltView`; SSE com fragmentos de view). `git grep` por `XsltView`, `SseEmitter`, `ResponseBodyEmitter`, `text/event-stream` e `FragmentsRendering` em `src/` da `main` e do PR #44 (back-end): 0 ocorrências. A justificativa de cada aceite no painel cita esta seção e a seção 3.
- **Quality Gate da `main`** (API pública `api/qualitygates/project_status` e `api/measures/component`, lida em 11/11/2026): **OK**; `vulnerabilities` = 0, `security_rating` = A (1.0), `security_hotspots` = 0, `security_hotspots_reviewed` = 100%.
- **Leitura para a D4:** pelas duas medições, não há vulnerabilidade crítica **aberta**: o Quality Gate tem 0 vulnerabilidades, e as dependências têm 0 Critical aberto. A ressalva é que os 2 Critical das dependências estão **aceitos com justificativa, não corrigidos**: se o professor ler "0 críticas" como "0 críticas presentes", o critério não é atendido e o risco residual tem de constar em `docs/decisao-qualidade.md` com impacto, justificativa e responsável.
