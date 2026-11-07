# Qualidade de Software 2026.2 — Auditoria da matéria e plano de execução

> Fonte: Hub de Aulas do Prof. Afonso (home, especificação do projeto, escopo da prova, página de entrega e materiais/slides das Aulas 01 a 17), lidos em 01/11/2026. Materiais das Aulas 09 a 15 e a especificação reconferidos em 04/11/2026 (https://afonsolelis.github.io/aulas_senac/pages/home_qualidade_2026_2.html).
> Plano escrito no **domingo, 01/11 (fim da Semana 44)**. Turma **STADSCAS4ND (sexta, 19h10–22h45, salas C237 / D156)**.
> **Entrega: sexta 13/11** (Aula 14), com meta interna de terminar antes. Prova oral: **sexta 20/11** (Semana 47).
> Grupo (3): Gustavo Oliveira Fernandes de Andrade · Leandro Sato · Pedro Henrique (conferir o nome completo com a lista da turma).
>
> **Status em 07/11/2026 (sábado):** PRs #27 (regra das specs) e #28 (V1 e testes de banco) abertos, com o Leandro como revisor e **sem aprovação ainda**; o Quality Gate do SonarCloud passou no #28 (0 issues). Neste PR entram também `docs/banco/schema.sql` (esquema consolidado gerado com `pg_dump` da V1, só estrutura, sem dados nem credenciais) e a correção do README (teste sem Docker falha, não é ignorado; `verify` com 501 testes e 0 ignorados depois do merge da `main` de 07/11; 74 deles são de banco). **Specs do Notion atualizadas em 07/11:** Specs do Banco (seções 1, 3, 4, 5 e 6 e nova seção 7 com o diagrama ER e a V1) e Specs do Back-end (contrato com o banco, D6, D7 e pontos em aberto); as caixas da seção 6 do Banco só serão marcadas depois do merge do #28. **Falta, com dono:** revisão do Leandro nos dois PRs; Pedro confirmar D6, D7 e os ADRs 0005 a 0007, escrever o serviço que traduz `23P01` em 409 e o teste de API; Gustavo perguntar a D4 ao professor, pedir o clone limpo do README, fazer o CI vermelho proposital e o `ciclo-01.md`. O parágrafo de 06/11 abaixo continua valendo no que não foi superado por este.
>
> **Status em 06/11/2026 (sexta):** PRs #10 a #13 do Gustavo mergeados (plano, `AGENTS.md`, `arquitetura.md`, `plano-de-teste.md`, riscos de dependências, ADRs 0001 a 0006 e decisões D1, D2, D3 e D5 na `main`). Etapa 3, parte do banco, no PR `feat/banco-v1`: migração Flyway **V1** (`V1__nucleo_da_reserva.sql`) e testes de banco em TDD (`test(red)` → `feat(green)`), `AplicacaoSmokeTest` sem `disabledWithoutDocker` (`verify` com 104 testes, 0 ignorados), ADR-0007 (proposto) e decisões D6 e D7 do Gustavo, a confirmar com o Pedro. Achado: sem trava por recurso, 9 de 40 rodadas de 20 pedidos simultâneos deram deadlock (`40P01`). **Falta** (Pedro): serviço de reserva que traduz `23P01` em 409, teste de API, entidades JPA. O parágrafo de 05/11 abaixo continua valendo no que não foi superado por este.
>
> **Status em 05/11/2026 (quinta):** parte do Gustavo da Etapa 2 (arquitetura e ADRs) escrita na branch `docs/etapa-2-adrs`: ADRs 0001 a 0004 com "Consequências" completas e ADRs 0005 (notificação) e 0006 (Flyway) propostos, aguardando revisão. `docs/rtm.md` (Leandro), `docs/testes/plano-tdd.md` e a migração Flyway V1 **não iniciados** (a V1 fica para PR próprio, agora que D1, D2 e D5 estão decididas). Decisões da seção 9 tomadas pelo Gustavo em 05/11: D1, D2, D3 e D5 (a D1 e a D3 a confirmar com Pedro e Leandro na revisão); a D4 segue **aguardando o professor**. O restante deste parágrafo é o estado de 04/11, com ajustes de 05/11.
>
> **Status em 04/11/2026 (quarta, noite):** Etapa 1 quase concluída. Repositório público criado (https://github.com/GustAndrade07/organizacao-recursos); PRs #1 a #8 já tratados (#1, #2, #4, #5, #6, #7 e #8 mergeados; #3 fechado sem merge e substituído pelo #5; os PRs do Gustavo são revisados pelo Leandro); esqueleto Spring Boot 3.5 / Java 21 / Maven Wrapper, CI `verify` + SonarCloud (`SONAR_TOKEN` criado, Quality Gate passou no PR #4) e `main` protegida por ruleset. Docker Desktop instalado e validado nesta máquina em 04/11 (`docker run hello-world`, `docker compose up -d`, `/actuator/health` = `UP`, `./mvnw -B verify` com 21 testes e 0 ignorados). Esqueleto de `docs/arquitetura.md` e `docs/plano-de-teste.md` escrito em 05/11 (proposta a validar com Pedro e Leandro). Ruleset conferido em 05/11 (SonarCloud agora é check obrigatório; bypass vazia). "Dependency risks" lidos em 05/11 (48; tratamento no PR `fix/dependencias-seguras`, ver Etapa 4). Pendentes da Etapa 1: Pedro aceitar o convite do repositório, validar o README em clone limpo (por outra pessoa) e confirmar a reanálise do SonarCloud depois do merge. Etapas 2 a 6 ainda não iniciadas (a Etapa 2 já tem parte do conteúdo no `arquitetura.md`). Regras de contexto para a IA e para o grupo: `AGENTS.md` (raiz). Releitura das Aulas 09 a 15 em 04/11: ajustes marcados com **[Aula NN]** nas etapas abaixo.

---

## 1. O que a matéria é, em uma frase

O professor mostra cada técnica num projeto de exemplo (o **Foot Fanatics**, API de conteúdo esportivo com assinatura) e cada equipe aplica a mesma técnica, na mesma semana, ao **projeto avaliado: o sistema de Organização de Recursos** (reservar salas, professores e materiais sem conflito). **A nota não vem de "funcionar"; vem das evidências de qualidade**: testes, CI, cobertura, rastreabilidade, desempenho e defeitos registrados.

## 2. O que precisa ser construído

**Stack obrigatória:** Java 21 · Spring Boot 3.x · Maven (`./mvnw`) · PostgreSQL · repositório **público** no GitHub · grupo de 1 a 4 pessoas.

### 2.1 Perfis
| Perfil | Pode |
|---|---|
| **Solicitante** (professor/coordenador) | Consultar disponibilidade; criar, alterar e cancelar **as próprias** reservas |
| **Responsável** | Aprovar recursos restritos, validar alocação de docente, registrar retirada/devolução de material |
| **Administrador** | Gerenciar salas, professores, materiais, usuários, bloqueios e manutenções |

### 2.2 Os 14 requisitos funcionais obrigatórios
1. Autenticação e autorização por perfil
2. Cadastro e consulta de salas, professores e materiais
3. Pesquisa por tipo, capacidade, localização, competência e disponibilidade
4. Criar, alterar e cancelar reservas
5. Detectar sobreposição de horário (inclusive na agenda do professor)
6. Proteção contra dupla reserva em pedidos simultâneos
7. Aprovação obrigatória para recursos restritos
8. Bloqueio de recursos em manutenção
9. Registro de retirada e devolução de materiais
10. Histórico auditável de mudanças
11. Notificação simulada ou integração com API externa
12. Relatório de utilização (horas por recurso, carga por professor, conflitos evitados)
13. Interface responsiva e mensagens de erro compreensíveis
14. Documentação da API (OpenAPI/README)

### 2.3 Regras críticas (é aqui que o professor ataca)
- Término **posterior** ao início.
- Reservas do mesmo recurso **não se sobrepõem** (sala, material **ou professor**).
- Duas solicitações simultâneas → **uma única** reserva aceita.
- Recurso em manutenção não pode ser reservado.
- Máquina de estados: `SOLICITADA → APROVADA → EM_USO → CONCLUÍDA` + alternativas `REJEITADA`, `CANCELADA`, `NAO_COMPARECEU`.
- Só Responsável aprova recurso restrito; Solicitante não aprova a própria reserva.
- Reserva iniciada não pode ser apagada.
- Toda mudança de estado gera auditoria (na mesma transação).

### 2.4 Estratégia de qualidade obrigatória
- Testes: unidade (JUnit 5), **parametrizados**, integração, API caixa-preta e end-to-end.
- **Testcontainers** (PostgreSQL real, nada de H2) + **WireMock** para a integração externa.
- Teste automatizado de **concorrência** provando que não há dupla reserva.
- **TDD ou BDD** com evidência no histórico (commits red → green → refactor).
- **GitHub Actions** em pull request + **JaCoCo** + **SonarCloud** (Quality Gate).
- Não funcionais: **JMeter**, autorização, validação de entrada, erro seguro (sem stack trace/SQL na resposta).
- Mock só em teste de unidade isolado.

### 2.5 Metas
**80% linhas · 70% branches · 100% dos requisitos críticos na RTM · 0 bugs/vulnerabilidades críticas.**
Teste trivial só para subir número **não** garante nota máxima.

### 2.6 Avaliação
| Dimensão | Peso |
|---|---|
| Requisitos e regras de negócio | 15% |
| Estratégia e documentação de testes | 15% |
| **Qualidade dos testes automatizados** | **25%** |
| CI, cobertura e análise estática | 15% |
| Performance, segurança e robustez | 10% |
| Rastreabilidade e gestão de defeitos | 10% |
| Organização, apresentação e autoria | 10% |

**Limitadores de nota (derrubam tudo):** aplicação que não executa · pipeline ausente · falha de autorização · possibilidade de dupla reserva.
→ Esses quatro são a prioridade zero do plano.

### 2.7 Como o professor confere (resumo das "checagens na mesa")
- Clona o repo e segue **só o README**.
- Abre a execução do CI **ligada à tag** `v1.0.0-rc1`.
- Remove a proteção de concorrência e roda o teste → tem que ficar vermelho.
- Abre o histórico procurando o commit **vermelho** de um cenário TDD.
- Faz requisição de um Solicitante na reserva de outro → tem que dar 403.
- Provoca erro de validação e lê o corpo da resposta (sem stack trace).
- Escolhe um número do relatório e pede a fonte; abre o `.jtl` do JMeter junto.
- Refaz à mão uma linha do relatório de utilização.
- Escolhe uma linha da RTM e segue até o teste.
- Faz pergunta do banco a um integrante sorteado.

---

## 3. Mapa: aula → o que entra no repositório

| Sem. | Aula | Tema | Artefato no projeto |
|---|---|---|---|
| 32 | 01 | Introdução à qualidade | Diagnóstico (ISO 25010, 5 achados) — exercício, não vai pro repo |
| 33 | 02 | ATAM e casos de teste | 3 personas, utility tree, cenários em 6 partes **com número** |
| 34 | 03 | Caixa branca × preta | Repo criado, `./mvnw test` verde, `README.md`, `docs/prd.md` (personas, RF-xx/RNF-xx numerados e medíveis), `docs/arquitetura.md`, `docs/plano-de-teste.md` |
| 35 | 04 | Arquitetura ATAM com IA | `docs/arquitetura.md`: C4 (contexto, contêineres, componentes) em Mermaid, sequências críticas, utility tree, riscos, trade-offs, matriz requisito→decisão→componente→evidência, `docs/adr/` |
| 36 | 05 | Gestão de bugs + TDD red | `docs/testes/plano-tdd.md` (inventário de regras e casos), testes **vermelhos** para cada regra, `docs/defeitos/` |
| 37 | 06 | CI no GitHub Actions + TDD green | Regras implementadas, `.github/workflows/ci.yml`, run vermelho proposital + verde registrados no README, proteção da `main`, `docs/avaliacao-arquitetural/ciclo-01.md` |
| 38 | 07 | JUnit 5 + JaCoCo + SonarCloud | Gate 80/70 no `verify`, relatório JaCoCo como artifact, SonarCloud no PR, ciclo-01 atualizado |
| 39 | 08 | WireMock e API segura | Adapter HTTP de notificação + matriz 200/4xx/5xx/timeout/JSON inválido, Testcontainers, linha na RTM |
| 40 | — | Prova escrita | (já passou) |
| 41 | 09 | TDD e BDD | `docs/testes/bdd-aprovacao-bloqueio.md`, commits `test(red)`/`feat(green)`/`refactor`, `docs/testes/evidencia-tdd.md`, `features/aprovacao-bloqueio.feature` (Cucumber) |
| 42 | 10 | Carga e concorrência | Teste com 20 threads (1 aceita, 19× 409, contagem no banco), ADR da proteção, `docs/performance/rnf.md`, `perf/reservas.jmx`, `perf/usuarios.csv`, `docs/performance/resultado-01.md` |
| 43 | 11 | Auditoria e relatórios | `docs/status-requisitos.md`, auditoria por TDD (com teste de rollback), `GET /relatorios/utilizacao` com oráculo calculado à mão |
| 44 | 12 | Autorização e RTM | `docs/seguranca/matriz-autorizacao.md` → `@ParameterizedTest`, `docs/testes/mutacao-manual.md`, `docs/rtm.md` 100% crítico |
| 45 | 13 | Preparação prova oral | `docs/relatorio-qualidade.md`, `docs/prova-oral/perguntas.md`, `docs/prova-oral/ensaio.md` |
| **46** | **14** | **Entrega** | `docs/entrega/checklist.md`, tag `v1.0.0-rc1` com CI verde, `CHANGELOG.md`, `docs/evidencias/README.md`, `docs/decisao-qualidade.md`, **envio no formulário** |
| **47** | — | **Prova oral** | Roteiro de 8 min: problema e risco → arquitetura → demo → testes e CI → RTM e relatório → limitações |
| 48 | 15 | Feedback e correções | Defeitos do feedback, 5 porquês, testes de regressão, `AGENTS.md`/`CLAUDE.md`, `docs/licoes-aprendidas.md` |
| 49 | 16 | Revisão integrada | `docs/revisao-integrada.md`, `docs/revisao-cruzada.md`, `docs/exercicio-incidente.md` |
| 50 | 17 | Encerramento | `docs/retrospectiva-final.md` (métricas, dívida, 3 incrementos, retrospectiva) |

> Observação: a Aula 04 do Foot Fanatics inclui um "cron diário de treino de modelo". **Isso é do case, não do nosso projeto** — a especificação pede só a arquitetura do sistema de recursos. Não vale gastar tempo nisso.

---

## 4. Diagnóstico da situação

- **Fato:** o projeto não foi iniciado. A turma, pelo cronograma, já deveria ter até a Aula 12 pronta.
- **Fato:** sobram ~8 a 12 dias até a entrega, conforme o dia da turma (seg 09/11, qui 12/11 ou sex 13/11).
- **Risco principal:** o professor avalia **histórico** (commits vermelhos antes dos verdes, PR revisado por outro integrante, runs do CI, autoria). Não dá para "gerar tudo no fim": o histórico precisa ser real, só que comprimido.
- **Vantagem:** os materiais trazem **prompts prontos** para cada etapa (feitos para agente de código com terminal). O roteiro está todo dado; o trabalho é executar em ordem e revisar.

---

## 5. Plano de execução (comprimido, seguindo as 6 etapas da especificação)

Regras de trabalho valem para todas as etapas:
- Nunca commitar direto na `main`: branch → PR → **revisão de outro integrante** → merge.
- `./mvnw -B verify` antes de cada commit.
- Nenhum token/senha no repositório (SonarCloud via secret `SONAR_TOKEN`).
- Cada número em documento precisa ter fonte. O que não foi medido fica "não medido".

### Etapa 1 — Diagnóstico inicial e fundação (Dia 1 · dom 01/11 – seg 02/11)
**Equivale às Aulas 01–03.**
- [x] Criar repo público, Spring Initializr (Java 21, Web, Security, Data JPA, Validation, PostgreSQL, Flyway, Testcontainers, Actuator, springdoc-openapi). — feito em 04/11 (PR #1), Spring Boot 3.5.16; Security e springdoc entram com o back-end (Pedro).
- [ ] `docker-compose.yml` com PostgreSQL; `README.md` com "como subir" e "como testar". — Compose e README escritos; compose e `spring-boot:run` conferidos na máquina do Gustavo em 04/11; README ainda não validado em clone limpo por outra pessoa.
- [x] **CI mínimo já no primeiro dia** (`ci.yml` com `./mvnw -B verify`) — tira o limitador "pipeline ausente" do caminho. — `ci.yml` rodando `verify`; verde no PR #1.
- [x] Proteção da `main` exigindo o check. — ruleset `proteger-main` ativo, conferido pelo Gustavo em 05/11: 1 aprovação, "Dismiss stale approvals", só método Merge, branch atualizada exigida, checks obrigatórios `verify` e `SonarCloud Code Analysis` (este adicionado em 05/11), lista de exceções (bypass) vazia, sem force push nem exclusão. **Pendência:** o Pedro ainda não aceitou o convite do repositório (o Leandro já participa); sem isso, a aprovação dele não vale e a roda de revisão (Pedro revisa Leandro) não funciona.
- [x] `docs/prd.md`: visão, 3 personas (Solicitante, Responsável, Administrador), RF-01..RF-14, RNF numerados **com número** (ex.: p95 ≤ 300 ms com 50 usuários, erro < 1%), regras críticas RN-01..RN-08, fora de escopo. — rascunho em `docs/prd.md` (RF-01..14, RN-01..13, RNF-01..03); personas e números a validar com o grupo.
- [x] Esqueleto de `docs/arquitetura.md` e `docs/plano-de-teste.md`. — feito em 05/11: C4 (contexto, contêineres, componentes) e sequências "criar reserva" e "aprovar restrito" em Mermaid, utility tree, cenários em 6 partes, trade-offs e riscos; plano de teste com níveis, política de mock, metas e responsabilidades. Pacotes, rotas e níveis ainda são **proposta** a validar com Pedro e Leandro; os ADRs completos e o `plano-tdd.md` seguem na Etapa 2.
- [x] Criar conta no SonarCloud e importar o repo (passo manual, de um humano). — feito: organização `gustandrade07`, projeto `GustAndrade07_organizacao-recursos`, análise pelo CI (Automatic Analysis desligada), secret `SONAR_TOKEN` criado; Quality Gate passou no PR #4. Pendente: olhar os "Dependency risks" (meta: 0 vulnerabilidades críticas) e confirmar a análise da `main`.

### Etapa 2 — Requisitos, riscos e RTM (Dia 2 · ter 03/11)
**Equivale às Aulas 02, 04 e 05 (parte de inventário).**
- [x] `docs/arquitetura.md`: C4 contexto/contêineres/componentes em Mermaid, sequências de "criar reserva" e "aprovar restrito", utility tree, cenários em 6 partes, sensibilidade, trade-off, riscos. — esqueleto em 05/11 (Etapa 1); em 05/11 os trade-offs apontam para os ADRs 0005 e 0006. Pacotes e rotas seguem como proposta a validar com Pedro e Leandro (pendência no fim do documento).
- [x] ADRs em `docs/adr/`: autenticação (JWT), proteção de concorrência (constraint `EXCLUDE USING gist` em `tstzrange` vs lock pessimista), notificação via HTTP isolada, migrações com Flyway. — feito em 05/11: "Consequências" dos ADRs 0001 a 0004 (ganhamos, perdemos, exige de testes) e ADRs novos 0005 (notificação) e 0006 (Flyway), ambos com status **proposto** até a revisão. Os ADRs 0002, 0005 e 0006 registram D1, D2, D3 e D5 como decididas pelo Gustavo em 05/11 (ver seção 9); a D4 segue pendente do professor.
- [ ] `docs/rtm.md` inicial: requisito → risco → teste (a escrever) → evidência.
- [ ] `docs/testes/plano-tdd.md`: inventário de todas as regras com casos (feliz, limite, inválido, conflito, estado proibido).

### Etapa 3 — Testes e integração (Dias 3–5 · qua 04/11 – sex 06/11)
**Equivale às Aulas 05, 06, 08 e 09.**
- [ ] **[Aula 09 · decisão D1, seção 9]** D1 decidida em 05/11 pelo Gustavo: intervalo semiaberto `[início, fim)` (ADR-0002). **Falta** levar a decisão para `docs/testes/bdd-aprovacao-bloqueio.md` (tabela de fronteiras e "Perguntas em aberto", arquivo do Pedro) e obter a confirmação de Pedro e Leandro, **antes** do primeiro teste vermelho. A decisão define os limites do `tstzrange` no banco (limite inferior fechado e superior aberto).
- [x] **Migração Flyway V1 e testes de banco (parte do Gustavo):** `V1__nucleo_da_reserva.sql` (usuario, recurso, manutencao, reserva, reserva_recurso, tentativa_recusada, auditoria_reserva, `btree_gist` e a constraint de exclusão), com `test(red)` antes de `feat(green)`. — feito em 06/11 no PR `feat/banco-v1`: testes em `src/test/java/br/com/senac/recursos/banco/` (Testcontainers, PostgreSQL 16, sem H2); `verify` com 104 testes, 0 ignorados. Decisões D6 e D7 (seção 9) e ADR-0007 (proposto). Mutação feita na V1 (constraint neutralizada → 22 de 41 testes vermelhos); ainda **não** registrada em `docs/testes/mutacao-manual.md`. A tradução em 409 e o teste de API dependem do serviço do Pedro. Esquema consolidado para consulta em `docs/banco/schema.sql` (gerado com `pg_dump` em 07/11 a partir da V1 e recarregado num PostgreSQL 16.15 limpo; documentação, não migração).
- [ ] **TDD red:** testes vermelhos para cada regra do plano (commits `test(red): ...`). — os de banco estão feitos (linha acima); faltam os de domínio, API e BDD.
- [ ] **TDD green:** domínio e regras até a suíte ficar verde (`feat(green): ...`, `refactor: ...`). `Clock` injetado, nunca `Instant.now()` solto nem `Thread.sleep`.
- [ ] Entidades: Recurso (sala/material, restrito, capacidade, localização), Professor (competências), Reserva + estados, Manutenção/Bloqueio, Retirada/Devolução, Auditoria, Usuário/Perfil.
- [ ] Spring Security com login que devolve token; erros com `ProblemDetail`.
- [ ] Integração com **Testcontainers** (PostgreSQL real).
- [ ] Notificação via cliente HTTP + **WireMock** cobrindo 200, 404, 500, timeout, JSON inválido e "token do provedor não vaza".
- [ ] **BDD:** `docs/testes/bdd-aprovacao-bloqueio.md` + `aprovacao-bloqueio.feature` (Cucumber) para aprovação de restrito, auto-aprovação proibida, manutenção (fronteiras exatas) e transições de estado; `docs/testes/evidencia-tdd.md`.
- [ ] Ciclo ATAM 01: `docs/avaliacao-arquitetural/ciclo-01.md`.
- [ ] Interface responsiva mínima (Thymeleaf, como o repo da disciplina, ou página estática simples).

### Etapa 4 — CI e SonarCloud (em paralelo desde o Dia 1, fechado no Dia 5)
**Equivale às Aulas 06–07.**
- [ ] `ci.yml` completo: `pull_request` + push na `main`, `permissions: contents: read`, Temurin 21, cache Maven, `concurrency`, `timeout-minutes`, artifacts do Surefire/Failsafe/JaCoCo com `if: always()`.
- [ ] Um run vermelho proposital e sua recuperação, IDs anotados no README.
- [ ] JaCoCo com gate **80% linhas / 70% branches** falhando o `verify`.
- [ ] SonarCloud no PR (`fetch-depth: 0`, lendo o XML do JaCoCo), Quality Gate verde, cada issue tratada (corrigir/justificar), nunca baixando meta. — estado em 05/11: Quality Gate Passed na `main`, cobertura 100% lida do JaCoCo (código ainda pequeno), 0 Security Hotspots, 1 issue médio (regex em `CssSemValoresSoltosTest`, a corrigir pelo Leandro), 48 "Dependency risks" (8 Critical e 10 High por CVSS). PR `fix/dependencias-seguras` sobe Tomcat, Jackson, PostgreSQL, Logback, log4j e jsoup e inclui os templates na análise; os 2 críticos do Spring Framework 6.2.19 não têm correção na linha 3.5 e estão justificados em `docs/seguranca/riscos-dependencias.md`. Falta: reanalisar a `main` depois do merge, marcar os 2 como Accepted no SonarCloud e deixar o projeto **Public** (hoje aparece como Private).

### Etapa 5 — Concorrência, carga, auditoria, autorização (Dias 6–7 · sáb 07/11 – dom 08/11)
**Equivale às Aulas 10–12.**
- [ ] **[Aula 11]** Começar a Etapa 5 pelo diagnóstico: atualizar `docs/status-requisitos.md` (feito/parcial/ausente; só vale "feito" com teste citado), ordenar as pendências pelos 4 limitadores de nota e dividir entre os três até a Semana 46. Esse arquivo é atualizado em **todo** PR depois disso, não só no fim.
- [x] **[Aula 11 · banco]** (feito em 06/11: tabela `tentativa_recusada` na V1, com recurso, solicitante, período pedido, motivo `SOBREPOSICAO` e `registrada_em`; gravação em transação própria provada em `TentativaRecusadaBancoTest`. A consulta do relatório ainda não existe.) O relatório de utilização pede "conflitos evitados" = tentativas recusadas por sobreposição no período. Isso só existe se a tentativa recusada for **gravada** (data, recurso, intervalo pedido, motivo). Entra no modelo de dados da migração V1, senão o RF-12 não tem como ser calculado nem testado.
- [ ] **[Aula 10 · pré-requisito]** Apache JMeter 5.6 instalado (`jmeter -v`) na máquina de quem rodar a carga (Leandro); aplicação local com PostgreSQL em container. Rodada sempre em modo CLI contra a aplicação local.
- [ ] Teste de concorrência: 20 requisições simultâneas (`ExecutorService` + `CountDownLatch`) → 1 aceita, 19× **409**, contagem **no banco**; repetir para a agenda do professor; rodar 5×. Mostrar vermelho sem a proteção. — **nível do banco feito em 06/11** (`ConcorrenciaReservaBancoTest`: 1 aceita e 19 recusadas com `23P01`, contagem no banco, recurso e professor, 5 rodadas; vermelho sem a proteção mostrado trocando o predicado por `WHERE (false)`). Falta o 409 de ponta a ponta (serviço do Pedro).
- [ ] `docs/performance/rnf.md` (escrito **antes** de rodar) → `perf/reservas.jmx` + `perf/usuarios.csv` → rodada CLI → `docs/performance/resultado-01.md` (p50/p95/p99, vazão, erro separando 409 de erro real, limites da conclusão).
- [ ] Auditoria por TDD, com teste de rollback (sem auditoria órfã) e 403 do Solicitante no histórico. **[Aula 11]** O registro guarda reserva, usuário, instante (via `Clock`), estado anterior, estado novo e motivo; a API não altera nem apaga registro; reserva já iniciada não pode ser apagada; a consulta do histórico é só de Responsável e Administrador (RN-09 a RN-12 do PRD). Testes de unidade primeiro, depois integração com Testcontainers; commits de teste e de código separados.
- [ ] `GET /relatorios/utilizacao` com testes caixa-preta e valores calculados à mão no comentário do teste (período vazio, reserva cruzando borda, cancelada não conta).
- [ ] `docs/status-requisitos.md` (feito/parcial/ausente com teste citado).
- [ ] `docs/seguranca/matriz-autorizacao.md` (operação × anônimo/Solicitante/Responsável/Admin) → `@ParameterizedTest` lendo a matriz; casos de posse; validação 400 com mensagem clara e sem stack trace. **[Aula 12]** Toda rota do código precisa estar na matriz; marcar onde o código diverge da matriz (sem corrigir na hora) e listar rotas fora de qualquer regra de segurança. Cada célula vermelha vira `docs/defeitos/DEF-xxx` **antes** da correção, e a correção vem em commit próprio citando o defeito. Nenhuma célula é apagada para o teste passar.
- [ ] `docs/testes/mutacao-manual.md`: quebrar cada regra crítica, ver o teste falhar, `git restore`. **[Aula 12]** As seis regras: sobreposição, dupla reserva, aprovação de restrito, manutenção, autorização e auditoria. Teste que continua verde com a regra quebrada é lacuna: escrever o teste que falta. Nenhuma mutação pode sobrar no código.
- [ ] `docs/rtm.md` com **100%** dos críticos ligados a teste e run do CI.
- [ ] Cada falha encontrada vira arquivo em `docs/defeitos/` antes da correção.

### Etapa 6 — Entrega (Dias 9–12 · seg 09/11 a sex 13/11)
> Meta interna: tag `v1.0.0-rc1` e formulário enviados até **qua 11/11**. Quinta fica de folga para correções (`rc2`) e sexta é a aula de entrega e conferência na mesa.
> Na aula de sex 06/11 (Aula 13), usar o tempo de sala para avançar o projeto e tirar dúvidas com o professor.
**Equivale às Aulas 13–14.**
- [ ] `docs/relatorio-qualidade.md` com números reais (cada número com fonte). **[Aula 13]** Estrutura pedida: identificação (repositório, commit, data, integrantes, ambiente), escopo e fora de escopo, estratégia de teste por nível, resultados comparados às metas (80/70, 100% críticos na RTM, 0 bugs/vulnerabilidades críticas), defeitos por severidade e situação, riscos residuais e recomendação. Cada afirmação com **evidência, interpretação e limite**; o que não foi medido fica "não medido", com o motivo.
- [ ] `docs/prova-oral/perguntas.md` (15 perguntas do projeto) e ensaio cronometrado → `docs/prova-oral/ensaio.md`. **[Aula 13]** Perguntas citam arquivos do projeto e distribuídas para ninguém responder só sobre a própria parte; respostas **não** são escritas; o ensaio é individual (todos presentes) e as lacunas viram tarefas com responsável.
- [ ] **[Entregável da especificação: "Apresentação e demonstração do sistema"]** Roteiro da demo dentro dos 8 minutos, massa de dados de demonstração e ensaio no notebook que será usado (banco subindo com `docker compose up -d`, aplicação em `localhost:8080`). Sem aplicação executando, é limitador de nota.
- [ ] Checklist em **clone limpo** seguindo só o README → `docs/entrega/checklist.md`. **[Aula 14]** Quem faz o clone não pode ter escrito o README nem reaproveitar nada da própria máquina; anotar o que faltou, listar bloqueadores em ordem e só então corrigir.
- [ ] Tag anotada `v1.0.0-rc1` com CI verde (correção depois = `rc2`, nunca mover tag).
- [ ] `CHANGELOG.md`, `docs/evidencias/README.md` (tudo apontando para o commit da tag), `docs/decisao-qualidade.md` (liberar/ressalvas/bloquear + quem fez o quê).
- [ ] **Um integrante envia o formulário** (nomes completos + link do repo). Reenvio com o mesmo link substitui. **[Aula 14]** Texto curto para colar no formulário: link do repositório público + tag avaliada (`v1.0.0-rc1`).

### Pós-entrega (Semanas 47–50)
- **S47 — Prova oral:** cada integrante precisa responder sobre partes que **não** escreveu.
- **S48 — Feedback:** defeitos do feedback → 5 porquês → `test(regressao)` + `fix(...)` → `AGENTS.md`/`CLAUDE.md` + `docs/licoes-aprendidas.md`. **[Aula 15]** Classificar cada observação (defeito de produto, lacuna de teste, lacuna de documentação ou falha de argumentação); os três primeiros viram arquivo em `docs/defeitos/`; commits `test(regressao): <id>` e `fix(<id>): <descrição>`; causa raiz = algo sobre o qual o grupo age (processo, teste ausente, requisito ambíguo, decisão de design); decisão de arquitetura que mudar vira ADR novo que substitui o anterior, sem apagar o antigo. O `AGENTS.md` já existe desde 04/11 (regras de contexto); na S48 ele só ganha as regras que os defeitos do feedback ensinarem, cada uma apontando para o teste ou verificação que a garante.
- **S49 — Revisão integrada:** mapa regra→técnica→evidência, revisão do repo de outra equipe, incidente simulado.
- **S50 — Encerramento:** `docs/retrospectiva-final.md`.

---

## 6. Divisão por área (definida pelo grupo)

| Área | Dono | Testes da área |
|---|---|---|
| Banco de dados | Gustavo | Integração de repositório (Testcontainers), constraints, concorrência (20 threads), transação/rollback da auditoria, consultas do relatório, migrações Flyway |
| Back-end | Pedro | Unidade + parametrizado, TDD, BDD (Cucumber), API caixa-preta, matriz de autorização, WireMock, mutação manual |
| Front-end | Leandro | E2E (Playwright), testes de view (MockMvc), responsividade, mensagens de erro, carga (JMeter) |

Transversais: CI/JaCoCo/SonarCloud, arquitetura/ADR e README = Gustavo · PRD = Pedro · RTM, relatório e decisão de qualidade = Leandro.
Revisão em roda: Gustavo → Pedro → Leandro → Gustavo.

## 7. Riscos do plano
| Risco | Mitigação |
|---|---|
| Prazo curto vs. escopo | Limitadores primeiro; escopo de UI mínimo |
| Histórico "fabricado" | Commits e PRs reais em cada etapa, red antes do green de verdade |
| SonarCloud/branch protection dependem de conta | Fazer no Dia 1 |
| Docker indisponível em alguma máquina | Testcontainers roda no CI (runner Ubuntu já tem Docker) |
| Integrante sem domínio na oral | Banco de perguntas cruzado + ensaio |

## 8. Decisões técnicas (aprovadas em 01/11)
1. **Login:** token JWT (o sistema entrega uma "credencial temporária" no login e ela vai em cada requisição). Os prompts do professor (JMeter extraindo token, testes com "tokens de cada perfil") já supõem esse modelo.
2. **Reserva dupla:** regra dentro do próprio PostgreSQL (`EXCLUDE USING gist` em `tstzrange`): o banco recusa dois horários sobrepostos para o mesmo recurso, mesmo com pedidos simultâneos. É a primeira opção que a Aula 10 cita.
3. **Telas:** Thymeleaf, com páginas HTML geradas pelo próprio Spring, no mesmo projeto. O repositório da disciplina usa esse modelo. Basta cumprir "interface responsiva e mensagens de erro claras".
4. **Teste E2E:** Playwright para Java (roda sem tela no CI).

## 9. Decisões pendentes do grupo
Perguntas que o PRD não resolve e que **ninguém decidiu ainda**. Enquanto estiverem abertas, ninguém inventa a política: o código, os testes e os ADRs tratam o ponto como "decisão pendente". Registrado em 05/11/2026. No mesmo dia o Gustavo decidiu D1, D2, D3 e D5 e definiu como tratar a D4 (coluna "Decisão"); as decisões de D1 e D3 ainda pedem a confirmação de Pedro e Leandro na revisão do PR.

| # | Pergunta | Quem decide | Prazo (o que trava) | Impacto | Proposta | Decisão |
|---|---|---|---|---|---|---|
| D1 | O intervalo de reserva e de manutenção é **semiaberto `[início, fim)`**? Ou seja: uma reserva que termina exatamente quando a manutenção começa (ou começa exatamente quando ela termina) é aceita? | Grupo (Gustavo propõe, Pedro implementa a regra, Leandro confere as mensagens de tela); confirmar com o professor se houver dúvida | **Antes do primeiro teste vermelho** das regras de manutenção e de sobreposição (Etapa 3) e **antes da migração Flyway V1** | Define os limites do `tstzrange` na constraint `EXCLUDE USING gist` (ADR-0002), a tabela de fronteiras do BDD (`docs/testes/bdd-aprovacao-bloqueio.md`), os testes de manutenção e o relatório (reserva que cruza a borda do período) | `[início, fim)`: é o padrão do `tstzrange` no PostgreSQL e os exemplos do BDD já assumem "termina exatamente no início → aceita". Se aceito, vira registro no ADR-0002 | **Decidida em 05/11/2026 (Gustavo): sim, semiaberto `[início, fim)`.** Registrada no ADR-0002. Falta confirmar com Pedro e Leandro e levar ao BDD (arquivo do Pedro) |
| D2 | A tentativa recusada por sobreposição é gravada **fora** da transação que sofreu rollback? | Gustavo (banco) com o Pedro (serviço) | Antes da migração V1 (a tabela das tentativas entra no modelo) | Sem isso o "conflitos evitados" do relatório (RF-12) perde dado | Gravar em transação própria; teste que prova a contagem. Ver `docs/arquitetura.md`, seção 5.1 | **Decidida em 05/11/2026 (Gustavo, dono do banco): sim, em transação própria.** Registrada nos ADRs 0002, 0005 e 0006; a tabela das tentativas entra na V1 |
| D3 | Qual código HTTP para transição de estado inválida: 409 ou 422? | Pedro (API) | Antes dos testes da máquina de estados | Contrato da API e matriz de autorização | Ver `docs/arquitetura.md`, seção 5.2 | **Decidida em 05/11/2026 (Gustavo): 409**, com `ProblemDetail` de tipo próprio (diferente do conflito de horário). Como a API é do Pedro, **a confirmar com ele**; registrada no ADR-0005 e na `arquitetura.md` |
| D4 | "0 vulnerabilidades críticas" se mede pelo CVSS das dependências ou pelo Quality Gate e issues do SonarCloud? | Professor (Gustavo pergunta) | Antes da entrega (13/11) | Define se os 2 críticos do Spring 6.2.19 precisam de algo além da justificativa | Ver `docs/seguranca/riscos-dependencias.md` | **Tratamento definido em 05/11/2026 (Gustavo):** reportar as **duas medições separadas** no relatório (CVSS das dependências e Quality Gate) e perguntar ao professor qual vale. **Resposta do professor ainda pendente** |
| D5 | Quais estados da reserva ocupam o horário (e, portanto, entram na constraint de exclusão)? | Grupo (Gustavo propõe); o PRD (RN-05) lista os estados mas não responde | Antes da migração V1 | Define a cláusula parcial da constraint `EXCLUDE USING gist` (ADR-0002) e o teste de cancelamento | Cancelada, rejeitada e não compareceu liberam o horário | **Decidida em 05/11/2026 (Gustavo): ocupam `SOLICITADA`, `APROVADA`, `EM_USO` e `CONCLUIDA`; liberam `CANCELADA`, `REJEITADA` e `NAO_COMPARECEU`.** Registrada nos ADRs 0002 e 0006; a confirmar com Pedro e Leandro |
| D6 | Como modelar, no banco, a proteção contra reserva dupla para recurso **e** professor, o estado que a torna parcial (D5), a tabela das tentativas recusadas (D2) e os índices da V1? | Gustavo (banco); o Pedro escreve o serviço que grava as linhas | Antes da migração V1 | Forma da V1 e do serviço de reserva (duas linhas por reserva de sala com docente, em ordem de `recurso_id`) | As quatro escolhas recomendadas ao Gustavo (ADR-0007) | **Decidida em 06/11/2026 (Gustavo):** professor é recurso e há uma só constraint `EXCLUDE`; estado copiado para `reserva_recurso` por FK composta `ON UPDATE CASCADE`; `tentativa_recusada(recurso_id, solicitante_id, periodo_pedido, motivo, registrada_em)`; só os índices necessários (nenhum de busca antes de medir); V1 só com o núcleo da reserva (`movimentacao_material` e colunas sem decisão ficam para a V2). Registrada no ADR-0007 (proposto); a confirmar com o Pedro |
| D7 | Como evitar o deadlock (`40P01`) que apareceu nos 20 pedidos simultâneos sobre a constraint de exclusão? | Gustavo (banco) | Antes de fechar a V1 | Código de erro previsível (`23P01` → 409) e tempo de resposta sob disputa (RNF-03) | Trigger com `pg_advisory_xact_lock` por recurso | **Decidida em 06/11/2026 (Gustavo):** trigger de trava por recurso na V1. Medido: sem ela 9 de 40 rodadas falharam; com ela 30 de 30 passaram (só nesta máquina). Efeito sobre RNF-01 e RNF-03 **não medido**. Registrada no ADR-0007; a confirmar com o Pedro |

Quando uma decisão for tomada: anotar a data e quem decidiu na linha, ajustar o ADR ou documento afetado e acrescentar o que mudou na seção 9 do `AGENTS.md`, se virar regra.

## 10. Como este plano se mantém atualizado
Regra do grupo (detalhes em `AGENTS.md`): **todo pull request atualiza este plano**. Na descrição do PR entra a seção "Plano de execução" (o que foi marcado ou mudou aqui) e, neste arquivo, as caixas da etapa, a linha de status do topo (com a data) e, se for o caso, `docs/status-requisitos.md`, `docs/rtm.md` e `CHANGELOG.md`. Quando um defeito, um feedback ou a revisão de um colega ensinar algo novo ao grupo, a regra correspondente entra no `AGENTS.md` com a indicação de onde ela é verificada.
