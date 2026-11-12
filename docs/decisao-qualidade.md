# Decisão de qualidade — `v1.0.0-rc1`

> Parecer de entrega (Aula 14, lacuna 2 do plano). Escrito pelo Leandro, dono da decisão de qualidade (`AGENTS.md`, seção 1), em 12/11/2026, noite.
> - **Base:** [relatório de qualidade](relatorio-qualidade.md), versão 2 (PR #57), a [RTM](rtm.md) e os números da `main` em `6ce4478`. A `main` atual (`9acbbcd`) tem o mesmo código; desde então, só a documentação mudou.
> - **No commit da tag:** a decisão vale para o commit que receber a tag. Os números são conferidos de novo nele (relatório, seção 8) e a seção 1 abaixo é atualizada antes da tag.

## 1. Decisão

**Liberar `v1.0.0-rc1` com ressalvas, condicionada ao clone limpo.**

| Condição | Situação em 12/11/2026, noite | Se não for cumprida |
|---|---|---|
| O clone limpo, seguindo só o `README.md` e feito por quem não o escreveu, sobe a aplicação (`docs/entrega/roteiro-clone-limpo.md`, registro em `docs/entrega/checklist.md`) | **Pendente** (lacuna 11) | A decisão passa a **bloquear** até o README ser corrigido e o clone passar |
| CI (`verify` e SonarCloud) verde no commit da tag | Verde na `main` (run 43303372662 e os 6 pushes anteriores) | **Bloquear** e corrigir antes da tag (correção depois da tag vira `v1.0.0-rc2`) |
| A D4 (resposta do professor sobre "0 vulnerabilidades críticas") não dizer que risco aceito com justificativa reprova | Sem resposta; pergunta na Aula 14 (13/11) | Registrar a resposta aqui e reavaliar: bloquear, ou manter com a ressalva explícita |

**Por que não "liberar" sem ressalvas:** 2 dos 14 RF não têm entrega (RF-11 e RF-14), 3 estão parciais (RF-02, RF-03 e RF-12), e há riscos aceitos, não corrigidos (seção 3).

**Por que não "bloquear":**
- Dos quatro limitadores de nota (`AGENTS.md`, seção 2), três têm prova em teste e no CI: pipeline verde, sem falha de autorização e sem reserva dupla.
- O quarto, "a aplicação executa", tem prova nos testes e em `java -jar` (seção 2), e falta só a prova em clone limpo, que é a condição acima.

## 2. Critérios, com a evidência

| Critério | Meta | Resultado | Evidência | Cumprido? |
|---|---|---|---|---|
| Cobertura (RNF-02) | ≥ 80% das linhas, ≥ 70% dos branches | 95,8% e 81,3% | `jacoco.csv` do `verify` local de `6ce4478`; gate do JaCoCo no run 43303372662; SonarCloud: 95,9% e 81,7% | Sim |
| Críticos ligados a teste e run do CI | 100% (RF-01 e RF-06) | 2 de 2 `feito` | [RTM](rtm.md), "Resumo"; run 43303372662 | Sim |
| Requisitos com teste citado | 14 de 14 RF | 12 de 14 com teste (9 `feito`, 3 `parcial`); RF-11 e RF-14 sem código | [RTM](rtm.md) | **Não** (ressalva) |
| 0 bugs ou vulnerabilidades críticas | 0 | Quality Gate OK: 0 bugs, 0 vulnerabilidades, 0 hotspots. Dependências: 0 Critical aberto e 6 riscos aceitos com justificativa (2 Critical) | API pública do SonarCloud (análise de `6ce4478`); `docs/seguranca/riscos-dependencias.md`, seções 3, 7 e 8 | Depende da D4 (ressalva) |
| Pipeline presente e verde (limitador) | Verde | `verify` verde nos 6 últimos pushes na `main`; ruleset exige `verify` e SonarCloud | Runs 207 a 221 do `ci.yml` | Sim |
| Sem reserva dupla (limitador) | 20 pedidos → 1 aceito | Banco: 1 aceito e 19 recusados. API: 1 × 201 e 19 × 409. Carga: uma reserva por horário em 80 horários disputados | `ConcorrenciaReservaBancoTest`, `ConcorrenciaReservaApiTest`, mutações M1 e M2; `docs/performance/resultado-01.md` (PR #56) | Sim |
| Sem falha de autorização (limitador) | Toda rota com regra, testada por perfil | Matriz com 222 casos; rota fora da matriz é negada e o teste falha se surgir rota nova; limite de tentativas no login | `MatrizAutorizacaoTest`, `LimiteDeTentativasApiTest`, `AcessoE2ETest` | Sim |
| Aplicação executa (limitador) | Sobe seguindo o README | Sobe nos testes (`AplicacaoSmokeTest`) e subiu com `java -jar` e `docker compose` para a carga | Run 43303372662; `docs/performance/resultado-01.md` | **Pendente**: falta o clone limpo (condição da seção 1) |
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
| 5 | RF-11 (notificação) e RF-14 (OpenAPI) sem entrega | 2 dos 14 RF `ausente` | Não couberam no prazo; nenhum dos dois é limitador de nota. Ficam para depois da tag (`v1.0.0-rc2` ou seguinte) | Pedro |
| 6 | RF-02 e RF-03 sem localização, patrimônio e competência; RF-12 com premissas pendentes (D8) | Busca sem dois filtros do PRD; campos na tela que não são gravados ("Não informado"); números do relatório apoiados em premissa | Dependem da migração V2 e da D8, não feitas no prazo. A premissa do relatório está testada com valores calculados à mão | Gustavo (V2) / grupo (D8) |
| 7 | D9, D10 e D11 sem decisão do grupo | O comportamento é a proposta do back-end, não uma decisão; o professor pode discordar da regra | A proposta está implementada e testada; o PRD não decide esses pontos (regra 4 do `AGENTS.md`) | Grupo (Pedro propõe) |
| 8 | Limite de tentativas do login em memória, sem limite por IP | Com mais de uma instância, cada uma conta as suas tentativas; uma senha tentada em muitos logins não é barrada; quem conhece um login pode bloqueá-lo por 15 min | Uma instância só na entrega (ADR-0009, ressalva 1) | Leandro (telas) / Pedro (API) |
| 9 | Cookie de sessão sem `Secure` por padrão | Sessão exposta se rodar em HTTP fora da máquina local | `Secure` exige HTTPS; liga com `SESSAO_COOKIE_SEGURO=true` | Gustavo (ambiente) |
| 10 | Desempenho medido só localmente | O p95 não vale para outra máquina nem para produção | JMeter, aplicação e banco na mesma máquina de 4 vCPUs; duas rodadas | Leandro |
| 11 | BDD com Cucumber e mutação das regras do serviço não feitos (lacunas 4 e 6) | Menos evidência de que os testes de aprovação, manutenção e autorização pegam a regra quebrada | Os testes de API dessas regras existem e passam; a mutação de autorização feita nas telas foi pega | Pedro |

## 4. O que cada integrante fez

Fonte: PRs mergeados na `main` até 12/11/2026, noite (lista do GitHub), e as áreas da seção 1 do `AGENTS.md`. A revisão foi em roda (Gustavo → Pedro → Leandro → Gustavo).

| Integrante | Área | PRs mergeados | Entregas principais |
|---|---|---|---|
| **Gustavo Oliveira Fernandes de Andrade** | Banco, CI, SonarCloud, arquitetura e ADRs, README, tag | 30 (#1 a #55) | Esqueleto Spring Boot; CI com SonarCloud, gate do JaCoCo, CI vermelho proposital e checagem de migração imutável; migração V1 com a constraint contra reserva dupla e os testes de banco em TDD; auditoria e consultas do relatório no banco; ADRs 0001 a 0007; avaliação arquitetural (ciclo 01); riscos de dependências e a D4; plano de execução e `AGENTS.md`; índice de evidências; roteiro do clone limpo |
| **Pedro Henrique** | Back-end, PRD | 1 (#44), mais as revisões dos PRs do Leandro | Back-end em camadas: API REST com JWT, serviço de reserva com a máquina de estados, auditoria na mesma transação, tradução do `23P01` em 409, manutenção, relatório; matriz de autorização lida pelo teste; testes de API, incluindo os 20 pedidos simultâneos; ADR-0008 (back-end em camadas, ainda "proposto") |
| **Leandro Sato** | Front-end, RTM, relatório, decisão de qualidade | 21 (#5 a #52), e os PRs #56 e #57 abertos | As 16 telas em Thymeleaf e o design system; ligação das telas aos services com login por sessão (D12, ADR-0009); E2E Playwright em 375 px; limite de tentativas no login (telas e API) em TDD; DEF-001 e DEF-002; carga com JMeter; RTM, relatório de qualidade e esta decisão |

**Uso de IA**, declarado:
- Parte do trabalho do Leandro foi feita com o Claude Code. Os commits trazem a linha `Co-Authored-By: Claude`.
- 7 commits na `main` saíram com o autor "Claude" em vez do nome do Leandro: 2 de 04/11/2026 (PR #5) e 5 de 12/11/2026 (PR #52). A causa foi o `git config` da máquina usada. Não foram corrigidos, porque isso exigiria reescrever a `main` (relatório, seção 5).
- O uso de IA pelos outros integrantes fica na declaração de cada um.

**Declaração de cada integrante** (o checklist da entrega pede; cada um escreve a sua, com as próprias palavras):
- **Gustavo:** _a escrever pelo Gustavo._
- **Pedro:** _a escrever pelo Pedro._
- **Leandro:** _a escrever pelo Leandro._

## 5. Limite do que foi provado

- **Teste que passa não é "pronto para produção".** As provas são de testes automatizados (PostgreSQL em container, MockMvc, navegador sem tela) e de uma carga numa máquina só. Nada aqui foi medido num ambiente de produção, e esta decisão não afirma nada sobre ele (`AGENTS.md`, seção 6).
- **Uma instância.** A reserva dupla é barrada no banco e vale com várias instâncias. Já o limite de tentativas e a sessão das telas valem para uma só.
- **Cobertura não é qualidade de teste.** As mutações feitas à mão mostram que os testes pegam as regras listadas no relatório (seção 3). Para as regras do serviço sem mutação registrada, isso **não foi medido**.
- **Decisões pendentes:** o que está testado em D8 a D11 é a proposta, não uma regra decidida pelo grupo.
- **Os números são de `6ce4478`.** No commit da tag, eles são conferidos de novo, e esta seção e a seção 1 são atualizadas antes da tag.
