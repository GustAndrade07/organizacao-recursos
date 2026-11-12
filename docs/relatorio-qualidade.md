# Relatório de qualidade — Organização de Recursos

> **Versão 1, de 11/11/2026 (pré-entrega).** Escrita pelo Leandro (dono do relatório e da decisão de qualidade, `AGENTS.md` seção 1). Estrutura da Aula 13: cada resultado traz **evidência, interpretação e limite**, e o que não foi medido está escrito **"não medido"**, com o motivo. Os números valem para o commit da seção 1 e precisam ser refeitos no commit da tag `v1.0.0-rc1` (seção 8).

## 1. Identificação

| Item | Valor |
|---|---|
| Projeto | Organização de Recursos: reservar salas, professores e materiais sem conflito de horário |
| Disciplina | Qualidade de Software 2026.2, Prof. Afonso, turma STADSCAS4ND |
| Integrantes | Gustavo Oliveira Fernandes de Andrade (banco, CI, SonarCloud, arquitetura) · Pedro Henrique (back-end, PRD) · Leandro Sato (front-end, RTM, relatório, decisão de qualidade) |
| Repositório | https://github.com/GustAndrade07/organizacao-recursos |
| Commit avaliado | `e66c000` (hash de antes da reescrita de autoria de 11/11/2026; mesmo conteúdo que `8b5ec44`, correspondência em `docs/status/2026-11-11-satoworktree-eloquent-turing-f9j5ey.md`), ponta do PR #42 (branch `satoworktree/eloquent-turing-f9j5ey`). Contém a `main` até `8b18dd9` mais as telas 7 e 8, o E2E e a RTM. **Ainda não está na `main`** |
| Data | 11/11/2026 |
| Ambiente de CI | GitHub Actions, `ubuntu-latest`, Java 21 (Temurin), Maven Wrapper; PostgreSQL 16 em Testcontainers; Chromium 1243 pelo Playwright para Java 1.63.0 |
| Pilha | Spring Boot 3.5.16, Thymeleaf, Flyway, JUnit 5, Testcontainers, Playwright, JaCoCo, SonarCloud |
| Run de referência | GitHub Actions **43091027823** (commit `3d63d72`, a mesma suíte do `e66c000`, cujos commits seguintes só mudaram documentação) e **43092312730** (commit `e66c000`, job `verify` e SonarCloud verdes) |

## 2. Escopo e fora de escopo

**Avaliado:** os 30 requisitos do [PRD](prd.md) (RF-01 a RF-14, RN-01 a RN-13, RNF-01 a RNF-03), rastreados na [RTM](rtm.md).

**O que existe no commit avaliado:**
- **Banco:** migração Flyway V1, com a constraint `EXCLUDE USING gist` contra reserva dupla, a trava por recurso, a auditoria e as tentativas recusadas; consultas do relatório RF-12 em `src/main/resources/db/consultas/`.
- **Telas:** as 15 telas do inventário em Thymeleaf (`docs/frontend/telas.md`), com os controllers que só devolvem a view.
- **Pipeline:** CI com `verify`, gate de cobertura, SonarCloud e checagem de migração imutável.

**O que não existe (e por isso não foi avaliado):** o back-end. Não há serviço de reserva, API REST, autenticação JWT, matriz de autorização, máquina de estados, serviço de auditoria, endpoint do relatório, notificação externa nem OpenAPI. Fonte: `docs/status-requisitos.md` e a spec principal do Notion, onde o Pedro é o dono.

**Fora de escopo (proposta da spec principal, ainda não decidida pelo grupo):** front-end separado (SPA), app nativo, reservas recorrentes, SSO, envio real de e-mail e teste de carga fora da máquina local.

## 3. Estratégia de teste por nível

