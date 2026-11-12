# ADR-0009 — Telas autenticam por formulário com sessão e chamam os services

- **Status:** **aceito** pelo Gustavo na revisão do PR #48 (12/11/2026), "aprovado com ressalvas", confirmando a opção (a) como combinada com o grupo. Antes: proposto (Leandro, 12/11/2026). Escolha da D12 (seção 9 do plano) pela opção (a), informada pelo Leandro como combinada com o grupo; vira **aceito** na revisão do PR (regra 6 do `AGENTS.md`). **Não substitui** o [ADR-0001](0001-login-jwt.md) (JWT da API) nem o [ADR-0008](0008-backend-api-rest-em-camadas.md); completa o que o ADR-0008 deixou aberto ("Ligar as telas à API é trabalho seguinte, junto com a D12").
- **Contexto:** depois do PR #44 as telas Thymeleaf estavam abertas a todos e sem dado real (ADR-0008, "Perdemos"), e a API exigia JWT no cabeçalho. Para as telas mostrarem dados era preciso decidir como a pessoa entra e como o perfil é conferido (D12): (a) formulário em `/login` com sessão do Spring Security, ou (b) o JWT da API guardado num cookie `HttpOnly`.
- **Decisão:**
  - **Opção (a).** `POST /login` (campos `login` e `senha`) é atendido pelo Spring Security numa cadeia própria (`config/SegurancaDasTelasConfig`), com sessão HTTP, CSRF ligado e `POST /logout`. O usuário vem da mesma tabela e do mesmo hash BCrypt da API (`web/sessao/UsuariosDaTela`).
  - **As telas chamam os services do back-end no mesmo processo**, não a API por HTTP. A regra continua toda no service e no banco (posse no `ReservaService`, 409 da constraint de exclusão); o controller da tela só converte o formulário (data e hora no fuso `America/Sao_Paulo`, propriedade `recursos.telas.fuso-horario`) e monta o modelo do template.
  - **Perfil por rota espelhando a API**: cada linha da tabela "Telas" de `docs/seguranca/matriz-autorizacao.md` tem a mesma permissão da linha da API que a tela usa. Sem sessão a tela leva ao login (`?destino=` no `GET`, `?expirou` quando o cookie de sessão venceu); perfil sem permissão recebe a página 403. URL fora da tabela é negada (`denyAll`, regra 13 do `AGENTS.md`): sem sessão leva ao login, com sessão 403.
  - **Depois do login**, o `destino` do formulário é revalidado com `CaminhoDoSite` antes do redirecionamento; sem destino válido, `/` leva à primeira tela do menu do perfil. Na falha, volta a `/login?erro` com o login digitado em atributo flash, nunca a senha.
  - **O mesmo `UsuarioAutenticado`** chega aos services pela API (do JWT) e pela tela (de `UsuarioDaSessao`), pelo resolvedor de `config/WebConfig`.
- **Consequências:**
  - **Ganhamos:**
    - As 15 telas mostram e gravam dado real, com a mesma regra da API (nada duplicado na tela).
    - CSRF de verdade nas telas: a sessão vai em cookie, e todo formulário leva o `_csrf` (o Spring já colocava o campo desde o PR #44).
    - O formulário de login que existia (`login.html`, `login.js`) continua; só o campo passou de "E-mail" para "Login", porque o cadastro aceita `[a-z0-9._-]` e um `type=email` recusava esses logins.
  - **Perdemos:**
    - **Duas formas de autenticação no mesmo sistema** (JWT na API, sessão nas telas): duas cadeias para manter e testar.
    - Sessão no servidor: se um dia houver mais de uma instância, a sessão precisa ser compartilhada (não medido nem previsto para a entrega).
    - O esquema ainda não guarda localização, patrimônio, competências nem o motivo do bloqueio (V2). As telas mostram "Não informado" e o que for digitado nesses campos **não é gravado**.
    - A contagem de reservas por linha do relatório é feita na tela (reservas que ocupam horário e cruzam o período, a mesma regra das consultas), porque o service devolve só as horas.
  - **Exige de testes:**
    - Matriz de autorização das telas célula a célula com o usuário de cada perfil na sessão e o CSRF em todo `POST` (`MatrizAutorizacaoTest`, 210 casos com a API).
    - Cada tela contra PostgreSQL real, conferindo HTML **e banco** (`web/TelasComBancoTest`).
    - Login de ponta a ponta com servidor real: senha errada, destino revalidado, 403 fora da matriz e 404 de reserva inexistente com sessão (`PaginaNaoEncontradaIntegracaoTest`), e no navegador em 375 px (`AcessoE2ETest`, `ReservaE2ETest`, `ResponsavelEAdministradorE2ETest`).
    - Os recortes `@WebMvcTest` usam services dublês sem resposta programada (`support/TelasNoRecorte`) e entram com `@ComoUsuario`.
- **Ressalvas da revisão (Gustavo, PR #48):**
  1. **Sem limite de tentativas no `/login`.** Fica como risco registrado (relatório de qualidade, seção 6); não implementado.
  2. **Cookie de sessão sem `Secure`/`SameSite`.** Corrigido no PR seguinte: `SameSite=Lax` e `HttpOnly` em `application.yml`, conferidos por `PaginaNaoEncontradaIntegracaoTest`; `Secure` liga com `SESSAO_COOKIE_SEGURO=true` (exige HTTPS, desligado no desenvolvimento local).
  3. **Testes e código no mesmo commit, sem o `test(red)`.** Registrado como desvio de processo no relatório de qualidade, seção 5.
- **Pendências:** D8 (premissas do relatório), D9 (quando alterar e cancelar), D10 (estado inicial e quem cancela reserva de outra pessoa), D11 (manutenção sobre reservas existentes) e D13 (RN-04) continuam pendentes na seção 9 do plano; as telas seguem o comportamento atual do service. A lista de itens do menu por perfil é a proposta de `docs/frontend/README.md` ("Layout base e menu"). Aceite do grupo para a opção (a) na revisão do PR.
- **Fonte:** `src/main/java/br/com/senac/recursos/config/SegurancaDasTelasConfig.java`, `src/main/java/br/com/senac/recursos/web/`, `docs/seguranca/matriz-autorizacao.md`, `docs/plano-execucao.md` (seção 9, D12).
