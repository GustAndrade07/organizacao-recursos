# Status em 11/11/2026 (quarta, branch `satoworktree/eloquent-turing-f9j5ey`, PR #42, Leandro)

**Telas 7 e 8.** Minhas reservas (`GET /reservas/minhas`) e Detalhe, alterar e cancelar (`GET /reservas/{id}`) em Thymeleaf, com `MinhasReservasPaginaTest` e `ReservaDetalhePaginaTest`; as 15 telas do inventário agora têm HTML. Nova pergunta em aberto **D9** no plano (quando a reserva ainda pode ser alterada ou cancelada); a tela assume uma proposta e a registra. A listagem, a alteração e o cancelamento continuam com o back-end do Pedro.

**E2E Playwright em 375 px (ADR-0004).** Dependência `com.microsoft.playwright:playwright` 1.63.0 no `pom.xml`, passo "Navegador do E2E" e artifact `e2e-375` no `ci.yml`, e 52 testes em `src/test/java/br/com/senac/recursos/e2e`: aplicação real em porta aleatória com PostgreSQL em Testcontainers e Chromium sem tela; telas com dados de teste em `/e2e/...`, porque o back-end ainda não existe. Estáveis em 3 rodadas seguidas; 5 mutações (RN-01, diálogo, envio único, tabela larga, menu) pegas. Caixa "Interface responsiva mínima" da Etapa 3 marcada.

**RTM completa.** `docs/rtm.md` passa a ter os 30 requisitos do PRD (14 RF, 13 RN, 3 RNF), com risco, teste por camada, evidência e situação: 5 RF, 6 RN e 0 RNF `parcial`; 9 RF, 7 RN e 2 RNF `ausente`; só o RNF-02 `feito` (cobertura local de `3d63d72`: 100,0% das linhas, 106 de 106, e 94,0% dos branches, 79 de 84; o código de produção hoje é só a camada de tela). Dos 2 críticos, o RF-06 tem teste no banco e o RF-01 não tem teste da regra. Todos os testes citados foram conferidos contra o código por script. Caixa "`docs/rtm.md` inicial" da Etapa 2 marcada.

**Relatório de qualidade, versão 1.** `docs/relatorio-qualidade.md` com a estrutura da Aula 13 sobre o commit `e66c000`: 636 testes no CI (500 de tela, 83 de banco, 52 E2E, 1 de fumaça; run 43091027823), cobertura de 100,0% das linhas e 94,0% dos branches (só 106 linhas de produção, todas de tela), 0 de 2 críticos completos na RTM, "0 vulnerabilidades críticas" dependendo da D4, 1 defeito (DEF-001, corrigido), RNF-01 e RNF-03 não medidos. Recomendação: **bloquear** a `v1.0.0-rc1` no estado atual, por causa da autorização (limitador) sem implementação nem teste; a decisão formal vai em `docs/decisao-qualidade.md`, ainda não escrito. A caixa do relatório no plano continua aberta até os números serem refeitos no commit da tag.

**Verificado:** `./mvnw -B verify` com 636 testes, 0 falhas, 0 ignorados, nesta máquina (container de nuvem, `postgres:16` de `mirror.gcr.io`, Chromium 1243 já instalado).

**Verificado no CI:** run 43091027823 (GitHub Actions, commit `3d63d72`) verde com 636 testes, 0 ignorados; o passo "Navegador do E2E" instalou o Chromium 1243 em cerca de 38 s e as 4 classes E2E somaram cerca de 25 s.

**Não verificado:** os fluxos contra o back-end real (login, 409, 401 e 403); larguras de 768 e 1280 px (só à mão); contraste AA de todas as telas.

**Run preso no GitHub (11/11/2026, 20:15 a 20:45 UTC).** O run 43094668839 (commit `67b247b`) terminou todos os passos com sucesso às 20:18:03: navegador em 26 s, "Build, testes e cobertura" em 66 s, SonarCloud em 55 s e "Complete job" com sucesso. Mesmo assim, o GitHub continuou mostrando o job e o run como `in_progress` e recusou as duas saídas pela API: cancelar ("409 Cannot cancel a workflow run that is not in progress") e reexecutar ("403 This workflow is already running"). Os testes e o código não tiveram nenhum papel nisso. O check obrigatório `verify` ficou pendente por causa desse registro, e o run seguinte (deste commit) substitui o preso, pelo `concurrency` com `cancel-in-progress` do `ci.yml`.

**Autoria reescrita (11/11/2026, pedido do Leandro).** Os 10 commits do PR passaram a ter o Leandro como autor e committer, e as linhas de coautoria da ferramenta de IA saíram das mensagens (`git filter-branch` só no intervalo `origin/main..`, com force push). O conteúdo ficou idêntico (diff vazio entre a ponta antiga e a nova), e os 2 merges com a `main` continuam com os mesmos pais na `main`. **Os hashes citados neste arquivo, no relatório de qualidade, na RTM e no plano são os de antes da reescrita**, porque os runs do CI citados como evidência rodaram nesses commits e continuam visíveis no GitHub Actions. Correspondência:

| Antes | Depois | Commit |
|---|---|---|
| `43f8890` | `62dc6cb` | docs(status): registra o run 43094668839 preso como in_progress no GitHub |
| `67b247b` | `8b231b1` | docs(e2e): registra a falha intermitente do print no relatório e no README do front-end |
| `3c573fe` | `18b99f3` | fix(e2e): print de página inteira espera um quadro desenhado antes de capturar |
| `e15199a` | `bf38071` | docs(qualidade): relatório de qualidade, versão 1 (estrutura da Aula 13) |
| `e66c000` | `8b5ec44` | docs(rtm): todos os requisitos do PRD com risco, teste, evidência e situação |
| `4bf214c` | `4ffb1c4` | docs(e2e): tempo do navegador e do E2E no primeiro run do CI (run 43091027823) |
| `3d63d72` | `0891dd8` | chore: merge origin/main e status e changelog no formato da regra 12 |
| `3a8d85a` | `5e39c07` | test(e2e): Playwright em 375 px nas telas e nos fluxos (ADR-0004, RF-13) |
| `c0f21ef` | `ba5b54d` | chore: merge origin/main nas telas 7 e 8 (resolve conflitos em CHANGELOG e plano) |
| `b58380d` | `a81922a` | feat(frontend): telas de minhas reservas e detalhe da reserva (spec 02, RF-04) |

**Fila do CI destravada (11/11/2026, 20:55 UTC).** Os runs 43098174406 e 43098470818 ficaram em `queued` sem criar o job, porque o grupo de concorrência do `ci.yml` (`github.ref`, igual a `refs/pull/42/merge`) continuava ocupado pelo run preso 43094668839, que o GitHub não deixa cancelar. A chave do grupo passou a ser `github.head_ref || github.ref` (o nome da branch no PR). O comportamento continua o mesmo: um run por PR ou branch, e o push novo cancela o anterior. O `ci.yml` é da área do Gustavo; ele precisa revisar essa mudança.

**Ressalva 1 da revisão do Gustavo registrada (11/11/2026).** O Gustavo aprovou com ressalvas (review de 21:27 UTC). A ressalva 1 (telas 7 e 8 sem o `test(red)` antes) entrou em `docs/relatorio-qualidade.md`, seção 5, "Desvio de processo", com o motivo, a evidência (mutação de RN-07: 5 vermelhos) e o limite. As ressalvas 2 (hashes antigos, mapeados acima) e 3 (E2E com dados de teste, já dito na RTM) não pediram mudança. O commit novo derruba a aprovação ("dismiss stale approvals"), e o PR precisa da reaprovação do Gustavo.
