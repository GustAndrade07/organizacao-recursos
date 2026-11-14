# Relatório de qualidade — Organização de Recursos

> **Versão 2, de 12/11/2026, noite (pré-entrega).** Escrita pelo Leandro, dono do relatório e da decisão de qualidade (`AGENTS.md`, seção 1).
> - **Estrutura** da Aula 13: cada resultado traz **evidência, interpretação e limite**. O que não foi medido está escrito **"não medido"**, com o motivo.
> - **Números:** valem para o commit da seção 1 e precisam ser refeitos no commit da tag `v1.0.0-rc1` (seção 8).
> - **Versão 1:** 11/11/2026, sobre o commit `e66c000`, quando o back-end ainda não existia; a recomendação era "bloquear".
> - **Atualização de 13/11/2026** (depois dos PRs #59 a #62, `main` em `bc40183`): RF-14 (OpenAPI), BDD com Cucumber e mutação das regras do serviço passaram a existir (PR #61); `verify` local do #61 com **1138 testes, 0 falhas, 0 ignorados** (1116 + 13 BDD + 5 OpenAPI + 4 RN-08); clone limpo rodado pela IA (PR #60). Os números por nível (seção 3), a cobertura e o Quality Gate seguem sendo os de `6ce4478` e **não foram refeitos** (refazer no commit da tag, seção 8).
> - **Versão 3, 13/11/2026, noite (PR de finalização `docs/finalizacao-rc1`, grupo reunido):** números refeitos pela seção 8 sobre o código da `main` `4ab8075` (merge do PR #73; o PR de finalização só muda README, `docker-compose.yml` e documentos): `./mvnw -B verify` local com **1366 testes, 0 falhas, 0 erros, 0 ignorados**, `BUILD SUCCESS` em 5 min 35 s; JaCoCo **96,2% das linhas (1555 de 1617) e 83,7% dos branches (538 de 643)**. Defeitos (seção 5), riscos (seção 6) e ressalvas (seção 7) atualizados. Quality Gate e painel de dependências do SonarCloud: ler no commit da tag (índice de evidências).

## 1. Identificação

| Item | Valor |
|---|---|
| Projeto | Organização de Recursos: reservar salas, professores e materiais sem conflito de horário |
| Disciplina | Qualidade de Software 2026.2, Prof. Afonso, turma STADSCAS4ND |
| Integrantes | Gustavo Oliveira Fernandes de Andrade (banco, CI, SonarCloud, arquitetura) · Pedro Henrique (back-end, PRD) · Leandro Sato (front-end, RTM, relatório, decisão de qualidade) |
| Repositório | https://github.com/GustAndrade07/organizacao-recursos |
| Commit avaliado | Versão 3: código da `main` `4ab8075` (merge do PR #73, 13/11/2026), que recebe a tag `v1.0.0-rc1` junto com o PR de finalização (só README, compose e documentos). Versão 2: `6ce4478`. A carga (seção 4) vem do PR #56, e o código da busca e da criação de reserva não mudou depois dela, a não ser pelo login por e-mail (PR #67) |
| Data | 13/11/2026 (versão 3); 12/11/2026 (versão 2) |
| Ambiente de CI | GitHub Actions, `ubuntu-latest`, Java 21 (Temurin), Maven Wrapper; PostgreSQL 16 em Testcontainers; Chromium pelo Playwright para Java 1.63.0 |
| Pilha | Spring Boot 3.5.16, Spring Security (JWT na API, sessão nas telas), Thymeleaf, Flyway, JUnit 5, Testcontainers, Playwright, JaCoCo, SonarCloud, JMeter 5.6.3 |
| Run de referência | GitHub Actions **43303372662** (run 221, push na `main`, commit `6ce4478`: job `verify` verde) e a análise do SonarCloud do mesmo commit (API pública, 12/11/2026, 20:49 UTC) |

## 2. Escopo e fora de escopo

**Avaliado:** os 30 requisitos do [PRD](prd.md) (RF-01 a RF-14, RN-01 a RN-13, RNF-01 a RNF-03), rastreados na [RTM](rtm.md).

**O que existe no commit avaliado:**
- **Banco:** migração Flyway V1 com a constraint `EXCLUDE USING gist` contra reserva dupla, a trava por recurso, a auditoria, as tentativas recusadas e as consultas do relatório.
- **Back-end** (PR #44): API REST em `/api/**` com JWT, serviço de reserva com a máquina de estados, a auditoria na mesma transação, a tradução de `23P01` em 409, a manutenção e o relatório. A matriz de autorização é lida pelo próprio teste (regra 13).
- **Telas** (PR #48 e #51): as 16 telas em Thymeleaf, ligadas aos mesmos services, com login por sessão, CSRF e perfil por rota (D12, ADR-0009). Limite de tentativas no login das telas e da API (PR #52).
- **Pipeline:** CI com `verify`, gate de cobertura, SonarCloud, checagem de migração imutável e E2E no Chromium.

**Depois de `6ce4478`** (PR #61, 13/11/2026): OpenAPI em `/v3/api-docs` e Swagger UI (RF-14, `OpenApiDocumentacaoTest`), BDD executável com Cucumber (13 cenários no `verify`) e as mutações das regras do serviço.

**O que não existe:**
- RF-11 (notificação externa, sem WireMock);
- localização, patrimônio e competências do professor no banco (V2).

Fonte: [RTM](rtm.md) e as lacunas do `docs/plano-execucao.md`.

**Fora de escopo** (proposta da spec principal, ainda não decidida pelo grupo): front-end separado (SPA), app nativo, reservas recorrentes, SSO, envio real de e-mail e teste de carga fora da máquina local.

## 3. Estratégia de teste por nível

Contagem do `./mvnw -B verify` local de `6ce4478` (12/11/2026), somando o atributo `tests` de cada `target/surefire-reports/TEST-*.xml` pelo pacote e pela base de cada classe. O run 43303372662 roda a mesma suíte.

| Nível | Ferramenta | Onde | Testes | O que prova | Limite |
|---|---|---|---|---|---|
| Unidade | JUnit 5 com `Clock` | `enums`, `model`, `converter`, `service` (contador do login e tradução do `23P01`), `web.sessao`, `controller.handler`, `ConfiguracaoTest` | **117** | Máquina de estados (58 casos, um por par de estados), período semiaberto, ordem das linhas por `recurso_id`, limite de tentativas, tratador de erros | Sem banco: as regras que dependem dele estão nos níveis abaixo |
| Banco | Testcontainers (PostgreSQL 16 real, sem H2), SQL direto | `banco` | **83** em 7 classes | Constraints, sobreposição (recurso e professor), concorrência (20 pedidos), atomicidade da auditoria, tentativa recusada em transação própria, consultas do relatório com valores calculados à mão | Prova o banco, não o serviço |
| API e serviço | MockMvc com token de cada perfil, ou HTTP real, contra PostgreSQL | `controller`, `AuditoriaServiceTest`, `AdministradorInicialTest`, `AplicacaoSmokeTest` | **306** (301 em `controller`) | Matriz de autorização (222 casos, API e telas, falha com rota fora da matriz), regras de reserva com fronteiras, 409 de ponta a ponta, 20 pedidos HTTP → 1 × 201 e 19 × 409, relatório caixa-preta, limite de tentativas | Uma instância; o `MockMvc` não passa pelo Tomcat (o teste de concorrência e o de login das telas passam) |
| Tela com banco ou servidor real | `@SpringBootTest` com a sessão do perfil ou servidor real com CSRF e cookie | `TelasComBancoTest`, `PaginaNaoEncontradaIntegracaoTest`, `LimiteDeTentativasDoLoginIntegracaoTest` | **37** | Cada tela grava e lê do banco de verdade; login, 403, 404, limite de tentativas | Sem navegador: o JavaScript não roda |
| Tela (contrato do HTML) | Recorte `@WebMvcTest` + jsoup, com `Clock` fixo (DEF-002) | 27 classes em `web` | **511** | Rótulos, `data-testid`, estados, escape de HTML, um `h1`, título e idioma em todos os templates | Services dublês; o que vale com dado real está no nível acima |
| Ponta a ponta (E2E) | Playwright para Java, 375 px | `e2e` | **62** em 5 classes | A aplicação real no Chromium contra o back-end real: login, 403, reserva gravada, 409 real, aprovação, cancelamento, bloqueio do login; responsividade de 28 telas e estados | Só 375 px (768 e 1280 px à mão); um navegador |
| Carga | JMeter 5.6.3 (CLI, local) | `perf/` (PR #56) | 2 rodadas: 2102 e 6102 amostras | RNF-01 e RNF-03 (seção 4) | Fora do CI; uma máquina com tudo junto |
| Mutação manual | Roteiro | `docs/testes/mutacao-manual.md`, `docs/frontend/README.md`, `docs/rtm.md` | Banco: M1 a M5. API: M1. Serviço: 8 (MA1, MA2, MM1 a MM3, MZ1, MZ2 e MAU1, PR #61). Tela: E1 a E5 e RN-07. Telas com back-end: 4. Limite de tentativas: 14 | Cada mutação deixou pelo menos um teste vermelho, com uma exceção: conferir o bloqueio antes do hash não é pego (só muda o tempo de resposta). A MAU1 achou a lacuna da auditoria do cancelamento e do não comparecimento, e entraram 4 testes | As seis regras críticas têm mutação. O serviço do relatório foi mutado em 13/11/2026 (MR1 a MR5, PR #70) e D10/D11 na API também (MD10c, MD10d, MD11, PR #71), todas pegas. As do serviço foram medidas só na máquina de quem rodou, sem run do CI |
| BDD | Cucumber 7.23.0 | `src/test/resources/features/aprovacao-bloqueio.feature`, `bdd/` | **13** (PR #61; **não** contados nos 1116 acima) | Aprovação de restrito, auto-aprovação proibida, manutenção com fronteiras exatas e transições de estado, contra a aplicação real com PostgreSQL e relógio fixo | As mutações MA1, MM1 e MM3 deixam cenários vermelhos | Cobre as regras de aprovação e bloqueio, não o sistema todo |
| Integração externa | WireMock | — | **0** | — | Sem notificação (RF-11) |

**Total em `6ce4478`:** **1116 testes, 0 falhas, 0 erros, 0 ignorados** (117 + 83 + 306 + 37 + 511 + 62 = 1116).

**Depois do PR #61** (13/11/2026): **1138 testes, 0 falhas, 0 ignorados** no `verify` local, que são os 1116 mais 13 cenários BDD, 5 de `OpenApiDocumentacaoTest` e 4 de `AuditoriaDeCadaTransicao` (os 5 e os 4 são do nível "API e serviço"; os 306 daquela linha seriam 315). A contagem **por nível não foi refeita**: refazer no commit da tag.

**Versão 3, código de `4ab8075`** (13/11/2026, noite, `verify` local, soma do atributo `tests` dos `TEST-*.xml` de `target/surefire-reports`, como manda a seção 8): **1366 testes, 0 falhas, 0 erros, 0 ignorados**. Por nível: unidade **203** (`enums` 58, `validation` 82, `model` 20, `converter` 13, `dto` 8, `service` sem o `AuditoriaServiceTest` 18, `config` sem o `AdministradorInicialTest` 4); banco **107**; API e serviço **363** (`controller` 358, dos quais 234 em `MatrizAutorizacaoTest`, mais `AuditoriaServiceTest` 2, `AdministradorInicialTest` 2 e `AplicacaoSmokeTest` 1); tela com banco ou servidor real **48** (`TelasComBancoTest` 24, `PoliticasPublicasIntegracaoTest` 11, `LimiteDeTentativasDoLoginIntegracaoTest` 7, `PaginaNaoEncontradaIntegracaoTest` 6); tela, contrato do HTML **555**; E2E **77**; BDD **13**. Soma: 203 + 107 + 363 + 48 + 555 + 77 + 13 = 1366. Os testes novos desde `6ce4478` vieram do #61 (BDD, OpenAPI, RN-08), #66 (políticas), #67 (login por e-mail), #70 (relatório) e #71 (D10/D11). A tabela acima continua com os números de `6ce4478` para comparação.

**Fontes:**
- local: última linha "Tests run" do `./mvnw -B verify` em 12/11/2026 (1116) e a do PR #61 em 13/11/2026 (1138, `docs/status-requisitos.md`);
- CI: run 43303372662 verde, com o mesmo código dos 1116; `ci.yml` verde também nos merges do #61 (run 248) e do #62 (run 251).

## 4. Resultados comparados às metas

| Meta | Resultado | Evidência | Interpretação | Limite | Atingida? |
|---|---|---|---|---|---|
| Cobertura ≥ 80% de linhas e ≥ 70% de branches (RNF-02) | **96,2%** das linhas (1555 de 1617) e **83,7%** dos branches (538 de 643) no código de `4ab8075` (versão 3; em `6ce4478`: 95,8% e 81,3%) | `target/site/jacoco/jacoco.csv` do `verify` local de 13/11/2026, noite ("All coverage checks have been met"); versão 2: `jacoco.csv` de `6ce4478`; "All coverage checks have been met" no run 43303372662. SonarCloud, análise de `6ce4478`: 95,9% das linhas, 81,7% dos branches, 92,3% no total | O código de produção inteiro (back-end e telas) passa pelos testes, com folga nas duas metas | Cobertura diz que a linha rodou, não que o teste confere o resultado; as mutações (seção 3) é que mostram isso, para as regras listadas | **Sim** |
| 100% dos requisitos críticos ligados a teste e run do CI | **2 de 2**: RF-01 e RF-06 `feito` | [RTM](rtm.md), seção "Resumo"; run 43303372662 | A autorização e a reserva dupla têm prova no banco, no serviço, na API e no navegador | A RTM considera crítico o que a spec principal marca (RF-01 e RF-06) | **Sim** |
| Requisitos com teste citado (KPI da spec principal: 14 de 14 RF) | RF: **10 `feito`**, 3 `parcial` (RF-02, 03, 12), 1 `ausente` (RF-11). RN: 13 `feito`. RNF: 3 `feito`. (Em `6ce4478`: 9 `feito`, 2 `ausente` e RN-08 `parcial`; RF-14 e RN-08 passaram a `feito` no PR #61) | [RTM](rtm.md), contagem das 30 linhas, atualizada em 13/11/2026 | 13 dos 14 RF têm teste citado; o único sem teste (notificação) não tem código | "Feito" segue a escala da RTM; decisões pendentes (D8 e D9) não rebaixam a situação quando a proposta está testada | **Não** (13 de 14 com teste; 10 de 14 completos) |
| 0 bugs ou vulnerabilidades críticas | Quality Gate da `main` **OK**: 0 bugs, 0 vulnerabilidades, 0 Security Hotspots, notas A em confiabilidade, segurança e manutenção, 1 code smell. Dependências: 0 Critical **aberto**; **6 riscos aceitos com justificativa**, não corrigidos: os 2 Critical do `spring-webmvc` 6.2.19, o High do `jaxb-core` 4.0.9 e 3 do Spring Security 6.5.11 (1 High, CVSS 7,4; 2 Medium, um deles, o CVE-2026-59276, marcado como "não verificado") | API pública do SonarCloud (`api/qualitygates/project_status` e `api/measures/component`, análise de `6ce4478`, lida em 12/11/2026); `docs/seguranca/riscos-dependencias.md`, seções 6, 7 e 8 (painel lido pelo Gustavo em 11 e 12/11; seção 8 no PR #55) | Pelo Quality Gate, cumprida. Pelo CVSS das dependências, só se "aceito com justificativa" contar: o projeto não usa as funções afetadas (`XsltView`, SSE, desserialização de XML, `Encryptors`; o DPoP é recusado porque o token não tem `cnf.jkt`). O CVE-2026-59276 (comparação com `String.equals()`) foi conferido em 13/11/2026 e **não atinge** (`riscos-dependencias.md`, seção 8.1) | O professor não respondeu; a **D4 foi decidida pelo grupo em 13/11/2026** com a leitura mais rígida: as duas medições separadas e os 2 Critical como risco residual aceito (decisão, risco 1). O painel de dependências exige login e é relido no commit da tag | **Sim, pela D4**, com os 2 Critical aceitos como ressalva |
| Pipeline presente e verde | **Versão 3:** `ci.yml` com `success` nos merges dos PRs #68, #69, #70, #72 e #73 (runs 285, 287, 289, 293 e 295; o 295 é `4ab8075`); o run 291 (merge do #71) foi cancelado pelo push seguinte, sem falha (API do GitHub, 13/11/2026, noite). **Versão 2:** `verify` verde nos **6 últimos pushes** na `main` (runs 207, 209, 211, 217, 219 e 221, de 18:58 a 20:50 UTC de 12/11/2026, desde o merge do PR #51, que corrigiu o DEF-002) e Quality Gate OK | Lista de runs do workflow `ci.yml` na `main`, lida pela API do GitHub em 12/11/2026; ruleset `proteger-main` exige `verify` e `SonarCloud Code Analysis` | O CI barra código quebrado antes do merge. Ele já ficou vermelho de verdade (DEF-002) e foi corrigido no mesmo dia | — | **Sim** |
| Reserva dupla impossível (limitador) | 20 pedidos simultâneos → 1 aceito e 19 recusados no banco **e** 1 × 201 e 19 × 409 pela API, sala e professor, 5 rodadas cada; na carga, 80 horários disputados por 50 usuários com uma reserva cada | `ConcorrenciaReservaBancoTest` e `ConcorrenciaReservaApiTest` no run 43303372662; mutações M1 (24 de 83 testes de banco vermelhos sem a constraint) e M2; `docs/performance/resultado-01.md` | A proteção está no banco e a aplicação traduz a recusa em 409, nunca em 500 | Sem mutação à mão no nível da API. O "9 de 40 rodadas com deadlock sem a trava" é de 06/11/2026 e não foi refeito | **Sim** |
| Sem falha de autorização (limitador) | Matriz com 222 casos (cada perfil × cada rota da API e das telas, anônimo, CSRF); rota fora da matriz é negada; login com limite de tentativas | `MatrizAutorizacaoTest` (`matrizCobreExatamenteAsRotasDoCodigo` falha se surgir rota nova fora da matriz), `LimiteDeTentativasApiTest`, `LimiteDeTentativasDoLoginIntegracaoTest`, `AcessoE2ETest`; mutação "perfil de `GET /aprovacoes` trocado" → 4 falhas | Nenhuma rota fica sem regra, e a posse (dono da reserva) é conferida no serviço | Instância única (contador de tentativas em memória); sem limite por IP; cookie `Secure` só com HTTPS (seção 6) | **Sim** |
| Aplicação executa (limitador) | Sobe contra PostgreSQL real nos testes, subiu para a carga com `java -jar` e o `docker compose` e subiu num clone limpo feito pela IA em 13/11/2026 (`9acbbcd`: `UP`, reserva pela API e pela tela, 409 na sobreposição, `verify` com 1116 testes e 0 falhas) | `AplicacaoSmokeTest` no run 43303372662; `docs/performance/resultado-01.md` (seção "Onde rodou"); `docs/entrega/checklist.md` e seção 4 de `docs/entrega/roteiro-clone-limpo.md` (PR #60) | A aplicação inicia, o Flyway aplica a V1 e o README leva a uma aplicação de pé numa máquina sem nada instalado | **Versão 3:** verificado por uma **pessoa** que não escreveu o README: o Pedro, em 13/11/2026, noite, no Windows, no commit `38cd0ce` (`verify` com 1366 testes, `UP`, reserva gravada e auditada, dois cliques simultâneos → 1 reserva e 1 recusa 409; `docs/entrega/checklist.md`, "Terceira rodada"). Limite: na máquina do Gustavo, com JDK, Docker e cache do Maven já instalados | **Sim** |
| RNF-01: busca com p95 ≤ 300 ms com 50 usuários | p95 = **17 ms** (rodada 01, pico de 48 usuários ativos) e **8 ms** (rodada 02, 50 ativos) | `perf/resultados/rodada-01.jtl` e `rodada-02.jtl`; `docs/performance/resultado-01.md`; meta escrita antes em `docs/performance/rnf.md` (commit `0464847`) | Folga grande para a meta no cenário medido | Só local (4 vCPUs, JMeter, aplicação e banco juntos); poucas salas no banco; duas rodadas; a 02 com a JVM aquecida. Nada sobre produção | **Sim, local** |
| RNF-03: criar reserva sob carga só responde 201 ou 409 | 80 × 201 e 3920 × 409, nenhum outro código, nenhum timeout | Mesmos `.jtl`; banco conferido (80 reservas, 3920 tentativas recusadas) | Sob disputa, a aplicação recusa com 409 e não quebra | Mesmo limite da linha acima | **Sim, local** |
| RF-13: telas responsivas e erros claros | 16 telas com dado real; 28 telas e estados em 375 px sem rolagem horizontal, um `h1`, alvo de toque ≥ 44 px; páginas 401, 403, 404 e 500 sem jargão; 409 real explicado | E2E no run 43303372662; prints no artifact `e2e-375` e em `docs/evidencias/e2e-375/` | A camada de tela cumpre o que promete no celular, com o back-end real | 768 e 1280 px só conferidos à mão; contraste AA de todas as telas **não medido** | **Sim** (375 px) |

## 5. Defeitos por severidade e situação

| Defeito | Severidade | Situação | Correção | Regressão |
|---|---|---|---|---|
| [DEF-001](defeitos/DEF-001.md): Quality Gate da `main` reprovado (Reliability C) por páginas de erro sem `<title>` | Alta (pipeline vermelho é limitador de nota) | **Corrigido** (PR #32, 09/11/2026); Quality Gate da `main` de volta a Passed, conferido na API pública do SonarCloud em 09/11/2026 | `fix(DEF-001): páginas de erro declaram title e lang` | `TemplatesComTituloTest`, que varre todos os templates (regra 10 do `AGENTS.md`) |
| [DEF-002](defeitos/DEF-002.md): teste de tela dependia do relógio real e ficou vermelho depois do horário da reserva de exemplo (12/11/2026, 17:00 UTC) | Alta (`verify` vermelho na `main` e nos PRs é limitador de nota) | **Corrigido** no PR #51, 12/11/2026 | `fix(DEF-002): relógio fixo nos recortes de tela` | Todos os recortes de tela com `Clock` fixo (`support/TelasNoRecorte`), conferido rodando a suíte depois do horário da reserva; regra 14 do `AGENTS.md` |

| [DEF-003](defeitos/DEF-003.md): reserva em data passada é aceita | Média | **Aberto**: o PRD não diz se o Administrador pode registrar uso passado (decisão do grupo) | — | — (dono: Pedro, serviço) |
| [DEF-004](defeitos/DEF-004.md): cadastro de sala pede a localização e descarta o valor | Média | **Aberto**: depende da V3 (fora da rc1, risco 6 da decisão) | — | — (dono: Gustavo, V3; Leandro, tela) |
| [DEF-005](defeitos/DEF-005.md): campos de texto aceitam emoji | Média | **Corrigido em parte** (PR #67): nome e e-mail do cadastro de usuário; faltam os campos de recurso, motivo e manutenção | `feat(green)` do PR #67 | `SemEmojiVarreduraTest` (dono do resto: Pedro / Leandro) |
| [DEF-006](defeitos/DEF-006.md): nome de recurso duplicado é aceito | Baixa a média | **Aberto**: aguarda decisão (nome único?) e exigiria migração com índice único | — | — (dono: Gustavo / grupo) |
| [DEF-007](defeitos/DEF-007.md): retirada e devolução antes do horário da reserva | Baixa a média | **Aberto**: o PRD não fixa a janela; hoje o teste da D10 trava o comportamento atual como decidido (PR #71), e o grupo precisa alinhar os dois | — | — (dono: Pedro / grupo) |
| [DEF-008](defeitos/DEF-008.md): login bloqueado mostra "senha incorreta" | Baixa | **Aberto** (usabilidade; o bloqueio funciona) | — | — (dono: Leandro, tela) |
| [DEF-009](defeitos/DEF-009.md): URL inexistente devolve 403 para quem está logado; `/favicon.ico` dá 404 | Baixa | **Aberto**; a parte (a) é efeito do `denyAll` de propósito (regra 13 do `AGENTS.md`) | — | — (dono: Pedro, segurança; Leandro, tela) |
| [DEF-010](defeitos/DEF-010.md): o login é um nome de usuário e deveria ser um e-mail | Média | **Corrigido** (PR #67, D14 decidida) | `feat(green)` do PR #67 | `UsuarioRequestTest`, `EsquemaBancoTest`, testes de login das telas e da API |

**Totais (13/11/2026, noite):** 10 defeitos registrados (DEF-001 a DEF-010). Alta: 2, os dois corrigidos. Média: 4, sendo 1 corrigido (DEF-010), 1 corrigido em parte (DEF-005) e 2 abertos (DEF-003, DEF-004). Baixa a média e baixa: 4, todos abertos (DEF-006 a DEF-009). **Nenhum aberto toca os quatro limitadores de nota.** Os abertos entram como risco 12 da decisão de qualidade e ficam para a `v1.0.0-rc2`. Fonte: `docs/defeitos/`, campo "Severidade" e "Commit de correção" de cada arquivo.

**Achados que não viraram DEF**, com o motivo:
- **Deadlock (`40P01`) nos 20 pedidos simultâneos (06/11/2026).** Foi achado e corrigido antes do merge da V1, com a trava por recurso. Ficou registrado como decisão D7 e regra 9 do `AGENTS.md`, não como DEF.
- **Clique duplo no envio único (11/11/2026).** O Chromium dispara dois `submit` antes de o `envio-unico.js` travar o botão, mas só um POST sai. O contrato (um POST) se manteve no navegador medido. Em outros navegadores, **não medido**. Registrado em `docs/frontend/README.md`.
- **Print do E2E que falhou no CI (11/11/2026).** No run 43093990942, um caso de `Responsividade375E2ETest` passou nas asserções, mas falhou ao tirar o print de evidência ("Unable to capture screenshot"). Foi corrigido no próprio PR #42 (`3c573fe`): o print espera um quadro desenhado e tenta mais uma vez só para essa falha. É a única intermitência observada no E2E até agora.
- **Dois defeitos do próprio plano JMeter (12/11/2026).** Os ensaios antes da rodada 01 acharam o Bearer vazio no login (401) e os parâmetros da busca sem `=` (400). Foram corrigidos antes de medir e não chegaram a nenhuma rodada (`docs/performance/resultado-01.md`).

**Desvio de processo** (ressalva da revisão do Gustavo no PR #42, 11/11/2026):
- **Telas 7 e 8 sem o `test(red)` antes.** Os testes `MinhasReservasPaginaTest` e `ReservaDetalhePaginaTest` entraram no mesmo commit que o código das telas: `b58380d`, que é `a81922a` depois da reescrita de autoria. Não houve um commit vermelho antes.
- **Motivo:** a regra 11 do `AGENTS.md` exige o OK explícito do dono da área antes do commit vermelho, e esse OK não foi pedido.
- **Evidência que existe:** a mutação manual de RN-07 (tirar `reserva.iniciada == false` das duas telas) deixou 5 testes vermelhos, e o código foi restaurado.
- **Limite:** não há prova de que esses testes falhavam antes de as telas existirem, porque o `git log` não tem o vermelho antes do verde. O E2E não é TDD (foi escrito sobre telas que já existiam). Para as outras telas e para os testes de banco, este relatório não conferiu, commit a commit, se o vermelho veio antes.

**Desvio de processo** (ressalva 3 da revisão do Gustavo no PR #48, 12/11/2026): a ligação das telas ao back-end (commit `fa94726`) trouxe código e testes juntos, sem um commit `test(red)` antes, pelo mesmo motivo. Evidência que existe: 4 mutações feitas à mão, todas pegas (lista na RTM). O mesmo vale para a tela 16 (usuários).

**Desvio de processo** (pendência da aprovação do Gustavo no PR #52, 12/11/2026): os 5 commits do PR #52 (limite de tentativas) saíram com o autor "Claude", e não com o nome do Leandro.
- **Causa:** o `git config user.name` da máquina de nuvem onde a IA trabalhou era "Claude".
- **Por que não foi corrigido:** o PR foi mergeado logo depois da aprovação. Corrigir agora exigiria reescrever a `main` com push forçado, o que o ruleset `proteger-main` impede e o `AGENTS.md` proíbe.
- **O que vale como registro:** os commits são do Leandro, feitos com a IA, e trazem a linha `Co-Authored-By: Claude` que registra isso.
- **Depois:** a partir do PR #56, os commits saem com o autor "Leandro Sato".

**Exemplo de TDD com o vermelho antes do verde** (limite de tentativas no login das telas, 12/11/2026, ressalva 1 do PR #48):
- **`test(red)`:** só `LimiteDeTentativasDoLoginIntegracaoTest`, com o OK do Leandro (dono da área) pedido antes, pela regra 11. O `./mvnw -B verify` deu 1092 testes, 4 falhas, 0 erros e 0 ignorados. As 4 falhas são do teste novo, todas porque a senha certa entrava (302 para `/`) quando o login deveria estar bloqueado. O teste compila; o vermelho veio da regra que ainda não existia.
- **`feat(green)`:** a implementação e os testes de unidade (`TentativasDeLoginTest`, `LoginComLimiteTest`), com o `verify` verde.
- **Mutações:** 7 feitas à mão, todas pegas (lista na RTM).
- **Limite:** os testes de unidade entraram junto com o código, no `feat(green)`, porque uma classe nova num teste do commit vermelho impede a compilação, e aí não sai número de teste. Isso foi combinado antes.
- **Segundo ciclo, a API** (mesmo PR): `test(red)` só com `LimiteDeTentativasApiTest`, com 1113 testes, 5 falhas, 0 erros e 0 ignorados, todas porque o login bloqueado ainda entrava. Depois, `feat(green)` com o contador compartilhado e 4 mutações, todas pegas. A API é área do Pedro; o Leandro autorizou a mudança, e o Pedro ainda precisa confirmar.

**Limite:** defeito achado e corrigido dentro do próprio PR, antes do merge, não foi registrado como DEF. A contagem acima é a do que chegou à `main` ou foi registrado.

## 6. Riscos residuais

| Risco | Impacto | Situação em 12/11/2026 | Dono | O que reduz o risco |
|---|---|---|---|---|
| Aplicação não executa em clone limpo | **Zera a nota** (limitador) | **Fechado em 13/11/2026, noite:** clone limpo humano do Pedro passou sem bloqueador (checklist, "Terceira rodada") | Pedro | Executar `docs/entrega/roteiro-clone-limpo.md` antes da tag, seguindo só o README (no Windows, com o comando do PowerShell), e anotar o que faltou em `docs/entrega/checklist.md` |
| 6 riscos de dependências aceitos: 2 Critical (`spring-webmvc` 6.2.19), 2 High (`jaxb-core` 4.0.9; `spring-security-oauth2-jose` 6.5.11) e 2 Medium (Spring Security 6.5.11) | Os Critical podem contar como "vulnerabilidade crítica" se o professor não aceitar a justificativa | Aceitos com justificativa e marcados "Accepted" no SonarCloud (`docs/seguranca/riscos-dependencias.md`, seções 3, 7 e 8); sem versão corrigida na mesma linha (Spring Security só na 7.x, que exige o Spring Boot 4). **D4 decidida pelo grupo em 13/11/2026** (leitura mais rígida, sem resposta do professor). O CVE-2026-59276 foi conferido em 13/11/2026: **não atinge** (seção 8.1) | Gustavo | Subir a versão assim que houver correção (Spring Framework 6.2.20 ou migração para o Boot 4) |
| Decisões pendentes D8 e D9 (D10 e D11 foram decididas no PR #58: "fica como está no código") | O comportamento testado é a proposta, não uma decisão do grupo; o professor pode discordar da regra. D10 (c), D10 (d) e D11 na API ganharam teste em 13/11/2026 (PR #71) | O código segue a proposta e os testes provam essa proposta | Grupo (Pedro propõe) | D8 e D9 ficam como premissa registrada na decisão de qualidade (risco 7) |
| RF-11 sem entrega | 1 dos 14 RF `ausente` (o RF-14 ficou `feito` no PR #61) | Sem código | Pedro | Entrar como risco aceito na decisão de qualidade (riscos, item 5), ou entregar depois da tag (`v1.0.0-rc2`) |
| RF-02 e RF-03 sem localização e competência | Busca sem dois filtros do PRD; campos na tela que não são gravados (DEF-004) | Migração de localização (V3; a V2 virou o login por e-mail, PR #67) fora da rc1, decidido pelo Gustavo em 13/11/2026 | Gustavo / Pedro | Migração V3 numa `v1.0.0-rc2`, ou tirar os campos da tela |
| Força bruta no login | Senha adivinhada por tentativa e erro | **Telas e API, com um contador só:** 5 falhas do mesmo login em 15 minutos bloqueiam esse login por 15 minutos (ADR-0009). Continua aberto: (a) contador na memória de uma instância só; (b) sem limite por IP; (c) quem sabe um login pode bloqueá-lo por 15 minutos; (d) tempo de resposta igual no bloqueio não medido | Leandro (telas) / Pedro (API) | Aceitos para a entrega; contador compartilhado (banco ou cache) só se houver mais de uma instância |
| Cookie de sessão sem `Secure` em produção | Sessão exposta se a aplicação rodar em HTTP fora da máquina local | `HttpOnly` e `SameSite=Lax` ligados; `Secure` depende de `SESSAO_COOKIE_SEGURO=true`, que exige HTTPS | Gustavo (ambiente) | Ligar a variável onde houver HTTPS |
| Desempenho medido só localmente | O número não vale para outra máquina | Duas rodadas, uma máquina com tudo junto | Leandro | Repetir na máquina da apresentação, se houver tempo; senão, manter o limite escrito |
| ~~Serviço do relatório sem mutação registrada~~ | — | **Fechado em 13/11/2026** (PR #70): MR1 a MR5, todas pegas; a MR4 achou uma lacuna e entrou `RelatorioInstantaneoTest` | Pedro | — |
| Defeitos abertos (DEF-003, DEF-004, DEF-006 a DEF-009; DEF-005 em parte) | Dado incoerente, campo não gravado, nomes repetidos, usabilidade; nenhum nos limitadores | Registrados, sem correção (seção 5) | Dono de cada um na seção 5 | Decisões do grupo e `v1.0.0-rc2` |
| Prazo | Itens `ausente` na entrega | Entrega em 13/11/2026 | Grupo | O que ficar de fora entra como risco aceito, com responsável, na decisão de qualidade |

## 7. Recomendação

> **Confirmada pelo Leandro** (dono da decisão) em 12/11/2026, noite. A decisão formal, com critérios, riscos aceitos e quem fez o quê, está em [`decisao-qualidade.md`](decisao-qualidade.md) (lacuna 2, PR #59).

**Recomendação deste relatório: liberar `v1.0.0-rc1` com ressalvas.** A condição da versão 2 (clone limpo antes da tag) foi cumprida pelo Pedro em 13/11/2026, noite.

O critério são os quatro limitadores de nota do `AGENTS.md` (seção 2):
- **Pipeline verde:** atendido (run 43303372662 e Quality Gate OK).
- **Reserva dupla:** atendido no banco, na API e sob carga.
- **Falha de autorização:** atendido; a matriz de autorização cobre todas as rotas do código e falha se surgir uma nova.
- **Aplicação executa:** atendido: sobe nos testes, com `java -jar`, no clone limpo da IA e no **clone limpo humano do Pedro** (13/11/2026, noite, checklist, "Terceira rodada").

**Ressalvas que a liberação carrega**, cada uma com evidência ou com "não medido" na seção 4:
1. RF-11 (notificação) ausente. O RF-14 (OpenAPI) ficou `feito` no PR #61.
2. RF-02, RF-03 e RF-12 parciais: sem localização e competência (V2); premissas do relatório pendentes (D8).
3. Comportamento de D8 e D9 é proposta testada, não decisão do grupo; D10 e D11 foram decididas no PR #58 e têm teste na API desde o PR #71.
4. Riscos de dependências aceitos com justificativa (2 Critical do `spring-webmvc`), pela D4 decidida pelo grupo (leitura mais rígida); o CVE-2026-59276 foi conferido e não atinge.
5. Desempenho medido só localmente.
6. Seis defeitos abertos de severidade média ou baixa (DEF-003, DEF-004, DEF-006 a DEF-009), nenhum nos limitadores.

**O que mudaria a recomendação para "bloquear":** clone limpo que não sobe; CI vermelho no commit da tag; resposta da D4 dizendo que "aceito com justificativa" não vale.

## 8. Como refazer os números

No commit da tag, refazer cada linha da seção 4 e trocar o commit e o run da seção 1:
- **Testes por nível:** somar o atributo `tests` dos `target/surefire-reports/TEST-*.xml`, agrupando como na seção 3. O total é a última linha `Tests run` do `verify` (no CI, o passo "Build, testes e cobertura").
- **Cobertura:** em `target/site/jacoco/jacoco.csv`, somar as colunas `LINE_COVERED`/`LINE_MISSED` e `BRANCH_COVERED`/`BRANCH_MISSED` (artifact `relatorios` do CI ou `verify` local). No SonarCloud, ler `api/measures/component?component=GustAndrade07_organizacao-recursos&branch=main&metricKeys=line_coverage,branch_coverage,bugs,vulnerabilities,security_hotspots` e conferir que a análise é do commit da tag (`api/project_analyses/search`).
- **RTM:** recontar as colunas "Situação" de `docs/rtm.md` e trocar o run.
- **Dependências:** reler o painel "Dependency risks" do SonarCloud (exige login) e atualizar `docs/seguranca/riscos-dependencias.md`.
- **Desempenho:** `perf/resultados/rodada-01.jtl` e `rodada-02.jtl`, com a leitura em `docs/performance/resultado-01.md`. Uma rodada nova só se o código da busca ou da criação mudar.
