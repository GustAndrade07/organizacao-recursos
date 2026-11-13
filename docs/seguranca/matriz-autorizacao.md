# Matriz de autorização (RF-01, Aula 12)

> **Fonte única.** `MatrizAutorizacaoTest` lê as duas tabelas abaixo e confere cada célula com token de cada perfil (ou sem token). O mesmo teste falha se existir rota no código (API em `br.com.senac.recursos.controller` ou tela em `br.com.senac.recursos.web`) que não esteja aqui, ou linha aqui que não exista no código (regra 13 do `AGENTS.md`). Para mudar uma permissão: mude a linha, mude `SegurancaConfig` (ou a regra de posse no serviço) e rode o teste. **Nenhuma célula é apagada para o teste passar** (Etapa 5 do plano): célula vermelha vira `docs/defeitos/DEF-xxx.md` antes da correção.
>
> Última atualização: 13/11/2026 (seção nova "Rotas da documentação", RF-14). Antes: 12/11/2026 (telas ligadas aos services, D12 opção (a), ADR-0009). Antes: 11/11/2026 (PR #44, API sob `/api/**` ao lado das telas, ADR-0008).

## Legenda

| Célula | Significado | Como o teste confere |
|---|---|---|
| `401` | sem token, ou token inválido | status exatamente 401, `ProblemDetail` |
| `403` | perfil sem permissão (Spring Security) | status exatamente 403 |
| `login` | tela sem sessão: vai para o formulário de login | status 302 com `Location` em `/login` |
| `ok` | o perfil passa pela segurança | status diferente de 401 e 403 (o resultado de negócio, ex.: 409, é de outro teste) |
| `dono` | passa só na reserva da própria pessoa | na própria: diferente de 401 e 403; na de outro usuário: 403 |
| `não-dono` | passa só na reserva de **outra** pessoa (RN-06: ninguém aprova a própria) | na de outro: diferente de 401 e 403; na própria: 403 |

## API REST (`/api/**`, JWT no cabeçalho `Authorization`)

| Método | Rota | Anônimo | Solicitante | Responsável | Administrador | Regra |
|---|---|---|---|---|---|---|
| POST | /api/auth/login | ok | ok | ok | ok | RF-01 |
| GET | /api/usuarios/me | 401 | ok | ok | ok | RF-01 |
| GET | /api/usuarios | 401 | 403 | 403 | ok | 2.1 do plano |
| POST | /api/usuarios | 401 | 403 | 403 | ok | 2.1 do plano |
| GET | /api/recursos | 401 | ok | ok | ok | RF-02, RF-03 |
| GET | /api/recursos/disponiveis | 401 | ok | ok | ok | RF-03 |
| GET | /api/recursos/{id} | 401 | ok | ok | ok | RF-02 |
| POST | /api/recursos | 401 | 403 | 403 | ok | RF-02 |
| PUT | /api/recursos/{id} | 401 | 403 | 403 | ok | RF-02 |
| GET | /api/recursos/{id}/manutencoes | 401 | ok | ok | ok | RF-08 |
| POST | /api/recursos/{id}/manutencoes | 401 | 403 | 403 | ok | RF-08 |
| POST | /api/reservas | 401 | ok | ok | ok | RF-04 |
| GET | /api/reservas | 401 | ok | ok | ok | RF-04 (Solicitante só vê as próprias) |
| GET | /api/reservas/{id} | 401 | dono | ok | ok | RF-04 |
| PUT | /api/reservas/{id}/periodo | 401 | dono | dono | dono | RF-04, RN-07 |
| POST | /api/reservas/{id}/cancelamento | 401 | dono | dono | dono | RF-04, RN-07 |
| POST | /api/reservas/{id}/aprovacao | 401 | 403 | não-dono | 403 | RF-07, RN-06 |
| POST | /api/reservas/{id}/rejeicao | 401 | 403 | ok | 403 | RF-07 |
| POST | /api/reservas/{id}/retirada | 401 | 403 | ok | 403 | RF-09 |
| POST | /api/reservas/{id}/devolucao | 401 | 403 | ok | 403 | RF-09 |
| POST | /api/reservas/{id}/nao-comparecimento | 401 | 403 | ok | 403 | RN-05 |
| GET | /api/reservas/{id}/historico | 401 | 403 | ok | ok | RF-10, RN-11 |
| GET | /api/relatorios/utilizacao | 401 | 403 | ok | ok | RF-12, RN-13 |

Rota sob `/api/**` fora desta tabela: **negada** (`denyAll` em `SegurancaConfig`): sem token 401, com token 403. Esquecer de cadastrar uma rota fecha o acesso em vez de abrir.

## Rotas da documentação (RF-14, springdoc)

Decidido em 13/11/2026, com a entrada do `springdoc-openapi`: `GET /v3/api-docs`, `GET /v3/api-docs/**`, `GET /swagger-ui.html` e `GET /swagger-ui/**` **exigem sessão** (qualquer perfil logado), como as telas. Sem sessão, 302 para `/login`.

Por que ficam **fora das duas tabelas acima:** elas não são rotas do projeto — quem as serve é a biblioteca (pacote `org.springdoc`), e `MatrizAutorizacaoTest` compara a matriz com as rotas dos pacotes `controller` e `web`. Pôr uma linha aqui quebraria essa comparação sem acrescentar segurança.

Por que **autenticada e não pública:** a documentação descreve cada rota, parâmetro e código de resposta da API; é informação útil para quem já entrou e desnecessária para quem não entrou. Quem for demonstrar abre `http://localhost:8080/swagger-ui/index.html` **depois** de entrar pelo `/login`. Se o grupo quiser a documentação aberta, muda esta linha, muda `SegurancaDasTelasConfig` e o `OpenApiDocumentacaoTest.semSessaoADocumentacaoLevaAoLogin` fica vermelho até ser ajustado — de propósito.

Conferido por `OpenApiDocumentacaoTest`: sem sessão vai para o login; com sessão, o JSON descreve **exatamente** as rotas `/api/**` desta matriz (nem a mais, nem a menos).

## Telas Thymeleaf (cadeia própria, sessão do `/login`)

Decisão **D12, opção (a)** (12/11/2026, ADR-0009): formulário em `/login` com sessão do Spring Security, CSRF ligado e perfil conferido por rota em `SegurancaDasTelasConfig`. As telas chamam os mesmos services da API, no mesmo processo, então cada linha abaixo **espelha a linha da API** que a tela usa (quem não pode chamar a API também não vê a tela). A posse (`dono`, `não-dono`) continua no `ReservaService`. Na tabela, `login` quer dizer que quem não está logado é levado ao formulário de login (302 para `/login`, com `?destino=` no `GET`); é o "401" das telas.

| Método | Rota | Anônimo | Solicitante | Responsável | Administrador | Regra |
|---|---|---|---|---|---|---|
| GET | / | login | ok | ok | ok | RF-01 (leva à primeira tela do menu do perfil) |
| GET | /login | ok | ok | ok | ok | RF-01, tela 1 |
| GET | /recursos/disponiveis | login | ok | ok | ok | RF-03 (como `GET /api/recursos/disponiveis`) |
| GET | /reservas/nova | login | ok | ok | ok | RF-04 |
| POST | /reservas | login | ok | ok | ok | RF-04 (como `POST /api/reservas`) |
| GET | /reservas/minhas | login | ok | ok | ok | RF-04 (só as do usuário logado, para todos os perfis) |
| GET | /reservas/{id} | login | dono | ok | ok | RF-04 (como `GET /api/reservas/{id}`) |
| POST | /reservas/{id} | login | dono | dono | dono | RF-04, RN-07 (como `PUT /api/reservas/{id}/periodo`) |
| POST | /reservas/{id}/cancelar | login | dono | dono | dono | RF-04, RN-07 |
| GET | /reservas/{id}/historico | login | 403 | ok | ok | RF-10, RN-11 |
| GET | /aprovacoes | login | 403 | ok | 403 | RF-07 |
| POST | /aprovacoes/{id}/aprovar | login | 403 | não-dono | 403 | RF-07, RN-06 |
| POST | /aprovacoes/{id}/rejeitar | login | 403 | ok | 403 | RF-07 |
| GET | /retirada-devolucao | login | 403 | ok | 403 | RF-09 |
| POST | /retirada-devolucao/{id}/retirar | login | 403 | ok | 403 | RF-09 |
| POST | /retirada-devolucao/{id}/devolver | login | 403 | ok | 403 | RF-09 |
| POST | /retirada-devolucao/{id}/nao-compareceu | login | 403 | ok | 403 | RN-05 |
| GET | /relatorios/utilizacao | login | 403 | ok | ok | RF-12, RN-13 |
| GET | /admin/recursos | login | 403 | 403 | ok | RF-02 |
| GET | /admin/recursos/novo | login | 403 | 403 | ok | RF-02 |
| GET | /admin/recursos/{id} | login | 403 | 403 | ok | RF-02 |
| POST | /admin/recursos | login | 403 | 403 | ok | RF-02 |
| POST | /admin/recursos/{id} | login | 403 | 403 | ok | RF-02 |
| GET | /admin/recursos/{id}/manutencao | login | 403 | 403 | ok | RF-08 |
| POST | /admin/recursos/{id}/manutencao | login | 403 | 403 | ok | RF-08 |
| GET | /admin/usuarios | login | 403 | 403 | ok | RF-01, tela 16 (como `GET /api/usuarios`) |
| GET | /admin/usuarios/novo | login | 403 | 403 | ok | RF-01, tela 16 |
| POST | /admin/usuarios | login | 403 | 403 | ok | RF-01, tela 16 (como `POST /api/usuarios`) |

URL de tela fora desta tabela: **negada** (`denyAll`, regra 13 do `AGENTS.md`): sem sessão leva ao login, com sessão 403. Esquecer de cadastrar uma rota fecha o acesso em vez de abrir. A página 404 aparece para recurso ou reserva que não existe numa rota da tabela.

## Fora da matriz (não são rotas de negócio)
- `POST /login` e `POST /logout`: atendidos pelo filtro do Spring Security, não por um controller (o login é aberto; o logout encerra a sessão e volta a `/login`).
- `GET /actuator/health`: aberta, só diz `{"status":"UP"}` (README, "como subir").
- `/error` e as páginas de erro: abertas para o Spring devolver o corpo de erro (HTML para o navegador, JSON para a API); sem stack trace (`server.error.include-*` em `application.yml`).
- Arquivos estáticos (`/css/**`, `/js/**`): abertos.

## Decisões e justificativas
- **Duas cadeias de segurança** (`SegurancaConfig`): `/api/**` stateless com JWT; o resto (telas) na cadeia das telas. Proposta do Gustavo na revisão do PR #44.
- **CSRF desligado só na API.** A API é stateless: o token vai no cabeçalho `Authorization` (nunca em cookie) e não há sessão; um site de terceiros não consegue fazer o navegador anexar esse cabeçalho, então não existe a requisição forjada que o CSRF previne. É a justificativa escrita da regra `java:S4502` do SonarCloud, suprimida na própria linha de `SegurancaConfig` com `NOSONAR` (o comentário aponta para cá). Na cadeia das telas o CSRF fica **ligado** (padrão do Spring): a sessão vai em cookie, então todo formulário leva o `_csrf`; o `ReservaE2ETest` confere que o token vai no corpo enviado e o `MatrizAutorizacaoTest` envia todo `POST` de tela com ele.
- **Posse no serviço, perfil na segurança.** "Dono" e "não-dono" dependem do dado (quem criou a reserva), então são conferidos em `ReservaService` e respondem 403 com `ProblemDetail` do tipo `acesso-negado`.
- **D10 decidida em 12/11/2026 (Pedro): fica como está no código.** O Administrador **não** cancela nem altera reserva de outra pessoa (célula `dono`); o Responsável **pode** rejeitar a própria reserva (RN-06 só proíbe aprovar). Ver seção 9 do plano. A D12 (autenticação das telas) foi decidida em 12/11/2026: opção (a), ADR-0009.
