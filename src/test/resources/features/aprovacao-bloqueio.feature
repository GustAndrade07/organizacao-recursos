# language: pt
# Cenários escritos em docs/testes/bdd-aprovacao-bloqueio.md (Aula 9) e executados pelos passos de
# br.com.senac.recursos.bdd.AprovacaoBloqueioSteps: aplicação inteira, PostgreSQL de verdade (Testcontainers) e
# relógio fixo. Cada cenário tem um único "Quando" e um "Então" observável.
Funcionalidade: Aprovação de recurso restrito e bloqueio por manutenção
  Para não haver reserva indevida nem conflito de horário
  Como grupo responsável pelas regras RF-07, RF-08, RN-04, RN-05 e RN-06
  Queremos que a aprovação, o bloqueio e as transições de estado se comportem como está escrito

  Contexto:
    Dado o sistema sem reservas
    E um recurso restrito chamado "Laboratório de redes"
    E um recurso comum chamado "Sala C237"
    E um Solicitante chamado "ana"
    E um Responsável chamado "bruno"

  # Cenário 1 de docs/testes/bdd-aprovacao-bloqueio.md (RN-06, RF-07)
  Cenário: Recurso restrito exige aprovação de um Responsável
    Dado uma reserva de "ana" no "Laboratório de redes" das 10:00 às 11:00
    E a reserva está "SOLICITADA"
    Quando "bruno" aprova a reserva
    Então a reserva fica "APROVADA"
    E o histórico da reserva tem 2 registros, o último feito por "bruno"

  # Cenário 2 (RN-06): ninguém aprova a própria reserva
  Cenário: Solicitante não aprova a própria reserva
    Dado uma reserva de "ana" no "Laboratório de redes" das 10:00 às 11:00
    Quando "ana" tenta aprovar a reserva
    Então a resposta é 403
    E a reserva continua "SOLICITADA"

  # Cenário 2b (RN-06): nem um Responsável, quando a reserva é dele
  Cenário: Responsável não aprova a própria reserva
    Dado uma reserva de "bruno" no "Laboratório de redes" das 10:00 às 11:00
    Quando "bruno" tenta aprovar a reserva
    Então a resposta é 403
    E a reserva continua "SOLICITADA"

  Cenário: Rejeição libera o horário do recurso restrito
    Dado uma reserva de "ana" no "Laboratório de redes" das 10:00 às 11:00
    Quando "bruno" rejeita a reserva com o motivo "laboratório sem técnico"
    Então a reserva fica "REJEITADA"
    E "ana" consegue reservar o "Laboratório de redes" das 10:00 às 11:00

  # Cenário 3 (RN-04, RF-08) e a tabela de fronteiras do mesmo documento: manutenção das 10:00 às 12:00,
  # intervalo semiaberto [início, fim) (D1, ADR-0002).
  Esquema do Cenário: Fronteiras da manutenção das 10:00 às 12:00
    Dado uma manutenção na "Sala C237" das 10:00 às 12:00
    Quando "ana" tenta reservar a "Sala C237" das <inicio> às <fim>
    Então a resposta é <status>
    E o sistema tem <reservas> reservas gravadas

    Exemplos:
      | inicio   | fim      | status | reservas | observação                          |
      | 09:00    | 10:00    | 201    | 1        | termina exatamente no início        |
      | 12:00    | 13:00    | 201    | 1        | começa exatamente no fim            |
      | 09:00    | 10:00:01 | 409    | 0        | cruza 1 segundo no início           |
      | 11:59:59 | 13:00    | 409    | 0        | cruza 1 segundo no fim              |
      | 10:30    | 11:30    | 409    | 0        | dentro da manutenção                |

  Cenário: Recusa por manutenção não é conflito de horário
    Dado uma manutenção na "Sala C237" das 10:00 às 12:00
    Quando "ana" tenta reservar a "Sala C237" das 10:30 às 11:30
    Então a resposta é 409
    E o tipo do problema é "urn:recursos:problema:recurso-em-manutencao"
    E nenhuma tentativa recusada foi gravada

  # Cenário 4 (RN-05): transições de estado
  Cenário: Retirada e devolução levam a reserva até CONCLUIDA
    Dado uma reserva de "ana" na "Sala C237" das 10:00 às 11:00
    E a reserva está "APROVADA"
    Quando "bruno" registra a retirada
    Então a reserva fica "EM_USO"
    E depois da devolução a reserva fica "CONCLUIDA"

  Cenário: Transição inválida é recusada e não muda o estado
    Dado uma reserva de "ana" na "Sala C237" das 10:00 às 11:00
    E a reserva está "APROVADA"
    Quando "bruno" tenta registrar a devolução sem a retirada
    Então a resposta é 409
    E o tipo do problema é "urn:recursos:problema:transicao-invalida"
    E a reserva continua "APROVADA"
    E o histórico da reserva tem 1 registros, o último feito por "ana"

  Cenário: Não comparecimento libera o horário
    Dado uma reserva de "ana" na "Sala C237" das 10:00 às 11:00
    Quando "bruno" registra o não comparecimento
    Então a reserva fica "NAO_COMPARECEU"
    E "ana" consegue reservar a "Sala C237" das 10:00 às 11:00