| Nível | Ferramenta | Onde | Testes no CI (run 43091027823) | O que prova | Limite |
|---|---|---|---|---|---|
| Unidade (regra de domínio) | JUnit 5 com `Clock` | — | **0** | — | Não há código de domínio: o serviço do back-end não existe |
| Banco e integração | Testcontainers (PostgreSQL 16 real, sem H2) | `src/test/java/.../banco` | **83** em 7 classes | Constraints, sobreposição (recurso e professor), concorrência (20 pedidos), atomicidade da auditoria, tentativa recusada em transação própria, consultas do relatório com valores calculados à mão | SQL direto: prova o banco, não o serviço que vai usá-lo |
| Tela (contrato do HTML) | MockMvc + jsoup | `src/test/java/.../web` | **500** em 27 classes | Cada tela renderiza o contrato com o back-end: rótulos, `data-testid`, estados, escape de HTML, um `h1`, título e idioma em todos os templates | Os dados vêm de controllers de teste; o JavaScript não roda |
| Ponta a ponta (E2E) | Playwright para Java, 375 px | `src/test/java/.../e2e` | **52** em 4 classes | A aplicação real no Chromium: responsividade das 28 telas e estados, login, menu, páginas de erro, fluxo da busca ao envio, diálogos, envio único | Telas com **dados de teste** e POST interceptados; sem back-end, não há login nem 409 de verdade |
| Fumaça | `@SpringBootTest` + Testcontainers | `AplicacaoSmokeTest` | **1** | A aplicação sobe contra PostgreSQL real e o Flyway roda | Não segue o README em clone limpo |
| API com token por perfil | MockMvc | — | **0** | — | Sem API |
| BDD | Cucumber | — | **0** | — | `aprovacao-bloqueio.feature` não existe |
| Integração externa | WireMock | — | **0** | — | Sem notificação (RF-11) |
| Carga e concorrência via HTTP | JMeter (CLI, local) | `perf/` | **0** | — | Só existe `perf/README.md` |
| Mutação manual | Roteiro | `docs/testes/mutacao-manual.md`, `docs/frontend/README.md` | 5 mutações no banco (M1 a M5) e 6 na tela (E1 a E5 do E2E e RN-07) | Cada mutação deixou pelo menos um teste vermelho | As 4 regras de serviço (aprovação, manutenção, autorização e a parte de serviço da auditoria) não têm mutação: o código não existe |

**Total:** 636 testes, 0 falhas, 0 erros, **0 ignorados**. Fonte: log do job `verify` do run 43091027823 ("Tests run: 636, Failures: 0, Errors: 0, Skipped: 0"). A contagem por nível é a soma das linhas "Tests run" de cada classe nesse mesmo log.

## 4. Resultados comparados às metas

