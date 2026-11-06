# Pranchas da seção 01 · Acesso e navegação

Esta pasta guarda os desenhos da seção 01 da spec do front-end (login, menu por perfil e páginas de erro), feitos com o HTML e o CSS reais do projeto, em 375 px e 1280 px. Eles existem para aprovação antes da implementação dos estados novos do login. Não são telas do sistema: nenhum quadro é servido pelo Spring, e nada em `src/` foi alterado.

## Como abrir

Abra `index.html` a partir de uma cópia do repositório: ele mostra cada tela em 375 px e em 1280 px, lado a lado. Cada quadro (`*.html`) também abre sozinho no navegador. Os quadros leem `tokens.css`, `app.css` e `menu.js` de `src/main/resources/static`, por caminho relativo, então a pasta precisa continuar em `docs/frontend/pranchas/secao-01/`. O arquivo `secao01.css` traz só as regras dos estados novos do login e usa apenas tokens.

## Quadros

| Quadro (arquivos) | Tela (`telas.md`) | Requisito | O que mostra |
|---|---|---|---|
| Login (`Login-375`, `Login-1280`) | 1 · Login | RF-01 | Formulário de e-mail e senha, com o menu de visitante (só a marca). |
| Login com erro (`LoginErro-*`) | 1 · Login | RF-01 | Aviso "E-mail ou senha incorretos" com `role="alert"`, e o e-mail digitado mantido no campo. |
| Login com sessão expirada (`LoginSessao-*`) | 1 · Login | RF-01 | Aviso "Sua sessão expirou. Entre novamente." com `role="alert"`. |
| Login enviando (`LoginCarregando-*`) | 1 · Login | RF-01 | Botão Entrar desabilitado com o texto "Entrando…" e anel de carregamento. |
| Menu do Solicitante, do Responsável e do Administrador (`MenuSolicitante-*`, `MenuResponsavel-*`, `MenuAdministrador-*`) | 2 · Layout base e menu | RF-01, RF-13 | Itens por perfil, nome e perfil da pessoa e botão Sair, em 1280 px e em 375 px com o menu fechado. |
| Menu aberto (`Menu*Aberto-375`) | 2 · Layout base e menu | RF-13 | O mesmo menu aberto em 375 px, com o item da tela atual marcado. |
| Erros 401, 403, 404 e 500 (`Erro401-*`, `Erro403-*`, `Erro404-*`, `Erro500-*`) | 3 · Erro 401 / 403 / 404 / 500 | RF-13 | Código, título, mensagem e caminho para continuar. |

## O que foi assumido

Os itens de menu por perfil seguem `docs/frontend/README.md`: Solicitante, Buscar recursos e Minhas reservas; Responsável, Aprovações, Retirada e devolução e Relatórios; Administrador, Recursos e Relatórios. O item de Retirada e devolução usa o nome novo do PR #16. Os nomes de pessoas são fictícios. O 401 aparece como visitante (só a marca), os 403 e 404 com o menu de Solicitante e o 500 com o de Responsável.

## O que a implementação precisa tratar

Os quadros diferem dos templates atuais da `main` em quatro pontos. O login não mantém o e-mail digitado depois do erro (falta `th:value` em `login.html`). Não existe o estado de envio do botão Entrar, desabilitado e com carregamento. Não existe a variante do login com "Sua sessão expirou. Entre novamente." nem o caminho para voltar à página anterior. As classes `.botao:disabled`, `.carregando` e `.alerta--aviso` estão só em `secao01.css` ou na branch da busca.

## Decisões pendentes (back-end, Pedro)

Falta decidir o `POST /logout`, para onde redirecionar depois do login por perfil (Solicitante: Buscar disponibilidade; Responsável: Fila de aprovação; Administrador: Recursos) e os itens de menu por perfil. As páginas 403, 404 e 500 só mostram o menu se o back-end puser `usuario` e `menu` no modelo de todas as telas, inclusive `/error`.

## Verificação feita

Os quadros foram abertos em 375, 768 e 1280 px, sem rolagem horizontal, com todo campo ligado a um `label`, botões, links `.botao` e campos com 44 px ou mais de altura e `role="alert"` nos avisos do login. Os quadros das páginas de erro não têm `role="alert"` (como os templates atuais); fica como ponto para revisão.
