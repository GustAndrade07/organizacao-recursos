# Evidência de TDD e de BDD executável

> O que este arquivo prova: quais regras nasceram pelo ciclo **red → green → refactor**, com o commit de cada etapa, e quais **não** nasceram assim. Nada aqui é afirmado sem o hash do commit, que qualquer pessoa confere com `git show <hash>`.
>
> Escrito em 13/11/2026, sobre a `main` no commit `9acbbcd`. Atualizar quando entrar um ciclo novo.

## 1. O que conta como evidência

A regra 11 do `AGENTS.md` permite **um** commit com o `verify` vermelho: o `test(red)` do TDD, e só com o OK do dono da área e os números do vermelho escritos no corpo do commit. Então a evidência de TDD aqui é literal:

1. um commit `test(red): ...` que **não** compila ou falha, com o resultado vermelho no corpo;
2. o commit `feat(green): ...` seguinte, que faz passar;
3. quando houve, o `refactor: ...` que limpa sem mudar comportamento.

O merge é sempre pelo método **Merge** (`AGENTS.md`, seção 3), justamente para esses três commits continuarem visíveis no histórico.

## 2. Ciclos completos (red → green)

| # | Regra | `test(red)` | `feat(green)` | O que o vermelho mostrava |
|---|---|---|---|---|
| 1 | Núcleo da reserva no banco: constraint de exclusão, fronteiras `[início, fim)` (D1), estados que ocupam (D5), tentativa recusada em transação própria (D2), concorrência | `7314a43` (06/11/2026, Gustavo) | `30013b1` (06/11/2026) | os testes de banco existiam antes da migração `V1__nucleo_da_reserva.sql`; sem a tabela, o contexto nem subia |
| 2 | Consultas do relatório de utilização (RF-12): horas por recurso, carga por professor, conflitos evitados | `5c92499` (07/11/2026, Gustavo) | `fa1c910` (07/11/2026) | `RelatorioUtilizacaoBancoTest` com os valores calculados à mão, antes dos arquivos `.sql` |
| 3 | Limite de tentativas no login **das telas** (ressalva 1 do PR #48, D12) | `cd14f8c` (12/11/2026) | `e86c777` (12/11/2026) | `LimiteDeTentativasDoLoginIntegracaoTest` com servidor real: 5 falhas em 15 min bloqueiam, relógio controlado, sem `Thread.sleep` |
| 4 | Limite de tentativas também no login **da API**, com o mesmo contador | `f0777de` (12/11/2026) | `5c13534` (12/11/2026) | `LimiteDeTentativasApiTest`: o bloqueio das telas ainda não valia para `POST /api/auth/login` |

O corpo de cada `test(red)` traz os números do vermelho e, nos ciclos 3 e 4, o **OK explícito do dono da área** exigido pela regra 11. Confira com `git show cd14f8c -s`.

### Ciclo com `refactor`

O único `refactor` isolado do histórico é o `9fe367c` (06/11/2026, Leandro): o aviso de confirmação das telas virou `fragments/confirmacao.html`, sem mudar comportamento — a suíte continuou verde antes e depois.

## 3. Ciclo de regressão (defeito → teste → correção)

A mesma ordem vale para defeito, com `test(regressao)` no lugar de `test(red)` (`AGENTS.md`, seção 3):

| Defeito | Teste de regressão | Correção |
|---|---|---|
| [DEF-001](../defeitos/DEF-001.md): páginas de erro sem `<title>` deixaram o Quality Gate da `main` em Reliability C | `7b62b0b` — `TemplatesComTituloTest` varre **todos** os templates, sem lista manual | `93f0a1c` |
| [DEF-002](../defeitos/DEF-002.md): teste de tela comparava a reserva de exemplo com o relógio real | relógio fixo em `support/TelasNoRecorte` | `cc363a4` |

## 4. O que **não** nasceu de TDD (dito aqui para não parecer que nasceu)

- **As telas Thymeleaf (RF-13, telas 7 e 8)**: escritas antes dos testes; registrado desde 11/11/2026 no `docs/relatorio-qualidade.md` (commit `93f5de6`).
- **A API em camadas (PR #44)**: controllers, serviços e testes de API entraram no mesmo commit, sem par red/green.
- **Os cenários BDD deste PR** (seção 5): foram escritos **depois** do código que eles descrevem. Eles provam que a regra escrita no documento é a regra que roda; não provam TDD. Dizer o contrário seria evidência falsa.

## 5. BDD executável

Os cenários de [`bdd-aprovacao-bloqueio.md`](bdd-aprovacao-bloqueio.md) deixaram de ser só texto: estão em `src/test/resources/features/aprovacao-bloqueio.feature` (Gherkin em português) e rodam no `verify`.

| Peça | Arquivo |
|---|---|
| Cenários | `src/test/resources/features/aprovacao-bloqueio.feature` |
| Passos (falam com a aplicação pela API) | `src/test/java/br/com/senac/recursos/bdd/AprovacaoBloqueioSteps.java` |
| Contexto (aplicação real + PostgreSQL em Testcontainers + relógio fixo) | `src/test/java/br/com/senac/recursos/bdd/ConfiguracaoDoCucumber.java` |
| Entrada pelo Surefire | `src/test/java/br/com/senac/recursos/bdd/AprovacaoBloqueioTest.java` |

**Como os passos conferem o resultado:** estado da reserva e contagens são lidos **no banco** (`jdbc`), não na resposta; o status HTTP e o `type` do `ProblemDetail` vêm da resposta. Nada de H2 e nada de `Thread.sleep`: o tempo é o `RelogioDeTeste`.

**Cenários que rodam** (13 no total, contando cada linha do Esquema do Cenário):

| Cenário | Regra |
|---|---|
| Recurso restrito exige aprovação de um Responsável | RF-07 |
| Solicitante não aprova a própria reserva | RN-06 |
| Responsável não aprova a própria reserva | RN-06 |
| Rejeição libera o horário do recurso restrito | RF-07 |
| Fronteiras da manutenção das 10:00 às 12:00 (5 linhas: −1 s, 0 e +1 s de cada lado, e dentro) | RN-04, RF-08, D1 |
| Recusa por manutenção não é conflito de horário (tipo próprio, sem tentativa gravada) | RN-04, D2 |
| Retirada e devolução levam a reserva até CONCLUIDA | RF-09, RN-05 |
| Transição inválida é recusada e não muda o estado nem o histórico | D3, RN-08 |
| Não comparecimento libera o horário | RN-05, D5 |

**Resultado medido** (13/11/2026, container Linux desta sessão, OpenJDK 21.0.12.1, Docker 29.9.2):

```
./mvnw -B test -Dtest=AprovacaoBloqueioTest
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 15.12 s
```

No `verify` inteiro desta branch, na mesma máquina: **1129 testes, 0 falhas, 0 erros, 0 ignorados**, com o gate do JaCoCo atendido (eram 1116 na `main`: 13 cenários BDD a mais).

**Limite desta evidência:** medido só nesta máquina. O que vale para a entrega é o run do CI no commit da tag.

## 6. Versão do Cucumber (por que não é a última)

O projeto usa **Cucumber 7.23.0**, não a 7.34. Da 7.26 em diante o `cucumber-junit-platform-engine` é construído sobre o JUnit Platform 1.13 e usa a classe `DiscoveryIssueReporter`, que não existe no JUnit 5.12.2 gerenciado pelo Spring Boot 3.5.16; com a 7.34.9 a descoberta falha com `NoClassDefFoundError` e o build quebra antes de rodar qualquer cenário (medido nesta sessão). Subir o Cucumber exige subir o JUnit junto, o que não se faz no dia da entrega. O `pom.xml` importa o `cucumber-bom` para fixar as versões internas (`messages`, `gherkin`, `tag-expressions`), que por padrão vêm por faixa de versão.
