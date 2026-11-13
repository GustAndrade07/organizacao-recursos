# Status — 13/11/2026 — `feat/politicas-site`

**Quem:** Leandro (com IA), na área do front-end (templates, layout, CSS, E2E) e com uma regra de segurança e um controller na área do **Pedro** (back-end): **Pedro revisa** e confere a regra `permitAll` e as linhas da matriz. A Spec foi aprovada pelo Leandro em 13/11/2026 e está no vault (`Projetos/Faculdade/Organizacao de recursos/Spec - Políticas do site`).

## O que mudou
- **Três páginas públicas**, abertas sem login (inclusive para quem está na tela de login): `GET /privacidade`, `GET /cookies`, `GET /termos` (telas 17 a 19 do inventário). Texto fixo nos templates, sem banco, sem migração, sem rota nova em `/api`.
- **Rodapé de página em todas as telas** (`layout.html`): `© <ano> Organização de Recursos` e os links Política de privacidade, Política de cookies e Termos de uso. O ano vem do `Clock` (componente `RodapeDoSite`, no fuso das telas), nunca do HTML. O `menu__rodape` (usuário e Sair, dentro do menu) continua como era.
- `SegurancaDasTelasConfig`: **uma** regra nova antes do `denyAll`, `GET` com os três caminhos exatos.
- `docs/seguranca/matriz-autorizacao.md`: 3 linhas novas (anônimo `ok`) e um parágrafo explicando por que são públicas de propósito.
- `AGENTS.md`, seção 9, regra 16: mudança que colete dado, crie cookie ou integre serviço externo atualiza as páginas de política no mesmo PR.
- `docs/plano-execucao.md` (uma caixa na Etapa 6), `docs/frontend/telas.md` (telas 17 a 19 e o rodapé), `changelog.d/feat-politicas-site.md`.

## O texto é rascunho técnico
Não é parecer jurídico. Os dados que o repositório não tem aparecem como **"a definir"**, em elemento marcado (`data-pendente`), e as páginas mostram o aviso de rascunho enquanto houver pendência: controlador, e-mail de contato, encarregado, prazo de resposta, prazo de guarda e foro. O PR 2 preenche quando o grupo responder (perguntas da Spec, seção 10).

**Política de cookies, a base da dispensa do banner:** o sistema só emite o `JSESSIONID` (medido com `curl` no `/login` e numa rota negada, em 13/11/2026; `HttpOnly`, `SameSite=Lax`; `Secure` só com `SESSAO_COOKIE_SEGURO=true`). Sem analytics, sem terceiros. Se isso mudar, o teste `PoliticasPublicasIntegracaoTest` fica vermelho e entra um banner.

## Decisões deste PR (minhas, para o grupo conferir)
- Caminhos em português, como o resto do site. Termos de uso incluídos (curtos).
- O texto fica no template, sem propriedade de configuração; invariante testado: aviso de rascunho ⇔ existe `data-pendente`.
- **Fora do PR:** `RF-15` e `RNF-04` no PRD/RTM (pergunta 11 da Spec, decisão do Pedro); log do administrador inicial com e-mail depois do DEF-010; CSP e HSTS.
- O E2E ganhou `redimensionar(largura)` e `semRolagemHorizontal(largura)` em `E2ETeste`, sem mexer nos testes de 375 px.
- CA-12 da Spec (inventário das colunas de dados pessoais contra o texto) **não foi feito**: ficou como opcional.
- CA-5 da Spec: a prova de "toda tela tem o rodapé" é a varredura de templates (`RodapeNosTemplatesTest`); o E2E confere uma amostra de 9 combinações de largura e tela, não as 28.

## Verificado
- `./mvnw -B test` em todo o pacote `br.com.senac.recursos.web`, **menos** as 3 classes que sobem Testcontainers (`TelasComBancoTest`, `PaginaNaoEncontradaIntegracaoTest`, `LimiteDeTentativasDoLoginIntegracaoTest`) e a nova `PoliticasPublicasIntegracaoTest`: **541 testes, 0 falhas, 0 erros, 0 ignorados**, `BUILD SUCCESS` (13/11/2026, JDK 21 portátil desta máquina). Inclui `PoliticasPaginaTest` (17), `RodapeNosTemplatesTest` (5), `RodapeDoSiteTest` (3), `ContrasteDasPoliticasTest` (3), os 2 testes novos do `LayoutBaseTest` e a varredura `TemplatesComTituloTest`.
- O vermelho antes do código (commit `test(red)`): 43 testes rodados nas 7 classes afetadas, 27 falhas e 2 erros; o vermelho vinha do código que ainda não existia. No caminho o `PoliticasPaginaTest` pegou um "(a definir)" solto no texto dos termos, e o `CssSemValoresSoltosTest` pegou um `font-weight` solto no CSS: os dois foram corrigidos.
- Na aplicação desta branch rodando local (porta 8081, PostgreSQL portátil), com `curl`: `GET /privacidade`, `/cookies` e `/termos` sem sessão respondem **200 e sem `Set-Cookie`**; `/login` continua criando o `JSESSIONID` (`HttpOnly; SameSite=Lax`); `/privacidade/` e `HEAD /privacidade` respondem 302. Confirma duas previsões da Spec.
- No navegador (390 e 1280 px): rodapé com direitos e três links na tela de login e nas páginas, página de cookies com o bloco do cookie legível em 390 px, **sem rolagem horizontal** (`scrollWidth` igual à largura em 390; 1265 de 1280 em 1280 px, a barra de rolagem). 768 px **não** foi aberto à mão (o E2E cobre no CI).

## Não verificado
- `./mvnw -B verify` completo e todo teste que sobe Testcontainers (**sem Docker nesta máquina**): `MatrizAutorizacaoTest`, `PoliticasPublicasIntegracaoTest` (200 sem cookie, bordas do matcher, POST, cookies emitidos) e `PoliticasE2ETest` (390/768/1280). O CI é a prova; o `PoliticasPublicasIntegracaoTest` foi escrito sem poder ser rodado e pode precisar de ajuste.
- Previsões da Spec que só esse teste confirma: que `/privacidade.html` e `/Privacidade` caem no `denyAll` (a barra final e o `HEAD` já foram vistos com `curl`) e o status de cada `POST`.
- Lighthouse e contraste computado no navegador (só os pares de token foram calculados); 768 px à mão.
- Specs do Notion (Front-end e Back-end): **a atualizar em até 24 h** (regra 7 da seção 9), pelo Leandro.
- Revisão jurídica do texto.
