# Checklist de entrega (meta interna 11/11 · prazo 13/11)

> Formato do professor (Aula 14): **item → existe → atualizado → caminho**.
> Deve ser preenchido a partir de um **clone limpo**, seguindo só o README, por quem **não** escreveu o README.
> Nesta etapa só se anota; nada é corrigido. No fim, listar os bloqueadores em ordem.
> Estado abaixo: 04/11/2026, repositório ainda sem código.

| Item | Existe | Atualizado | Caminho |
|---|---|---|---|
| Repositório público com revisão por pares (PRs revisados) | não | não | (link do GitHub) |
| Aplicação executável (sobe seguindo o README) | não | não | `README.md` |
| README e documentação da API (OpenAPI) | parcial | não | `README.md` |
| PRD com RF, RN e RNF numerados | sim | parcial | `docs/prd.md` |
| RTM (requisito → risco → teste → evidência) | parcial | não | `docs/rtm.md` |
| Status dos requisitos | sim | sim | `docs/status-requisitos.md` |
| Diagramas: contexto, contêineres/componentes e sequências críticas | parcial | não | `docs/arquitetura.md` (esqueleto de 05/11, a validar) |
| ADRs (JWT, anti reserva dupla, Thymeleaf, Playwright) | parcial | não | `docs/adr/` |
| Plano de testes | parcial | não | `docs/plano-de-teste.md` (esqueleto de 05/11) e `docs/testes/plano-tdd.md` (inventário, a fazer) |
| Evidência de TDD (commits red → green → refactor) | não | não | `docs/testes/evidencia-tdd.md` |
| BDD (cenários e `.feature`) | parcial | não | `docs/testes/bdd-aprovacao-bloqueio.md` |
| Mutação manual das regras críticas | não | não | `docs/testes/mutacao-manual.md` |
| Matriz de autorização | não | não | `docs/seguranca/matriz-autorizacao.md` |
| Pipeline de CI (verde) | parcial | não | `.github/workflows/ci.yml` |
| Relatório do JaCoCo (≥ 80% linhas / 70% branches) | não | não | artifact do CI / `target/site/jacoco` |
| Quality Gate do SonarCloud | parcial | parcial | https://sonarcloud.io/project/overview?id=GustAndrade07_organizacao-recursos (Passed na `main` em 05/11; projeto precisa estar Public; riscos de dependência em `docs/seguranca/riscos-dependencias.md`) |
| Plano de carga (JMeter) | não | não | `perf/reservas.jmx` |
| RNF de desempenho (escrito antes da rodada) | não | não | `docs/performance/rnf.md` |
| Resultado do teste de carga | não | não | `docs/performance/resultado-01.md` + `perf/resultados/rodada-01.jtl` |
| Registro de defeitos | parcial | não | `docs/defeitos/` |
| Relatório final de qualidade (fonte de cada número) | sim (versão 1, 11/11/2026, PR #42) | não (refazer os números no commit da tag) | `docs/relatorio-qualidade.md` |
| Banco de perguntas e ensaio da prova oral | parcial | não | `docs/prova-oral/perguntas.md`, `docs/prova-oral/ensaio.md` |
| Apresentação e demonstração do sistema (entregável da especificação; roteiro de 8 min com demo ensaiada) | não | não | `docs/prova-oral/ensaio.md` (a demo é a etapa 3 do roteiro) |

## Entregáveis finais
- [ ] Tag anotada `v1.0.0-rc1` com CI verde **no commit da tag** (se corrigir depois: `rc2`; nunca mover tag já usada)
- [ ] `CHANGELOG.md` com o que entra na versão (entradas de `changelog.d/` juntadas com `bash .github/scripts/juntar-changelog.sh`, num PR antes da tag)
- [ ] `docs/evidencias/README.md`: índice com link do CI no commit da tag, JaCoCo (artifact), Quality Gate, `resultado-01.md`, `rtm.md`, `defeitos/` e `relatorio-qualidade.md`; nenhum link quebrado
- [ ] `docs/decisao-qualidade.md`: liberar / liberar com ressalvas / bloquear; critérios e evidência de cada um; riscos aceitos **com impacto, justificativa e responsável**; declaração de cada integrante
- [ ] Formulário de entrega enviado por **um** integrante: nomes completos (até 4) e link do repositório público

## Bloqueadores (em ordem; os 4 primeiros zeram a nota)
1. Aplicação não executa
2. Pipeline ausente / vermelho
3. Falha de autorização
4. Possibilidade de reserva dupla
5. (demais, após o clone limpo)

## Conferido sem clone (07/11/2026)
**Isto não substitui o clone limpo.** A tabela acima continua com o estado de 04/11 e só será refeita por quem fizer o clone (ver o [roteiro](roteiro-clone-limpo.md); **o clone ainda não foi feito**). Aqui está apenas o que se conferiu sem clone, e a fonte de cada linha.

**Existência de arquivos na `main`** (`git ls-tree -r origin/main`, commit `009a4be`, 07/11/2026; só diz se o arquivo existe, **não** se está atualizado nem correto):

| Arquivo | Na `main` |
|---|---|
| `README.md`, `docker-compose.yml`, `CHANGELOG.md` | existem |
| `.github/workflows/ci.yml` | existe (o resultado dos runs **não** foi conferido aqui) |
| `docs/prd.md`, `docs/rtm.md`, `docs/status-requisitos.md`, `docs/arquitetura.md`, `docs/plano-de-teste.md`, `docs/plano-execucao.md` | existem |
| `docs/adr/0001` a `0006` | existem (0005 e 0006 como "proposto") |
| `docs/testes/bdd-aprovacao-bloqueio.md`, `docs/seguranca/riscos-dependencias.md` | existem |
| `docs/prova-oral/perguntas.md`, `docs/prova-oral/ensaio.md` | existem |
| `docs/testes/plano-tdd.md`, `docs/testes/evidencia-tdd.md`, `docs/testes/mutacao-manual.md` | **não existem** |
| `docs/seguranca/matriz-autorizacao.md` | **não existe** |
| `perf/reservas.jmx`, `docs/performance/rnf.md`, `docs/performance/resultado-01.md` | **não existem** |
| `docs/decisao-qualidade.md`, `docs/evidencias/README.md` | **não existem** (o `docs/relatorio-qualidade.md` entrou na versão 1 em 11/11/2026, PR #42) |
| `docs/avaliacao-arquitetural/ciclo-01.md`, `docs/banco/schema.sql` | **não existem** (em PRs abertos) |
| Arquivos `.feature` (BDD), OpenAPI/Swagger | **não existem** |
| `docs/defeitos/` | só `TEMPLATE.md` (nenhum defeito registrado) |

**Pontos que o README da `main` tem hoje e que o clone deve confirmar:** o README diz que "sem Docker os testes são ignorados"; o PR #28 corrige isso (teste sem Docker falha). A seção "Histórico de CI vermelho proposital" do README está vazia na `main`; o PR #30 a preenche. Os dois PRs estão abertos, sem aprovação de pessoa, em 07/11/2026.

**Não conferido:** se a aplicação sobe seguindo o README, o `verify` em máquina limpa, o resultado dos runs do CI na `main`, o Quality Gate atual e qualquer coluna "Atualizado".
