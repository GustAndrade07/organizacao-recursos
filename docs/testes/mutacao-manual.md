# Mutação manual das regras críticas

Plano de execução, Etapa 5 [Aula 12]: quebrar cada regra crítica de propósito, ver o teste falhar e desfazer a quebra com `git restore`. Teste que continua verde com a regra quebrada é lacuna: escrever o teste que falta. **Nenhuma mutação pode sobrar no código.**

Este arquivo tem **só a seção do banco** (Gustavo). As regras que dependem do serviço de reserva, da aprovação e da autorização ficam como **pendente (Pedro)**.

## Como foi medido (vale para todas as linhas)

- Data: 07/11/2026. Máquina: a do Gustavo (Windows 11, JDK 21.0.8, Docker Desktop, PostgreSQL 16.15 em Testcontainers). **Medido só nesta máquina; não generalizar.** Não há run do CI com estes números.
- Base: branch `feat/banco-auditoria-relatorio` no commit `fa1c910`, que tem a V1 do PR #28.
- Comando de cada rodada (M1 e M2: o pacote inteiro de banco; M3 a M5: só a classe citada):

```powershell
.\mvnw.cmd -B test "-Dtest=br.com.senac.recursos.banco.*Test"
```

- Linha de base, sem mutação: **83 testes, 0 falhas, 0 erros, 0 ignorados**.
- Depois de cada mutação: `git restore -- <arquivo>` e `git diff -- <arquivo>` vazio. No fim, `git status --short` sem nenhuma linha. Os logs das rodadas ficaram na pasta temporária da sessão (não versionados); para conferir, repita a mutação descrita e o comando acima.

## As seis regras críticas

| Regra | Situação | Mutação |
|---|---|---|
| Sobreposição | banco feito | M1 |
| Dupla reserva (concorrência) | banco feito | M1 e M2 (mesma constraint: uma mutação cobre as duas regras) |
| Aprovação de recurso restrito | pendente (Pedro) | regra de serviço, não há constraint no banco |
| Manutenção | pendente (Pedro) | a V1 não tem exclusão em `manutencao` (só um índice); o bloqueio é do serviço, então não afirmo mutação de banco |
| Autorização | pendente (Pedro) | matriz de autorização e testes de API |
| Auditoria | banco parcial (M3a e M3b) | falta a mutação no serviço que grava a auditoria (Pedro) |

## Mutações do banco

### M1 — constraint de exclusão deixa de proteger (sobreposição e dupla reserva)

- **Arquivo e quebra:** `V1__nucleo_da_reserva.sql`, na constraint `reserva_recurso_sem_sobreposicao`, `WHERE (estado IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA'))` trocado por `WHERE (false)`: nenhuma linha entra na exclusão.
- **Resultado:** **24 de 83 testes vermelhos**.
  - `ConcorrenciaReservaBancoTest`: 10 (os dois cenários de 20 pedidos, recurso e professor, 5 rodadas cada; a asserção "pedidos aceitos" deixa de ser 1).
  - `SobreposicaoBancoTest`: 12 (mesmo recurso, professor com sala diferente, fronteiras do intervalo, estados que ocupam horário, reativar reserva cancelada em horário ocupado).
  - `AuditoriaBancoTest`: 1 (`mudancaDeEstadoRecusadaPeloBancoDesfazAAuditoriaJaGravada`: o banco deixa de recusar a reativação).
  - `TentativaRecusadaBancoTest`: 1 (`tentativaGravadaEmTransacaoPropriaSobreviveAoRollbackDoPedidoRecusado`: não há violação para recusar).
  - Os 22 de Concorrência e Sobreposição são os mesmos "22 de 41" do PR #28; os outros 2 são testes escritos depois.
- **Conclusão:** a proteção contra reserva dupla não é enfeite; sem ela, os testes pegam. Nenhuma lacuna encontrada nesta mutação.

### M2 — trava por recurso (D7) deixa de travar

