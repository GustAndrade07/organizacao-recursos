# Status dos requisitos

> Entregável da Aula 11. Regra: **nenhum item vira "feito" sem citar o teste** (arquivo e nome do teste).
> Situação: `feito` · `parcial` · `ausente`. Atualizar a cada PR e ao fim de cada etapa.
> Ordem das pendências: primeiro o que zera a nota (aplicação não roda, CI ausente, falha de autorização, reserva dupla).

Última atualização: 11/11/2026 (RF-13: telas 7 e 8 com teste de contrato). Antes: 07/11/2026, noite (RF-10 e RF-12 ganham testes de banco da auditoria e do relatório; antes: RF-13 `parcial` com as telas em Thymeleaf; RF-05, RF-06, RF-10 e RF-12 `parcial` só no nível do banco, com a migração V1; os demais seguem `ausente`).

| Requisito | Situação | Evidência (arquivo e nome do teste) | Risco se ficar de fora | Dono |
|---|---|---|---|---|
| RF-01 Login e permissão por perfil | ausente | | Falha de autorização zera a nota | Pedro |
| RF-02 Cadastro e consulta | ausente | | | Pedro |
| RF-03 Pesquisa | ausente | | | Pedro |
| RF-04 Criar, alterar e cancelar reservas | ausente | | | Pedro |
| RF-05 Sobreposição de horário (inclusive professor) | parcial | Só o banco: `SobreposicaoBancoTest` (recurso, professor, fronteiras da D1, estados da D5). Falta a regra no serviço e o teste de API | | Pedro |
| RF-06 Reserva dupla em pedidos simultâneos | parcial | Só o banco: `ConcorrenciaReservaBancoTest` (20 threads → 1 aceita e 19 recusadas, contagem no banco, recurso e professor, 5 rodadas; vermelho com a constraint neutralizada) e `MigracoesBancoTest` (constraint no catálogo). Falta o 409 de ponta a ponta (serviço do Pedro) | Reserva dupla zera a nota | Gustavo |
| RF-07 Aprovação de recurso restrito | ausente | | | Pedro |
| RF-08 Bloqueio por manutenção | ausente | | | Pedro |
| RF-09 Retirada e devolução | ausente | | | Pedro |
| RF-10 Histórico auditável | parcial | Esquema: `EsquemaBancoTest` (auditoria com estado anterior, novo, instante e motivo; chaves estrangeiras). Atomicidade no banco: `AuditoriaBancoTest` (commit grava estado e auditoria juntos; falha do banco em um lado desfaz o outro; mutações M3a e M3b em `docs/testes/mutacao-manual.md`). Faltam o serviço que grava a auditoria, a consulta do histórico e o 403 do Solicitante | | Gustavo / Pedro |
| RF-11 Notificação externa | ausente | | | Pedro |
| RF-12 Relatório de utilização | parcial | Banco: `TentativaRecusadaBancoTest` (tentativa gravada em transação própria sobrevive ao rollback) e `RelatorioUtilizacaoBancoTest` (horas por recurso, carga por professor e conflitos evitados, com valores calculados à mão: período vazio, reserva cruzando a borda, cancelada não conta). Premissas pendentes de confirmação (D8 do plano). Faltam o endpoint, o serviço e os testes caixa-preta (401/403) | | Gustavo / Pedro / Leandro |
| RF-13 Telas responsivas e erros claros | parcial | Telas em Thymeleaf com teste de contrato: `LayoutBaseTest`, `PaginasDeErroTest`, `LoginPaginaTest`, `CaminhoDoSiteTest`, `ContrasteBotaoDesabilitadoTest`, `BuscaPaginaTest`, `ResultadosBuscaTest`, `NovaReservaPaginaTest`, `NovaReservaEstadosTest`, `MinhasReservasPaginaTest`, `ReservaDetalhePaginaTest`, `HistoricoPaginaTest`, `RelatorioPaginaTest`, `FilaAprovacaoTest`, `RetiradaDevolucaoTest`, `DialogoCssTest`, `ListaRecursosTest`, `RecursoFormTest`, `ManutencaoPaginaTest`, `RecursoControllerTest`; nenhum valor solto no CSS: `CssSemValoresSoltosTest`. As 15 telas do inventário têm HTML (`docs/frontend/telas.md`, 11/11/2026). Falta o E2E Playwright em 375 px | | Leandro |
| RF-14 Documentação da API | ausente | | | Pedro |

## Pendências e divisão
(preencher: ordem de prioridade e quem pega cada uma)
