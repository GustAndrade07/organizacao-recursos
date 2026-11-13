# Resultado da carga — rodadas 01 e 02 (12/11/2026)

Metas e cenário fixados **antes** da primeira rodada em [`rnf.md`](rnf.md), no commit `0464847`. Plano: `perf/reservas.jmx`. Usuários fictícios: `perf/usuarios.csv`. Resultado bruto: `perf/resultados/rodada-01.jtl` e `perf/resultados/rodada-02.jtl`. Os números abaixo foram calculados desses dois arquivos.

## Conclusão

| Meta | Rodada 01 | Rodada 02 | Situação |
|---|---|---|---|
| RNF-01: busca com p95 ≤ 300 ms com 50 usuários | p95 = **17 ms** (no máximo 48 usuários ativos ao mesmo tempo) | p95 = **8 ms** (50 ativos ao mesmo tempo; 8 ms também só nas 1877 buscas feitas com os 50 ativos) | **Cumprida nesta máquina**, com folga |
| RNF-03: criação sob disputa só 201 ou 409 | 20 × 201 e 980 × 409; nenhum outro código | 60 × 201 e 2940 × 409; nenhum outro código | **Cumprida nesta máquina** |

Nenhum erro de verdade nas duas rodadas: nenhum 5xx, timeout ou conexão recusada (`success=true` em todas as 8204 amostras).

## Números por requisição

Percentis pelo método do posto mais próximo (*nearest rank*), sobre o `elapsed` do `.jtl`, em ms. O relatório HTML do JMeter interpola e pode diferir em até 1 ms (ex.: p95 da criação na rodada 01, 36 aqui e 36,95 no `statistics.json`).

**Rodada 01** (cenário do `rnf.md`: 50 usuários, rampa de 10 s, 20 repetições; 20:57:46 a 20:58:10 UTC):

| Requisição | Amostras | p50 | p90 | p95 | p99 | Máx. | Média | Códigos |
|---|---|---|---|---|---|---|---|---|
| Busca disponibilidade | 1000 | 7 | 15 | **17** | 25 | 56 | 8,0 | 1000 × 200 |
| Criar reserva | 1000 | 17 | 32 | 36 | 50 | 142 | 19,9 | 20 × 201, 980 × 409 |
| Login (fora da meta) | 50 | 95 | 142 | 171 | 199 | 199 | 104,2 | 50 × 200 |

Vazão: 110,4 requisições/s no total e 56,2 buscas/s, numa janela de 18,6 s.

**Rodada 02** (mesmo cenário, com 60 repetições; 20:58:40 a 20:59:16 UTC):

| Requisição | Amostras | p50 | p90 | p95 | p99 | Máx. | Média | Códigos |
|---|---|---|---|---|---|---|---|---|
| Busca disponibilidade | 3000 | 3 | 6 | **8** | 13 | 47 | 3,7 | 3000 × 200 |
| Criar reserva | 3000 | 8 | 15 | 18 | 26 | 50 | 9,6 | 60 × 201, 2940 × 409 |
| Login (fora da meta) | 50 | 83 | 98 | 108 | 130 | 130 | 85,4 | 50 × 200 |

Vazão: 172,6 requisições/s no total e 86,9 buscas/s, numa janela de 35,1 s.

**O 409 não é erro.** Em cada repetição, os 50 usuários pedem a mesma sala no mesmo horário: 1 recebe 201 e 49 recebem 409. O banco confere depois das duas rodadas:
- 80 reservas (20 + 60), uma por horário de cada sala (`reserva_recurso`: 20 linhas na sala da rodada 01 e 60 na da rodada 02);
- 3920 tentativas recusadas (980 + 2940) em `tentativa_recusada`;
- 50 solicitantes distintos entre reservas e tentativas na rodada 01, ou seja, cada usuário virtual entrou com um login diferente do CSV.

## Por que houve uma rodada 02
- Na rodada 01, o maior número de usuários ativos ao mesmo tempo foi **48** (coluna `allThreads` do `.jtl`). Os primeiros terminaram as 20 repetições antes de os últimos subirem.
- Como a meta fala em 50 usuários, a rodada 02 repetiu o cenário com 60 repetições para os 50 ficarem ativos juntos.
- A meta não mudou. O motivo da segunda rodada foi a concorrência, não o tempo de resposta: a rodada 01 já cumpria a meta.
- A rodada 02 rodou na mesma JVM, logo depois da 01, com o código já aquecido (JIT). Isso explica, ao menos em parte, os tempos menores. Não foi medido quanto.

## Onde rodou (e o que isso limita)
- **Máquina:** container Linux da sessão de nuvem, 4 vCPUs (Intel Xeon @ 2,10 GHz) e 15 GiB de memória. O JMeter, a aplicação e o PostgreSQL rodaram na mesma máquina.
- **Aplicação:** o jar do `./mvnw -B verify` (`target/organizacao-recursos-0.0.1-SNAPSHOT.jar`, commit `0464847`), com `java -jar`, Java 21.0.12.1.
- **Banco:** PostgreSQL 16.15 do `docker-compose.yml`, apagado antes da rodada 01 (`docker compose down -v`). Começou só com o Administrador da carga.
- **JMeter:** 5.6.3, em modo CLI (`-n`). Conferido pelo SHA-512 publicado pela Apache.
- **O Administrador da carga** foi criado no start por `ADMIN_LOGIN` e `ADMIN_SENHA`, com uma senha aleatória gerada só para a rodada e passada ao JMeter por `-J`. A senha não está no repositório nem no `.jtl` (conferido com `grep`). Os tokens também não aparecem no `.jtl`.
- **Limites:**
  - Os números valem para esta máquina e este cenário. **Não** dizem nada sobre produção, outra máquina, mais usuários ou dados maiores: o banco tinha só as salas da carga.
  - São duas rodadas, não uma série. Variação entre rodadas: **não medida**.
  - A busca consultou uma janela de uma hora com poucas salas. Com muitos recursos, o tempo: **não medido**.
  - O login (BCrypt) é a requisição mais lenta, mas não tem meta.

## Ensaio do plano (antes da rodada 01, fora das métricas)
- O plano foi validado em ensaios pequenos (de 1 a 3 usuários, 1 ou 2 repetições), com os resultados guardados fora do repositório. Foi o que achou dois defeitos do próprio plano, corrigidos antes da rodada 01:
  - o cabeçalho `Authorization: Bearer ${token}` ia também no login, quando o token ainda não existia, e a API respondia 401;
  - os parâmetros da busca saíam sem o `=` (`?inicio2027-...`), e a API respondia 400.
- Os dados dos ensaios foram apagados com o banco antes da rodada 01.

## Como refazer
```bash
docker compose down -v
```
```bash
docker compose up -d
```
```bash
./mvnw -B verify
```
```bash
ADMIN_LOGIN=carga.admin@exemplo.com ADMIN_SENHA=<senha-so-desta-rodada> java -jar target/organizacao-recursos-0.0.1-SNAPSHOT.jar
```
```bash
jmeter -n -t perf/reservas.jmx -Jadmin_login=carga.admin@exemplo.com -Jadmin_senha=<a-mesma> -l perf/resultados/rodada-01.jtl -e -o perf/relatorio-01
```
Para a rodada 02, acrescentar `-Jrepeticoes=60` e trocar `01` por `02` nos caminhos.

Para repetir, use o `perf/usuarios.csv` novo (logins em formato de e-mail). As rodadas 01 e 02 foram feitas antes da troca do login para e-mail (13/11/2026), com `carga.admin` e `carga.usuarioNN`; os números acima valem para aquela medição e continuam como foram medidos.
