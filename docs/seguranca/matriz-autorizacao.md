# Matriz de autorização (RF-01, Aula 12)

> **Fonte única.** `MatrizAutorizacaoTest` lê a tabela abaixo e confere cada célula pela API, com token de cada perfil. O mesmo teste falha se existir rota no código (`br.com.senac.recursos.controller`) que não esteja aqui. Para mudar uma permissão: mude a linha, mude `SegurancaConfig` (ou a regra de posse no serviço) e rode o teste. **Nenhuma célula é apagada para o teste passar** (Etapa 5 do plano): célula vermelha vira `docs/defeitos/DEF-xxx.md` antes da correção.
>
> Última atualização: 11/11/2026 (back-end em camadas, branch `feat/backend-camadas`).

## Legenda

| Célula | Significado | Como o teste confere |
|---|---|---|
| `401` | sem token, ou token inválido | status exatamente 401, `ProblemDetail` |
| `403` | perfil sem permissão (Spring Security) | status exatamente 403 |
| `ok` | o perfil passa pela segurança | status diferente de 401 e 403 (o resultado de negócio, ex.: 409, é de outro teste) |
| `dono` | passa só na reserva da própria pessoa | na própria: diferente de 401 e 403; na de outro usuário: 403 |
| `não-dono` | passa só na reserva de **outra** pessoa (RN-06: ninguém aprova a própria) | na de outro: diferente de 401 e 403; na própria: 403 |

## Matriz

| Método | Rota | Anônimo | Solicitante | Responsável | Administrador | Regra |
|---|---|---|---|---|---|---|
| POST | /auth/login | ok | ok | ok | ok | RF-01 |
| GET | /usuarios/me | 401 | ok | ok | ok | RF-01 |
| GET | /usuarios | 401 | 403 | 403 | ok | 2.1 do plano |
| POST | /usuarios | 401 | 403 | 403 | ok | 2.1 do plano |
| GET | /recursos | 401 | ok | ok | ok | RF-02, RF-03 |
| GET | /recursos/disponiveis | 401 | ok | ok | ok | RF-03 |
| GET | /recursos/{id} | 401 | ok | ok | ok | RF-02 |
| POST | /recursos | 401 | 403 | 403 | ok | RF-02 |
| PUT | /recursos/{id} | 401 | 403 | 403 | ok | RF-02 |
| GET | /recursos/{id}/manutencoes | 401 | ok | ok | ok | RF-08 |
| POST | /recursos/{id}/manutencoes | 401 | 403 | 403 | ok | RF-08 |
| POST | /reservas | 401 | ok | ok | ok | RF-04 |
| GET | /reservas | 401 | ok | ok | ok | RF-04 (Solicitante só vê as próprias) |
| GET | /reservas/{id} | 401 | dono | ok | ok | RF-04 |
| PUT | /reservas/{id}/periodo | 401 | dono | dono | dono | RF-04, RN-07 |
| POST | /reservas/{id}/cancelamento | 401 | dono | dono | dono | RF-04, RN-07 |
| POST | /reservas/{id}/aprovacao | 401 | 403 | não-dono | 403 | RF-07, RN-06 |
| POST | /reservas/{id}/rejeicao | 401 | 403 | ok | 403 | RF-07 |
| POST | /reservas/{id}/retirada | 401 | 403 | ok | 403 | RF-09 |
| POST | /reservas/{id}/devolucao | 401 | 403 | ok | 403 | RF-09 |
| POST | /reservas/{id}/nao-comparecimento | 401 | 403 | ok | 403 | RN-05 |
| GET | /reservas/{id}/historico | 401 | 403 | ok | ok | RF-10, RN-11 |
| GET | /relatorios/utilizacao | 401 | 403 | ok | ok | RF-12, RN-13 |

## Fora da matriz (não são rotas de negócio)
- `GET /actuator/health`: aberta, só diz `{"status":"UP"}` (README, "como subir").
- `/error`: aberta para o Spring devolver o corpo de erro; sem stack trace (`server.error.include-*` em `application.yml`).
- Qualquer outra rota: **negada** (`denyAll` em `SegurancaConfig`): sem token 401, com token 403. Esquecer de cadastrar uma rota fecha o acesso em vez de abrir.

## Decisões e justificativas
- **CSRF desligado na API.** A API é stateless: o token vai no cabeçalho `Authorization`, nunca em cookie, e não há sessão. Um site de terceiros não consegue fazer o navegador anexar esse cabeçalho, então não existe a requisição forjada que o CSRF previne. Justificativa para o hotspot do SonarCloud (regra de "desabilitar proteção CSRF"), a marcar como **Safe** por quem administra o projeto no SonarCloud (Gustavo).
- **Posse no serviço, perfil na segurança.** "Dono" e "não-dono" dependem do dado (quem criou a reserva), então são conferidos em `ReservaService` e respondem 403 com `ProblemDetail` do tipo `acesso-negado`.
- **Pendentes do grupo (não decididas aqui):** se o Administrador pode cancelar reserva de outra pessoa; se o Responsável pode rejeitar a própria reserva. Hoje: não e sim, respectivamente (ver seção 9 do plano, D9).
