# BDD — aprovação e bloqueio (Aula 9)

> Cada cenário tem **um único Quando** e **um Então observável**.
> **Desde 13/11/2026 estes cenários são executáveis:** viraram `src/test/resources/features/aprovacao-bloqueio.feature` (Cucumber-JVM, passos em `src/test/java/br/com/senac/recursos/bdd/`, chamando a API por MockMvc com o token de cada perfil, contra PostgreSQL de verdade em Testcontainers). Eles rodam no `verify`; cenário vermelho quebra o build. Resultado e limites em [`evidencia-tdd.md`](evidencia-tdd.md), seção 5.
> Regras de manutenção recebem `Clock` injetado: sem `Instant.now()` oculto e sem `Thread.sleep`.

## Cenários

### 1. Recurso restrito exige aprovação de um Responsável (RN-06, RF-07)
```gherkin
Dado um recurso restrito e uma reserva SOLICITADA por um Solicitante
Quando um Responsável aprova a reserva
Então a reserva passa para APROVADA
```

### 2. Solicitante não aprova a própria reserva (RN-06)
```gherkin
Dado uma reserva SOLICITADA de um recurso restrito feita por um Solicitante
Quando o mesmo Solicitante tenta aprová-la
Então a resposta é 403 e a reserva continua SOLICITADA
```

### 3. Recurso em manutenção não pode ser reservado (RN-04, RF-08)
```gherkin
Dado um recurso em manutenção de 10:00 a 12:00
Quando um Solicitante reserva esse recurso de 10:30 a 11:30
Então a resposta é 409 e nenhuma reserva é criada
```

### 4. Transições de estado (RN-05)
```gherkin
Dado uma reserva APROVADA
Quando o Responsável registra a retirada
Então a reserva passa para EM_USO
```
(repetir por transição válida e por transição inválida, ex.: CONCLUÍDA → APROVADA deve ser recusada)

## Tabela de fronteiras — manutenção (trinca −1, 0, +1)

Manutenção de **10:00 a 12:00**.

| Reserva | Esperado | Por quê |
|---|---|---|
| 09:00 a 10:00 (termina exatamente no início) | aceita | sem sobreposição (a confirmar: intervalo semiaberto) |
| 12:00 a 13:00 (começa exatamente no fim) | aceita | idem |
| 09:00 a 10:00:01 (cruza 1 segundo) | recusada | sobrepõe |
| 11:59:59 a 13:00 (cruza 1 segundo no fim) | recusada | sobrepõe |

## Perguntas em aberto (não inventar política)
- [x] O intervalo de reserva/manutenção é semiaberto `[início, fim)`? **Decidido (D1, 05/11/2026, Gustavo; ADR-0002):** sim, `[início, fim)`. É o que a constraint do banco usa e o que os cenários de fronteira deste arquivo executam.
- (acrescentar aqui qualquer fronteira que o PRD não define)

## Classificação dos cenários
| Cenário | Tipo de teste | Onde roda hoje |
|---|---|---|
| 1 | API (MockMvc) | `aprovacao-bloqueio.feature` ("Recurso restrito exige aprovação...") e `ReservaApiControllerTest.Aprovacao` |
| 2 | API (MockMvc) | `aprovacao-bloqueio.feature` (duas versões: Solicitante e Responsável) e `ReservaApiControllerTest.Aprovacao` |
| 3 | unidade (regra) + API | `aprovacao-bloqueio.feature` (Esquema do Cenário com as 5 fronteiras) e `ReservaApiControllerTest.Manutencao` |
| 4 | unidade (máquina de estados) + API | `aprovacao-bloqueio.feature` (retirada/devolução, transição inválida, não comparecimento), `EstadoReservaTest` e `ReservaApiControllerTest.CicloDeUso` |

> As fronteiras da tabela acima aparecem **duas vezes** de propósito: no cenário BDD (linguagem de negócio, para a mesa ler) e no teste parametrizado de API (`@CsvSource`). Se um dia divergirem, os dois ficam vermelhos.
