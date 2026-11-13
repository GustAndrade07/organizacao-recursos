# Roteiro da demonstração (8 minutos)

> Entregável da especificação ("Apresentação e demonstração do sistema"), Aula 14. **Sem aplicação executando é limitador de nota**, então a ordem abaixo começa por deixar a aplicação de pé **antes** da apresentação começar.
>
> Escrito em 13/11/2026. **O ensaio cronometrado ainda não foi feito** — os tempos abaixo são alvo, não medida. Quem ensaiar preenche a tabela de `ensaio.md` com o tempo real.

## 0. Antes de entrar na sala (10 min, não conta no tempo)

| # | O que fazer | Como conferir que deu certo |
|---|---|---|
| 1 | `docker compose up -d` | `docker compose ps` mostra o serviço de pé |
| 2 | Subir a aplicação com o administrador inicial: `ADMIN_LOGIN=admin ADMIN_SENHA='<senha escolhida na hora>' ./mvnw spring-boot:run` | log com `Started RecursosApplication` |
| 3 | Abrir http://localhost:8080/actuator/health | responde `{"status":"UP"}` |
| 4 | Carregar a massa de demonstração (seção 1) | o script imprime os ids criados |
| 5 | Abrir as abas já logadas: `/login` (Solicitante), `/aprovacoes` (Responsável), `/admin/recursos` (Administrador) | três janelas prontas; **nada de digitar senha na frente da banca** |
| 6 | Deixar abertos, sem mostrar ainda: o run verde do CI, o Quality Gate do SonarCloud e `target/site/jacoco/index.html` | abas carregadas, para não esperar carregamento no meio da fala |

**Se a aplicação não subir:** apresentar com o `verify` local e dizer o que falhou, sem improvisar correção na hora. O tempo de demo passa a ser de testes e evidências.

## 1. Massa de dados da demonstração

A massa é criada **pela própria API**, com o token do administrador inicial — nada de `insert` solto no banco, para a demonstração usar os mesmos caminhos que o usuário usa.

Salve como `demo.sh` fora do repositório (ele pede a senha e não a escreve em arquivo):

```bash
#!/usr/bin/env bash
set -euo pipefail
API=http://localhost:8080/api
read -rsp 'Senha do admin: ' SENHA; echo
TOKEN=$(curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
        -d "{\"login\":\"admin\",\"senha\":\"$SENHA\"}" | sed -E 's/.*"token":"([^"]+)".*/\1/')
auth=(-H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json')

# Pessoas (a senha de demonstração é só para a mesa; trocar depois)
for p in '{"nome":"Ana Souza","login":"ana","senha":"demo-senha-123","perfil":"SOLICITANTE"}' \
         '{"nome":"Bruno Lima","login":"bruno","senha":"demo-senha-123","perfil":"RESPONSAVEL"}'; do
  curl -s "${auth[@]}" -X POST "$API/usuarios" -d "$p" > /dev/null
done

# Recursos: uma sala comum, um laboratório restrito, um professor e um projetor
curl -s "${auth[@]}" -X POST "$API/recursos" -d '{"tipo":"SALA","nome":"Sala C237","capacidade":40,"restrito":false}'
curl -s "${auth[@]}" -X POST "$API/recursos" -d '{"tipo":"SALA","nome":"Laboratório de redes","capacidade":20,"restrito":true}'
curl -s "${auth[@]}" -X POST "$API/recursos" -d '{"tipo":"PROFESSOR","nome":"Prof. Afonso","restrito":false}'
curl -s "${auth[@]}" -X POST "$API/recursos" -d '{"tipo":"MATERIAL","nome":"Projetor 01","restrito":false}'
echo
echo 'Massa criada. Nenhuma reserva foi criada de propósito: as reservas nascem na demonstração.'
```

> **Rodado em 13/11/2026** contra a aplicação do clone limpo (commit `9acbbcd`): o script criou as duas pessoas e os quatro recursos sem erro, e os passos 2 a 5 e 7 da demonstração responderam 201, 409, 201 (`SOLICITADA`), 200 e o histórico com `ana` criando e `bruno` aprovando. **Não** foi rodado na máquina da apresentação.

**Por que nenhuma reserva vem pronta:** a parte que a banca precisa ver acontecendo é a reserva sendo criada e a segunda sendo recusada. Reserva pré-criada deixa a dúvida de ter sido "preparada".

