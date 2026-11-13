# Status — 13/11/2026 — `docs/clone-limpo-windows-1311`

**Quem:** IA (Claude Code) na máquina e na sessão do Pedro, a pedido dele. Revisão: Gustavo (dono do README).

## O que mudou
- `docs/entrega/roteiro-clone-limpo.md`: seção 4.2, a rodada 2 do clone limpo, em **Windows 11** e com a forma `$env:` do README, no commit `bf2584a`.
- `docs/entrega/checklist.md`: seção "Segunda rodada" e o bloqueador 6.
- `docs/status-requisitos.md`: pendência 1.

## O que a rodada achou
1. **Bloqueador do README nesta máquina:** a porta 5432 já estava em uso por um PostgreSQL 18 nativo (serviço `postgresql-x64-18`). O `docker compose up -d` não deu erro, mas a aplicação falou com o PostgreSQL nativo e parou no Flyway com `autenticação do tipo senha falhou para o usuário "recursos"`. O README não avisa disso. A correção é do Gustavo, em PR separado (regra do roteiro: aqui só se anota).
2. Com o contorno (container na 5433 e `$env:DB_URL`), a aplicação subiu e o fluxo de ponta a ponta passou, inclusive a ligação **telas ↔ API** no mesmo banco (detalhe no roteiro, seção 4.2).
3. A forma `$env:` do README funcionou. A forma do `cmd` não foi testada.

## Verificado
- Os passos e respostas da seção 4.2 do roteiro, em 13/11/2026, só nesta máquina.
- Código igual ao da `main` `bf2584a` (só documentação muda); `./mvnw -B verify` desse código mais os 8 testes da branch `test/d10-d11-api-1311`: 1213 testes, 0 falhas, 0 ignorados (13/11/2026, esta máquina).

## Não verificado
- A conferência **humana** da Aula 14: continua pendente (o Pedro repete os passos e assina a linha do checklist).
- O serviço `postgresql-x64-18` **não** foi parado nem alterado.
