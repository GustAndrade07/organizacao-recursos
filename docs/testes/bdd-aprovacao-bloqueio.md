# BDD — aprovação e bloqueio (Aula 9)

> Cada cenário tem **um único Quando** e **um Então observável**.
> Vira `src/test/resources/features/aprovacao-bloqueio.feature` (Cucumber-JVM, steps chamando a API por MockMvc com token de cada perfil).
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
- [ ] O intervalo de reserva/manutenção é semiaberto `[início, fim)`? Confirmar no PRD e com o professor.
- (acrescentar aqui qualquer fronteira que o PRD não define)

## Classificação dos cenários
| Cenário | Tipo de teste |
|---|---|
| 1 | API (MockMvc) |
| 2 | API (MockMvc) |
| 3 | unidade (regra) + API |
| 4 | unidade (máquina de estados) + API |
