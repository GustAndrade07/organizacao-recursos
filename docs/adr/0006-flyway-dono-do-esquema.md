# ADR-0006 — Migrações com Flyway como único dono do esquema
- **Status:** proposto (a regra já está em `AGENTS.md` e na configuração; vira "aceito" na revisão do PR)
- **Contexto:** o esquema do banco precisa ser **reproduzível** (clone limpo, CI, Testcontainers) e a proteção contra reserva dupla ([ADR-0002](0002-anti-reserva-dupla.md)) vive no banco, então qualquer divergência de esquema é risco para um dos quatro limitadores de nota. O esqueleto já traz o Flyway (`flyway-core` e `flyway-database-postgresql` no `pom.xml`).
- **Decisão:**
  1. O **Flyway é o único dono do esquema**: tabelas, índices e constraints só nascem de migrações em `src/main/resources/db/migration` (nome `V<n>__descricao.sql`). Hoje a pasta só tem `.gitkeep`: **ainda não existe migração**.
  2. `spring.jpa.hibernate.ddl-auto: validate` (já em `application.yml`): o Hibernate só confere o esquema, nunca altera. Entidade diferente do banco derruba a subida da aplicação.
  3. **Migração já mergeada na `main` não se edita.** Erro se corrige com a **próxima** versão. O Flyway guarda o checksum de cada migração aplicada e recusa uma migração editada num banco que já a aplicou.
  4. A migração que cria a constraint de exclusão cria antes a extensão `btree_gist` (`CREATE EXTENSION IF NOT EXISTS btree_gist`), necessária para combinar igualdade de recurso com sobreposição de `tstzrange` em `EXCLUDE USING gist`.
  5. Nenhum teste altera o esquema por fora; testes de persistência partem de PostgreSQL 16 limpo em Testcontainers, com as migrações aplicadas do zero.
- **Consequências:**
  - **Ganhamos:**
    - Esquema reproduzível: o mesmo resultado no clone limpo do professor, no CI e no teste.
    - Histórico de mudanças do banco revisável em PR, como código.
    - `validate` pega cedo a divergência entre entidade e tabela (o teste de fumaça já sobe o contexto completo).
  - **Perdemos:**
    - **Migração mergeada não se edita**: erro de V1 vira V2, e o histórico guarda o erro. Por isso a V1 só foi adiada até as decisões que mudam o esquema (D1, D2 e D5, abaixo) estarem tomadas.
    - Banco local que aplicou uma migração depois editada fica inconsistente e precisa de `docker compose down -v` (AGENTS.md, seção 8).
    - A extensão `btree_gist` precisa poder ser criada no banco de destino. A imagem `postgres:16` (a do `docker-compose.yml` e a do `AplicacaoSmokeTest`) a traz: conferido em 05/11/2026 num container descartável (PostgreSQL 16.15, `btree_gist` 1.7 disponível, `CREATE EXTENSION` e uma `EXCLUDE USING gist` criadas sem erro). Em outro ambiente (banco gerenciado, outra versão) isso **não foi conferido**.
    - Mais disciplina de PR: toda mudança de modelo exige migração, não só alteração de entidade.
  - **Exige de testes:**
    - Teste com Testcontainers que aplica **todas** as migrações num banco vazio e sobe o contexto com `ddl-auto: validate`.
    - Teste que confirma a extensão `btree_gist` instalada e a constraint de exclusão existente no catálogo do banco (ex.: `pg_extension`, `pg_constraint`), para o teste de concorrência não passar em banco sem proteção.
    - Revisão de PR (e, como proposta, uma checagem no CI que falhe se um arquivo já existente em `db/migration` for modificado em relação a `origin/main`; **não existe hoje**).
    - O `AplicacaoSmokeTest` continua cobrindo a subida; a troca de `disabledWithoutDocker` foi feita em 06/11/2026, junto com os testes de banco ([plano-de-teste.md](../plano-de-teste.md), seção 3).
- **Pendências:**
  - **D1 decidida** (Gustavo, 05/11/2026, seção 9 do plano): intervalo semiaberto `[início, fim)`; os limites do `tstzrange` na V1 seguem isso.
  - **D2 decidida** (idem): a tentativa recusada é gravada em transação própria, então a **tabela das tentativas recusadas** (base do "conflitos evitados", RF-12) entra na V1.
  - **D5 decidida** (idem): a constraint de exclusão é parcial, só sobre os estados que ocupam o horário ([ADR-0002](0002-anti-reserva-dupla.md)).
  - **D3** (409 para transição inválida) não muda o esquema; nada a fazer aqui.
  - A migração V1 foi escrita em 06/11/2026 (`V1__nucleo_da_reserva.sql`, PR `feat/banco-v1`), com as decisões de modelagem em [ADR-0007](0007-modelagem-protecao-reserva-dupla.md). O "Exige de testes" acima foi atendido por `MigracoesBancoTest` (migrações num banco vazio, contexto em `validate`, `btree_gist` e constraint no catálogo). Limite: sem entidades JPA ainda, o `validate` confere pouco; passa a valer quando o Pedro criar as entidades.
  - A checagem no CI contra edição de migração já mergeada continua **não existindo** (proposta; só com pedido do Gustavo).
- **Fonte:** `pom.xml`, `src/main/resources/application.yml`, `docker-compose.yml`, `AGENTS.md` (seções 5 e 8), `docs/arquitetura.md` (seções 8 e 9), `docs/plano-execucao.md` (seção 9).
