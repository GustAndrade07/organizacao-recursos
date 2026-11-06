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

## Histórico da reserva

`GET /reservas/{id}/historico` (`HistoricoController`, template `historico.html`) é a tela 11, só leitura (RF-10, RN-09, RN-10). **Não lê a auditoria**; contrato com o back-end, a confirmar com o Pedro:

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `reserva` | `recurso`, `solicitante`, `data` (`LocalDate`), `horario` (texto já formatado, ex. "14:00 às 15:30") e `estado` (nome escrito do estado atual, ex. "Em uso") | A página mostra só o título e o link Voltar |
| `eventos` | Lista **do mais antigo ao mais recente** (a tela não reordena). Cada item: `instante` (`LocalDateTime`), `de` (nulo na criação da reserva, senão o nome do estado anterior), `para` (nome do estado novo), `usuario` (nome) e `motivo` (nulo ou vazio mostra "Não informado") | Linha do tempo vazia |
| `voltar` | Endereço do link "Voltar" (opcional) | O link vai para `/` |

- Os nomes de estado são os sete da RN-05 por extenso: Solicitada, Aprovada, Em uso, Concluída, Rejeitada, Cancelada e Não compareceu. A cor do badge vem do nome; um nome fora da lista aparece escrito, sem cor.
- Acesso (RN-11): Responsável e Administrador veem; para o Solicitante o back-end deve responder **403**, que é a página de erro 403 que já existe (não há tela própria). A rota exige `{id}` de uma reserva existente; o 404 também é do back-end.
- Somente leitura: a tela não tem formulário, botão nem link de editar ou apagar, e diz "Os registros são somente leitura: não podem ser alterados nem apagados." O motivo da rejeição (RN-09) aparece na linha da mudança para Rejeitada.
- Onde fica o link "Ver histórico" ainda é decisão do back-end (sugestão: uma ligação em cada linha da retirada e devolução e no detalhe da reserva para esses perfis).
- Acessibilidade: lista ordenada (`<ol>`), data e hora em `<time datetime>`, estado sempre escrito além da cor e um único `<h1>`.

## Relatório de utilização

`GET /relatorios/utilizacao?inicio=&fim=` (`RelatorioController`, template `relatorio.html`) é a tela 12 (RF-12, RN-13; a rota já está no [PRD](../prd.md)). O controller devolve o período ao filtro e recusa fim anterior ao início. **Não calcula o relatório**; contrato com o back-end, a confirmar com o Pedro:

| Atributo de modelo | Conteúdo | Sem ele |
|---|---|---|
| `filtro` | `inicio` e `fim` (`LocalDate`; o controller já preenche com os parâmetros `AAAA-MM-DD`) | — |
| `erros` | Mapa com a chave `fim` para o texto do erro; o controller já preenche quando `fim` é anterior a `inicio` | Sem erro |
| `relatorio` | `recursos` e `professores` (listas de `nome`, `reservas` e `horas`, esta já formatada como "18 h" ou "12 h 30 min"), `totalReservas` (número), `totalHoras` (texto) e `conflitos` (número) | A página mostra só o filtro e a nota |

- Com `erros` a tela não mostra o relatório, mesmo que `relatorio` exista; o back-end deve ignorar `fim` anterior a `inicio` antes de calcular. `totalReservas` igual a 0 mostra só "Nenhuma reserva no período", sem tabelas.
- "Professor" é quem pediu a reserva (o solicitante). Os totais valem para os dois blocos de tabela; reserva cancelada não conta como uso (nota fixa na tela). Como o relatório conta a reserva que cruza a borda do período é decisão do back-end (caso obrigatório do PRD).
- Acesso (RN-13): Responsável e Administrador; o Solicitante recebe 403 do back-end.
- `relatorio-form.js` bloqueia fim anterior ao início antes do envio, com o mesmo texto do servidor, e leva o foco ao campo. Sem JavaScript o servidor devolve o erro.
- Tabelas no desktop e cartões no mobile com o mesmo HTML (cada célula traz o rótulo em `data-rotulo`).
- Acessibilidade: rótulos visíveis ligados por `for`, erro em texto junto ao campo (`role="alert"`, `aria-invalid`, `aria-describedby`), `<caption>` em cada tabela e um único `<h1>`.
