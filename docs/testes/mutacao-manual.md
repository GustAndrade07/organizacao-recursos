# Mutação manual das regras críticas

Plano de execução, Etapa 5 [Aula 12]: quebrar cada regra crítica de propósito, ver o teste falhar e desfazer a quebra com `git restore`. Teste que continua verde com a regra quebrada é lacuna: escrever o teste que falta. **Nenhuma mutação pode sobrar no código.**

Este arquivo tem a seção do banco (Gustavo, 07/11/2026), a M1 repetida no nível da API (Pedro, 12/11/2026, seção "Mutações do back-end") e a **seção do serviço** (13/11/2026): aprovação, manutenção, autorização e auditoria, que eram as regras pendentes.

## Como foi medido (vale para todas as linhas)

- Data: 07/11/2026. Máquina: a do Gustavo (Windows 11, JDK 21.0.8, Docker Desktop, PostgreSQL 16.15 em Testcontainers). **Medido só nesta máquina; não generalizar.** Não há run do CI com estes números.
- Base: branch `feat/banco-auditoria-relatorio` no commit `fa1c910`, que tem a V1 do PR #28.
- Comando de cada rodada (M1 e M2: o pacote inteiro de banco; M3 a M5: só a classe citada):

```powershell
.\mvnw.cmd -B test "-Dtest=br.com.senac.recursos.banco.*Test"
```

- Linha de base, sem mutação: **83 testes, 0 falhas, 0 erros, 0 ignorados**.
- Depois de cada mutação: `git restore -- <arquivo>` e `git diff -- <arquivo>` vazio. No fim, `git status --short` sem nenhuma linha. Os logs das rodadas ficaram na pasta temporária da sessão (não versionados); para conferir, repita a mutação descrita e o comando acima.

## As seis regras críticas

| Regra | Situação | Mutação |
|---|---|---|
| Sobreposição | **feita** (banco em 07/11, API em 12/11) | M1 |
| Dupla reserva (concorrência) | **feita** (banco em 07/11, API em 12/11) | M1 e M2 (mesma constraint: uma mutação cobre as duas regras) |
| Aprovação de recurso restrito | **feita** (13/11) | MA1 e MA2, na seção do serviço |
| Manutenção | **feita** (13/11) | MM1, MM2 e MM3, na seção do serviço |
| Autorização | **feita** (13/11) | MZ1 (perfil) e MZ2 (posse), na seção do serviço |
| Auditoria | **feita** (banco em 07/11 com M3a e M3b; serviço em 13/11) | MAU1, na seção do serviço — e foi ela que achou a lacuna da RN-08 |

## Mutações do banco

### M1 — constraint de exclusão deixa de proteger (sobreposição e dupla reserva)

- **Arquivo e quebra:** `V1__nucleo_da_reserva.sql`, na constraint `reserva_recurso_sem_sobreposicao`, `WHERE (estado IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA'))` trocado por `WHERE (false)`: nenhuma linha entra na exclusão.
- **Resultado:** **24 de 83 testes vermelhos**.
  - `ConcorrenciaReservaBancoTest`: 10 (os dois cenários de 20 pedidos, recurso e professor, 5 rodadas cada; a asserção "pedidos aceitos" deixa de ser 1).
  - `SobreposicaoBancoTest`: 12 (mesmo recurso, professor com sala diferente, fronteiras do intervalo, estados que ocupam horário, reativar reserva cancelada em horário ocupado).
  - `AuditoriaBancoTest`: 1 (`mudancaDeEstadoRecusadaPeloBancoDesfazAAuditoriaJaGravada`: o banco deixa de recusar a reativação).
  - `TentativaRecusadaBancoTest`: 1 (`tentativaGravadaEmTransacaoPropriaSobreviveAoRollbackDoPedidoRecusado`: não há violação para recusar).
  - Os 22 de Concorrência e Sobreposição são os mesmos "22 de 41" do PR #28; os outros 2 são testes escritos depois.
- **Conclusão:** a proteção contra reserva dupla não é enfeite; sem ela, os testes pegam. Nenhuma lacuna encontrada nesta mutação.

### M2 — trava por recurso (D7) deixa de travar

