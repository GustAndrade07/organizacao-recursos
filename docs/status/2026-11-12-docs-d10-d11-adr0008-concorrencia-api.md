# Status em 12/11/2026 (quinta, branch `docs/d10-d11-adr0008-concorrencia-api`)

Documentação e dois comentários de Javadoc, a partir da `main` (`986e1cd`). Nenhuma regra de código mudou.

O que muda:
- **D10 decidida (Pedro): fica como está no código.**
  - (a) Reserva sem recurso restrito nasce APROVADA.
  - (b) Cancelar e alterar são só do dono, inclusive para o Administrador.
  - (c) O Responsável pode rejeitar a própria reserva (RN-06 só proíbe aprovar).
  - (d) Não há limite de horário para retirada e para "não compareceu".
- **D11 decidida (Pedro): fica como está no código.** A manutenção não mexe nas reservas que já existem no período; só as novas são recusadas. A tela 15 lista as afetadas e pede confirmação.
- As duas decisões estão na seção 9 do plano, no ADR-0008, no ADR-0009 (linha de pendências), na matriz de autorização, no `status-requisitos.md` e no Javadoc de `ReservaService.criar` e `ManutencaoService`.
- **ADR-0008 passa para aceito.** A condição dele (D10 a D13 decididas) está cumprida; o aceite vale com a aprovação deste PR (regra 6 do `AGENTS.md`).
- **Plano, Etapa 5 (concorrência): caixa marcada.**
  - Correção do status anterior: o run 43276426460, citado em 12/11 como evidência, terminou com `conclusion: failure` no passo "Build, testes e cobertura" (API pública do GitHub). O run que vale é o **43302104277** (`main`, `986e1cd`, verde).
  - Vermelho sem a proteção no nível da API: mutação M1 → `ConcorrenciaReservaApiTest` com 10 de 10 vermelhos (`{201=20}`). Registrado em `docs/testes/mutacao-manual.md`, "Mutações do back-end".
- `status-requisitos.md` e RTM: RF-06 ganha a mutação M1 no nível da API; RF-07 e RF-08 citam a D10 e a D11 como decididas, com as lacunas de teste abaixo (a situação `feito` deles vem da recontagem do Leandro no PR #57, não deste PR).
- Índice de evidências (linha da concorrência) atualizado.
- **Correção do merge da `main` (13/11/2026, pedida na revisão do Leandro):** o merge `8eed58f` tinha resolvido os conflitos com a versão antiga desta branch e desfazia os PRs #56 e #57 em `docs/rtm.md`, `docs/status-requisitos.md` e `docs/plano-execucao.md`. Os três arquivos voltaram à versão da `main` e só as mudanças deste PR foram reaplicadas (caixa da concorrência, D10 e D11, RF-06 com a M1 na API, RF-07, RF-08 e a pendência 6).

**Verificado:**
- `ConcorrenciaReservaApiTest` na `main` (`986e1cd`), máquina do Pedro: 10 testes, 0 falhas. Com a mutação M1: 10 de 10 vermelhos. Migração restaurada, `git status --short` vazio depois da rodada.
- Conclusão dos runs 43276426460 (failure) e 43302104277 (success) pela API pública do GitHub.
- `./mvnw -B verify` antes do commit, máquina do Pedro: 1116 testes, 0 falhas, 0 erros, 0 ignorados; "All coverage checks have been met".
- `./mvnw -B verify` depois da correção do merge (13/11/2026), máquina do Pedro: 1116 testes, 0 falhas, 0 erros, 0 ignorados; "All coverage checks have been met".

**Não verificado:**
- Log por teste do run 43302104277 (pede login). A evidência de CI é o `verify` verde com o teste na suíte.
- D10 (c), Responsável rejeitando a própria reserva: sem teste.
- D10 (d), retirada e "não compareceu" sem limite de horário: sem teste.
- D11 na API: só a tela tem teste (`TelasComBancoTest.bloqueioComReservaNoPeriodoPedeConfirmacaoAntesDeGravar`).
- **Spec do Back-end no Notion: não atualizada** (o conector do Notion não estava autorizado nesta sessão). Fica para o Pedro em até 24 h: seção Arquitetura (API em `/api/**`, camadas, as duas cadeias de segurança, ADR-0008 aceito) e as decisões D10 e D11.
- Os comentários de `web/RecursoController` e `web/RetiradaDevolucaoController` (área do Leandro) ainda citam a D10 e a D11 sem dizer que foram decididas. O conteúdo deles continua certo; não foram alterados.
