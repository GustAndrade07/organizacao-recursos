# Status dos requisitos

> Entregável da Aula 11. Regra: **nenhum item vira "feito" sem citar o teste** (arquivo e nome do teste).
> Situação: `feito` · `parcial` · `ausente`. Atualizar a cada PR e ao fim de cada etapa.
> Ordem das pendências: primeiro o que zera a nota (aplicação não roda, CI ausente, falha de autorização, reserva dupla).

**Última atualização: 12/11/2026, noite (Leandro).** Recontagem junto com a RTM, sobre a `main` em `6ce4478`:
- run do CI **43303372662** verde (`verify` e SonarCloud);
- `./mvnw -B verify` local com **1116 testes, 0 falhas, 0 erros, 0 ignorados**;
- carga do PR #56.

Depois da recontagem, 13/11/2026 (Leandro com IA, PR #61): RF-14 `feito` (OpenAPI), RN-08 `feito`, cenários BDD executáveis, mutação do serviço e `plano-tdd.md`; `verify` local com 1138 testes, 0 falhas, 0 ignorados. Ver os arquivos `docs/status/2026-11-13-*` do PR.

Depois da recontagem, 12/11/2026 (Pedro, PR #58): RF-06 ganha o vermelho sem a proteção no nível da API (mutação M1); D10 e D11 decididas ("fica como está no código"). Ver `docs/status/2026-11-12-docs-d10-d11-adr0008-concorrencia-api.md`.

A lista completa de testes por requisito está na [RTM](rtm.md). Aqui fica a situação, com os testes principais.

**Histórico:**
- 11/11/2026, PR #44: back-end em camadas, `verify` local com 965 testes;
- 11/11/2026: RF-13, telas 7 e 8 com teste de contrato e E2E;
- 07/11/2026: RF-05, RF-06, RF-10 e RF-12 só no nível do banco, com a V1.

| Requisito | Situação | Evidência (arquivo e nome do teste) | Risco se ficar de fora | Dono |
|---|---|---|---|---|
| RF-01 Login e permissão por perfil | feito | `AutenticacaoApiControllerTest` (login por e-mail, caixa e espaços, mesma resposta para e-mail inexistente e senha errada; DEF-010); `UsuarioApiControllerTest` (cadastro com nome completo, e-mail e senha); `MatrizAutorizacaoTest` (222 casos, API e telas, `matrizCobreExatamenteAsRotasDoCodigo`); `PaginaNaoEncontradaIntegracaoTest` (login das telas com servidor real); limite de tentativas em `LimiteDeTentativasDoLoginIntegracaoTest`, `LimiteDeTentativasApiTest` e `LimiteDeTentativasE2ETest`; `AcessoE2ETest`. Run 43303372662 | Falha de autorização zera a nota | Pedro |
| RF-02 Cadastro e consulta | parcial | `RecursoApiControllerTest` (`administradorCadastraEAlteraSemMudarOTipo`, `filtraPorTipoECapacidadeMinima`), `UsuarioApiControllerTest`, `TelasComBancoTest.administradorCadastraEEditaRecurso`. Falta gravar localização, patrimônio e competências (V2, Gustavo) | Catálogo incompleto | Pedro |
| RF-03 Pesquisa | parcial | `RecursoApiControllerTest.disponiveisExcluiReservaQueOcupaEManutencaoMasNaoACancelada`, `TelasComBancoTest.buscaMostraSoOQueEstaLivreNoHorario`. Falta localização e competência (V2) | Busca sem dois dos filtros do PRD | Pedro |
| RF-04 Criar, alterar e cancelar reservas | feito | `ReservaApiControllerTest` (`Criar`, `Alterar`, `Cancelar`), `TelasComBancoTest.alterarTrocaOPeriodoNoBancoECancelarLiberaOHorario`, `ReservaE2ETest`. Regra de quando alterar e cancelar: proposta D9 testada, decisão pendente | | Pedro |
| RF-05 Sobreposição de horário (inclusive professor) | feito | `SobreposicaoBancoTest`; `ReservaApiControllerTest.Sobreposicao.agendaDoProfessorTambemEhProtegidaMesmoEmOutraSala`; `ReservaE2ETest.conflitoRealDeHorarioExplicaOMotivoComFocoEOfereceVoltarABusca` | Duas turmas na mesma sala | Pedro |
| RF-06 Reserva dupla em pedidos simultâneos | feito | `ConcorrenciaReservaBancoTest`; `ConcorrenciaReservaApiTest` (20 pedidos HTTP → 1 × 201 e 19 × 409, sala e professor, 5 rodadas); carga do RNF-03. Vermelho sem a proteção nos dois níveis: mutação M1 de `docs/testes/mutacao-manual.md` (API: 10 de 10 vermelhos em 12/11/2026, só local) | Reserva dupla zera a nota | Gustavo / Pedro |
| RF-07 Aprovação de recurso restrito | feito | `ReservaApiControllerTest.Aprovacao`, `TelasComBancoTest.recursoRestritoEsperaAprovacaoEOResponsavelAprovaNaFila`. Estado inicial sem restrito = APROVADA (D10, decidida em 12/11/2026): `ReservaApiControllerTest.Criar.salaComProfessorSemRestritoNasceAprovadaComAuditoria`. Sem teste para o Responsável rejeitar a própria reserva (D10 (c)) | | Pedro |
| RF-08 Bloqueio por manutenção | feito | `ReservaApiControllerTest.Manutencao.fronteirasDaManutencaoDe10a12`, `RecursoApiControllerTest.manutencaoECadastradaEListadaComPeriodoInvalidoRecusado`. Efeito sobre reservas existentes (D11, decidida em 12/11/2026: continuam como estão): `TelasComBancoTest.bloqueioComReservaNoPeriodoPedeConfirmacaoAntesDeGravar`, sem teste da mesma regra na API; corrida da D13 aceita (ADR-0008) | | Pedro |
| RF-09 Retirada e devolução | feito | `ReservaApiControllerTest.CicloDeUso.retiradaEDevolucaoLevamAteConcluidaComAuditoriaDeCadaPasso`, `TelasComBancoTest.retiradaEDevolucaoMudamOEstadoEOHistoricoMostraCadaPasso` | | Pedro |
| RF-10 Histórico auditável | feito | `AuditoriaBancoTest`, `AuditoriaServiceTest`, `ReservaApiControllerTest.Aprovacao.responsavelAprovaRestritoEHistoricoGuardaQuemEQuando`, `HistoricoPaginaTest` | | Gustavo / Pedro |
| RF-11 Notificação externa | ausente | Sem código, sem WireMock | Requisito do PRD sem entrega | Pedro |
| RF-12 Relatório de utilização | parcial | `RelatorioUtilizacaoBancoTest`, `RelatorioApiControllerTest.valoresCalculadosAMaoComBordaCanceladaEConflito`, `TelasComBancoTest.relatorioContaReservasEHorasNoPeriodoDoFusoDasTelas`. Premissas de cálculo pendentes (D8) | Número do relatório contestado | Gustavo / Pedro / Leandro |
| RF-13 Telas responsivas e erros claros | feito (375 px) | 27 classes de contrato de tela (511 testes), `TelasComBancoTest` (24), E2E Playwright em 375 px (62 testes em 5 classes, contra o back-end real); prints em `docs/evidencias/e2e-375/`. 768 e 1280 px só conferidos à mão | | Leandro |
| RF-14 Documentação da API | feito | `OpenApiDocumentacaoTest` (5 testes; a documentação bate com a matriz rota a rota); Swagger UI em `/swagger-ui/index.html`, com login. `./mvnw -B verify` local de 13/11/2026 (1138 testes, 0 falhas); run do CI do PR #61 | Integrador sem contrato | Pedro |
| RNF-01 Busca: p95 ≤ 300 ms com 50 usuários | feito (local) | `perf/reservas.jmx`; `perf/resultados/rodada-01.jtl` (p95 = 17 ms) e `rodada-02.jtl` (p95 = 8 ms, 50 ativos); `docs/performance/resultado-01.md`. Meta a validar com o professor | Perde o RNF-01 | Leandro |
| RNF-02 Cobertura ≥ 80% linhas, ≥ 70% branches | feito | Gate do JaCoCo no run 43303372662; 95,8% das linhas e 81,3% dos branches no `verify` local de `6ce4478` | Pipeline vermelho | Gustavo |
| RNF-03 Criação sob carga: só 201 ou 409 | feito (local) | Asserção de `perf/reservas.jmx`: 80 × 201 e 3920 × 409, nenhum outro código; `ConcorrenciaReservaApiTest` | Erro 500 sob disputa | Leandro |

As regras de negócio (RN-01 a RN-13) estão na [RTM](rtm.md): 13 `feito` desde 13/11/2026: a RN-08 deixou de ser `parcial` com `ReservaApiControllerTest.AuditoriaDeCadaTransicao` (PR #61).

## Pendências e divisão

Em ordem de prioridade, primeiro o que bloqueia a tag `v1.0.0-rc1` (entrega em 13/11/2026). Fonte das lacunas: `docs/plano-execucao.md`, conferência de 12/11/2026.

| # | Pendência | Bloqueia a tag? | Quem pega | Situação em 12/11/2026, noite |
|---|---|---|---|---|
| 1 | Clone limpo seguindo só o README e `docs/entrega/checklist.md` (lacuna 11). É o único limitador sem prova: "aplicação executa" | Sim | Leandro ou Pedro (quem não escreveu o README) | Roteiro em `docs/entrega/roteiro-clone-limpo.md`. Rodada feita **pela IA** em 13/11/2026 (PR #60, `9acbbcd`): verde. **Falta a rodada humana** (Pedro) |
| 2 | `docs/decisao-qualidade.md` (lacuna 2): decisão, critérios com evidência, riscos aceitos e quem fez o quê | Sim | Leandro | Mergeada no PR #59 e atualizada em 13/11/2026; faltam as declarações dos integrantes e a conferência no commit da tag |
| 3 | Números no commit da tag: refazer a seção 4 do relatório e o run da RTM no commit que receber a tag | Sim | Leandro (com o Gustavo, que faz a tag) | Números de `6ce4478` neste PR |
| 4 | Merge do PR #56 (carga JMeter, lacuna 1) | Sim | Pedro (revisão) | Mergeado em 13/11/2026 |
| 5 | D4: perguntar ao professor se "0 vulnerabilidades críticas" vale pelo CVSS das dependências ou pelo Quality Gate | Não, mas muda a decisão de qualidade | Gustavo | Pergunta pessoalmente na Aula 14 (13/11) |
| 6 | Decisões pendentes D8 (relatório) e D9 (alterar e cancelar). D10 (estado inicial e limites de perfil) e D11 (manutenção sobre reservas existentes) decididas em 12/11/2026 no PR #58: "fica como está no código" | Não: o código segue a proposta, testada | Grupo (Pedro propõe) | Pendentes desde 11/11; se não forem decididas, entram como premissa na decisão de qualidade |
| 7 | BDD executável com Cucumber e `docs/testes/evidencia-tdd.md` (lacuna 4) | Não, mas pesa em "qualidade dos testes" | Pedro | Feito em 13/11/2026 (PR #61): 13 cenários Cucumber no `verify` e `docs/testes/evidencia-tdd.md` |
| 8 | Mutação manual das regras do serviço (aprovação, manutenção, autorização) em `docs/testes/mutacao-manual.md` (lacuna 6) | Não | Pedro | Feito em 13/11/2026 (PR #61): 8 mutações, todas pegas; a MAU1 achou a lacuna da RN-08 |
| 9 | `docs/testes/plano-tdd.md` (lacuna 7) e OpenAPI do RF-14 (lacuna 8) | Não | Pedro | Feitos em 13/11/2026 (PR #61): `plano-tdd.md` e OpenAPI com `OpenApiDocumentacaoTest` |
| 10 | RF-11 (notificação com WireMock) | Não (risco aceito se ficar de fora) | Pedro | Fora desta entrega: **aceito como risco pelo Pedro em 13/11/2026** (`docs/decisao-qualidade.md`, risco 5) |
| 11 | RN-08: conferir a auditoria do cancelamento e do não comparecimento em teste | Não | Pedro | Feito em 13/11/2026 (PR #61): `AuditoriaDeCadaTransicao`, 4 testes |
| 12 | Perguntas da prova oral e ensaio (lacuna 12) | Não (Semana 47) | Todos | Perguntas: 15 de 15 (PR #62, 13/11/2026). **Ensaio cronometrado não feito** (humano) |
| 13 | Pedro confirmar a mudança no `AutenticacaoService` feita pelo Leandro no PR #52 (pendência da aprovação do Gustavo) | Não | Pedro | **Confirmado em 13/11/2026** (Pedro): o hash é conferido antes da recusa por bloqueio, e login inexistente, senha errada e login bloqueado dão a mesma resposta (lido em `AutenticacaoService.entrar`), a tentativa durante o bloqueio não estende o bloqueio e o sucesso zera o contador; testes `LimiteDeTentativasApiTest` e `LimiteDeTentativasDoLoginIntegracaoTest`. Ver `docs/status/2026-11-13-docs-pedro-confirmacoes-1311.md` |

Decisões já tomadas em 12/11/2026: D12 (autenticação das telas, opção (a), ADR-0009) e D13 (RN-04 "consulta e depois grava", risco aceito, ADR-0008).
