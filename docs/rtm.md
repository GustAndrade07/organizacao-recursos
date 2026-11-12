# RTM — Matriz de Rastreabilidade

Requisito → risco → teste → evidência, para todos os requisitos do [PRD](prd.md): RF-01 a RF-14, RN-01 a RN-13 e RNF-01 a RNF-03. Dono: Leandro. Atualizado em 11/11/2026 (PR #42).

**Como ler**
- **Situação** usa a mesma escala de [`status-requisitos.md`](status-requisitos.md). `feito`: o requisito inteiro tem teste. `parcial`: a camada que garante a regra tem teste em parte (hoje, quase sempre o banco), e falta o resto. `ausente`: nenhuma camada que garante a regra tem teste. Teste só de tela não tira um requisito de `ausente`, exceto o RF-13, que é um requisito da própria tela.
- **Teste** cita só arquivos que existem no repositório. A camada vem entre colchetes:
  - [banco]: PostgreSQL em Testcontainers, `src/test/java/br/com/senac/recursos/banco`;
  - [tela]: teste de contrato do HTML, `.../web`;
  - [E2E]: Playwright em 375 px, `.../e2e`, com dados de teste.
  - "A escrever" é o teste que falta, com o nível previsto no [plano de teste](plano-de-teste.md).
- **Evidência** aponta o run do CI ou o arquivo onde o resultado pode ser conferido. O run **43091027823** (GitHub Actions, job `verify`, commit `3d63d72` do PR #42, 11/11/2026) rodou toda a suíte: 636 testes, 0 falhas, 0 ignorados. Ele cobre todos os testes citados abaixo.
- Teste de tela e E2E provam o que a tela mostra e o que sai do navegador. **Não** provam a regra: quem a garante é o banco ou o serviço do back-end.

## Resumo

| | Total | feito | parcial | ausente |
|---|---|---|---|---|
| Requisitos funcionais (RF) | 14 | 0 | 5 (RF-05, 06, 10, 12 e 13) | 9 |
| Regras de negócio (RN) | 13 | 0 | 6 (RN-01, 02, 03, 05, 09 e 12) | 7 |
| Requisitos não funcionais (RNF) | 3 | 1 (RNF-02) | 0 | 2 |

**Críticos.** A [spec principal](https://app.notion.com/p/40e8bb977b7b8155b440c12880c46df1) marca 2 RFs como críticos, porque zeram a nota:
- **RF-06** (reserva dupla): ligado a teste e a run do CI, mas só no banco. Falta o 409 de ponta a ponta.
- **RF-01** (login e permissão): sem teste da regra. A autenticação ainda não existe.

A meta do plano, "100% dos críticos ligados a teste e run do CI", ainda **não** foi atingida.

## Requisitos funcionais

| Requisito | Risco | Teste | Evidência | Situação | Dono |
|---|---|---|---|---|---|
| RF-01 Login e permissão por perfil | Falha de autorização zera a nota (limitador) | [tela] `LoginPaginaTest`, `LayoutBaseTest` (menu por perfil: `responsavelNaoVeItensDeSolicitante`), `CaminhoDoSiteTest`. [E2E] `AcessoE2ETest` (avisos, envio, menu, 401/403). **A escrever:** matriz de autorização parametrizada (API, MockMvc com token de cada perfil) | Run 43091027823 (só tela) | ausente | Pedro |
| RF-02 Cadastro e consulta de salas, professores e materiais | Catálogo errado: reserva de recurso que não existe ou com dados errados | [banco] `EsquemaBancoTest` (tipo, capacidade, recurso com reserva não se apaga). [tela] `ListaRecursosTest`, `RecursoFormTest`, `RecursoControllerTest`. **A escrever:** API de cadastro e consulta | Run 43091027823 | ausente | Pedro |
| RF-03 Pesquisa por tipo, capacidade, localização, competência e disponibilidade | Usuário não acha recurso livre e reserva por tentativa e erro | [tela] `BuscaPaginaTest`, `ResultadosBuscaTest`. [E2E] `ReservaE2ETest` (fluxo da busca à reserva). **A escrever:** consulta de disponibilidade (serviço e API); filtro de competência fora da tela | Run 43091027823 (só tela) | ausente | Pedro |
| RF-04 Criar, alterar e cancelar reservas | Reserva que não grava, não muda ou não cancela; cancelamento de reserva já iniciada | [tela] `NovaReservaPaginaTest`, `NovaReservaEstadosTest`, `MinhasReservasPaginaTest`, `ReservaDetalhePaginaTest`. [E2E] `ReservaE2ETest` (um POST com os campos certos, alterar, cancelar). **A escrever:** `POST /reservas`, `POST /reservas/{id}`, `POST /reservas/{id}/cancelar` | Run 43091027823 (só tela) | ausente | Pedro |
| RF-05 Horário sobreposto, inclusive na agenda do professor | Duas turmas na mesma sala ou o professor em dois lugares | [banco] `SobreposicaoBancoTest` (`sobreposicaoNoMesmoRecursoERecusadaPeloBancoEODesfazTudo`, `sobreposicaoNaAgendaDoProfessorERecusadaMesmoComSalaDiferente`, `fronteirasDoIntervaloSemiaberto`). Mutação M1 em [`mutacao-manual.md`](testes/mutacao-manual.md). [tela] aviso de 409 em `NovaReservaEstadosTest` e `ReservaE2ETest`. **A escrever:** serviço que traduz `23P01` em 409 e o teste de API | Run 43091027823 | parcial | Pedro |
| RF-06 Impedir reserva dupla com pedidos simultâneos | Reserva dupla zera a nota (limitador) | [banco] `ConcorrenciaReservaBancoTest` (20 pedidos → 1 aceito e 19 recusados, contagem no banco, recurso e professor, 5 rodadas), `MigracoesBancoTest` (constraint no catálogo). Mutações M1 e M2. **A escrever:** 20 pedidos pela API → 1 × 201 e 19 × 409 | Run 43091027823 | parcial | Gustavo |
| RF-07 Aprovação obrigatória para recurso restrito | Laboratório usado sem autorização | [tela] `FilaAprovacaoTest`. [E2E] `ResponsavelEAdministradorE2ETest` (rejeitar exige motivo; pedido próprio sem botões). **A escrever:** BDD `aprovacao-bloqueio.feature` e teste de API | Run 43091027823 (só tela) | ausente | Pedro |
| RF-08 Bloquear recurso em manutenção | Sala em reforma reservada | [banco] `EsquemaBancoTest` (`manutencaoAceitaPeriodoSemiabertoParaRecursoExistente`, `manutencaoComPeriodoForaDoPadraoSemiabertoERecusada`, `manutencaoDeRecursoInexistenteERecusada`: só a tabela, sem barrar reserva). [tela] `ManutencaoPaginaTest`. [E2E] fim antes do início barrado. **A escrever:** recusa da reserva no período (BDD, fronteiras 09:00–10:00 e 12:00–13:00) | Run 43091027823 | ausente | Pedro |
| RF-09 Registrar retirada e devolução de material | Equipamento sai sem registro | [tela] `RetiradaDevolucaoTest`. [E2E] `ResponsavelEAdministradorE2ETest` (envio único, não compareceu). **A escrever:** transições `APROVADA → EM_USO → CONCLUIDA` no serviço | Run 43091027823 (só tela) | ausente | Pedro |
| RF-10 Histórico auditável de mudanças | Não se sabe quem mudou a reserva nem por quê | [banco] `EsquemaBancoTest` (`auditoriaGuardaReservaUsuarioInstanteEstadoAnteriorEstadoNovoEMotivo`, `primeiroRegistroDaReservaNaoTemEstadoAnterior`), `AuditoriaBancoTest` (mutações M3a e M3b). [tela] `HistoricoPaginaTest`. [E2E] histórico sem ação de editar. **A escrever:** serviço que grava a auditoria e consulta do histórico | Run 43091027823 | parcial | Gustavo / Pedro |
| RF-11 Notificação simulada ou integração com API externa | Integração quebrada derruba a reserva | **A escrever:** adapter HTTP com WireMock (200, 4xx, 5xx, timeout, JSON inválido), ADR-0005 | — | ausente | Pedro |
| RF-12 Relatório de utilização | Número errado no relatório que o professor confere à mão | [banco] `RelatorioUtilizacaoBancoTest` (período vazio, reserva cruzando a borda, cancelada não conta; valores calculados à mão; mutações M4 e M5), `TentativaRecusadaBancoTest`. [tela] `RelatorioPaginaTest`. [E2E] fim antes do início barrado. Premissas pendentes (D8). **A escrever:** `GET /relatorios/utilizacao` caixa-preta | Run 43091027823 | parcial | Gustavo / Pedro / Leandro |
| RF-13 Telas responsivas e mensagens de erro claras | Tela que não serve no celular (375 px) ou erro sem explicação: o Solicitante desiste ou repete o pedido | [tela] 20 classes de contrato (lista em `status-requisitos.md`) e `CssSemValoresSoltosTest`. [E2E] `Responsividade375E2ETest` (28 telas: sem rolagem horizontal, um `h1`, tabela vira cartão, alvo de toque ≥ 44 px), `AcessoE2ETest`, `ReservaE2ETest`, `ResponsavelEAdministradorE2ETest` | Run 43091027823 e artifact `e2e-375` (prints); amostra em `docs/evidencias/e2e-375/` | parcial | Leandro |
| RF-14 Documentação da API (OpenAPI) | Integrador sem contrato da API | **A escrever:** teste que lê `/v3/api-docs` e confere as rotas | — | ausente | Pedro |

## Regras de negócio

| Regra | Risco | Teste | Evidência | Situação | Dono |
|---|---|---|---|---|---|
| RN-01 Término depois do início | Reserva com intervalo vazio ou invertido | [banco] `SobreposicaoBancoTest.periodoComTerminoAntesDoInicioERecusadoPeloBanco`. [E2E] `ReservaE2ETest` (barrado antes do envio, na nova reserva e na alteração; mutação E1 do README do front-end). **A escrever:** 400 no serviço | Run 43091027823 | parcial | Pedro |
| RN-02 Reservas do mesmo recurso não se sobrepõem | Mesmo que RF-05 | [banco] `SobreposicaoBancoTest` (recurso, professor, fronteiras da D1, estados da D5) | Run 43091027823 | parcial | Gustavo / Pedro |
| RN-03 Pedidos simultâneos geram uma única reserva aceita | Mesmo que RF-06 | [banco] `ConcorrenciaReservaBancoTest` | Run 43091027823 | parcial | Gustavo / Pedro |
| RN-04 Recurso em manutenção não pode ser reservado | Mesmo que RF-08 | [tela] aviso de manutenção em `NovaReservaEstadosTest`. **A escrever:** recusa no serviço e cenários BDD com fronteiras | Run 43091027823 (só tela) | ausente | Pedro |
| RN-05 Estados e transições da reserva | Transição inválida (ex.: `CONCLUIDA → APROVADA`) aceita | [banco] `SobreposicaoBancoTest.estadoForaDaMaquinaDeEstadosERecusadoPeloBanco`, `EsquemaBancoTest.auditoriaComEstadoForaDaMaquinaDeEstadosERecusada` (só os nomes). [tela] `EstadoBadgeTest`, `EstadosDaReservaCssTest`, ações por estado em `RetiradaDevolucaoTest`. **A escrever:** máquina de estados no serviço (D3: 409) | Run 43091027823 | parcial | Pedro |
| RN-06 Só o Responsável aprova; ninguém aprova a própria reserva | Autoaprovação ou Solicitante aprovando: falha de autorização | [tela] `FilaAprovacaoTest` e [E2E] `pedidoDoProprioResponsavelNaoTemAprovarNemRejeitar`: a tela só não oferece o botão, o que não é barreira. **A escrever:** 403 na API e BDD | Run 43091027823 (só tela) | ausente | Pedro |
| RN-07 Reserva já iniciada não pode ser apagada | Histórico some ou reserva em uso é cancelada | [tela] Alterar e Cancelar só sem `iniciada`: `MinhasReservasPaginaTest`, `ReservaDetalhePaginaTest` (proposta D9; mutação de RN-07 deixou 5 testes vermelhos). **A escrever:** recusa no serviço | Run 43091027823 (só tela) | ausente | Pedro |
| RN-08 Toda mudança de estado gera auditoria | Mudança sem rastro | [banco] `AuditoriaBancoTest` grava os dois juntos quando o teste grava os dois; quem garante "toda mudança" é o serviço. **A escrever:** teste do serviço por transição | Run 43091027823 | ausente | Pedro |
| RN-09 Auditoria guarda reserva, usuário, instante (`Clock`), estado anterior, novo e motivo | Auditoria incompleta | [banco] `EsquemaBancoTest.auditoriaGuardaReservaUsuarioInstanteEstadoAnteriorEstadoNovoEMotivo`. **A escrever:** instante vindo do `Clock` injetado (serviço) | Run 43091027823 | parcial | Gustavo / Pedro |
| RN-10 Auditoria não pode ser alterada nem apagada pela API | Rastro adulterado | [tela] `HistoricoPaginaTest` (sem editar nem apagar). **A escrever:** ausência de rota de escrita na API | Run 43091027823 (só tela) | ausente | Pedro |
| RN-11 Histórico só para Responsável e Administrador (Solicitante: 403) | Falha de autorização | **A escrever:** 403 na matriz de autorização | — | ausente | Pedro |
| RN-12 Auditoria na mesma transação da mudança de estado | Auditoria órfã depois de rollback | [banco] `AuditoriaBancoTest` (falha em cada direção desfaz a outra; mutações M3a e M3b) | Run 43091027823 | parcial | Gustavo / Pedro |
| RN-13 Relatório só para Responsável e Administrador (sem token: 401; Solicitante: 403) | Falha de autorização | **A escrever:** 401 e 403 na matriz de autorização | — | ausente | Pedro |

## Requisitos não funcionais

| Requisito | Risco | Teste | Evidência | Situação | Dono |
|---|---|---|---|---|---|
| RNF-01 Busca: p95 ≤ 300 ms com 50 usuários (a validar) | Busca lenta no horário de pico | **A escrever:** `perf/reservas.jmx` (JMeter em modo CLI, local); `docs/performance/rnf.md` antes da rodada | **Não medido:** o plano JMeter e a busca no back-end ainda não existem (`perf/` só tem o README) | ausente | Leandro |
| RNF-02 Cobertura ≥ 80% de linhas e ≥ 70% de branches | Código sem teste | Gate do JaCoCo no `pom.xml` (o build falha abaixo da meta) | Run 43091027823: "All coverage checks have been met". `target/site/jacoco/jacoco.csv` local do `verify` de `3d63d72` (11/11/2026): 100,0% das linhas (106 de 106) e 94,0% dos branches (79 de 84). **Limite:** hoje o código de produção é só a camada de tela (controllers); o número vai mudar quando o back-end entrar | feito | Gustavo |
| RNF-03 Criação de reserva sob carga: só 201 ou 409, nunca 500 | Erro 500 sob disputa | **A escrever:** asserção no plano JMeter | **Não medido:** o plano JMeter e o `POST /reservas` ainda não existem | ausente | Leandro |

## Testes do back-end (PR #44) — a incorporar nas linhas acima

> Seção acrescentada pelo Pedro no PR #44 (11/11/2026). As linhas acima são do Leandro e **não** foram alteradas: a escala de situação delas pede o run do CI, que este PR ainda não tem. Depois do run, o dono do RTM troca os "A escrever" pelos testes abaixo e atualiza a situação e o resumo. Camada [API]: MockMvc com token de cada perfil (ou HTTP real, no teste de concorrência) contra PostgreSQL em Testcontainers, `src/test/java/br/com/senac/recursos/controller`; [serviço] e [unidade]: `service`, `model`, `enums`, `converter`.

| Requisito | Teste do back-end | Evidência |
|---|---|---|
| RF-01, RN-11, RN-13 | [API] `AutenticacaoApiControllerTest` (token certo, credencial errada sem revelar o login, sem token, token expirado pelo `Clock`, token adulterado); `MatrizAutorizacaoTest` (cada célula de `docs/seguranca/matriz-autorizacao.md`, API e telas, e falha com rota fora da matriz) | local, ver `docs/status/2026-11-11-feat-backend-camadas.md`; run do CI a anotar |
| RF-02 | [API] `RecursoApiControllerTest` (cadastro, alteração sem trocar o tipo, 404, filtros), `UsuarioApiControllerTest` | idem |
| RF-03 | [API] `RecursoApiControllerTest.disponiveisExcluiReservaQueOcupaEManutencaoMasNaoACancelada` (tipo, capacidade e disponibilidade; localização e competência dependem da V2) | idem |
| RF-04, RN-01, RN-07 | [API] `ReservaApiControllerTest` (`Criar`, `Alterar`, `Cancelar`: término antes do início, só o dono, reserva iniciada recusada na fronteira de 1 s); [unidade] `PeriodoTest` | idem |
| RF-05, RN-02 | [API] `ReservaApiControllerTest.Sobreposicao` (trinca de fronteiras da D1, professor em outra sala, cancelar libera) | idem |
| RF-06, RN-03, RNF-03 | [API] `ConcorrenciaReservaApiTest` (20 pedidos HTTP → 1 × 201 e 19 × 409, nunca 500, contagem no banco, sala e professor, 5 rodadas cada) | idem |
| RF-07, RN-06 | [API] `ReservaApiControllerTest.Aprovacao` (Responsável aprova; ninguém aprova a própria reserva: 403 e a reserva continua SOLICITADA) | idem |
| RF-08, RN-04 | [API] `ReservaApiControllerTest.Manutencao` (trinca de fronteiras 10:00 a 12:00, 409 de tipo próprio) | idem |
| RF-09, RN-05 | [API] `ReservaApiControllerTest.CicloDeUso` (APROVADA → EM_USO → CONCLUIDA, transição inválida 409 da D3); [unidade] `EstadoReservaTest` (49 pares) | idem |
| RF-10, RN-08, RN-09, RN-10, RN-12 | [serviço] `AuditoriaServiceTest` (auditoria fora de transação recusada; rollback leva a auditoria junto); [API] histórico com quem, quando e motivo em `ReservaApiControllerTest.Aprovacao`; RN-10: a API não tem rota de escrita na auditoria (`MatrizAutorizacaoTest.matrizCobreExatamenteAsRotasDoCodigo`) e o repositório não tem `delete` | idem |
| RF-12 | [API] `RelatorioApiControllerTest` (valores calculados à mão: borda, cancelada, conflito dentro e fora da janela, período vazio, 401 e 403); premissas pendentes (D8) | idem |

## Telas ligadas ao back-end (12/11/2026, D12 opção (a)) — a incorporar nas linhas acima

> Seção acrescentada pelo Leandro em 12/11/2026, no PR que liga as telas aos services (ADR-0009). Como na seção do Pedro acima, a coluna **Situação** e o resumo do topo **não** foram recontados aqui: a recontagem vai junto com a do back-end, com o run do CI deste PR como evidência. Testes conferidos por nome no código desta branch.

| Requisito | Teste das telas com o back-end | Evidência |
|---|---|---|
| RF-01, RN-11, RN-13 | [tela + banco] `MatrizAutorizacaoTest` (tabela "Telas": cada perfil na sessão, anônimo levado ao login, CSRF em todo `POST`); `PaginaNaoEncontradaIntegracaoTest` (servidor real: login com CSRF e cookie, senha errada volta com o login e sem a senha, destino revalidado contra redirecionamento aberto, 403 para URL fora da matriz e 404 de reserva inexistente com sessão); `TelasComBancoTest.inicioLevaCadaPerfilParaATelaDoSeuMenu` e `menuMostraONomeEOPerfilDeQuemEntrouEMarcaAPaginaAtual`. [E2E] `AcessoE2ETest` (`loginRealLevaDeVoltaAPaginaPedidaEMostraOMenuDoPerfil`, `loginRealComSenhaErradaVoltaComOAvisoEOLoginDigitado`, `sairEncerraASessaoEAsTelasVoltamAPedirLogin`, `solicitanteRecebe403NaTelaDoResponsavel`) | Run do CI deste PR; local em 12/11/2026 |
| RF-03 | [tela + banco] `TelasComBancoTest.buscaMostraSoOQueEstaLivreNoHorario` | idem |
| RF-04, RN-01, RN-07 | [tela + banco] `TelasComBancoTest` (`novaReservaGravaNoBancoNoFusoDasTelasELevaAoDetalheComAConfirmacao`, `terminoAntesDoInicioVoltaComOErroNoCampoSemChamarOBanco`, `minhasReservasMostraSoAsDoUsuarioLogadoMesmoParaQuemVeTodas`, `solicitanteNaoAbreAReservaDeOutraPessoa`, `alterarTrocaOPeriodoNoBancoECancelarLiberaOHorario`, `reservaQueJaComecouNaoOfereceAlterarNemCancelar`). [E2E] `ReservaE2ETest` (`fluxoRealDaBuscaAteAReservaGravada...`, `minhasReservasReaisOferecemCancelarECancelarMudaOEstado`) | idem |
| RF-05, RN-02, RN-03 | [tela + banco] `TelasComBancoTest.horarioOcupadoVoltaAoFormularioComOAvisoDeConflitoEONaoGravaASegunda` (409, uma reserva e uma tentativa recusada no banco). [E2E] `ReservaE2ETest.conflitoRealDeHorarioExplicaOMotivoComFocoEOfereceVoltarABusca` | idem |
| RF-07, RN-06 | [tela + banco] `TelasComBancoTest` (`recursoRestritoEsperaAprovacaoEOResponsavelAprovaNaFila`, `responsavelNaoAprovaAPropriaReservaETelaMostraOAviso`, `rejeitarExigeMotivoEOMotivoApareceParaQuemPediu`). [E2E] `ResponsavelEAdministradorE2ETest.pedidoRealDeRecursoRestritoEhAprovadoNaFilaEOSolicitanteVeAprovada` | idem |
| RF-08, RN-04 | [tela + banco] `TelasComBancoTest` (`recursoEmManutencaoVoltaComOPeriodoDaManutencao`, `bloqueioComReservaNoPeriodoPedeConfirmacaoAntesDeGravar`) | idem |
| RF-09, RN-05 | [tela + banco] `TelasComBancoTest` (`retiradaEDevolucaoMudamOEstadoEOHistoricoMostraCadaPasso`, `devolverReservaQueNaoEstaEmUsoRespondeConflitoSemMudarNada`, `naoCompareceuSoEOferecidoDepoisQueOHorarioTermina`) | idem |
| RF-10, RN-09 | [tela + banco] `TelasComBancoTest.retiradaEDevolucaoMudamOEstadoEOHistoricoMostraCadaPasso` e `rejeitarExigeMotivoEOMotivoApareceParaQuemPediu` | idem |
| RF-12 | [tela + banco] `TelasComBancoTest.relatorioContaReservasEHorasNoPeriodoDoFusoDasTelas` (cancelada fica de fora; período em dias de São Paulo) | idem |
| RF-01 (cadastro de usuários, tela 16) | [tela] `UsuariosPaginaTest` (8: lista sem senha, campos, erros com as mensagens da API, login repetido, perfil inválido, 403 para o Responsável); [tela + banco] `TelasComBancoTest.administradorCadastraUsuarioComSenhaSoEmHashEOUsuarioNovoEntra` e `loginRepetidoNaoCriaSegundoUsuario`; [E2E] `ResponsavelEAdministradorE2ETest.administradorCadastraUsuarioPelaTelaEOUsuarioNovoEntra` | idem |
| RF-10, RN-11 (link do histórico) | [tela + banco] `TelasComBancoTest.historicoTemLinkNoDetalheENaRetiradaSoParaQuemPodeVer`; [E2E] `ResponsavelEAdministradorE2ETest.responsavelAbreOHistoricoPelaRetiradaEDevolucao` | idem |
| RF-02 | [tela + banco] `TelasComBancoTest` (`administradorCadastraEEditaRecurso`, `cadastroSemCapacidadeVoltaComOErroENaoGrava`). Localização, patrimônio e competências **não** são gravados (V2) | idem |

**Mutações feitas à mão em 12/11/2026, nesta máquina** (cada uma desfeita depois): perfil de `GET /aprovacoes` trocado por "qualquer logado" (4 falhas em `MatrizAutorizacaoTest`); filtro "só as minhas" removido (1 falha em `TelasComBancoTest`); fuso das telas trocado por UTC (3 falhas em `TelasComBancoTest`); destino do login sem `CaminhoDoSite` (1 falha em `PaginaNaoEncontradaIntegracaoTest`, redirecionamento para `http://evil.example`).
