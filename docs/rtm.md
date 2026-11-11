# RTM — Matriz de Rastreabilidade

> Requisito → risco → teste → evidência. Evidência "local" = rodado só na máquina de quem escreveu; vira link do run do CI quando o PR passar.

| Requisito | Risco | Teste | Evidência (CI / arquivo) | Dono |
|---|---|---|---|---|
| RF-06 / RN-03 | Reserva dupla zera a nota | Banco: `ConcorrenciaReservaBancoTest`. API: `ConcorrenciaReservaApiTest` (20 pedidos → 1 × 201, 19 × 409, contagem no banco, sala e professor, 5 rodadas) | local em 11/11 (PR `feat/backend-camadas`); run do CI a anotar | Gustavo / Pedro |
| RF-01 / RF-07 / RN-06 / RN-11 / RN-13 | Falha de autorização zera a nota | `MatrizAutorizacaoTest` lendo `docs/seguranca/matriz-autorizacao.md`; `AutenticacaoControllerTest`; `ReservaControllerTest.Aprovacao` | local em 11/11; run do CI a anotar | Pedro |
| RF-05 / RN-02 / D1 | Sobreposição aceita na fronteira errada | `SobreposicaoBancoTest`; `ReservaControllerTest.Sobreposicao` | local em 11/11 | Gustavo / Pedro |
| RF-08 / RN-04 | Reserva em recurso bloqueado | `ReservaControllerTest.Manutencao` (trinca −1, 0, +1) | local em 11/11 | Pedro |
| RN-05 / D3 | Estado ilegal aceito | `EstadoReservaTest` (49 pares); `ReservaControllerTest.CicloDeUso` | local em 11/11 | Pedro |
| RF-10 / RN-08 / RN-12 | Mudança sem auditoria ou auditoria órfã | `AuditoriaBancoTest`; `AuditoriaServiceTest` | local em 11/11 | Gustavo / Pedro |
| RF-12 | Número do relatório sem fonte | `RelatorioUtilizacaoBancoTest`; `RelatorioControllerTest` (oráculo à mão) | local em 11/11 | Gustavo / Pedro |
| RF-13 | Interface ausente (telas removidas, ADR-0008) | — | ausente | Leandro / Pedro |
