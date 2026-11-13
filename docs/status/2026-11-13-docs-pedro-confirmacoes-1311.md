# Status — 13/11/2026 — `docs/pedro-confirmacoes-1311`

**Quem:** Pedro (back-end), com IA (Claude Code). Revisão: Gustavo.
**Ordem de merge sugerida:** depois de `test/d10-d11-api-1311` e `test/mutacao-relatorio-1311`, porque a decisão de qualidade passa a citar os testes desses dois PRs.

## 1. Confirmações pedidas ao Pedro

| Item | O que foi conferido | Resultado |
|---|---|---|
| Limite de login no `AutenticacaoService` (PR #52, Leandro) | Código de `AutenticacaoService.entrar` e `TentativasDeLogin`: o hash é conferido nos três casos (login inexistente, senha errada, bloqueado) antes da recusa, e os três dão a mesma resposta; a tentativa durante o bloqueio não conta; o sucesso zera o contador; o mapa em memória é limpo acima de 10 000 registros. Testes: `LimiteDeTentativasApiTest`, `LimiteDeTentativasDoLoginIntegracaoTest` (servidor real, regra 15 do `AGENTS.md`) e `LimiteDeTentativasE2ETest` | **Confirmado** |
| Rotas do Swagger fora das tabelas da matriz (PR #61) | `SegurancaDasTelasConfig` exige sessão em `GET /v3/api-docs`, `/v3/api-docs/**`, `/swagger-ui.html` e `/swagger-ui/**`; a matriz explica numa seção própria por que não entram nas tabelas (servidas pelo pacote `org.springdoc`, fora dos pacotes que o `MatrizAutorizacaoTest` compara); `OpenApiDocumentacaoTest.semSessaoADocumentacaoLevaAoLogin` e `documentacaoDescreveExatamenteAsRotasDaApiDaMatriz` prendem as duas coisas | **Confirmado** |
| RF-11 (WireMock) | Continua sem código e sem WireMock no `pom.xml` | **Aceito como risco** (decisão do Pedro, 13/11/2026): fora da `v1.0.0-rc1`; risco 5 da decisão de qualidade |
| D13 (RN-04, janela entre a consulta e a gravação) | Já aceita (ADR-0008, OK do Pedro em 12/11/2026) | Sem mudança |
| Testes que faltavam | D10 (c), D10 (d) e D11 na API: PR `test/d10-d11-api-1311`. Mutação do serviço do relatório: PR `test/mutacao-relatorio-1311` | Ver os status desses PRs |
| Declaração | Escrita em `docs/decisao-qualidade.md`, seção 4 | Feita |

## 2. Itens do grupo (o Pedro registra a posição dele; não decide pelo grupo)
- **D8 e D9:** continuam **sem decisão** (seção 9 do plano). O código e os testes seguem a proposta; nada mudou aqui (regra 4 do `AGENTS.md`).
- **Limite de login em memória e sem limite por IP:** **Pedro aceita por escrito** (parte da API), risco 8 da decisão de qualidade. **Falta o aceite do Leandro.**
- **Testes sem commit `test(red)`** (telas 7 e 8, #48, #51, #61): registrados como desvio de processo em `docs/testes/evidencia-tdd.md`, seção 4, junto com os testes novos de 13/11 (que também nasceram verdes). Fica como proposta até o aceite do Gustavo e do Leandro.

## 3. Spec do Back-end no Notion — texto para colar
**Não atualizada por esta sessão:** o Notion ligado ao Claude Code nesta máquina é um workspace pessoal, sem as specs do grupo (a busca não achou nenhuma página). O Pedro cola o texto abaixo na spec, dentro do prazo de 24 h da regra 7 do `AGENTS.md`.

- **Cabeçalho:** "Atualizada em 13/11/2026 a partir da `main` (até o PR #62)".
- **Seção 1:**
  - RF-14: "Feito: springdoc 2.8.17, `/v3/api-docs` e `/swagger-ui/index.html` (exigem sessão), `OpenApiDocumentacaoTest` contra a matriz (#61)".
  - RF-01 e RF-06: "Feito (RTM, run 43303372662)".
  - RF-10: acrescentar "RN-08 em toda transição (#61)".
  - Demais linhas: copiar de `docs/status-requisitos.md`.
- **Seção 3:** trocar "210 casos" por "222" e acrescentar "Rotas do Swagger exigem sessão e ficam fora das tabelas da matriz, numa seção própria".
- **Seção 5:** trocar "Ainda sem OpenAPI" por "OpenAPI em `/v3/api-docs`; Swagger UI em `/swagger-ui/index.html`, depois do login".
- **Seção 8:**
  - BDD: "Cucumber 7.23.0, 13 cenários, rodam no `verify`".
  - Carga: "JMeter local, p95 de 17 e 8 ms; 80 × 201 e 3920 × 409".
  - Mutação: "seis regras feitas (banco, API e serviço); a MAU1 achou a lacuna da auditoria e entraram 4 testes".
- **Seção 9:** mover D10 e D11 para "Decididas" ("fica como está no código"; as lacunas de teste de D10 (c), D10 (d) e D11 na API foram fechadas pelo PR `test/d10-d11-api-1311`). Tirar "RF-14" dos abertos e o "run do CI" de RF-01 e RF-06. **D4:** trocar "pendente" por "decidida em 13/11/2026, leitura rígida".

Conferido no repositório antes de escrever: springdoc 2.8.17 e Cucumber 7.23.0 no `pom.xml`; 222 casos e run 43303372662 em `docs/status-requisitos.md`; p95 17 e 8 ms e 80 × 201 / 3920 × 409 em `docs/performance/resultado-01.md`; D4 decidida em `docs/plano-execucao.md`, seção 9 (PR #63).

## Verificado
- Código igual ao da `main` `bf2584a` (só documentação muda); o `verify` desse código mais os 8 testes da branch `test/d10-d11-api-1311` deu 1213 testes, 0 falhas, 0 ignorados (13/11/2026, esta máquina).

## Não verificado
- Spec do Notion (ver seção 3).
- Aceite do Leandro (risco 8) e do grupo (desvio de TDD, D8, D9).
