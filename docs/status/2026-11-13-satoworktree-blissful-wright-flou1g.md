# Status — 13/11/2026 — `satoworktree/blissful-wright-flou1g`

**Quem:** pendências do **Leandro** da análise do Gustavo depois dos PRs #59 a #62 (feito com IA, a pedido do Leandro). Só documentação e as specs do Notion; nenhum código, teste ou configuração mudou.

## O que mudou
- `docs/decisao-qualidade.md`: condições da seção 1 com a situação de 13/11 (clone limpo **parcial**: rodada da IA feita, humana pendente; CI verde até `bc40183`); RF-14 deixa de ser ausente, então o risco 5 trata só o RF-11; o risco 11 vira "lacunas de teste que restam" (BDD e mutação das seis regras já estão feitos); contagem de PRs do Leandro de 23 para 27 (#5 a #62), conferida pela lista do GitHub; commits com autor "Claude" de 7 para 8 (o oitavo é o merge de 09/11 do PR #32).
- `docs/relatorio-qualidade.md`: RF-14, BDD e mutação do serviço passam a existir; RTM de 13/11 (10 RF feitos, 3 parciais, RF-11 ausente, 13 RN feitas); 1138 testes depois do #61; clone limpo da IA nas seções 4, 6 e 7; recomendação aponta para a decisão de qualidade já mergeada.
- `docs/plano-execucao.md`: caixas marcadas com a evidência de cada uma (D1, Testcontainers, auditoria, relatório, matriz). As caixas **TDD red** e **TDD green** continuam abertas, de propósito (ver abaixo).
- `docs/entrega/checklist.md`: linhas que dependiam dos PRs #56 a #59 relidas na árvore de `bc40183` e marcadas como "lido na árvore, não em clone"; `bc40183` é provisório até a tag.
- `docs/prova-oral/roteiro-demo.md`: comando do passo 2 para o PowerShell; aviso de que `demo.sh` é de bash; texto sobre quem fala cada parte corrigido (a tabela segue as áreas, não inverte).
- `docs/prova-oral/perguntas.md`: pergunta 4 com "Pedro" só, igual à tabela de distribuição.
- `docs/status-requisitos.md`: linhas 1, 2, 4 e 12 da seção "Pendências e divisão" (clone, decisão, PR #56, perguntas).
- Notion: spec principal, índice do Front-end e specs 02 a 05 (ver "Specs atualizadas" na descrição do PR).

## Por que TDD red e green continuam abertas
Gustavo listou as duas entre as caixas já atendidas. `docs/testes/evidencia-tdd.md` (PR #61) mostra 4 ciclos `test(red)` → `feat(green)` e diz que as telas 7 e 8, a API em camadas e os cenários BDD **não** nasceram de TDD. A caixa diz "para cada regra do plano", então marcá-la seria afirmar o que a evidência nega. O grupo pode marcar assim mesmo, mas aí a ressalva precisa ficar escrita.

## Verificado
- Contagem de PRs: lista de PRs fechados da API do GitHub (59 mergeados: Leandro 27, Pedro 2, Gustavo 30; #3, #26 e #29 fechados sem merge).
- `ci.yml` verde na `main`: run 251 (id 43465315272, `bc40183`) e run 248 (id 43458709849, #61), pela API do GitHub.
- Autor "Claude": `git log origin/main --author=Claude` = 8 commits.
- Nomes de classe e de teste citados nas caixas existem no repositório (`git ls-files`, `grep`).
- 1138 = 1116 + 13 + 5 + 4, cada parcela em seu arquivo de status do #61.

## Não verificado
- `./mvnw -B verify`: não rodado nesta sessão (sem Docker no container). Este PR não muda código nem teste.
- Cobertura, Quality Gate do SonarCloud e painel de dependências **depois** do #61: não relidos. Seguem os de `6ce4478`.
- Contagem de testes por nível: não refeita (só o total).
- Comando do PowerShell do roteiro: não rodado em Windows.
- Notion: conteúdo relido só pelo retorno das edições, sem nova leitura completa das páginas.
- Pendências humanas que **não** foram feitas: ensaio cronometrado, validar o p95 ≤ 300 ms com o professor, a declaração do Leandro em `decisao-qualidade.md`, aceite por escrito do limite de login em memória.
