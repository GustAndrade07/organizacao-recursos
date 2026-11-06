# ADR-0007 — Modelagem da proteção contra reserva dupla: uma constraint, estado copiado e trava por recurso
- **Status:** proposto (decisões D6 e D7 do Gustavo em 06/11/2026; a validar com o Pedro, que escreve o serviço, e vira "aceito" na revisão do PR)
- **Contexto:** o [ADR-0002](0002-anti-reserva-dupla.md) decidiu que a reserva dupla é barrada por uma constraint de exclusão no banco, parcial pelo estado da reserva (D5), com intervalo semiaberto (D1), e deixou três pontos para a migração V1: como cobrir recurso **e** professor, como o predicado enxerga o estado da reserva (que fica em outra tabela) e a tabela das tentativas recusadas (D2). Na V1 apareceu um quarto ponto, achado ao rodar os 20 pedidos simultâneos: deadlock. Este ADR **não substitui** o ADR-0002; completa o que ele deixou para a V1.
- **Decisão:**
  1. **Professor é recurso** (`recurso.tipo = 'PROFESSOR'`, como já dizia a spec do Banco) e há **uma única** constraint `reserva_recurso_sem_sobreposicao`: `EXCLUDE USING gist (recurso_id WITH =, periodo WITH &&) WHERE (estado IN ('SOLICITADA','APROVADA','EM_USO','CONCLUIDA'))`. Uma reserva de sala com docente grava **duas linhas** em `reserva_recurso` (sala e professor) na mesma transação; qualquer violação desfaz tudo. (D6)
  2. **O estado da reserva é copiado** para `reserva_recurso.estado` por chave estrangeira composta `(reserva_id, estado) → reserva(id, estado) ON UPDATE CASCADE`. Mudar `reserva.estado` atualiza as linhas em cascata, então "ativa" nunca diverge do estado, sem trigger e sem depender do código da aplicação. O predicado da constraint (que só enxerga a própria linha) usa essa cópia. (D6)
  3. **Tabela das tentativas recusadas:** `tentativa_recusada(id, recurso_id, solicitante_id, periodo_pedido, motivo, registrada_em)`; `motivo` só aceita `SOBREPOSICAO` (o PRD define "conflitos evitados" como tentativas recusadas por sobreposição); `registrada_em` vem do `Clock` da aplicação (sem `DEFAULT now()`). Gravada em transação própria (D2). (D6)
  4. **Trava por recurso (D7):** um trigger `BEFORE INSERT OR UPDATE` em `reserva_recurso` toma `pg_advisory_xact_lock` por recurso, liberada no commit ou rollback. Escritores do mesmo recurso entram em fila e o perdedor recebe sempre `23P01` (violação de exclusão), que o serviço traduz em 409. A garantia continua sendo a constraint; o trigger só ordena a fila.
  5. **Índices da V1:** só os necessários (o GiST da própria constraint, GiST `(recurso_id, periodo)` em `manutencao`, e índices de chave estrangeira e do relatório). Nenhum índice de busca por capacidade, localização ou competência antes de medir (RNF-01 **não medido**); adicionar depois é uma V2 simples. (D6)
  6. **Escopo da V1:** núcleo da reserva (`usuario`, `recurso`, `manutencao`, `reserva`, `reserva_recurso`, `tentativa_recusada`, `auditoria_reserva`). Ficam para uma V2: `movimentacao_material` (RF-09) e as colunas de `recurso` ainda sem decisão (competências, localização). (D6)
- **Consequências:**
  - **Ganhamos:**
    - Um mecanismo só para sala, material e professor; o teste prova os dois casos (`SobreposicaoBancoTest`, `ConcorrenciaReservaBancoTest`).
    - "Ativa" não pode divergir do estado: é o banco que propaga (`SobreposicaoBancoTest`: cancelar libera, estado incoerente é recusado com 23503, reativar com conflito é recusado com 23P01).
    - Resposta de conflito previsível: `23P01` sempre, em vez de uma mistura com `40P01`.
  - **Perdemos:**
    - **Sem a trava, 9 de 40 rodadas de 20 pedidos simultâneos terminaram em deadlock (`40P01`) e cada rodada ruim levou cerca de 17 a 20 s** (PostgreSQL 16.15 em Docker, uma máquina, 06/11/2026; não generalizar). Com a trava, 30 de 30 rodadas passaram.
    - Lógica imperativa a mais (o trigger) e **escrita serializada por recurso**: o efeito sobre RNF-01 e RNF-03 está **não medido**.
    - Cada mudança de estado atualiza também as linhas de `reserva_recurso` (cascata); custo **não medido**.
    - A constraint não diz sozinha se o conflito foi de sala ou de professor; o serviço sabe pelo `recurso_id` e pelo tipo do recurso.
    - Os períodos ficam repetidos em cada linha de `reserva_recurso`; nada no banco obriga as linhas da mesma reserva a terem o mesmo período. Fica como regra do serviço (ponto em aberto abaixo).
  - **Exige de testes:**
    - `ConcorrenciaReservaBancoTest`: 20 threads → 1 aceita e 19 recusadas, contagem no banco, para o recurso e para o professor, 5 rodadas cada; mais o teste determinístico da trava (`pg_locks`), que falha sem o trigger.
    - `SobreposicaoBancoTest`, `TentativaRecusadaBancoTest`, `EsquemaBancoTest` e `MigracoesBancoTest`, como listados no PR da V1.
    - Mutação manual em `docs/testes/mutacao-manual.md` (Pedro/Leandro, Etapa 5): remover a constraint; o teste de concorrência deve ficar vermelho. Feita em 06/11/2026 durante a V1 trocando o predicado por `WHERE (false)`: 22 de 41 testes ficaram vermelhos; ainda não registrada no arquivo de mutação.
- **Pendências:**
  - **Para o Pedro (serviço de reserva):** gravar as linhas de `reserva_recurso` de uma reserva em ordem crescente de `recurso_id` (evita duas reservas se travarem em ordem oposta); traduzir `23P01` em 409; tratar `40P01` como defesa (nova tentativa ou 409), embora a trava deva evitá-lo; gravar a tentativa recusada em transação própria. O teste de API (409 de ponta a ponta) é dele e **não existe**.
  - **Ponto em aberto:** impor no banco que as linhas da mesma reserva tenham o mesmo período (hoje é regra do serviço).
  - **D6 e D7** estão na seção 9 do [plano](../plano-execucao.md), decididas pelo Gustavo em 06/11/2026 e **a confirmar com o Pedro**.
  - A extensão `btree_gist` fora da imagem `postgres:16` continua **não conferida** ([ADR-0006](0006-flyway-dono-do-esquema.md)).
- **Fonte:** `src/main/resources/db/migration/V1__nucleo_da_reserva.sql`, `src/test/java/br/com/senac/recursos/banco/`, [ADR-0002](0002-anti-reserva-dupla.md), [ADR-0006](0006-flyway-dono-do-esquema.md), `docs/plano-execucao.md` (seção 9), `docs/prd.md` (RN-02, RN-03, RN-05, RF-12). Medições: execuções do `mvnw` de 06/11/2026 na máquina do Gustavo; não há `.jtl` nem run de CI com esses números.
