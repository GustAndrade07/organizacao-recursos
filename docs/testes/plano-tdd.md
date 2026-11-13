# Plano de TDD — inventário de regras e casos

> **Para que serve:** mostrar que nenhuma regra de negócio ficou sem teste e onde está cada caso. Para cada regra, cinco tipos de caso: **feliz**, **limite**, **inválido**, **conflito** e **estado proibido**. Quando um tipo não se aplica à regra, a célula diz "não se aplica" e por quê — célula vazia esconde lacuna.
>
> **Regra deste arquivo:** só entra teste que **existe** hoje, com o nome da classe e do método. Caso sem teste fica escrito como **falta**, não como "a escrever um dia".
>
> Escrito em 13/11/2026 sobre a `main` no commit `9acbbcd`, mais os testes dos PRs desta sessão (marcados com ⁺). Conferido com `grep` nos arquivos de teste, não de memória.

## Como ler

| Coluna | O que é |
|---|---|
| **Feliz** | o caminho que deve funcionar |
| **Limite** | a fronteira exata (o segundo antes, o segundo depois, o valor mínimo) |
| **Inválido** | entrada que o sistema deve recusar com 400 |
| **Conflito** | duas verdades que não cabem juntas: 409 |
| **Estado proibido** | a operação certa no estado errado |

Camadas: **[b]** banco (Testcontainers), **[u]** unidade, **[s]** serviço, **[a]** API (caixa-preta), **[t]** tela, **[e]** E2E, **[bdd]** cenário Cucumber.

## 1. Reserva e sobreposição (RF-04, RF-05, RN-01, RN-02)

| Caso | Teste | Situação |
|---|---|---|
| Feliz: criar reserva de sala e professor no mesmo período | [a] `ReservaApiControllerTest.Criar.salaComProfessorSemRestritoNasceAprovadaComAuditoria`; [t] `TelasComBancoTest`; [e] `ReservaE2ETest` | coberto |
| Limite: `[início, fim)` — termina no início, começa no fim, cruza 1 s (D1) | [u] `PeriodoTest.fronteirasDoIntervaloSemiaberto`; [b] `SobreposicaoBancoTest.fronteirasDoIntervaloSemiaberto`; [a] `ReservaApiControllerTest.Sobreposicao.fronteirasDaSala` (5 linhas) | coberto nas 3 camadas |
| Limite: duração de um microssegundo | [u] `PeriodoTest.umMicrossegundoDeDuracaoEhValido` | coberto |
| Limite: nanossegundos truncados para a precisão do banco | [u] `PeriodoTest.nanossegundosSaoTruncadosParaAPrecisaoDoBanco` | coberto |
| Inválido: término antes do início ou igual (RN-01) | [u] `PeriodoTest.terminoAntesDoInicioFereRn01`, `terminoIgualAoInicioFereRn01`; [b] `SobreposicaoBancoTest.periodoComTerminoAntesDoInicioERecusadoPeloBanco`; [a] `ReservaApiControllerTest.Criar.terminoAntesDoInicioDa400ENaoGrava` | coberto |
| Inválido: lista vazia, recurso repetido, recurso inexistente, data sem deslocamento | [a] `ReservaApiControllerTest.Criar.listaVaziaRecursoRepetidoEInexistenteSaoRecusados`, `dataSemDeslocamentoDa400Legivel` | coberto |
| Conflito: mesmo recurso, horários que se cruzam → 409 e tentativa gravada | [b] `SobreposicaoBancoTest.sobreposicaoNoMesmoRecursoERecusadaPeloBancoEODesfazTudo`; [a] `ReservaApiControllerTest.Sobreposicao.sobreposicaoDa409GravaTentativaENaoGravaReserva` | coberto |
| Conflito: agenda do professor, mesmo em outra sala | [b] `SobreposicaoBancoTest.sobreposicaoNaAgendaDoProfessorERecusadaMesmoComSalaDiferente`; [a] `...agendaDoProfessorTambemEhProtegidaMesmoEmOutraSala` | coberto |
| Estado proibido: cancelar libera o horário; reativar cancelada em horário ocupado é recusada | [b] `SobreposicaoBancoTest.cancelarReservaNaoViolaAConstraintELiberaOHorario`, `reativarReservaCanceladaComHorarioJaOcupadoERecusadoPeloBanco`; [a] `...cancelarLiberaOHorario` | coberto |

## 2. Reserva dupla em pedidos simultâneos (RF-06, RN-03) — limitador de nota

