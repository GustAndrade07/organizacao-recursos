# Plano de teste — Organização de Recursos

> **Esqueleto da Etapa 1 (Aula 03).** Define níveis, ferramentas, ambiente, metas e responsabilidades. O inventário regra por regra, com os casos (feliz, limite, inválido, conflito, estado proibido), é o `docs/testes/plano-tdd.md` (Etapa 2). Última atualização: 05/11/2026.
> Número sem fonte não entra: o que não foi medido está escrito **"não medido"**.

## 1. Objetivo e escopo
Dar **evidência objetiva** de que o sistema cumpre os 14 requisitos funcionais e as regras críticas do [PRD](prd.md), com foco nos quatro limitadores de nota: aplicação que não executa, pipeline ausente, falha de autorização e reserva dupla.
- **Dentro do escopo:** regras de negócio (RN-01..13), API, telas, banco e migrações, integração de notificação, concorrência, desempenho e segurança básica (autorização, validação de entrada, erro sem vazamento).
- **Fora do escopo:** teste em ambiente de produção ou compartilhado, teste de usabilidade com usuários reais, segurança além de autorização, validação de entrada e erro seguro. Resultados de desempenho valem **para a máquina e a carga medidas**, não para produção.

## 2. Níveis de teste
| Nível | Ferramenta | O que cobre | Dono da área | Onde roda | Situação |
|---|---|---|---|---|---|
| Unidade | JUnit 5 (Mockito só aqui) | Regras isoladas: intervalo, estados, aprovação, `Clock` | Pedro | `./mvnw test`, CI | começou no front (layout, páginas de erro, login); regras de negócio a fazer |
| Unidade parametrizado | JUnit 5 `@ParameterizedTest` | Fronteiras de horário, tabela de estados, matriz de autorização | Pedro | CI | a fazer |
| Repositório e constraints | JUnit 5 + Testcontainers (PostgreSQL real) | Migrações Flyway, `EXCLUDE USING gist`, auditoria, consultas do relatório | Gustavo | CI e local (com Docker) | a fazer (teste de fumaça existe) |
| Integração / API caixa-preta | MockMvc + Testcontainers | 200/4xx, 401/403, `ProblemDetail`, posse da reserva | Pedro | CI | a fazer |
| BDD | Cucumber-JVM (`.feature`) | Aprovação de restrito, auto-aprovação proibida, manutenção, transições de estado | Pedro | CI | cenários escritos em `docs/testes/bdd-aprovacao-bloqueio.md`; `.feature` a fazer |
| Integração externa | WireMock | Notificação: 200, 404, 500, timeout, JSON inválido, token que não vaza | Pedro | CI | a fazer |
| Concorrência | JUnit 5 + Testcontainers + `ExecutorService`/`CountDownLatch` | 20 pedidos simultâneos: 1 aceito, 19 × 409, contagem **no banco**; também agenda do professor | Gustavo | CI | a fazer |
| Ponta a ponta (E2E) | Playwright para Java ([ADR-0004](adr/0004-playwright.md)) | Fluxos de tela, 375 px, mensagens de erro | Leandro | CI (sem tela) | a fazer |
| Desempenho | JMeter 5.6 (somente CLI, aplicação local) | Disponibilidade e criação de reserva sob carga | Leandro | local | a fazer; plano em `perf/` |
| Mutação manual | `git restore` após quebrar a regra | Prova de que o teste crítico detecta a quebra | todos | local | a fazer (`docs/testes/mutacao-manual.md`) |

## 3. Política de mock e ambiente
- **Mock só em teste de unidade isolado.** Persistência e integração crítica usam **Testcontainers** (PostgreSQL 16 real) ou **WireMock**. Sem H2.
- **Docker** é requisito para desenvolver (README). Nenhum teste usa `disabledWithoutDocker` (o `AplicacaoSmokeTest` foi trocado em 06/11/2026, junto com os testes de banco), então o `verify` mostra `Skipped: 0`; sem Docker os testes falham. O CI (Ubuntu) já tem Docker.
- **Tempo:** `Clock` injetado nas regras de manutenção, auditoria e relatório; nunca `Instant.now()` solto nem `Thread.sleep` em teste.
- **Dados de teste:** massa pequena semeada pelo próprio teste, com usuários dos três perfis (Solicitante, Responsável, Administrador) e recursos comum e restrito. Valores esperados de relatório são **calculados à mão**, com a conta no comentário do teste (nunca copiados da saída da aplicação). A massa de demonstração é separada da massa de teste.
- **Banco:** esquema só por migração Flyway; nenhum teste altera o esquema por fora.