- **Arquivo e quebra:** `V1__nucleo_da_reserva.sql`, no corpo de `reserva_recurso_trava_recurso()`, `PERFORM pg_advisory_xact_lock(...)` trocado por `PERFORM 1;` (o trigger continua existindo, mas não trava).
- **Resultado:** **2 de 83 testes vermelhos**.
  - `ConcorrenciaReservaBancoTest.gravarOcupacaoTomaUmaTravaPorRecursoEASoltaNoFimDaTransacao`: determinístico, confere as travas em `pg_locks` (sala e professor).
  - `ConcorrenciaReservaBancoTest.vintePedidosSimultaneosParaAAgendaDoMesmoProfessor...`: falhou numa rodada com "SQLState diferente de 23P01", o efeito da corrida sem a trava. **Esse é intermitente por natureza**: apareceu nesta execução, mas não se deve contar com ele; a prova estável da trava é o teste de `pg_locks`. Uma execução só; não repetida para medir frequência (não medido).
- **Conclusão:** a trava tem teste determinístico. Os números do PR #28 (9 de 40 rodadas com deadlock sem a trava) continuam sendo de 06/11/2026 e não foram refeitos aqui.

### M3a e M3b — auditoria e mudança de estado deixam de ser atômicas

Não existe código de produção de auditoria ainda (o serviço é do Pedro). Por isso estas duas mutações foram feitas **no próprio `AuditoriaBancoTest`**, simulando um serviço errado, e restauradas em seguida. Elas provam que o teste detecta o defeito; **não provam** nada sobre o serviço real, que continua pendente.

| Mutação | Quebra | Resultado |
|---|---|---|
| M3a | a auditoria passa a ser gravada em transação própria (`REQUIRES_NEW`) antes da mudança de estado que o banco recusa | 1 de 3 vermelhos: `mudancaDeEstadoRecusadaPeloBancoDesfazAAuditoriaJaGravada` (a auditoria sobrevive ao rollback: auditoria órfã) |
| M3b | a mudança de estado passa a ser gravada em transação própria antes da auditoria que o banco recusa | 1 de 3 vermelhos: `auditoriaRecusadaPeloBancoDesfazAMudancaDeEstadoJaFeita` (o estado muda sem auditoria) |

Este é também o "vermelho" do teste de rollback, que nasceu verde porque o rollback é do PostgreSQL (regra 11 do `AGENTS.md`).
**Pendente (Pedro):** repetir a mutação de auditoria no serviço de reserva (gravar a auditoria fora da transação) e ver o teste do serviço falhar.

### M4 e M5 — consultas do relatório (RF-12), mutações a mais

Não estão entre as seis regras, mas servem para provar que os valores calculados à mão do `RelatorioUtilizacaoBancoTest` não são tautologia.

| Mutação | Arquivo e quebra | Resultado |
|---|---|---|
| M4 | `relatorio-horas-por-recurso.sql`: o filtro de estado trocado por `WHERE true` (cancelada, rejeitada e não compareceu passam a contar) | 1 de 6 vermelhos: `reservaCanceladaRejeitadaOuQueNaoCompareceuNaoContaComoUso` |
| M5 | `relatorio-horas-por-recurso.sql`: a duração passa a ser a da reserva inteira, sem a interseção com a janela | 1 de 6 vermelhos: `reservaQueCruzaABordaContaSoAParteDentroDaJanela` |

Não mutei ainda `relatorio-carga-por-professor.sql` nem `relatorio-conflitos-evitados.sql` (não medido): têm teste próprio, mas sem rodada de mutação.

## Mutações do back-end

### M1 no nível da API — 20 pedidos HTTP sem a constraint de exclusão

- **Data e máquina:** 12/11/2026, máquina do Pedro (Windows 11, OpenJDK 21.0.13, Docker Desktop 29.9.3, PostgreSQL 16.15 em Testcontainers). **Medido só nesta máquina; não generalizar.**
- **Base:** `main` no commit `986e1cd`.
- **Comando:**

```powershell
.\mvnw.cmd -B test "-Dtest=ConcorrenciaReservaApiTest" "-Djacoco.skip=true"
```

