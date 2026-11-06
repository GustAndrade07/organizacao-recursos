## O que muda
<!-- resumo curto -->

## Requisito / defeito ligado
<!-- RF-xx, RN-xx ou docs/defeitos/DEF-xxx -->

## Plano de execução
<!-- O que mudou em docs/plano-execucao.md (caixas marcadas, status do topo) e nos docs vivos
     (status-requisitos, RTM, CHANGELOG, ADR). Sem mudança no plano? Diga por quê. -->

## Verificado
<!-- O que foi rodado e o resultado. Ex.: ./mvnw -B verify, 21 testes, 0 ignorados -->

## Não verificado
<!-- O que ficou sem conferir e por quê. Se nada, escreva "nada". -->

## Checklist
- [ ] `./mvnw -B verify` passou localmente
- [ ] Testes novos/ajustados (no TDD, commit vermelho veio antes)
- [ ] `docs/plano-execucao.md` atualizado (caixas e status do topo, com a data)
- [ ] RTM e `docs/status-requisitos.md` atualizados, se a mudança tocou requisito
- [ ] Regra nova para `AGENTS.md` (seção 9), se o PR ensinou algo ao grupo
- [ ] Branch atualizada com merge de `origin/main` (sem rebase)
- [ ] Sem token/senha no código
- [ ] Revisor (outra pessoa) designado
