# Changelog

## [Não lançado]
- Estrutura inicial do repositório e documentação base.
- Esqueleto Spring Boot 3.5 (Java 21, Maven Wrapper), PostgreSQL + Flyway, JaCoCo com gate de 80% de linhas / 70% de branches e teste de fumaça com Testcontainers.
- Design tokens do front-end (`src/main/resources/static/css/tokens.css`), espelhando o arquivo Figma do Design System.
- Documentação do front-end em `docs/frontend/` (tokens e inventário de telas).
- Layout base e menu em Thymeleaf (`layout.html`, `app.css`, `menu.js`), com teste que impede valor solto fora dos tokens.
- Pranchas da seção 04 (histórico da reserva e relatório de utilização) em `docs/frontend/pranchas/secao-04/`, em 375 px e 1280 px, para aprovação antes da implementação.
- `AGENTS.md` (e `CLAUDE.md` apontando para ele): contexto e regras do grupo e da IA, incluindo a regra de que todo PR atualiza o plano; modelo de PR com as seções "Plano de execução", "Verificado" e "Não verificado".
- Plano de execução atualizado (status de 04/11, SonarCloud feito) e ajustado com os pontos das Aulas 09 a 15 reconferidas no Hub do professor; item "Apresentação e demonstração" entra no checklist de entrega.
- Páginas de erro 401, 403, 404 e 500 em Thymeleaf (mais modelo genérico 4xx/5xx), com caminho para continuar e teste de servidor que prova a 404 real.
- Tela de login em Thymeleaf (`GET /login`): formulário acessível e aviso de falha (`/login?erro`); a autenticação continua com o back-end.
