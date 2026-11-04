# Banco de perguntas da prova oral

> Regras (Aula 13): 15 perguntas **específicas do projeto**, nunca genéricas. Cada uma aponta o arquivo onde está a resposta e descreve como seria uma **resposta fraca**.
> **Não escrever as respostas.** Distribuir para que cada um responda sobre o que **não** escreveu.
> Os caminhos abaixo são os previstos no plano; confirmar quando os arquivos existirem.
> Linhas 1 a 4 são rascunho a partir dos exemplos do professor.

| # | Pergunta | Arquivo da resposta | Resposta fraca | Quem responde |
|---|---|---|---|---|
| 1 | Qual mecanismo impede a dupla reserva e qual teste prova isso? | `docs/adr/0002-anti-reserva-dupla.md`, teste de concorrência | Diz "tem um teste de concorrência" sem citar a constraint nem a contagem feita no banco | Pedro |
| 2 | Por que houve mock em um ponto e Testcontainers em outro? | `docs/testes/` (plano), testes de unidade vs. repositório | Diz "o professor mandou", sem relacionar mock a teste unitário isolado e banco real a persistência | Leandro |
| 3 | O que o p95 medido permite ou não permite afirmar? | `docs/performance/resultado-01.md`, `perf/resultados/rodada-01.jtl` | Generaliza para produção a partir de máquina local; não separa 409 esperado de erro real | Gustavo |
| 4 | Qual teste falha se a aprovação de restrito for removida? | `src/test/resources/features/aprovacao-bloqueio.feature`, `docs/testes/mutacao-manual.md` | Não sabe indicar o teste nem mostrar a mutação | Pedro / Gustavo |
| 5 | | | | |
| 6 | | | | |
| 7 | | | | |
| 8 | | | | |
| 9 | | | | |
| 10 | | | | |
| 11 | | | | |
| 12 | | | | |
| 13 | | | | |
| 14 | | | | |
| 15 | | | | |

## Ideias para completar (cada uma precisa virar pergunta com arquivo e resposta fraca)
- Como o Solicitante é impedido de mexer na reserva de outro? Qual célula da matriz prova?
- Como a auditoria evita registro órfão em um rollback?
- Como foi calculada à mão uma linha do relatório de utilização?
- Qual commit mostra o vermelho de um cenário TDD?
- O que a cobertura de 80% não garante?
- Por que o token do provedor externo não vaza na resposta?

## Conferência de distribuição
| Integrante | Perguntas que responde | Sobre áreas que não escreveu? |
|---|---|---|
| Gustavo | | |
| Pedro | | |
| Leandro | | |
