# Changelog

## [Não lançado]
- Estrutura inicial do repositório e documentação base.
- Esqueleto Spring Boot 3.5 (Java 21, Maven Wrapper), PostgreSQL + Flyway, JaCoCo com gate de 80% de linhas / 70% de branches e teste de fumaça com Testcontainers.
- CSS compartilhado das telas de Responsável e Administrador (`app.css`): sete estados da reserva (RN-05), botão de perigo, avisos de sucesso e de informação, ações em linha na tabela e área de texto, com teste dos estados.
- Fragmento `fragments/estado :: badge(estado, testid)` (nome por extenso do estado da reserva vira classe por tabela fechada; desconhecido em cinza) e `envio-unico.js` (`form[data-envio-unico]` não envia duas vezes).
- Design tokens do front-end (`src/main/resources/static/css/tokens.css`), espelhando o arquivo Figma do Design System.
- Documentação do front-end em `docs/frontend/` (tokens e inventário de telas).
- Layout base e menu em Thymeleaf (`layout.html`, `app.css`, `menu.js`), com teste que impede valor solto fora dos tokens.
- `AGENTS.md` (e `CLAUDE.md` apontando para ele): contexto e regras do grupo e da IA, incluindo a regra de que todo PR atualiza o plano; modelo de PR com as seções "Plano de execução", "Verificado" e "Não verificado".
- Plano de execução atualizado (status de 04/11, SonarCloud feito) e ajustado com os pontos das Aulas 09 a 15 reconferidas no Hub do professor; item "Apresentação e demonstração" entra no checklist de entrega.
- Páginas de erro 401, 403, 404 e 500 em Thymeleaf (mais modelo genérico 4xx/5xx), com caminho para continuar e teste de servidor que prova a 404 real.
- Tela de login em Thymeleaf (`GET /login`): formulário acessível e aviso de falha (`/login?erro`); a autenticação continua com o back-end.
- `docs/plano-execucao.md`: nova seção 9 "Decisões pendentes do grupo" (D1 intervalo semiaberto, D2 tentativa recusada em transação própria, D3 código HTTP de transição inválida, D4 critério de "0 vulnerabilidades críticas"), com quem decide, prazo, impacto e proposta.
- Dependências atualizadas por risco no SonarCloud (Tomcat 10.1.61, Jackson 2.22.4, PostgreSQL 42.7.14, Logback 1.5.38, log4j 2.25.5, jsoup 1.22.2); templates Thymeleaf incluídos na análise do Sonar; `docs/seguranca/riscos-dependencias.md` com os riscos e as justificativas dos 2 críticos do Spring Framework 6.2.19 sem correção na linha 3.5.
- `docs/arquitetura.md` (esqueleto): C4 em Mermaid, sequências críticas, utility tree, cenários, trade-offs e riscos; `docs/plano-de-teste.md` (esqueleto): níveis, política de mock, metas e responsabilidades. Pacotes, rotas e níveis são proposta a validar com Pedro e Leandro.
- ADRs: seção "Consequências" (ganhamos, perdemos, exige de testes) completa nos ADRs 0001 a 0004; ADR-0005 (notificação por cliente HTTP isolado) e ADR-0006 (Flyway como único dono do esquema) propostos; `TEMPLATE.md` ganha os três itens de consequência, "Pendências" e "Fonte". `docs/arquitetura.md` liga os dois ADRs na tabela de trade-offs. Decisões do plano registradas em 05/11 (Gustavo): D1 intervalo semiaberto, D2 tentativa recusada em transação própria, D3 409 para transição inválida e D5 (nova) estados que ocupam o horário; D4 (critério de vulnerabilidade crítica) segue aguardando o professor.
- Telas de busca e resultados em Thymeleaf (`GET /recursos/disponiveis`): filtros, tabela no desktop e cards no mobile, estado vazio e idioma pt-BR; a consulta de disponibilidade continua com o back-end.
- Tela de nova reserva em Thymeleaf (`GET /reservas/nova`): formulário preenchido pela busca, aviso de recurso restrito, erros de campo com resumo no topo, conflito e manutenção (409) e envio sem duplicar; a gravação continua com o back-end.
- Sonar: scripts em `static/js` fora da cobertura de código novo (o JaCoCo só mede Java), para o gate de 80% não reprovar telas com JavaScript.
