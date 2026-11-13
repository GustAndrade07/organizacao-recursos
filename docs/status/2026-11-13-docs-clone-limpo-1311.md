# Status — 13/11/2026 — `docs/clone-limpo-1311`

**Quem:** feito pelo Leandro (com IA) na área do Gustavo (README e entrega), com o OK do Leandro; **Gustavo revisa**.

## O que mudou
- `docs/entrega/roteiro-clone-limpo.md`: seções 2 e 4 preenchidas com a rodada de 13/11/2026 (ambiente, commit, hora de cada passo, texto dos erros, o que faltou no README, bloqueadores).
- `docs/entrega/checklist.md`: tabela "item → existe → atualizado → caminho" refeita no commit `9acbbcd`, seção nova com a rodada e os limites dela, e a ressalva de que quem rodou foi a IA.
- `README.md`: "Como subir" passa a subir com `ADMIN_LOGIN`/`ADMIN_SENHA` e explica por quê; a seção da API deixa de repetir a criação do administrador; o aviso "não validado em clone limpo" vira o registro do que foi validado e do que continua pendente.
- `docs/plano-execucao.md`: lacuna 11 e a caixa da Etapa 6 atualizadas; **a caixa continua aberta** (falta a conferência humana).

## Como foi medido
Container Linux novo (Ubuntu 24.04, kernel 6.18.44, 4 CPUs, 15 GB), OpenJDK 21.0.12.1, Docker Engine 29.9.2, sem `~/.m2` e sem nada compilado. Clone de `https://github.com/GustAndrade07/organizacao-recursos.git` no commit `9acbbcd`, seguindo só o `README.md`.

| Passo | Resultado |
|---|---|
| `docker compose up -d` | contêiner `postgres:16` de pé em 11,2 s |
| `./mvnw spring-boot:run` | subiu na 3ª tentativa: as duas primeiras pararam no download de dependências (falha de rede do container, não do projeto; os artefatos existem no Maven Central, conferido com `curl`). `Started RecursosApplication in 5.829 seconds` |
| `/actuator/health` | `{"status":"UP"}` |
| API | `POST /api/auth/login` 200; `POST /api/recursos` 201; `POST /api/reservas` **201**; o pedido que sobrepõe **409**; cancelamento 200 |
| Telas | login pelo formulário, `/reservas/nova`, reserva gravada e exibida como Aprovada (Playwright, Chromium) |
| `./mvnw -B verify` | `BUILD SUCCESS`, `Tests run: 1116, Failures: 0, Errors: 0, Skipped: 0`, 3 min 52 s |
| JaCoCo | 95,8% de linhas (1464/1528), 81,3% de branches (422/519) — gate de 80/70 atendido |

## Verificado
- Todos os passos acima, nesta máquina, no commit `9acbbcd`.
- `./mvnw -B verify` na branch deste PR: **1116 testes, 0 falhas, 0 erros, 0 ignorados**, `BUILD SUCCESS`. O PR não muda código nem teste; foi rodado assim mesmo.

## Não verificado
- O README em **Windows/PowerShell** (`.\mvnw.cmd`): esta rodada foi em Linux.
- O estado depois dos merges dos PRs #56, #57, #58 e #59: a rodada é do `9acbbcd`.
- O Quality Gate do SonarCloud no commit da tag (continua a cargo do Gustavo).
- Se o grupo aceita a rodada da IA no lugar da conferência humana da Aula 14.
