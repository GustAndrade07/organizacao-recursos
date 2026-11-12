# Status em 12/11/2026 (quinta, branch `satoworktree/laughing-mayer-yvtj0y`, Leandro)

**Limite de tentativas no login das telas (ressalva 1 do Gustavo no PR #48, ADR-0009), em TDD.**
- 5 falhas do mesmo login dentro de 15 minutos (janela deslizante) bloqueiam esse login por 15 minutos, contados do instante da 5ª falha pelo `Clock` injetado.
- Login certo zera o contador. Tentativa durante o bloqueio não estende o bloqueio.
- O bloqueado recebe a mesma resposta e a mesma tela de uma senha errada (`/login?erro`). A senha é conferida do mesmo jeito, para o tempo de resposta não revelar o bloqueio.
- Login inexistente também conta.
- Propriedades `recursos.telas.login.tentativas` (5) e `recursos.telas.login.bloqueio` (`PT15M`).
- Contador em memória, para uma instância só (limitação escrita no ADR-0009).
- Código: `web/sessao/TentativasDeLogin`, `web/sessao/LoginComLimite` e `config/SegurancaDasTelasConfig`, com um `ProviderManager` sem pai (regra 15 nova no `AGENTS.md`).

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
- o run do CI deste PR;
- o comportamento com mais de uma instância (o contador não é compartilhado, limitação aceita);
- o tempo de resposta do bloqueado comparado com o de uma senha errada (não medido; a igualdade vem de a senha ser conferida nos dois casos, provada só pelo número de chamadas em `LoginComLimiteTest`);
- o E2E no navegador do bloqueio (não escrito; o teste com servidor real cobre o HTTP).

**Fica aberto:** a API (`POST /api/auth/login`, área do Pedro) não tem o limite. A proposta é usar o mesmo `TentativasDeLogin` no `AutenticacaoService`, e o Leandro combina isso com o Pedro.

**Specs atualizadas:** Notion, Specs do Front-end, página "01 · Acesso e navegação" (critério de aceite do limite de tentativas), 12/11/2026.
