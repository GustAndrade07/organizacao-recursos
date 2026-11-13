# Banco de perguntas da prova oral

> Regras (Aula 13): 15 perguntas **específicas do projeto**, nunca genéricas. Cada uma aponta o arquivo onde está a resposta e descreve como seria uma **resposta fraca**.
> **Não escrever as respostas.** Distribuir para que cada um responda sobre o que **não** escreveu.
> **Completo em 13/11/2026** (15 perguntas). Os arquivos citados foram conferidos um a um no repositório nesta data; os das perguntas 1 a 4, que eram previsão, também.
> Cada pergunta é de uma área: quem responde **não** escreveu aquela parte (conferência no fim do arquivo).

| # | Pergunta | Arquivo da resposta | Resposta fraca | Quem responde |
|---|---|---|---|---|
| 1 | Qual mecanismo impede a dupla reserva e qual teste prova isso? | `docs/adr/0002-anti-reserva-dupla.md`, teste de concorrência | Diz "tem um teste de concorrência" sem citar a constraint nem a contagem feita no banco | Pedro |
| 2 | Por que houve mock em um ponto e Testcontainers em outro? | `docs/plano-de-teste.md` (política de mock), `docs/testes/plano-tdd.md`, `ApiIntegracaoTest` vs. `TentativasDeLoginTest` | Diz "o professor mandou", sem relacionar mock a teste unitário isolado e banco real a persistência | Leandro |
| 3 | O que o p95 medido permite ou não permite afirmar? | `docs/performance/resultado-01.md`, `perf/resultados/rodada-01.jtl` | Generaliza para produção a partir de máquina local; não separa 409 esperado de erro real | Gustavo |
| 4 | Qual teste falha se a aprovação de restrito for removida? | `src/test/resources/features/aprovacao-bloqueio.feature`, `docs/testes/mutacao-manual.md` | Não sabe indicar o teste nem mostrar a mutação | Pedro |
| 5 | O `EXCLUDE USING gist` barra a sobreposição, mas por que foi preciso **também** uma trava por recurso? O que foi medido antes de decidir? | `docs/adr/0007-modelagem-protecao-reserva-dupla.md`, D7 da seção 9 de `docs/plano-execucao.md`, `ConcorrenciaReservaBancoTest.gravarOcupacaoTomaUmaTravaPorRecursoEASoltaNoFimDaTransacao` | Diz "para evitar concorrência" sem falar do deadlock `40P01` nem citar o número medido (sem a trava, 9 de 40 rodadas falharam) | Leandro |
| 6 | Como o Solicitante é impedido de mexer na reserva de outro, e qual teste prova **célula por célula**? | `docs/seguranca/matriz-autorizacao.md` (células `dono` e `não-dono`), `MatrizAutorizacaoTest.cadaCelulaDaMatrizValeNaApi` | Responde "tem Spring Security" ou cita só o `hasRole`; não distingue permissão de rota (segurança) de posse (serviço) | Gustavo |
| 7 | O que acontece se alguém criar uma rota nova e esquecer de cadastrá-la na matriz? | `MatrizAutorizacaoTest.matrizCobreExatamenteAsRotasDoCodigo`, `anyRequest().denyAll()` em `SegurancaConfig` e `SegurancaDasTelasConfig`, regra 13 do `AGENTS.md` | Diz "o teste quebra" sem explicar que a rota esquecida é **negada**, não liberada | Leandro |
| 8 | Como a auditoria evita registro órfão num rollback? Em que ordem as coisas acontecem? | `AuditoriaBancoTest` (3 testes), `AuditoriaServiceTest`, RN-12 do `docs/prd.md` | Responde "está na mesma transação" sem explicar os dois sentidos do teste (falha na mudança de estado e falha na auditoria) | Leandro |
| 9 | Mostre uma linha do relatório de utilização calculada **à mão** e explique por que o valor é esse. | `RelatorioUtilizacaoBancoTest` (comentários com a conta), `RelatorioApiControllerTest.valoresCalculadosAMaoComBordaCanceladaEConflito` | Lê o número da tela sem saber recalcular; não sabe dizer que reserva cancelada não conta nem o que acontece na borda | Pedro |
| 10 | Quais perguntas do relatório de utilização o grupo **não** decidiu, e o que o código faz enquanto isso? | D8 da seção 9 de `docs/plano-execucao.md`, comentários em `src/main/resources/db/consultas/` | Afirma que está tudo decidido, ou não sabe que os testes registram a premissa em comentário | Gustavo |
| 11 | Qual commit mostra o **vermelho** de um ciclo de TDD, e o que o corpo dele traz? | `docs/testes/evidencia-tdd.md`, commits `cd14f8c` (red) e `e86c777` (green) | Diz "fizemos TDD" sem citar um par red/green, ou não sabe que o `test(red)` é a única exceção ao `verify` verde (regra 11 do `AGENTS.md`) | Gustavo |
| 12 | O que a cobertura de 80% **não** garante? Dê um exemplo neste projeto. | `docs/relatorio-qualidade.md`, `docs/testes/mutacao-manual.md`, gate no `pom.xml` | Trata cobertura como qualidade; não cita a mutação como a verificação que realmente mostra se o teste protege a regra | Pedro |
| 13 | Por que o projeto usa PostgreSQL de verdade nos testes em vez de H2, e qual regra deixaria de ser testável com H2? | `AGENTS.md` seção 5, `ApiIntegracaoTest`, `SobreposicaoBancoTest` | Diz "é mais parecido com produção" sem citar `tstzrange`, `btree_gist` e a constraint de exclusão, que o H2 não tem | Leandro |
| 14 | Um defeito foi achado na `main`: conte o caminho dele, do sintoma ao teste que impede a volta. | `docs/defeitos/DEF-001.md` e `DEF-002.md`, commits `7b62b0b`/`93f0a1c` e `cc363a4` | Conta que "corrigimos" sem citar o registro do defeito **antes** da correção nem o teste de regressão que generaliza (varrer todos os templates, não listar à mão) | Pedro |
| 15 | O RF-11 (notificação externa) está ausente. Por que isso não impede a entrega, e o que exatamente ficaria faltando testar? | `docs/adr/0005-notificacao-cliente-http-isolado.md` (tabela de cenários do WireMock), `docs/decisao-qualidade.md`, `docs/status-requisitos.md` | Esconde a ausência, ou não sabe listar os cenários (200, 404, 500, timeout, JSON inválido, "token não vaza", "só depois do commit") | Gustavo |

