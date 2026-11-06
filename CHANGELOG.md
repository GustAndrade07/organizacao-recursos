# Changelog

## [Não lançado]
- Estrutura inicial do repositório e documentação base.
- Esqueleto Spring Boot 3.5 (Java 21, Maven Wrapper), PostgreSQL + Flyway, JaCoCo com gate de 80% de linhas / 70% de branches e teste de fumaça com Testcontainers.
- Design tokens do front-end (`src/main/resources/static/css/tokens.css`), espelhando o arquivo Figma do Design System.
- Documentação do front-end em `docs/frontend/` (tokens e inventário de telas).
- Layout base e menu em Thymeleaf (`layout.html`, `app.css`, `menu.js`), com teste que impede valor solto fora dos tokens.
- Páginas de erro 401, 403, 404 e 500 em Thymeleaf (mais modelo genérico 4xx/5xx), com caminho para continuar e teste de servidor que prova a 404 real.
- Tela de login em Thymeleaf (`GET /login`): formulário acessível e aviso de falha (`/login?erro`); a autenticação continua com o back-end.
- Menu do Responsável: o item "Empréstimos" passa a se chamar "Retirada e devolução", igual ao título da tela 10; rota proposta `/retirada-devolucao` e prefixo `retirada-devolucao-` no inventário de telas.
