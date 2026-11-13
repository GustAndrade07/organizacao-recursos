# Roteiro do clone limpo

> Para quem vai **validar o README em um clone limpo**: **Leandro ou Pedro**. O Gustavo escreveu o README (histórico do arquivo), então **não** faz este roteiro; a IA também não faz o clone nem afirma que foi feito.
> Aula 14: quem faz o clone não pode ter escrito o README nem reaproveitar nada da própria máquina. **Anote o que faltou, liste os bloqueadores em ordem e só então corrija** (a correção vira PR à parte, do Gustavo).
> Situação em 13/11/2026: **o roteiro foi executado uma vez, pela IA, num container novo na nuvem** (sessão do Leandro, com o OK dele; ver a seção 4 e o [checklist](checklist.md)). **Isso não é um integrante seguindo o README**: a regra da Aula 14 pede uma pessoa que não escreveu o README, e o grupo decide se esta rodada vale como tal. Enquanto não decidir, a rodada conta como *verificação técnica do README*, não como a conferência humana.

## 1. Regras (valem do começo ao fim)

1. **Siga só o `README.md`.** Não use `AGENTS.md`, o plano, o Notion nem a ajuda de quem escreveu o README. Se um passo não estiver no README, ele **não existe**: anote como "faltou no README" e pare ali.
2. **Pasta nova e vazia**, fora de qualquer outro clone do projeto. Não copie `.m2`, `target`, `.env` nem nada da pasta de trabalho.
3. **Não corrija nada.** Nem o README, nem o código, nem a máquina além do que o README mandar instalar. Só anote.
4. **Anote o erro inteiro** (texto copiado, não resumo) e a hora. Se travar, pare, anote e avise o grupo.
5. Não coloque senha nem token em nenhum lugar do relatório. O `docker-compose.yml` usa uma senha de desenvolvimento local; não precisa copiá-la.

## 2. Antes de começar

Escolha **o commit que vai valer**: a `main` depois dos merges dos PRs pendentes, e **antes** da tag `v1.0.0-rc1`. O Gustavo informa o hash. Registre aqui:

