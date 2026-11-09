# Avaliação arquitetural — Ciclo 01 (ATAM)

> **Escrito em 07/11/2026 por Gustavo (dono da arquitetura).** É um ciclo de **leitura e análise**, feito a partir de `docs/arquitetura.md`, dos ADRs, do PRD e dos testes que existem. **Não houve sessão com Pedro e Leandro**: os pontos que dependem deles estão marcados como pendentes. O ciclo 02 está previsto para depois da API, do `GET /relatorios/utilizacao` e da rodada de carga (Etapa 5).
> **Regra de números:** todo número tem fonte na seção 9; o que não foi medido está escrito **"não medido"**. Medições feitas na máquina do Gustavo (Windows 11, JDK 21, Docker Desktop, PostgreSQL 16.15 em Testcontainers) **não se generalizam**; não há run do CI com elas.
> **Versão avaliada (atualizada em 09/11/2026):** `main` em `a6d6374`, que já tem a V1 e o ADR-0007 (PR #28), a auditoria e o relatório no nível do banco (PR #33), a checagem de CI de migração (PR #34), a seção 10 da arquitetura (PR #35) e a correção DEF-001 (PR #32, do Leandro). A versão de 07/11 era a `main` em `009a4be` mais duas branches empilhadas; o `verify` dessa pilha deu 510 testes, 0 falhas, 0 ignorados (fonte 8). O `verify` desta branch, depois do merge da `main` de 09/11, está na fonte 9.

## 1. Objetivo e escopo

Verificar se a arquitetura proposta sustenta os atributos de qualidade que o professor ataca e que derrubam a nota (os quatro limitadores de `AGENTS.md`, seção 2): **reserva dupla**, **falha de autorização**, **pipeline vermelho** e **aplicação que não executa**. Este ciclo olha os três primeiros por cenário; o quarto é coberto pelo teste de fumaça e pelo `verify` e não gera cenário próprio.

Fora do escopo do ciclo 01: a camada de telas (Thymeleaf, do Leandro) e o provedor de notificação, que só entram como trade-off.

## 2. Direcionadores de negócio

| # | Direcionador | Fonte |
|---|---|---|
| N1 | Nota vem de evidência de qualidade (testes, CI, cobertura, RTM), não de "funcionar" | `AGENTS.md`, seção 1 |
| N2 | Quatro limitadores: aplicação que não executa, pipeline ausente ou vermelho, falha de autorização, reserva dupla | `AGENTS.md`, seção 2 |
| N3 | Entrega 13/11/2026 (tag `v1.0.0-rc1`); meta interna 11/11; prova oral 20/11 | `AGENTS.md`, seção 1 |
| N4 | Três perfis (Solicitante, Responsável, Administrador) e três tipos de recurso (sala, material, professor) | `docs/prd.md` |
| N5 | Stack fechada: Java 21, Spring Boot 3.5, PostgreSQL 16, Flyway, Thymeleaf, Testcontainers | `AGENTS.md`, seção 1 e ADRs |

## 3. Visão da arquitetura avaliada

Resumo (detalhes e diagramas C4 em [`docs/arquitetura.md`](../arquitetura.md), seções 2 a 5): um único deploy Spring Boot com camada web (Thymeleaf e API REST), serviços de domínio, JPA e PostgreSQL com esquema versionado pelo Flyway. Decisões que sustentam os cenários:

| ADR | Decisão | Status |
|---|---|---|
| [0001](../adr/0001-login-jwt.md) | Login com JWT | aceito |
| [0002](../adr/0002-anti-reserva-dupla.md) | Reserva dupla barrada por `EXCLUDE USING gist` no banco | aceito |
| [0003](../adr/0003-thymeleaf.md) | Thymeleaf no mesmo projeto | aceito |
| [0005](../adr/0005-notificacao-cliente-http-isolado.md) | Notificação por cliente HTTP isolado | proposto |
| [0006](../adr/0006-flyway-dono-do-esquema.md) | Flyway como único dono do esquema | proposto |
| [0007](../adr/0007-modelagem-protecao-reserva-dupla.md) | Uma constraint para recurso e professor, estado copiado por FK composta, trava por recurso | proposto (D6 e D7; a confirmar com o Pedro) |

O que **existe no código** da `main` (conferido em 09/11/2026 em `a6d6374`): a V1 do banco e seus testes, as consultas do relatório e os testes de auditoria no nível do banco, as telas Thymeleaf (em `src/main/java` só há controllers de tela, sem lógica de reserva), o `verify` no CI e a checagem de migração imutável no CI. O que **não existe**: serviço de reserva, API REST, autenticação JWT, auditoria no serviço, endpoint do relatório. Toda conclusão sobre esses itens é, portanto, **sobre o desenho**, não sobre código.

## 4. Utility tree

Importância (A/M/B) e dificuldade/risco técnico (A/M/B), do ponto de vista da nota. Os cenários C1 a C5 estão na seção 5.

| Atributo | Refinamento | Cenário | Importância | Risco | Evidência hoje |
|---|---|---|---|---|---|
| Integridade dos dados | Nenhuma reserva sobreposta, mesmo com pedidos simultâneos, para recurso e professor | C1 | A | A | nível banco: sim; ponta a ponta: não |
| Segurança | Cada perfil faz só o que a matriz permite; erro sem stack trace | C2 | A | M | nenhuma (matriz e testes não existem) |
| Desempenho | Busca e criação de reserva sob 50 usuários | C3 | M | M | nenhuma (**não medido**) |
| Auditabilidade | Mudança de estado e auditoria na mesma transação | C4 | A | M | nível banco: sim; serviço: não |
| Usabilidade | Tela de reserva em 375 px, erro de conflito claro | C5 | M | B | nenhuma medida neste ciclo (**não medido**) |
| Testabilidade | Banco real em teste, relógio injetado, integração externa isolada | transversal | A | B | banco real: sim; `Clock` e WireMock: ainda sem código que os use |
| Manutenibilidade | Esquema só por Flyway; regra fora dos controllers | transversal | M | B | Flyway: sim (V1 aplicada em banco vazio, `MigracoesBancoTest`); migração mergeada não editada: checagem de CI (fonte 10), que só tem o que proteger agora que a V1 está na `main`; regra fora dos controllers: sem serviço para conferir |

## 5. Cenários em seis partes

### C1 — Reserva dupla (personas: Responsável e Solicitante; RF-05, RF-06, RN-02, RN-03)

| Parte | Descrição |
|---|---|
| Fonte do estímulo | 20 clientes simultâneos (teste automatizado) |
| Estímulo | 20 pedidos para o mesmo recurso e o mesmo horário; repetido para a agenda do professor |
| Artefato | Constraint `reserva_recurso_sem_sobreposicao` e trigger de trava na V1; serviço de reserva (não existe) |
| Ambiente | Operação normal; PostgreSQL 16 real em container; uma máquina |
| Resposta | Exatamente uma reserva aceita; as outras recusadas pelo banco com `23P01` |
| Medida | 1 aceita e 19 recusadas, **contagem feita no banco**, em 5 rodadas para o recurso e 5 para o professor |

- **Estado:** medido **no nível do banco**, em `ConcorrenciaReservaBancoTest` (fonte 1). Sem a trava por recurso, **9 de 40 rodadas** de 20 pedidos terminaram em deadlock (`40P01`) e cada rodada ruim levou cerca de 17 a 20 s; com a trava, **30 de 30** passaram (fonte 2, 06/11/2026, uma máquina). O 409 de ponta a ponta **não existe**: depende do serviço do Pedro.
- **Evidência de que o teste enxerga o defeito:** mutação do predicado da constraint (`WHERE (false)`): 22 de 41 testes de Concorrência e Sobreposição ficaram vermelhos no PR #28 (fonte 2); a rodada refeita em 07/11 deu 24 de 83 testes de banco vermelhos (fonte 3).

### C2 — Falha de autorização (Solicitante; RF-01, RN-13)

| Parte | Descrição |
|---|---|
| Fonte do estímulo | Solicitante autenticado; também requisição sem token |
| Estímulo | Pede alterar a reserva de outro Solicitante, aprovar uma reserva ou abrir o relatório |
| Artefato | Camada de segurança e serviço de reserva (não existem) |
| Ambiente | Operação normal |
| Resposta | Sem token: 401; perfil errado ou posse alheia: 403; reserva intacta; erro sem stack trace |
| Medida | 403 em 100% das células "proibido" da matriz perfil × operação |

- **Estado:** meta definida, **não medido**. Não existem `docs/seguranca/matriz-autorizacao.md`, o teste parametrizado nem o código de segurança (fonte 4: caixas abertas da Etapa 5). É o cenário de maior risco ainda sem nenhuma evidência.

### C3 — Desempenho (Solicitante; RNF-01, RNF-03)

| Parte | Descrição |
|---|---|
| Fonte do estímulo | 50 usuários simulados no JMeter (modo CLI) |
| Estímulo | Consulta de disponibilidade e criação de reserva |
| Artefato | API REST e banco (a API não existe) |
| Ambiente | Máquina local, aplicação local; **não se mede produção** |
| Resposta | Respostas dentro do limite; criação responde só 201 ou 409 |
| Medida | p95 ≤ 300 ms na busca (RNF-01, "a validar"); nenhum 500 na criação (RNF-03) |

- **Estado:** **não medido**. Não existe API nem plano de carga (`docs/performance/rnf.md` e `perf/` ainda não escritos). A única observação de tempo é a do deadlock sem trava (C1), que **não** é medida de RNF-01.

### C4 — Auditoria atômica (Responsável; RF-10, RN-12)

| Parte | Descrição |
|---|---|
| Fonte do estímulo | Serviço de reserva ao mudar o estado de uma reserva |
| Estímulo | Mudança de estado, com falha no meio da transação |
| Artefato | Tabela `auditoria_reserva` e a transação do serviço |
| Ambiente | Operação normal; falha provocada pelo próprio banco |
| Resposta | Rollback completo: nem auditoria órfã, nem mudança de estado sem auditoria |
| Medida | 0 linhas de auditoria depois do rollback da mudança recusada; estado original preservado; e o inverso |

- **Estado:** medido **no nível do banco** em `AuditoriaBancoTest` (3 testes, nas duas direções; fonte 5). Mutações M3a e M3b (auditoria ou estado em transação própria) deixaram 1 de 3 testes vermelhos cada, mas foram feitas **no próprio teste**, porque o serviço não existe (fonte 3). O serviço real e a API do histórico continuam pendentes (Pedro).

### C5 — Usabilidade em 375 px (Solicitante; RF-13)

| Parte | Descrição |
|---|---|
| Fonte do estímulo | Solicitante no celular entre uma aula e outra |
| Estímulo | Reservar uma sala e receber um conflito |
| Artefato | Telas Thymeleaf |
| Ambiente | Viewport de 375 px |
| Resposta | Fluxo completo sem rolagem horizontal; erro de conflito em linguagem simples |
| Medida | 0 rolagem horizontal; mensagem de conflito exibida |

- **Estado:** as telas existem com testes de página (área do Leandro), mas **este ciclo não mediu** a rolagem em 375 px: **não medido** aqui. Fica para o Leandro registrar no ciclo 02.

## 6. Pontos de sensibilidade

| # | Ponto | Por que é sensível | Onde está tratado |
|---|---|---|---|
| S1 | Definição do intervalo (`[início, fim)`, D1) | Muda o resultado de toda fronteira: reserva que termina quando outra começa | `SobreposicaoBancoTest.fronteirasDoIntervaloSemiaberto`; tabela do BDD do Pedro **ainda por atualizar** (arquitetura, R4) |
| S2 | Ordem de gravação das linhas de `reserva_recurso` | Reservas com vários recursos podem se travar em ordem oposta | Regra no `AGENTS.md`, seção 5; teste de API **não existe** (Pedro) |
| S3 | Transação própria para a tentativa recusada (D2) | Se gravada na mesma transação, o rollback apaga o "conflitos evitados" | `TentativaRecusadaBancoTest` |
| S4 | Estados que ocupam o horário (D5) | Cancelada, rejeitada e não compareceu liberam; mudar a lista muda a constraint | `SobreposicaoBancoTest` (estados) e D5 do plano |
| S5 | Tempo de vida e transporte do JWT | Define janela de risco e como as telas enviam o token | ADR-0001; **pendente do Pedro** (lista de pendências do grupo) |

## 7. Trade-offs

| # | Trade-off | Ganhamos | Perdemos | Atributos em tensão | Registro |
|---|---|---|---|---|---|
| T1 | Constraint no banco **mais** trigger de trava por recurso | Garantia independente do número de instâncias; resposta de conflito previsível (`23P01`) | Escrita serializada por recurso; dependência do PostgreSQL e de `btree_gist`; **efeito sobre RNF-01 e RNF-03 não medido** | **Integridade (C1) × desempenho (C3)** | ADR-0002, ADR-0007 |
| T2 | Estado copiado para `reserva_recurso` por FK composta | "Ativa" nunca diverge do estado, sem trigger e sem depender do código | Cascata em cada mudança de estado (custo **não medido**); período repetido em cada linha, sem checagem no banco | Integridade × desempenho e manutenibilidade | ADR-0007 |
| T3 | JWT em cada requisição | Casa com JMeter e com teste por perfil | Sem revogação imediata | Segurança × simplicidade | ADR-0001 |
| T4 | Thymeleaf no mesmo projeto | Um deploy; menos peças | Interação rica limitada | Usabilidade × simplicidade | ADR-0003 |
| T5 | Notificação isolada por cliente HTTP | Falha externa não derruba a reserva; testável com WireMock | A notificação pode se perder (aceito: é "simulada") | Disponibilidade × garantia de entrega | ADR-0005 |
| T6 | Flyway imutável | Esquema reproduzível no CI e em clone limpo | Erro em migração mergeada só se corrige com a próxima versão | Manutenibilidade × agilidade | ADR-0006; checagem de CI em `.github/scripts/verificar-migracoes-imutaveis.sh` (PR #34) |

O trade-off decisivo do ciclo é **T1**: a decisão que protege o limitador de maior peso (reserva dupla) é a mesma que pode custar desempenho. Hoje só um lado está medido.

## 8. Riscos, não riscos e ações

### Riscos

| # | Risco | Efeito | Ligado a | Mitigação | Dono |
|---|---|---|---|---|---|
| RK1 | Serviço não traduz `23P01` em 409 e devolve 500 | Quebra RNF-03 e o 409 de C1 | C1, T1, ADR-0007 | Serviço de reserva com a tradução; teste de API de 20 pedidos | Pedro |
| RK2 | Autorização sem matriz nem teste | **Limitador de nota** (C2) | C2 | `docs/seguranca/matriz-autorizacao.md` e teste parametrizado, célula a célula | Pedro |
| RK3 | Trava por recurso degrada p95 ou vazão | RNF-01 e RNF-03 | C3, T1 | Rodada de carga em modo CLI e registro dos números em `docs/performance/` | Leandro (carga), Gustavo (banco) |
| RK4 | `btree_gist` ausente em banco gerenciado ou outra imagem | Migração V1 falha fora do CI | ADR-0006, ADR-0007 | Conferir antes de qualquer deploy; **não conferido** | Gustavo |
| RK5 | Linhas da mesma reserva com períodos diferentes | Reserva incoerente entre sala e professor | T2 | Regra do serviço; decidir se o banco impõe (ponto em aberto do ADR-0007) | Pedro / Gustavo |
| RK6 | Dependências com CVSS crítico: 2 críticos do Spring Framework 6.2.19 sem correção na linha 3.5 | Meta "0 vulnerabilidades críticas" (critério da D4 ainda sem resposta do professor) | N2 | Justificativa em `docs/seguranca/riscos-dependencias.md`; pergunta ao professor | Gustavo |
| RK7 | Fronteira do BDD desatualizada em relação à D1 | Teste e banco discordam | S1 | Atualizar `docs/testes/bdd-aprovacao-bloqueio.md` | Pedro |
| RK8 | Premissas do relatório (D8) sem confirmação, incluindo (d): "professor" no relatório é o docente alocado (consultas do banco) ou o solicitante (spec do Front-end) | Valores do RF-12 sem definição fechada | C1, RF-12 | Confirmar D8 na seção 9 do plano | Grupo |
| RK9 | Quality Gate do SonarCloud vermelho na `main` desde o merge do PR #12 (06/11/2026): `Web:PageWithoutTitleCheck` em páginas de erro (DEF-001); correção no PR #32. **Gate verde na `main` a confirmar** (painel privado; lista de issues não lida) | **Limitador de nota** "pipeline vermelho" | N2 | Reanalisar a `main` e ler o gate; regra 10 do `AGENTS.md` | Gustavo (Sonar), Leandro (DEF-001) |

### Não riscos (com a ressalva do nível)

| # | Afirmação | Evidência | Ressalva |
|---|---|---|---|
| NR1 | O banco recusa sobreposição de sala e de professor e permite reservas adjacentes (`[início, fim)`) | `SobreposicaoBancoTest` | Só no nível do banco; sem API |
| NR2 | Pedidos simultâneos para o mesmo recurso terminam com uma reserva aceita e 19 recusadas por `23P01` | `ConcorrenciaReservaBancoTest` (5 rodadas × 2 cenários) | Uma máquina; não está em run do CI com este número |
| NR3 | A mudança de estado e a auditoria se desfazem juntas | `AuditoriaBancoTest` | Simulação no teste; serviço pendente |

### Ações

| # | Ação | Dono | Prazo sugerido |
|---|---|---|---|
| A1 | Serviço de reserva com tradução de `23P01` em 409 e teste de API de 20 pedidos | Pedro | antes de 11/11 |
| A2 | Matriz de autorização e teste parametrizado | Pedro | antes de 11/11 |
| A3 | Rodada de carga com JMeter (RNF-01 e RNF-03) e `docs/performance/` | Leandro, com apoio do Gustavo | antes de 11/11 |
| A4 | Confirmar D1, D3, D5, D6, D7 e D8 (seção 9 do plano) | Pedro e Leandro | na revisão dos PRs |
| A5 | Atualizar a tabela de fronteiras do BDD com a D1 | Pedro | antes de 11/11 |
| A6 | Perguntar ao professor o critério da D4 | Gustavo | esta semana |
| A7 | Ciclo 02: repetir C1 a C4 com serviço, API e carga; medir C5 | Gustavo | depois de A1 a A3 |

## 9. Limites deste ciclo

- Escrito por uma pessoa, sem sessão com os outros dois integrantes: os "dono" da seção 8 são **proposta**.
- Os cenários C2, C3 e C5 não têm medição; C1 e C4 têm medição **só no nível do banco**, em **uma máquina**, e fora do CI.
- Os ADRs 0005, 0006 e 0007 estão como "proposto"; este ciclo não os aceita. Eles só viram "aceito" na revisão dos PRs.
- As fontes 3 e 5 entraram na `main` com o PR #33 (merge em 09/11/2026); os links agora valem na `main`. As medições citadas continuam sendo as de 07/11/2026, só na máquina do Gustavo: o merge não as repetiu no CI.

## 10. Fontes

1. `src/test/java/br/com/senac/recursos/banco/ConcorrenciaReservaBancoTest.java` e `SobreposicaoBancoTest.java`.
2. [`docs/adr/0007-modelagem-protecao-reserva-dupla.md`](../adr/0007-modelagem-protecao-reserva-dupla.md): 9 de 40 rodadas com deadlock, 17 a 20 s por rodada ruim, 30 de 30 com a trava (06/11/2026, PostgreSQL 16.15 em Docker, uma máquina); mutação `WHERE (false)`, 22 de 41.
3. [`docs/testes/mutacao-manual.md`](../testes/mutacao-manual.md): M1 (24 de 83), M3a e M3b (1 de 3 cada), medidas em 07/11/2026 só nesta máquina.
4. [`docs/plano-execucao.md`](../plano-execucao.md): Etapa 5 (caixas abertas) e seção 9 (D1 a D8).
5. `src/test/java/br/com/senac/recursos/banco/AuditoriaBancoTest.java`.
6. [`docs/seguranca/riscos-dependencias.md`](../seguranca/riscos-dependencias.md): 2 críticos do Spring Framework 6.2.19 (painel do SonarCloud lido em 05/11/2026).
7. [`docs/arquitetura.md`](../arquitetura.md) e [`docs/prd.md`](../prd.md): cenários, utility tree, RNF-01 a RNF-03, personas.
8. `verify` de 07/11/2026 na pilha `feat/banco-auditoria-relatorio`: 510 testes, 0 falhas, 0 ignorados (máquina do Gustavo).
9. `.\mvnw.cmd -B verify` desta branch, depois do merge da `main` (`a6d6374`) em 09/11/2026: 510 testes, 0 falhas, 0 ignorados, "All coverage checks have been met", BUILD SUCCESS (só na máquina do Gustavo; o run do CI do PR é a fonte oficial).
10. [`.github/scripts/verificar-migracoes-imutaveis.sh`](../../.github/scripts/verificar-migracoes-imutaveis.sh) e [`testar-verificacao-migracoes.sh`](../../.github/scripts/testar-verificacao-migracoes.sh): auto-teste de 9 casos, 0 falhas no run do PR #34 (job 129396491011, lido em 09/11/2026); o run da `main` correspondente é o CI #143. Caso "migração existente alterada" provado só pelo auto-teste, não contra a V1 real.
