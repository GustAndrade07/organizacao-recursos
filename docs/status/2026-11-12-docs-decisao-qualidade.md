# Status em 12/11/2026, noite (branch `docs/decisao-qualidade`, Leandro)

**Lacuna 2 do plano (Aula 14):** `docs/decisao-qualidade.md`, que bloqueava a tag. Só documentação.

**Decisão:** liberar `v1.0.0-rc1` com ressalvas, **condicionada ao clone limpo**. Se o clone limpo não passar, bloquear. Também bloqueia se o CI ficar vermelho no commit da tag; e a decisão é reavaliada se a resposta do professor à D4 reprovar risco aceito com justificativa. É a recomendação do relatório de qualidade, versão 2 (PR #57), confirmada pelo Leandro.

**As cinco partes pedidas pelo plano:**
1. A decisão e as condições.
2. Os critérios com evidência: cobertura, críticos, requisitos com teste, vulnerabilidades, os quatro limitadores, RNF-01 e RNF-03 e os defeitos.
3. 11 riscos aceitos, com impacto, motivo e responsável: os 6 de dependências (2 Critical do Spring, `jaxb-core` e os 3 do Spring Security, com o CVE-2026-59276 "não verificado"), a D13, os RF ausentes e parciais, as decisões pendentes, o login em memória, o cookie `Secure`, o desempenho local, e o BDD e as mutações de serviço.
4. O que cada integrante fez, tirado da lista de PRs mergeados (Gustavo 30, Leandro 23, Pedro 2, até o PR #58) e das áreas do `AGENTS.md`; o uso de IA declarado (7 commits na `main` com o autor "Claude").
5. O limite do que foi provado.

**Falta:**
- as **declarações dos três integrantes**, que cada um escreve com as próprias palavras (seção 4);
- o clone limpo (lacuna 11);
- a conferência dos números no commit da tag.

**Também atualizados:**
- `docs/evidencias/README.md` e `docs/entrega/checklist.md` (situação da decisão);
- plano (lacuna 2);
- changelog em `changelog.d/docs-decisao-qualidade.md`.

**Depende dos PRs #56 e #57:** a decisão cita `docs/performance/` e o relatório versão 2. A ordem de merge é #56, #57 e depois este.

**Atualização em 13/11/2026, depois do merge do PR #58:** `main` (`c152d01`) juntada nesta branch, sem conflito. A decisão passou a dizer que D10 e D11 foram decididas (risco 7 e seção 5, com as lacunas de teste), que o ADR-0008 foi aceito e que os PRs #56, #57 e #58 estão mergeados (contagem da seção 4). A decisão não mudou.

**Verificado:**
- os números vêm do relatório versão 2 e da RTM (PR #57), com fonte;
- a contagem de PRs saiu da API do GitHub;
- os autores dos ADRs e dos commits saíram do `git log`;
- `./mvnw -B verify` do mesmo código (a `main` não mudou código desde `6ce4478`): 1116 testes, 0 falhas, 0 erros, 0 ignorados.

**Não verificado:** o clone limpo; a resposta da D4; o painel de dependências do SonarCloud (exige login).

**Specs atualizadas:** Notion, spec principal (critério de aceite da decisão de qualidade), 12/11/2026.
