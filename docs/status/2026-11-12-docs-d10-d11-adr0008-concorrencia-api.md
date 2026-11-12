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
- `status-requisitos.md`: RF-06 passa a **feito**. RF-07 e RF-08 continuam **parcial**, pelas lacunas abaixo.
- RTM (RF-06) e índice de evidências (linha da concorrência) atualizados.

**Verificado:**
- `ConcorrenciaReservaApiTest` na `main` (`986e1cd`), máquina do Pedro: 10 testes, 0 falhas. Com a mutação M1: 10 de 10 vermelhos. Migração restaurada, `git status --short` vazio depois da rodada.
- Conclusão dos runs 43276426460 (failure) e 43302104277 (success) pela API pública do GitHub.
- `./mvnw -B verify` antes do commit, máquina do Pedro: 1116 testes, 0 falhas, 0 erros, 0 ignorados; "All coverage checks have been met".

**Não verificado:**
- O log por teste do run 43302104277 (pede login). A evidência de CI é o `verify` verde com o teste na suíte.
- Não há teste para D10 (c), Responsável rejeitando a própria reserva, nem para D10 (d), não comparecer sem limite de horário. A D11 tem teste só na tela (`TelasComBancoTest.bloqueioComReservaNoPeriodoPedeConfirmacaoAntesDeGravar`), não na API.
- **Spec do Back-end no Notion: não atualizada** (o conector do Notion não estava autorizado nesta sessão). Fica para o Pedro em até 24 h: seção Arquitetura (API em `/api/**`, camadas, as duas cadeias de segurança, ADR-0008 aceito) e as decisões D10 e D11.
- Os comentários de `web/RecursoController` e `web/RetiradaDevolucaoController` (área do Leandro) ainda citam a D10 e a D11 sem dizer que foram decididas. O conteúdo deles continua certo; não foram alterados.
