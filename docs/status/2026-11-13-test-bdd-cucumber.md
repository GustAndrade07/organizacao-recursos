# Status — 13/11/2026 — `test/bdd-cucumber`

**Quem:** feito pelo Leandro (com IA) na área do Pedro (back-end e testes), com o OK do Leandro; **Pedro revisa**. Mesmo combinado do PR #52.

> Este arquivo de status é de uma das partes do PR `test/qualidade-testes-e-openapi-1311`, que junta cinco assuntos da área do Pedro num PR só (combinado com o Leandro em 13/11/2026, para não serem cinco revisões no dia da entrega). O `verify` da ponta do PR tem **1138 testes, 0 falhas, 0 erros, 0 ignorados**.

**Lacuna fechada:** a 4 da tabela "Lacunas para a entrega" (BDD executável e evidência de TDD).

## O que mudou
- `pom.xml`: Cucumber 7.23.0 (`cucumber-java`, `cucumber-spring`, `cucumber-junit-platform-engine`), `junit-platform-suite` e o `cucumber-bom` em `dependencyManagement`.
- `src/test/resources/features/aprovacao-bloqueio.feature`: os cenários de `docs/testes/bdd-aprovacao-bloqueio.md` em Gherkin português.
- `src/test/java/br/com/senac/recursos/bdd/`: passos (`AprovacaoBloqueioSteps`), contexto (`ConfiguracaoDoCucumber`) e a entrada do Surefire (`AprovacaoBloqueioTest`).
- `docs/testes/evidencia-tdd.md` (novo), `docs/testes/bdd-aprovacao-bloqueio.md` (aponta para o `.feature`, D1 deixa de ser pergunta em aberto), `README.md` (Cucumber sai da lista de "ainda não presentes"), `docs/plano-execucao.md`.

## Decisões técnicas e por quê
- **Cucumber 7.23.0, não 7.34.9.** Da 7.26 em diante o `cucumber-junit-platform-engine` exige JUnit Platform 1.13 (`DiscoveryIssueReporter`); com o JUnit 5.12.2 que o Spring Boot 3.5.16 gerencia, a descoberta falha com `NoClassDefFoundError` e nenhum cenário roda. Medido nesta sessão (log da tentativa com a 7.34.9). Subir o Cucumber exige subir o JUnit junto — não no dia da entrega.
- **`cucumber-bom` importado** porque as dependências internas do Cucumber vêm por faixa de versão (`[29.0.1,31.0.0)`), o que faz o build consultar o repositório a cada resolução.
- **Contexto próprio, não o `ApiIntegracaoTest`.** O Cucumber exige uma classe `@CucumberContextConfiguration`; ela sobe o seu próprio PostgreSQL em Testcontainers e o mesmo `RelogioDeTeste`. Sem H2 e sem `Thread.sleep` (AGENTS.md, seção 5).
- **Os passos conferem no banco.** Estado da reserva e contagens saem do `jdbc`; só status HTTP e `type` do `ProblemDetail` saem da resposta.

## Verificado
- `./mvnw -B test -Dtest=AprovacaoBloqueioTest`: **13 testes, 0 falhas, 0 erros, 0 ignorados**, 15,12 s (13/11/2026, container Linux desta sessão, OpenJDK 21.0.12.1, Docker 29.9.2).
- `./mvnw -B verify` completo nesta branch: **1129 testes, 0 falhas, 0 erros, 0 ignorados**, `BUILD SUCCESS`, "All coverage checks have been met" (1116 na `main` + 13 cenários).
- Que os cenários **pegam a regra quebrada**: as mutações MA1, MM1 e MM3 (`docs/testes/mutacao-manual.md`, PR irmão desta sessão) deixam cenários deste `.feature` vermelhos.

## Não verificado
- Run do CI (só depois do push).
- Quanto o Cucumber aumenta o tempo do `verify` no CI (local: +15 s mais a subida de um contêiner a mais).
- **RTM e `status-requisitos.md` não foram tocados de propósito:** o PR #57 reescreve os dois e qualquer edição aqui daria conflito. Depois do merge do #57, as linhas RF-07, RF-08, RN-04, RN-05 e RN-06 ganham o `.feature` como evidência; está anotado na descrição do PR.
