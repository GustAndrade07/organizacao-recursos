# Status por PR

Desde 11/11/2026, cada PR registra o seu status **num arquivo próprio desta pasta**, e não mais num parágrafo novo no topo de `docs/plano-execucao.md`.

**Por quê:** todos os PRs inseriam o parágrafo no mesmo ponto do plano. Dois PRs abertos ao mesmo tempo sempre davam conflito: assim que um entrava na `main`, o outro precisava ser resolvido à mão (foi o caso do PR #37 com o #36 em 11/11). Arquivos diferentes não conflitam.

## Como escrever
- Nome do arquivo: `AAAA-MM-DD-<branch com "/" trocado por "-">.md`. Exemplo: `2026-11-11-docs-status-e-changelog-por-arquivo.md`.
- Conteúdo: o mesmo que ia no parágrafo **Status em …** do plano. Data, branch, o que mudou, o que foi verificado, o que **não** foi verificado e as caixas marcadas no plano.
- Atualizar o status do mesmo PR = editar o arquivo dele, nunca criar outro.
- As caixas das etapas continuam em `docs/plano-execucao.md`. Só o parágrafo de status saiu de lá.

## Ordem de leitura
A ordem cronológica é a ordem alfabética dos nomes (a data vem primeiro). Os parágrafos de status até 09/11/2026 continuam no topo do plano, como histórico.