| Meta | Resultado | Evidência | Interpretação | Limite | Atingida? |
|---|---|---|---|---|---|
| Cobertura ≥ 80% de linhas e ≥ 70% de branches (RNF-02) | **100,0%** das linhas (106 de 106) e **94,0%** dos branches (79 de 84) | `target/site/jacoco/jacoco.csv` do `verify` local de `3d63d72`, em 11/11/2026; gate do JaCoCo "All coverage checks have been met" no run 43091027823; SonarCloud do PR #42: "100.0% Coverage on New Code" | O código que existe está todo exercitado pelos testes | Só **106 linhas** de produção, todas da camada de tela (controllers). O número vai mudar quando o back-end entrar; hoje ele não diz nada sobre regra de negócio | **Sim**, para o código que existe |
| 100% dos requisitos críticos ligados a teste e run do CI | **0 de 2** completos. RF-06 `parcial` (só banco); RF-01 `ausente` | [RTM](rtm.md), seção "Resumo" | A reserva dupla tem proteção provada no banco; a autorização não tem teste nenhum | A RTM considera crítico o que a spec principal marca como crítico (RF-01 e RF-06) | **Não** |
| Requisitos com teste citado (KPI da spec principal: 14 de 14) | 0 RF `feito`, 5 `parcial`, 9 `ausente`; RN: 6 `parcial`, 7 `ausente` | [RTM](rtm.md), contagem das 30 linhas | Só o banco e as telas têm teste; o meio (serviço e API) não existe | Teste só de tela não conta como teste da regra | **Não** |
| 0 bugs ou vulnerabilidades críticas | Quality Gate: **0** issues novas e **0** Security Hotspots no PR #42. Dependências: **2** CVEs Critical (CVSS 9,8) no `spring-webmvc` 6.2.19, aceitos com justificativa | Comentário do SonarCloud no PR #42 (commit `e66c000`); `docs/seguranca/riscos-dependencias.md` (painel lido em 09/11/2026: 23 riscos em 123 dependências) | Pelo Quality Gate, a meta está cumprida. Pelo CVSS das dependências, não: os 2 críticos não têm correção na linha 6.2 e o projeto não usa as funções afetadas (`XsltView`, SSE) | Qual medição vale é a **D4, ainda sem resposta do professor**. A marcação "Accepted" no SonarCloud está pendente. O painel de dependências exige login e não foi relido depois de 09/11 | **Depende da D4** |
| Pipeline presente e verde | `verify` e SonarCloud verdes no run 43092312730 (`e66c000`). CI vermelho proposital no run #120 e recuperação no #121 (PR #30) | Checks do PR #42; seção "Histórico de CI vermelho proposital" do README | O CI barra código quebrado e se recupera | A `main` ainda não tem este commit; o CI da `main` não foi relido para este relatório | **Sim** |
| Reserva dupla impossível (limitador) | 20 pedidos simultâneos → 1 aceito e 19 recusados, recurso e professor, 5 rodadas cada | `ConcorrenciaReservaBancoTest` no run 43091027823; mutação M1: 24 de 83 testes de banco vermelhos sem a constraint; mutação M2: 2 vermelhos sem a trava | A proteção está no banco, não depende da aplicação, e os testes a pegam | Falta o 409 de ponta a ponta (serviço e API). O "9 de 40 rodadas com deadlock sem a trava" é de 06/11/2026 e não foi refeito | **Parcial** |
| Sem falha de autorização (limitador) | **Não medido** | — | Sem autenticação nem API, não há o que testar | É o maior risco da entrega (seção 6) | **Não** |
| Aplicação executa (limitador) | Sobe contra PostgreSQL real | `AplicacaoSmokeTest` no run 43091027823 | A aplicação inicia e o Flyway aplica a V1 | **Não verificado** em clone limpo seguindo só o README (roteiro em `docs/entrega/roteiro-clone-limpo.md`, ainda não executado) | **Parcial** |
| RNF-01: busca com p95 ≤ 300 ms para 50 usuários | **Não medido** | — | — | Não há busca no back-end nem plano JMeter | **Não medido** |
| RNF-03: criar reserva sob carga só responde 201 ou 409 | **Não medido** | — | — | Não há `POST /reservas` nem plano JMeter | **Não medido** |
| RF-13: telas responsivas e erros claros | 28 telas e estados em 375 px sem rolagem horizontal, um `h1`, alvo de toque ≥ 44 px; páginas 401, 403, 404 e 500 sem jargão; aviso de 409 | E2E no run 43091027823; prints no artifact `e2e-375` e em `docs/evidencias/e2e-375/`; E2E estável em 3 rodadas seguidas | A camada de tela cumpre o que promete no celular | Dados de teste; 768 e 1280 px só conferidos à mão; contraste AA de todas as telas **não medido** | **Parcial** |

## 5. Defeitos por severidade e situação

