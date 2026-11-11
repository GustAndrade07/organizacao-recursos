# Status em 11/11/2026 (quarta, branch `docs/status-e-changelog-por-arquivo`)

Só processo e documentação, a partir da `main` (`dfc5c93`). Os PRs davam conflito porque todos inseriam texto no mesmo ponto de dois arquivos. O primeiro é o parágrafo **Status em …** no topo de `docs/plano-execucao.md`, alterado em 14 dos últimos 40 merges da `main`. O segundo é a linha logo abaixo de `## [Não lançado]` no `CHANGELOG.md`, alterada em 31 desses 40. Em 11/11, 6 das 7 branches abertas mexiam no `CHANGELOG.md` e 5 no plano. O PR #37 ficou em conflito com o #36 por isso.

O que muda:
- Status de cada PR num arquivo próprio em `docs/status/`, como este. Os parágrafos antigos do topo do plano ficam como histórico.
- Entrada de changelog de cada PR num arquivo próprio em `changelog.d/`, juntada no `CHANGELOG.md` antes da tag por `.github/scripts/juntar-changelog.sh`.
- `.gitattributes`: `CHANGELOG.md merge=union`. Vale só para merge **local** (`git merge origin/main`): enquanto houver branch aberta no formato antigo, o Git junta as linhas sozinho. O botão "Update branch" do GitHub ignora essa configuração.
- `AGENTS.md` (seção 4, regra 12 na seção 9), modelo de PR, seção 10 do plano e checklist de entrega descrevem o formato novo.

Verificado: o script de junção foi rodado nesta máquina numa cópia do `CHANGELOG.md` com duas entradas. As duas foram para baixo de `## [Não lançado]`, os arquivos foram apagados e o CRLF ficou igual. **Não verificado:** nenhuma checagem de CI obriga o formato novo; quem confere é a revisão do PR. As branches abertas no formato antigo ainda vão conflitar uma vez e precisam mover o status e a entrada para os arquivos novos ao atualizar. Nenhuma caixa do plano foi marcada.
