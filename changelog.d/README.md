# Entradas do CHANGELOG por PR

Desde 11/11/2026, cada PR escreve a sua entrada do changelog **num arquivo próprio desta pasta**, e não mais direto em `CHANGELOG.md`.

**Por quê:** todos os PRs inseriam uma linha logo abaixo de `## [Não lançado]`. Quando dois PRs ficavam abertos ao mesmo tempo, o segundo dava conflito no `CHANGELOG.md` depois do merge do primeiro. Arquivos diferentes não conflitam.

## Como escrever
- Nome do arquivo: `<branch com "/" trocado por "-">.md`. Exemplo: `docs-status-e-changelog-por-arquivo.md`.
- Conteúdo: uma ou mais linhas no formato de lista do `CHANGELOG.md` (`- O que mudou, para quem lê a versão.`).
- Não editar `CHANGELOG.md` no PR.

## Na versão (tag)
Quem prepara a tag roda `bash .github/scripts/juntar-changelog.sh`. O script copia as entradas para baixo de `## [Não lançado]` no `CHANGELOG.md` e apaga os arquivos. Depois, quem prepara a tag renomeia a seção para a versão. Tudo vai num PR próprio, antes da tag.