| Defeito | Severidade | Situação | Correção | Regressão |
|---|---|---|---|---|
| [DEF-001](defeitos/DEF-001.md): Quality Gate da `main` reprovado (Reliability C) por páginas de erro sem `<title>` | Alta (pipeline vermelho é limitador de nota) | **Corrigido** (PR #32, 09/11/2026); Quality Gate da `main` de volta a Passed, conferido na API pública do SonarCloud em 09/11/2026 (spec principal do Notion) | `fix(DEF-001): páginas de erro declaram title e lang` | `TemplatesComTituloTest`, que varre todos os templates (regra 10 do `AGENTS.md`) |
| [DEF-002](defeitos/DEF-002.md): teste de tela dependia do relógio real e ficou vermelho depois do horário da reserva de exemplo (12/11/2026, 17:00 UTC) | Alta (`verify` vermelho na `main` e nos PRs é limitador de nota) | **Corrigido** no PR #51, 12/11/2026 | `fix(DEF-002): relógio fixo nos recortes de tela` | Todos os recortes de tela com `Clock` fixo (`support/TelasNoRecorte`), conferido rodando a suíte depois do horário da reserva |

**Totais:** 1 defeito registrado. Alta: 1 corrigido. Média: 0. Baixa: 0. Abertos: 0.

**Achados que não viraram DEF**, com o motivo:
- **Deadlock (`40P01`) nos 20 pedidos simultâneos (06/11/2026).** Foi achado e corrigido antes do merge da V1, com a trava por recurso. Ficou registrado como decisão D7 e regra 9 do `AGENTS.md`, não como DEF.
- **Clique duplo no envio único (11/11/2026).** O Chromium dispara dois `submit` antes de o `envio-unico.js` travar o botão, mas só um POST sai. O contrato (um POST) se manteve no navegador medido. Em outros navegadores, **não medido**. Registrado em `docs/frontend/README.md`.

- **Print do E2E que falhou no CI (11/11/2026).** No run 43093990942, um caso de `Responsividade375E2ETest` (`/admin/recursos` vazia) passou nas asserções, mas falhou ao tirar o print de evidência ("Unable to capture screenshot"). O mesmo caso tinha passado nos dois runs anteriores. Foi corrigido no próprio PR #42 (`3c573fe`): o print espera um quadro desenhado e tenta mais uma vez só para essa falha. Ficou fora dos DEF porque não chegou à `main`, e é a única intermitência observada no E2E até agora.

**Desvio de processo** (ressalva da revisão do Gustavo no PR #42, 11/11/2026):
- **Telas 7 e 8 sem o `test(red)` antes.** Os testes `MinhasReservasPaginaTest` (36) e `ReservaDetalhePaginaTest` (38) entraram no mesmo commit que o código das telas: `b58380d`, que é `a81922a` depois da reescrita de autoria. Não houve um commit vermelho antes.
- **Motivo:** a regra 11 do `AGENTS.md` exige o OK explícito do dono da área antes do commit vermelho, e esse OK não foi pedido.
- **Evidência que existe:** a mutação manual de RN-07 (tirar `reserva.iniciada == false` das duas telas) deixou 5 testes vermelhos, e o código foi restaurado. Isso mostra que os testes pegam a regra quando ela é quebrada.
- **Limite:** não há prova de que esses testes falhavam antes de as telas existirem, porque o `git log` não tem o vermelho antes do verde. O E2E não é TDD (foi escrito sobre telas que já existiam). Para as outras telas e para os testes de banco, este relatório não conferiu, commit a commit, se o vermelho veio antes.

**Desvio de processo** (ressalva 3 da revisão do Gustavo no PR #48, 12/11/2026): a ligação das telas ao back-end (commit `fa94726`) trouxe código e testes juntos, sem um commit `test(red)` antes, pelo mesmo motivo das telas 7 e 8 (o OK da regra 11 não foi pedido). Evidência que existe: 4 mutações feitas à mão, todas pegas (lista na RTM, seção "Telas ligadas ao back-end"). O mesmo vale para a tela 16 (usuários).

**Limite:** defeito achado e corrigido dentro do próprio PR, antes do merge, não foi registrado como DEF. A contagem acima é a do que chegou à `main` ou foi registrado.

## 6. Riscos residuais

| Risco | Impacto | Situação em 11/11/2026 | Dono | O que reduz o risco |
|---|---|---|---|---|
| Falha de autorização | **Zera a nota** (limitador) | Sem autenticação, sem matriz de autorização, sem teste. As telas só escondem botões, o que não é barreira | Pedro | RF-01 com JWT e o teste parametrizado da matriz (`docs/seguranca/matriz-autorizacao.md`) antes da tag |
| Reserva dupla pela API | **Zera a nota** (limitador) | Protegida no banco e provada; a tradução de `23P01` em 409 e o teste de 20 pedidos pela API não existem | Pedro / Gustavo | Serviço que grava em ordem de `recurso_id` e devolve 409 (ADR-0007), com o teste de API |
| Aplicação não executa em clone limpo | **Zera a nota** (limitador) | Roteiro pronto, clone ainda não feito | Leandro ou Pedro | Executar `docs/entrega/roteiro-clone-limpo.md` antes da tag e anotar o que faltou |
| Regras de negócio sem teste no serviço | Requisitos `ausente` na RTM (RN-04, 06, 07, 08, 10, 11, 13) | O serviço não existe | Pedro | TDD das regras no serviço e BDD de aprovação e bloqueio |
| Desempenho desconhecido | Perde RNF-01 e RNF-03 | Não medido | Leandro | Plano JMeter em modo CLI contra a aplicação local, quando houver busca e `POST /reservas`; se não der tempo, manter "não medido" no relatório |
| 2 CVEs críticos aceitos no Spring 6.2.19 | Pode contar como "vulnerabilidade crítica", conforme a D4 | Aceitos com justificativa; marcação no SonarCloud pendente | Gustavo | Resposta do professor (D4) e marcação "Accepted" |
| Decisões pendentes | Testes e telas apoiados em premissas | **D4** (professor), **D8** (critérios do relatório) e **D9** (quando alterar ou cancelar) sem decisão; D1, D3 e D5 decididas pelo Gustavo, aguardando confirmação de Pedro e Leandro | Grupo | Decidir antes de o Pedro escrever os serviços correspondentes |
| E2E com dados de teste | A tela pode divergir do que o back-end mandar | Contratos documentados em `docs/frontend/README.md` | Leandro / Pedro | Rodar os mesmos E2E contra o back-end real quando ele existir |
| Força bruta no login das telas (registrado em 12/11/2026) | Senha adivinhada por tentativa e erro | Sem limite de tentativas nem atraso progressivo em `POST /login` (ressalva 1 do Gustavo no PR #48); o login só não revela se o usuário existe | Leandro / Pedro | Limite por login e por IP com bloqueio temporário, ou aceitar o risco por escrito antes da tag |
| Cookie de sessão sem `Secure` em produção | Sessão exposta se a aplicação rodar em HTTP fora da máquina local | `HttpOnly` e `SameSite=Lax` ligados; `Secure` depende de `SESSAO_COOKIE_SEGURO=true`, que exige HTTPS | Gustavo (ambiente) | Ligar a variável onde houver HTTPS |
| Prazo | Itens `ausente` na entrega | Meta interna 11/11; entrega 13/11 | Grupo | Priorizar os três limitadores acima; o que ficar de fora entra como risco aceito, com responsável |

## 7. Recomendação

**Recomendação deste relatório: não liberar como `v1.0.0-rc1` no estado do commit avaliado (bloquear).**

O critério são os quatro limitadores de nota do `AGENTS.md` (seção 2):
- **Pipeline verde:** atendido.
- **Reserva dupla:** atendida no banco, sem prova pela API.
- **Aplicação executa:** sobe nos testes, sem prova em clone limpo.
- **Falha de autorização:** não há como afirmar que não existe, porque a autorização não foi implementada nem testada.

Por isso a meta de 100% dos críticos não foi atingida.

**O que mudaria a recomendação para "liberar com ressalvas"** (cada item com teste ou evidência citável na RTM):
1. RF-01 com a matriz de autorização testada (401 sem token, 403 por perfil).
2. 20 pedidos pela API → 1 × 201 e 19 × 409.
3. Clone limpo seguindo só o README, por quem não o escreveu.
4. Desempenho medido, ou registrado como "não medido" e aceito como ressalva pelo grupo.

**A decisão formal (liberar, liberar com ressalvas ou bloquear)** fica em `docs/decisao-qualidade.md`, que **ainda não existe**. Ela é tomada com o grupo, a partir deste relatório.

## 8. Como refazer os números

No commit da tag, refazer cada linha da seção 4 e trocar o commit e os runs da seção 1:
- **Testes por nível:** somar as linhas `Tests run: … -- in br.com.senac.recursos.<pacote>` do log do job `verify` (pacotes `banco`, `web`, `e2e` e o `AplicacaoSmokeTest`). O total é a última linha `Tests run` do passo "Build, testes e cobertura".
- **Cobertura:** em `target/site/jacoco/jacoco.csv`, somar as colunas `LINE_COVERED`/`LINE_MISSED` e `BRANCH_COVERED`/`BRANCH_MISSED` (artifact `relatorios` do CI ou `verify` local).
- **RTM:** recontar as colunas "Situação" de `docs/rtm.md`.
- **Dependências:** reler o painel "Dependency risks" do SonarCloud (exige login) e atualizar `docs/seguranca/riscos-dependencias.md`.
- **Desempenho:** `perf/resultados/rodada-01.jtl`, quando existir, e `docs/performance/resultado-01.md`.
