# Checklist de entrega (meta interna 11/11 · prazo 13/11)

> Formato do professor (Aula 14): **item → existe → atualizado → caminho**.
> Deve ser preenchido a partir de um **clone limpo**, seguindo só o README, por quem **não** escreveu o README.
> Nesta etapa só se anota; nada é corrigido. No fim, listar os bloqueadores em ordem.
> Estado abaixo: **13/11/2026**. A rodada de clone limpo é do commit `9acbbcd` (antes dos merges de #56 a #59). Quem rodou foi a **IA**, num container novo, não um integrante: ver a ressalva no fim desta seção e o [roteiro](roteiro-clone-limpo.md).
> **Atualização de 13/11/2026, tarde** (depois dos PRs #56 a #62, `main` em `bc40183`): as linhas cujo arquivo entrou nesses PRs foram relidas **na árvore do repositório** em `bc40183`, **não** em clone. Cada uma diz isso. **Trocar `bc40183` pelo commit da tag** quando ele existir (o clone limpo humano e os números da tag ainda precisam ser refeitos nele).
>
> "Existe" = o arquivo está no commit testado. "Atualizado" = foi conferido nesta rodada **ou** traz data e fonte de uma medição desta semana; quando não deu para conferir, a coluna diz "não conferido" em vez de "sim".

| Item | Existe | Atualizado | Caminho |
|---|---|---|---|
| Repositório público com revisão por pares (PRs revisados) | sim | sim | https://github.com/GustAndrade07/organizacao-recursos (59 PRs mergeados até `bc40183`, #1 a #62, com 3 fechados sem merge; ruleset `proteger-main` com 1 aprovação e checks `verify` e `SonarCloud Code Analysis`) |
| Aplicação executável (sobe seguindo o README) | sim | sim | `README.md`; rodada de 13/11: `{"status":"UP"}`, reserva gravada pela API e pela tela, 409 na sobreposição |
| README e documentação da API (OpenAPI) | sim | parcial | `README.md` conferido e corrigido em 13/11 (faltava dizer que `ADMIN_LOGIN`/`ADMIN_SENHA` são o único jeito de entrar). **OpenAPI feito no PR #61** (RF-14): `/v3/api-docs` e `/swagger-ui/index.html`, com sessão, e `OpenApiDocumentacaoTest`; lido na árvore de `bc40183`. O comando do README para o PowerShell **não foi conferido** |
| PRD com RF, RN e RNF numerados | sim | não conferido | `docs/prd.md` (RF-01 a RF-14, RN-01 a RN-13, RNF-01 a RNF-03) |
| RTM (requisito → risco → teste → evidência) | sim | sim (lido em `bc40183`) | `docs/rtm.md` — recontada no PR #57; RF-14 e RN-08 `feito` no #61. Refazer o run no commit da tag |
| Status dos requisitos | sim | sim (lido em `bc40183`) | `docs/status-requisitos.md` — atualizações nos PRs #57, #58 e #61 |
| Diagramas: contexto, contêineres/componentes e sequências críticas | sim | não conferido | `docs/arquitetura.md` (C4 e sequências em Mermaid; pacotes e rotas ainda marcados como proposta) |
| ADRs (JWT, anti reserva dupla, Thymeleaf, Playwright) | sim | sim | `docs/adr/0001` a `0009` (o 0008 ficou aceito no PR #58) |
| Plano de testes | sim | sim (lido em `bc40183`) | `docs/plano-de-teste.md` e `docs/testes/plano-tdd.md` (PR #61, lacuna 7; cita só teste que existe e nomeia 3 lacunas) |
| Evidência de TDD (commits red → green → refactor) | sim | sim (lido em `bc40183`) | `docs/testes/evidencia-tdd.md` (PR #61): 4 ciclos red → green com o hash de cada commit e a lista do que **não** nasceu de TDD (telas 7 e 8, API em camadas, BDD) |
| BDD (cenários e `.feature`) | sim | sim (lido em `bc40183`) | `docs/testes/bdd-aprovacao-bloqueio.md` e `src/test/resources/features/aprovacao-bloqueio.feature`; Cucumber 7.23.0 no `pom.xml`; 13 cenários rodam no `verify` (PR #61) |
| Mutação manual das regras críticas | sim | sim (lido em `bc40183`) | `docs/testes/mutacao-manual.md`: banco (M1 a M5), a M1 na API (PR #58) e 8 mutações do serviço (PR #61): as seis regras críticas. Serviço do relatório sem mutação (**não medido**) |
| Matriz de autorização | sim | sim | `docs/seguranca/matriz-autorizacao.md` + `MatrizAutorizacaoTest` (lê a matriz e falha se houver rota fora dela) |
| Pipeline de CI (verde) | sim | sim | `.github/workflows/ci.yml`; `verify` rodado no clone limpo: 1116 testes, 0 falhas, 0 ignorados |
| Relatório do JaCoCo (≥ 80% linhas / 70% branches) | sim | sim | `target/site/jacoco/index.html` gerado no clone limpo: **95,8% de linhas** (1464/1528) e **81,3% de branches** (422/519); gate de 80/70 atendido |
| Quality Gate do SonarCloud | sim | não conferido nesta rodada | https://sonarcloud.io/project/overview?id=GustAndrade07_organizacao-recursos (último registro: OK no `2610d6a`, ver `docs/plano-execucao.md`, seção 4.1). **O gate do commit da tag ainda precisa ser lido** |
| Plano de carga (JMeter) | sim | sim (lido em `bc40183`) | `perf/reservas.jmx` (PR #56) |
| RNF de desempenho (escrito antes da rodada) | sim | sim (lido em `bc40183`) | `docs/performance/rnf.md` (PR #56, commit `0464847` antes da rodada). A meta de p95 ≤ 300 ms ainda **não foi validada com o professor** |
| Resultado do teste de carga | sim | sim (lido em `bc40183`) | `docs/performance/resultado-01.md` + `perf/resultados/rodada-01.jtl` e `rodada-02.jtl` (PR #56). Só local |
| Registro de defeitos | sim | sim | `docs/defeitos/DEF-001.md` (templates fora da varredura) e `DEF-002.md` (relógio real no teste de tela), os dois com correção e teste |
| Relatório final de qualidade (fonte de cada número) | sim | parcial | `docs/relatorio-qualidade.md` — versão 2 (PR #57), com a atualização de 13/11/2026 sobre o #61. Números por nível e cobertura ainda de `6ce4478`: refazer no commit da tag |
| Banco de perguntas e ensaio da prova oral | parcial | parcial | `docs/prova-oral/perguntas.md` (15 de 15, PR #62), `docs/prova-oral/ensaio.md` (**ensaio não feito**, é humano) |
| Apresentação e demonstração do sistema (roteiro de 8 min com demo ensaiada) | parcial | não | `docs/prova-oral/roteiro-demo.md` (PR #62). **Demo não ensaiada**: o ensaio cronometrado e quem fala cada parte são humanos |
| Decisão de qualidade | sim | parcial | `docs/decisao-qualidade.md` (PR #59, atualizada em 13/11/2026). Faltam as declarações dos três integrantes e a conferência no commit da tag |

## Entregáveis finais
- [ ] Tag anotada `v1.0.0-rc1` com CI verde **no commit da tag** (se corrigir depois: `rc2`; nunca mover tag já usada)
- [ ] `CHANGELOG.md` com o que entra na versão (entradas de `changelog.d/` juntadas com `bash .github/scripts/juntar-changelog.sh`, num PR antes da tag)
- [ ] `docs/evidencias/README.md`: índice com link do CI no commit da tag, JaCoCo (artifact), Quality Gate, `resultado-01.md`, `rtm.md`, `defeitos/` e `relatorio-qualidade.md`; nenhum link quebrado
- [ ] `docs/decisao-qualidade.md`: liberar / liberar com ressalvas / bloquear; critérios e evidência de cada um; riscos aceitos **com impacto, justificativa e responsável**; declaração de cada integrante — escrita em 12/11/2026 (Leandro); faltam as declarações dos três integrantes e a conferência no commit da tag
- [ ] Formulário de entrega enviado por **um** integrante: nomes completos (até 4) e link do repositório público

## Bloqueadores (em ordem; os 4 primeiros zeram a nota)
1. Aplicação não executa
2. Pipeline ausente / vermelho
3. Falha de autorização
4. Possibilidade de reserva dupla
5. **Nenhum encontrado na rodada de 13/11/2026** (clone limpo pela IA, commit `9acbbcd`): nenhum dos quatro limitadores apareceu — a aplicação executa, o `verify` ficou verde, a matriz de autorização é testada e a sobreposição respondeu 409 com a reserva única no banco.
6. **Rodada 2 (Windows, `bf2584a`, máquina do Pedro):** "aplicação não executa" **seguindo só o README** numa máquina com PostgreSQL nativo na porta 5432. É lacuna do README, não do código (com outra porta, sobe e passa). Ver a seção "Segunda rodada" abaixo.

## Segunda rodada — Windows, máquina do Pedro, 13/11/2026 (feita pela IA a pedido do Pedro)

**O que foi feito.** Clone novo de `bf2584a` numa pasta vazia do Windows 11 (PowerShell 5.1, JDK 21.0.13, Docker 29.9.3), com cache do Maven vazio, seguindo só o README e a forma `$env:` dele. Passo a passo e erros na seção 4.2 do [roteiro](roteiro-clone-limpo.md).

**Resultado, em uma linha:** seguindo só o README, **a aplicação não subiu nesta máquina**: um PostgreSQL 18 nativo do Windows ocupa a porta 5432 e a aplicação falou com ele, não com o container (`autenticação do tipo senha falhou para o usuário "recursos"`). Com o banco do projeto na porta 5433 e `DB_URL` (contorno fora do README), tudo o mais passou: `UP`, 201 e 409 pela API, a reserva da API aparece nas telas e a da tela aparece na API, 409 pela tela, cancelamento visto nas duas pontas, Swagger só com sessão; `verify` **não rodado dentro do clone** (o mesmo código deu 1213 testes, 0 falhas, na pasta de trabalho).

| Bloqueador (rodada 2) | Correção | Dono |
|---|---|---|
| README não diz que a porta 5432 precisa estar livre; com PostgreSQL instalado, a aplicação não sobe | Uma linha em "Pré-requisitos" e o exemplo de `DB_URL` com outra porta, para PowerShell | Gustavo (README), em PR separado |
| A rodada foi da IA, não do Pedro com as próprias mãos | O Pedro repete os passos 3 a 5 e assina aqui: "Feito por Pedro Henrique em __/__/2026, commit ____, resultado ____" | Pedro |

## Rodada de clone limpo — 13/11/2026 (feita pela IA, não por um integrante)

**O que foi feito.** Num container Linux novo (Ubuntu 24.04, OpenJDK 21.0.12.1, Docker 29.9.2), sem `~/.m2` e sem nada compilado, a IA clonou `https://github.com/GustAndrade07/organizacao-recursos.git` no commit `9acbbcd` e seguiu **só o `README.md`**: `docker compose up -d`, `./mvnw spring-boot:run`, `/actuator/health`, login, uma reserva de ponta a ponta (API e tela) e `./mvnw -B verify`. Passo a passo, com horas e texto dos erros, na seção 4 do [roteiro](roteiro-clone-limpo.md).

**Resultado, em uma linha:** nenhum bloqueador; o único ajuste necessário foi no README (como entrar na aplicação), feito no mesmo PR.

| Medida | Valor | Fonte |
|---|---|---|
| `GET /actuator/health` | `{"status":"UP"}` | resposta da rodada |
| `./mvnw -B verify` | `BUILD SUCCESS`, `Tests run: 1116, Failures: 0, Errors: 0, Skipped: 0`, 3 min 52 s | saída do Maven |
| Cobertura (JaCoCo do mesmo run) | 95,8% de linhas (1464/1528), 81,3% de branches (422/519) | `target/site/jacoco/jacoco.csv` |
| Reserva de ponta a ponta | 201 na primeira, **409** na que sobrepõe, 200 no cancelamento; pela tela, reserva gravada e exibida como Aprovada | respostas da API e Playwright |

**Ressalva que o grupo precisa decidir.** A Aula 14 pede que o clone seja feito por **uma pessoa que não escreveu o README**. Esta rodada foi feita por uma **IA**, num container de nuvem, numa sessão do Leandro. Ela prova que o README funciona numa máquina sem nada instalado, mas **não substitui** a conferência humana; o grupo decide se aceita como tal. Até lá, a caixa correspondente do plano fica aberta.

**Limites desta rodada** (o que ela **não** prova): foi feita no `9acbbcd`, antes dos merges dos PRs #56 a #59, então não cobre o que eles mudam; não foi feita em Windows (o README dá o comando do PowerShell, **não conferido**); e mediu uma máquina só — nada aqui se generaliza para outra.

## Conferido sem clone (07/11/2026)
**Histórico.** Esta seção é de antes do clone; a tabela do topo foi refeita em 13/11/2026 com a rodada acima. Mantida para mostrar o que se sabia sem clone, e a fonte de cada linha.

**Existência de arquivos na `main`** (`git ls-tree -r origin/main`, commit `009a4be`, 07/11/2026; só diz se o arquivo existe, **não** se está atualizado nem correto):

| Arquivo | Na `main` |
|---|---|
| `README.md`, `docker-compose.yml`, `CHANGELOG.md` | existem |
| `.github/workflows/ci.yml` | existe (o resultado dos runs **não** foi conferido aqui) |
| `docs/prd.md`, `docs/rtm.md`, `docs/status-requisitos.md`, `docs/arquitetura.md`, `docs/plano-de-teste.md`, `docs/plano-execucao.md` | existem |
| `docs/adr/0001` a `0006` | existem (0005 e 0006 como "proposto") |
| `docs/testes/bdd-aprovacao-bloqueio.md`, `docs/seguranca/riscos-dependencias.md` | existem |
| `docs/prova-oral/perguntas.md`, `docs/prova-oral/ensaio.md` | existem |
| `docs/testes/plano-tdd.md`, `docs/testes/evidencia-tdd.md`, `docs/testes/mutacao-manual.md` | **não existem** |
| `docs/seguranca/matriz-autorizacao.md` | **não existe** |
| `perf/reservas.jmx`, `docs/performance/rnf.md`, `docs/performance/resultado-01.md` | **não existem** |
| `docs/decisao-qualidade.md`, `docs/evidencias/README.md` | Os dois existem: o índice desde o PR #46; a decisão em 12/11/2026 (branch `docs/decisao-qualidade`). O `docs/relatorio-qualidade.md` está na versão 2 (PR #57) |
| `docs/avaliacao-arquitetural/ciclo-01.md`, `docs/banco/schema.sql` | **não existem** (em PRs abertos) |
| Arquivos `.feature` (BDD), OpenAPI/Swagger | **não existem** |
| `docs/defeitos/` | só `TEMPLATE.md` (nenhum defeito registrado) |

**Pontos que o README da `main` tem hoje e que o clone deve confirmar:** o README diz que "sem Docker os testes são ignorados"; o PR #28 corrige isso (teste sem Docker falha). A seção "Histórico de CI vermelho proposital" do README está vazia na `main`; o PR #30 a preenche. Os dois PRs estão abertos, sem aprovação de pessoa, em 07/11/2026.

**Não conferido:** se a aplicação sobe seguindo o README, o `verify` em máquina limpa, o resultado dos runs do CI na `main`, o Quality Gate atual e qualquer coluna "Atualizado".
