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
jmeter -n -t perf/reservas.jmx -Jadmin_login=<login> -Jadmin_senha=<senha> -l perf/resultados/rodada-01.jtl -e -o perf/relatorio-01
```

- **Preparação** (setUp do plano, fora das métricas): o Administrador cria os 50 usuários do CSV e uma sala nova a cada rodada.
- **O Administrador** vem por propriedade (`-Jadmin_login`, `-Jadmin_senha`). Na aplicação local, ele é criado no start com `ADMIN_LOGIN` e `ADMIN_SENHA`. Use uma senha só desta rodada, nunca escrita em arquivo do repositório.
- **Outras propriedades**, com os padrões do cenário de `docs/performance/rnf.md`: `-Jusuarios=50`, `-Jrampa=10`, `-Jrepeticoes=20`, `-Jhost=localhost` e `-Jporta=8080`.
- **Cada usuário entra uma vez, com a senha certa.** Senha errada esbarra no limite de tentativas do login (5 falhas em 15 min bloqueiam, ADR-0009).
- **Resultado da primeira medição:** `docs/performance/resultado-01.md` (rodadas 01 e 02, 12/11/2026).
