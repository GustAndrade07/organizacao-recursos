# Status dos requisitos

> Entregável da Aula 11. Regra: **nenhum item vira "feito" sem citar o teste** (arquivo e nome do teste).
> Situação: `feito` · `parcial` · `ausente`. Atualizar a cada PR e ao fim de cada etapa.
> Ordem das pendências: primeiro o que zera a nota (aplicação não roda, CI ausente, falha de autorização, reserva dupla).

Última atualização: 11/11/2026 (branch `feat/backend-camadas`: back-end em camadas com API REST e JWT; telas removidas, ADR-0008 proposto).

> **Limite da evidência desta atualização:** `./mvnw -B verify` completo rodou **só na máquina do Pedro** em 11/11/2026 (Docker Desktop 29.9.3, Testcontainers com `postgres:16`): 361 testes, 0 falhas, 0 erros, 0 ignorados, incluindo os 83 testes de banco do Gustavo; gate do JaCoCo atendido com 97,4% de linhas e 87,2% de branches (`target/site/jacoco/jacoco.csv`). Nenhuma linha vira "feito" antes do run do CI do PR ficar verde.

| Requisito | Situação | Evidência (arquivo e nome do teste) | Risco se ficar de fora | Dono |
|---|---|---|---|---|
| RF-01 Login e permissão por perfil | parcial | `AutenticacaoControllerTest` (token certo, credencial errada sem revelar login, sem token, token expirado com `Clock`, token adulterado) e `MatrizAutorizacaoTest` (100 casos parametrizados lidos de `docs/seguranca/matriz-autorizacao.md`; falha se surgir rota fora da matriz). Falta o run do CI | Falha de autorização zera a nota | Pedro |
| RF-02 Cadastro e consulta | parcial | `RecursoControllerTest` (cadastro, alteração sem trocar o tipo, 404, filtros) e `UsuarioControllerTest`. Localização, patrimônio e competências não existem na V1 (pendente de V2, Gustavo) | | Pedro |
| RF-03 Pesquisa | parcial | `RecursoControllerTest.disponiveisExcluiReservaQueOcupaEManutencaoMasNaoACancelada` (tipo, capacidade e disponibilidade, fronteira `[início, fim)`). Falta localização e competência (dependem da V2) | | Pedro |
| RF-04 Criar, alterar e cancelar reservas | parcial | `ReservaControllerTest` (`Criar`, `Alterar`, `Cancelar`: RN-01, dono, RN-07 com fronteira de 1 s). Falta o run do CI | | Pedro |
| RF-05 Sobreposição de horário (inclusive professor) | parcial | Banco: `SobreposicaoBancoTest`. API: `ReservaControllerTest.Sobreposicao` (trinca de fronteiras da D1, professor em outra sala, cancelar libera). Falta o run do CI | | Pedro |
| RF-06 Reserva dupla em pedidos simultâneos | parcial | Banco: `ConcorrenciaReservaBancoTest`. Ponta a ponta: `ConcorrenciaReservaApiTest` (20 pedidos HTTP → 1 × 201 e 19 × 409, contagem no banco, sala e professor, 5 rodadas cada). Falta o run do CI e a mutação no nível da API | Reserva dupla zera a nota | Gustavo / Pedro |
| RF-07 Aprovação de recurso restrito | parcial | `ReservaControllerTest.Aprovacao` (Responsável aprova; RN-06 para Responsável e Solicitante, reserva continua SOLICITADA; rejeição libera). Estado inicial sem recurso restrito = APROVADA é proposta (D9) | | Pedro |
| RF-08 Bloqueio por manutenção | parcial | `ReservaControllerTest.Manutencao` (trinca de fronteiras 10:00 a 12:00 e 409 próprio) e `RecursoControllerTest.manutencaoECadastradaEListadaComPeriodoInvalidoRecusado`. Efeito sobre reservas já existentes: D10 pendente | | Pedro |
| RF-09 Retirada e devolução | parcial | `ReservaControllerTest.CicloDeUso` (APROVADA → EM_USO → CONCLUIDA, auditoria de cada passo). Sem registro de movimentação de material (`movimentacao_material` é V2) | | Pedro |
| RF-10 Histórico auditável | parcial | Banco: `EsquemaBancoTest`, `AuditoriaBancoTest`. Serviço: `AuditoriaServiceTest` (auditoria fora de transação recusada; rollback leva a auditoria junto). API: histórico com quem, quando e motivo e 403 do Solicitante (`ReservaControllerTest.Aprovacao`, `MatrizAutorizacaoTest`) | | Gustavo / Pedro |
| RF-11 Notificação externa | ausente | | | Pedro |
| RF-12 Relatório de utilização | parcial | Banco: `TentativaRecusadaBancoTest`, `RelatorioUtilizacaoBancoTest`. API: `RelatorioControllerTest` (valores calculados à mão: borda, cancelada, conflito dentro e fora da janela, período vazio, 401 e 403). Premissas pendentes (D8) | | Gustavo / Pedro / Leandro |
| RF-13 Telas responsivas e erros claros | ausente | Telas removidas em 11/11 (ADR-0008, proposto). Os erros da API são claros e sem detalhe interno (`TratadorDeErrosTest`, casos de 400 em `UsuarioControllerTest` e `ReservaControllerTest`), mas **não há interface**. **Decisão do grupo pendente antes da tag** | RF-13 é obrigatório na especificação | Leandro / Pedro |
| RF-14 Documentação da API | ausente | Rotas e regras descritas em `docs/seguranca/matriz-autorizacao.md`; falta OpenAPI | | Pedro |

## Pendências e divisão
1. Run do CI verde no PR `feat/backend-camadas` (Testcontainers e testes de banco do Gustavo junto com os novos) — Pedro.
2. RF-13: decidir com o Leandro o que entra no lugar das telas antes da tag (ADR-0008) — grupo.
3. D8, D9 e D10 (seção 9 do plano) — grupo.
4. RF-11 (notificação com WireMock) e RF-14 (OpenAPI) — Pedro.
