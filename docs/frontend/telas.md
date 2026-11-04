# Inventário de telas

Os critérios de aceite de cada tela estão na spec do módulo, no Notion ([Specs do Front-end](https://app.notion.com/p/40e8bb977b7b81dbbcbadb904897cbb9)). O design de cada tela está no Figma, página **Telas**. Este arquivo é o mapa técnico: rota, perfil, requisito e prefixo de `data-testid` usado pelos testes E2E.

> **Rotas são proposta** e precisam ser combinadas com o Pedro antes da implementação (controllers do Spring).
> Situação: `figma` (só no Figma) · `html` (template feito) · `e2e` (coberta por teste Playwright).

| # | Tela | Spec | Perfil | Rota proposta | Requisito | Prefixo `data-testid` | Situação |
|---|---|---|---|---|---|---|---|
| 1 | Login | 01 | Todos | `GET /login` | RF-01 | `login-` | — |
| 2 | Layout base e menu | 01 | Todos | (fragmento `layout.html`) | RF-01, RF-13 | `menu-` | `html` |
| 3 | Erro 401 / 403 / 404 / 500 | 01 | Todos | `/error` (fragmentos por status) | RF-13 | `erro-` | — |
| 4 | Buscar disponibilidade | 02 | Solicitante | `GET /recursos/disponiveis` | RF-03 | `busca-` | — |
| 5 | Resultados da busca | 02 | Solicitante | mesma rota, com parâmetros | RF-03 | `resultado-` | — |
| 6 | Nova reserva | 02 | Solicitante | `GET /reservas/nova` | RF-04, RF-05, RF-06 | `reserva-form-` | — |
| 7 | Minhas reservas | 02 | Solicitante | `GET /reservas/minhas` | RF-04 | `minhas-` | — |
| 8 | Detalhe / alterar / cancelar | 02 | Solicitante | `GET /reservas/{id}` | RF-04 | `reserva-` | — |
| 9 | Fila de aprovação | 03 | Responsável | `GET /aprovacoes` | RF-07 | `aprovacao-` | — |
| 10 | Retirada e devolução | 03 | Responsável | `GET /emprestimos` | RF-09 | `emprestimo-` | — |
| 11 | Histórico da reserva | 04 | Responsável, Administrador | `GET /reservas/{id}/historico` | RF-10 | `historico-` | — |
| 12 | Relatório de utilização | 04 | Responsável, Administrador | `GET /relatorios/utilizacao` | RF-12 | `relatorio-` | — |
| 13 | Lista de recursos | 05 | Administrador | `GET /admin/recursos` | RF-02 | `recursos-` | — |
| 14 | Formulário de recurso | 05 | Administrador | `GET /admin/recursos/novo`, `/admin/recursos/{id}` | RF-02 | `recurso-form-` | — |
| 15 | Bloqueio por manutenção | 05 | Administrador | `GET /admin/recursos/{id}/manutencao` | RF-08 | `manutencao-` | — |

A rota do relatório (`/relatorios/utilizacao`) já está definida no [PRD](../prd.md). RF-14 (OpenAPI) é o Swagger UI e não tem tela própria.

## Convenção de `data-testid`

- Formato: `<prefixo><elemento>`, em kebab-case. Exemplos: `login-email`, `login-entrar`, `reserva-form-erro-conflito`, `aprovacao-aprovar-{id}`.
- Coloque em todo elemento que o teste clica, preenche ou verifica (campos, botões, mensagens de erro, badges de estado, linhas de lista).
- Não mude um `data-testid` existente sem atualizar o teste no mesmo PR.

## Larguras obrigatórias

Cada tela é desenhada no Figma e testada em **375 px** (mobile) e **1280 px** (desktop). O E2E do RF-13 roda em 375 px.
