# perf/ — teste de carga (JMeter)

Estrutura pedida na Aula 10 (carga e concorrência no projeto):

| Caminho | O que é | Versionar? |
|---|---|---|
| `perf/reservas.jmx` | Plano JMeter: Thread Group, login com JSON Extractor (token extraído, **sem token fixo**), consulta de disponibilidade e criação de reserva disputando o mesmo recurso. Asserção: criação responde só 201 ou 409, nunca 500 | sim |
| `perf/usuarios.csv` | CSV Data Set com usuários **fictícios** | sim |
| `perf/resultados/rodada-01.jtl` | Resultado bruto da rodada (o professor abre este arquivo junto com o grupo) | sim |
| `perf/relatorio-01/` | Relatório HTML gerado pelo JMeter | **não** (está no `.gitignore`) |

Os documentos da análise ficam em `docs/performance/`:
`rnf.md` (escrito **antes** da primeira rodada) e `resultado-01.md` (números tirados do `.jtl`).

## Como rodar (somente modo CLI, contra a aplicação local, nunca ambiente compartilhado)
```bash
jmeter -n -t perf/reservas.jmx -l perf/resultados/rodada-01.jtl -e -o perf/relatorio-01
```