- **Linha de base, sem mutação:** 10 testes, 0 falhas, 0 erros, 0 ignorados.
- **Quebra:** a mesma da M1 do banco: em `V1__nucleo_da_reserva.sql`, o predicado da constraint `reserva_recurso_sem_sobreposicao` trocado por `WHERE (false)`. A migração alterada nunca foi commitada: `git restore` logo depois da rodada e `git status --short` vazio.
- **Resultado:** **10 de 10 vermelhos** (`vintePedidosParaAMesmaSalaGeramUmaReserva` e `vintePedidosParaOMesmoProfessorEmSalasDiferentesGeramUmaReserva`, 5 rodadas cada). Em todas as rodadas, as 20 respostas foram 201 (`{201=20}` em vez de `{201=1, 409=19}`).
- **Conclusão:** o serviço não tem checagem própria de sobreposição; de ponta a ponta, quem barra a reserva dupla é o banco, e o teste de API pega a falta da constraint.
- **CI:** o `ConcorrenciaReservaApiTest` faz parte do `verify`; o run 43302104277 (`main`, `986e1cd`) terminou verde. O log por teste não foi aberto (pede login).
## Mutações do serviço (aprovação, manutenção, autorização e auditoria)

> Feitas em **13/11/2026** pelo Leandro (com IA) na área do Pedro, com o OK do Leandro; Pedro revisa. Fecham a lacuna 6 do plano: até aqui, aprovação, manutenção e autorização estavam como "pendente (Pedro)".

### Como foi medido (vale para toda esta seção)

- **Máquina:** container Linux desta sessão (Ubuntu 24.04, 4 CPUs, 15 GB), OpenJDK 21.0.12.1, Docker Engine 29.9.2, PostgreSQL 16 em Testcontainers. **Medido só nesta máquina; não generalizar.** Não há run do CI com estes números.
- **Base:** `main` no commit `9acbbcd`, mais os 4 testes novos da RN-08 (PR `test/rn08-auditoria-servico` desta mesma leva). Sem eles, a MAU1 mostra menos (ver a tabela da MAU1).
- **Comando de cada rodada** (a suíte inteira, porque mutação de serviço atinge API, telas e E2E):

```bash
./mvnw -B test -Djacoco.skip=true -Dmaven.test.failure.ignore=true
```

- **Linha de base, sem mutação:** **1120 testes, 0 falhas, 0 erros, 0 ignorados**.
- Depois de cada mutação: `git checkout -- src/main` e `git status --short` conferido — nenhuma mutação sobrou no código (a conferência final está no fim desta seção).
- Cada mutação foi aplicada **sozinha**, sobre a base limpa.

### Resumo

| Mutação | Regra quebrada | Vermelhos (de 1120) |
|---|---|---|
| MA1 | ninguém aprova a própria reserva (RN-06) | **4** |
| MA2 | recurso restrito nasce SOLICITADA (RF-07) | **11** (10 falhas + 1 erro) |
| MM1 | manutenção bloqueia reserva nova (RN-04) | **5** |
| MM2 | manutenção bloqueia alteração de reserva (RN-04) | **1** |
| MM3 | a manutenção usa o intervalo `[início, fim)` (D1) | **1** |
| MZ1 | só o Responsável registra retirada, devolução e não comparecimento | **10** |
| MZ2 | só o dono altera, cancela e vê a própria reserva | **18** |
| MAU1 | toda mudança de estado gera auditoria (RN-08) | ver abaixo |

Nenhuma mutação passou despercebida. Detalhe de cada uma a seguir.

### MA1 — qualquer um aprova a própria reserva

- **Quebra:** em `ReservaService.aprovar`, apagar o `if (reserva.pertenceA(quem.id())) throw new AcessoNegadoException(...)`.
- **Resultado: 4 vermelhos.**

| Teste | O que acusou |
|---|---|
| `ReservaApiControllerTest.Aprovacao.responsavelNaoAprovaAPropriaReserva` | a reserva própria passou a `APROVADA` em vez de 403 |
| `TelasComBancoTest.responsavelNaoAprovaAPropriaReservaETelaMostraOAviso` | a tela deixou de mostrar o aviso |
| `MatrizAutorizacaoTest.cadaCelulaDaMatrizValeNaApi` (2 casos: célula `não-dono` de `POST /api/reservas/{id}/aprovacao` e de `POST /aprovacoes/{id}/aprovar`) | na própria reserva a resposta deixou de ser 403 |

