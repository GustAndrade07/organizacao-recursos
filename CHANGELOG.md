# Changelog

## [Não lançado]
- Estrutura inicial do repositório e documentação base.
- Esqueleto Spring Boot 3.5 (Java 21, Maven Wrapper), PostgreSQL + Flyway, JaCoCo com gate de 80% de linhas / 70% de branches e teste de fumaça com Testcontainers.
- CSS compartilhado das telas de Responsável e Administrador (`app.css`): sete estados da reserva (RN-05), botão de perigo, avisos de sucesso e de informação, ações em linha na tabela e área de texto, com teste dos estados.
- Fragmento `fragments/estado :: badge(estado, testid)` (nome por extenso do estado da reserva vira classe por tabela fechada; desconhecido em cinza) e `envio-unico.js` (`form[data-envio-unico]` não envia duas vezes).
- Design tokens do front-end (`src/main/resources/static/css/tokens.css`), espelhando o arquivo Figma do Design System.
- Documentação do front-end em `docs/frontend/` (tokens e inventário de telas).
- Layout base e menu em Thymeleaf (`layout.html`, `app.css`, `menu.js`), com teste que impede valor solto fora dos tokens.
- Páginas de erro 401, 403, 404 e 500 em Thymeleaf (mais modelo genérico 4xx/5xx), com caminho para continuar e teste de servidor que prova a 404 real.
- Tela de login em Thymeleaf (`GET /login`): formulário acessível e aviso de falha (`/login?erro`); a autenticação continua com o back-end.
- Telas de busca e resultados em Thymeleaf (`GET /recursos/disponiveis`): filtros, tabela no desktop e cards no mobile, estado vazio e idioma pt-BR; a consulta de disponibilidade continua com o back-end.
- Tela de nova reserva em Thymeleaf (`GET /reservas/nova`): formulário preenchido pela busca, aviso de recurso restrito, erros de campo com resumo no topo, conflito e manutenção (409) e envio sem duplicar; a gravação continua com o back-end.
- Telas de fila de aprovação (`GET /aprovacoes`) e de retirada e devolução (`GET /retirada-devolucao`) em Thymeleaf: aprovar e rejeitar com motivo obrigatório, pedido do próprio Responsável sem botões, ações por estado da reserva, "Horário já passou" com confirmação de não compareceu, avisos com o novo estado e estados vazios; diálogos dirigidos pelo servidor (funcionam sem JavaScript). A decisão e o registro continuam com o back-end: as listas só são reais depois da integração (hoje o controller só devolve a view e a tela mostra o estado vazio).
