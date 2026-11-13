# Qualidade de Software 2026.2 — Auditoria da matéria e plano de execução

> **Status mais recente:** desde 11/11/2026, um arquivo por PR em [`docs/status/`](status/) (em ordem de data). Os parágrafos "Status em" abaixo são o histórico até 09/11.

> Fonte: Hub de Aulas do Prof. Afonso (home, especificação do projeto, escopo da prova, página de entrega e materiais/slides das Aulas 01 a 17), lidos em 01/11/2026. Materiais das Aulas 09 a 15 e a especificação reconferidos em 04/11/2026 (https://afonsolelis.github.io/aulas_senac/pages/home_qualidade_2026_2.html).
> Plano escrito no **domingo, 01/11 (fim da Semana 44)**. Turma **STADSCAS4ND (sexta, 19h10–22h45, salas C237 / D156)**.
> **Entrega: sexta 13/11** (Aula 14), com meta interna de terminar antes. Prova oral: **sexta 20/11** (Semana 47).
> Grupo (3): Gustavo Oliveira Fernandes de Andrade · Leandro Sato · Pedro Henrique (conferir o nome completo com a lista da turma).
>
> **Status em 09/11/2026 (segunda, noite, branch `docs/adr-0006-arquitetura-pos-merge`):** só documentação, a partir da `main` (`a6d6374`). ADR-0006 e ADR-0002 deixam de dizer que a V1 e a checagem de CI de migração não existem (V1 na `main` pelo PR #28; checagem pelo PR #34, que só roda em `pull_request`: skipped nos pushes da `main` #143 e #145, passou no run #146 do PR #36, sem alterar migração). `docs/arquitetura.md`, seção 10: RF-10 passa a parcial com `AuditoriaBancoTest` (só nível do banco) e o RF-12 cita as 3 consultas e `RelatorioUtilizacaoBancoTest` (PR #33). O Quality Gate do SonarCloud da `main` voltou a Passed com o merge do #32 (DEF-001): evento "Green (was Red)" na análise de `e172977`, lido na API pública em 09/11/2026, o que fecha o "a confirmar" do status do DEF-001 abaixo. Nenhum número novo de teste, nenhuma caixa marcada.
>
> **Status em 09/11/2026 (segunda, Etapa 3, ATAM):** `docs/avaliacao-arquitetural/ciclo-01.md` (branch `docs/ciclo-01-atam`, que escrevi em 07/11) atualizado com o que já está na `main` (`a6d6374`): #28, #33, #34, #35 e #32 mergeados; fontes 3 e 5 agora na `main`; checagem de CI de migração como fonte nova; risco novo RK9 (Quality Gate do SonarCloud vermelho na `main` desde 06/11, DEF-001, gate verde **a confirmar**) e a D8(d) em RK8. Continua sendo um rascunho do Gustavo, **sem sessão com Pedro e Leandro**; as ações A1 a A7 (seção 8 do ciclo) têm dono proposto. Números só de uma máquina e do nível do banco. A caixa "Ciclo ATAM 01" fica **aberta** até a revisão do Leandro e do Pedro.
>
> **Status em 09/11/2026 (segunda, branch `docs/arquitetura-secao-10`):** `docs/arquitetura.md`, seção 10, ganha a evidência do RF-13 (`parcial`, testes de contrato das telas que já estão na `main`, sem E2E em 375 px); RF-04, RF-05, RF-06 e RF-12 mantêm a evidência parcial de banco que veio da `main`, e as demais linhas continuam "a preencher". Nenhum número novo, nenhuma caixa marcada.
>
> **Status em 09/11/2026 (segunda, branch `ci/migracao-imutavel`, T6 do ADR-0006):** checagem de CI que falha se uma migração `V<n>__*.sql` já existente em `db/migration` for modificada, apagada ou renomeada em relação à base do PR (`.github/scripts/verificar-migracoes-imutaveis.sh` e dois passos no `ci.yml`), com auto-teste de 9 casos (`.github/scripts/testar-verificacao-migracoes.sh`; duas mutações do script deixaram 3 e 1 casos vermelhos, só nesta máquina). **Não verificado:** o run do GitHub Actions e a checagem contra a alteração de uma migração real (a V1 entrou na `main` pelo PR #28 depois que o script foi escrito). Nenhuma caixa marcada.
>
> **Status em 09/11/2026 (segunda):** Quality Gate do SonarCloud **vermelho na `main`** desde o merge do PR #12 (06/11): "C Reliability Rating on New Code". Causa deduzida em `docs/defeitos/DEF-001.md`: páginas de erro 403, 404, 4xx, 500, 5xx e `fragments/erro` sem `<title>` no arquivo (`Web:PageWithoutTitleCheck`); correção e teste de regressão neste PR. **A confirmar:** gate verde na `main` depois do merge (o painel do SonarCloud é privado e a lista de issues não foi lida).
>
> **Status em 07/11/2026 (sábado, noite, Etapa 6, preparação do clone limpo):** branch `docs/roteiro-clone-limpo`, a partir da `main` (`009a4be`). Entram `docs/entrega/roteiro-clone-limpo.md` (roteiro curto para quem **não** escreveu o README, Leandro ou Pedro) e a seção "Conferido sem clone" do `docs/entrega/checklist.md`, só com a existência de arquivos na `main` e a fonte. **O clone limpo ainda não foi feito** e as colunas "Existe" e "Atualizado" da tabela seguem como estavam em 04/11. Gustavo pede o clone ao Leandro ou ao Pedro, depois dos merges e antes da tag.
>
> **Status em 07/11/2026 (sábado, noite, Etapa 5 no nível do banco):** branch `feat/banco-auditoria-relatorio`, empilhada sobre `feat/banco-v1` (em 09/11/2026 a branch recebeu o merge de `origin/feat/banco-v1` e vai a PR contra a `main`; o #28 já foi mergeado na `main`, então o diff do PR traz só a Etapa 5). Entram: `AuditoriaBancoTest` (rollback da auditoria nas duas direções, 3 testes), as consultas do relatório RF-12 em `src/main/resources/db/consultas/` com `RelatorioUtilizacaoBancoTest` (6 testes, em TDD: `test(red)` com 510 testes, 0 falhas e 6 erros, depois `feat(green)`), `docs/testes/mutacao-manual.md` (só a seção do banco, mutações M1 a M5 medidas só nesta máquina), regra 11 do `AGENTS.md` (exceção do `test(red)` ao `verify` verde) e a pergunta em aberto D8 (critérios do relatório; **ninguém decidiu**). **Falta, com dono:** Pedro (serviço de auditoria, endpoint `GET /relatorios/utilizacao`, 401/403, mutações no serviço, OK para carregar o SQL de `db/consultas`, confirmar D8); Leandro (revisão deste PR). A caixa da auditoria, a do relatório e a da mutação continuam abertas na Etapa 5.
>
> **Status em 07/11/2026 (sábado, Etapa 4):** CI vermelho proposital feito no PR #30 (rascunho): o run #120 (ID 42427577443) ficou vermelho com as 2 falhas provocadas (`Tests run: 428, Failures: 2`) e o commit de correção as removeu; o run #121 (ID 42428144208) ficou verde, os dois IDs estão no README e a caixa do run vermelho está marcada. Caixas do `ci.yml` e do gate 80/70 marcadas, citando os arquivos e a medição local. Os PRs #27 e #28 já foram mergeados na `main`.
>
> **Status em 07/11/2026 (sábado):** PR #27 (regra das specs) já mergeado na `main` e PR #28 (V1 e testes de banco) aberto, com o Leandro como revisor e **sem aprovação ainda**; o Quality Gate do SonarCloud passou no #28 (0 issues). Neste PR entram também `docs/banco/schema.sql` (esquema consolidado gerado com `pg_dump` da V1, só estrutura, sem dados nem credenciais) e a correção do README (teste sem Docker falha, não é ignorado; `verify` com 501 testes e 0 ignorados depois do merge da `main` de 07/11; 74 deles são de banco). **Specs do Notion atualizadas em 07/11:** Specs do Banco (seções 1, 3, 4, 5 e 6 e nova seção 7 com o diagrama ER e a V1) e Specs do Back-end (contrato com o banco, D6, D7 e pontos em aberto); as caixas da seção 6 do Banco só serão marcadas depois do merge do #28. **Falta, com dono:** revisão do Leandro no PR #28; Pedro confirmar D6, D7 e os ADRs 0005 a 0007, escrever o serviço que traduz `23P01` em 409 e o teste de API; Gustavo perguntar a D4 ao professor, pedir o clone limpo do README, fazer o CI vermelho proposital e o `ciclo-01.md`. O parágrafo de 06/11 abaixo continua valendo no que não foi superado por este.
>
> **Status em 06/11/2026 (sexta):** PRs #10 a #13 do Gustavo mergeados (plano, `AGENTS.md`, `arquitetura.md`, `plano-de-teste.md`, riscos de dependências, ADRs 0001 a 0006 e decisões D1, D2, D3 e D5 na `main`). Nova regra do grupo (PR #27, mergeado): toda mudança que toque requisito, regra, rota, modelo de dados, decisão ou estado de entrega atualiza a spec do Notion da própria área no mesmo PR (ou em até 24 h), com a linha "Specs atualizadas" na descrição do PR (`AGENTS.md`, seção 4 item 5 e seção 9 regra 7). Etapa 3, parte do banco, no PR `feat/banco-v1`: migração Flyway **V1** (`V1__nucleo_da_reserva.sql`) e testes de banco em TDD (`test(red)` → `feat(green)`), `AplicacaoSmokeTest` sem `disabledWithoutDocker` (`verify` com 104 testes, 0 ignorados), ADR-0007 (proposto) e decisões D6 e D7 do Gustavo, a confirmar com o Pedro. Achado: sem trava por recurso, 9 de 40 rodadas de 20 pedidos simultâneos deram deadlock (`40P01`). **Falta** (Pedro): serviço de reserva que traduz `23P01` em 409, teste de API, entidades JPA. O parágrafo de 05/11 abaixo continua valendo no que não foi superado por este.
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

### 4.1 Conferência contra as aulas em 12/11/2026 (Gustavo)

Leitura dos materiais das Aulas 01 a 17 e da especificação no site do professor, comparada com a `main` em `2610d6a` (PRs #48, #49, #50 e #51 mergeados; CI da `main` verde no run 211).

**CI e SonarCloud (Aulas 06 e 07): atendidos.**
- `ci.yml`, JaCoCo 80/70 derrubando o `verify`, artifacts com `if: always()` e o run vermelho com a recuperação no README atendem os Prompts 2 da Aula 06 e 1 e 2 da Aula 07.
- Ruleset `proteger-main` lido em 12/11/2026 em Settings → Rules:
  - PR obrigatório, com 1 aprovação e aprovação antiga descartada a cada push novo.
  - Checks `verify` e `SonarCloud Code Analysis` obrigatórios.
  - Branch atualizada antes do merge.
  - Bloqueio de force push e de exclusão.
  - Lista de bypass vazia.
- SonarCloud, análise do `2610d6a` lida na API pública em 12/11/2026:
  - Quality Gate "Sonar way" **OK**.
  - 0 bugs, 0 vulnerabilidades, 0 hotspots e 1 code smell.
  - Cobertura de 95,7% das linhas e 80,3% dos branches, duplicação de 0%.
  - Os 80,3% de branches ficam logo acima da condição de 80% do gate no código novo.
- Diferenças só de nome ou versão em relação ao exemplo da Aula 06:
  - O check se chama `verify`, não "qualidade / test".
  - O artifact não tem `retention-days: 7`.
  - Usamos `checkout@v4` e `setup-java@v4` (o exemplo usa v6 e v5).
  - Nenhuma delas é exigência.

**Lacunas para a entrega (dono pela seção 6):**

| # | Lacuna | Aula | Dono | Bloqueia a tag? |
|---|---|---|---|---|
| 1 | **Resolvida em 12/11/2026** (Leandro, branch `perf/jmeter-carga`, ver `docs/performance/resultado-01.md`). Carga com JMeter. Não existem `docs/performance/rnf.md`, `perf/reservas.jmx`, `perf/usuarios.csv` nem `docs/performance/resultado-01.md`. RNF-01 e RNF-03 seguem "ausente" na RTM | 10 (P2 e P3) e 14 | Leandro | Sim (índice de evidências) |
| 2 | **Escrita em 12/11/2026, noite** (Leandro, branch `docs/decisao-qualidade`): liberar com ressalvas, condicionada ao clone limpo; faltam as declarações dos integrantes e a conferência no commit da tag. Antes: `docs/decisao-qualidade.md` não existe: decisão, critérios com evidência, riscos aceitos (2 críticos do Spring, `jaxb-core`, D13) e quem fez o quê | 14 (P3) | Leandro | Sim |
| 3 | **Resolvida em 12/11/2026, noite, sobre `6ce4478`** (branch `docs/rtm-relatorio-1211`; refazer no commit da tag). RTM e relatório de qualidade desatualizados. O resumo da RTM diz "0 de 2 críticos" e o relatório está no commit `e66c000`, ambos sem os testes do back-end e das telas | 12 (P3) e 13 (P1) | Leandro | Sim (números no commit da tag) |
| 4 | ~~BDD executável~~ **feito em 13/11/2026**: Cucumber 7.23.0 no `pom.xml`, `src/test/resources/features/aprovacao-bloqueio.feature` com 13 cenários verdes no `verify` e `docs/testes/evidencia-tdd.md` escrito (ciclos red → green com hash, e o que **não** nasceu de TDD) | 09 (P2 e P3) | Pedro | Não, mas pesa em "qualidade dos testes" |
| 5 | README × `pom.xml`: **metade resolvida em 13/11/2026** — o Cucumber passou a existir de verdade (lacuna 4) e a linha do README foi reescrita. **O WireMock continua ausente** e o README agora diz isso com todas as letras, junto com o RF-11 | 08 e 09 | Gustavo (README) | Não, mas a mesa confere o README |
| 6 | ~~Mutação manual: aprovação, manutenção e autorização pendentes~~ **feitas em 13/11/2026**: 8 mutações do serviço (MA1, MA2, MM1, MM2, MM3, MZ1, MZ2, MAU1), todas pegas por teste; a MAU1 achou uma lacuna real na RN-08 (0 vermelhos sem os testes novos) | 12 (P3) | Pedro | Não |
| 7 | ~~`docs/testes/plano-tdd.md` não existe~~ **escrito em 13/11/2026**: matriz regra → caso → teste existente → situação, em 12 seções, citando classe e método de cada teste e nomeando as 3 lacunas e os 2 pontos travados por decisão (D8, D9) | 05 (P1) e 06 (P1) | Pedro | Não |
| 8 | ~~Endpoint do relatório sem documentação e sem OpenAPI~~ **feito em 13/11/2026**: `springdoc-openapi` 2.8.17, `/v3/api-docs` e `/swagger-ui`, `GET /api/relatorios/utilizacao` com resumo, parâmetros e respostas 200/400/401/403; `OpenApiDocumentacaoTest` (5 testes) confere que a documentação descreve exatamente as rotas `/api/**` da matriz | 11 (P3) | Pedro | Não |
| 9 | **Resolvida em 12/11/2026, noite** (branch `docs/rtm-relatorio-1211`). Seção "Pendências e divisão" de `docs/status-requisitos.md` vazia | 11 (P1) | Leandro | Não |
| 10 | `docs/avaliacao-arquitetural/ciclo-01.md` sem os números do JaCoCo e do Sonar, e com o RK9 "a confirmar" | 07 (P2) | Gustavo | Não |
| 11 | Clone limpo e `docs/entrega/checklist.md` ~~não feitos~~ — **rodada feita em 13/11/2026 pela IA** no commit `9acbbcd` (verde, sem bloqueador; README corrigido no mesmo PR). Falta a conferência por **uma pessoa** que não escreveu o README, que o grupo decide se ainda exige | 14 (P1) | Leandro ou Pedro | Sim |
| 12 | Prova oral: ~~4 de 15 perguntas~~ **as 15 escritas em 13/11/2026**, com arquivo, resposta fraca e distribuição conferida (ninguém responde só sobre a própria área); **o ensaio continua não feito** (é humano). Roteiro da demo de 8 min em `docs/prova-oral/roteiro-demo.md`, também sem ensaio | 13 (P2 e P3) | Todos | Não (Semana 47) |

**O que cada lacuna é e para que serve (para estudo):**

1. **Carga com JMeter:** mede como a aplicação responde com muitos usuários ao mesmo tempo. O teste de concorrência prova que **não há reserva dupla**; o de carga mede **tempo e erro** sob volume. A ordem é:
   - `rnf.md`: escrito **antes** de rodar, com a meta (ex.: p95 ≤ 300 ms com 50 usuários), para o número não ser escolhido depois de ver o resultado.
   - `reservas.jmx`: o plano do JMeter (quantos usuários, por quanto tempo, quais requisições, que respostas são aceitas).
   - `usuarios.csv`: os logins fictícios que os usuários virtuais usam.
   - Rodada em modo CLI, gerando o `.jtl` com cada requisição.
   - `resultado-01.md`: a leitura do resultado.
     - **p50** é o tempo da metade das requisições; **p95**, o tempo abaixo do qual ficam 95% delas (mostra os casos lentos que a média esconde); **p99** é o extremo.
     - Vazão é quantas requisições por segundo a aplicação atendeu.
     - O **409** de conflito é resposta correta, não erro; erro de verdade é 500 ou timeout.
   - Cobre o RNF-01 (tempo da busca) e o RNF-03 (criação sob carga só responde 201 ou 409).
2. **`docs/decisao-qualidade.md`:** a conclusão da entrega, como um parecer, com cinco partes:
   - A decisão: **liberar**, **liberar com ressalvas** ou **bloquear**.
   - Cada critério (80/70, 100% dos críticos na RTM, 0 vulnerabilidades críticas…) com a evidência.
   - Os **riscos aceitos**, cada um com impacto, motivo e responsável: os 2 críticos do Spring, o `jaxb-core` e a D13.
   - O que cada integrante fez.
   - O limite do que foi provado: teste local não é "pronto para produção".
3. **RTM e relatório atualizados no commit da tag:**
   - A RTM (matriz de rastreabilidade) liga cada requisito ao risco, ao teste que o prova e à evidência (run do CI).
   - O relatório de qualidade compara os resultados com as metas.
   - Os dois precisam descrever **o mesmo commit que a tag aponta**, porque o professor confere os números na mesa. Número desatualizado conta como evidência errada.
4. **BDD executável (Cucumber) e evidência de TDD:**
   - BDD descreve a regra em linguagem de negócio (Dado / Quando / Então) num arquivo `.feature`. O Cucumber transforma cada frase num passo de teste que roda no `verify`, então o cenário escrito vira teste de verdade.
   - Hoje os cenários de aprovação e bloqueio existem só em texto (`bdd-aprovacao-bloqueio.md`).
   - O `evidencia-tdd.md` mostra o ciclo **red → green → refactor** (commit do teste falhando, commit que faz passar, commit que limpa) para cada cenário.
5. **README × `pom.xml`:**
   - O professor clona e segue o README. Se o README promete WireMock e Cucumber e a ferramenta não está no projeto, a afirmação não tem evidência.
   - Correção: tirar da lista o que não existe ou incluir de fato.
6. **Mutação manual:** quebrar de propósito uma regra crítica no código (ex.: deixar o Solicitante aprovar) e ver se algum teste fica vermelho. Se nenhum falhar, o teste não protege aquela regra e é preciso escrever o que falta. Depois, `git restore`, para nenhuma mutação sobrar no código. O banco já fez as dele (M1 a M5); faltam aprovação, manutenção e autorização no serviço.
7. **`plano-tdd.md`:** o inventário das regras de negócio, com a matriz **regra → caso de teste → teste → status**. Mostra que nenhuma regra ficou sem teste e serve de mapa para os ciclos de TDD.
8. **OpenAPI (RF-14):** documentação da API gerada a partir do código (ex.: `springdoc-openapi`, com Swagger UI em `/swagger-ui.html`). Lista cada rota, parâmetro, corpo e código de resposta, para alguém usar a API sem ler o código. O endpoint do relatório (`GET /relatorios/utilizacao`) também precisa estar documentado.
9. **"Pendências e divisão" em `status-requisitos.md`:**
   - O arquivo marca cada RF como feito, parcial ou ausente; só vale "feito" com o teste citado.
   - A seção vazia é a lista do que falta, ordenada pelos limitadores de nota e com dono, para o grupo não esquecer nada até a entrega.
10. **Números no `ciclo-01.md`:**
    - O ATAM liga atributos de qualidade (desempenho, segurança, testabilidade) a cenários, riscos e decisões.
    - A Aula 07 pede levar ao ciclo 01 as medidas reais (cobertura do JaCoCo, Quality Gate e issues do Sonar), para os riscos de testabilidade serem confirmados ou fechados com números. O RK9 é um desses riscos e ainda está "a confirmar".
11. **Clone limpo e checklist:**
    - Alguém que **não** escreveu o README clona o repositório numa pasta vazia e segue só o README: sobe o banco, roda a aplicação, roda o `verify`, abre o JaCoCo.
    - Prova o primeiro limitador de nota ("aplicação executa").
    - O que falhar vira bloqueador no `checklist.md`, a ser corrigido antes da tag.
12. **Prova oral:**
    - O banco de 15 perguntas cita arquivos do projeto e mistura as áreas, porque cada integrante precisa responder sobre o que **não** escreveu.
    - O ensaio cronometrado (8 minutos: problema, arquitetura, demo, testes e CI, RTM e relatório, limitações) mostra as lacunas, que viram tarefas com dono.

**Fora do escopo do projeto do grupo:** as Aulas 01 a 03 e 08 usam o repositório da disciplina ou o Foot Fanatics. O RF-11 (notificação externa, ADR-0005) segue ausente, sem cliente e sem WireMock.

**Não verificado:** os números do JaCoCo do run 211 (lidos só no SonarCloud) e se a matriz de autorização já teve célula vermelha.

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
- [x] `docs/rtm.md` inicial: requisito → risco → teste (a escrever) → evidência. — feito em 11/11/2026 (Leandro, PR #42): RF-01 a RF-14, RN-01 a RN-13 e RNF-01 a RNF-03, cada um com risco, teste existente por camada (ou "a escrever"), evidência no run 43091027823 do CI e situação. A caixa de "100% dos críticos" (Etapa 5) continua aberta: o RF-01 não tem teste da regra.
- [x] `docs/testes/plano-tdd.md`: inventário de todas as regras com casos (feliz, limite, inválido, conflito, estado proibido). — **feito em 13/11/2026** (Leandro com IA, na área do Pedro, com o OK do Leandro; Pedro revisa), citando só teste que existe e nomeando as lacunas: Responsável rejeitar a própria reserva (D10 (c)), RF-03 completo (depende da V2) e o RF-11 inteiro.

### Etapa 3 — Testes e integração (Dias 3–5 · qua 04/11 – sex 06/11)
**Equivale às Aulas 05, 06, 08 e 09.**
- [ ] **[Aula 09 · decisão D1, seção 9]** D1 decidida em 05/11 pelo Gustavo: intervalo semiaberto `[início, fim)` (ADR-0002). **Falta** levar a decisão para `docs/testes/bdd-aprovacao-bloqueio.md` (tabela de fronteiras e "Perguntas em aberto", arquivo do Pedro) e obter a confirmação de Pedro e Leandro, **antes** do primeiro teste vermelho. A decisão define os limites do `tstzrange` no banco (limite inferior fechado e superior aberto).
- [x] **Migração Flyway V1 e testes de banco (parte do Gustavo):** `V1__nucleo_da_reserva.sql` (usuario, recurso, manutencao, reserva, reserva_recurso, tentativa_recusada, auditoria_reserva, `btree_gist` e a constraint de exclusão), com `test(red)` antes de `feat(green)`. — feito em 06/11 no PR `feat/banco-v1`: testes em `src/test/java/br/com/senac/recursos/banco/` (Testcontainers, PostgreSQL 16, sem H2); `verify` com 104 testes, 0 ignorados. Decisões D6 e D7 (seção 9) e ADR-0007 (proposto). Mutação feita na V1 (constraint neutralizada → 22 de 41 testes vermelhos); ainda **não** registrada em `docs/testes/mutacao-manual.md`. A tradução em 409 e o teste de API dependem do serviço do Pedro. Esquema consolidado para consulta em `docs/banco/schema.sql` (gerado com `pg_dump` em 07/11 a partir da V1 e recarregado num PostgreSQL 16.15 limpo; documentação, não migração).
- [ ] **TDD red:** testes vermelhos para cada regra do plano (commits `test(red): ...`). — os de banco estão feitos (linha acima); faltam os de domínio, API e BDD.
- [ ] **TDD green:** domínio e regras até a suíte ficar verde (`feat(green): ...`, `refactor: ...`). `Clock` injetado, nunca `Instant.now()` solto nem `Thread.sleep`.
- [ ] Entidades: Recurso (sala/material, restrito, capacidade, localização), Professor (competências), Reserva + estados, Manutenção/Bloqueio, Retirada/Devolução, Auditoria, Usuário/Perfil. — 11/11 (Pedro): entidades JPA de todas as tabelas da V1 (`model/`), `Periodo` mapeado para `tstzrange` (`converter/PeriodoUserType`), `ddl-auto: validate` conferido contra a V1. **Falta** (depende de V2, Gustavo): localização, patrimônio, competências e `movimentacao_material`.
- [x] Spring Security com login que devolve token; erros com `ProblemDetail`. — 11/11 (Pedro, PR #44): `POST /api/auth/login` (JWT HS256, `Clock` injetado na emissão e na validação), cadeia stateless em `/api/**` com `denyAll` para rota fora da matriz e cadeia própria para as telas (D12 decidida em 12/11: login com sessão, ADR-0009), `TratadorDeErros` e `RespostaDeErroDeSeguranca`; testes em `AutenticacaoControllerTest` e `TratadorDeErrosTest`. Verificado só localmente.
- [ ] Integração com **Testcontainers** (PostgreSQL real).
- [ ] Notificação via cliente HTTP + **WireMock** cobrindo 200, 404, 500, timeout, JSON inválido e "token do provedor não vaza".
- [x] **BDD:** `docs/testes/bdd-aprovacao-bloqueio.md` + `aprovacao-bloqueio.feature` (Cucumber) para aprovação de restrito, auto-aprovação proibida, manutenção (fronteiras exatas) e transições de estado; `docs/testes/evidencia-tdd.md`. — **feito em 13/11/2026** (Leandro com IA, na área do Pedro, com o OK do Leandro; Pedro revisa): Cucumber 7.23.0, 13 cenários (9 cenários, um deles com 5 linhas de Esquema), passos contra a aplicação real com PostgreSQL em Testcontainers e relógio fixo; `./mvnw -B test -Dtest=AprovacaoBloqueioTest` com 13 testes, 0 falhas, 0 ignorados, nesta máquina. A D1 saiu de "pergunta em aberto" do BDD, apontando para o ADR-0002.
- [x] Ciclo ATAM 01: `docs/avaliacao-arquitetural/ciclo-01.md`. (feito em 07/11: cenários C1 a C5 em seis partes, pontos de sensibilidade, trade-offs ligados aos ADRs, riscos e ações; medido só no nível do banco, C2, C3 e C5 **não medidos**; **escrito sem sessão com Pedro e Leandro**; o ciclo 02 repete C1 a C4 depois da API e da rodada de carga.)
- [x] Interface responsiva mínima (Thymeleaf, como o repo da disciplina, ou página estática simples). — feito em 11/11/2026: as 15 telas do inventário em Thymeleaf e o E2E Playwright em 375 px (`Responsividade375E2ETest` e mais 3 classes em `src/test/java/br/com/senac/recursos/e2e`, prints em `docs/evidencias/e2e-375/`). Em 12/11/2026 as telas foram ligadas ao back-end (D12, ADR-0009) e o E2E ganhou fluxos contra o back-end real: login, sair, 403, reserva gravada, 409 de horário, cancelamento e aprovação (`AcessoE2ETest`, `ReservaE2ETest`, `ResponsavelEAdministradorE2ETest`); os testes de layout continuam com dados de exemplo em `/e2e/...`.

### Etapa 4 — CI e SonarCloud (em paralelo desde o Dia 1, fechado no Dia 5)
**Equivale às Aulas 06–07.**
- [x] `ci.yml` completo: `pull_request` + push na `main`, `permissions: contents: read`, Temurin 21, cache Maven, `concurrency`, `timeout-minutes`, artifacts do Surefire/Failsafe/JaCoCo com `if: always()`. — conferido em `.github/workflows/ci.yml` em 07/11: gatilhos (linhas 3 a 6), `permissions` (8 e 9), `concurrency` (12 a 14), `timeout-minutes: 20` (19), Temurin 21 com `cache: maven` (24 a 28), artifacts `surefire-reports` e `site/jacoco` com `if: always()` (31 a 38), SonarCloud com `fetch-depth: 0` (23 e 40 a 45). Failsafe não se aplica: o `pom.xml` não usa o plugin, todos os testes rodam no Surefire.
- [x] Um run vermelho proposital e sua recuperação, IDs anotados no README. — vermelho: run #120, ID 42427577443, commit `22b68ae`, `failure`; recuperação: run #121, ID 42428144208, commit `1e5cc0b`, `success` com `verify` e SonarCloud verdes (PR #30, 07 e 08/11 em UTC).
- [x] JaCoCo com gate **80% linhas / 70% branches** falhando o `verify`. — regra no `pom.xml` (`jacoco.min.line` 0.80 e `jacoco.min.branch` 0.70, propriedades nas linhas 24 e 25, execução `check` na fase `verify`). Medido só nesta máquina em 07/11: `verify` normal passa (427 testes, 0 ignorados, "All coverage checks have been met"; cobertura de `target/site/jacoco/jacoco.csv`: 99 de 99 linhas e 71 de 76 branches, 93,4%); com `-Djacoco.min.branch=0.99` o mesmo `verify` falha com "branches covered ratio is 0.93, but expected minimum is 0.99". Não medido: falha do gate no GitHub Actions.
- [ ] SonarCloud no PR (`fetch-depth: 0`, lendo o XML do JaCoCo), Quality Gate verde, cada issue tratada (corrigir/justificar), nunca baixando meta. — estado em 05/11: Quality Gate Passed na `main`, cobertura 100% lida do JaCoCo (código ainda pequeno), 0 Security Hotspots, 1 issue médio (regex em `CssSemValoresSoltosTest`, a corrigir pelo Leandro), 48 "Dependency risks" (8 Critical e 10 High por CVSS). PR `fix/dependencias-seguras` sobe Tomcat, Jackson, PostgreSQL, Logback, log4j e jsoup e inclui os templates na análise; os 2 críticos do Spring Framework 6.2.19 não têm correção na linha 3.5 e estão justificados em `docs/seguranca/riscos-dependencias.md`. Em 09/11 o gate da `main` estava vermelho (Reliability C) desde o PR #12 e voltou a Passed com o merge do #32 (DEF-001), lido na API pública em 09/11. O projeto está **Public** desde 09/11/2026. Em 11/11/2026 os 2 críticos do `spring-webmvc` 6.2.19 foram marcados como **Accepted** no SonarCloud (PR #45, seção 6 de `docs/seguranca/riscos-dependencias.md`): 19 riscos abertos, 0 Critical, 1 High (`jaxb-core` 4.0.9, ainda não avaliado); Quality Gate OK com 0 vulnerabilidades. Em 11/11/2026, noite, o `jaxb-core` foi avaliado: sem versão corrigida e o projeto não desserializa XML, então ficou **aceito com justificativa** (seção 7 de `docs/seguranca/riscos-dependencias.md`; a marcação Accepted no painel é manual do Gustavo). Na mesma leitura o painel tinha 22 riscos em 141 dependências, com 3 novos do Spring Security 6.5.11 ainda sem nota e não avaliados. Em 12/11/2026 o CVE-2026-92877 (`jaxb-core`) foi marcado como **Accepted** no painel, com o motivo da seção 7, e conferido no filtro "Accepted". A análise do `2610d6a` (merge do #50) teve Quality Gate OK, 0 vulnerabilidades e 0 hotspots (seção 4.1). **Falta:** conferir o Quality Gate e o painel de riscos na análise do commit da tag; a caixa fica aberta até lá.

### Etapa 5 — Concorrência, carga, auditoria, autorização (Dias 6–7 · sáb 07/11 – dom 08/11)
**Equivale às Aulas 10–12.**
- [x] **[Aula 11]** Começar a Etapa 5 pelo diagnóstico: atualizar `docs/status-requisitos.md` (feito/parcial/ausente; só vale "feito" com teste citado), ordenar as pendências pelos 4 limitadores de nota e dividir entre os três até a Semana 46. Esse arquivo é atualizado em **todo** PR depois disso, não só no fim. — feito em 12/11/2026, noite (Leandro, branch `docs/rtm-relatorio-1211`): `status-requisitos.md` recontado com teste citado, e a seção "Pendências e divisão" ordenada pelos limitadores, com dono.
- [x] **[Aula 11 · banco]** (feito em 06/11: tabela `tentativa_recusada` na V1, com recurso, solicitante, período pedido, motivo `SOBREPOSICAO` e `registrada_em`; gravação em transação própria provada em `TentativaRecusadaBancoTest`. A consulta do relatório ainda não existe.) O relatório de utilização pede "conflitos evitados" = tentativas recusadas por sobreposição no período. Isso só existe se a tentativa recusada for **gravada** (data, recurso, intervalo pedido, motivo). Entra no modelo de dados da migração V1, senão o RF-12 não tem como ser calculado nem testado.
- [x] **[Aula 10 · pré-requisito]** Apache JMeter 5.6 instalado (`jmeter -v`) na máquina de quem rodar a carga (Leandro); aplicação local com PostgreSQL em container. Rodada sempre em modo CLI contra a aplicação local. — feito em 12/11/2026 (Leandro, branch `perf/jmeter-carga`): JMeter 5.6.3 conferido pelo SHA-512, rodado em modo CLI no container da sessão contra a aplicação local.
- [x] Teste de concorrência: 20 requisições simultâneas (`ExecutorService` + `CountDownLatch`) → 1 aceita, 19× **409**, contagem **no banco**; repetir para a agenda do professor; rodar 5×. Mostrar vermelho sem a proteção. — **nível do banco feito em 06/11** (`ConcorrenciaReservaBancoTest`: 1 aceita e 19 recusadas com `23P01`, contagem no banco, recurso e professor, 5 rodadas; vermelho sem a proteção mostrado trocando o predicado por `WHERE (false)`). Falta o 409 de ponta a ponta (serviço do Pedro). — **11/11 (Pedro): 409 de ponta a ponta** em `ConcorrenciaReservaApiTest` (20 pedidos HTTP com servidor real → 1 × 201 e 19 × 409, nunca 500; contagem no banco; sala e professor; 5 rodadas cada), verde só localmente. A caixa continua aberta até o run do CI e o vermelho sem a proteção no nível da API. — **12/11 (Gustavo):** o `ConcorrenciaReservaApiTest` roda no `verify` do CI. No log do run 43276426460 (`main`, merge do #48) ele aparece com 10 testes e 0 falhas. Falta o vermelho sem a proteção no nível da API (Pedro). — **12/11 (Pedro): caixa fechada.** Correção: o run 43276426460 terminou com `conclusion: failure` no passo "Build, testes e cobertura" (API pública do GitHub, conferida em 12/11); o run verde que vale é o **43302104277** (`main`, commit `986e1cd`, `conclusion: success`). O log por teste desse run pede login e não foi aberto: a evidência é o `verify` verde com o teste na suíte. **Vermelho sem a proteção no nível da API:** mutação M1 (`WHERE (false)` na constraint `reserva_recurso_sem_sobreposicao`) → `ConcorrenciaReservaApiTest` com **10 de 10 vermelhos**, as 20 respostas viram 201 (`{201=20}`); sem a mutação, 10 de 10 verdes. Medido só na máquina do Pedro; ver `docs/testes/mutacao-manual.md`, "Mutações do back-end".
- [x] `docs/performance/rnf.md` (escrito **antes** de rodar) → `perf/reservas.jmx` + `perf/usuarios.csv` → rodada CLI → `docs/performance/resultado-01.md` (p50/p95/p99, vazão, erro separando 409 de erro real, limites da conclusão). — feito em 12/11/2026 (Leandro): `rnf.md` no commit `0464847`, antes da rodada; `perf/reservas.jmx`, `perf/usuarios.csv`, `perf/resultados/rodada-01.jtl` e `rodada-02.jtl`; `docs/performance/resultado-01.md`. Busca com p95 de 17 ms (rodada 01) e 8 ms (rodada 02, os 50 ativos ao mesmo tempo); criação só 201 ou 409. Só local.
- [ ] Auditoria por TDD, com teste de rollback (sem auditoria órfã) e 403 do Solicitante no histórico. (**nível banco feito em 07/11:** `AuditoriaBancoTest`, 3 testes: commit grava estado e auditoria juntos; falha do banco na mudança de estado desfaz a auditoria; falha do banco na auditoria desfaz a mudança de estado. Mutações M3a e M3b em `docs/testes/mutacao-manual.md`. Falta o serviço que grava a auditoria, a API do histórico e o 403 — Pedro; a caixa continua aberta.) **[Aula 11]** O registro guarda reserva, usuário, instante (via `Clock`), estado anterior, estado novo e motivo; a API não altera nem apaga registro; reserva já iniciada não pode ser apagada; a consulta do histórico é só de Responsável e Administrador (RN-09 a RN-12 do PRD). Testes de unidade primeiro, depois integração com Testcontainers; commits de teste e de código separados. — **13/11/2026 (Leandro com IA, na área do Pedro, com o OK do Leandro; Pedro revisa):** a RN-08 passa a ser conferida em **toda** transição: `ReservaApiControllerTest.AuditoriaDeCadaTransicao` cobre cancelamento, não comparecimento e rejeição (quem, quando pelo `Clock`, estado anterior e novo, motivo) e prova que uma transição recusada **não** grava registro (contagem no banco). A caixa continua aberta pelo que falta no texto acima.
- [ ] `GET /relatorios/utilizacao` com testes caixa-preta e valores calculados à mão no comentário do teste (período vazio, reserva cruzando borda, cancelada não conta). (**consultas no nível banco feitas em 07/11:** `src/main/resources/db/consultas/relatorio-*.sql` (horas por recurso, carga por professor, conflitos evitados) e `RelatorioUtilizacaoBancoTest`, 6 testes com valores calculados à mão. Premissas **pendentes de confirmação do grupo**: estados que contam como uso, "período" dos conflitos evitados (`registrada_em`) e recurso sem uso sem linha. Falta o endpoint, o serviço, o 401/403 e os testes caixa-preta — Pedro; a caixa continua aberta.)
- [x] `docs/status-requisitos.md` (feito/parcial/ausente com teste citado). — feito em 12/11/2026, noite (Leandro): recontado sobre `6ce4478`, com linhas de RNF.
- [ ] `docs/seguranca/matriz-autorizacao.md` (operação × anônimo/Solicitante/Responsável/Admin) → `@ParameterizedTest` lendo a matriz; casos de posse; validação 400 com mensagem clara e sem stack trace. **[Aula 12]** Toda rota do código precisa estar na matriz; marcar onde o código diverge da matriz (sem corrigir na hora) e listar rotas fora de qualquer regra de segurança. Cada célula vermelha vira `docs/defeitos/DEF-xxx` **antes** da correção, e a correção vem em commit próprio citando o defeito. Nenhuma célula é apagada para o teste passar.
- [x] `docs/testes/mutacao-manual.md`: quebrar cada regra crítica, ver o teste falhar, `git restore`. (**seção do banco feita em 07/11:** M1 constraint, M2 trava, M3a/M3b atomicidade da auditoria, M4/M5 relatório; as demais regras estão como "pendente (Pedro)". Caixa continua aberta.) **[Aula 12]** As seis regras: sobreposição, dupla reserva, aprovação de restrito, manutenção, autorização e auditoria. Teste que continua verde com a regra quebrada é lacuna: escrever o teste que falta. Nenhuma mutação pode sobrar no código. — **13/11/2026: as seis regras feitas.** A seção do serviço traz 8 mutações (aprovação, manutenção, autorização e auditoria) com o número de vermelhos de cada uma e, na MAU1, a lacuna que ela achou. Nenhuma mutação sobrou no código (`git status` vazio).
- [x] `docs/rtm.md` com **100%** dos críticos ligados a teste e run do CI. — feito em 12/11/2026, noite (Leandro): RF-01 e RF-06 `feito` no run 43303372662; as seções "a incorporar" do PR #44 e das telas viraram linhas. Conferir de novo no commit da tag.
- [ ] Cada falha encontrada vira arquivo em `docs/defeitos/` antes da correção.

### Etapa 6 — Entrega (Dias 9–12 · seg 09/11 a sex 13/11)
> Meta interna: tag `v1.0.0-rc1` e formulário enviados até **qua 11/11**. Quinta fica de folga para correções (`rc2`) e sexta é a aula de entrega e conferência na mesa.
> Na aula de sex 06/11 (Aula 13), usar o tempo de sala para avançar o projeto e tirar dúvidas com o professor.
**Equivale às Aulas 13–14.**
- [ ] `docs/relatorio-qualidade.md` com números reais (cada número com fonte). (**versão 1 em 11/11/2026**, Leandro, PR #42, sobre o commit `e66c000`: estrutura completa da Aula 13 e recomendação "bloquear"; a caixa fica aberta até os números serem refeitos no commit da tag, seção 8 do relatório.) **[Aula 13]** Estrutura pedida: identificação (repositório, commit, data, integrantes, ambiente), escopo e fora de escopo, estratégia de teste por nível, resultados comparados às metas (80/70, 100% críticos na RTM, 0 bugs/vulnerabilidades críticas), defeitos por severidade e situação, riscos residuais e recomendação. Cada afirmação com **evidência, interpretação e limite**; o que não foi medido fica "não medido", com o motivo. **Versão 2 em 12/11/2026, noite** (Leandro, branch `docs/rtm-relatorio-1211`), sobre `6ce4478` e o run 43303372662, com a carga do PR #56. A recomendação passou a "liberar com ressalvas, desde que o clone limpo passe", confirmada pelo Leandro em 12/11/2026, noite. A caixa fica aberta até os números do commit da tag.
- [ ] `docs/prova-oral/perguntas.md` (15 perguntas do projeto) e ensaio cronometrado → `docs/prova-oral/ensaio.md`. — **perguntas feitas em 13/11/2026** (15, cada uma com o arquivo da resposta e a descrição da resposta fraca; distribuição conferida na seção final do arquivo). **O ensaio continua pendente e é humano**, por isso a caixa fica aberta. **[Aula 13]** Perguntas citam arquivos do projeto e distribuídas para ninguém responder só sobre a própria parte; respostas **não** são escritas; o ensaio é individual (todos presentes) e as lacunas viram tarefas com responsável.
- [ ] **[Entregável da especificação: "Apresentação e demonstração do sistema"]** Roteiro da demo dentro dos 8 minutos, massa de dados de demonstração e ensaio no notebook que será usado (banco subindo com `docker compose up -d`, aplicação em `localhost:8080`). Sem aplicação executando, é limitador de nota. — **roteiro e massa escritos em 13/11/2026** (`docs/prova-oral/roteiro-demo.md`: preparo antes da sala, script que cria a massa pela própria API, os 8 minutos divididos por pessoa, o passo a passo da demonstração e o que cortar se faltar tempo). **O ensaio no notebook da apresentação continua pendente e é humano**; os tempos do roteiro são alvo, não medida.
- [ ] Checklist em **clone limpo** seguindo só o README → `docs/entrega/checklist.md`. **[Aula 14]** Quem faz o clone não pode ter escrito o README nem reaproveitar nada da própria máquina; anotar o que faltou, listar bloqueadores em ordem e só então corrigir. (**roteiro pronto em 07/11:** `docs/entrega/roteiro-clone-limpo.md`. **Rodada feita em 13/11/2026**, pela IA, num container novo, no commit `9acbbcd`: aplicação sobe, `/actuator/health` responde `{"status":"UP"}`, reserva de ponta a ponta pela API e pela tela, 409 na sobreposição, `verify` com 1116 testes, 0 falhas e 0 ignorados em 3 min 52 s, JaCoCo 95,8% de linhas e 81,3% de branches; único ajuste necessário foi no README, que não dizia que `ADMIN_LOGIN`/`ADMIN_SENHA` são o único jeito de entrar. Checklist e seção 4 do roteiro preenchidos. **A caixa continua aberta** porque a Aula 14 pede uma **pessoa** que não escreveu o README, e o grupo ainda não decidiu se a rodada da IA vale.)
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
| D3 | Qual código HTTP para transição de estado inválida: 409 ou 422? | Pedro (API) | Antes dos testes da máquina de estados | Contrato da API e matriz de autorização | Ver `docs/arquitetura.md`, seção 5.2 | **Decidida em 05/11/2026 (Gustavo): 409**, com `ProblemDetail` de tipo próprio (diferente do conflito de horário). Como a API é do Pedro, **a confirmar com ele**; registrada no ADR-0005 e na `arquitetura.md`. **OK do Pedro em 12/11/2026** |
| D4 | "0 vulnerabilidades críticas" se mede pelo CVSS das dependências ou pelo Quality Gate e issues do SonarCloud? | Professor (Gustavo pergunta) | Antes da entrega (13/11) | Define se os 2 críticos do Spring 6.2.19 precisam de algo além da justificativa | Ver `docs/seguranca/riscos-dependencias.md` | **Tratamento definido em 05/11/2026 (Gustavo):** reportar as **duas medições separadas** no relatório (CVSS das dependências e Quality Gate) e perguntar ao professor qual vale. **Resposta do professor ainda pendente** (sem contato até 11/11/2026). **Situação em 11/11/2026, noite:** pelas duas medições não há crítica **aberta** (Quality Gate OK com 0 vulnerabilidades; dependências com 0 Critical aberto, os 2 do Spring **aceitos com justificativa**, não corrigidos), ver a seção 6 de `docs/seguranca/riscos-dependencias.md`. **Sem resposta até a entrega, vale o tratamento de 05/11 com a leitura mais rígida:** o relatório e `docs/decisao-qualidade.md` (Leandro) mostram as duas medições e registram os 2 aceitos como risco residual (impacto, justificativa, responsável Gustavo), e a pergunta vai ao professor pessoalmente na Aula 14 (13/11), antes do envio do formulário |
| D5 | Quais estados da reserva ocupam o horário (e, portanto, entram na constraint de exclusão)? | Grupo (Gustavo propõe); o PRD (RN-05) lista os estados mas não responde | Antes da migração V1 | Define a cláusula parcial da constraint `EXCLUDE USING gist` (ADR-0002) e o teste de cancelamento | Cancelada, rejeitada e não compareceu liberam o horário | **Decidida em 05/11/2026 (Gustavo): ocupam `SOLICITADA`, `APROVADA`, `EM_USO` e `CONCLUIDA`; liberam `CANCELADA`, `REJEITADA` e `NAO_COMPARECEU`.** Registrada nos ADRs 0002 e 0006; a confirmar com Pedro e Leandro |
| D6 | Como modelar, no banco, a proteção contra reserva dupla para recurso **e** professor, o estado que a torna parcial (D5), a tabela das tentativas recusadas (D2) e os índices da V1? | Gustavo (banco); o Pedro escreve o serviço que grava as linhas | Antes da migração V1 | Forma da V1 e do serviço de reserva (duas linhas por reserva de sala com docente, em ordem de `recurso_id`) | As quatro escolhas recomendadas ao Gustavo (ADR-0007) | **Decidida em 06/11/2026 (Gustavo):** professor é recurso e há uma só constraint `EXCLUDE`; estado copiado para `reserva_recurso` por FK composta `ON UPDATE CASCADE`; `tentativa_recusada(recurso_id, solicitante_id, periodo_pedido, motivo, registrada_em)`; só os índices necessários (nenhum de busca antes de medir); V1 só com o núcleo da reserva (`movimentacao_material` e colunas sem decisão ficam para a V2). Registrada no ADR-0007 (aceito). **OK do Pedro em 12/11/2026** |
| D7 | Como evitar o deadlock (`40P01`) que apareceu nos 20 pedidos simultâneos sobre a constraint de exclusão? | Gustavo (banco) | Antes de fechar a V1 | Código de erro previsível (`23P01` → 409) e tempo de resposta sob disputa (RNF-03) | Trigger com `pg_advisory_xact_lock` por recurso | **Decidida em 06/11/2026 (Gustavo):** trigger de trava por recurso na V1. Medido: sem ela 9 de 40 rodadas falharam; com ela 30 de 30 passaram (só nesta máquina). Efeito sobre RNF-01 e RNF-03 **não medido**. Registrada no ADR-0007 (aceito). **OK do Pedro em 12/11/2026** |
| D8 | No relatório de utilização (RF-12): (a) quais estados contam como uso nas horas por recurso e na carga por professor; (b) o "período" dos conflitos evitados é o da hora em que a tentativa foi registrada (`registrada_em`) ou o do intervalo pedido (`periodo_pedido`); (c) recurso sem uso na janela aparece com zero ou não aparece?; (d) "professor" na carga por professor é o docente alocado (recurso do tipo PROFESSOR, D6) ou o solicitante que pediu a reserva? Contrato com a tela 12 (`docs/frontend/README.md`, seção do relatório): a tela espera `reservas` por linha e `totalReservas`, que as consultas ainda não devolvem, e `totalHoras` não pode contar duas vezes a reserva de sala com docente (duas linhas em `reserva_recurso`) | Grupo: Pedro (serviço), Leandro (tela e RTM), Gustavo (consultas) | Antes de o Pedro ligar o endpoint às consultas | Valores do relatório, oráculo calculado à mão e a tela 12 | Os testes de banco assumem (a) os estados da D5 (`SOLICITADA`, `APROVADA`, `EM_USO`, `CONCLUIDA`), (b) `registrada_em` (o índice `tentativa_recusada_recurso_data_idx` da V1 já foi feito para isso) e (c) sem linha (zero é apresentação) e (d) o docente alocado (`relatorio-carga-por-professor.sql` filtra `recurso.tipo = 'PROFESSOR'`); a spec do Leandro diz "quem pediu a reserva (o solicitante)", o que diverge das consultas | **Pendente.** Nenhuma escolha foi decidida (registrado em 09/11/2026 a partir da leitura da spec do Front-end; nenhum teste cobre ainda a contagem de reservas nem o total sem dupla contagem); as consultas em `src/main/resources/db/consultas/` e `RelatorioUtilizacaoBancoTest` só registram a premissa em comentário |
| D9 | Quando o Solicitante ainda pode **alterar** ou **cancelar** a própria reserva? (a) Cancelar vale para `SOLICITADA` além de `APROVADA` (o diagrama da spec principal só mostra `APROVADA → CANCELADA`, "a confirmar")? (b) "Reserva já iniciada" (RN-07) é medida pelo horário de início ou pelo estado `EM_USO`? (c) Alterar uma reserva `APROVADA` de recurso restrito volta a `SOLICITADA`? | Grupo: Pedro (regra no serviço), Leandro (tela) | Antes de o Pedro escrever `POST /reservas/{id}` e `POST /reservas/{id}/cancelar` | Botões das telas 7 e 8 e testes de transição de estado | Telas 7 e 8 mostram Alterar e Cancelar só para `Solicitada` ou `Aprovada` com `iniciada` falso (o back-end manda `iniciada` = o horário de início já chegou); (c) fica para o back-end | **Pendente.** Registrada em 11/11/2026 (Leandro) junto com as telas 7 e 8, que assumem a proposta; o back-end continua obrigado a recusar a transição inválida |
| D10 | Regras de reserva que o PRD não fixa: (a) reserva **sem** recurso restrito nasce APROVADA ou SOLICITADA? (b) o Administrador pode cancelar ou alterar reserva de outra pessoa? (c) o Responsável pode rejeitar a própria reserva (RN-06 só fala em aprovar)? (d) há limite de horário para retirada e para "não compareceu" (antes do início, depois do fim)? | Grupo (Pedro propõe) | Antes da tag | Estado inicial, matriz de autorização e testes de `ReservaControllerTest` | O código de 11/11 implementa a proposta: (a) APROVADA, lendo o RF-07 ("aprovação obrigatória para recurso restrito"); (b) não, só o dono; (c) sim; (d) sem limite | **Decidida em 12/11/2026 (Pedro): fica como está no código**, a proposta vira a regra: (a) sem recurso restrito nasce APROVADA (`ReservaApiControllerTest.Criar.salaComProfessorSemRestritoNasceAprovadaComAuditoria`); (b) não, cancelar e alterar são só do dono, inclusive para o Administrador (célula `dono` da matriz, conferida por `MatrizAutorizacaoTest.cadaCelulaDaMatrizValeNaApi`); (c) sim, o Responsável pode rejeitar a própria reserva (RN-06 só proíbe aprovar; teste `ReservaApiControllerTest.Aprovacao.responsavelPodeRejeitarAPropriaReserva` desde 13/11/2026); (d) sem limite de horário para retirada e para "não compareceu" (teste `ReservaApiControllerTest.SemLimiteDeHorarioNoCicloDeUso` desde 13/11/2026). Aceite do grupo na revisão do PR |
| D11 | Quando o Administrador cria uma manutenção, o que acontece com as reservas que já existem no período (RF-08)? | Grupo (Pedro propõe, Leandro confere o aviso de "reservas afetadas" da tela 15) | Antes da tag | Regra de manutenção e mensagem ao usuário | Manter as reservas e só bloquear as novas (comportamento de 11/11) **ou** recusar a manutenção com 409 listando as afetadas | **Decidida em 12/11/2026 (Pedro): fica como está no código.** A manutenção é gravada, as reservas que já existem no período continuam como estão e só as novas são recusadas (RN-04). A tela 15 lista as reservas afetadas e pede confirmação antes de gravar (`TelasComBancoTest`, comentário "D11"); avisar quem tem reserva afetada fica fora da V1. Teste na API desde 13/11/2026: `ReservaApiControllerTest.Manutencao.manutencaoSobreReservaExistenteMantemAReservaEBloqueiaSoAsNovas`. Leandro confere o aviso da tela 15 na revisão do PR; aceite do grupo na revisão |
| D12 | Como as telas Thymeleaf autenticam e conferem o perfil? (a) formulário em `/login` com sessão do Spring Security, numa cadeia própria; (b) o mesmo JWT da API, guardado em cookie `HttpOnly` pela tela de login; (c) outra. Inclui: quais telas cada perfil vê e o que acontece com o 401/403 da tela (`/login?expirou`, página 403) | Grupo: Pedro (segurança), Leandro (telas), Gustavo (ADR) | **Antes de qualquer tela exibir dado real** e antes da tag | Matriz de autorização (hoje todas as telas `ok` para todos), RF-01 e RF-13, E2E | Pedida pelo Leandro na revisão do PR #44 ("vamos combinar hoje como as telas convivem"). Sem proposta escolhida: (a) casa com o formulário que já existe; (b) reaproveita a API | **Decidida em 12/11/2026: opção (a)**, informada pelo Leandro como combinada com o grupo e aceita pelo Gustavo na revisão do PR #48 (ADR-0009 aceito, com 3 ressalvas registradas no ADR). Formulário em `/login` com sessão do Spring Security (`SegurancaDasTelasConfig`), telas chamando os services no mesmo processo, perfil por rota espelhando a API (tabela "Telas" de `docs/seguranca/matriz-autorizacao.md`); 401 da tela leva a `/login` (`?expirou` com o cookie vencido), 403 é a página 403. Itens do menu por perfil: a proposta de `docs/frontend/README.md`. **Ressalva 1 (limite de tentativas no `/login`) resolvida em 12/11/2026** (Leandro, em TDD): 5 falhas em 15 min bloqueiam o login por 15 min, nas telas e na API, com um contador só |
| D13 | A recusa por manutenção (RN-04) é "consulta e depois grava" no serviço: uma manutenção criada entre a consulta e o commit da reserva passa. Aceitamos o risco ou protegemos no banco (trigger ou constraint entre `manutencao` e `reserva_recurso`, migração V2)? | Grupo: Gustavo (banco), Pedro (serviço) | Antes da tag | RN-04, RF-08 e o argumento "a regra crítica vive no banco" | Aceitar e registrar: a janela é de milissegundos, só o Administrador cria manutenção e o caso não está no PRD | **Decidida em 12/11/2026 (Gustavo): aceitar e registrar o risco**, sem migração V2 nem mudança no serviço. A brecha existe: uma manutenção criada entre a consulta de `ReservaService.exigirForaDeManutencao` e o commit da reserva deixa passar uma reserva no período bloqueado. Motivos do aceite: a janela é de milissegundos, só o Administrador cria manutenção e o caso não está no PRD. Responsável: Gustavo. Reabrir se o caso entrar no PRD ou depois da entrega. A evolução prevista é uma V2 com trigger em `reserva_recurso` e em `manutencao`, usando a mesma trava por recurso da D7, e o 409 traduzido no serviço; o lado "manutenção sobre reservas existentes" depende da D11. Registrada no ADR-0008. **OK do Pedro em 12/11/2026** |

Quando uma decisão for tomada: anotar a data e quem decidiu na linha, ajustar o ADR ou documento afetado e acrescentar o que mudou na seção 9 do `AGENTS.md`, se virar regra.

## 10. Como este plano se mantém atualizado
Regra do grupo (detalhes em `AGENTS.md`): **todo pull request atualiza este plano**. Na descrição do PR entra a seção "Plano de execução" (o que foi marcado ou mudou aqui) e, neste arquivo, as caixas da etapa e, se for o caso, `docs/status-requisitos.md` e `docs/rtm.md`. Desde 11/11/2026 o status de cada PR vai num arquivo próprio em [`docs/status/`](status/) e a entrada do changelog em `changelog.d/` (juntada no `CHANGELOG.md` antes da tag), e não mais num parágrafo novo no topo deste plano nem direto no `CHANGELOG.md`: todos os PRs inseriam no mesmo ponto e davam conflito a cada merge (regra 12 do `AGENTS.md`). Os parágrafos de status do topo ficam como histórico até 09/11. Quando um defeito, um feedback ou a revisão de um colega ensinar algo novo ao grupo, a regra correspondente entra no `AGENTS.md` com a indicação de onde ela é verificada.

- [x] **Specs do Notion (regra de 06/11/2026):** toda mudança que toque requisito, regra de negócio, rota, modelo de dados, decisão ou estado de entrega atualiza a spec correspondente no Notion no mesmo PR ou em até 24 h (avisando na descrição). Cada pessoa cuida da spec da própria área (Gustavo: Banco e arquitetura; Pedro: Back-end; Leandro: Front-end) e da spec principal quando mudar escopo, KPI ou critério de aceite. A descrição do PR traz a linha "Specs atualizadas: `<link>` ou não se aplica, porque ...". — regra escrita no `AGENTS.md` (seção 4 item 5, seção 9 regra 7) e no modelo de PR; a conferência é a linha na descrição e a revisão. Ainda **não há** checagem automática disso.