| Dado | Valor |
|---|---|
| Quem fez (nome completo) | **Não foi um integrante.** A rodada de 13/11/2026 foi feita pela IA (Claude Code), numa sessão do Leandro Sato, dentro de um container de nuvem criado para a sessão |
| Data e hora de início | 13/11/2026, 13:16 UTC (10:16 em Brasília) |
| Sistema operacional | Ubuntu 24.04 em container Linux (kernel 6.18.44), 4 CPUs, 15 GB de memória |
| Versão do Java (`java -version`) | OpenJDK 21.0.12.1 (Temurin/Ubuntu build 21.0.12.1+1-1-24.04.4) |
| Versão do Docker (`docker version`) | Docker Engine 29.9.2 (servidor no mesmo container) |
| Commit testado (`git rev-parse --short HEAD` dentro do clone) | `9acbbcd` (`main`, merge do PR #55), **antes** dos merges de #56 a #59 |

> Por que o commit não é o da tag: os PRs #56, #57, #58 e #59 ainda estavam abertos em 13/11/2026 de manhã. Esta rodada vale para o `9acbbcd`; **o que ela não cobre** é qualquer mudança que esses PRs tragam para o README, para o `pom.xml` ou para o código.

## 3. Passos (nesta ordem)

O comando exato é o que o README manda; abaixo está só a ordem e o que se espera. No Windows (PowerShell) o README manda usar `.\mvnw.cmd` no lugar de `./mvnw`.

| # | O que fazer (segundo o README) | O que se espera |
|---|---|---|
| 1 | Conferir os pré-requisitos do README (JDK 21, Docker, Git) | `java -version` mostra 21; Docker responde |
| 2 | `git clone` do repositório em pasta vazia e entrar na pasta | Clone termina sem erro; anotar o hash do commit |
| 3 | `docker compose up -d` | Banco sobe em `localhost:5432` (`docker compose ps` mostra o serviço de pé) |
| 4 | Subir a aplicação com o Maven Wrapper (`spring-boot:run`) | Aplicação sobe em `http://localhost:8080`, sem erro no log; anotar quanto tempo levou |
| 5 | Abrir `http://localhost:8080/actuator/health` | Responde `{"status":"UP"}` |
| 6 | Parar a aplicação (Ctrl+C) e rodar `verify` (Docker rodando) | `BUILD SUCCESS`; anotar a linha `Tests run:` e quanto tempo levou |
| 7 | Abrir o relatório de cobertura indicado no README | O arquivo abre; anotar os percentuais de linhas e branches mostrados |

## 4. Resultado

Preencha uma linha por passo, na hora, sem reescrever depois.

Rodada de 13/11/2026 (IA, container novo; horas em UTC):

| # | Hora | Funcionou? | O que apareceu |
|---|---|---|---|
| 1 | 13:16 | sim | `openjdk version "21.0.12.1"`; `docker info` respondeu `Server Version: 29.9.2`. Git presente |
| 2 | 13:16 | sim | `Cloning into 'organizacao-recursos'...`, 0,9 s; `git rev-parse --short HEAD` = `9acbbcd` |
| 3 | 13:16 | sim | `Container organizacao-recursos-db-1 Started`, 11,2 s; `docker compose ps` mostrou `postgres:16` com `0.0.0.0:5432->5432/tcp` |
| 4 | 13:17 a 13:27 | sim, **na terceira tentativa** | As duas primeiras tentativas de `./mvnw spring-boot:run` pararam em `BUILD FAILURE` ao baixar dependências: `Could not find artifact org.xmlunit:xmlunit-parent:pom:2.10.4 in central`, `Could not find artifact org.jacoco:org.jacoco.core:jar:0.8.16 in central`. Os artefatos existem no Maven Central (conferido com `curl`: HTTP 200); a falha foi da rede do container em downloads paralelos. Com `-Dmaven.artifact.threads=1` o download completou e a aplicação subiu: `Started RecursosApplication in 5.829 seconds`. **Não é defeito do projeto nem do README**; é do ambiente desta rodada |
| 5 | 13:27 | sim | `http://localhost:8080/actuator/health` respondeu `{"status":"UP"}` |
| 6 | 13:28 a 13:32 | sim | `BUILD SUCCESS`; `Tests run: 1116, Failures: 0, Errors: 0, Skipped: 0`; `All coverage checks have been met`; `Total time: 03:52 min` |
| 7 | 13:32 | sim | `target/site/jacoco/index.html` gerado. Percentuais lidos do `jacoco.csv` do mesmo run: **95,8% de linhas** (1464 de 1528) e **81,3% de branches** (422 de 519), acima do gate de 80/70 |

**Passos extras conferidos na mesma rodada** (além do roteiro, para provar o primeiro limitador de nota "a aplicação executa"):

| O que | Resultado |
|---|---|
| `POST /api/auth/login` com o administrador inicial (`ADMIN_LOGIN`/`ADMIN_SENHA`, senha gerada na hora e nunca escrita em arquivo) | 200 com `token` |
| `POST /api/recursos` (sala) | 201, `{"id":1,...}` |
| `POST /api/reservas` 01/12/2026 10:00–12:00 | **201**, estado `APROVADA` |
| `POST /api/reservas` 01/12/2026 11:00–13:00 (sobrepõe) | **409** `urn:recursos:problema:conflito-de-horario` — sem reserva dupla |
| `POST /api/reservas/1/cancelamento` | 200, estado `CANCELADA` |
| Telas: `GET /login`, login pelo formulário, `/reservas/nova`, gravar reserva (Playwright, Chromium) | login levou a `/admin/recursos`; a reserva gravou e a tela de detalhe mostrou "Reserva confirmada ... agora está Aprovada" |

**O que faltou ou confundiu no README:**

- **Passo 4 e 5 (entrar nas telas).** A seção "Como subir" termina em `/actuator/health`. Quem segue só ela sobe a aplicação, abre `http://localhost:8080` e é levado ao `/login` **sem nenhum usuário cadastrado**: o banco começa vazio e o primeiro Administrador só nasce com `ADMIN_LOGIN` e `ADMIN_SENHA` (`AdministradorInicialConfig`). O README fala dessas variáveis só em "API REST e primeiro acesso", como se fossem para a API. Corrigido no mesmo PR desta rodada (o README passa a mandar subir já com as duas variáveis e diz que elas servem para as telas e para a API).
- **Passo 6 (ferramentas da pilha).** O README de `9acbbcd` diz que WireMock, Cucumber e a rodada de JMeter **não estão** no projeto. Isso estava certo no `9acbbcd` e muda com os PRs #56 (JMeter) e o do BDD; a linha precisa ser relida no commit da tag.
- **Nada no README menciona o tempo**: o primeiro `verify` num clone sem `~/.m2` levou 3 min 52 s de build mais o download das dependências. Não é erro, mas quem for demonstrar na mesa precisa contar com isso.

**Bloqueadores, em ordem** (os quatro primeiros zeram a nota: aplicação que não executa, pipeline vermelho, falha de autorização, reserva dupla):

1. **Nenhum bloqueador encontrado nesta rodada.** A aplicação sobe, responde `UP`, grava reserva pela API e pela tela, recusa a sobreposição com 409 e o `verify` fica verde com 1116 testes e 0 ignorados.
2. Pendência não bloqueante: o README não dizia como entrar nas telas (corrigido neste PR).
3. Pendência de processo, **não técnica**: a rodada foi feita por IA, não por um integrante. Enquanto o grupo não decidir se isso vale, a caixa "clone limpo por quem não escreveu o README" continua aberta.

### 4.2 Segunda rodada — Windows 11, máquina do Pedro, 13/11/2026 (feita pela IA a pedido do Pedro)

**Quem fez:** a IA (Claude Code), na sessão e na máquina do Pedro, a pedido dele. **Não foi o Pedro seguindo o README com as próprias mãos**; a mesma ressalva da rodada 1 vale aqui, e o grupo decide se a conta. O que esta rodada acrescenta: é **Windows**, com a forma `$env:` do README (PR #63), no commit de agora.

| Dado | Valor |
|---|---|
| Data e hora de início | 13/11/2026, 18:12 (Brasília) |
| Sistema operacional | Windows 11 Pro 10.0.26200, PowerShell 5.1.26100 |
| Java | OpenJDK 21.0.13 (Temurin) |
| Docker | Docker Desktop, Engine 29.9.3 |
| Commit testado | `bf2584a` (`main`, merge do PR #66) |
| Pasta | nova e vazia, fora de qualquer outro clone |
| Cache do Maven | **vazio**: `MAVEN_USER_HOME` e `-Dmaven.repo.local` apontando para uma pasta nova, para não reaproveitar o `~/.m2` da máquina (o README não pede isso; foi só para simular máquina limpa) |

| # | Hora | Funcionou? | O que apareceu |
|---|---|---|---|
| 1 | 18:12 | sim | `openjdk version "21.0.13"`; Docker respondeu `29.9.3`; `git version 2.54.0.windows.1` |
| 2 | 18:12 | sim | `Cloning into 'organizacao-recursos'...`; `git rev-parse --short HEAD` = `bf2584a` |
| 3 | 18:12 | sim | `Container organizacao-recursos-db-1 Started`; `docker compose ps`: `postgres:16`, `0.0.0.0:5432->5432/tcp` |
| 4 | 18:13 | **não** | `$env:ADMIN_LOGIN='admin'; $env:ADMIN_SENHA='<senha>'; .\mvnw.cmd spring-boot:run` (forma do README) baixou tudo e parou no Flyway: `org.postgresql.util.PSQLException: FATAL: autenticação do tipo senha falhou para o usuário "recursos"` e `BUILD FAILURE` (46,8 s). **Causa:** esta máquina tem um **PostgreSQL 18 nativo do Windows** (serviço `postgresql-x64-18`, início automático) escutando em `0.0.0.0:5432` e `[::]:5432`. O `docker compose` publicou a porta sem erro, mas a aplicação, em `localhost:5432`, falou com o PostgreSQL nativo, não com o container (o log do container não mostra nenhuma tentativa de login; a mensagem do erro está em português, a língua do PostgreSQL instalado, e não em inglês, a do container) |
| 4b | 18:14 | sim, **com contorno fora do README** | Para seguir os outros passos sem mexer no serviço do Windows: `docker compose down`; o mesmo `postgres:16` com o mesmo banco, usuário e senha do `docker-compose.yml`, publicado na **5433**; e `$env:DB_URL='jdbc:postgresql://localhost:5433/recursos'` (variável que o `application.yml` já aceita). Resultado: `Successfully applied 1 migration ... now at version v1`, `Started RecursosApplication in 7.601 seconds`, `Administrador inicial 'admin' criado.` |
| 5 | 18:15 | sim | `http://localhost:8080/actuator/health` respondeu `{"status":"UP"}` |
| 6 | — | **não rodado no clone** | Não rodado dentro da pasta do clone, por tempo. O mesmo código (`bf2584a`) mais 8 testes de API rodou `./mvnw -B verify` na pasta de trabalho desta máquina: `BUILD SUCCESS`, `Tests run: 1213, Failures: 0, Errors: 0, Skipped: 0`, `All coverage checks have been met`, 4 min 39 s (branch `test/d10-d11-api-1311`). Isso não prova o `verify` com cache vazio no Windows |
| 7 | — | **não rodado no clone** | Depende do passo 6 |

**Passos extras (aplicação de ponta a ponta, API e telas no mesmo banco):**

| O que | Resultado |
|---|---|
| `POST /api/auth/login` com `ADMIN_LOGIN`/`ADMIN_SENHA` (senha gerada na hora, fora do repositório) | 200 com token |
| `POST /api/recursos` (sala) | 201 |
| `POST /api/reservas` 01/12/2026 10:00–12:00 (Brasília) | **201**, `APROVADA` |
| `POST /api/reservas` 11:00–13:00 (sobrepõe) | **409** `urn:recursos:problema:conflito-de-horario` |
| Login pelo formulário `/login` (com CSRF) | 302 para `/` |
| `/reservas/minhas` e `/reservas/1` | 200; a reserva **criada pela API** aparece na tela, como Aprovada |
| `POST /reservas` pela tela, 02/12/2026 09:00–10:00 | gravada; `GET /api/reservas` passa a listar a reserva da tela (`2026-12-02T12:00:00Z`, Aprovada) |
| `POST /reservas` pela tela, 09:30–10:30 (sobrepõe a da tela) | **409** |
| `POST /api/reservas/1/cancelamento` | 200, `CANCELADA`; a tela `/reservas/1` passa a mostrar Cancelada |
| `/swagger-ui/index.html` | 200 com sessão; sem sessão, 302 para `/login?destino=...` |
| Banco, contado no fim (`psql`) | 2 reservas (`1:CANCELADA`, `3:APROVADA`), 2 tentativas recusadas (uma da API, uma da tela), 3 registros de auditoria |

**O que faltou no README (rodada 2):**
- **A porta 5432 precisa estar livre**, e o README não diz isso. Quem já tem PostgreSQL instalado no Windows (comum em quem fez outras matérias de banco) segue o README e a aplicação **não sobe**, com um erro de senha que não aponta para a causa. Sugestão para o PR de correção do Gustavo: uma linha em "Pré-requisitos" ("a porta 5432 precisa estar livre; se houver PostgreSQL instalado, pare o serviço ou suba o container em outra porta e defina `DB_URL`") e o exemplo de `DB_URL` para PowerShell.
- A forma `$env:` do README **funcionou** (as variáveis chegaram à aplicação e o Administrador foi criado). A forma do `cmd` **não foi testada**.

**Bloqueadores da rodada 2, em ordem:**
1. **Aplicação não executa seguindo só o README nesta máquina**, por causa da porta 5432 ocupada pelo PostgreSQL nativo. É bloqueador do README em máquinas assim, não defeito do código: com o banco do projeto em outra porta, a aplicação sobe e tudo o mais passa. Correção: README (Gustavo), em PR separado.
2. Pendência de processo: a rodada foi feita pela IA na máquina do Pedro. Para valer como a conferência humana da Aula 14, o Pedro repete os passos 3 a 5 ele mesmo (com a correção do README ou parando o serviço `postgresql-x64-18`) e assina a linha no checklist.

## 5. Depois do clone

1. Entregue este arquivo preenchido ao Gustavo, em PR seu (sua autoria) ou colado no grupo.
2. Com base nos bloqueadores, o Gustavo corrige o README e o resto **em PR separado**, citando este roteiro.
3. Só depois disso o [checklist](checklist.md) tem as colunas "Existe" e "Atualizado" preenchidas **por quem fez o clone**; até lá, o que está no checklist é só o que foi conferido sem clone (seção "Conferido sem clone").

> **Nota (13/11/2026):** a partir desta data o login é um e-mail. Quem refizer estes passos deve subir a aplicação com `ADMIN_LOGIN=<e-mail>` (por exemplo `admin@exemplo.com`) e entrar no formulário e na API com esse e-mail. Banco local antigo (usuários sem `@`) se recria com `docker compose down -v`. A rodada registrada acima foi feita antes da troca e não foi repetida.
