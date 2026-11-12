# Status em 12/11/2026, noite (branch `docs/rtm-relatorio-1211`, Leandro)

**Lacunas 3 e 9 do plano:** RTM, `status-requisitos.md` e relatório de qualidade recontados sobre a `main` em `6ce4478`. Bloqueava a tag. Só documentação; nenhum código mudou.

**Base dos números:**
- run **43303372662** (run 221, push na `main`, `verify` verde);
- análise do SonarCloud do mesmo commit (API pública, lida em 12/11/2026);
- `./mvnw -B verify` local de `6ce4478`: **1116 testes, 0 falhas, 0 erros, 0 ignorados**, cobertura de 95,8% das linhas (1464 de 1528) e 81,3% dos branches (422 de 519);
- carga do PR #56 (`docs/performance/resultado-01.md`).

**RTM:**
- As seções "a incorporar" do back-end (PR #44) e das telas (PR #48) viraram linhas. Cada linha cita só classes e métodos que existem, conferidos por script contra os nomes de `src/test/java`.
- Situação recontada:
  - RF: 9 `feito`, 3 `parcial` (RF-02, 03, 12), 2 `ausente` (RF-11, 14);
  - RN: 12 `feito`, 1 `parcial` (RN-08);
  - RNF: 3 `feito` (RNF-01 e RNF-03 só local).
- **Críticos: 2 de 2** (RF-01 e RF-06), contra 0 de 2 em 11/11.
- As mutações feitas à mão foram para uma seção própria.

**`status-requisitos.md`:**
- tabela coerente com a RTM, com linhas de RNF;
- seção **"Pendências e divisão"** preenchida (lacuna 9): 13 itens em ordem de prioridade, com dono e se bloqueiam a tag.

**Relatório de qualidade, versão 2:**
- seções 1 a 8 refeitas sobre `6ce4478`:
  - testes por nível somados dos `TEST-*.xml` (117 de unidade, 83 de banco, 306 de API e serviço, 37 de tela com banco, 511 de contrato de tela, 62 de E2E);
  - metas com evidência e limite;
- 2 defeitos (DEF-001 e DEF-002, corrigidos);
- desvio de autoria dos commits do PR #52 registrado;
- riscos atualizados.
- **Recomendação** (seção 7): "liberar com ressalvas, desde que o clone limpo passe antes da tag; se não passar, bloquear". É **proposta a confirmar pelo Leandro**. A decisão formal vai em `docs/decisao-qualidade.md` (lacuna 2), num PR próprio.

**Verificado:**
- todo nome de teste citado na RTM existe;
- o total por nível soma 1116;
- os 6 últimos pushes na `main` estão verdes (runs 207 a 221, pela API do GitHub);
- o Quality Gate da `main` está OK (0 bugs, 0 vulnerabilidades, 0 hotspots).

**Não verificado:**
- os números no commit da tag (refazer pela seção 8 do relatório);
- o painel de dependências do SonarCloud, que exige login e não foi relido;
- o clone limpo (lacuna 11).

**Depende do PR #56:** as linhas de RNF-01 e RNF-03 citam arquivos de `perf/` e `docs/performance/` que entram com ele. A ordem de merge é o #56 primeiro; depois, merge da `main` nesta branch.

**Specs atualizadas:** Notion, spec principal (KPI "Requisitos com teste citado", critério de aceite da RTM e riscos), 12/11/2026.
