# ADR-0005 — Notificação por cliente HTTP isolado em adapter
- **Status:** proposto (a validar com o Pedro, dono do back-end, na revisão do PR)
- **Contexto:** o RF-11 pede notificação simulada ou integração com API externa. O provedor é externo, pode demorar, falhar ou responder lixo, e **não pode derrubar a reserva** nem vazar o token de acesso (risco R7 de [arquitetura.md](../arquitetura.md)). A Aula 08 pede teste com WireMock cobrindo 200, 404, 500, timeout e JSON inválido.
- **Decisão:**
  1. Um **adapter** no pacote `notificacao` (porta no domínio, implementação HTTP) é o único ponto que fala com o provedor; o serviço de reserva depende só da porta.
  2. O cliente HTTP tem **timeout** de conexão e de leitura configurados por propriedade. O valor **não está definido** (a escolher pelo Pedro e medir; sem número inventado aqui).
  3. A URL e o **token do provedor** vêm de variável de ambiente; nunca do repositório, de log nem de resposta.
  4. A notificação só dispara **depois do commit** da reserva (e da auditoria da mesma transação). Reserva que sofre rollback não notifica.
  5. **Falha da notificação não desfaz a reserva**: o adapter trata erro HTTP, timeout e resposta inválida sem propagar exceção para a transação já concluída.
  6. Sem reenvio garantido: a notificação "pode se perder" (trade-off já aceito em [arquitetura.md](../arquitetura.md), seção 8, porque o provedor é simulado).
- **Consequências:**
  - **Ganhamos:**
    - Falha externa não derruba a criação de reserva (RF-04) e é totalmente testável com WireMock, sem rede real.
    - O token do provedor fica confinado a um ponto, o que torna a checagem "token não vaza" um teste só.
    - Trocar o provedor (simulado → real) muda o adapter, não as regras.
  - **Perdemos:**
    - **Notificação pode se perder** (provedor fora do ar, timeout) e a tela não sabe se ela saiu.
    - Um adapter e uma porta a mais para manter, e testes de integração mais pesados (WireMock além do PostgreSQL em Testcontainers).
    - Ordem "depois do commit" exige mecanismo explícito (por exemplo, evento transacional); se feito errado, a notificação sai antes do commit e anuncia reserva que pode não existir. Ponto de implementação do Pedro, ainda aberto: execução síncrona ou assíncrona depois do commit.
  - **Exige de testes (WireMock + Testcontainers):**

    | Cenário do provedor | Resultado esperado |
    |---|---|
    | 200 com JSON válido | notificação concluída; reserva intacta |
    | 404 | falha tratada, sem exceção para o chamador; reserva continua no banco |
    | 500 | idem |
    | timeout (atraso do WireMock maior que o timeout configurado, sem `Thread.sleep` no teste) | falha tratada dentro do tempo limite; reserva continua no banco |
    | 200 com JSON inválido | falha tratada; reserva continua no banco |
    | **token do provedor não vaza** | o token chega ao provedor só no cabeçalho da chamada (conferido com `verify` do WireMock) e **não** aparece na resposta da API, no `ProblemDetail` nem no log capturado |

    - **Só depois do commit:** reserva com rollback gera **zero** requisições ao WireMock; reserva confirmada gera uma.
    - A contagem de reservas após cada falha do provedor é feita **no banco**, não pela resposta.
    - Teste de unidade do serviço de reserva usa a porta com dublê (mock só em teste de unidade isolado); a integração real do adapter usa WireMock.
- **Pendências:**
  - **pendente (seção 9 do plano, D2):** se uma falha de notificação, ou uma tentativa recusada, é registrada de forma persistente fora da transação da reserva depende da decisão D2. Este ADR não decide.
  - **pendente (seção 9 do plano, D3):** o código HTTP da recusa por transição de estado inválida (409 ou 422) é decisão da API; este ADR só diz que **recusa não gera notificação**, porque não houve mudança.
  - Valor do timeout e modelo (síncrono ou assíncrono depois do commit): abertos com o Pedro.
- **Fonte:** `docs/arquitetura.md` (seções 5.1, 8, 9 e 10), `docs/prd.md` (RF-11), `docs/plano-execucao.md` (Etapa 3, linha da notificação), `docs/plano-de-teste.md` (seção 2), `AGENTS.md` (seção 5). WireMock ainda **não está no `pom.xml`**.
