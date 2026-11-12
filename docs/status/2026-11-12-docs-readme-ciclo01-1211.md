# Status em 12/11/2026 (quinta, branch `docs/readme-ciclo01-1211`)

Só documentação, a partir da `main` (`2610d6a`, CI verde no run 211). Resolve as lacunas 5 e 10 da conferência contra as aulas (seção 4.1 do plano, PR #53).

O que muda:
- **README, seção Stack:** saem WireMock, Cucumber e JMeter, que não estão no `pom.xml` nem têm resultado versionado. Entra uma linha dizendo que estão previstos e ainda não presentes.
- **`docs/avaliacao-arquitetural/ciclo-01.md`:**
  - Seção 8.1 nova com as medidas do JaCoCo e do SonarCloud no `2610d6a`: 95,7% das linhas, 80,3% dos branches, 0 bugs, 0 vulnerabilidades, 0 hotspots, duplicação de 0% e Quality Gate OK.
  - RK9 fechado.
  - Fonte 11 nova.

**Plano:** nenhuma caixa marcada. As lacunas 5 e 10 estão na tabela da seção 4.1, que entra pelo PR #53. Quem fizer o merge por último marca as duas como resolvidas.

**Não verificado:**
- A suíte não rodou: a mudança é só de texto.
- Os percentuais vêm do SonarCloud, que lê o `jacoco.xml` do run 211. O relatório do JaCoCo no artifact não foi aberto.
