# Status — 13/11/2026 — `feat/login-email`

**Quem:** pedido e decisão do Leandro (teste como usuário, 13/11/2026), feito com IA. Toca a área do Pedro (RF-01, back-end), do Gustavo (migração V2) e do Leandro (telas): **precisa do OK dos dois antes do merge** (D14 na seção 9 do plano). Revisão em roda: Leandro é revisado pelo Pedro.

**Fecha:** DEF-010 (login é nome de usuário, deveria ser e-mail) e a parte do usuário do DEF-005 (emoji em nome e e-mail). O arquivo `docs/defeitos/DEF-010.md` está no PR #65, não na `main`; quando o #65 entrar, o campo "Commit de correção" aponta para este PR.

## O que mudou
- **Cadastro** (`/admin/usuarios/novo`, `POST /api/usuarios`): nome completo, e-mail e senha, mais o perfil (continua escolhido pelo Administrador). Sem campo de repetir a senha. Só o Administrador cadastra, como antes.
- **Login** (`/login`, `POST /api/auth/login`): só e-mail e senha. Rótulo "E-mail", `type=email`, placeholder `nome@exemplo.com`. A mensagem de erro é uma só para e-mail inexistente, e-mail fora do formato e senha errada ("E-mail ou senha incorretos.").
- **E-mail:** minúsculas, sem espaços nas pontas, até 254 caracteres, formato estrito (`validation/Emails`), único sem diferenciar caixa. Normaliza em `UsuarioRequest`, `LoginRequest`, `UsuariosDaTela`, `AutenticacaoService` e `TentativasDeLogin` (o limite de tentativas conta `ANA@x.com` e `ana@x.com` como a mesma conta).
- **Nome completo (proposta):** pelo menos duas palavras, até 120 caracteres, só letras, espaço, apóstrofo, hífen e ponto; espaços repetidos viram um.
- **Sem emoji** em nome e e-mail: `@SemEmoji`, uma anotação só para API e tela; `SemEmojiVarreduraTest` descobre os campos de texto de `UsuarioRequest` por reflexão, sem lista.
- **V2** (`V2__login_e_email.sql`): índice único em `lower(login)` e `CHECK usuario_login_e_email` **`NOT VALID`** (vale só para gravações novas). Não converte, não apaga e **não inventa e-mail** de usuário antigo.
- **`ADMIN_LOGIN`** passa a ser e-mail e é validado com a mesma regra; valor inválido interrompe a subida com mensagem clara. O e-mail do administrador deixou de ir para o log.
- Documentação: README, roteiro da demo, roteiro de clone limpo (nota), `perf/` (CSV com e-mails), `docs/frontend/README.md`, ADR-0010 (**proposto**, substitui a parte do campo do ADR-0009 sem apagá-lo), D14 e D15 no plano, regra 17 do `AGENTS.md`, RTM e `status-requisitos.md`.

## Decisões deste PR que **não** são do grupo (propostas, ficam na D14)
1. **Regra do nome completo** (acima). O PRD não decide.
2. **Usuários antigos não são convertidos.** Escolhi recriar o banco local (`docker compose down -v`) ou recadastrar, em vez de um script que invente e-mail. Se o grupo preferir conversão automática, é migração nova (V3) com regra a combinar.
3. **O campo continua chamado `login`** no JSON, no formulário e em `ADMIN_LOGIN`; só o valor e o texto mudam. Evita quebrar contrato.

## Achados das revisões (segurança e código) e o que foi feito
- **Corrigido neste PR:** conta antiga sem e-mail nem é procurada no login (API e tela); `LoginRequest` limita e-mail a 254 e senha a 1024 caracteres; chave do contador cortada em 254; índice `lower(login)` agora provado sozinho (transação que tira o `CHECK`); teste de concordância Java × banco exige SQLState 23514 ou 23505; textos de `docs/frontend/README.md` e o typo do comando do JMeter; normalização repetida removida de `AutenticacaoService` e `UsuarioService`.
- **Fica como follow-up (já existia, não é desta mudança):** corrida no limite de tentativas (várias requisições simultâneas passam pela checagem antes da 5ª falha ser registrada; precisa de reserva atômica da tentativa e teste concorrente); teto de entradas do contador; e-mail no log do Hibernate numa corrida de cadastro do mesmo e-mail (não verificado em execução); duplicação de blocos de campo em `usuario-form.html` e dos guardas dos validadores (refatoração); nome padrão do administrador inicial ("Administrador", uma palavra) fora da regra do nome completo, que só vale no cadastro.
- **A V2 muda o nome da migração do Gustavo:** os documentos dele chamam de "V2" a futura (localização, patrimônio, `movimentacao_material`, D13); com esta ela vira V3. Combinar com ele e ajustar as menções.

