# Índice de evidências da entrega

Este é o ponto de entrada do professor: cada linha diz **qual evidência existe, onde ela está e como conferir**. Na entrega, todos os links apontam para o **commit da tag `v1.0.0-rc1`** (`docs/entrega/checklist.md`, "Entregáveis finais").

**Estado deste índice:** esqueleto escrito em 11/11/2026 a partir da `main` (`8b18dd9`), **antes da tag**. As linhas "a preencher na tag" só ganham link quando a tag existir. Arquivo que ainda não está na `main` fica em `código` e não vira link, para não haver link quebrado. Quem fecha o índice na tag é o Gustavo (dono da tag e do README), com o Leandro conferindo as linhas do relatório e da decisão.

## 1. Pipeline e análise estática
| Evidência | Onde | Como conferir | Estado em 11/11/2026 |
|---|---|---|---|
| Run do CI **no commit da tag** (`verify`: build, testes, JaCoCo) | GitHub Actions, workflow [`ci.yml`](../../.github/workflows/ci.yml) | Abrir o run do commit da tag; o check `verify` tem de estar verde | **a preencher na tag** (link do run) |
| Relatório do JaCoCo (≥ 80% linhas, ≥ 70% branches) | Artifact `relatorios` do run acima (`target/site/jacoco`) | Baixar o artifact e abrir `index.html`; o gate do JaCoCo falha o `verify` abaixo da meta | **a preencher na tag** (link do artifact) |
| Quality Gate do SonarCloud | [Painel do projeto](https://sonarcloud.io/project/overview?id=GustAndrade07_organizacao-recursos) (público) | Quality Gate da análise do commit da tag | `main` em 11/11: OK, 0 vulnerabilidades, Security A |
| Riscos de dependência (CVE) | [riscos-dependencias.md](../seguranca/riscos-dependencias.md) | Painel "Dependency risks" (exige login) e o documento | 21 riscos com 2 Critical na leitura de 11/11 (seção 4 do documento); a marcação dos 2 Critical como Accepted em 11/11 é registrada no PR #45 (D4 pendente) |
| Migração não editável depois do merge | Passos do [`ci.yml`](../../.github/workflows/ci.yml) e [`verificar-migracoes-imutaveis.sh`](../../.github/scripts/verificar-migracoes-imutaveis.sh) | Passo "Migrações existentes não podem ser alteradas" nos runs de PR | na `main` desde o PR #34 |
| CI vermelho proposital e recuperação | [README](../../README.md), seção "Histórico de CI vermelho proposital" | Run #120 (vermelho) e o run seguinte (verde) do PR #30 | na `main` |

## 2. Requisitos e rastreabilidade
| Evidência | Onde | Estado em 11/11/2026 |
|---|---|---|
| PRD (RF, RN, RNF) | [prd.md](../prd.md) | na `main` |
| RTM (requisito → risco → teste → evidência) | [rtm.md](../rtm.md) | na `main`, parcial; versão completa no PR #42 (Leandro) |
| Status dos requisitos (feito / parcial / ausente, com teste citado) | [status-requisitos.md](../status-requisitos.md) | na `main` |

## 3. Testes
| Evidência | Onde | Estado em 11/11/2026 |
|---|---|---|
| Plano de testes | [plano-de-teste.md](../plano-de-teste.md) | na `main` |
| BDD (cenários) | [bdd-aprovacao-bloqueio.md](../testes/bdd-aprovacao-bloqueio.md) | na `main`; `.feature` (Cucumber) **não existe** |
| Mutação manual das regras críticas | [mutacao-manual.md](../testes/mutacao-manual.md) | na `main`, seção do banco |
| Evidência de TDD (commits `test(red)` → `feat(green)` → `refactor`) | `docs/testes/evidencia-tdd.md` | **não existe** |
| Concorrência (20 pedidos → 1 aceito, 19 recusados, contagem no banco) | `ConcorrenciaReservaBancoTest` (banco, na `main`); `ConcorrenciaReservaApiTest` (HTTP, PR #44) | nível banco na `main`; nível API em PR aberto |
| Matriz de autorização (teste parametrizado) | `docs/seguranca/matriz-autorizacao.md` e `MatrizAutorizacaoTest` | no PR #44 (Pedro), ainda não na `main` |
| E2E em 375 px (Playwright) | `docs/evidencias/e2e-375/` | no PR #42 (Leandro), ainda não na `main` |

## 4. Desempenho
| Evidência | Onde | Estado em 11/11/2026 |
|---|---|---|
| RNF de desempenho (escrito **antes** da rodada) | `docs/performance/rnf.md` | **não existe** |
| Plano de carga (JMeter, modo CLI) | `perf/reservas.jmx`; instruções em [perf/README.md](../../perf/README.md) | plano **não existe** |
| Resultado da rodada | `docs/performance/resultado-01.md` e `perf/resultados/rodada-01.jtl` | **não existe** |

## 5. Arquitetura
| Evidência | Onde | Estado em 11/11/2026 |
|---|---|---|
| Arquitetura e matriz requisito → decisão → componente → evidência | [arquitetura.md](../arquitetura.md) | na `main` |
| ADRs | [docs/adr/](../adr/) (0001 a 0007; 0008 proposto no PR #44) | na `main` |
| Avaliação arquitetural (ATAM, ciclo 01) | [ciclo-01.md](../avaliacao-arquitetural/ciclo-01.md) | na `main`, rascunho sem sessão com Pedro e Leandro |
| Esquema do banco consolidado | [schema.sql](../banco/schema.sql) | na `main` |

## 6. Defeitos, relatório e decisão
| Evidência | Onde | Estado em 11/11/2026 |
|---|---|---|
| Registro de defeitos | [docs/defeitos/](../defeitos/) ([DEF-001](../defeitos/DEF-001.md)) | 1 defeito registrado e corrigido |
| Relatório final de qualidade (fonte de cada número) | `docs/relatorio-qualidade.md` | no PR #42 (Leandro), ainda não na `main` |
| Decisão de qualidade (liberar / ressalvas / bloquear, riscos aceitos) | `docs/decisao-qualidade.md` | **não existe** (Leandro); precisa trazer os 2 Critical aceitos (D4) |
| Checklist de entrega e clone limpo | [checklist.md](../entrega/checklist.md), [roteiro-clone-limpo.md](../entrega/roteiro-clone-limpo.md) | na `main`; clone limpo **não feito** |
| Prova oral | [perguntas.md](../prova-oral/perguntas.md), [ensaio.md](../prova-oral/ensaio.md) | na `main`, parcial |

## 7. Como fechar este índice na tag
1. Depois que a tag `v1.0.0-rc1` existir, trocar cada "a preencher na tag" pelo link real (run, artifact, análise do SonarCloud do commit da tag).
2. Para cada arquivo em `código` que tiver entrado na `main`, trocar pelo link relativo.
3. Conferir que nenhum link está quebrado: abrir cada link a partir da página da tag no GitHub.
4. Correção depois da tag vai para a `v1.0.0-rc2`. Tag já usada nunca é movida.
