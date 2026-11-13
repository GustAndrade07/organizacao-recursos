# Status — 13/11/2026 — `docs/cookie-sessao-conferido`

**Quem:** Gustavo (com IA), na área do README. Só documentação: nenhum código de produção muda.

## O que mudou
- `README.md`, "Como subir": tabela com o ciclo de vida do cookie de sessão, medido na aplicação rodando.
- `changelog.d/docs-cookie-sessao-conferido.md`.

## Verificado (13/11/2026, `main` `bf2584a`, Windows 11, PostgreSQL do `docker-compose.yml`)
- Aplicação local na porta 8091, por HTTP, conferida com `curl`:
  - `GET /cookies` sem sessão: nenhum `Set-Cookie`.
  - `GET /login`: `Set-Cookie: JSESSIONID=...; Path=/; HttpOnly; SameSite=Lax`, sem `Max-Age` nem `Expires`.
  - `POST /login` com a senha certa: 302 para `/`, com `JSESSIONID` novo (o identificador trocou).
  - Logado, `GET` em `/`, `/admin/recursos`, `/reservas`, `/aprovacoes`, `/usuarios`, `/privacidade`, `/termos`, `/css/app.css` e uma rota inexistente: nenhum `Set-Cookie`; o pote de cookies do `curl` só tem `JSESSIONID`.
  - `POST /api/auth/login`: 200, sem `Set-Cookie`.
  - `POST /logout`: `JSESSIONID=; Max-Age=0; Expires=Thu, 01 Jan 1970 ...` e 302 para `/login`.
  - `GET /reservas` com `JSESSIONID=sessao-antiga`: 302 para `/login?expirou&destino=%2Freservas`.
- Tempo sem uso da sessão: `server.servlet.session.timeout` não está configurado; o padrão do Spring Boot 3.5.16 é 30 minutos (`javap -c` em `org.springframework.boot.web.servlet.server.Session`: `Duration.ofMinutes(30)`).
- `src/main/resources/static`: 0 ocorrências de `localStorage`, `sessionStorage` e `document.cookie`.
- `./mvnw.cmd -B test` com `PoliticasPublicasIntegracaoTest` (11), `PaginaNaoEncontradaIntegracaoTest` (6), `PortaDeEntradaDasTelasTest` (4) e `PoliticasPaginaTest` (17): 38 testes, 0 falhas, 0 erros, 0 ignorados.
- `.\mvnw.cmd -B verify` nesta branch: **1205 testes, 0 falhas, 0 erros, 0 ignorados**, "All coverage checks have been met", `BUILD SUCCESS`.

## Não verificado
- Aplicação atrás de HTTPS de verdade, com `SESSAO_COOKIE_SEGURO=true` (o `Secure` no `Set-Cookie` já foi conferido por HTTP no PR #63).
- A sessão vencer de fato depois de 30 minutos parada (não esperei os 30 minutos; o valor vem da configuração e do padrão do Spring Boot).

## Para o Leandro (área do front-end, fora deste PR)
- `politica-cookies.html` diz que o prazo de 30 minutos "ainda precisa ser confirmado". Está confirmado pelo item acima; a frase pode sair num PR dele.
