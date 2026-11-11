# Status em 11/11/2026 (quarta, branch `satoworktree/eloquent-turing-f9j5ey`, PR #42, Leandro)

**Telas 7 e 8.** Minhas reservas (`GET /reservas/minhas`) e Detalhe, alterar e cancelar (`GET /reservas/{id}`) em Thymeleaf, com `MinhasReservasPaginaTest` e `ReservaDetalhePaginaTest`; as 15 telas do inventário agora têm HTML. Nova pergunta em aberto **D9** no plano (quando a reserva ainda pode ser alterada ou cancelada); a tela assume uma proposta e a registra. A listagem, a alteração e o cancelamento continuam com o back-end do Pedro.

**E2E Playwright em 375 px (ADR-0004).** Dependência `com.microsoft.playwright:playwright` 1.63.0 no `pom.xml`, passo "Navegador do E2E" e artifact `e2e-375` no `ci.yml`, e 52 testes em `src/test/java/br/com/senac/recursos/e2e`: aplicação real em porta aleatória com PostgreSQL em Testcontainers e Chromium sem tela; telas com dados de teste em `/e2e/...`, porque o back-end ainda não existe. Estáveis em 3 rodadas seguidas; 5 mutações (RN-01, diálogo, envio único, tabela larga, menu) pegas. Caixa "Interface responsiva mínima" da Etapa 3 marcada.

**Verificado:** `./mvnw -B verify` com 636 testes, 0 falhas, 0 ignorados, nesta máquina (container de nuvem, `postgres:16` de `mirror.gcr.io`, Chromium 1243 já instalado).

**Não verificado:** o passo de instalação do navegador no GitHub Actions (só o `--dry-run` local); os fluxos contra o back-end real (login, 409, 401 e 403); larguras de 768 e 1280 px (só à mão); contraste AA de todas as telas.
