# Índice de evidências da entrega

Este é o ponto de entrada do professor: cada linha diz **qual evidência existe, onde ela está e como conferir**. A versão avaliada é a tag anotada **`v1.0.0-rc1`**, criada no merge do PR de finalização (`docs/finalizacao-rc1`); os links relativos abaixo valem a partir da página da tag no GitHub.

**Estado deste índice:** fechado em 13/11/2026, noite, no PR de finalização, com o grupo reunido (Gustavo fecha o índice; Leandro confere as linhas do relatório e da decisão; Pedro, a do clone limpo). Os números citados vêm do `verify` local sobre o código de `4ab8075` (merge do PR #73), que é o código da tag: o PR de finalização só muda README, `docker-compose.yml` e documentos.

## 1. Pipeline e análise estática
| Evidência | Onde | Como conferir | Estado |
|---|---|---|---|
| Run do CI **no commit da tag** (`verify`: build, testes, JaCoCo) | [Checks do commit da tag](https://github.com/GustAndrade07/organizacao-recursos/commit/v1.0.0-rc1) e workflow [`ci.yml`](../../.github/workflows/ci.yml) | Abrir o commit da tag; os checks `verify` e `SonarCloud Code Analysis` têm de estar verdes | A tag só é criada com os dois verdes (`docs/plano-execucao.md`, etapa 6). Antes dela: run 295 verde em `4ab8075` |
| Testes e cobertura | Artifact `relatorios` do run acima (`target/site/jacoco`); [relatório de qualidade](../relatorio-qualidade.md), seções 3 e 4 | Baixar o artifact e abrir `index.html`; o gate do JaCoCo falha o `verify` abaixo de 80% / 70% | Local, 13/11/2026: **1366 testes, 0 falhas, 0 ignorados; 96,2% das linhas e 83,7% dos branches** |
| Quality Gate do SonarCloud | [Painel do projeto](https://sonarcloud.io/project/overview?id=GustAndrade07_organizacao-recursos) (público) | Quality Gate da análise do commit da tag | Lido pelo Gustavo no commit da tag antes de criá-la |
| Riscos de dependência (CVE) | [riscos-dependencias.md](../seguranca/riscos-dependencias.md) | Painel "Dependency risks" (exige login) e o documento | D4 decidida (leitura mais rígida); 2 Critical do `spring-webmvc` aceitos com justificativa; CVE-2026-59276 conferido, não atinge (seção 8.1) |
| Migração não editável depois do merge | [`ci.yml`](../../.github/workflows/ci.yml) e [`verificar-migracoes-imutaveis.sh`](../../.github/scripts/verificar-migracoes-imutaveis.sh) | Passo "Migrações existentes não podem ser alteradas" nos runs de PR | Na `main` desde o PR #34 |
| CI vermelho proposital e recuperação | [README](../../README.md), "Histórico de CI vermelho proposital" | Run #120 (vermelho) e o seguinte (verde) do PR #30 | Na `main` |

## 2. Requisitos e rastreabilidade
| Evidência | Onde | Estado |
|---|---|---|
| PRD (RF, RN, RNF) | [prd.md](../prd.md) | Na tag |
| RTM (requisito → risco → teste → evidência) | [rtm.md](../rtm.md) | RF: 10 `feito`, 3 `parcial` (RF-02, 03, 12), 1 `ausente` (RF-11); RN: 13 `feito`; RNF: 3 `feito` |
| Status dos requisitos | [status-requisitos.md](../status-requisitos.md) | Na tag |

## 3. Testes
| Evidência | Onde | Estado |
|---|---|---|
| Plano de testes e de TDD | [plano-de-teste.md](../plano-de-teste.md), [plano-tdd.md](../testes/plano-tdd.md) | Na tag |
| Evidência de TDD (`test(red)` → `feat(green)` → `refactor`) | [evidencia-tdd.md](../testes/evidencia-tdd.md) | Ciclos com hash e os desvios registrados |
| BDD (Cucumber, roda no `verify`) | [bdd-aprovacao-bloqueio.md](../testes/bdd-aprovacao-bloqueio.md) e `src/test/resources/features/aprovacao-bloqueio.feature` | 13 cenários |
| Mutação manual | [mutacao-manual.md](../testes/mutacao-manual.md) | Banco, API, serviço (8), relatório (MR1 a MR5), D10/D11 (3), telas |
| Concorrência (20 pedidos → 1 aceito, 19 recusados) | `ConcorrenciaReservaBancoTest` (banco), `ConcorrenciaReservaApiTest` (HTTP) | Na tag |
| Matriz de autorização | [matriz-autorizacao.md](../seguranca/matriz-autorizacao.md) e `MatrizAutorizacaoTest` (234 testes no `verify` de 13/11) | Na tag |
| E2E em 375 px (Playwright) | [e2e-375/](e2e-375/) | 77 testes E2E no `verify` de 13/11 |

## 4. Desempenho
| Evidência | Onde | Estado |
|---|---|---|
| RNF de desempenho (escrito antes da rodada) | [rnf.md](../performance/rnf.md) | Na tag |
| Plano de carga (JMeter, CLI) | `perf/reservas.jmx`, [perf/README.md](../../perf/README.md) | Na tag |
| Resultado | [resultado-01.md](../performance/resultado-01.md), `perf/resultados/` | p95 17 e 8 ms; 80 × 201 e 3920 × 409; só local |

## 5. Arquitetura
| Evidência | Onde | Estado |
|---|---|---|
| Arquitetura e matriz requisito → decisão → componente → evidência | [arquitetura.md](../arquitetura.md) | Na tag |
| ADRs | [docs/adr/](../adr/) (0001 a 0010) | Na tag |
| Avaliação arquitetural (ATAM, ciclo 01) | [ciclo-01.md](../avaliacao-arquitetural/ciclo-01.md) | Na tag |
| Esquema do banco consolidado (V1 + V2 de login) | [schema.sql](../banco/schema.sql) | Na tag |

## 6. Defeitos, relatório, decisão e entrega
| Evidência | Onde | Estado |
|---|---|---|
| Registro de defeitos | [docs/defeitos/](../defeitos/) | 10 registrados: 3 corrigidos, 1 em parte, 6 abertos (média/baixa, nenhum nos limitadores) |
| Relatório de qualidade | [relatorio-qualidade.md](../relatorio-qualidade.md) | Versão 3, 13/11/2026 |
| Decisão de qualidade e declarações | [decisao-qualidade.md](../decisao-qualidade.md) | Liberar com ressalvas; 12 riscos aceitos com responsável; declarações de Gustavo, Pedro e Leandro |
| Checklist de entrega e clone limpo | [checklist.md](../entrega/checklist.md), [roteiro-clone-limpo.md](../entrega/roteiro-clone-limpo.md) | Clone limpo humano feito pelo Pedro em 13/11/2026 |
| Prova oral e demo | [perguntas.md](../prova-oral/perguntas.md), [roteiro-demo.md](../prova-oral/roteiro-demo.md), [ensaio.md](../prova-oral/ensaio.md) | Perguntas e roteiro prontos; ensaio cronometrado até 20/11/2026 |

## 7. Depois da tag
Correção depois da tag vai para a `v1.0.0-rc2`. Tag já usada nunca é movida.