- **Leitura:** a RN-06 está protegida nas três camadas (API, tela e matriz). O `solicitanteNaoAprovaAPropriaReserva` **não** ficou vermelho porque o Solicitante já é barrado antes, pela permissão de perfil — ou seja, aquele teste não prova a posse, prova o perfil. Bom saber antes da mesa.

### MA2 — recurso restrito nasce APROVADA

- **Quebra:** em `ReservaService.criar`, trocar `EstadoReserva.SOLICITADA` por `EstadoReserva.APROVADA` no ramo do recurso restrito (a aprovação deixa de existir na prática).
- **Resultado: 11 vermelhos** (10 falhas e 1 erro), incluindo `Criar.recursoRestritoNasceSolicitada`, os quatro de `Aprovacao`, o `AuditoriaDeCadaTransicao.rejeicaoEntraNoHistoricoComQuemEMotivo`, a fila de aprovação das telas (`TelasComBancoTest`, 3) e o E2E `ResponsavelEAdministradorE2ETest`.
- **Leitura:** é a mutação mais barulhenta, e devia ser: o RF-07 atravessa banco, serviço, tela e E2E.

### MM1 — reserva nova ignora a manutenção

- **Quebra:** em `ReservaService.criar`, remover a chamada `exigirForaDeManutencao(ids, periodo)`.
- **Resultado: 5 vermelhos**: as três linhas de sobreposição do `Manutencao.fronteirasDaManutencaoDe10a12` (`09:00–10:00:01`, `11:59–13:00`, `10:30–11:30`), o `manutencaoDa409DeTipoProprioSemTentativaESemReserva` e o `TelasComBancoTest.recursoEmManutencaoVoltaComOPeriodoDaManutencao`.
- **Leitura:** as duas linhas de fronteira que **aceitam** (`09:00–10:00` e `12:00–13:00`) continuaram verdes, como esperado: elas não dependem da regra.

### MM2 — alterar reserva ignora a manutenção

- **Quebra:** a mesma remoção, agora em `ReservaService.alterarPeriodo`.
- **Resultado: 1 vermelho** — `ReservaApiControllerTest.Alterar.periodoParaManutencaoDa409`.
- **Leitura:** **um só teste separa a alteração da criação.** É a cobertura mais magra das oito mutações: se esse teste for apagado, a regra some sem ninguém notar. Fica registrado como ponto frágil, não como lacuna (o teste existe e pega a quebra).

### MM3 — manutenção com intervalo fechado em vez de `[início, fim)`

- **Quebra:** em `ManutencaoRepository`, trocar `m.periodo && cast(:periodo as tstzrange)` por `tstzrange(lower(m.periodo), upper(m.periodo), '[]') && cast(:periodo as tstzrange)` — o fim da manutenção passa a ser incluído, contra a D1.
- **Resultado: 1 vermelho** — a linha `12:00–13:00` do `fronteirasDaManutencaoDe10a12`, que deve ser **aceita** e passou a ser recusada.
- **Leitura:** é a prova de que a trinca de fronteiras vale a pena: sem a linha "começa exatamente no fim", essa mutação passaria. Nenhum outro teste do projeto pegou.

### MZ1 — retirada, devolução e não comparecimento para qualquer um logado

- **Quebra:** em `SegurancaConfig`, trocar `.hasRole(RESPONSAVEL)` por `.authenticated()` nas rotas `POST /api/reservas/*/aprovacao|rejeicao|retirada|devolucao|nao-comparecimento`.
- **Resultado: 10 vermelhos**, todos no `MatrizAutorizacaoTest.cadaCelulaDaMatrizValeNaApi` — as células `403` de Solicitante e de Administrador nas cinco rotas.
- **Leitura:** a matriz é o que segura a autorização da API. Nenhum teste de negócio percebeu a mudança, porque do ponto de vista do negócio a operação continuou funcionando — **para a pessoa errada**. É exatamente o caso que a Aula 12 quer que o grupo saiba responder.

