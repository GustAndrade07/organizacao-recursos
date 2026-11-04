# Checklist de entrega (meta interna 11/11 · prazo 13/11)

> Formato do professor (Aula 14): **item → existe → atualizado → caminho**.
> Deve ser preenchido a partir de um **clone limpo**, seguindo só o README, por quem **não** escreveu o README.
> Nesta etapa só se anota; nada é corrigido. No fim, listar os bloqueadores em ordem.
> Estado abaixo: 04/11/2026, repositório ainda sem código.

| Item | Existe | Atualizado | Caminho |
|---|---|---|---|
| Repositório público com revisão por pares (PRs revisados) | não | não | (link do GitHub) |
| Aplicação executável (sobe seguindo o README) | não | não | `README.md` |
| README e documentação da API (OpenAPI) | parcial | não | `README.md` |
| PRD com RF, RN e RNF numerados | sim | parcial | `docs/prd.md` |
| RTM (requisito → risco → teste → evidência) | parcial | não | `docs/rtm.md` |
| Status dos requisitos | sim | sim | `docs/status-requisitos.md` |
| Diagramas: contexto, contêineres/componentes e sequências críticas | não | não | `docs/arquitetura.md` |
| ADRs (JWT, anti reserva dupla, Thymeleaf, Playwright) | parcial | não | `docs/adr/` |
| Plano de testes | não | não | `docs/testes/plano-tdd.md` |
| Evidência de TDD (commits red → green → refactor) | não | não | `docs/testes/evidencia-tdd.md` |
| BDD (cenários e `.feature`) | parcial | não | `docs/testes/bdd-aprovacao-bloqueio.md` |
| Mutação manual das regras críticas | não | não | `docs/testes/mutacao-manual.md` |
| Matriz de autorização | não | não | `docs/seguranca/matriz-autorizacao.md` |
| Pipeline de CI (verde) | parcial | não | `.github/workflows/ci.yml` |
| Relatório do JaCoCo (≥ 80% linhas / 70% branches) | não | não | artifact do CI / `target/site/jacoco` |
| Quality Gate do SonarCloud | não | não | (link do SonarCloud) |
| Plano de carga (JMeter) | não | não | `perf/reservas.jmx` |
| RNF de desempenho (escrito antes da rodada) | não | não | `docs/performance/rnf.md` |
| Resultado do teste de carga | não | não | `docs/performance/resultado-01.md` + `perf/resultados/rodada-01.jtl` |
| Registro de defeitos | parcial | não | `docs/defeitos/` |
| Relatório final de qualidade (fonte de cada número) | não | não | `docs/relatorio-qualidade.md` |
| Banco de perguntas e ensaio da prova oral | parcial | não | `docs/prova-oral/perguntas.md`, `docs/prova-oral/ensaio.md` |

## Entregáveis finais
- [ ] Tag anotada `v1.0.0-rc1` com CI verde **no commit da tag** (se corrigir depois: `rc2`; nunca mover tag já usada)
- [ ] `CHANGELOG.md` com o que entra na versão
- [ ] `docs/evidencias/README.md`: índice com link do CI no commit da tag, JaCoCo (artifact), Quality Gate, `resultado-01.md`, `rtm.md`, `defeitos/` e `relatorio-qualidade.md`; nenhum link quebrado
- [ ] `docs/decisao-qualidade.md`: liberar / liberar com ressalvas / bloquear; critérios e evidência de cada um; riscos aceitos **com impacto, justificativa e responsável**; declaração de cada integrante
- [ ] Formulário de entrega enviado por **um** integrante: nomes completos (até 4) e link do repositório público

## Bloqueadores (em ordem; os 4 primeiros zeram a nota)
1. Aplicação não executa
2. Pipeline ausente / vermelho
3. Falha de autorização
4. Possibilidade de reserva dupla
5. (demais, após o clone limpo)