| Caso | Teste | Situação |
|---|---|---|
| Feliz / conflito: 20 pedidos simultâneos → 1 aceito, 19 recusados, contagem **no banco**, 5 rodadas | [b] `ConcorrenciaReservaBancoTest.vintePedidosSimultaneosParaOMesmoRecursoGeramUmaReservaEDezenoveRecusas`; [a] `ConcorrenciaReservaApiTest` (1 × 201, 19 × 409) | coberto |
| O mesmo para a agenda do professor | [b] `...ParaAAgendaDoMesmoProfessor...`; [a] `ConcorrenciaReservaApiTest` | coberto |
| Limite: a trava por recurso existe e some no fim da transação (D7) | [b] `ConcorrenciaReservaBancoTest.gravarOcupacaoTomaUmaTravaPorRecursoEASoltaNoFimDaTransacao` | coberto |
| Vermelho sem a proteção | mutação M1 de [`mutacao-manual.md`](mutacao-manual.md), nos níveis de banco e de API | medido |
| Inválido / estado proibido | não se aplica: a regra é sobre simultaneidade, não sobre entrada | — |

## 3. Aprovação de recurso restrito (RF-07, RN-06)

| Caso | Teste | Situação |
|---|---|---|
| Feliz: Responsável aprova; histórico guarda quem, quando e motivo | [a] `ReservaApiControllerTest.Aprovacao.responsavelAprovaRestritoEHistoricoGuardaQuemEQuando`; [bdd]⁺ "Recurso restrito exige aprovação de um Responsável"; [t] `TelasComBancoTest`; [e] `ResponsavelEAdministradorE2ETest` | coberto |
| Limite: recurso restrito nasce SOLICITADA; sem restrito nasce APROVADA (D10) | [u] `ReservaTest.reservaComUmRecursoRestritoExigeAprovacao`; [a] `Criar.recursoRestritoNasceSolicitada` e `...NasceAprovadaComAuditoria` | coberto |
| Estado proibido: ninguém aprova a própria reserva, nem o Responsável | [a] `Aprovacao.responsavelNaoAprovaAPropriaReserva`, `solicitanteNaoAprovaAPropriaReserva`; [bdd]⁺ dois cenários; [t] `TelasComBancoTest.responsavelNaoAprovaAPropriaReservaETelaMostraOAviso` | coberto; mutação **MA1**⁺ deixa 4 testes vermelhos |
| Conflito: rejeitar libera o horário | [a] `Aprovacao.rejeitarLiberaOHorario`; [bdd]⁺ "Rejeição libera o horário" | coberto |
| **Falta** | o Responsável **rejeitar a própria** reserva (D10 (c) diz que pode) não tem teste próprio | **falta** |

## 4. Bloqueio por manutenção (RF-08, RN-04)

| Caso | Teste | Situação |
|---|---|---|
| Feliz: manutenção cadastrada e listada | [a] `RecursoApiControllerTest.manutencaoECadastradaEListadaComPeriodoInvalidoRecusado` | coberto |
| Limite: trinca de fronteiras 10:00–12:00 (−1 s, 0, +1 s) | [a] `ReservaApiControllerTest.Manutencao.fronteirasDaManutencaoDe10a12` (5 linhas); [bdd]⁺ Esquema do Cenário com as mesmas 5 | coberto em duas linguagens |
| Inválido: período invertido na manutenção → 400 | [a] `RecursoApiControllerTest.manutencaoECadastradaEListada...` | coberto |
| Conflito: 409 de **tipo próprio**, sem tentativa recusada gravada | [a] `Manutencao.manutencaoDa409DeTipoProprioSemTentativaESemReserva`; [bdd]⁺ "Recusa por manutenção não é conflito de horário" | coberto; mutações **MM1** e **MM3**⁺ |
| Estado proibido: alterar reserva para período em manutenção | [a] `Alterar.periodoParaManutencaoDa409` | coberto |
| **Limite conhecido** | a regra é "consulta e depois grava" (D13, risco aceito): manutenção criada entre a consulta e o commit passa. **Sem teste**, por decisão registrada no ADR-0008 | risco aceito |

## 5. Transições de estado (RN-05, D3)

