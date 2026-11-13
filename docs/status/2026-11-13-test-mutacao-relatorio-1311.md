# Status — 13/11/2026 — `test/mutacao-relatorio-1311`

**Quem:** Pedro (back-end), com IA (Claude Code). Revisão: Gustavo.

## O que mudou
- `RelatorioInstantaneoTest` (novo, 1 teste): uma reserva confirmada em outra transação entre a consulta das horas e a da carga por professor não pode entrar em só uma das partes do relatório. Prende o `REPEATABLE READ` de `RelatorioService`.
- `docs/testes/mutacao-manual.md`: seção "Mutações do serviço do relatório" (MR1 a MR5) e a lacuna da MR4.
- `docs/rtm.md` (RF-12) e `docs/decisao-qualidade.md` (risco 11).

**Sem `test(red)`:** o comportamento já existia; o teste nasceu verde. A prova de que ele não é trivial é a MR4: sem ele, 0 vermelhos; com ele, 1.

## Verificado
- Mutações: tabela em `mutacao-manual.md` (281 testes por rodada, só nesta máquina).
- `RelatorioInstantaneoTest` verde 3 vezes seguidas.
- `./mvnw -B verify` da ponta da branch (com a `main` `5e50d70`): **1358 testes, 0 falhas, 0 erros, 0 ignorados**, cobertura atendida, 4 min 20 s (13/11/2026, esta máquina).

## Não verificado
- Mutações com a suíte inteira (descartada; motivo na seção da mutação).
- Run do CI.