## 4. Regras críticas e onde são testadas
Mapa resumido. A lista completa de casos está em `docs/testes/plano-tdd.md`; a rastreabilidade oficial, em `docs/rtm.md`.

| Regra (PRD) | Nível principal | Observação |
|---|---|---|
| RN-01 término depois do início | Unidade parametrizado | Fronteiras exatas |
| RN-02 sem sobreposição (sala, material, professor) | Repositório (constraint) + unidade | Intervalo semiaberto a confirmar (pergunta em aberto do BDD) |
| RN-03 pedidos simultâneos → 1 reserva | Concorrência | Vermelho sem a proteção; rodar 5 vezes |
| RN-04 manutenção bloqueia reserva | Unidade parametrizado + BDD | Fronteiras: termina no início, começa no fim, cruza 1 segundo |
| RN-05 máquina de estados | Unidade parametrizado + BDD | Tabela de transições permitidas e proibidas |
| RN-06 só Responsável aprova restrito; ninguém aprova a própria | BDD + API | 403 e reserva continua SOLICITADA |
| RN-07 reserva iniciada não se apaga | Unidade + API | |
| RN-08..12 auditoria | Unidade + integração com rollback | Sem auditoria órfã; 403 do Solicitante no histórico |
| RN-13 relatório só Responsável e Administrador | API + matriz de autorização | 401 sem token, 403 Solicitante |
| RNF-01..03 | JMeter, JaCoCo, teste de criação sob carga | p95 e taxa de erro a medir |

## 5. Metas e critérios
| Meta | Valor | Como se mede | Estado atual |
|---|---|---|---|
| Cobertura de linhas | ≥ 80% | JaCoCo no `verify` (o build falha abaixo) | gate atendido no `verify` de 05/11/2026; valor exato **não anotado aqui** |
| Cobertura de branches | ≥ 70% | JaCoCo no `verify` | idem |
| Requisitos críticos na RTM com teste e evidência | 100% | `docs/rtm.md` | **não medido** (RTM ainda inicial) |
| Bugs ou vulnerabilidades críticas no SonarCloud | 0 | Quality Gate e painel do SonarCloud | Quality Gate passou no PR #4; "Dependency risks" ainda **não** analisados |
| Testes ignorados | 0 | `Skipped: 0` no `verify` | 0 em 30 testes (`./mvnw -B verify` local em 05/11/2026) |
| Desempenho (proposta) | p95 ≤ 300 ms com 50 usuários; erro < 1% | `.jtl` do JMeter | **não medido** |

- **Teste trivial só para subir cobertura não conta.** Teste que continua verde com a regra quebrada é lacuna (mutação manual).
- **Critério de entrada de um PR:** `./mvnw -B verify` verde localmente. **Critério de saída:** CI verde (`verify` e SonarCloud), revisão de outra pessoa, RTM e `docs/status-requisitos.md` atualizados.
- **"Feito" em `docs/status-requisitos.md`** só vale quando existe teste (arquivo e nome) que falharia se a regra quebrasse.

## 6. Execução e automação
- **Local:** `./mvnw -B verify` (PowerShell: `.\mvnw.cmd -B verify`) com o Docker aberto. Relatório de cobertura: `target/site/jacoco/index.html`.
- **CI:** GitHub Actions em todo pull request e em push na `main`; relatórios do Surefire e do JaCoCo como artifact; SonarCloud lê o XML do JaCoCo.
- **Desempenho:** só em modo CLI contra a aplicação local, com RNF escrito **antes** da primeira rodada (`docs/performance/rnf.md`); o `.jtl` bruto é versionado para conferência.

## 7. Gestão de defeitos
Defeito achado vira `docs/defeitos/DEF-xxx.md` (modelo em `docs/defeitos/TEMPLATE.md`) **antes** da correção. O commit de correção cita o defeito; a regressão ganha teste que falhava antes (`test(regressao): <id>`, `fix(<id>): …`). Severidades: crítica, alta, média, baixa; o que mexe nos quatro limitadores é crítica.

## 8. Relatório de testes
O relatório final é `docs/relatorio-qualidade.md` (Etapa 6): resultados comparados às metas acima, defeitos por severidade, riscos residuais e recomendação, cada afirmação com **evidência, interpretação e limite**. Até lá, a evidência corrente é o artifact `relatorios` do CI.

## 9. Pendências deste documento
- [ ] Pedro e Leandro validarem os níveis e a divisão por área.
- [ ] Criar `docs/testes/plano-tdd.md` com o inventário completo de casos (Etapa 2) e ligar cada linha da seção 4 a ele.
- [ ] Anotar o valor exato de cobertura (JaCoCo) quando houver código de regra de negócio.
- [ ] Ligar este plano à RTM e ao relatório de qualidade.
