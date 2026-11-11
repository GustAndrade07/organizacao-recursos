# Status em 11/11/2026 (quarta, branch `satoworktree/eloquent-turing-f9j5ey`, PR #42, Leandro)

**Telas 7 e 8.** Minhas reservas (`GET /reservas/minhas`) e Detalhe, alterar e cancelar (`GET /reservas/{id}`) em Thymeleaf, com `MinhasReservasPaginaTest` e `ReservaDetalhePaginaTest`; as 15 telas do inventário agora têm HTML. Nova pergunta em aberto **D9** no plano (quando a reserva ainda pode ser alterada ou cancelada); a tela assume uma proposta e a registra. A listagem, a alteração e o cancelamento continuam com o back-end do Pedro.

**E2E Playwright em 375 px (ADR-0004).** Dependência `com.microsoft.playwright:playwright` 1.63.0 no `pom.xml`, passo "Navegador do E2E" e artifact `e2e-375` no `ci.yml`, e 52 testes em `src/test/java/br/com/senac/recursos/e2e`: aplicação real em porta aleatória com PostgreSQL em Testcontainers e Chromium sem tela; telas com dados de teste em `/e2e/...`, porque o back-end ainda não existe. Estáveis em 3 rodadas seguidas; 5 mutações (RN-01, diálogo, envio único, tabela larga, menu) pegas. Caixa "Interface responsiva mínima" da Etapa 3 marcada.

**RTM completa.** `docs/rtm.md` passa a ter os 30 requisitos do PRD (14 RF, 13 RN, 3 RNF), com risco, teste por camada, evidência e situação: 5 RF, 6 RN e 0 RNF `parcial`; 9 RF, 7 RN e 2 RNF `ausente`; só o RNF-02 `feito` (cobertura local de `3d63d72`: 100,0% das linhas, 106 de 106, e 94,0% dos branches, 79 de 84; o código de produção hoje é só a camada de tela). Dos 2 críticos, o RF-06 tem teste no banco e o RF-01 não tem teste da regra. Todos os testes citados foram conferidos contra o código por script. Caixa "`docs/rtm.md` inicial" da Etapa 2 marcada.

**Verificado:** `./mvnw -B verify` com 636 testes, 0 falhas, 0 ignorados, nesta máquina (container de nuvem, `postgres:16` de `mirror.gcr.io`, Chromium 1243 já instalado).

**Verificado no CI:** run 43091027823 (GitHub Actions, commit `3d63d72`) verde com 636 testes, 0 ignorados; o passo "Navegador do E2E" instalou o Chromium 1243 em cerca de 38 s e as 4 classes E2E somaram cerca de 25 s.

**Não verificado:** os fluxos contra o back-end real (login, 409, 401 e 403); larguras de 768 e 1280 px (só à mão); contraste AA de todas as telas.