**Datas:** use um dia da semana seguinte, sempre o mesmo (ex.: `2026-11-18`, das 10:00 às 12:00). Horário no futuro, senão a RN-07 bloqueia cancelar e alterar no meio da demonstração.

## 2. Os 8 minutos

| Parte | Alvo | Quem | O que mostrar | O que dizer (uma frase) |
|---|---|---|---|---|
| 1. Problema e risco | 0:45 | Leandro | nada na tela (ou o PRD aberto) | "Reservar sala, professor e material sem conflito; o risco que zera tudo é reserva dupla" |
| 2. Arquitetura | 1:00 | Gustavo | `docs/arquitetura.md` (C4 de contêineres) e a constraint na `V1__nucleo_da_reserva.sql` | "A regra crítica vive no banco: `EXCLUDE USING gist` sobre `tstzrange`, não no serviço" |
| 3. Demonstração | 3:00 | Pedro conduz | ver o passo a passo abaixo | — |
| 4. Testes e CI | 1:30 | Gustavo | run verde do CI, `Tests run` e o JaCoCo; depois `docs/testes/mutacao-manual.md` | "O número que importa não é a cobertura: é a mutação — quebrei a constraint e X testes ficaram vermelhos" |
| 5. RTM e relatório | 1:00 | Leandro | `docs/rtm.md` e `docs/relatorio-qualidade.md` no commit da tag | "Cada requisito aponta para o teste e para o run que o prova" |
| 6. Limitações | 0:45 | Leandro | `docs/decisao-qualidade.md` | "O que não foi medido, os riscos aceitos e por que a recomendação é essa" |

### Passo a passo da parte 3 (3 minutos, sem digitar senha)

| # | Tela | Ação | O que a banca vê |
|---|---|---|---|
| 1 | Solicitante, `/recursos/disponiveis` | buscar sala para 18/11, 10:00–12:00 | a Sala C237 aparece livre |
| 2 | `/reservas/nova` | reservar a Sala C237 | "Reserva confirmada", estado **Aprovada** (sem recurso restrito, D10) |
| 3 | mesma tela, outra aba | tentar **o mesmo horário** de novo | mensagem de horário ocupado (**409**), sem reserva dupla |
| 4 | Solicitante | reservar o **Laboratório de redes** no mesmo dia, 14:00–16:00 | estado **Solicitada**: recurso restrito espera aprovação (RF-07) |
| 5 | Responsável, `/aprovacoes` | aprovar o pedido do laboratório | passa para **Aprovada** |
| 6 | Responsável | tentar aprovar uma reserva **própria** | recusado: ninguém aprova a própria reserva (RN-06) |
| 7 | `/reservas/{id}/historico` | abrir o histórico da primeira reserva | quem, quando e o estado anterior e novo de cada passo (RF-10) |
| 8 | Administrador, `/admin/recursos/{id}/manutencao` | bloquear a Sala C237 das 10:00 às 12:00 do dia 19 e tentar reservar nesse horário | recusa **de tipo próprio**: manutenção, não conflito (RN-04) |

**Se sobrar tempo (não planeje contar com ele):** `http://localhost:8080/swagger-ui/index.html` com a API documentada, e a tela em 375 px (celular).

**Se faltar tempo:** corte os passos 7 e 8 — o histórico e a manutenção têm teste citado na RTM e podem ser respondidos em vez de mostrados. **Nunca** corte os passos 2 e 3: são o limitador de nota.

## 3. Regras da demonstração

1. **Nada de digitar senha na frente da banca.** As abas já entram logadas (passo 5 da seção 0).
2. **Nada de terminal com erro vermelho na tela** sem explicar na hora o que é.
3. **Não dizer "está pronto para produção".** O que foi medido foi medido numa máquina; está escrito assim em todos os documentos.
4. Quem conduz a demonstração **não** é quem escreveu a tela: o combinado da prova oral vale aqui também.
5. Se algo falhar ao vivo: dizer o que era esperado, abrir o teste que cobre aquilo e seguir. Um erro explicado vale mais que um erro escondido.

## 4. O que ainda é humano

- **Ensaiar cronometrado** no notebook que será usado, pelo menos uma vez, e preencher a tabela de tempos em [`ensaio.md`](ensaio.md).
- Decidir quem fala cada parte (a sugestão acima segue as áreas, invertendo quem apresenta o quê).
- Conferir, no dia, que o Docker e o JDK 21 estão na máquina da apresentação.
