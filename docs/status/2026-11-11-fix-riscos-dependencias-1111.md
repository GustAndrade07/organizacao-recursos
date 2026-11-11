# Status em 11/11/2026 (quarta, branch `fix/riscos-dependencias-1111`)

`jsoup` (teste) de 1.22.2 para 1.24.1; Spring Framework conferido de novo no Maven Central (linha 6.2 segue em 6.2.19, sem correção), então os 2 críticos do `spring-webmvc` continuam aceitos com justificativa em `docs/seguranca/riscos-dependencias.md` (seção 4 nova). `verify` com 510 testes, 0 ignorados, só nesta máquina. **Não verificado:** a reanálise do painel "Dependency risks" (exige login) e a marcação "Accepted" no SonarCloud. Nenhuma caixa marcada.
