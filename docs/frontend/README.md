# Front-end

Telas em Thymeleaf no mesmo projeto Spring ([ADR-0003](../adr/0003-thymeleaf.md)). Dono: Leandro.

## Onde está cada coisa

| O quê | Onde | Observação |
|---|---|---|
| Specs (visão geral, problema, objetivos de negócio, critérios de aceite) | Notion: [Specs do Front-end](https://app.notion.com/p/40e8bb977b7b81dbbcbadb904897cbb9), dentro da [Spec do projeto](https://app.notion.com/p/40e8bb977b7b8155b440c12880c46df1) | Ficam **só no Notion**, não no repositório nem no Figma |
| Design System (tokens, estilos, componentes) | Figma, página **Design System** | Fonte da verdade dos tokens |
| Telas (375 px e 1280 px) | Figma, página **Telas** | Uma seção por módulo e por largura |
| Tokens em CSS | [`src/main/resources/static/css/tokens.css`](../../src/main/resources/static/css/tokens.css) | Espelho das variáveis do Figma |
| Referência dos tokens | [`design-tokens.md`](design-tokens.md) | Nomes, valores e regras de uso |
| Inventário de telas | [`telas.md`](telas.md) | Tela → perfil → rota → requisito → `data-testid` |

Arquivo Figma (só design): [Organização de Recursos — Design System](https://www.figma.com/design/ogVFcwHpGNgPXuv6X9cLPE)

## Regras

- Nenhuma cor, espaçamento, raio ou fonte solta no CSS: use os tokens semânticos de `tokens.css`.
- Mudou um token? Mude no Figma **e** em `tokens.css` no mesmo PR, e atualize `design-tokens.md`.
- Toda tela nova entra primeiro no inventário (`telas.md`) com rota e prefixo de `data-testid`.
- Critério de aceite de uma tela é a spec do módulo no Notion; o PR da tela cita a spec e o teste E2E que o comprova.
- RF-13 é comprovado por E2E Playwright em 375 px ([ADR-0004](../adr/0004-playwright.md), [RTM](../rtm.md)).

## Layout base e menu

`src/main/resources/templates/layout.html` é o fragmento que toda tela usa. Ele importa `tokens.css` e `app.css`, carrega `menu.js` e `envio-unico.js` (com `defer`, em toda página) e traz a barra superior, o menu e o `<main>`.

```html
<html lang="pt-BR" xmlns:th="http://www.thymeleaf.org"
      th:replace="~{layout :: pagina(titulo='Minhas reservas', conteudo=~{::#conteudo-pagina})}">
<body><div id="conteudo-pagina"> ... </div></body></html>
```

O menu é montado a partir de dois atributos de modelo, que o back-end (RF-01) preenche:

| Atributo | Conteúdo | Sem ele |
|---|---|---|
| `usuario` | `nome`, `perfil` | Visitante: só a marca (login e erros 401) |
| `menu` | lista de `id`, `rotulo`, `href`, `ativo` | Sem itens |

Itens por perfil (proposta em uso desde 12/11/2026): Solicitante: Buscar recursos e Minhas reservas. Responsável: Aprovações, Retirada e devolução e Relatórios. Administrador: Recursos e Relatórios.

`Sair` é um formulário `POST /logout`, atendido pelo Spring Security. Detalhes de acessibilidade: link "Pular para o conteúdo", botão do menu com `aria-expanded`, item atual com `aria-current="page"`.

O teste `CssSemValoresSoltosTest` reprova cor, medida ou fonte solta em qualquer CSS de tela (só `tokens.css` pode ter valor literal).

## Ligação com o back-end

Desde 12/11/2026 (D12, opção (a), [ADR-0009](../adr/0009-autenticacao-das-telas.md)) as telas chamam os **mesmos services da API**, no mesmo processo (não a API por HTTP). A regra continua no service e no banco; o controller da tela só converte o formulário e monta o modelo de cada contrato abaixo.

- **Quem pode abrir cada tela:** tabela "Telas" de [`docs/seguranca/matriz-autorizacao.md`](../seguranca/matriz-autorizacao.md), espelho da API, conferida por `MatrizAutorizacaoTest`. Sem sessão leva ao login; perfil sem permissão recebe a página 403; reserva de outra pessoa (posse no service) também responde 403.
- **Menu:** `AtributosDasTelas` põe `usuario` e `menu` no modelo de toda tela (inclusive `/error`) a partir da sessão, com os itens propostos acima.
- **Fuso:** data e hora dos formulários e das listas são de `America/Sao_Paulo` (`recursos.telas.fuso-horario`, em `FormatoDasTelas`); o banco guarda o instante em UTC.
- **Erros de regra:** os que a tela sabe mostrar no formulário (409 de horário, manutenção, campo inválido, rejeição sem motivo, autoaprovação) voltam à própria tela; os demais viram as páginas de erro (`TratadorDeErrosDasTelas`: 403, 404, 400 e 409 na página genérica 4xx).
- **O que o esquema ainda não guarda** (V2, área do Gustavo): localização, patrimônio, competências e motivo do bloqueio. As telas mostram "Não informado" e o que for digitado nesses campos **não é gravado**. Por isso o filtro de local da busca fica sem opções.
- **Relatório:** horas e conflitos vêm do `RelatorioService`; a contagem de reservas por linha é feita na tela, com a regra das consultas (estados que ocupam horário, D5, e período que cruza a janela).
- **Rotas das telas × API:** `POST /reservas/{id}` chama `alterarPeriodo` (API: `PUT /api/reservas/{id}/periodo`) e `POST /reservas/{id}/cancelar` chama `cancelar` (API: `POST /api/reservas/{id}/cancelamento`); as demais seguem a tabela da matriz.
- **Testes:** `TelasComBancoTest` (cada tela contra PostgreSQL real, conferindo HTML e banco), `PaginaNaoEncontradaIntegracaoTest` (login com servidor real) e os E2E com `entrarComo`. Os recortes `@WebMvcTest` de contrato usam services dublês sem resposta (`support/TelasNoRecorte`) e entram com `@ComoUsuario`.

## Páginas de erro

O Spring escolhe o template pelo status: `templates/error/401.html`, `403.html`, `404.html` e `500.html`. Status sem página própria usam `4xx.html` ou `5xx.html` (modelo genérico, em português). O corpo é o fragmento `fragments/erro.html`: código com tom (`aviso`, `info`, `perigo`), título, mensagem e um caminho para continuar.

- 401 leva a `/login?expirou` (a tela de login mostra "Sua sessão expirou. Entre novamente."); 403 e 404 levam ao início; 500 oferece "Tentar novamente" na URL original e "Voltar ao início". O destino só é aceito se for um caminho deste site (regra `CaminhoDoSite`, descrita em "Login"); qualquer outro vira o início.
- A página usa o layout base. Para mostrar o menu a quem está logado (403, 404, 500), o back-end precisa colocar `usuario` e `menu` no modelo de **todas** as telas, inclusive `/error`, por exemplo com um `@ControllerAdvice` com `@ModelAttribute`. Sem isso, a página mostra só a marca.
- Se o back-end passar a responder erros de API com `ProblemDetail`, a negociação por `Accept: text/html` precisa continuar levando às páginas acima.

## Login

`GET /login` (`LoginController`, template `login.html`) mostra o formulário; quem autentica é o **Spring Security**, no `POST /login` da cadeia das telas (D12, opção (a), [ADR-0009](../adr/0009-autenticacao-das-telas.md), `config/SegurancaDasTelasConfig`).

| O quê | Valor |
|---|---|
| Envio | `POST /login` com os campos `login` e `senha` (e `destino`, se houver) e o `_csrf` |
| Falha | Volta a `/login?erro` (repetindo o `destino` na URL) e a tela mostra "Login ou senha incorretos", sem dizer qual dos dois errou. O login digitado volta por **atributo flash** (`login`), nunca na URL; a senha nunca volta |
| Sessão expirada | Quem abre uma tela com o cookie de sessão vencido vai para `/login?expirou` (e `&destino=` no `GET`); a tela mostra "Sua sessão expirou. Entre novamente." Se vierem `erro` e `expirou` juntos, só o aviso de credencial aparece |
| Sem sessão | Qualquer tela leva a `/login?destino=<página pedida>` (só no `GET`, só se o destino passar em `CaminhoDoSite`) |
| Sucesso | Redireciona para o `destino` **revalidado** com `CaminhoDoSite` (o campo oculto é editável); sem destino válido, `/`, que leva à primeira tela do menu do perfil |
| Sair | `POST /logout` (botão "Sair" do menu) encerra a sessão, apaga o cookie e volta a `/login` |
| Limite de tentativas | 5 falhas do mesmo login em 15 minutos bloqueiam esse login por 15 minutos, desde a 5ª falha (`recursos.seguranca.login.tentativas` e `recursos.seguranca.login.bloqueio`). Durante o bloqueio, até a senha certa volta a `/login?erro`, com a **mesma** tela e o mesmo aviso de uma senha errada: a tela não diz que bloqueou nem se o login existe. Login certo zera o contador, e login inexistente também conta. Tentativa no bloqueio não estende o bloqueio. O contador é o **mesmo** do `POST /api/auth/login`: falhas na API também bloqueiam a tela, e vice-versa. Ele fica na memória (uma instância só). E2E em 375 px: `LimiteDeTentativasE2ETest`. Ver ADR-0009, ressalva 1 |

O campo é **Login** (`type=text`, `autocomplete=username`), não e-mail: o cadastro aceita `[a-z0-9._-]` (`UsuarioRequest`), e um `type=email` recusava esses logins no navegador.

Regra de `CaminhoDoSite` (`br.com.senac.recursos.web`, usada no login e nas páginas de erro): começa com `/`; o segundo caractere não é `/` nem `\`; não tem `\` em posição nenhuma; não tem caractere de controle (TAB, LF, CR, NUL...), espaço (inclusive Unicode) nem caractere invisível; não tem `%2f`, `%5c` nem `%25` (sem diferenciar maiúsculas); no máximo 2000 caracteres; é uma URI válida, sem esquema nem autoridade. Isso fecha `/<TAB>/evil.com` (o navegador remove TAB, LF e CR e lê `//evil.com`) e `/\evil.com`. Um valor que não passa não vai ao formulário (login) nem vira link (páginas de erro, que caem em `/`). Os templates chamam a regra por `${caminhoDoSite.valido(x)}`, atributo que um `@ControllerAdvice` (`AtributosDasTelas`) põe no modelo de todas as telas, inclusive `/error`.

Acessibilidade: rótulos visíveis, `autocomplete="username"` e `"current-password"`, colar liberado, avisos com `role="alert"` e `tabindex="-1"` (o `login.js` leva o foco ao aviso de erro ou de sessão expirada) e o erro não diz qual campo errou.

Envio: no evento `submit`, o `login.js` desabilita o botão Entrar, mostra "Entrando…" com um anel (`aria-hidden`) e marca `aria-busy="true"` e `aria-disabled="true"`, sem cancelar o envio (o botão não tem `name`, então nada some do formulário). O botão também volta ao normal (com o foco nele) em qualquer `pageshow` e por um temporizador de segurança de 30 s, para não ficar preso em "Entrando…" se a navegação não terminar (Esc/Parar, rede caída, resposta 204 ou download). Sem JavaScript o formulário envia normalmente. O texto do botão desabilitado (`--color-text-secondary` sobre `--color-bg-disabled`) tem contraste de 6,1:1.


## Busca e resultados

`GET /recursos/disponiveis` (`BuscaController`, template `busca.html`) serve as telas 4 e 5.  Desde 12/11/2026 o controller preenche estes atributos com o back-end (ver "Ligação com o back-end"). Atributos:

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `filtros` | `tipo`, `capacidade`, `local`, `data`, `inicio`, `termino` (o controller já preenche com os parâmetros) | — |
| `tipos`, `locais` | listas de texto para os filtros de tipo e localização | Só a opção padrão ("Todos os tipos", "Qualquer local") |
| `resultados` | lista de `id`, `nome`, `tipo`, `local`, `capacidade` (texto já formatado, ex. "40 lugares") e `restrito` | A página mostra o formulário; lista vazia mostra "Nenhum recurso livre nesse horário" |

- Parâmetros: `tipo`, `capacidade`, `local`, `data` (AAAA-MM-DD), `inicio` e `termino` (HH:mm). O parâmetro `editar` volta ao formulário com os filtros preenchidos; o back-end só deve preencher `resultados` quando ele não vier.
- "Reservar" leva a `/reservas/nova?recursoId=…&data=…&inicio=…&termino=…` (rota proposta da tela 6).
- Resultados: tabela no desktop e cards no mobile com o mesmo HTML (cada célula traz o rótulo em `data-rotulo`).
- O filtro de competência (professor) ficou de fora: depende de decidir se professor é recurso reservável.
- O idioma fixo é pt-BR (`spring.web.locale`), para nomes de dia e datas.

## Nova reserva

`GET /reservas/nova` (`ReservaController`, template `reserva-nova.html`) mostra o formulário preenchido com o que veio da busca (`recursoId`, `data`, `inicio`, `termino`) e envia por `POST /reservas`.  Desde 12/11/2026 o controller preenche estes atributos com o back-end (ver "Ligação com o back-end"). Atributos:

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `reserva` | `recursoId`, `data`, `inicio`, `termino` (o controller já preenche) | — |
| `recurso` | `nome`, `local`, `restrito`; o back-end deve fornecer também no GET (hoje o controller não o preenche) | A tela não mostra o recurso nem o aviso de restrição |
| `erros` | `LinkedHashMap` de `data`, `inicio` ou `termino` para texto pronto para o usuário, na ordem do formulário; outra chave aparece no resumo como texto simples, sem link | Sem erros |
| `conflito` | verdadeiro após o 409 de horário (RN-02, RN-03) | Sem aviso |
| `manutencao` | `inicio` e `fim`, do tipo `LocalDateTime`, após o 409 de manutenção (RN-04) | Sem aviso |

`conflito` e `manutencao` são excludentes. O corpo do `POST /reservas` leva `recursoId`, `data`, `inicio` e `termino`; o back-end rejeita `recursoId` ausente ou vazio. Ao recusar, ele renderiza `reserva-nova` com `reserva` (os mesmos quatro campos) e `recurso`; o status HTTP é livre, a tela não depende dele. Campo obrigatório vazio é barrado pelo navegador (aviso nativo), sem resumo.

- Recurso restrito avisa, dentro do formulário e antes de confirmar, que a reserva ficará Solicitada até a aprovação. "Trocar recurso" leva à lista de resultados e "Voltar à busca" ao formulário, os dois com o horário escolhido.
- Erro de campo aparece junto ao campo e num resumo no topo (`role="alert"`), com link para o campo.
- `reserva-form.js` bloqueia término igual ou anterior ao início antes do envio (RN-01) e desabilita o botão Confirmar durante o envio. Sem JavaScript o back-end valida.
- Sucesso leva ao detalhe da reserva (tela 8), decisão do back-end.
- `reserva-form.js` serve a esta tela e à alteração da tela 8: o formulário diz o prefixo dos ids em `data-reserva-form` (`reserva-form` aqui, `reserva-alterar` na tela 8). `GanchosDoScriptDeReserva` (teste) confere que todo seletor do script existe nas duas páginas.

## Minhas reservas e detalhe

Telas 7 e 8 do Solicitante (`ReservaController`, RF-04, RN-01, RN-05 e RN-07; spec 02 do Notion).  Desde 12/11/2026 o controller preenche estes atributos com o back-end (ver "Ligação com o back-end"). Atributos:

| Rota | Template | Para quê |
|---|---|---|
| `GET /reservas/minhas?estado=` | `reservas-minhas.html` | Tela 7: lista do usuário logado com filtro por estado |
| `GET /reservas/{id}` | `reserva-detalhe.html` | Tela 8: detalhe; `?acao=alterar` abre o formulário e `?acao=cancelar` abre a confirmação (outro valor é ignorado; `{id}` não numérico responde 400) |
| `POST /reservas/{id}` | (back-end) | Altera data e horário (campos `data`, `inicio`, `termino`) |
| `POST /reservas/{id}/cancelar` | (back-end) | Cancela, sem corpo |

**Quando a reserva pode mudar.** Alterar e Cancelar só aparecem para `Solicitada` ou `Aprovada` com `iniciada` **exatamente** falso (nome exato, como nas telas 9 e 10; `iniciada` ausente conta como iniciada). Isso é a proposta da pergunta em aberto **D9** do plano (cancelar Solicitada, o que é "já iniciada" e se alterar uma Aprovada restrita volta a Solicitada); o back-end continua obrigado a recusar a transição inválida. Nos outros casos o detalhe diz "Esta reserva não pode mais ser alterada nem cancelada." e a lista mostra só "Ver detalhes".

**Tela 7, lista (`reservas-minhas.html`)**

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `filtro` | `todos` (padrão), `solicitada`, `aprovada`, `em-uso`, `concluida`, `rejeitada`, `cancelada` ou `nao-compareceu`; o controller preenche com `?estado=` (desconhecido vira `todos`) e o back-end filtra por ele | — |
| `reservas` | do usuário logado, **já ordenadas com as próximas primeiro** (a tela não reordena): `id`, `recurso`, `local`, `data` e `horario` (texto pronto), `estado` (nome por extenso) e `iniciada` (booleano: o horário de início já chegou) | Vazia com `todos`: "Você ainda não tem reservas" e "Buscar recursos"; vazia com outro filtro: "Nenhuma reserva com esse estado" e "Ver todas as reservas" |

- Cada linha tem "Ver detalhes" (`/reservas/{id}`) e, quando pode mudar, "Alterar" (`?acao=alterar`) e "Cancelar" (`?acao=cancelar`, que só abre a confirmação). Os três links têm nome acessível com o recurso e a data.
- Tabela no desktop e cartões no mobile com o mesmo HTML (`data-rotulo`); o badge vem de `fragments/estado :: badge`.

**Tela 8, detalhe (`reserva-detalhe.html`)**

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `acao` | `alterar` ou `cancelar` (o controller preenche com `?acao=`). Ao recusar o `POST /reservas/{id}`, o back-end renderiza a view com `acao` = `alterar` | Só o resumo |
| `reserva` | `id`, `recurso`, `local`, `data` (`LocalDate`), `inicio` e `termino` (`LocalTime`; a tela formata "14:00 às 15:30"), `estado`, `iniciada` e `motivo` (da rejeição, opcional; aparece só em `Rejeitada`) | A página mostra só o título e "Voltar para minhas reservas" |
| `formulario` | `data`, `inicio`, `termino` que o usuário enviou na alteração recusada | O formulário vem com os valores da reserva |
| `erros` | `LinkedHashMap` de `data`, `inicio` ou `termino` para texto pronto; outra chave entra no resumo como texto, sem link | Sem erros |
| `conflito` | verdadeiro após o 409 de horário na alteração | Sem aviso |
| `manutencao` | `inicio` e `fim` (`LocalDateTime`) após o 409 de manutenção | Sem aviso |
| `confirmacao` | `titulo`, `reserva` e `estado` (fragmento `fragments/confirmacao`), depois de criar, alterar ou cancelar | Sem aviso |

- `Solicitada` mostra "Aguardando a aprovação de um Responsável."
- A alteração mantém o recurso e só troca data e horário; usa os mesmos campos, resumo de erros e avisos de conflito e manutenção da nova reserva, e `reserva-form.js` bloqueia término igual ou anterior ao início (RN-01) e desabilita "Salvar alterações" durante o envio. "Voltar sem alterar" volta a `/reservas/{id}`.
- Cancelar abre o diálogo "Cancelar a reserva?" (mesmo padrão das telas 9 e 10: `<dialog open>` do servidor, `dialogo.js`, foco inicial em "Voltar"), que avisa que a ação não pode ser desfeita. "Sim, cancelar reserva" envia `POST /reservas/{id}/cancelar` uma vez só (`data-envio-unico`).
- Onde fica o link para o histórico (tela 11) continua decisão do back-end; o Solicitante não o vê (RN-11).

## Recursos e bloqueio por manutenção

Telas 13, 14 e 15 do Administrador (`RecursoController`, RF-02 e RF-08). Desde 12/11/2026 o controller lista, grava e consulta as reservas afetadas pelo `RecursoService`, pelo `ManutencaoService` e pelo `ReservaService` (ver "Ligação com o back-end"; localização, patrimônio, competências e motivo ainda não são gravados).

| Rota | Template | Para quê |
|---|---|---|
| `GET /admin/recursos?tipo=salas\|materiais\|professores&nome=` | `recursos.html` | Tela 13: lista por aba, com busca por nome |
| `GET /admin/recursos/novo?tipo=` | `recurso-form.html` | Tela 14, cadastro |
| `GET /admin/recursos/{id}` | `recurso-form.html` | Tela 14, edição |
| `GET /admin/recursos/{id}/manutencao` | `manutencao.html` | Tela 15, bloqueio |
| `POST /admin/recursos` e `POST /admin/recursos/{id}` | (back-end) | Grava o recurso (campos `tipo`, `nome`, `capacidade`, `patrimonio`, `localizacao`, `competencias`, `restrito`) |
| `POST /admin/recursos/{id}/manutencao` | (back-end) | Registra o bloqueio (`inicio`, `fim`, `motivo`; `confirmar=true` quando o Administrador confirma depois do aviso de reservas afetadas) |

O parâmetro `tipo` é o mesmo nas três telas: `salas`, `materiais` ou `professores`. O enum `TipoRecurso` traz os rótulos e os campos de cada tipo. Na lista (`/admin/recursos`) um `tipo` desconhecido vira `salas`; em `/admin/recursos/novo`, `/admin/recursos/{id}` e `/admin/recursos/{id}/manutencao` um `tipo` presente e desconhecido responde 400 (ausente vale `salas`), assim como `restrito` que não seja booleano ou `inicio`/`fim` que não sejam data e hora. Salvar leva de volta à lista com `confirmacao` (decisão do back-end).

O back-end pode usar DTO próprio no lugar de `FormularioRecurso`, `FormularioBloqueio` e dos mapas, desde que tenha as mesmas propriedades. Cuidado com a palavra `recurso`: é o atributo de modelo nas telas 14 e 15, é a variável do `th:each` na tela 13 e, na tela 15, `recurso.tipo` é um **texto** pronto ("Sala, 120 lugares"), enquanto o atributo `tipo` das telas 13, 14 e 15 é o enum `TipoRecurso`.

**Tela 13, lista (`recursos.html`)**

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `tipo` | `TipoRecurso` da aba (o controller preenche com `?tipo=`) | — |
| `nome` | texto da busca (o controller preenche com `?nome=`) | Campo vazio |
| `recursos` | lista de `id`, `nome`, `capacidade` (texto pronto, ex. "40 lugares"), `local`, `patrimonio`, `competencias`, `emManutencao`, `periodoManutencao` (texto pronto, ex. "10/11/2026 a 20/11/2026"), `restrito`; `emManutencao` e `restrito` precisam ser booleanos | A página mostra só o cabeçalho, as abas e a busca. Lista vazia mostra "Nenhuma sala cadastrada" (ou o tipo da aba) com "Cadastrar o primeiro recurso"; vazia com `nome` mostra "Nenhum resultado para “nome”" e "Limpar busca" |
| `confirmacao` | `titulo` e `texto`, depois de salvar o recurso ou registrar o bloqueio | Sem aviso |

- "Editar" e "Manutenção" levam `tipo` da aba (`/admin/recursos/{id}?tipo=…`, `/admin/recursos/{id}/manutencao?tipo=…`) para a tela seguinte voltar à mesma aba.
- Cada aba usa as colunas do seu tipo: Salas (Nome, Capacidade, Localização, Situação), Materiais (Nome, Patrimônio, Localização, Situação), Professores (Nome, Competências, Situação). Só os campos do tipo precisam vir preenchidos.
- A situação aparece escrita ("Ativo" ou "Em manutenção" com o período) e "Restrito" é uma etiqueta. Professor mostra só "Ativo" e só tem **Editar**, porque professor não é reservável por enquanto; a tela só esconde o link, então o back-end deve recusar o bloqueio por manutenção de professor.
- Abas são links com `aria-current="page"`; a busca é um `GET` que mantém a aba. Tabela no desktop e cartões no mobile com o mesmo HTML (`data-rotulo` em cada célula). `confirmacao` é um `role="status"`.

**Tela 14, formulário (`recurso-form.html`)**

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `tipo` | `TipoRecurso`; no cadastro o controller preenche com `?tipo=`, na edição o back-end define o tipo do recurso | — |
| `recurso` | `id`, `nome`, `capacidade` (texto), `localizacao`, `patrimonio`, `competencias`, `restrito`. Com `id` é a **edição** e o back-end preenche com o que está gravado; sem `id` é o cadastro e o controller devolve o que veio na URL | — |
| `erros` | `LinkedHashMap` de `nome`, `capacidade`, `localizacao`, `patrimonio` ou `competencias` para texto pronto, na ordem do formulário; outra chave aparece no resumo como texto, sem link | Sem erros |

- Sala: nome, capacidade, localização. Material: nome, patrimônio, localização. Professor: nome e competências, sem "Recurso restrito". Todo campo de dados tem rótulo visível com "(obrigatório)", `required` e `aria-required` (o seletor de tipo não tem "(obrigatório)", como no quadro); o formulário tem `novalidate`, então quem valida é o back-end, que devolve `erros` (resumo `role="alert"` no topo com links e mensagem junto de cada campo). O resumo conta só os erros de campos que o tipo mostra; erro de outra chave (por exemplo `geral`) entra na lista como texto, sem contar e sem link.
- O back-end valida os campos obrigatórios **conforme o `tipo` recebido** no `POST`: o seletor pode estar diferente dos campos que o usuário viu (o navegador pode restaurar a escolha do seletor, e por isso ele leva `autocomplete="off"`). Na edição o `tipo` do `POST` deve ser ignorado: vale o tipo gravado.
- A troca de tipo é sempre pelo botão "Atualizar campos", visível com ou sem JavaScript ("Depois de escolher o tipo, use Atualizar campos."): ele envia o formulário por `GET /admin/recursos/novo` e a página volta com os campos do tipo e com o que já foi digitado. Não há envio automático ao mudar o seletor (WCAG 3.2.2). Um botão de envio invisível vem antes do "Atualizar campos" para a tecla Enter continuar salvando; um teste garante que o primeiro botão de envio do formulário é o de salvar, sem `formaction` nem `formmethod`.
- Na edição o título é "Editar recurso", o tipo aparece como texto simples (e vai num campo oculto) e o botão é "Salvar alterações". `recurso-form.js` só leva o foco ao resumo quando a página volta com erros.
- "Recurso restrito (exige aprovação)" é uma caixa de seleção que explica que a reserva fica Solicitada até um Responsável aprovar.

**Tela 15, bloqueio (`manutencao.html`)**

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `tipo` | `TipoRecurso` da aba da lista a que a tela volta (o controller preenche com `?tipo=`) | Os links "Voltar" e "Cancelar" levam à lista sem aba |
| `recurso` | `id`, `nome`, `tipo` (**texto** pronto, ex. "Sala, 120 lugares"), `local`, `emManutencao` | Sem o resumo do recurso |
| `bloqueio` | `recursoId`, `inicio` e `fim` (`LocalDateTime`), `motivo`; o controller preenche com a URL e o back-end devolve o que foi enviado | — |
| `erros` | mapa de `inicio`, `fim` ou `motivo` para texto pronto | Sem erros |
| `afetadas` | lista de `data` (`LocalDate`), `horario` (texto, ex. "14:00 às 16:00"), `solicitante`, `estado` (só o nome por extenso, ex. "Em uso"; o badge vem do fragmento comum, ver "Estado da reserva (RN-05)") das reservas que caem no período | Sem aviso (lista vazia também) |

- O back-end só preenche `afetadas` ao responder o `POST`. Com ela, a tela mostra o aviso `role="alert"` "N reservas caem no período do bloqueio", com data, solicitante e estado, e as ações "Confirmar bloqueio" (reenvia o formulário com `confirmar=true`) e "Voltar e ajustar o período" (link para o `GET` com os valores digitados). Nesse estado início, fim e motivo ficam **somente leitura** (`readonly`, fundo cinza) e "Confirmar bloqueio" é o primeiro botão de envio (a tecla Enter confirma o que o aviso mostrou); mudar o período só é possível por "Voltar e ajustar o período". Mesmo assim o back-end deve **recalcular as afetadas no `POST` com `confirmar=true`** e recusar (voltando ao aviso) se o período for outro que o do aviso. O que acontece com essas reservas (cancelar, manter ou notificar) é decisão pendente: a tela só lista.
- Fim igual ou anterior ao início é barrado antes do envio por `manutencao-form.js` e também pode vir em `erros`, com "O fim do bloqueio precisa ser depois do início." junto do campo (`role="alert"`). O script só apaga o erro que ele mesmo mostrou. Sem JavaScript o back-end valida.

- Os formulários que gravam (cadastro, edição e bloqueio, inclusive "Confirmar bloqueio", que é um botão ligado ao formulário por `form=`) têm `data-envio-unico` (ver "Envio único de formulário"); o `name`/`value` do botão (`confirmar=true`) segue no `POST`, que sai uma vez só no clique duplo. O "Atualizar campos" é `GET` e não precisa.
- `.situacao` (Ativo, Em manutenção) divide a base de estilo do `.estado` e só acrescenta borda e cores.

**Testes e JavaScript.** Os testes de contrato só conferem que cada tela carrega o seu script. O E2E em 375 px cobre o bloqueio de fim antes do início do `manutencao-form.js` (`ResponsavelEAdministradorE2ETest`); a troca de tipo do `recurso-form.js` continua sem E2E (ver "E2E em 375 px").
## Fila de aprovação

`GET /aprovacoes` (`AprovacaoController`, template `aprovacoes.html`) é a tela 9, do Responsável (RF-07, RN-06).  Desde 12/11/2026 o controller preenche estes atributos com o back-end (ver "Ligação com o back-end"). Atributos:

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `pedidos` | lista dos pedidos Solicitados, os mais próximos do horário primeiro: `id`, `solicitante`, `recurso`, `data` e `horario` (texto já formatado, ex. "09/11/2026" e "08:00 às 10:00"), `estado` (nome por extenso, "Solicitada") e `proprio` (verdadeiro se o pedido é do próprio Responsável) | Lista ausente ou vazia mostra "Nenhum pedido aguardando aprovação" |
| `rejeitando` | o pedido que está sendo rejeitado (mesmos campos de `pedidos`); o back-end o preenche quando a URL traz `?rejeitar={id}`. O template só abre o diálogo se o pedido estiver Solicitado e não for do próprio Responsável (`proprio` falso); o back-end deve responder 404 ou ignorar o parâmetro quando o id não existe ou o estado não permite | Sem diálogo |
| `erros` | mapa com a chave `motivo` e o texto do erro, para a rejeição sem motivo (só vale com `rejeitando`) | Sem erro no campo |
| `confirmacao` | `titulo` (ex. "Reserva aprovada"), `reserva` (ex. "Laboratório de Química, de Laura Mendes") e `estado` (novo estado por extenso); o aviso diz "{reserva}, agora está {estado}. A mudança ficou registrada." | Sem aviso |
| `erroAprovacao` | verdadeiro quando a API respondeu 403 à aprovação ("Você não pode aprovar esta reserva", estado inalterado) | Sem aviso |

- Ações por formulário: `POST /aprovacoes/{id}/aprovar` (sem corpo) e `POST /aprovacoes/{id}/rejeitar` com o campo `motivo`. Rotas propostas. Depois de decidir, o back-end pode redirecionar para `GET /aprovacoes` com `confirmacao` (atributo flash) ou renderizar a view; a tela não depende disso. Rejeitar sem motivo: renderizar a view de novo com `pedidos`, `rejeitando` e `erros`.
- O pedido do próprio Responsável (`proprio`) não mostra Aprovar nem Rejeitar, só "Você não pode decidir a sua própria reserva." O back-end continua responsável por recusar a decisão (RN-06); a tela apenas não a oferece.
- Rejeitar é um link (anunciado como "link" pelos leitores de tela) para `?rejeitar={id}`: foi uma escolha consciente, para o diálogo vir do servidor e funcionar sem JavaScript.
- Acessibilidade: todo botão da tabela tem nome acessível com a ação, o recurso e o solicitante; o motivo é obrigatório (`required`, rótulo visível) e o erro fica junto ao campo, com `role="alert"`, `aria-invalid` e `aria-describedby`; o aviso de decisão é `role="status"` e o 403 é `role="alert"`. O estado aparece escrito, além da cor (RN-05).

## Retirada e devolução

`GET /retirada-devolucao` (`RetiradaDevolucaoController`, template `retirada-devolucao.html`) é a tela 10, do Responsável (RF-09, RN-05, RN-08).  Desde 12/11/2026 o controller preenche estes atributos com o back-end (ver "Ligação com o back-end"). Atributos:

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `filtro` | `ativas` (Aprovadas e em uso, o padrão), `aprovada`, `em-uso` ou `todos`; o controller preenche com `?estado=` (valor desconhecido vira `ativas`) e o back-end deve filtrar a lista por ele | — |
| `reservas` | lista de `id`, `recurso`, `solicitante`, `data` e `horario` (texto já formatado), `estado` (nome por extenso: Solicitada, Aprovada, Em uso, Concluída, Rejeitada, Cancelada ou Não compareceu) e `horarioPassou` (verdadeiro se o horário da reserva já passou) | Lista ausente ou vazia mostra "Nenhuma reserva aguardando retirada ou devolução" (o filtro continua na página) |
| `naoCompareceu` | a reserva em confirmação (mesmos campos de `reservas`); o back-end a preenche quando a URL traz `?naoCompareceu={id}`. O template só abre o diálogo se a reserva estiver Aprovada e com `horarioPassou` verdadeiro; o back-end deve responder 404 ou ignorar o parâmetro quando o id não existe ou o estado não permite | Sem diálogo |
| `confirmacao` | `titulo` (ex. "Retirada registrada"), `reserva` (ex. "Sala 203, de Rafael Costa") e `estado` (novo estado por extenso) | Sem aviso |

- Ações por formulário: `POST /retirada-devolucao/{id}/retirar`, `POST /retirada-devolucao/{id}/devolver` e `POST /retirada-devolucao/{id}/nao-compareceu` (sem corpo). Rotas propostas.
- Só a ação que o estado permite é oferecida (RN-05): Aprovada mostra "Registrar retirada", Em uso mostra "Registrar devolução" e qualquer outro estado mostra "Sem ação disponível". O back-end continua validando a transição.
- Reserva Aprovada com `horarioPassou` mostra a etiqueta "Horário já passou" e o botão "Marcar como não compareceu", que leva a `?estado={filtro}&naoCompareceu={id}` e abre um diálogo de confirmação (`role="alertdialog"`, foco em "Voltar"), porque a mudança não tem volta: Não compareceu é estado terminal (RN-05) e fica registrada para auditoria (RN-08). "Marcar como não compareceu" também é um link, pelo mesmo motivo de "Rejeitar". O prazo para marcar NAO_COMPARECEU ainda é decisão pendente do back-end: a tela mostra o botão quando ele disser `horarioPassou`. Reserva Em uso nunca mostra a etiqueta nem o botão.
- Depois de cada ação o aviso `role="status"` diz o novo estado; o redirecionamento para `GET /retirada-devolucao` com `confirmacao` fica a cargo do back-end.

### Diálogos (telas 8, 9 e 10)

Os dois diálogos são `<dialog>` que o servidor entrega com o atributo `open`, e `dialogo.js` (só carregado quando há diálogo) troca `open` por `showModal()`: fundo escurecido, foco preso, Esc fecha e o endereço volta ao da lista. Sem JavaScript o `<dialog open>` fica não modal, aparece fixo sobre a lista sem o fundo escurecido e vem depois da tabela na ordem de tabulação (quem navega por teclado precisa passar pela lista para chegar a ele); "Voltar" é um link para a lista. Com JavaScript, ao fechar com Esc o foco volta ao link que abriu o diálogo (`data-origem` guarda o `data-testid` dele) e o endereço volta ao da lista. O foco inicial é o campo do motivo na rejeição e o botão "Voltar" na confirmação de não compareceu.

O badge do estado vem de `fragments/estado.html :: badge` (ver "Estado da reserva (RN-05)" abaixo: nome desconhecido aparece escrito, com cor neutra; estado nulo não aparece). O aviso de confirmação é o fragmento `fragments/confirmacao.html`, usado pelas duas telas.

**As decisões de ação comparam o nome EXATO e diferenciam maiúsculas.** A tela 10 só oferece "Registrar retirada" se `estado` for exatamente `Aprovada` e "Registrar devolução" se for exatamente `Em uso`; a tela 9 só abre o diálogo de rejeição para `Solicitada`, e a tela 10 só abre o de não compareceu para `Aprovada`. Qualquer outro texto (`aprovada`, `EM USO`, `Em análise`, nulo) mostra "Sem ação disponível" e nenhum botão, mesmo que o badge consiga colori-lo.

**`confirmacao` tem duas formas no projeto:** `{titulo, reserva, estado}` nas telas 9 e 10 (o aviso monta "{reserva}, agora está {estado}. A mudança ficou registrada.") e `{titulo, texto}` na tela de recursos (seção 05), que não tem estado de reserva. Não misture os dois: cada tela documenta a sua.

Todo formulário que envia uma ação (aprovar, rejeitar, retirar, devolver, confirmar não compareceu) tem `data-envio-unico` e um botão de envio, então `envio-unico.js` (carregado pelo layout) desabilita o botão no primeiro envio e a ação não sai duas vezes; ver "Envio único de formulário" abaixo. O formulário do filtro (GET) não precisa. O diálogo reaproveita `cartao__titulo`, `pagina__descricao` e `formulario__acoes`, e o "(você)" usa `tabela__nota`.

O comportamento de `dialogo.js` (modal, foco inicial, Esc, endereço e foco de volta ao gatilho) é coberto pelo E2E em 375 px nas telas 8, 9 e 10 (`ReservaE2ETest` e `ResponsavelEAdministradorE2ETest`).

## Estado da reserva (RN-05)

O contrato com o back-end é **só o nome por extenso**, em `estado` (String): `Solicitada`, `Aprovada`, `Em uso`, `Concluída`, `Rejeitada`, `Cancelada` ou `Não compareceu`. O modelo não traz classe nem código; quem converte o nome em classe é **um único** fragmento, `fragments/estado.html`:

```html
<span th:replace="~{fragments/estado :: badge(${reserva.estado}, 'aprovacao-estado')}">Aprovada</span>
```

| Entrada (`estado`) | Saída |
|---|---|
| Um dos sete nomes | `<span class="estado estado--X" data-testid="...">Nome</span>`, com o texto exatamente como veio (escapado) |
| Maiúsculas, espaços nas pontas ou duplos, grafia sem acento (`concluida`, `nao compareceu`) | Mesma classe do nome certo; o texto continua como veio |
| Nome desconhecido | Só `class="estado"`, badge visível com cor neutra (`--color-bg-subtle` e `--color-text-secondary`) |
| Nulo ou em branco | Não renderiza nada |

A classe sai de uma tabela fechada (`em uso` → `estado--em-uso`, `concluída` → `estado--concluida`, `não compareceu` → `estado--nao-compareceu`), nunca de uma conversão do texto; por isso não existe `estado--null` nem `estado--em--uso`. O segundo argumento é o `data-testid` do badge (`null` ou vazio = sem o atributo).

- Use **sempre** o fragmento; nunca escreva `<span class="estado ...">` à mão nem monte a classe na tela ou no controller.
- Estado novo? Acrescente o nome na tabela do fragmento, a classe `.estado--*` em `app.css` e os tokens `color/status/*` (Figma, `tokens.css` e `design-tokens.md`); `EstadoBadgeTest` e `EstadosDaReservaCssTest` cobrem os sete atuais.
- Estado desconhecido aparece em cinza para não esconder a reserva, mas indica nome fora do contrato: confira o que o back-end está enviando.

## Envio único de formulário

`<form data-envio-unico>` evita o segundo POST por segundo clique ou Enter extra. O `envio-unico.js` (carregado pelo layout em toda página; sem o atributo ele não faz nada) trata o evento `submit`:

- Nunca cancela o envio. No ciclo seguinte ao evento, depois que o navegador já coletou os campos, desabilita os botões de envio do formulário e marca `aria-busy="true"`. O `name`/`value` do botão clicado (por exemplo `confirmar=true`) segue no POST.
- Não muda o texto do botão. `login.js` e `reserva-form.js` continuam com o seu próprio comportamento e não usam o atributo.
- Se outro script cancelou o envio (validação), nada é desabilitado. Botão que já estava desabilitado continua assim.
- Restaura tudo em qualquer `pageshow` (voltar pelo histórico) e, por segurança, depois de 30 s (resposta que não navega, como download).

Use em formulários que mudam estado no servidor e não têm script próprio: aprovar, rejeitar, cancelar, registrar retirada e devolução, bloquear e liberar recurso. O formulário precisa ter um botão de envio (`type="submit"`); sem ele o Enter nos campos não é barrado.

O E2E em 375 px prova o POST único com clique duplo nas telas 8 e 10 (botão desabilitado, um só POST). **Observação medida em 11/11/2026 (Chromium 1243):** num clique duplo imediato (ou Enter duas vezes) o navegador dispara dois eventos `submit` antes de o `setTimeout(0)` travar o botão; mesmo assim só um POST sai, porque o Chromium troca a primeira navegação pela segunda. Em outros navegadores **não medido**. A restauração no `pageshow` e nos 30 s continua sem teste automatizado.

## Histórico da reserva

`GET /reservas/{id}/historico` (`HistoricoController`, template `historico.html`) é a tela 11, só leitura (RF-10, RN-09, RN-10).  Desde 12/11/2026 o controller preenche estes atributos com o back-end (ver "Ligação com o back-end"). Atributos:

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `reserva` | `recurso`, `solicitante`, `data` (`LocalDate`, formatada na tela), `horario` (texto já formatado, ex. "14:00 às 15:30") e `estado` (nome escrito do estado atual, ex. "Em uso") | A página mostra só o título e o link Voltar |
| `eventos` | Lista **do mais antigo ao mais recente** (a tela não reordena). Cada item: `instante` (`LocalDateTime`, formatado na tela), `de` (nulo na criação da reserva, senão o nome do estado anterior), `para` (nome do estado novo; nulo mostra "Estado novo não informado"), `usuario` (nome) e `motivo` (nulo, vazio ou só espaços mostra "Não informado") | Linha do tempo vazia mostra "Nenhuma mudança registrada." |
| `voltar` | Endereço do link "Voltar" (opcional). Só pode vir de uma **lista fixa do back-end**, nunca de parâmetro de requisição: o valor vai direto para o `href` e `javascript:` seria executado | O link vai para `/` |

- O badge de estado (resumo e linha do tempo) usa o fragmento `fragments/estado :: badge` (seção "Estado da reserva (RN-05)"): `estado` nulo ou em branco não renderiza badge e nome desconhecido aparece escrito em cinza, sem erro de página.
- Formatos: `reserva.data` e `evento.instante` chegam como tipos temporais e a tela formata (`dd/MM/yyyy`, `HH:mm`, `<time datetime>`); `horario` e `horas` chegam como texto pronto.
- A rota exige `{id}` numérico: outro valor responde 400. Reserva inexistente (404) é do back-end.
- Acesso (RN-11): Responsável e Administrador veem; para o Solicitante o back-end deve responder **403**, que é a página de erro 403 que já existe (não há tela própria). A rota exige `{id}` de uma reserva existente; o 404 também é do back-end.
- Somente leitura: a tela não tem formulário, botão nem link de editar ou apagar, e diz "Os registros são somente leitura: não podem ser alterados nem apagados." O motivo da rejeição (RN-09) aparece na linha da mudança para Rejeitada.
- Onde fica o link "Ver histórico" ainda é decisão do back-end (sugestão: uma ligação em cada linha da retirada e devolução e no detalhe da reserva para esses perfis).
- Acessibilidade: lista ordenada (`<ol>`), data e hora em `<time datetime>`, estado sempre escrito além da cor e um único `<h1>`.

## Relatório de utilização

`GET /relatorios/utilizacao?inicio=&fim=` (`RelatorioController`, template `relatorio.html`) é a tela 12 (RF-12, RN-13; a rota já está no [PRD](../prd.md)). O controller devolve o período ao filtro e recusa fim anterior ao início. Desde 12/11/2026 calcula com o `RelatorioService` (ver \"Ligação com o back-end\"). Atributos:

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `filtro` | `inicio` e `fim` (`LocalDate`; o controller já preenche com os parâmetros `AAAA-MM-DD`) | — |
| `erros` | Mapa com a chave `fim` para o texto do erro; o controller já preenche quando `fim` é anterior a `inicio` | Sem erro |
| `relatorio` | `recursos` e `professores` (listas de `nome`, `reservas` e `horas`, esta já formatada como "18 h" ou "12 h 30 min"), `totalReservas` (número), `totalHoras` (texto pronto, como `horas`) e `conflitos` (número) | A página mostra só o filtro e a nota |

- Com `erros` a tela não mostra o relatório, mesmo que `relatorio` exista; o back-end deve ignorar `fim` anterior a `inicio` antes de calcular. `totalReservas` igual a 0 mostra só "Nenhuma reserva no período", sem tabelas.
- "Professor" é quem pediu a reserva (o solicitante). Os totais valem para os dois blocos de tabela; reserva cancelada não conta como uso (nota fixa na tela). Como o relatório conta a reserva que cruza a borda do período é decisão do back-end (caso obrigatório do PRD).
- Acesso (RN-13): Responsável e Administrador; o Solicitante recebe 403 do back-end.
- `relatorio-form.js` bloqueia fim anterior ao início antes do envio, com o mesmo texto do servidor (`#relatorio-fim-erro`, sempre presente no HTML e escondido quando não há erro), e leva o foco ao campo. Sem JavaScript o servidor devolve o erro. Parâmetros vazios ou só o início não são erro.
- Tabelas no desktop e cartões no mobile com o mesmo HTML (cada célula traz o rótulo em `data-rotulo`).
- Acessibilidade: rótulos visíveis ligados por `for`, erro em texto junto ao campo (`role="alert"`, `aria-invalid`, `aria-describedby`), `<caption>` em cada tabela e um único `<h1>`.

## Usuários (tela 16)

`GET /admin/usuarios` (lista) e `GET /admin/usuarios/novo` (cadastro), templates `usuarios.html` e `usuario-form.html` (`UsuarioController`), só do Administrador (matriz). Usa o `UsuarioService`, o mesmo de `POST /api/usuarios`.

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `usuarios` | lista de `id`, `nome`, `login` e `perfil` (por extenso), em ordem de nome | Lista vazia mostra "Nenhum usuário cadastrado" |
| `confirmacao` | `titulo` e `texto`, depois de cadastrar (flash) | Sem aviso |
| `usuario` | `nome`, `login`, `perfil` do formulário; **a senha nunca volta** | — |
| `perfis` | `valor` (`SOLICITANTE`, `RESPONSAVEL`, `ADMINISTRADOR`) e `rotulo` | — |
| `erros` | mapa de `nome`, `login`, `senha` ou `perfil` para a mensagem, na ordem do formulário | Sem erros |

- As regras dos campos são as do `UsuarioRequest` da API, com as mesmas mensagens (nome até 120; login `[a-z0-9._-]{3,60}`; senha de 8 a 72; perfil obrigatório). Login repetido: "Já existe um usuário com este login." no campo.
- A senha é `type=password` com `autocomplete="new-password"` e só é guardada como hash BCrypt pelo service.
- Não há editar nem desativar usuário: o service ainda não tem essas operações.
- O menu do Administrador ganhou o item "Usuários".

## Link para o histórico (tela 11)

"Ver histórico" aparece em cada linha da retirada e devolução (tela 10, só Responsável) e no detalhe da reserva (tela 8) quando o controller põe `verHistorico` verdadeiro, só para Responsável e Administrador (RN-11). É um link de leitura: na tela 10 ele não conta como ação, e as linhas sem transição continuam com "Sem ação disponível".

## E2E em 375 px

`src/test/java/br/com/senac/recursos/e2e` (ADR-0004, RF-13). Rodam no `./mvnw -B verify` como qualquer teste: sobem a aplicação real numa porta aleatória contra PostgreSQL em Testcontainers e abrem o Chromium do Playwright sem tela, em 375 × 812 px (`isMobile`).

| Classe | O que prova |
|---|---|
| `Responsividade375E2ETest` | 28 telas e estados: sem rolagem horizontal, um único `h1`, tabela vira cartão (cabeçalho só para leitor de tela) e todo controle tocável com pelo menos 44 px de altura; um print por tela |
| `AcessoE2ETest` | Avisos do login com foco, "Entrando…" durante o envio, menu recolhido que abre e fecha com Esc, itens só do perfil, páginas 401, 403, 404 e 500 com caminho para continuar e sem jargão técnico |
| `ReservaE2ETest` | Busca → nova reserva → envio (RN-01 barrada, um POST com os campos certos), conflito 409 com foco, aviso de restrito, minhas reservas, alterar e cancelar (modal, Esc, clique duplo) |
| `ResponsavelEAdministradorE2ETest` | Diálogos de rejeitar e de não compareceu, motivo obrigatório, envio único da retirada, fim antes do início no relatório e na manutenção, histórico sem ação de editar |

- **Sem back-end:** as rotas reais só mostram o estado vazio. `TelasComDados` (só do teste, fora do component scan) renderiza os mesmos templates com dados de exemplo em `/e2e/...`, seguindo os contratos deste README. Os `POST` são interceptados no navegador, contados e respondidos com 204. Quando o back-end existir, os fluxos de login, 409 real, 401 e 403 devem passar a rodar contra ele.
- **Navegador:** a versão vem da dependência `com.microsoft.playwright:playwright` do `pom.xml`. No CI o passo "Navegador do E2E" instala o Chromium com as bibliotecas do sistema; numa máquina de desenvolvimento o Playwright baixa o navegador na primeira execução (ou use `PLAYWRIGHT_BROWSERS_PATH` apontando para um Chromium da mesma versão).
- **Esperas:** só as do Playwright (auto-wait, `waitForURL`, asserções); nada de `Thread.sleep`. O print espera dois quadros de animação antes de capturar e tenta mais uma vez só se o Chromium responder "Unable to capture screenshot" (falha vista uma vez no CI, run 43093990942).
- **Prints:** `target/e2e/375/*.png`, no artifact `e2e-375` do CI; amostra versionada em `docs/evidencias/e2e-375/`.
- **Mutações conferidas em 11/11/2026**, cada uma deixou vermelho pelo menos um E2E e foi desfeita:
  - E1: tirar o bloqueio de RN-01 do `reserva-form.js`;
  - E2: trocar `showModal()` por `open` no `dialogo.js`;
  - E3: desligar a trava do `envio-unico.js`;
  - E4: deixar a tabela mais larga que a tela;
  - E5: abrir o menu ao carregar.
- **Fora do E2E por enquanto:** a troca de tipo do formulário de recurso (`recurso-form.js`), a restauração do `envio-unico.js` no `pageshow` e nos 30 s, e larguras de 768 e 1280 px (conferidas à mão).