| Caso | Teste | Situação |
|---|---|---|
| Feliz: APROVADA → EM_USO → CONCLUIDA, com auditoria de cada passo | [u] `EstadoReservaTest.cadaTransicaoSegueORn05`; [a] `CicloDeUso.retiradaEDevolucaoLevamAteConcluidaComAuditoriaDeCadaPasso`; [bdd]⁺ | coberto |
| Limite: quais estados ocupam o horário (D5) | [u] `EstadoReservaTest.estadosQueOcupamOHorarioSaoOsDaD5`, `estadosQueLiberamOHorarioNaoOcupam`; [b] `SobreposicaoBancoTest.estadoQueLibera...`, `estadoQueOcupa...` | coberto |
| Estado proibido: devolução sem retirada; CONCLUIDA não volta para APROVADA | [u] `EstadoReservaTest.concluidaNaoVoltaParaAprovada`, `ReservaTest.transicaoForaDoMapaEhRecusadaEOEstadoNaoMuda`; [a] `CicloDeUso.concluidaNaoVoltaParaAprovadaEDevolucaoSemRetiradaEhRecusada`; [bdd]⁺ | coberto |
| Estado proibido no banco: estado fora da máquina, ocupação incoerente | [b] `SobreposicaoBancoTest.estadoForaDaMaquinaDeEstadosERecusadoPeloBanco`, `estadoDaOcupacaoDiferenteDoEstadoDaReservaERecusadoPeloBanco` | coberto |
| Conflito: não comparecimento libera o horário | [a] `CicloDeUso.naoComparecimentoLiberaOHorario`; [bdd]⁺ | coberto |
| Inválido: transição em reserva inexistente → 404 | [a] `CicloDeUso.transicaoEmReservaInexistenteDa404` | coberto |

## 6. Alterar e cancelar (RF-04, RN-07)

| Caso | Teste | Situação |
|---|---|---|
| Feliz: dono muda o período e a alteração entra no histórico | [a] `Alterar.donoMudaOPeriodoEAAlteracaoEntraNoHistorico` | coberto |
| Limite: reserva já iniciada (fronteira de 1 s) não muda nem cancela | [u] `PeriodoTest.iniciadoAPartirDoInstanteDeInicio`; [a] `Alterar.reservaJaIniciadaNaoMudaDePeriodo`, `Cancelar.reservaIniciadaNaoPodeSerCancelada` (09:59:59 cancela, 10:00:00 não) | coberto |
| Limite: encostar no próprio horário não conflita consigo mesma | [a] `Alterar.encostarNoProprioHorarioNaoConflitaConsigoMesma` | coberto |
| Inválido: motivo acima de 500 caracteres → 400; reserva inexistente → 404 | [a] `Cancelar.motivoLongoDemaisDa400`, `Alterar.reservaInexistenteDa404` | coberto |
| Conflito: mudar para horário ocupado → 409, mantém o antigo e grava tentativa | [a] `Alterar.mudarParaHorarioOcupadoDa409MantemOAntigoEGravaTentativa` | coberto |
| Estado proibido: alterar reserva cancelada | [a] `Alterar.reservaCanceladaNaoMudaDePeriodo` | coberto |
| Posse: outro Solicitante recebe 403 e a reserva fica intacta | [a] `Alterar.outroSolicitanteRecebe403...`, `Cancelar.outroSolicitanteRecebe403...` | coberto; mutação **MZ2**⁺ |
| **Pendente de decisão** | D9 (cancelar vale para SOLICITADA? alterar APROVADA de restrito volta a SOLICITADA?) — **não inventar política**: enquanto a D9 não for decidida, não há teste a escrever | pendente (D9) |

## 7. Auditoria (RF-10, RN-08 a RN-12)

| Caso | Teste | Situação |
|---|---|---|
| Feliz: criar, aprovar, alterar, retirar, devolver geram registro | [a] `Criar...ComAuditoria`, `Aprovacao...`, `Alterar...`, `CicloDeUso...` | coberto |
| Feliz⁺: **cancelar**, **rejeitar** e **não comparecer** também geram, com quem, quando e motivo | [a]⁺ `ReservaApiControllerTest.AuditoriaDeCadaTransicao` (3 testes) | coberto desde 13/11/2026 |
| Limite: instante vem do `Clock`, não do relógio do sistema | [a] asserções em `2026-11-02T08:00:00Z` etc. com `RelogioDeTeste`; [t] `support/TelasNoRecorte` (DEF-002) | coberto |
| Estado proibido⁺: transição recusada **não** grava auditoria | [a]⁺ `AuditoriaDeCadaTransicao.transicaoRecusadaNaoGravaAuditoria`; [bdd]⁺ "Transição inválida ... não muda o estado" | coberto |
| Conflito / atomicidade (RN-12): rollback não deixa auditoria órfã, nos dois sentidos | [b] `AuditoriaBancoTest` (3 testes); [s] `AuditoriaServiceTest.rollbackDaMudancaDeEstadoLevaAAuditoriaJunto`, `registrarForaDeTransacaoEhRecusadoENaoGravaNada` | coberto; mutações **M3a**, **M3b** e **MAU1**⁺ |
| Inválido (RN-10): a API não tem rota que altere ou apague auditoria | [a] `MatrizAutorizacaoTest.matrizCobreExatamenteAsRotasDoCodigo` (nenhuma rota de escrita) e `rotaForaDaMatrizEhNegada` | coberto por construção |
| RN-11: Solicitante não lê o histórico | [a] matriz (célula 403) | coberto |

## 8. Autorização (RF-01, RN-11, RN-13) — limitador de nota

