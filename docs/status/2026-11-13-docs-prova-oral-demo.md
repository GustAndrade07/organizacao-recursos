# Status — 13/11/2026 — `docs/prova-oral-demo`

**Quem:** Leandro (com IA), com o OK do Leandro; **Pedro revisa** (área do front-end e da apresentação).

**Lacuna fechada:** a 12 da tabela "Lacunas para a entrega", na parte que não é humana (o ensaio continua pendente).

## O que mudou
- `docs/prova-oral/perguntas.md`: das 4 perguntas de rascunho para **15**, cada uma com o arquivo da resposta, a descrição de uma resposta fraca e quem responde. Seções novas: o que ficou de fora e por quê, quais arquivos citados entraram nos PRs de hoje, e a conferência de distribuição preenchida.
- `docs/prova-oral/roteiro-demo.md` (novo): os 8 minutos.
- `docs/plano-execucao.md`: lacuna 12 e as duas caixas da Etapa 6 (as duas **continuam abertas**, porque dependem do ensaio).

## Como as perguntas foram montadas
- Cada pergunta aponta para um arquivo ou commit **que existe** — conferido um a um no repositório em 13/11/2026 (`git grep`, não de memória). As perguntas 2, 4, 11 e 12 citam arquivos que entram nos PRs desta mesma leva; está escrito no arquivo que, se algum não for mergeado, a pergunta perde a fonte.
- A distribuição foi conferida contra a divisão de áreas da seção 6 do plano: **nenhum integrante responde sobre a própria área**.
- Duas ideias viraram "ficaram de fora", com o motivo: a do token do provedor (RF-11 não existe) e as que dependem das decisões D8 e D9, ainda abertas — pergunta sobre ponto não decidido vira discussão, não avaliação.

## Verificado
- Os arquivos citados nas 15 perguntas existem (ou está dito de qual PR vêm).
- `./mvnw -B verify` nesta branch: **1116 testes, 0 falhas, 0 erros, 0 ignorados**, `BUILD SUCCESS`. **Este PR não muda código nem teste.**

## O script e o passo a passo foram rodados
Em 13/11/2026, contra a aplicação do clone limpo (commit `9acbbcd`), no container desta sessão:

| O que | Resultado |
|---|---|
| `demo.sh` inteiro | criou Ana e Bruno e os 4 recursos (ids 2 a 5), sem erro, com `set -euo pipefail` |
| Passo 2 da demonstração: Ana reserva a Sala C237 em 18/11, 10:00–12:00 | **201**, estado `APROVADA` |
| Passo 3: mesma sala, mesmo horário | **409** |
| Passo 4: Ana reserva o Laboratório de redes, 14:00–16:00 | **201**, estado `SOLICITADA` (restrito) |
| Passo 5: Bruno aprova | **200**, estado `APROVADA` |
| Passo 7: histórico | dois registros, com `ana` criando e `bruno` aprovando, cada um com instante e motivo |

## Não verificado
- **O ensaio.** Não foi feito e é humano: os tempos do roteiro são alvo, não medida, e `docs/prova-oral/ensaio.md` continua vazio de propósito.
- Os passos 1, 6 e 8 da demonstração (busca pela tela, aprovar a própria reserva, manutenção) **pelas telas**: têm teste automatizado, mas não foram clicados nesta conferência.
- O roteiro **na máquina da apresentação** (outra máquina, outro sistema).
- Quem fala cada parte: a sugestão do roteiro segue as áreas invertidas, mas o grupo decide.
