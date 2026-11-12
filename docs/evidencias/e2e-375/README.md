# Prints do E2E em 375 px (RF-13)

Gerados pelo E2E Playwright (`src/test/java/br/com/senac/recursos/e2e`) em **11/11/2026**, na branch
`satoworktree/eloquent-turing-f9j5ey` (PR #42), num container Linux com Chromium sem tela (Playwright 1.63.0,
Chromium 1243). Viewport de 375 × 812 px, `isMobile`, escala 1. O conjunto completo (35 prints) sai a cada run do
CI no artifact `e2e-375`; aqui fica só uma amostra.

As telas com dados usam `TelasComDados` (dados de teste no lugar do back-end, que ainda não existe). Os campos de
hora aparecem como "02:00 PM" porque o Chromium sem tela ignora o idioma pt-BR nesse controle nativo; o valor
enviado é `14:00`.

| Arquivo | O que mostra | Teste |
|---|---|---|
| `02-fluxo-1-resultados.png` | Resultados da busca em cartões | `ReservaE2ETest` (fluxo) |
| `02-fluxo-2-termino-antes-do-inicio.png` | RN-01 barrada antes do envio, erro junto ao campo e no resumo | `ReservaE2ETest` (fluxo) |
| `02-fluxo-3-enviando.png` | Botão "Enviando…" desabilitado depois do envio (um POST) | `ReservaE2ETest` (fluxo) |
| `02-nova-reserva-conflito.png` | Aviso de conflito (409) com "Voltar à busca" | `Responsividade375E2ETest` |
| `02-minhas.png` | Minhas reservas em cartões, Alterar e Cancelar só quando pode | `Responsividade375E2ETest` |
| `02-detalhe-cancelar.png` | Confirmação de cancelamento | `Responsividade375E2ETest` |
| `01-menu-aberto.png` | Menu do Solicitante aberto no celular | `AcessoE2ETest` |
| `01-login-bloqueado.png` | Login bloqueado depois de 5 falhas, com a senha certa: a mesma tela de senha errada, com o aviso em foco (gerado em 12/11/2026, branch `satoworktree/laughing-mayer-yvtj0y`, contra o back-end real) | `LimiteDeTentativasE2ETest` |
| `01-erro-403.png` | Página 403 | `Responsividade375E2ETest` |
| `03-retirada-nao-compareceu.png` | Confirmação de não compareceu | `Responsividade375E2ETest` |
| `04-relatorio.png` | Relatório em cartões | `Responsividade375E2ETest` |
