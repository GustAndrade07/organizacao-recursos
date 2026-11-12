# Status em 11/11/2026 (quarta, noite, branch `docs/specs-plano-pos-merge-1111`)

Só documentação, a partir da `main` (`2d1da67`), depois dos merges de 11/11 (#36 a #46). Confere se a spec do Banco no Notion e as partes do Gustavo no plano acompanharam esses PRs.

O que muda:
- Plano, Etapa 4, caixa do SonarCloud: o "Falta" de 05/11 (marcar os 2 críticos como Accepted e deixar o projeto Public) já foi feito. O projeto está Public desde 09/11 e os 2 riscos foram aceitos em 11/11 (PR #45). Fica faltando conferir o Quality Gate e os riscos no commit da tag e avaliar o `jaxb-core` 4.0.9 (High). A caixa continua aberta.
- ADR-0006 e ADR-0007: o status continua **proposto**. Ele só ganha a nota de que os PRs desses ADRs foram mergeados e de que o back-end do #44 roda sobre a V1. Vira "aceito" quando o Pedro confirmar por escrito (regra 6 do `AGENTS.md`).
- Spec do Banco de Dados no Notion, atualizada em 11/11/2026:
  - `btree_gist` conferido em `postgres:16-alpine` e `postgres:17` (PR #40), e o ponto em aberto foi fechado.
  - A convenção de `db/consultas/` foi confirmada pelo uso no `RelatorioRepository` (PR #44).
  - O back-end do #44 entrou sem migração nova.
  - A D13 entrou como pendente.
  - Os 965 testes do #44 foram medidos só na máquina do Pedro.

**Não verificado:** a suíte não rodou nesta máquina, porque a mudança é só de texto. Também não foi conferido o run do CI da `main` depois do #44. Ficou de fora a seção "Arquitetura" da spec do Back-end, que é página do Pedro e não foi editada: ela ainda descreve só o pacote `web`. Nenhuma caixa do plano foi marcada.
