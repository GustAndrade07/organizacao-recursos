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

## Fila de aprovação

`GET /aprovacoes` (`AprovacaoController`, template `aprovacoes.html`) é a tela 9, do Responsável (RF-07, RN-06). **Não lista nem decide pedidos**; o controller só devolve a view. Contrato com o back-end, a confirmar com o Pedro:

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

`GET /retirada-devolucao` (`RetiradaDevolucaoController`, template `retirada-devolucao.html`) é a tela 10, do Responsável (RF-09, RN-05, RN-08). **Não lista nem registra nada**; o controller devolve o filtro de estado escolhido. Contrato com o back-end, a confirmar com o Pedro:

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

### Diálogos (telas 9 e 10)

Os dois diálogos são `<dialog>` que o servidor entrega com o atributo `open`, e `dialogo.js` (só carregado quando há diálogo) troca `open` por `showModal()`: fundo escurecido, foco preso, Esc fecha e o endereço volta ao da lista. Sem JavaScript o `<dialog open>` fica não modal, aparece fixo sobre a lista sem o fundo escurecido e vem depois da tabela na ordem de tabulação (quem navega por teclado precisa passar pela lista para chegar a ele); "Voltar" é um link para a lista. Com JavaScript, ao fechar com Esc o foco volta ao link que abriu o diálogo (`data-origem` guarda o `data-testid` dele) e o endereço volta ao da lista. O foco inicial é o campo do motivo na rejeição e o botão "Voltar" na confirmação de não compareceu.

O nome do estado vem por extenso e a classe `estado--*` do badge sai dele (fragmento `fragments/estado.html`, que também traz o aviso de confirmação); um nome fora dos sete estados aparece escrito, mas sem cor.

Risco aberto: `dialogo.js` não tem teste automatizado (o teste de servidor só confere o HTML). O comportamento com JavaScript (modal, foco, Esc, endereço) foi conferido à mão no Chromium e depende do E2E Playwright em 375 px (RF-13) para ficar coberto.

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