## Perguntas que ficaram de fora (e por quê)
- **"Por que o token do provedor externo não vaza na resposta?"** — o RF-11 não foi implementado; a pergunta 15 trata da ausência, que é a resposta honesta hoje.
- Perguntas sobre as premissas do relatório e sobre quando o Solicitante altera ou cancela: dependem das decisões **D8** e **D9**, ainda abertas. Pergunta sobre ponto não decidido vira discussão, não avaliação.

## Arquivos citados que entraram em 13/11/2026
As perguntas 2, 4, 11 e 12 citam `docs/testes/plano-tdd.md`, `src/test/resources/features/aprovacao-bloqueio.feature`, `docs/testes/evidencia-tdd.md` e `docs/testes/mutacao-manual.md` (seção do serviço). Os quatro vêm dos PRs de 13/11/2026 desta mesma leva; se algum não for mergeado, a pergunta correspondente perde a fonte e precisa ser trocada antes do ensaio.

## Conferência de distribuição

Combinado da Aula 13: ninguém responde só sobre o que escreveu. Áreas: Gustavo (banco, CI, SonarCloud, arquitetura, README), Pedro (back-end), Leandro (front-end, RTM, relatório, decisão de qualidade).

| Integrante | Perguntas que responde | Sobre áreas que não escreveu? |
|---|---|---|
| Gustavo | 3, 6, 10, 11, 15 | **Sim.** 3 é carga (Leandro), 6 é autorização e 15 é o ADR da notificação (Pedro), 10 é relatório (Pedro e Leandro), 11 é TDD das telas (Leandro). Nenhuma é de banco |
| Pedro | 1, 4, 9, 12, 14 | **Sim.** 1 e 4 são banco e mutação (Gustavo), 9 é a consulta do relatório (Gustavo), 12 é cobertura e relatório (Leandro e Gustavo), 14 é defeito de template (Leandro) |
| Leandro | 2, 5, 7, 8, 13 | **Sim.** Todas são de back-end ou banco: política de mock, trava por recurso, `denyAll` e matriz, auditoria, Testcontainers |

## Como usar no ensaio
Uma pergunta por vez, sem consultar arquivo. A resposta vale quando **cita a evidência** (arquivo, teste, commit ou run) e sabe dizer o limite dela. O ensaio cronometrado e a pontuação ficam em [`ensaio.md`](ensaio.md) — ele é humano e ainda **não foi feito**.
