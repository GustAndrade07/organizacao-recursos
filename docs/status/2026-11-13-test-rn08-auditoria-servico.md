# Status — 13/11/2026 — `test/rn08-auditoria-servico`

**Quem:** feito pelo Leandro (com IA) na área do Pedro (back-end), com o OK do Leandro; **Pedro revisa**. Mesmo combinado do PR #52.

> Este arquivo de status é de uma das partes do PR `test/qualidade-testes-e-openapi-1311`, que junta cinco assuntos da área do Pedro num PR só (combinado com o Leandro em 13/11/2026, para não serem cinco revisões no dia da entrega). O `verify` da ponta do PR tem **1138 testes, 0 falhas, 0 erros, 0 ignorados**.

## O que mudou
`ReservaApiControllerTest` ganhou a classe aninhada **`AuditoriaDeCadaTransicao`**, com 4 testes:

| Teste | O que prova |
|---|---|
| `cancelamentoEntraNoHistoricoComQuemQuandoEMotivo` | cancelar grava quem (`sol`), quando (instante do `Clock`, 05/11/2026 09:00Z), `APROVADA → CANCELADA` e o motivo já sem espaços nas pontas |
| `naoComparecimentoEntraNoHistoricoComQuemQuandoEMotivo` | o mesmo para `APROVADA → NAO_COMPARECEU`, com o Responsável como autor |
| `rejeicaoEntraNoHistoricoComQuemEMotivo` | `SOLICITADA → REJEITADA` com autor e motivo |
| `transicaoRecusadaNaoGravaAuditoria` | depois de cancelar, um segundo cancelamento e um não comparecimento dão 409 e o histórico **continua com 2 registros** (contagem no banco) |

**Por quê:** a RN-08 diz "toda mudança de estado gera registro". Até ontem, criar, aprovar, alterar, retirar e devolver tinham a auditoria conferida; cancelar e não comparecer não. O PR #57 marca a RN-08 como `parcial` exatamente por isso.

**O outro lado da regra:** o teste da transição recusada existe porque "toda mudança gera registro" implica "o que não é mudança não gera". Sem ele, uma implementação que gravasse auditoria antes de validar passaria.

## Verificado
- `./mvnw -B test -Dtest='ReservaApiControllerTest*'`: **45 testes na classe, 0 falhas, 0 erros, 0 ignorados** (13/11/2026, container Linux desta sessão, OpenJDK 21.0.12.1, PostgreSQL 16 em Testcontainers).
- `./mvnw -B verify` completo: resultado na descrição do PR.
- Que os testes **pegam** a regra quebrada: na rodada de mutação do PR irmão (`docs/mutacao-servico`), a mutação **MAU1** (não gravar auditoria em cancelamento e não comparecimento) deixa estes testes vermelhos; sem eles, a mesma mutação passaria despercebida em parte dos casos. Os números estão em `docs/testes/mutacao-manual.md`.

## Não verificado
- Run do CI (só depois do push).
- `docs/rtm.md` e `docs/status-requisitos.md`: atualizados depois do merge do PR #57 na `main` (RN-08 `feito`, `.feature` como evidência de RF-07, RF-08, RN-04, RN-05 e RN-06).
