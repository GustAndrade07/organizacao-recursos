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
- [ ] Reler os riscos restantes que não têm correção e decidir (corrigir, aceitar com motivo ou migrar de linha). Primeiro da fila: o `jaxb-core` 4.0.9 (CVSS 8,3, High), único High aberto. — `jaxb-core` avaliado em 11/11/2026: aceito com justificativa (seção 7). Faltam os riscos sem nota (CVSS "Unscored (pending)"), entre eles os 3 novos do Spring Security 6.5.11 que entraram com o back-end (PR #44).
- [x] No SonarCloud, marcar o CVE-2026-92877 (`jaxb-core`) como **Accepted** com o motivo da seção 7 (ação manual do Gustavo no painel). — feito em 12/11/2026, depois do merge do PR #49, e conferido no filtro "Accepted" do painel. Na mesma leitura o painel mostrava esse risco com severidade de impacto "Low" (CVSS 8,3 inalterado).
- [ ] Perguntar ao professor se "0 vulnerabilidades críticas" se mede pelo CVSS das dependências ou pelo Quality Gate/issues. — sem resposta até 11/11/2026; ver a seção 6 e a D4 da seção 9 do plano.

## 6. Painel e D4 em 11/11/2026, noite (Gustavo, logado no SonarCloud)
- **Dependency risks (branch `main`, commit `8b18dd9`):** antes da marcação, 21 riscos em 123 dependências, com 2 Critical (os da seção 3). Depois de marcar os 2 como **Accepted**: **19 riscos abertos, 0 Critical, 1 High** (`org.glassfish.jaxb:jaxb-core` 4.0.9, CVE-2026-92877, CVSS 8,3, escopo runtime, transitivo). O risco do `jsoup` não aparece mais na lista depois do PR #38.
- **Descrição dos CVEs conferida no painel antes de aceitar:** o título do SonarCloud muda (CVE-2026-47884 aparece como "Path Traversal", CWE-22; CVE-2026-47890 como "CRLF Injection", CWE-93), mas a descrição continua sendo a da seção 3 (`XsltView`; SSE com fragmentos de view). `git grep` por `XsltView`, `SseEmitter`, `ResponseBodyEmitter`, `text/event-stream` e `FragmentsRendering` em `src/` da `main` e do PR #44 (back-end): 0 ocorrências. A justificativa de cada aceite no painel cita esta seção e a seção 3.
- **Quality Gate da `main`** (API pública `api/qualitygates/project_status` e `api/measures/component`, lida em 11/11/2026): **OK**; `vulnerabilities` = 0, `security_rating` = A (1.0), `security_hotspots` = 0, `security_hotspots_reviewed` = 100%.
- **Leitura para a D4:** pelas duas medições, não há vulnerabilidade crítica **aberta**: o Quality Gate tem 0 vulnerabilidades, e as dependências têm 0 Critical aberto. A ressalva é que os 2 Critical das dependências estão **aceitos com justificativa, não corrigidos**: se o professor ler "0 críticas" como "0 críticas presentes", o critério não é atendido e o risco residual tem de constar em `docs/decisao-qualidade.md` com impacto, justificativa e responsável.

## 7. `jaxb-core` 4.0.9 (CVE-2026-92877) em 11/11/2026, noite (Gustavo)
- **O que o CVE descreve (painel do SonarCloud, logado, publicado em 12/10/2026):** XXE (CWE-611, CVSS 8,3 High; EPSS 0,265%, fora da lista KEV). As fábricas do JAXB confiavam só em `FEATURE_SECURE_PROCESSING`; em implementações de JAXP em que isso não basta, quem consegue fazer a aplicação **desserializar XML não confiável pelo JAXB** pode ler arquivos locais. O painel não mostra versão corrigida ("How can I fix it?": versão em uso 4.0.9, sem dados de correção) e o "Reachability" está "Not analyzed".
- **Versão corrigida:** não existe. `maven-metadata.xml` do Maven Central lido em 11/11/2026: a última versão publicada de `org.glassfish.jaxb:jaxb-core` é a **4.0.9** (atualizado em 28/05/2026), a que usamos.
- **Por onde entra:** transitivo, escopo runtime: `spring-boot-starter-data-jpa` 3.5.16 → `hibernate-core` 6.6.53.Final → `jaxb-runtime` 4.0.9 → `jaxb-core` 4.0.9 (`.\mvnw.cmd -B dependency:tree`, 11/11/2026). O Hibernate usa o JAXB para ler os próprios arquivos de mapeamento XML (`orm.xml`, `hbm.xml`).
- **Por que não nos atinge:** o projeto não desserializa XML de ninguém. `git grep` em `src/` e no `pom.xml` (11/11/2026, `main` em `19f5e31`) por `jakarta.xml.bind`, `javax.xml.bind`, `Unmarshaller`, `JAXBContext`, `@XmlRootElement`, `@XmlElement`, `application/xml`, `text/xml`, `APPLICATION_XML`, `jackson-dataformat-xml`, `XmlMapper`, `DocumentBuilder`, `SAXParser` e `XMLInputFactory`: **0 ocorrências**; nenhum `consumes`/`produces` nos controllers; nenhum arquivo `.xml` em `src/main/resources` (sem `orm.xml` nem `hbm.xml`). A API recebe e devolve só JSON, e as telas recebem formulários.
- **Decisão:** **aceito com justificativa**, responsável Gustavo. Revisão: se sair `jaxb-core` 4.0.10 ou superior (subir por `glassfish-jaxb.version` no `pom.xml`), ou se alguma rota passar a aceitar XML.
- **Alternativa não adotada:** excluir `jaxb-runtime` do `hibernate-core` tiraria a biblioteca do runtime, mas mexe na subida do Hibernate a dois dias da entrega. Não foi testada (não medido).
- **Painel na mesma leitura (branch `main`, análise mais recente; commit da análise não conferido):** **22 riscos abertos em 141 dependências**, 0 Critical, 1 High (este), 4 Medium (`commons-compress` 1.24.0 ×2 e `json-smart` 2.5.2, todos de teste; `spring-data-jpa` 3.5.13), 3 Low e 14 sem nota ("Unscored (pending)"). Subiu de 19 em 123 (seção 6) porque o back-end (PR #44) trouxe o Spring Security 6.5.11: 3 riscos novos, ainda sem nota (`spring-security-oauth2-jose` CVE-2026-41707, `spring-security-crypto` CVE-2026-47842, `spring-security-core` CVE-2026-59276). Esses 3 **não foram avaliados**.

## 8. Spring Security 6.5.11: 3 riscos avaliados em 12/11/2026 (Gustavo)

**Fontes:**
- Painel "Dependency risks" do SonarCloud, logado, branch `main`, lido em 12/11/2026: abas "What's the risk?", "Where is it?" e "How can I fix it?".
- `maven-metadata.xml` de `org.springframework.security:spring-security-core` no Maven Central, lido em 12/11/2026 (`lastUpdated` 22/10/2026).
- Bytecode dos jars 6.5.11 do repositório Maven local (`javap`).
- `git grep` em `src/` na `main` (`2610d6a`).

**Por onde entram:** os três são transitivos, de escopo compile, por `spring-boot-starter-oauth2-resource-server` 3.5.16 → Spring Security 6.5.11. A API usa esse starter para o JWT (ADR-0001).

**Versão corrigida:** só na linha 7.x (a mais próxima é a 7.0.7; a mais nova, a 7.1.2). No Maven Central, a última 6.5.x publicada é a **6.5.11**, a que usamos. Subir para a 7.x exige o Spring Boot 4 (Spring Framework 7), uma migração grande demais para a véspera da entrega (não testada).

| CVE | Pacote | O que é | CVSS | EPSS | Por que não nos atinge (ou o que falta saber) |
|---|---|---|---|---|---|
| CVE-2026-41707 | `spring-security-oauth2-jose` | Replay de prova DPoP: o cache de `jti` do `DPoPProofJwtDecoderFactory` tem tamanho fixo, e quem inunda o servidor consegue expulsar entradas e reusar uma prova DPoP interceptada (CWE-294) | 7,4 High | 0,39% | **Não atinge.** Detalhe abaixo da tabela. |
| CVE-2026-47842 | `spring-security-crypto` | `AesBytesEncryptor` com o construtor de dois argumentos (ou gerador de IV nulo) em CBC cifra com vetor de inicialização zerado (CWE-326) | 6,5 Medium | 0,15% | **Não atinge.** O projeto não usa `Encryptors` nem nenhum `*BytesEncryptor`/`TextEncryptor` (0 ocorrências em `src/`). Do `crypto`, usa só o `BCryptPasswordEncoder`. |
| CVE-2026-59276 | `spring-security-core` | Comparações de valores sensíveis com `String.equals()`, que termina no primeiro caractere diferente e deixa o tempo de resposta vazar quanto do valor bate (CWE-208) | 5,9 Medium | 0,33% | ~~Provavelmente não atinge; não verificado.~~ **Não atinge** (verificado em 13/11/2026 no advisory oficial, seção 8.1). |

**Por que o CVE-2026-41707 não nos atinge:**
- O suporte a DPoP liga **sozinho** no resource server: o `OAuth2ResourceServerConfigurer` 6.5.11 aplica o `DPoPAuthenticationConfigurer` quando a classe `DPoPProofJwtDecoderFactory` está no classpath, o que foi conferido no bytecode. Ou seja, o código vulnerável roda.
- Mas o `DPoPAuthenticationProvider` só aceita um token de acesso que tenha o claim `cnf.jkt` ("jkt claim is required"), e o nosso `TokenService` emite JWT HS256 sem `cnf`.
- Resultado: toda requisição DPoP é recusada antes de o replay importar, e não existe prova DPoP válida emitida para ser interceptada.

**O que falta saber sobre o CVE-2026-59276:**
- A descrição não diz quais componentes do Spring Security fazem a comparação vulnerável.
- O que usamos de sensível:
  - o token CSRF das telas;
  - a assinatura HS256 do JWT, verificada pela biblioteca Nimbus, não pelo Spring;
  - o BCrypt.
- **Não foi conferido** se algum desses passa pelo código afetado. Um ataque de tempo pela rede, com a variação normal de latência, é difícil, o que combina com a nota Medium.

Nenhum dos três está na lista KEV de exploração conhecida. Na mesma leitura, o painel mostrava os três com severidade de impacto "Low" e "Reachability: Not analyzed".

**Decisão:** os três ficam **aceitos com justificativa**.
- Responsável: Gustavo.
- Revisão: quando sair uma 6.5.12 (o Spring Boot 3.5.x a traz pelo BOM) ou quando o projeto migrar para o Spring Boot 4.
- O CVE-2026-59276 fica marcado como **não verificado** até alguém conferir os componentes afetados. — **conferido em 13/11/2026: não atinge** (seção 8.1).
- Marcados como **Accepted** no painel do SonarCloud em 12/11/2026, cada um com o motivo desta seção, e conferidos no filtro "Accepted". O filtro mostrava 6 riscos aceitos: os 2 do Spring Framework (seção 6), o `jaxb-core` (seção 7) e estes 3.
- Esses três riscos entram como risco residual em `docs/decisao-qualidade.md`, do Leandro.

### 8.1 CVE-2026-59276 conferido no advisory oficial em 13/11/2026 (Gustavo)

**Veredito: não atinge o projeto.** Sai de "não verificado" para "verificado: não atinge".

**Fontes (todas lidas em 13/11/2026):**
- Advisory oficial do Spring: https://spring.io/security/cve-2026-59276 (publicado em 20/08/2026, severidade Medium, vetor CVSS 3.1 `AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N`).
- NVD (`services.nvd.nist.gov/rest/json/cves/2.0?cveId=CVE-2026-59276`, situação "Analyzed"): 6.5.0 a 6.5.11 afetadas, correção a partir da 6.5.12.
- OSV (`api.osv.dev/v1/vulns/CVE-2026-59276`): mesmas faixas, referência ao mesmo advisory.
- `git grep` em `src/` e no `pom.xml` na `main` `bc40183`, e `dependency:tree` do mesmo commit.

**Componentes afetados, segundo o advisory, e o que o projeto usa:**

| Componente afetado | Condição para ser atingido (advisory) | No projeto |
|---|---|---|
| `DigestAuthenticationFilter` | Usar autenticação HTTP Digest | Não usa: API com JWT Bearer e telas com formulário (`SegurancaConfig`, `SegurancaDasTelasConfig`) |
| `KeyBasedPersistenceTokenService` | Usar essa classe (nenhum componente do Spring Security a liga por padrão) | Não usa |
| `BalloonHashingPassword4jPasswordEncoder` e `Pbkdf2Password4jPasswordEncoder` | Verificar senha com esses codificadores | Não usa: o único codificador é o `BCryptPasswordEncoder` (`SegurancaConfig`, linha 160); a biblioteca Password4j nem está no `dependency:tree` |
| `InMemoryOAuth2AuthorizationService` | Rodar um servidor de autorização OAuth2 com o serviço em memória | Não usa: o projeto é só *resource server*; `spring-security-oauth2-authorization-server` não está no `dependency:tree` |

`git grep -i -E 'DigestAuthentication|KeyBasedPersistenceTokenService|Password4j|BalloonHashing|Pbkdf2|InMemoryOAuth2AuthorizationService|authorization-server|httpDigest'` em `src/` e no `pom.xml`: **0 ocorrências**. Os três valores sensíveis que a seção 8 citava (token CSRF, assinatura HS256 verificada pela Nimbus, BCrypt) **não** estão na lista de componentes afetados.

**Versão corrigida:** a 6.5.12 existe, mas o advisory a marca como **"Enterprise Support only"**; no Maven Central a última 6.5.x continua sendo a 6.5.11 (`maven-metadata.xml` de `spring-security-core` lido em 13/11/2026). As correções abertas (OSS) são 7.0.7 e 7.1.1, que exigem o Spring Boot 4. A decisão da seção 8 (aceito com justificativa, responsável Gustavo) continua, agora com o motivo "componente afetado não usado" em vez de "provavelmente não atinge".

**Painel atualizado em 13/11/2026 (logado):** o risco continua **Accepted**, com um comentário novo que registra o veredito "verificado, não atinge" e aponta para esta seção. O comentário de 12/11 ("não verificado") ficou no histórico; o painel não abriu a edição dele na largura usada.

## 9. Dependências novas desde o último registro (13/11/2026, Gustavo)

Comparação do `pom.xml` da `main` `bc40183` com o que este documento citava até a seção 8: entraram o springdoc (RF-14), o Cucumber com o `junit-platform-suite` (BDD), o Playwright (E2E) e o override do `commons-lang3`. Nenhuma delas aparecia aqui.

**Como foi verificado (13/11/2026):**
- Versões e escopo: `.\mvnw.cmd -B dependency:tree` na `main` `bc40183`.
- CVE conhecido: consulta à base pública **OSV** (`api.osv.dev/v1/query`, que agrega GitHub Security Advisories e NVD), pacote a pacote, com a versão exata resolvida. Para o CVE do `commons-lang3`, também o registro do **NVD**.
- Versão mais nova: `maven-metadata.xml` de cada artefato no **Maven Central**.
- Painel "Dependency risks" do SonarCloud, logado, branch `main`, lido em 13/11/2026 (a API sem login respondeu 401): **54 riscos em 174 dependências**, e **nenhum** é das dependências desta seção (0 menções a `commons-lang3`, springdoc, swagger, webjars, Cucumber, gherkin, Playwright ou `junit-platform`).

| Dependência (versão resolvida) | Escopo | Motivo | CVE conhecido (OSV, 13/11/2026) | Observação |
|---|---|---|---|---|
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` 2.8.17 (e `-webmvc-api`, `-common` 2.8.17) | **compile** (vai no jar e roda em produção) | RF-14: documentação OpenAPI gerada do código, `/v3/api-docs` e `/swagger-ui` (PR #61) | Nenhum | 2.8.17 é a última da linha 2.8 (Spring Boot 3.5) no Maven Central; a 3.x é a linha do Spring Boot 4. O Swagger exige sessão (`docs/seguranca/matriz-autorizacao.md`) |
| ↳ transitivos: `io.swagger.core.v3:swagger-core-jakarta` 2.2.47 (e `-models`, `-annotations`), `org.webjars:swagger-ui` 5.32.2, `org.webjars:webjars-locator-lite` 1.1.3 | compile | Trazidos pelo springdoc | Nenhum (também conferido o pacote npm `swagger-ui` 5.32.2) | — |
| `org.apache.commons:commons-lang3` **3.22.0** (override por `commons-lang3.version`) | compile | Trazido pelo `swagger-core-jakarta`. O Spring Boot 3.5.16 gerencia a **3.17.0**, que tem o **CVE-2025-48924** (GHSA-j288-q9x7-2f5v): recursão sem limite em `ClassUtils.getClass(...)`, que pode lançar `StackOverflowError` com entrada muito longa; NVD: CVSS 3.1 5,3 Medium (`AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:L`), corrigido na 3.18.0 | 3.22.0: nenhum. 3.17.0: CVE-2025-48924 | Subida na mesma linha 3.x (regra 5 do `AGENTS.md`), PR #61, depois de o SonarCloud acusar "1 new dependency risk". 3.22.0 é a última no Maven Central (13/11/2026) |
| `io.cucumber:cucumber-java`, `cucumber-spring`, `cucumber-junit-platform-engine` 7.23.0, com o `io.cucumber:cucumber-bom` 7.23.0 no `dependencyManagement` | **test** (não vai no jar) | BDD executável: `aprovacao-bloqueio.feature` roda no `verify` (PR #61) | Nenhum (também conferidos `cucumber-core` 7.23.0, `gherkin` 32.1.1, `messages` 27.2.0, `html-formatter` 21.10.1) | Fica na 7.23.0 de propósito: da 7.26 em diante o engine exige JUnit Platform 1.13 e o Spring Boot 3.5.16 gerencia a 1.12.2 (comentário no `pom.xml`). A última no Maven Central é a 8.0.4 |
| `org.junit.platform:junit-platform-suite` 1.12.2 (versão do Spring Boot) | test | Suíte JUnit que dispara os cenários do Cucumber | Nenhum (também `junit-platform-suite-engine` 1.12.2) | — |
| `com.microsoft.playwright:playwright` 1.63.0 (e `driver`, `driver-bundle` 1.63.0) | test | E2E das telas em navegador real | Nenhum | A última no Maven Central é a 1.65.0 (12/11/2026); não há CVE que justifique subir a esta altura |

**Leitura:** só uma dependência nova vai para produção com risco conhecido na versão gerenciada pelo Boot, o `commons-lang3`, e ela já está corrigida pelo override. As de escopo test não entram no jar. Nenhum CVE foi inventado: onde a OSV não devolve nada, a coluna diz "Nenhum", com a data da consulta, e isso não garante que o SonarCloud não acuse algo que a OSV ainda não tem.

**Painel, mesma leitura (13/11/2026):** nada a marcar para as dependências desta seção. Seguem **abertos** riscos que não são delas: `commons-compress` 1.24.0 (2, test), `json-smart` 2.5.2 (test), `spring-web`, `spring-webmvc`, `spring-beans` e `spring-expression` 6.2.19 (vários, quase todos "Unscored (pending)"), `HdrHistogram` e `LatencyUtils` (Low, runtime) e um novo no `logback-classic` 1.5.38 (CVE-2026-104721, "Unscored (pending)"). Não foram avaliados neste PR; ficam para a próxima leitura (seção 5). Lidos 52 dos 54 itens da lista; 2 não carregaram na página.
