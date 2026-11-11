# Matriz de autorização (RF-01, Aula 12)

> **Fonte única.** `MatrizAutorizacaoTest` lê as duas tabelas abaixo e confere cada célula com token de cada perfil (ou sem token). O mesmo teste falha se existir rota no código (API em `br.com.senac.recursos.controller` ou tela em `br.com.senac.recursos.web`) que não esteja aqui, ou linha aqui que não exista no código (regra 13 do `AGENTS.md`). Para mudar uma permissão: mude a linha, mude `SegurancaConfig` (ou a regra de posse no serviço) e rode o teste. **Nenhuma célula é apagada para o teste passar** (Etapa 5 do plano): célula vermelha vira `docs/defeitos/DEF-xxx.md` antes da correção.
>
> Última atualização: 11/11/2026 (PR #44, `feat/backend-camadas`: API sob `/api/**` ao lado das telas, ADR-0008).

## Legenda

| Célula | Significado | Como o teste confere |
|---|---|---|
| `401` | sem token, ou token inválido | status exatamente 401, `ProblemDetail` |
| `403` | perfil sem permissão (Spring Security) | status exatamente 403 |
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

## Telas Thymeleaf (cadeia própria, sem JWT)

As telas **ainda não leem nem gravam dados** (quem grava é a API) e hoje são **abertas a todos**, como estavam antes do back-end. Como as telas vão autenticar (formulário com sessão, ou o JWT da API guardado em cookie) é a **decisão pendente D12** da seção 9 do plano, pedida na revisão do PR #44. Quando a D12 for decidida, as células abaixo mudam para o que o perfil pode ver, e o teste passa a cobrar isso. **Até lá, nenhuma tela pode exibir dado real** (risco registrado no ADR-0008).

| Método | Rota | Anônimo | Solicitante | Responsável | Administrador | Regra |
|---|---|---|---|---|---|---|
| GET | /login | ok | ok | ok | ok | RF-01, tela 1 |
| GET | /recursos/disponiveis | ok | ok | ok | ok | RF-03; D12 pendente |
| GET | /reservas/nova | ok | ok | ok | ok | RF-04; D12 pendente |
| GET | /reservas/minhas | ok | ok | ok | ok | RF-04; D12 pendente |
| GET | /reservas/{id} | ok | ok | ok | ok | RF-04; D12 pendente |
| GET | /reservas/{id}/historico | ok | ok | ok | ok | RF-10; D12 pendente (na API, só Responsável e Administrador) |
| GET | /aprovacoes | ok | ok | ok | ok | RF-07; D12 pendente |
| GET | /retirada-devolucao | ok | ok | ok | ok | RF-09; D12 pendente |
| GET | /relatorios/utilizacao | ok | ok | ok | ok | RF-12; D12 pendente (na API, só Responsável e Administrador) |
| GET | /admin/recursos | ok | ok | ok | ok | RF-02; D12 pendente |
| GET | /admin/recursos/novo | ok | ok | ok | ok | RF-02; D12 pendente |
| GET | /admin/recursos/{id} | ok | ok | ok | ok | RF-02; D12 pendente |
| GET | /admin/recursos/{id}/manutencao | ok | ok | ok | ok | RF-08; D12 pendente |

## Fora da matriz (não são rotas de negócio)
- `GET /actuator/health`: aberta, só diz `{"status":"UP"}` (README, "como subir").
- `/error` e as páginas de erro: abertas para o Spring devolver o corpo de erro (HTML para o navegador, JSON para a API); sem stack trace (`server.error.include-*` em `application.yml`).
- Arquivos estáticos (`/css/**`, `/js/**`): abertos.

## Decisões e justificativas
- **Duas cadeias de segurança** (`SegurancaConfig`): `/api/**` stateless com JWT; o resto (telas) na cadeia das telas. Proposta do Gustavo na revisão do PR #44.
- **CSRF desligado nas duas cadeias, por motivos diferentes.** Na API: ela é stateless, o token vai no cabeçalho `Authorization` (nunca em cookie) e não há sessão; um site de terceiros não consegue fazer o navegador anexar esse cabeçalho, então não existe a requisição forjada que o CSRF previne. Nas telas: hoje elas só têm rotas GET, sem sessão nem login, então não há o que forjar; com o CSRF ligado, o Spring colocaria um `_csrf` em todo formulário e mudaria o que as telas enviam (os E2E do `ReservaE2ETest` conferem o corpo e falharam assim no `verify` de 11/11/2026). O CSRF das telas **volta ligado** no PR que decidir a D12 e criar a primeira rota de tela que grave com sessão. Justificativa para os dois hotspots do SonarCloud (regra de "desabilitar proteção CSRF"), a marcar como **Safe** por quem administra o projeto no SonarCloud (Gustavo).
- **Posse no serviço, perfil na segurança.** "Dono" e "não-dono" dependem do dado (quem criou a reserva), então são conferidos em `ReservaService` e respondem 403 com `ProblemDetail` do tipo `acesso-negado`.
- **Pendentes do grupo (não decididas aqui):** D10 (se o Administrador pode cancelar reserva de outra pessoa; se o Responsável pode rejeitar a própria reserva; hoje: não e sim) e D12 (autenticação das telas). Ver seção 9 do plano.
