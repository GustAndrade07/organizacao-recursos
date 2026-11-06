# Pranchas da seção 03 · Aprovação, retirada e devolução

Esta pasta guarda os desenhos da seção 03, feitos para aprovação antes da implementação. Eles cobrem a tela 9 (Fila de aprovação) e a tela 10 (Retirada e devolução) do inventário em `docs/frontend/telas.md`. Cada quadro é um HTML comum, escrito com o HTML e o CSS reais do projeto (`tokens.css`, `app.css` e `menu.js` em `src/main/resources/static`), em 375 px (celular) e 1280 px (computador). Os arquivos `secao02.css` e `secao03.css` trazem só o que a seção usa além do CSS do projeto (tabela que vira card no celular, estado, aviso, diálogo, botões em linha e área de texto). Na implementação, esse CSS vai para o `app.css`. Os dados e nomes são de exemplo.

## Como abrir

Abra `index.html` a partir de uma cópia do repositório: ele mostra cada tela em 375 px e em 1280 px, lado a lado. Os caminhos dos CSS e do script sobem quatro pastas até `src/`, então a pasta precisa estar em `docs/frontend/pranchas/secao-03/` dentro do repositório. Cada quadro também abre sozinho no navegador. O nome termina com a largura, por exemplo `FilaRejeitar-375.html` e `FilaRejeitar-1280.html`.

## Quadros

| Quadro | Tela | Requisito | O que mostra |
|---|---|---|---|
| `Fila` | 9 | RF-07, RN-06 | Quatro pedidos, os mais próximos do horário primeiro. O pedido do próprio Responsável não tem botões, porque ninguém aprova a própria reserva (RN-06). |
| `FilaRejeitar` | 9 | RF-07 | Diálogo de rejeição, com motivo obrigatório. |
| `FilaRejeitarSemMotivo` | 9 | RF-07 | O mesmo diálogo sem motivo: erro ao lado do campo, anunciado por `role="alert"`. |
| `FilaAprovada` | 9 | RF-07, RN-05 | Aviso verde com o novo estado da reserva, depois de aprovar. |
| `FilaErro403` | 9 | RF-07, RN-06 | A API recusou a aprovação: "Você não pode aprovar esta reserva", com o estado inalterado. |
| `FilaVazia` | 9 | RF-07 | "Nenhum pedido aguardando aprovação". |
| `RetiradaDevolucao` | 10 | RF-09, RN-05 | Lista com filtro por estado. Aprovada oferece "Registrar retirada", Em uso oferece "Registrar devolução" e Concluída mostra "Sem ação disponível". |
| `RetiradaRegistrada` | 10 | RF-09, RN-05 | Aviso com o novo estado (Em uso) depois de registrar a retirada. |
| `DevolucaoRegistrada` | 10 | RF-09, RN-05 | Aviso com o novo estado (Concluída) depois de registrar a devolução. |
| `NaoCompareceu` | 10 | RF-09, RN-08 | Diálogo de confirmação antes de marcar a reserva como não compareceu, que não tem volta. |
| `RetiradaDevolucaoVazia` | 10 | RF-09 | Estado vazio, sem reservas para retirar ou devolver. |

Todos os estados aparecem com nome escrito, além da cor (RN-05), e todo botão da tabela tem nome acessível que diz a ação, o recurso e o solicitante. Os quadros usam `data-testid` com prefixo `aprovacao-` na tela 9 e `retirada-devolucao-` na tela 10.

## O que foi assumido

A confirmação com o novo estado, que a spec pede, é um aviso depois da ação; só "não compareceu" tem diálogo antes, por ser irreversível. A rejeição usa o mesmo aviso, com o badge Rejeitada, mas não tem quadro próprio. O botão "Marcar como não compareceu" aparece em toda reserva aprovada cujo horário já passou. O menu do Responsável usa o rótulo "Retirada e devolução" (PR #16).

## Decisões pendentes

O prazo para marcar NAO_COMPARECEU ainda não está definido na spec do projeto e depende do back-end (Pedro). As rotas `GET /aprovacoes` e `GET /retirada-devolucao` são proposta; o inventário `telas.md` ainda lista a tela 10 como `GET /emprestimos` com prefixo `emprestimo-`, e não foi alterado neste PR. O 403 na aprovação vem da API. Em 768 px o menu do Responsável fica apertado e quebra em duas linhas; essa não é uma largura obrigatória, mas vale olhar na implementação.
