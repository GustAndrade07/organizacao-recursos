# Status dos requisitos

> Entregável da Aula 11. Regra: **nenhum item vira "feito" sem citar o teste** (arquivo e nome do teste).
> Situação: `feito` · `parcial` · `ausente`. Atualizar a cada PR e ao fim de cada etapa.
> Ordem das pendências: primeiro o que zera a nota (aplicação não roda, CI ausente, falha de autorização, reserva dupla).

Última atualização: 06/11/2026 (RF-13 `parcial` com as telas em Thymeleaf; os demais seguem `ausente`).

| Requisito | Situação | Evidência (arquivo e nome do teste) | Risco se ficar de fora | Dono |
|---|---|---|---|---|
| RF-01 Login e permissão por perfil | ausente | | Falha de autorização zera a nota | Pedro |
| RF-02 Cadastro e consulta | ausente | | | Pedro |
| RF-03 Pesquisa | ausente | | | Pedro |
| RF-04 Criar, alterar e cancelar reservas | ausente | | | Pedro |
| RF-05 Sobreposição de horário (inclusive professor) | ausente | | | Pedro |
| RF-06 Reserva dupla em pedidos simultâneos | ausente | | Reserva dupla zera a nota | Gustavo |
| RF-07 Aprovação de recurso restrito | ausente | | | Pedro |
| RF-08 Bloqueio por manutenção | ausente | | | Pedro |
| RF-09 Retirada e devolução | ausente | | | Pedro |
| RF-10 Histórico auditável | ausente | | | Gustavo |
| RF-11 Notificação externa | ausente | | | Pedro |
| RF-12 Relatório de utilização | ausente | | | Gustavo / Pedro / Leandro |
| RF-13 Telas responsivas e erros claros | parcial | Telas em Thymeleaf com teste de contrato: `LayoutBaseTest`, `PaginasDeErroTest`, `LoginPaginaTest`, `BuscaPaginaTest`, `ResultadosBuscaTest`, `NovaReservaPaginaTest`, `NovaReservaEstadosTest`, `FilaAprovacaoTest`, `RetiradaDevolucaoTest`, `DialogoCssTest`; nenhum valor solto no CSS: `CssSemValoresSoltosTest`. Falta o E2E Playwright em 375 px e as demais telas | | Leandro |
| RF-14 Documentação da API | ausente | | | Pedro |

## Pendências e divisão
(preencher: ordem de prioridade e quem pega cada uma)
