# Status em 11/11/2026 (quarta, noite, branch `fix/jaxb-core`)

Só documentação, a partir da `main` (`19f5e31`). O `jaxb-core` 4.0.9 (CVE-2026-92877, XXE, CVSS 8,3) era o único risco High aberto no painel "Dependency risks". Foi avaliado e ficou **aceito com justificativa**, na seção 7 nova de `docs/seguranca/riscos-dependencias.md`. Os motivos:
- Não há versão corrigida: a 4.0.9 é a última no Maven Central.
- A biblioteca entra só pelo `hibernate-core`.
- O projeto não desserializa XML: 0 ocorrências de JAXB ou parser XML em `src/` e no `pom.xml`, e nenhum `.xml` em resources.

O plano (Etapa 4, caixa do SonarCloud) passa a citar a avaliação. A caixa continua aberta.

**Verificado:**
- Descrição do CVE e ausência de versão corrigida, lidas no painel do SonarCloud (logado).
- `maven-metadata.xml` do Maven Central.
- `dependency:tree` e `git grep`, nesta máquina.

**Não verificado:**
- A marcação Accepted no painel, que é manual do Gustavo.
- A exclusão do `jaxb-runtime`, que não foi testada.
- Os 3 riscos novos do Spring Security 6.5.11, ainda sem nota, que não foram avaliados.

A suíte não rodou, porque a mudança é só de documentação. Nenhuma caixa foi marcada.
