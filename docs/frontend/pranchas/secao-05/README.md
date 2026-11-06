# Pranchas da seção 05 · Cadastro e manutenção

Esta pasta guarda os desenhos das telas 13, 14 e 15 do [inventário](../../telas.md), feitos antes da implementação para aprovação. Cada quadro é um HTML comum que usa o CSS real do projeto (`tokens.css` e `app.css`, em `src/main/resources/static/css`) e o `menu.js` do layout, mais dois CSS extras: `secao02.css`, herdado da seção 02, e `secao05.css`, que traz o que é novo aqui (abas por tipo, situação do recurso, campos do formulário). Os dois extras só têm as regras que algum quadro usa e vão para o `app.css` na implementação. Os dados são de exemplo, o menu é o do Administrador (Recursos e Relatórios) e cada quadro existe em 375 px e 1280 px. Os `data-testid` seguem os prefixos `recursos-`, `recurso-form-` e `manutencao-` do inventário.

## Como abrir

Abra `index.html` a partir de uma cópia do repositório: ele mostra cada tela em 375 e 1280 lado a lado. Os caminhos dos CSS e do `menu.js` são relativos à raiz do repositório, então a pasta precisa continuar em `docs/frontend/pranchas/secao-05/`. Cada quadro também abre sozinho, por exemplo `FormErros-375.html`. Todos os quadros têm o sufixo `-375` ou `-1280`.

## Quadros, telas e requisitos

| Quadro | Tela | Requisito | O que mostra |
|---|---|---|---|
| `Lista-375.html` e `Lista-1280.html` | 13 · Lista de recursos | RF-02 | Abas Salas, Materiais e Professores, busca por nome, situação Ativo ou Em manutenção mais a etiqueta Restrito, botões Editar e Manutenção |
| `ListaSalvo-*` | 13 · Lista de recursos | RF-02 | Aviso de confirmação depois de salvar o recurso (`role="status"`) |
| `ListaBloqueado-*` | 13 · Lista de recursos | RF-08 | Aviso de confirmação depois de bloquear o recurso (`role="status"`) |
| `ListaProfessores-*` | 13 · Lista de recursos | RF-02 | Aba Professores, sem situação restrita nem em manutenção e sem o botão Manutenção |
| `ListaVazia-*` | 13 · Lista de recursos | RF-02 | Lista sem recursos, com a ação "Cadastrar o primeiro recurso" |
| `FormSala-*` | 14 · Formulário de recurso | RF-02 | Campos de sala, com "Recurso restrito (exige aprovação)" explicado |
| `FormMaterial-*` | 14 · Formulário de recurso | RF-02 | Campos de material, com o mesmo campo de recurso restrito |
| `FormProfessor-*` | 14 · Formulário de recurso | RF-02 | Campos de professor |
| `FormErros-*` | 14 · Formulário de recurso | RF-02 | Resumo dos erros com `role="alert"` e a mensagem junto de cada campo |
| `Manutencao-*` | 15 · Bloqueio por manutenção | RF-08, RN-04 | Início, fim e motivo do bloqueio |
| `ManutencaoTermino-*` | 15 · Bloqueio por manutenção | RF-08, RN-04 | Erro quando o fim é antes do início, junto do campo (`role="alert"`) |
| `ManutencaoAfetadas-*` | 15 · Bloqueio por manutenção | RF-08, RN-04 | Aviso das reservas que caem no período, antes de confirmar (`role="alert"`) |

A RN-04 diz que recurso em manutenção não pode ser reservado; por isso a lista marca o recurso como "Em manutenção" e o bloqueio mostra o período.

## O que foi assumido

A aba Materiais segue o mesmo padrão da de Salas e não tem quadro. O formulário de edição é o mesmo preenchido, com o botão "Salvar alterações", e também não tem quadro. A lista e o formulário de professor não têm situação restrita nem em manutenção, e o botão Manutenção não aparece, porque professor não é reservável por enquanto. No navegador usado para capturar os quadros, as datas do campo nativo apareceram no formato americano; em um navegador em português ficam dia/mês/ano.

## Decisões pendentes

Falta o back-end, com Pedro e Leandro, decidir se professor é recurso reservável. Também falta definir o que acontece com as reservas afetadas por um bloqueio (cancelar, manter ou notificar): a spec só manda avisar, e é isso que o quadro `ManutencaoAfetadas` mostra. As rotas `GET /admin/recursos`, `GET /admin/recursos/novo`, `GET /admin/recursos/{id}` e `GET /admin/recursos/{id}/manutencao` são proposta.
