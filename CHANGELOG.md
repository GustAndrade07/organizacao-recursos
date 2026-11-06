# Changelog

## [Não lançado]
- Estrutura inicial do repositório e documentação base.
- Esqueleto Spring Boot 3.5 (Java 21, Maven Wrapper), PostgreSQL + Flyway, JaCoCo com gate de 80% de linhas / 70% de branches e teste de fumaça com Testcontainers.
- CSS compartilhado das telas de Responsável e Administrador (`app.css`): sete estados da reserva (RN-05), botão de perigo, avisos de sucesso e de informação, ações em linha na tabela e área de texto, com teste dos estados.
- Design tokens do front-end (`src/main/resources/static/css/tokens.css`), espelhando o arquivo Figma do Design System.
- Documentação do front-end em `docs/frontend/` (tokens e inventário de telas).
- Layout base e menu em Thymeleaf (`layout.html`, `app.css`, `menu.js`), com teste que impede valor solto fora dos tokens.
- Páginas de erro 401, 403, 404 e 500 em Thymeleaf (mais modelo genérico 4xx/5xx), com caminho para continuar e teste de servidor que prova a 404 real.
- Tela de login em Thymeleaf (`GET /login`): formulário acessível e aviso de falha (`/login?erro`); a autenticação continua com o back-end.
- Telas de busca e resultados em Thymeleaf (`GET /recursos/disponiveis`): filtros, tabela no desktop e cards no mobile, estado vazio e idioma pt-BR; a consulta de disponibilidade continua com o back-end.
- Tela de nova reserva em Thymeleaf (`GET /reservas/nova`): formulário preenchido pela busca, aviso de recurso restrito, erros de campo com resumo no topo, conflito e manutenção (409) e envio sem duplicar; a gravação continua com o back-end.
- Login (`GET /login`): o e-mail digitado volta preenchido depois de uma falha (a senha nunca), aviso "Sua sessão expirou" em `/login?expirou`, campo oculto `destino` para voltar à página anterior (regra `CaminhoDoSite`, também usada nas páginas de erro; o 401 leva a `/login?expirou`) e botão Entrar desabilitado com "Entrando…" durante o envio; a autenticação continua com o back-end.
