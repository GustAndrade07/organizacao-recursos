# Status em 12/11/2026 (quinta, branch `satoworktree/laughing-mayer-yvtj0y`, Leandro)

**Limite de tentativas no login das telas (ressalva 1 do Gustavo no PR #48, ADR-0009), em TDD.**
- 5 falhas do mesmo login dentro de 15 minutos (janela deslizante) bloqueiam esse login por 15 minutos, contados do instante da 5ª falha pelo `Clock` injetado.
- Login certo zera o contador. Tentativa durante o bloqueio não estende o bloqueio.
- O bloqueado recebe a mesma resposta e a mesma tela de uma senha errada (`/login?erro`). A senha é conferida do mesmo jeito, para o tempo de resposta não revelar o bloqueio.
- Login inexistente também conta.
- Propriedades `recursos.seguranca.login.tentativas` (5) e `recursos.seguranca.login.bloqueio` (`PT15M`). Os nomes mudaram no segundo ciclo, quando o limite passou a valer também para a API.
- Contador em memória, para uma instância só (limitação escrita no ADR-0009).
- Código: `service/TentativasDeLogin` (era `web/sessao` no primeiro ciclo), `web/sessao/LoginComLimite` e `config/SegurancaDasTelasConfig`, com um `ProviderManager` sem pai (regra 15 nova no `AGENTS.md`).

**Decisões combinadas com o Leandro antes do código:**
- janela de 15 minutos para contar as falhas;
- tentativa no bloqueio não estende o bloqueio;
- `test(red)` só com o teste pelo HTTP, e os testes de unidade no `feat(green)`, porque uma classe nova no teste do red impede a compilação e não sai número de teste.

**Commits.**
- `test(red)`: só `LimiteDeTentativasDoLoginIntegracaoTest` (7 testes). OK do Leandro dado antes, pela regra 11. `./mvnw -B verify` nesta máquina deu **1092 testes, 4 falhas, 0 erros, 0 ignorados** (BUILD FAILURE). As 4 falhas são do teste novo, todas porque a senha certa entrava (302 para `/`) quando o login deveria estar bloqueado.
- `feat(green)`: a implementação, `TentativasDeLoginTest` (10) e `LoginComLimiteTest` (4). `./mvnw -B verify` nesta máquina deu **1106 testes, 0 falhas, 0 erros, 0 ignorados**, "All coverage checks have been met". Cobertura de `target/site/jacoco/jacoco.csv`: 95,8% das linhas (1459 de 1523) e 81,2% dos branches (420 de 517). `TentativasDeLogin`, `LoginComLimite` e `SegurancaDasTelasConfig` ficaram sem linha nem branch descoberto.

**Mutações feitas à mão, todas pegas e desfeitas** (lista em `docs/rtm.md`): 7.
- M1, não contar a falha: 6 testes vermelhos.
- M6, provedor com o gerenciador global de pai: 4 vermelhos, só no teste com servidor real.

**Documentos:**
- ADR-0009: ressalva 1 resolvida, com as limitações.
- `docs/relatorio-qualidade.md`: risco da seção 6 atualizado e exemplo de red → green na seção 5.
- `docs/rtm.md`: RF-01 e as mutações.
- `docs/frontend/README.md`: seção "Login".
- `docs/status-requisitos.md`: RF-01.
- `docs/plano-execucao.md`: linha da D12.
- `AGENTS.md`: regra 15.
- Changelog em `changelog.d/satoworktree-laughing-mayer-yvtj0y.md`.

**Não verificado:**
- o run do CI do segundo ciclo (API). O primeiro ciclo (telas, commit `e86c777`) passou no CI: `verify` no run 43293042621 e Quality Gate do SonarCloud aprovado;
- o comportamento com mais de uma instância (o contador não é compartilhado, limitação aceita);
- o tempo de resposta do bloqueado comparado com o de uma senha errada (não medido; nas telas, a conferência da senha é provada só pelo número de chamadas em `LoginComLimiteTest`; na API, por nenhum teste);

**Segundo ciclo: o limite também na API (`POST /api/auth/login`).** O Leandro pediu e autorizou a mudança na área do Pedro sem esperar por ele; o Pedro revisa o PR.
- **Um contador só para as duas portas.** `TentativasDeLogin` foi para `service`, como bean em `SegurancaConfig`. Falhar na tela conta na API, e vice-versa.
- **No `AutenticacaoService`:** o hash é conferido sempre; login bloqueado lança a mesma `CredenciaisInvalidasException`, com o mesmo 401 e o mesmo corpo de uma senha errada (não 429); a falha conta e o sucesso zera.
- **`test(red)`:** só `LimiteDeTentativasApiTest` (7 testes). `./mvnw -B verify` deu **1113 testes, 5 falhas, 0 erros, 0 ignorados**, todas porque o login bloqueado ainda entrava.
- **`feat(green)`:** `./mvnw -B verify` deu **1113 testes, 0 falhas, 0 erros, 0 ignorados**, "All coverage checks have been met". Linhas 95,8% (1464 de 1528) e branches 81,3% (422 de 519); `AutenticacaoService` e `TentativasDeLogin` sem linha nem branch descoberto.
- **4 mutações na API, todas pegas** (lista na RTM). **Não pega:** conferir o bloqueio antes do hash, que só muda o tempo de resposta.

**E2E do bloqueio (Playwright, pedido do Leandro em 12/11/2026).** `LimiteDeTentativasE2ETest`, 3 testes em 375 px contra o back-end real, com o `RelogioDeTeste` num contexto próprio do Spring (as outras classes E2E usam o relógio de verdade):
- o bloqueado vê a mesma tela de senha errada, mesmo com a senha certa: o mesmo aviso, com foco, o login digitado e a senha em branco, sem a palavra "bloqueado";
- depois de 15 minutos no relógio controlado, a senha certa entra pelo formulário;
- 5 falhas pela API (feitas pelo próprio navegador) bloqueiam o formulário.

O print está em `docs/evidencias/e2e-375/01-login-bloqueado.png`. Mutações contra o E2E, todas pegas: a tela não conta a falha (2 vermelhos), a API não conta a falha (1), provedor com o gerenciador global de pai (3).

Uma interrupção no meio da última mutação deixou o `SegurancaDasTelasConfig` mutado na máquina. Isso foi achado com `git diff` e restaurado com `git checkout` antes do `verify`, sem chegar a commit. Depois, `./mvnw -B verify` nesta máquina deu **1116 testes, 0 falhas, 0 erros, 0 ignorados**, "All coverage checks have been met". Testes escritos depois do código, não em TDD: o E2E confere o comportamento no navegador, e o red → green deste limite está nos testes de integração.

**Fica aberto:** contador em memória (uma instância só), sem limite por IP, bloqueio de um login por quem o conhece, e tempo de resposta não medido. Tudo aceito no ADR-0009 e no relatório de qualidade.

**Specs atualizadas:** Notion, Specs do Front-end, página "01 · Acesso e navegação" (critério de aceite do limite de tentativas, atualizado para telas e API), 12/11/2026.
