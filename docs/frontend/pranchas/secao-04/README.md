# Pranchas da seção 04 · Histórico e relatório

Esta pasta guarda os desenhos da seção 04 para aprovação antes da implementação: o histórico da reserva (tela 11 do inventário em `docs/frontend/telas.md`) e o relatório de utilização (tela 12). Cada quadro é uma página HTML comum, feita com o HTML e o CSS reais do projeto (`tokens.css`, `app.css` e `menu.js`, em `src/main/resources/static`), em 375 px e em 1280 px. Os dois arquivos `secao02.css` e `secao04.css` trazem só o que estes quadros usam e passam para o `app.css` na implementação.

## Como abrir

Abra `index.html` a partir de uma cópia do repositório: ele mostra cada tela em 375 e 1280 px lado a lado. Cada quadro também abre sozinho no navegador. Os caminhos dos CSS são relativos e só funcionam com a pasta no lugar em que está (`docs/frontend/pranchas/secao-04/`).

## Quadros, telas e requisitos

| Quadro (375 / 1280) | Tela | Requisito | O que mostra |
|---|---|---|---|
| `Historico-375.html` / `Historico-1280.html` | 11 Histórico da reserva | RF-10, RN-10 | Resumo da reserva e linha do tempo com data e hora, estado anterior e novo escritos por extenso, usuário e motivo, com a nota de que os registros são somente leitura. |
| `HistoricoRejeitada-375.html` / `HistoricoRejeitada-1280.html` | 11 Histórico da reserva | RF-10, RN-09, RN-10 | Reserva rejeitada, com o motivo escrito pelo Responsável na linha da rejeição. |
| `Relatorio-375.html` / `Relatorio-1280.html` | 12 Relatório de utilização | RF-12 | Filtro de início e fim, aviso de que reserva cancelada não conta como uso e os três blocos: horas por recurso, carga por professor e conflitos evitados. Em 375 px as tabelas viram cartões. |
| `RelatorioPeriodoInvalido-375.html` / `RelatorioPeriodoInvalido-1280.html` | 12 Relatório de utilização | RF-12 | Fim antes do início: o relatório não é gerado e o erro aparece em texto, com `role="alert"`. |
| `RelatorioVazio-375.html` / `RelatorioVazio-1280.html` | 12 Relatório de utilização | RF-12 | Período sem reservas: mensagem "Nenhuma reserva no período", sem tabelas vazias. |

O acesso por perfil (RN-11 e RN-13) segue o inventário: as duas telas são do Responsável e do Administrador. O 403 para o Solicitante usa a mesma página 403 da seção 01 e não tem quadro aqui. O Administrador usa as mesmas telas, com o menu dele (Recursos, Relatórios).

## O que foi assumido

A linha do tempo vai do registro mais antigo para o mais recente. Quando não há texto de motivo, a tela mostra "Não informado". No relatório, "professor" é quem pediu a reserva (o solicitante), porque professor não é recurso reservável por enquanto. Os números e nomes do relatório são de exemplo e somam certo: 20 reservas e 39 h 30 min, tanto no bloco de recursos quanto no de professores.

## Decisões pendentes

Dependem do back-end (Pedro): onde fica o link "Ver histórico" para Responsável e Administrador (a sugestão é uma ligação em cada linha da tela de retirada e devolução e no detalhe da reserva para esses perfis); o que a auditoria grava como motivo em aprovação, retirada e devolução; como o relatório conta a reserva que cruza a borda do período (caso obrigatório do PRD); e as rotas `GET /reservas/{id}/historico` e `GET /relatorios/utilizacao?inicio=&fim=` (esta já está no PRD).
