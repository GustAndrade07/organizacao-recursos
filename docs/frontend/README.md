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

Itens propostos por perfil (a confirmar com o Pedro): Solicitante: Buscar recursos e Minhas reservas. Responsável: Aprovações, Empréstimos e Relatórios. Administrador: Recursos e Relatórios.

`Sair` é um formulário `POST /logout` (proposta, depende da decisão de autenticação). Detalhes de acessibilidade: link "Pular para o conteúdo", botão do menu com `aria-expanded`, item atual com `aria-current="page"`.

O teste `CssSemValoresSoltosTest` reprova cor, medida ou fonte solta em qualquer CSS de tela (só `tokens.css` pode ter valor literal).

## Páginas de erro

O Spring escolhe o template pelo status: `templates/error/401.html`, `403.html`, `404.html` e `500.html`. Status sem página própria usam `4xx.html` ou `5xx.html` (modelo genérico, em português). O corpo é o fragmento `fragments/erro.html`: código com tom (`aviso`, `info`, `perigo`), título, mensagem e um caminho para continuar.

- 401 leva a `/login`; 403 e 404 levam ao início; 500 oferece "Tentar novamente" na URL original e "Voltar ao início". O destino só é aceito se for um caminho deste site.
- A página usa o layout base. Para mostrar o menu a quem está logado (403, 404, 500), o back-end precisa colocar `usuario` e `menu` no modelo de **todas** as telas, inclusive `/error`, por exemplo com um `@ControllerAdvice` com `@ModelAttribute`. Sem isso, a página mostra só a marca.
- Se o back-end passar a responder erros de API com `ProblemDetail`, a negociação por `Accept: text/html` precisa continuar levando às páginas acima.

## Login

`GET /login` (`LoginController`, template `login.html`) só mostra o formulário; **não autentica**. Contrato com o back-end (RF-01, ADR-0001), a confirmar com o Pedro:

| O quê | Valor |
|---|---|
| Envio | `POST /login` com os campos `email` e `senha` |
| Falha | O back-end redireciona para `/login?erro` (basta o parâmetro existir) e a tela mostra o aviso |
| Sucesso | Decisão do back-end (cookie ou header com o JWT e para onde redirecionar) |

Acessibilidade: rótulos visíveis, `autocomplete="username"` e `"current-password"`, colar liberado, aviso com `role="alert"` que não diz qual campo errou, e `login.js` leva o foco ao aviso depois de uma falha.


## Busca e resultados

`GET /recursos/disponiveis` (`BuscaController`, template `busca.html`) serve as telas 4 e 5. **Não consulta disponibilidade**; contrato com o back-end (RF-03), a confirmar com o Pedro:

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

`GET /reservas/nova` (`ReservaController`, template `reserva-nova.html`) mostra o formulário preenchido com o que veio da busca (`recursoId`, `data`, `inicio`, `termino`) e envia por `POST /reservas`. **Não grava a reserva**; contrato com o back-end (RF-04 a RF-06), a confirmar com o Pedro:

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

## Recursos e bloqueio por manutenção

Telas 13, 14 e 15 do Administrador (`RecursoController`, RF-02 e RF-08). O controller **só recebe os parâmetros e devolve a tela**; quem lista, grava e consulta reservas afetadas é o back-end, que responde aos `POST` e preenche o modelo. Rotas propostas, a confirmar com o Pedro.

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

**Testes e JavaScript.** Os testes de contrato só conferem que cada tela carrega o seu script; o comportamento dos scripts (`recurso-form.js`, `manutencao-form.js`) fica para o E2E Playwright em 375 px (RF-13).

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

**Risco aberto:** o comportamento no navegador (um único POST, restauração no `pageshow` e nos 30 s) foi conferido à mão com Playwright em página de teste, mas não está no repositório. Os testes de servidor só provam que o layout carrega o script e que a página marcada o referencia; o E2E de cada tela deve provar o POST único.
