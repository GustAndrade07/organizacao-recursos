# Requisitos de desempenho (escrito antes da primeira rodada)

Escrito em 12/11/2026 pelo Leandro (meta escolhida por ele), **antes** de rodar o JMeter. A meta não muda depois de ver o resultado. Se ela não for cumprida, o resultado diz isso. Fonte das metas: `docs/prd.md` (RNF-01 e RNF-03). Aula 10.

## Metas

| Id | O que se mede | Meta | Como se confere |
|---|---|---|---|
| RNF-01 | Tempo de resposta de `GET /api/recursos/disponiveis` (busca de disponibilidade, RF-03) com 50 usuários simultâneos | **p95 ≤ 300 ms** | Percentil 95 do `elapsed` das amostras "Busca disponibilidade" no `.jtl` |
| RNF-03 | Respostas de `POST /api/reservas` (criação, RF-04) com 50 usuários disputando os mesmos horários | **só 201 ou 409, nunca 500 nem timeout** | Contagem dos códigos das amostras "Criar reserva" no `.jtl`: qualquer código fora de 201 e 409 reprova |

- O **409** é a resposta certa quando o horário já foi tomado. Não conta como erro.
- Erro de verdade é 5xx, timeout, conexão recusada ou outro código.
- O p50 e o p99 também são registrados no resultado, mas não têm meta.

## Cenário (fixo antes da rodada)
- **Usuários virtuais:** 50, todos Solicitantes fictícios de `perf/usuarios.csv`, um login por usuário.
- **Subida:** 10 s para os 50 entrarem (rampa).
- **Repetições:** 20 por usuário. Cada repetição faz uma busca e uma tentativa de reserva.
- **Pausa entre as requisições:** de 100 a 300 ms, aleatória.
- **Login:** uma vez por usuário, no começo, com a senha certa. O token é tirado da resposta, sem token fixo no plano. O login não entra na meta.
- **Disputa:** na repetição *n*, os 50 usuários pedem a mesma sala no mesmo horário (a hora *n* de um dia de 2027). Por horário, espera-se **1 × 201 e 49 × 409**.
- **Preparação** (setUp do plano, fora das métricas): o Administrador cria os 50 usuários e uma sala nova a cada rodada. O login e a senha do Administrador vêm por propriedade na linha de comando e nunca ficam no plano.

## Onde roda (e o que isso limita)
- **Só em modo CLI, contra a aplicação local** (`./mvnw spring-boot:run`) com o PostgreSQL do `docker-compose.yml`, como manda `perf/README.md`. Nunca contra ambiente compartilhado.
- O JMeter, a aplicação e o banco rodam **na mesma máquina** e disputam a CPU. O número vale para essa máquina e esse cenário, e o resultado diz qual máquina foi.
- O resultado **não** afirma nada sobre produção (AGENTS.md, seção 6).
- Um número só, de uma rodada, também não é uma garantia: o resultado registra quantas rodadas houve.
