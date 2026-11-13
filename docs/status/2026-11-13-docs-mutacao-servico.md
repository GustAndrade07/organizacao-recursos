# Status — 13/11/2026 — `docs/mutacao-servico`

**Quem:** feito pelo Leandro (com IA) na área do Pedro (regras do serviço), com o OK do Leandro; **Pedro revisa**.

**Lacuna fechada:** a 6 da tabela "Lacunas para a entrega" (aprovação, manutenção e autorização estavam como "pendente (Pedro)").

## O que mudou
`docs/testes/mutacao-manual.md` ganhou a seção **"Mutações do serviço"**, com 8 mutações, e a tabela das seis regras críticas saiu de três "pendente" para todas feitas. O cabeçalho do arquivo deixou de dizer que ele tem "só a seção do banco".

## Resultado (1120 testes na linha de base, 0 falhas)

| Mutação | Regra quebrada | Vermelhos |
|---|---|---|
| MA1 | ninguém aprova a própria reserva (RN-06) | 4 |
| MA2 | recurso restrito nasce SOLICITADA (RF-07) | 11 |
| MM1 | manutenção bloqueia reserva nova (RN-04) | 5 |
| MM2 | manutenção bloqueia alteração (RN-04) | 1 |
| MM3 | manutenção usa `[início, fim)` (D1) | 1 |
| MZ1 | só o Responsável registra retirada, devolução e não comparecimento | 10 |
| MZ2 | só o dono altera, cancela e vê (posse) | 18 |
| MAU1 | toda mudança de estado gera auditoria (RN-08) | 3 **com** os testes novos da RN-08; **0 sem eles** |

## O que a rodada ensinou (está escrito no arquivo)
1. **Lacuna real achada pela MAU1:** até hoje, apagar a auditoria do cancelamento e do não comparecimento não deixava nenhum teste vermelho (0 de 1116). É a justificativa do PR irmão `test/rn08-auditoria-servico`.
2. **MZ1 só quebrou a matriz:** nenhum teste de negócio percebeu que a operação passou a valer para o perfil errado. A matriz é o que segura a autorização.
3. **MM3 só quebrou a linha "começa exatamente no fim"** da trinca de fronteiras — sem ela, a mutação passaria.
4. **MM2 tem um único teste** separando a regra na alteração de reserva: registrado como ponto frágil.
5. **MA1 mostrou** que o `solicitanteNaoAprovaAPropriaReserva` prova o perfil, não a posse: o Solicitante é barrado antes.

## Verificado
- As 9 rodadas (base + 8 mutações) e a rodada extra sem os testes da RN-08, cada uma com a suíte inteira, em 13/11/2026, no container desta sessão. Números acima.
- **Nada ficou mutado:** `git status --short` vazio na árvore das mutações, em `src/main` e em `src/test`.
- `./mvnw -B verify` nesta branch: **1116 testes, 0 falhas, 0 erros, 0 ignorados**, `BUILD SUCCESS`. **Este PR só muda documentação.**

## Não verificado
- As mutações **não** foram repetidas no CI (nem precisam: a suíte é a mesma).
- O serviço do relatório não foi mutado (**não medido**), e o RF-11 não existe para ser mutado.
- Os logs das rodadas ficaram na pasta temporária da sessão, não versionados: para conferir, repetir a quebra descrita e o comando da seção "Como foi medido".