### MZ2 — qualquer pessoa altera, cancela e vê a reserva de outro

- **Quebra:** em `ReservaService`, apagar o corpo de `exigirDono` (a posse deixa de ser conferida).
- **Resultado: 18 vermelhos**: 14 células `dono` da matriz (API e telas), mais `Alterar.outroSolicitanteRecebe403EAReservaFicaIntacta`, `Cancelar.outroSolicitanteRecebe403EAReservaContinuaAprovada`, `Consulta.solicitanteSoEnxergaAsProprias` e `TelasComBancoTest.solicitanteNaoAbreAReservaDeOutraPessoa`.
- **Leitura:** a posse é conferida no serviço e aparece na matriz pelas células `dono`; as duas pontas quebram juntas.

### MAU1 — cancelamento e não comparecimento sem auditoria

- **Quebra:** em `ReservaService.mudarEstado`, envolver a chamada `auditoria.registrar(...)` num `if (destino != CANCELADA && destino != NAO_COMPARECEU)`. As outras transições continuam gravando: é a quebra parcial, a mais difícil de pegar.
- **Resultado: depende dos testes da RN-08 que entraram hoje.**

| Suíte | Vermelhos |
|---|---|
| `main` + os 4 testes de `ReservaApiControllerTest.AuditoriaDeCadaTransicao` (1120 testes) | **3** — `cancelamentoEntraNoHistoricoComQuemQuandoEMotivo`, `naoComparecimentoEntraNoHistoricoComQuemQuandoEMotivo` e `transicaoRecusadaNaoGravaAuditoria` |
| `main` como estava, sem esses testes (1116 testes) | **0** |

- **Leitura:** esta é a lacuna que a mutação existe para achar. Até hoje, apagar a auditoria do cancelamento e do não comparecimento **não deixava nenhum teste vermelho**, embora a RN-08 diga "toda mudança de estado gera registro". Os testes do PR `test/rn08-auditoria-servico` fecham o buraco; a linha RN-08 da RTM sai de `parcial` por causa disso.
- **Detalhe:** o `transicaoRecusadaNaoGravaAuditoria` também ficou vermelho porque conta registros no banco e o cancelamento mutado não grava — ou seja, ele confere a RN-08 pelos dois lados.

### Conferência final: nada ficou mutado

Cada rodada terminou com `git checkout -- src/main` e, no fim de todas, também o arquivo de teste usado nas rodadas foi restaurado. Na árvore usada para as mutações, depois da última rodada:

```
$ git status --short
(nenhuma linha)
```

Nenhuma mutação sobrou — nem em `src/main`, nem em `src/test`.

### O que estas mutações **não** cobrem

- O serviço de notificação (RF-11) **não existe**, então não há o que mutar.
- A janela da RN-04 ("consulta e depois grava", D13): o risco foi aceito e **não** tem teste, então uma mutação ali não teria o que ficar vermelho. Está dito no ADR-0008.
- Mutações de condição de borda dentro das consultas do relatório (M4 e M5 do banco já cobrem parte; o serviço do relatório não foi mutado — **não medido**).

## Lacunas encontradas

**Lacuna achada em 13/11/2026 (MAU1):** apagar a auditoria do cancelamento e do não comparecimento não deixava **nenhum** teste vermelho (0 de 1116). Os 4 testes de `ReservaApiControllerTest.AuditoriaDeCadaTransicao` (PR `test/rn08-auditoria-servico`) fecham o buraco: com eles, a mesma mutação deixa 3 vermelhos.

**Ponto frágil, não lacuna (MM2):** um único teste (`Alterar.periodoParaManutencaoDa409`) separa a regra de manutenção na alteração de reserva.

Nenhuma nas mutações do banco: cada quebra deixou pelo menos um teste vermelho. Fica de fora, por escolha, mutar as constraints secundárias (CHECK do intervalo semiaberto, FK composta do estado), que têm teste em `EsquemaBancoTest` e `SobreposicaoBancoTest` mas não passaram por rodada de mutação (não medido).
