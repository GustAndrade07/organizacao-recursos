# Status em 11/11/2026 (quarta, branch `feat/backend-camadas`, Pedro)

Back-end em camadas (`config`, `controller`, `converter`, `dto`, `enums`, `exception`, `model`, `repository`, `service`) sobre a V1, sem migração nova, como **API REST sob `/api/**` ao lado das telas Thymeleaf** (ADR-0008, proposto; não substitui o ADR-0003). Login JWT (ADR-0001), usuários, recursos, disponibilidade, manutenção, reservas com `23P01` traduzido em 409 e a tentativa recusada em transação própria (D2), máquina de estados (D3), auditoria na mesma transação (RN-12), histórico e relatório carregando o SQL de `db/consultas`.

## O que mudou depois da revisão do PR #44 (Leandro e Gustavo)
- **Telas de volta**, idênticas à `main`: pacote `web`, `templates`, `static`, testes de tela e E2E. RF-13 continua com o Leandro.
- **API sob `/api/**`**, com controllers `*ApiController` (sem colisão de rota nem de nome de bean com as telas).
- **Duas cadeias de segurança** (`SegurancaConfig`): `/api/**` stateless com JWT e `denyAll` para rota fora da matriz; telas abertas como estavam, sem login e com o CSRF padrão ligado, até a D12.
- Tratador de erros (`ProblemDetail`) restrito ao pacote `controller`; as telas mantêm as páginas de erro do Thymeleaf.
- Matriz de autorização com uma tabela da API e outra das telas; `MatrizAutorizacaoTest` confere as duas e falha com rota fora da matriz.
- Testes de tela sem alteração: `META-INF/spring/...AutoConfigureWebMvc.imports` (segurança real no recorte) e `support/RecorteDasTelasSemApi` (recorte sem os controllers da API). Única mudança num teste do Leandro: `PaginaNaoEncontradaIntegracaoTest` sobe com Testcontainers, porque a aplicação inteira agora precisa de banco.
- Seção 9 do plano: a D9 do Leandro fica como estava; as perguntas do back-end viram D10 (estado inicial e limites de perfil) e D11 (manutenção sobre reservas existentes); novas D12 (autenticação das telas) e D13 (RN-04 "consulta e depois grava", apontada pelo Gustavo). Todas pendentes.
- `sonar.coverage.exclusions` igual à `main` (inclui `static/js`); este arquivo regravado em UTF-8 sem BOM.
- Quality Gate do SonarCloud vermelho no primeiro run (Reliability D, Security E, 7 issues): corrigidos o hash falso do login com texto fixo (S6437, agora aleatório), `SecureRandom` recriado (S2119), método `final` em entidade (S8947), duas asserções de `ReservaRecursoIdTest` (S5863, S5845) e o CSRF das telas (S4502, agora ligado; o `ReservaE2ETest` passa a conferir o `_csrf` no corpo). O CSRF desligado da API (S4502) fica com `NOSONAR` e justificativa escrita na matriz de autorização.

## Verificado
Nesta máquina (Windows, Docker Desktop, Testcontainers com `postgres:16`), depois do merge de `origin/main` e das correções do SonarCloud: `.\mvnw.cmd -B clean verify` com **965 testes, 0 falhas, 0 erros, 0 ignorados**, `BUILD SUCCESS` e o gate do JaCoCo atendido; cobertura de **97,9% das linhas e 89,2% dos branches** (somando `target/site/jacoco/jacoco.csv`).

## Não verificado
- Run do CI e Quality Gate do SonarCloud deste PR.
- Quality Gate do SonarCloud depois das correções abaixo (só confiro quando o CI rodar).
- Spec do Back-end no Notion: não atualizada por este PR (a fazer pelo Pedro em até 24 h).
- Telas ligadas à API: o contrato das telas (`docs/frontend/README.md`) ainda não bate com as rotas `/api/**` (ADR-0008); depende da D12.

## Caixas do plano
- Back-end e Spring Security marcados como feitos na seção correspondente, com D10 a D13 pendentes.