| Caso | Teste | Situação |
|---|---|---|
| Feliz: cada perfil acessa o que a matriz diz | [a] `MatrizAutorizacaoTest.cadaCelulaDaMatrizValeNaApi` (uma linha por célula, API e telas) | coberto |
| Limite: a matriz **é** a lista; rota do código fora dela quebra o teste | [a] `MatrizAutorizacaoTest.matrizCobreExatamenteAsRotasDoCodigo` | coberto (regra 13 do `AGENTS.md`) |
| Estado proibido: rota desconhecida → 401 sem token, 403 com token (`denyAll`) | [a] `MatrizAutorizacaoTest.rotaForaDaMatrizEhNegada` | coberto |
| Posse: `dono` e `não-dono` conferidos na própria e na de outro | [a] a mesma matriz, com os dois casos por célula | coberto; mutações **MZ1** e **MZ2**⁺ |
| Inválido: sem token, token expirado, token adulterado | [a] `AutenticacaoApiControllerTest` (4 testes), sem `Thread.sleep` | coberto |
| Conflito: 5 falhas em 15 min bloqueiam o login, nas telas e na API, com um contador só | [u] `TentativasDeLoginTest` (10 testes); [t] `LimiteDeTentativasDoLoginIntegracaoTest`; [a] `LimiteDeTentativasApiTest`; [e] `LimiteDeTentativasE2ETest` | coberto (ciclo TDD completo, ver `evidencia-tdd.md`) |

## 9. Relatório de utilização (RF-12, RN-13)

| Caso | Teste | Situação |
|---|---|---|
| Feliz: valores calculados à mão no comentário do teste | [b] `RelatorioUtilizacaoBancoTest` (6 testes); [a] `RelatorioApiControllerTest.valoresCalculadosAMaoComBordaCanceladaEConflito` | coberto |
| Limite: período vazio, reserva cruzando a borda, cancelada não conta | [a] `RelatorioApiControllerTest.periodoVazioDevolveListasVaziasEZeroConflitos` e o teste acima | coberto |
| Inválido: janela ausente ou invertida → 400 | [a] `janelaAusenteOuInvertidaDa400` | coberto |
| Autorização: 401 sem token, 403 para Solicitante | [a] `semTokenDa401ESolicitanteDa403` | coberto |
| **Pendente de decisão** | D8 (quais estados contam, o que é "período" dos conflitos evitados, recurso sem uso, quem é "professor") — os testes registram a premissa em comentário; a regra não está decidida | pendente (D8) |

## 10. Cadastro de recursos e usuários (RF-02, RF-03)

| Caso | Teste | Situação |
|---|---|---|
| Feliz: cadastrar e alterar recurso; cadastrar usuário | [a] `RecursoApiControllerTest.administradorCadastraEAlteraSemMudarOTipo`; `UsuarioApiControllerTest` | coberto |
| Limite: filtro por tipo e capacidade mínima; disponíveis exclui reserva que ocupa e manutenção, mas não a cancelada | [a] `filtraPorTipoECapacidadeMinima`, `disponiveisExcluiReservaQueOcupaEManutencaoMasNaoACancelada` | coberto |
| Inválido: capacidade zero, tipo desconhecido, período ausente ou invertido → 400; recurso inexistente → 404 | [a] `capacidadeZeroOuTipoDesconhecidoDao400`, `disponiveisSemPeriodoOuComPeriodoInvertidoDa400`, `recursoInexistenteDa404` | coberto |
| **Falta** | RF-03 completo: localização e competência dependem da migração V2, que não existe | **falta (fora do escopo da V1)** |

## 11. Notificação externa (RF-11)

Sem teste e **sem código**: nem o adapter nem o WireMock existem (ADR-0005). O requisito está como `ausente` em `docs/status-requisitos.md` e é risco aceito na decisão de qualidade. Os casos que o ADR-0005 exige (200, 404, 500, timeout, JSON inválido, "token não vaza", "só depois do commit") ficam registrados lá como o que **falta**, para não parecerem cobertos.

## 12. Resumo honesto

| Situação | Quais |
|---|---|
| Regra crítica com teste nas camadas que importam | sobreposição, reserva dupla, aprovação de restrito, manutenção, transições, auditoria, autorização, relatório |
| Lacunas conhecidas | Responsável rejeitar a própria reserva (seção 3); RF-03 completo (seção 10); RF-11 inteiro (seção 11) |
| Travado por decisão do grupo, **não** por falta de teste | D8 (relatório) e D9 (alterar/cancelar do Solicitante) |
| Risco aceito sem teste, por decisão | janela da RN-04 (D13, ADR-0008) |

⁺ = entrou nos PRs de 13/11/2026 desta sessão; confira no commit da tag.
