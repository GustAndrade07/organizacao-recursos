# Status — 13/11/2026 — `docs/defeitos-teste-usuario-1311`

**Quem:** quem abrir o PR, com IA; a IA só escreveu os arquivos. Cada área decide o que fazer com os defeitos da sua parte (Gustavo: banco; Pedro: back-end; Leandro: front-end).

## O que mudou
- `docs/defeitos/DEF-003.md` a `DEF-010.md`: oito defeitos achados num teste como usuário, com Playwright, em 13/11/2026, na `main` em `bc40183`. Nenhum está corrigido; cada um traz o teste de regressão **proposto**.
- Nenhum código, teste, migração ou configuração mudou.

| Defeito | Resumo | Severidade | Depende de decisão do grupo? |
|---|---|---|---|
| DEF-003 | Reserva em data passada é aceita | média | sim (regra não está no PRD) |
| DEF-004 | Cadastro de sala exige localização e descarta o valor | média | não: V2 (Gustavo) ou tirar o "obrigatório" |
| DEF-005 | Campos de texto aceitam emoji (login já recusa) | média | sim (quais campos; regra pedida em conversa) |
| DEF-006 | Nome de recurso duplicado é aceito | baixa a média | sim |
| DEF-007 | Retirada e devolução antes do horário da reserva | baixa a média | sim |
| DEF-008 | Login bloqueado mostra "senha incorreta" | baixa | sim (segurança × usabilidade) |
| DEF-009 | URL inexistente logado dá 403; favicon 404 | baixa | não |
| DEF-010 | Login é nome de usuário; o usuário decidiu que tem que ser e-mail (com `@`, obrigatório) | média | **decidido pelo usuário do teste; falta alinhar com o grupo** (Pedro: RF-01; Gustavo: migração V2; Leandro: telas) |

DEF-003, DEF-005, DEF-006, DEF-007 e DEF-008 tocam ponto que o PRD não decide: entram como **proposta** e pergunta em aberto, não como política (regra 4 da seção 9 do `AGENTS.md`).

## Como foi testado
Aplicação local com JDK 21 e PostgreSQL 16 portáteis (sem Docker), banco vazio, administrador e usuários de teste criados pela API; Chromium via Playwright, nos perfis Solicitante, Responsável e Administrador, em 390, 768 e 1280 px. Passaram sem defeito: sobreposição de reserva (total, parcial e horário encostado), fluxo de recurso restrito até "Concluída" com histórico, alteração em conflito, matriz de autorização (4 perfis × 19 rotas), limite de tentativas do login, redirecionamento aberto, XSS no nome de recurso e ausência de rolagem horizontal.

## Verificado
- Cada defeito foi reproduzido na aplicação local; os passos estão em cada arquivo.
- `grep` mostra que nenhum teste nem o CI lê `docs/defeitos/`.

## Não verificado
- `./mvnw -B verify`: **não rodado**, porque esta máquina não tem Docker (os testes usam Testcontainers). O PR só tem documentação; o CI roda o `verify` no PR.
- Emoji em motivo de rejeição, em motivo de manutenção, na API e na senha.
- Manutenção de recurso, relatório com dados, "não compareceu", edição de usuário, Firefox e WebKit.
- `docs/status-requisitos.md` e `docs/rtm.md` **não** mudaram: nenhum requisito mudou de situação. Quando um defeito for corrigido, a correção atualiza os dois.
- Caixas de `docs/plano-execucao.md`: nenhuma marcada ou ajustada.
- Um ponto informativo, fora dos defeitos: a tela reserva um recurso por vez, enquanto a API aceita até 10 por reserva (sala e professor exigem duas reservas na tela).
