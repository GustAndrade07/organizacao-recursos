# Status em 12/11/2026 (quinta, branch `perf/jmeter-carga`, Leandro)

**Carga com JMeter (lacuna 1 do plano, Aula 10).** Bloqueava a tag.

**Ordem (a meta antes do número):**
1. `docs/performance/rnf.md` no commit `0464847`, antes de qualquer rodada. Meta escolhida pelo Leandro: p95 ≤ 300 ms com 50 usuários (RNF-01); criação só 201 ou 409 (RNF-03). O cenário também ficou fixado antes.
2. `perf/reservas.jmx` e `perf/usuarios.csv`. A preparação cria 50 Solicitantes fictícios e uma sala; o Administrador vem por `-J` e o token é extraído das respostas, sem token fixo no plano.
3. Ensaios pequenos para validar o plano, fora das métricas. Acharam dois defeitos do próprio plano (Bearer vazio no login dava 401; parâmetros da busca sem `=` davam 400), corrigidos antes da rodada 01.
4. Rodada 01 (cenário do `rnf.md`) e rodada 02 (mesmo cenário com 60 repetições, porque na 01 o pico foi de 48 usuários ativos, não 50).

**Resultado** (`docs/performance/resultado-01.md`, números tirados dos `.jtl`):

| Rodada | Busca p95 | Criação | Erros |
|---|---|---|---|
| 01 | 17 ms | 20 × 201, 980 × 409 | 0 |
| 02 (50 ativos juntos) | 8 ms | 60 × 201, 2940 × 409 | 0 |

Banco conferido: 80 reservas e 3920 tentativas recusadas.

**Onde:** container da sessão de nuvem, 4 vCPUs, com o JMeter 5.6.3, a aplicação (jar do `verify`) e o PostgreSQL 16.15 na mesma máquina. Os números não dizem nada sobre produção.

**Verificado:** `./mvnw -B verify` nesta máquina antes do primeiro commit: 1116 testes, 0 falhas, 0 erros, 0 ignorados. Nenhuma senha nem token no `.jmx` nem nos `.jtl` (conferido com `grep`).

**Não verificado:**
- variação entre rodadas (só duas);
- busca com muitos recursos no banco;
- o run do CI deste PR, porque o JMeter não roda no CI: o CI só confere que o `verify` continua verde;
- a meta foi escolhida pelo grupo e falta validar com o professor (ponto em aberto da spec principal).

**Documentos:**
- RTM: RNF-01 e RNF-03 viram "feito (local)", e o resumo foi recontado.
- Plano: as duas caixas da Aula 10 marcadas e a lacuna 1 resolvida.
- `perf/README.md`: propriedades e preparação.
- Changelog em `changelog.d/perf-jmeter-carga.md`.

**Specs atualizadas:** Notion, spec principal (KPIs, tabela de RNF, critério de aceite do JMeter e risco das lacunas), 12/11/2026.
