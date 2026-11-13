# Decisão de qualidade — `v1.0.0-rc1`

> Parecer de entrega (Aula 14, lacuna 2 do plano). Escrito pelo Leandro, dono da decisão de qualidade (`AGENTS.md`, seção 1), em 12/11/2026, noite. **Atualizado em 13/11/2026**, depois dos PRs #59 a #62.
> - **Base:** [relatório de qualidade](relatorio-qualidade.md), versão 2 (PR #57), a [RTM](rtm.md) e os números da `main` em `6ce4478` (1116 testes, cobertura de 95,8% das linhas e 81,3% dos branches).
> - **O que mudou depois de `6ce4478`:** o PR #61 trouxe o OpenAPI (RF-14), os 13 cenários Cucumber, a mutação das regras do serviço e os testes da RN-08 (`./mvnw -B verify` local do #61: **1138 testes, 0 falhas, 0 ignorados**; 1116 + 13 + 5 + 4). A cobertura **não foi medida de novo** depois do #61. A `main` atual é `bc40183` (merge do #62, só documentação); o `ci.yml` está verde nela (run 251, id 43465315272) e no merge do #61 (run 248).
> - **No commit da tag:** a decisão vale para o commit que receber a tag. Os números são conferidos de novo nele (relatório, seção 8) e a seção 1 abaixo é atualizada antes da tag.

## 1. Decisão

**Liberar `v1.0.0-rc1` com ressalvas, condicionada ao clone limpo.**

| Condição | Situação em 13/11/2026 | Se não for cumprida |
|---|---|---|
| O clone limpo, seguindo só o `README.md` e feito por quem não o escreveu, sobe a aplicação (`docs/entrega/roteiro-clone-limpo.md`, registro em `docs/entrega/checklist.md`) | **Parcial** (lacuna 11). Rodada feita **pela IA** em 13/11/2026 (PR #60, commit `9acbbcd`): sobe, `/actuator/health` responde `UP`, 409 na sobreposição, `verify` verde. **Falta a conferência humana**: uma pessoa que não escreveu o README (Pedro, decidido pelo grupo) segue o README e anota o que faltar. A rodada da IA não foi feita em Windows | A decisão passa a **bloquear** até o README ser corrigido e o clone passar |
| CI (`verify` e SonarCloud) verde no commit da tag | Verde na `main`: `ci.yml` com `success` no merge do #61 (run 248) e no do #62, `bc40183` (run 251). O Quality Gate do SonarCloud não foi relido nesta atualização | **Bloquear** e corrigir antes da tag (correção depois da tag vira `v1.0.0-rc2`) |
| A D4 (resposta do professor sobre "0 vulnerabilidades críticas") não dizer que risco aceito com justificativa reprova | Sem resposta; pergunta na Aula 14 (13/11) | Registrar a resposta aqui e reavaliar: bloquear, ou manter com a ressalva explícita |

**Por que não "liberar" sem ressalvas:** 1 dos 14 RF não tem entrega (RF-11, notificação), 3 estão parciais (RF-02, RF-03 e RF-12), e há riscos aceitos, não corrigidos (seção 3). O RF-14 (OpenAPI) deixou de estar ausente no PR #61.

**Por que não "bloquear":**
- Dos quatro limitadores de nota (`AGENTS.md`, seção 2), três têm prova em teste e no CI: pipeline verde, sem falha de autorização e sem reserva dupla.
- O quarto, "a aplicação executa", tem prova nos testes, em `java -jar` (seção 2) e na rodada de clone limpo feita pela IA; falta só a conferência por uma pessoa, que é a condição acima.

## 2. Critérios, com a evidência

| Critério | Meta | Resultado | Evidência | Cumprido? |
|---|---|---|---|---|
| Cobertura (RNF-02) | ≥ 80% das linhas, ≥ 70% dos branches | 95,8% e 81,3% | `jacoco.csv` do `verify` local de `6ce4478`; gate do JaCoCo no run 43303372662; SonarCloud: 95,9% e 81,7% | Sim |
| Críticos ligados a teste e run do CI | 100% (RF-01 e RF-06) | 2 de 2 `feito` | [RTM](rtm.md), "Resumo"; run 43303372662 | Sim |
| Requisitos com teste citado | 14 de 14 RF | 13 de 14 com teste (10 `feito`, 3 `parcial`: RF-02, RF-03 e RF-12); RF-11 sem código. RN: 13 `feito`. RNF: 3 `feito` | [RTM](rtm.md), "Resumo" (13/11/2026, PR #61) | **Não** (ressalva) |
| 0 bugs ou vulnerabilidades críticas | 0 | Quality Gate OK: 0 bugs, 0 vulnerabilidades, 0 hotspots. Dependências: 0 Critical aberto e 6 riscos aceitos com justificativa (2 Critical) | API pública do SonarCloud (análise de `6ce4478`); `docs/seguranca/riscos-dependencias.md`, seções 3, 7 e 8 | Depende da D4 (ressalva) |
| Pipeline presente e verde (limitador) | Verde | `verify` verde nos 6 últimos pushes na `main`; ruleset exige `verify` e SonarCloud | Runs 207 a 221 do `ci.yml` | Sim |
| Sem reserva dupla (limitador) | 20 pedidos → 1 aceito | Banco: 1 aceito e 19 recusados. API: 1 × 201 e 19 × 409. Carga: uma reserva por horário em 80 horários disputados | `ConcorrenciaReservaBancoTest`, `ConcorrenciaReservaApiTest`, mutações M1 e M2; `docs/performance/resultado-01.md` (PR #56) | Sim |
| Sem falha de autorização (limitador) | Toda rota com regra, testada por perfil | Matriz com 222 casos; rota fora da matriz é negada e o teste falha se surgir rota nova; limite de tentativas no login | `MatrizAutorizacaoTest`, `LimiteDeTentativasApiTest`, `AcessoE2ETest` | Sim |
| Aplicação executa (limitador) | Sobe seguindo o README | Sobe nos testes (`AplicacaoSmokeTest`), subiu com `java -jar` e `docker compose` para a carga e subiu no clone limpo da IA (`9acbbcd`: `UP`, 409, `verify` com 1116 testes e 0 falhas) | Run 43303372662; `docs/performance/resultado-01.md`; `docs/entrega/checklist.md`, seção "Rodada de clone limpo" | **Parcial**: falta o clone limpo por uma pessoa (condição da seção 1) |
| RNF-01: busca | p95 ≤ 300 ms com 50 usuários | p95 = 17 ms e 8 ms | `perf/resultados/*.jtl`; meta escrita antes em `docs/performance/rnf.md` | Sim, só local |
| RNF-03: criação sob carga | só 201 ou 409 | 80 × 201 e 3920 × 409, nada mais | Mesmos `.jtl` | Sim, só local |
| Defeitos registrados antes da correção | Todo defeito que chegou à `main` vira `DEF-xxx` | 2 registrados (DEF-001 e DEF-002), os 2 corrigidos, com teste de regressão | `docs/defeitos/` | Sim |

## 3. Riscos aceitos

Cada risco com impacto, motivo de aceitar e responsável. A revisão de cada um fica com o responsável.

| # | Risco | Impacto | Por que aceitamos | Responsável |
|---|---|---|---|---|
| 1 | 2 CVEs **Critical** do `spring-webmvc` 6.2.19 (CVE-2026-47884, CVE-2026-47890) | Pode contar como "vulnerabilidade crítica" (D4) | Sem versão corrigida na linha 6.2; o projeto não usa `XsltView` nem SSE com fragmentos de view (conferido com `git grep`). Marcados "Accepted" no SonarCloud (`riscos-dependencias.md`, seções 3 e 6) | Gustavo |
| 2 | `jaxb-core` 4.0.9, CVE-2026-92877 (XXE, High, CVSS 8,3) | Leitura de arquivo local, se a aplicação desserializasse XML não confiável | Não existe versão corrigida; o projeto não desserializa XML de ninguém (0 ocorrências em `src/`); entra só pelo Hibernate (seção 7) | Gustavo |
| 3 | 3 riscos do Spring Security 6.5.11: CVE-2026-41707 (High, 7,4), CVE-2026-47842 e CVE-2026-59276 (Medium) | Replay de DPoP, IV zerado, vazamento por tempo de comparação | Correção só na 7.x, que exige o Spring Boot 4. O DPoP é recusado porque o token não tem `cnf.jkt`, e o projeto não usa `Encryptors`. **O CVE-2026-59276 não foi verificado** (seção 8) | Gustavo |
| 4 | D13: a recusa por manutenção (RN-04) é "consulta e depois grava" | Uma manutenção criada entre a consulta e o commit de uma reserva passa | Janela de milissegundos, só o Administrador cria manutenção, o caso não está no PRD (ADR-0008, decidida em 12/11/2026) | Gustavo / Pedro |
| 5 | RF-11 (notificação) sem entrega (o RF-14, OpenAPI, ficou `feito` no PR #61) | 1 dos 14 RF `ausente`; sem cliente HTTP de notificação e sem os testes de WireMock (200, 404, 500, timeout, JSON inválido, token que não vaza) | Não coube no prazo; não é limitador de nota. Fica para depois da tag (`v1.0.0-rc2` ou seguinte). **Aceito como risco pelo Pedro em 13/11/2026**: não entra na `v1.0.0-rc1`; o ADR-0005 (proposto) segue como o desenho para quando entrar | Pedro |
| 6 | RF-02 e RF-03 sem localização, patrimônio e competência; RF-12 com premissas pendentes (D8) | Busca sem dois filtros do PRD; campos na tela que não são gravados ("Não informado"); números do relatório apoiados em premissa | Dependem da migração V2 e da D8, não feitas no prazo. A premissa do relatório está testada com valores calculados à mão | Gustavo (V2) / grupo (D8) |
| 7 | D9 sem decisão do grupo; D10 e D11 decididas (PR #58, "fica como está no código") com lacunas de teste: D10 (c), D10 (d) e D11 na API | O comportamento de alterar e cancelar (D9) é a proposta do back-end, não uma decisão; o professor pode discordar da regra | A proposta está implementada e testada; o PRD não decide esse ponto (regra 4 do `AGENTS.md`). As lacunas de teste de D10 (c), D10 (d) e D11 na API foram fechadas em 13/11/2026 (PR `test/d10-d11-api-1311`: 8 testes; sem eles, 2 das 3 mutações de conferência passavam despercebidas) | Grupo (Pedro propõe) |
| 8 | Limite de tentativas do login em memória, sem limite por IP | Com mais de uma instância, cada uma conta as suas tentativas; uma senha tentada em muitos logins não é barrada; quem conhece um login pode bloqueá-lo por 15 min | Uma instância só na entrega (ADR-0009, ressalva 1). **Aceite por escrito:** Pedro (API) em 13/11/2026; Leandro (telas): pendente | Leandro (telas) / Pedro (API) |
| 9 | Cookie de sessão sem `Secure` por padrão | Sessão exposta se rodar em HTTP fora da máquina local | `Secure` exige HTTPS; liga com `SESSAO_COOKIE_SEGURO=true` | Gustavo (ambiente) |
| 10 | Desempenho medido só localmente | O p95 não vale para outra máquina nem para produção | JMeter, aplicação e banco na mesma máquina de 4 vCPUs; duas rodadas | Leandro |
| 11 | Lacunas de teste que restam (o BDD com Cucumber e a mutação das seis regras críticas foram feitos no PR #61, lacunas 4 e 6): ~~mutação do serviço do relatório (RF-12)~~ feita em 13/11/2026 (MR1 a MR5, todas pegas; a MR4 achou uma lacuna e entrou `RelatorioInstantaneoTest`, PR `test/mutacao-relatorio-1311`); testes de D10 (c), D10 (d) e D11 na API | Menos evidência de que o teste pega a regra quebrada nesses pontos | O BDD roda no `verify` (13 cenários) e as 8 mutações do serviço foram pegas (a MAU1 achou a lacuna da auditoria, e entraram 4 testes). Para os pontos que restam, a regra está implementada, mas sem teste que a prenda: **não medido** | Pedro |

## 4. O que cada integrante fez

Fonte: PRs mergeados na `main` até o #62 (`bc40183`, 13/11/2026, 16:41 UTC), contados pelo autor de cada PR na lista do GitHub (59 mergeados; os PRs #3, #26 e #29 foram fechados sem merge), e as áreas da seção 1 do `AGENTS.md`. A revisão foi em roda (Gustavo → Pedro → Leandro → Gustavo). Esta tabela dizia 23 para o Leandro (#5 a #57) e foi recontada: os PRs #59, #60, #61 e #62 somam 4.

| Integrante | Área | PRs mergeados | Entregas principais |
|---|---|---|---|
| **Gustavo Oliveira Fernandes de Andrade** | Banco, CI, SonarCloud, arquitetura e ADRs, README, tag | 30 (#1 a #55) | Esqueleto Spring Boot; CI com SonarCloud, gate do JaCoCo, CI vermelho proposital e checagem de migração imutável; migração V1 com a constraint contra reserva dupla e os testes de banco em TDD; auditoria e consultas do relatório no banco; ADRs 0001 a 0007; avaliação arquitetural (ciclo 01); riscos de dependências e a D4; plano de execução e `AGENTS.md`; índice de evidências; roteiro do clone limpo |
| **Pedro Henrique** | Back-end, PRD | 2 (#44 e #58), mais as revisões dos PRs do Leandro | Back-end em camadas: API REST com JWT, serviço de reserva com a máquina de estados, auditoria na mesma transação, tradução do `23P01` em 409, manutenção, relatório; matriz de autorização lida pelo teste; testes de API, incluindo os 20 pedidos simultâneos; ADR-0008 (back-end em camadas, aceito no PR #58); decisões D10 e D11; mutação M1 repetida no nível da API (10 de 10 vermelhos) |
| **Leandro Sato** | Front-end, RTM, relatório, decisão de qualidade | 27 (#5 a #62) | As 16 telas em Thymeleaf e o design system; ligação das telas aos services com login por sessão (D12, ADR-0009); E2E Playwright em 375 px; limite de tentativas no login (telas e API) em TDD; DEF-001 e DEF-002; carga com JMeter; RTM, relatório de qualidade e esta decisão; com IA e na área do Pedro, a pedido do grupo (revisão do Pedro): BDD com Cucumber, mutação das regras do serviço, testes da RN-08, plano de TDD e OpenAPI (PR #61); roteiro do clone limpo rodado (PR #60) e perguntas e roteiro da prova oral (PR #62) |

**Uso de IA**, declarado:
- Parte do trabalho do Leandro foi feita com o Claude Code. Os commits trazem a linha `Co-Authored-By: Claude`.
- 8 commits na `main` saíram com o autor "Claude" em vez do nome do Leandro (`git log origin/main --author=Claude`, 13/11/2026): 2 de 04/11/2026 (PR #5), 1 merge de 09/11/2026 (PR #32) e 5 de 12/11/2026 (PR #52). Esta seção dizia 7: faltava o merge do #32. A causa foi o `git config` da máquina usada. Não foram corrigidos, porque isso exigiria reescrever a `main` (relatório, seção 5). Se a explicação basta é decisão do Gustavo (pendência dele no plano).
- O uso de IA pelos outros integrantes fica na declaração de cada um.

**Declaração de cada integrante** (o checklist da entrega pede; cada um escreve a sua, com as próprias palavras):
- **Gustavo:** Fui responsável pelo banco de dados, pela CI, pelo SonarCloud, pela arquitetura e pelos ADRs, pelo README e pela tag. Escrevi a migração V1 com a constraint `EXCLUDE` contra reserva dupla e a trava por recurso (D7), com os testes de banco em TDD. Também fiz a CI com o gate do JaCoCo e o SonarCloud, o CI vermelho proposital, a checagem de migração imutável, os ADRs 0001 a 0007, a avaliação arquitetural (ciclo 01) e o tratamento dos riscos de dependência. Usei o Claude Code em boa parte do trabalho: para pesquisar CVEs e versões, rascunhar documentos e ADRs, rodar o `verify` e medir cobertura. Revisei cada diff antes do commit e decidi cada ponto. Os números que citei vêm de testes, runs do CI ou do SonarCloud, e o que não foi medido está escrito como "não medido". Concordo com a decisão de liberar com ressalvas, desde que o clone limpo humano passe. (13/11/2026)
- **Pedro:** Fui responsável pelo back-end e pelo PRD. Escrevi a API REST em camadas com JWT (PR #44): o serviço de reserva com a máquina de estados, a tradução do `23P01` em 409 com a tentativa recusada gravada à parte, a auditoria na mesma transação, a manutenção e o relatório, com os testes de API contra PostgreSQL real, incluindo os 20 pedidos simultâneos. Decidi D10 e D11 ("fica como está no código"), dei o OK na D13 e repeti a mutação M1 no nível da API. Conferi as duas mudanças que o Leandro fez na minha área: o limite de tentativas no `AutenticacaoService` (PR #52) e as rotas do Swagger fora das tabelas da matriz (PR #61). Aceito como risco o RF-11 fora desta versão e o limite de tentativas em memória, sem limite por IP, na parte da API. Usei o Claude Code: para escrever e rodar testes, fazer as mutações à mão, rodar o `verify` e rascunhar documentos, inclusive esta declaração; li cada diff antes do commit e as decisões são minhas. Os números que cito vêm de testes e do `verify` local, e o que não foi medido está escrito como "não medido". Concordo com liberar com ressalvas, desde que o clone limpo humano passe. (13/11/2026)
- **Leandro:** _a escrever pelo Leandro._

## 5. Limite do que foi provado

- **Teste que passa não é "pronto para produção".** As provas são de testes automatizados (PostgreSQL em container, MockMvc, navegador sem tela) e de uma carga numa máquina só. Nada aqui foi medido num ambiente de produção, e esta decisão não afirma nada sobre ele (`AGENTS.md`, seção 6).
- **Uma instância.** A reserva dupla é barrada no banco e vale com várias instâncias. Já o limite de tentativas e a sessão das telas valem para uma só.
- **Cobertura não é qualidade de teste.** As mutações feitas à mão mostram que os testes pegam as regras listadas no relatório (seção 3): banco, API e as oito do serviço. Para o serviço do relatório, isso **não foi medido**. As mutações do serviço foram medidas só na máquina de quem rodou, sem run do CI.
- **Decisões pendentes:** o que está testado em D8 e D9 é a proposta, não uma regra decidida pelo grupo. D10 e D11 foram decididas no PR #58; D10 (c), D10 (d) e D11 ganharam teste na API em 13/11/2026 (PR `test/d10-d11-api-1311`).
- **Os números da seção 2 são de `6ce4478`**, com a contagem de testes atualizada para o `verify` local do #61 (1138). Cobertura, Quality Gate e painel de dependências não foram relidos depois disso. No commit da tag, eles são conferidos de novo, e esta seção e a seção 1 são atualizadas antes da tag.
