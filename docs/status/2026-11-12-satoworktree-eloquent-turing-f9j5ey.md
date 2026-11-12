# Status em 12/11/2026 (quinta, branch `satoworktree/eloquent-turing-f9j5ey`, Leandro)

**Telas ligadas ao back-end (D12, opção (a), ADR-0009 proposto).** O Leandro informou a opção (a) como combinada com o grupo; o aceite formal fica na revisão do PR. Login em `/login` com sessão do Spring Security, CSRF ligado, `POST /logout` e perfil por rota (`config/SegurancaDasTelasConfig`, tabela "Telas" da matriz de autorização, espelho da API). As 15 telas chamam os mesmos services da API, no mesmo processo. O menu vem da sessão (`AtributosDasTelas`) e `/` leva cada perfil à primeira tela do seu menu. As datas das telas ficam no fuso `America/Sao_Paulo`.

**Mudanças de contrato.**
- O campo do login passou de "E-mail" para "Login", porque o cadastro usa `[a-z0-9._-]`.
- Na tela 8, Alterar e Cancelar só aparecem para o dono da reserva (`reserva.dono`).
- Na tela 6, um `recursoId` não numérico responde 400.

**O que o esquema ainda não guarda (V2, área do Gustavo).** Localização, patrimônio, competências e motivo do bloqueio: as telas mostram "Não informado", e o que for digitado nesses campos não é gravado. A contagem de reservas por linha do relatório é feita na tela, com a regra das consultas (D5 e período).

**Testes novos ou mudados.**
- `TelasComBancoTest`: 21 testes, cada tela contra PostgreSQL real, conferindo HTML e banco.
- `MatrizAutorizacaoTest`: 210 casos; as telas são conferidas com o usuário na sessão e CSRF.
- `PaginaNaoEncontradaIntegracaoTest`: login com servidor real (senha errada, destino revalidado, 403 para URL fora da matriz e 404 de reserva inexistente com sessão). URL de tela fora da matriz é negada (`denyAll`), como manda a regra 13 do `AGENTS.md`.
- E2E com login de verdade (57 testes, eram 52).
- Os recortes `@WebMvcTest` usam services dublês sem resposta (`support/TelasNoRecorte`) e `@ComoUsuario`.
- Testes de tela que conferiam "rota real sem dados" passaram a programar a resposta do service, ou foram para rotas `/teste/...`. Nenhum deixou de conferir o template.

**Verificado (nesta máquina, container de nuvem):** `./mvnw -B verify` com 1054 testes, 0 falhas, 0 ignorados, "All coverage checks have been met". Cobertura de `target/site/jacoco/jacoco.csv`: 95,3% das linhas (1343 de 1409) e 79,4% dos branches (374 de 471). 4 mutações feitas à mão e desfeitas depois, todas pegas (lista em `docs/rtm.md`, seção "Telas ligadas ao back-end").

**Não verificado:** o run do CI deste PR (ainda não rodou ao escrever); as larguras de 768 e 1280 px (só à mão, como antes); sessão com mais de uma instância; contraste AA de todas as telas.

**Specs atualizadas:** Notion, Specs do Front-end (seção "Ligação com o back-end" e critérios de aceite), 12/11/2026.

**Segundo commit (12/11/2026).**
- O Quality Gate da SonarCloud no PR #48 reprovou por `java:S2254` (vulnerabilidade crítica, nota de segurança D): `PortaDeEntradaDasTelas` usava `getRequestedSessionId()`. A tela agora confere só se o cookie de sessão veio e se a sessão não vale mais, com o mesmo comportamento; teste novo em `PortaDeEntradaDasTelasTest` (4 casos).
- URL de tela fora da matriz passou a ser negada (`denyAll`), como manda a regra 13 do `AGENTS.md`.
- `./mvnw -B verify` nesta máquina: 1059 testes, 0 falhas, 0 ignorados, "All coverage checks have been met".