- **Arquivo e quebra:** `V1__nucleo_da_reserva.sql`, no corpo de `reserva_recurso_trava_recurso()`, `PERFORM pg_advisory_xact_lock(...)` trocado por `PERFORM 1;` (o trigger continua existindo, mas não trava).
- **Resultado:** **2 de 83 testes vermelhos**.
  - `ConcorrenciaReservaBancoTest.gravarOcupacaoTomaUmaTravaPorRecursoEASoltaNoFimDaTransacao`: determinístico, confere as travas em `pg_locks` (sala e professor).
  - `ConcorrenciaReservaBancoTest.vintePedidosSimultaneosParaAAgendaDoMesmoProfessor...`: falhou numa rodada com "SQLState diferente de 23P01", o efeito da corrida sem a trava. **Esse é intermitente por natureza**: apareceu nesta execução, mas não se deve contar com ele; a prova estável da trava é o teste de `pg_locks`. Uma execução só; não repetida para medir frequência (não medido).
- **Conclusão:** a trava tem teste determinístico. Os números do PR #28 (9 de 40 rodadas com deadlock sem a trava) continuam sendo de 06/11/2026 e não foram refeitos aqui.

### M3a e M3b — auditoria e mudança de estado deixam de ser atômicas

Não existe código de produção de auditoria ainda (o serviço é do Pedro). Por isso estas duas mutações foram feitas **no próprio `AuditoriaBancoTest`**, simulando um serviço errado, e restauradas em seguida. Elas provam que o teste detecta o defeito; **não provam** nada sobre o serviço real, que continua pendente.

| Mutação | Quebra | Resultado |
|---|---|---|
| M3a | a auditoria passa a ser gravada em transação própria (`REQUIRES_NEW`) antes da mudança de estado que o banco recusa | 1 de 3 vermelhos: `mudancaDeEstadoRecusadaPeloBancoDesfazAAuditoriaJaGravada` (a auditoria sobrevive ao rollback: auditoria órfã) |
| M3b | a mudança de estado passa a ser gravada em transação própria antes da auditoria que o banco recusa | 1 de 3 vermelhos: `auditoriaRecusadaPeloBancoDesfazAMudancaDeEstadoJaFeita` (o estado muda sem auditoria) |

Este é também o "vermelho" do teste de rollback, que nasceu verde porque o rollback é do PostgreSQL (regra 10 do `AGENTS.md`).
**Pendente (Pedro):** repetir a mutação de auditoria no serviço de reserva (gravar a auditoria fora da transação) e ver o teste do serviço falhar.

### M4 e M5 — consultas do relatório (RF-12), mutações a mais

Não estão entre as seis regras, mas servem para provar que os valores calculados à mão do `RelatorioUtilizacaoBancoTest` não são tautologia.

| Mutação | Arquivo e quebra | Resultado |
|---|---|---|
| M4 | `relatorio-horas-por-recurso.sql`: o filtro de estado trocado por `WHERE true` (cancelada, rejeitada e não compareceu passam a contar) | 1 de 6 vermelhos: `reservaCanceladaRejeitadaOuQueNaoCompareceuNaoContaComoUso` |
| M5 | `relatorio-horas-por-recurso.sql`: a duração passa a ser a da reserva inteira, sem a interseção com a janela | 1 de 6 vermelhos: `reservaQueCruzaABordaContaSoAParteDentroDaJanela` |

Não mutei ainda `relatorio-carga-por-professor.sql` nem `relatorio-conflitos-evitados.sql` (não medido): têm teste próprio, mas sem rodada de mutação.

## Lacunas encontradas

Nenhuma nas mutações acima: cada quebra deixou pelo menos um teste vermelho. Fica de fora, por escolha, mutar as constraints secundárias (CHECK do intervalo semiaberto, FK composta do estado), que têm teste em `EsquemaBancoTest` e `SobreposicaoBancoTest` mas não passaram por rodada de mutação (não medido).
