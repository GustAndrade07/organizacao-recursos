## O que muda
<!-- resumo curto -->

## Requisito / defeito ligado
<!-- RF-xx, RN-xx ou docs/defeitos/DEF-xxx -->

## Plano de execução
<!-- Caixas marcadas em docs/plano-execucao.md, arquivo de status em docs/status/ e docs vivos
     (status-requisitos, RTM, ADR). Changelog em changelog.d/<branch>.md, nunca direto no CHANGELOG.md.
     Sem arquivo de status? Diga por quê. -->

## Specs atualizadas
<!-- Link da spec do Notion alterada neste PR (ou, no máximo em 24 h, a combinar aqui), ou
     "não se aplica, porque ...". Obrigatório se a mudança toca requisito, regra de negócio,
     rota, modelo de dados, decisão ou estado de entrega. -->

## Verificado
<!-- O que foi rodado e o resultado. Ex.: ./mvnw -B verify, 21 testes, 0 ignorados -->

## Não verificado
<!-- O que ficou sem conferir e por quê. Se nada, escreva "nada". -->

## Checklist
- [ ] `./mvnw -B verify` passou localmente
- [ ] Testes novos/ajustados (no TDD, commit vermelho veio antes)
- [ ] Caixas de `docs/plano-execucao.md` atualizadas e status em `docs/status/AAAA-MM-DD-<branch>.md` (sem parágrafo novo no topo do plano)
- [ ] Entrada em `changelog.d/<branch>.md` (sem editar `CHANGELOG.md`)
- [ ] RTM e `docs/status-requisitos.md` atualizados, se a mudança tocou requisito
- [ ] Spec do Notion da minha área atualizada (ou linha "Specs atualizadas" explica por que não se aplica)
- [ ] Regra nova para `AGENTS.md` (seção 9), se o PR ensinou algo ao grupo
- [ ] Branch atualizada com merge de `origin/main` (sem rebase)
- [ ] Sem token/senha no código
- [ ] Revisor (outra pessoa) designado
