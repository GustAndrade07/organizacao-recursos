# Design tokens

Fonte da verdade: Figma, página **Design System** → variáveis `Primitives`, `Color` e `Dimensions`, estilos de texto e de sombra.
Em código: [`src/main/resources/static/css/tokens.css`](../../src/main/resources/static/css/tokens.css).

> O plano Figma é Starter (1 modo por coleção), então só existe o tema claro. Modo escuro fica fora do escopo da v1.

## Camadas

| Camada | Coleção no Figma | Prefixo CSS | Usar nas telas? |
|---|---|---|---|
| Primitivas | `Primitives` | `--gray-*`, `--blue-*`, `--green-*`, `--amber-*`, `--red-*`, `--violet-*`, `--orange-*`, `--white` | **Não** |
| Cor semântica | `Color` | `--color-*` | Sim |
| Dimensões | `Dimensions` | `--space-*`, `--radius-*`, `--border-width-*`, `--size-*`, `--breakpoint-*` | Sim |
| Tipografia | estilos de texto | `--font-*` (shorthand `font:`) | Sim |
| Sombra | estilos de efeito | `--shadow-*` | Sim |

## Cor semântica

| Token | Primitiva | Uso |
|---|---|---|
| `--color-bg-page` | gray-50 | Fundo da página |
| `--color-bg-surface` | white | Cards, formulários, tabelas |
| `--color-bg-subtle` | gray-100 | Linhas alternadas, áreas secundárias |
| `--color-bg-disabled` | gray-200 | Controle desabilitado |
| `--color-bg-brand` / `-hover` | blue-600 / blue-700 | Botão primário |
| `--color-bg-danger` / `-hover` | red-600 / red-700 | Ação destrutiva (cancelar reserva) |
| `--color-bg-{info,success,warning,danger}-subtle` | 50 de cada cor | Fundo de alertas |
| `--color-text-primary` | gray-900 | Texto principal |
| `--color-text-secondary` | gray-600 | Texto de apoio |
| `--color-text-muted` | gray-500 | Legendas, placeholders |
| `--color-text-disabled` | gray-400 | Texto desabilitado (não precisa de contraste AA) |
| `--color-text-on-brand` | white | Texto sobre `bg-brand` e `bg-danger` |
| `--color-text-brand` | blue-700 | Links |
| `--color-text-{info,success,warning,danger}` | 700/800 de cada cor | Texto de alertas e erros |
| `--color-border-subtle` | gray-200 | Divisórias |
| `--color-border-default` | gray-300 | Contorno de cards |
| `--color-border-input` | gray-500 | Contorno de campos (contraste ≥ 3:1) |
| `--color-border-focus` | blue-600 | Anel de foco |
| `--color-border-{info,success,warning,danger}` | 600 de cada cor | Borda de alertas e campo com erro |

### Estados da reserva (RN-05)

| Estado | Fundo | Texto |
|---|---|---|
| SOLICITADA | `--color-status-solicitada-bg` (amber-100) | `--color-status-solicitada-fg` (amber-800) |
| APROVADA | `--color-status-aprovada-bg` (blue-100) | `--color-status-aprovada-fg` (blue-800) |
| EM_USO | `--color-status-em-uso-bg` (violet-100) | `--color-status-em-uso-fg` (violet-800) |
| CONCLUÍDA | `--color-status-concluida-bg` (green-100) | `--color-status-concluida-fg` (green-800) |
| REJEITADA | `--color-status-rejeitada-bg` (red-100) | `--color-status-rejeitada-fg` (red-800) |
| CANCELADA | `--color-status-cancelada-bg` (gray-200) | `--color-status-cancelada-fg` (gray-700) |
| NAO_COMPARECEU | `--color-status-nao-compareceu-bg` (orange-100) | `--color-status-nao-compareceu-fg` (orange-800) |

O badge sempre mostra o **nome** do estado, além da cor (cor não pode ser o único indicador).

## Dimensões

| Token | Valor | Uso |
|---|---|---|
| `--space-1` … `--space-16` | 4, 8, 12, 16, 20, 24, 32, 40, 48, 64 px | Gap e padding (escala de 4 px) |
| `--radius-sm` / `-md` / `-lg` / `-full` | 4 / 8 / 12 / 9999 px | Badge pequeno / campos e botões / cards / pílulas |
| `--border-width-default` / `-focus` | 1 / 2 px | Contorno / anel de foco |
| `--size-control-sm` / `-md` | 36 / 44 px | Altura de controle; 44 px é o alvo de toque mínimo |
| `--size-icon` | 20 px | Ícones |
| `--breakpoint-mobile` / `-tablet` / `-desktop` | 375 / 768 / 1280 px | Referência (media queries não aceitam `var()`; use os números) |

## Tipografia

Família Inter, com fallback `system-ui`. Use com `font: var(--font-body-default);`.

| Estilo no Figma | Token CSS | Peso · tamanho/altura |
|---|---|---|
| Heading/H1 | `--font-heading-h1` | 700 · 28/36 px, espaçamento −0,4 px |
| Heading/H2 | `--font-heading-h2` | 600 · 22/28 px, espaçamento −0,2 px |
| Heading/H3 | `--font-heading-h3` | 600 · 18/24 px |
| Body/Default | `--font-body-default` | 400 · 16/24 px (mínimo para campos, evita zoom no iOS) |
| Body/Small | `--font-body-small` | 400 · 14/20 px |
| Label/Default | `--font-label-default` | 500 · 14/20 px |
| Label/Small | `--font-label-small` | 500 · 12/16 px, espaçamento 0,2 px |
| Caption | `--font-caption` | 400 · 12/16 px |

## Sombra

| Estilo no Figma | Token CSS | Uso |
|---|---|---|
| Shadow/Card | `--shadow-card` | Cards e tabelas |
| Shadow/Overlay | `--shadow-overlay` | Menu aberto, diálogos |

## Acessibilidade

- Texto sobre `--color-bg-surface` e `--color-bg-page` atinge WCAG AA (≥ 4,5:1), exceto `--color-text-disabled`.
- **Não** use `--color-text-muted` sobre `--color-bg-subtle` (4,34:1, abaixo do AA). Ali use `--color-text-secondary`.
- Badges de estado: todas as combinações `-fg` sobre `-bg` ficam acima de 6:1.
- Foco visível: `outline: var(--border-width-focus) solid var(--color-border-focus);`
- Controles interativos com altura mínima `--size-control-md` (44 px).
