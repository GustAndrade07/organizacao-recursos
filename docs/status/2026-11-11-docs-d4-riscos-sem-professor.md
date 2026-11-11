# Status em 11/11/2026 (quarta, noite, branch `docs/d4-riscos-sem-professor`)

Só documentação, a partir da `main` (`8b18dd9`). No SonarCloud, logado, o Gustavo marcou como **Accepted** os 2 riscos Critical do `spring-webmvc` 6.2.19 (CVE-2026-47884 e CVE-2026-47890). Antes, conferiu no painel que a descrição de cada CVE é a da seção 3 de `docs/seguranca/riscos-dependencias.md`. O painel da `main` passou de 21 riscos com 2 Critical para 19 abertos, 0 Critical e 1 High (`jaxb-core` 4.0.9, CVSS 8,3). O Quality Gate está OK, com 0 vulnerabilidades, Security A e 100% dos hotspots revisados (API pública, 11/11/2026). Isso foi registrado na seção 6 nova do documento de riscos e na linha da D4 (seção 9 do plano).

A D4 continua sem resposta do professor. O tratamento de 05/11 continua valendo, com a leitura mais rígida: as duas medições vão no relatório, os 2 aceitos entram como risco residual em `docs/decisao-qualidade.md` (arquivo do Leandro) e a pergunta é feita na Aula 14.

**Não verificado:** o painel pode mudar com a próxima análise da `main`. O `jaxb-core` ainda não foi avaliado. A suíte não rodou nesta máquina, porque o Docker Desktop está parado; a mudança é só de documentação. Nenhuma caixa da etapa foi marcada.
