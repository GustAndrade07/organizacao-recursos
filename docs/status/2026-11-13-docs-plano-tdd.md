# Status — 13/11/2026 — `docs/plano-tdd`

**Quem:** feito pelo Leandro (com IA) na área do Pedro (testes do back-end), com o OK do Leandro; **Pedro revisa**.

> Este arquivo de status é de uma das partes do PR `test/qualidade-testes-e-openapi-1311`, que junta cinco assuntos da área do Pedro num PR só (combinado com o Leandro em 13/11/2026, para não serem cinco revisões no dia da entrega). O `verify` da ponta do PR tem **1138 testes, 0 falhas, 0 erros, 0 ignorados**.

**Lacuna fechada:** a 7 da tabela "Lacunas para a entrega".

## O que mudou
- `docs/testes/plano-tdd.md` (novo): 12 seções (reserva e sobreposição, reserva dupla, aprovação, manutenção, transições, alterar/cancelar, auditoria, autorização, relatório, cadastros, notificação e o resumo), cada caso ligado ao **teste que existe hoje**, com a classe e o método.
- `docs/plano-execucao.md`: lacuna 7 e a caixa da Etapa 2.

## Como foi montado
Os nomes de classe e de método saíram de `grep` nos arquivos de teste da `main` (`9acbbcd`), não de memória. Caso sem teste está escrito como **falta**, com o nome: Responsável rejeitar a própria reserva (D10 (c)), RF-03 completo (depende da V2) e o RF-11 inteiro. Onde a regra depende de decisão do grupo (D8, D9) está escrito "pendente", **sem inventar política** (regra 6 do `AGENTS.md`).

Os itens marcados com ⁺ vêm dos PRs de 13/11/2026 desta leva (auditoria de cancelamento e não comparecimento, cenários BDD, mutações do serviço). Se algum não for mergeado, a linha correspondente perde a evidência.

## Verificado
- Cada classe e método citados existem na `main` ou no PR indicado (conferido com `grep`).
- `./mvnw -B verify` nesta branch: **1116 testes, 0 falhas, 0 erros, 0 ignorados**, `BUILD SUCCESS`. **Este PR não muda código nem teste.**

## Não verificado
- Se algum teste citado cobre a regra **de fato** (isso é o que a mutação mede, e a mutação está no PR irmão `docs/mutacao-servico`).
- `docs/rtm.md` não foi tocado de propósito: o PR #57 o reescreve.
