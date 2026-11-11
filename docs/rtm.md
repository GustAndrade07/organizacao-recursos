# RTM — Matriz de Rastreabilidade

| Requisito | Risco | Teste | Evidência (CI / arquivo) | Dono |
|---|---|---|---|---|
| RF-06 / RN-03 | Reserva dupla zera a nota | Concorrência: 20 pedidos → 1 aceita, 19 × 409 | | Gustavo |
| RF-01 / RF-07 | Falha de autorização zera a nota | Matriz de autorização | | Pedro |
| RF-13 | Tela que não serve no celular (375 px) ou erro sem explicação: o Solicitante desiste ou repete o pedido | E2E Playwright em 375 px (`src/test/java/br/com/senac/recursos/e2e`): `Responsividade375E2ETest` (28 telas: sem rolagem horizontal, um `h1`, tabela vira cartão, alvo de toque ≥ 44 px), `AcessoE2ETest` (login, menu, 401/403/404/500), `ReservaE2ETest` (busca → nova reserva → envio, RN-01, 409, minhas reservas, alterar, cancelar), `ResponsavelEAdministradorE2ETest` (diálogos, envio único, relatório, manutenção). Telas com dados de teste (`TelasComDados`), sem back-end | Job `verify` do CI e artifact `e2e-375` (prints); prints de 11/11/2026 em `docs/evidencias/e2e-375/` | Leandro |