## Pergunta em aberto
- **D15, auto-cadastro:** "quem já tem conta só entra" sugere cadastro público. Hoje só o Administrador cadastra; abrir cadastro público muda o modelo de permissões (falha de autorização é limitador de nota) e exige perfil fixo, limite de abuso e confirmação do e-mail. **Não implementado.**

## Verificado (13/11/2026, JDK 21 e PostgreSQL 16 portáteis, **sem Docker**)
Os testes que sobem Testcontainers (`ApiIntegracaoTest`, `BancoIntegracaoTest`, Cucumber) rodaram contra o PostgreSQL 16 local trocando só o contêiner (um arquivo local, fora do git); o resto é o código do PR.
- **Vermelho, depois verde (regra 11):** `bd09e77` 161 testes sem banco, 78 falhas e 3 erros; 343 testes com banco, 41 falhas. `1e60787` (revisão de segurança) 65 testes, 4 falhas. O `feat(green)` zera as falhas.
- **Suíte inteira, exceto os E2E e 3 classes com contêiner próprio:** 1209 testes, **0 falhas**. 364 erros eram `FATAL: too many clients already` do PostgreSQL portátil (100 conexões, vários contextos Spring com pool de 30); as 12 classes afetadas rodaram de novo em 3 lotes: 26, 226 e 68 testes, **0 falhas, 0 erros, 0 ignorados** (inclui `MatrizAutorizacaoTest`, 222 casos, e `TelasComBancoTest`).
- **Depois do endurecimento da revisão de segurança:** 16 classes tocadas pelo PR (validação, requests, serviços, telas, API, banco, V2, administrador inicial): **261 testes, 0 falhas, 0 erros, 0 ignorados**.
- **No navegador (Chromium do Playwright, aplicação local contra um banco novo, 390 px e 1280 px):** login com `  ADMIN@Exemplo.com ` entra; cadastro com nome "Bia 😀" e e-mail `bia@exemplo` volta com "Não use emojis neste campo." e "Informe um e-mail válido, como nome@dominio.com."; cadastro de "  Bia   Campos " / `BIA.Campos@Exemplo.com` volta à lista com a confirmação; repetido com outra caixa dá "Já existe um usuário com este e-mail."; a Bia entra com `bia.campos@EXEMPLO.com`; e-mail inexistente e senha errada dão o mesmo `/login?erro` e o mesmo texto; um só campo de senha.
- **No banco (psql):** `senha_hash` BCrypt (`$2a$`, 60 caracteres), logins em minúsculas, V1 e V2 aplicadas com sucesso, `usuario_login_e_email` com `convalidated = f` (`NOT VALID`), `INSERT` de e-mail em maiúsculas recusado pelo `CHECK`.
- **Na API (curl):** login antigo sem `@` dá 401; e-mail com 300 caracteres dá 400.
- **Não medido:** tempo de resposta do login entre os casos (e-mail inexistente × senha errada).

## Não verificado
- `./mvnw -B verify` completo: precisa de Docker. **Só o CI roda:** os E2E `*E2ETest` (incluindo os dois testes de cadastro/entrada que mexi e o novo `cadastroRecusaEmoji...`), `AplicacaoSmokeTest`, `LimiteDeTentativasDoLoginIntegracaoTest`, `PaginaNaoEncontradaIntegracaoTest`, o gate de cobertura do JaCoCo e o Quality Gate do SonarCloud.
- Specs do Notion (Banco de Dados, Back-end, Front-end e principal): **não atualizadas neste PR**; o texto de cada uma está abaixo, para colar no prazo de 24 h (regra 7 do `AGENTS.md`).
- Aprovação do grupo para D14 e D15.

## Texto para as specs do Notion
- **Banco de Dados:** `usuario.login` agora guarda e-mail em minúsculas; V2 com índice único em `lower(login)` e `CHECK` `NOT VALID` (só gravações novas); sem conversão de usuário antigo.
- **Back-end:** cadastro com nome completo, e-mail e senha (+ perfil); `LoginRequest`/`UsuarioRequest` normalizam o e-mail; `ADMIN_LOGIN` é e-mail e é validado; mensagem de login única para e-mail inexistente e senha errada; limite de tentativas por e-mail normalizado.
- **Front-end:** tela de login com campo "E-mail" (`type=email`); cadastro com "Nome completo", "E-mail", "Senha" e "Perfil", sem repetir senha; mensagens "Não use emojis neste campo." e "Informe um e-mail válido, como nome@dominio.com.".
- **Principal:** RF-01 passa a dizer que o login é o e-mail; auto-cadastro fora do escopo (D15).
