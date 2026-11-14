# Status — 13/11/2026, noite — branch `docs/finalizacao-rc1`

PR de finalização da `v1.0.0-rc1`, feito com **Gustavo, Pedro e Leandro juntos**, na máquina do Gustavo. Um commit por dono, com o autor de cada um.

## Quem fez o quê
| Commit | Autor | O que mudou |
|---|---|---|
| `docs(readme): login por e-mail no Windows e porta 5432 ocupada` | Gustavo | README (PowerShell e cmd com `ADMIN_LOGIN` e-mail; porta 5432 ocupada com `DB_PORTA`/`DB_URL`); `docker-compose.yml` lê `DB_PORTA` |
| `docs(qualidade): números do commit da tag, D4, defeitos e declaração do Leandro` | Leandro | Decisão de qualidade e relatório (versão 3): 1366 testes, 96,2%/83,7%, contagem por nível; D4 decidida; CVE-2026-59276 conferido; V2 → V3; DEF-001 a DEF-010; risco 12; aceite do risco 8; PRs por autor até o #73; declaração ditada pelo Leandro |
| `docs(entrega): clone limpo humano do Pedro e teste de dois cliques simultâneos` | Pedro | Checklist, "Terceira rodada": clone limpo seguindo o README, `verify` verde no clone, reserva auditada, 1 reserva e 1 recusa 409 com dois navegadores; prints |
| `docs(qualidade): condição do clone limpo cumprida` | Leandro | Decisão e relatório: "liberar com ressalvas" sem condição pendente |
| `docs: fecha a v1.0.0-rc1 (plano, índice de evidências, CHANGELOG)` | Gustavo | README com os achados do clone; plano com caixas fechadas e o que fica fora da rc1; D14 com OK do Pedro; índice de evidências; `changelog.d/` juntado em `## [1.0.0-rc1]` |

## Decisões do grupo registradas neste PR (13/11/2026, noite)
- D14: **OK do Pedro** (login por e-mail, nome completo, sem conversão dos usuários antigos).
- Risco 8 (limite de login em memória): **aceite do Leandro** na parte das telas.
- Caixas "TDD red" e "TDD green": **fechadas com desvio registrado** (`docs/testes/evidencia-tdd.md`).

## Verificado
- `./mvnw -B verify` local sobre o código de `4ab8075` + README: 1366 testes, 0 falhas, 0 erros, 0 ignorados, `BUILD SUCCESS` (5 min 35 s); JaCoCo 96,2% das linhas e 83,7% dos branches.
- No clone do Pedro (`38cd0ce`): relatórios do `verify` com 1366 testes e 0 falhas; aplicação `UP`; banco conferido por `psql` (reservas, auditoria, tentativa recusada).
- CI da `main` em `4ab8075`: run 295 `success` (API do GitHub).
- `docker compose config` com e sem `DB_PORTA`.

## Não verificado
- Quality Gate do SonarCloud e painel de dependências no commit da tag (o Gustavo lê antes de criar a tag).
- Forma do cmd do README (só PowerShell e bash).
- Clone limpo numa máquina realmente sem nada instalado (foi na máquina do Gustavo).
- Ensaio cronometrado da demo e da prova oral (até 20/11/2026).

## Depois do merge
1. Gustavo: CI e SonarCloud verdes no commit do merge → `git tag -a v1.0.0-rc1 -m "..."` nesse commit → `git push origin v1.0.0-rc1`.
2. Um integrante envia o formulário com os nomes completos, o link do repositório e a tag.
3. Grupo: ensaio cronometrado (`docs/prova-oral/ensaio.md`) até 20/11/2026.
