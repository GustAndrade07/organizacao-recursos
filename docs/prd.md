# PRD — Organização de Recursos

## Personas (rascunho para validar com o grupo)

| | Solicitante | Responsável | Administrador |
|---|---|---|---|
| Quem é | Professora que reserva sala e equipamento para as próprias aulas | Coordenador de laboratório que libera recursos restritos e controla empréstimos | Funcionário da secretaria que mantém o cadastro de salas, materiais e usuários |
| Objetivo | Garantir sala e projetor no horário da aula, sem surpresa no dia | Aprovar só quem pode usar o laboratório e saber onde está cada equipamento | Manter o cadastro certo e bloquear o que está em manutenção |
| Contexto | Reserva pelo celular entre uma aula e outra | Recebe vários pedidos por dia, muitos em cima da hora | Trabalha no computador, em períodos de reforma e troca de semestre |
| Frustração | Chegar e encontrar a sala ocupada por outra turma | Equipamento que sai e não volta, sem registro de quem levou | Sala em reforma continuar aparecendo como disponível |
| Cenário de qualidade (rascunho) | Com 50 usuários simultâneos, a busca de disponibilidade responde em até 300 ms no p95 | Dois pedidos simultâneos para o mesmo recurso geram exatamente 1 reserva aceita | Recurso em manutenção é recusado em 100% das tentativas de reserva no período |
| Requisitos | RF-03, RF-04, RF-05, RF-13 | RF-06, RF-07, RF-09, RF-10 | RF-02, RF-08, RF-12 |

## Requisitos funcionais
- RF-01 Login e permissão por perfil
- RF-02 Cadastro e consulta de salas, professores e materiais
- RF-03 Pesquisa por tipo, capacidade, localização, competência e disponibilidade
- RF-04 Criar, alterar e cancelar reservas
- RF-05 Detectar horário sobreposto, inclusive na agenda do professor
- RF-06 Impedir reserva dupla com pedidos simultâneos
- RF-07 Aprovação obrigatória para recurso restrito
- RF-08 Bloquear recurso em manutenção
- RF-09 Registrar retirada e devolução de material
- RF-10 Histórico auditável de mudanças
- RF-11 Notificação simulada ou integração com API externa
- RF-12 Relatório de utilização (horas por recurso, carga por professor, conflitos evitados)
- RF-13 Telas responsivas e mensagens de erro claras
- RF-14 Documentação da API (OpenAPI)

## Regras de negócio (RN)
- RN-01 Término depois do início
- RN-02 Reservas do mesmo recurso (sala, material ou professor) não se sobrepõem
- RN-03 Pedidos simultâneos geram uma única reserva aceita
- RN-04 Recurso em manutenção não pode ser reservado
- RN-05 Estados: SOLICITADA → APROVADA → EM_USO → CONCLUÍDA, mais REJEITADA, CANCELADA e NAO_COMPARECEU
- RN-06 Só o Responsável aprova recurso restrito; ninguém aprova a própria reserva
- RN-07 Reserva já iniciada não pode ser apagada
- RN-08 Toda mudança de estado gera registro de auditoria
- RN-09 O registro de auditoria guarda: reserva, usuário, instante (obtido via `Clock`), estado anterior, estado novo e motivo
- RN-10 O registro de auditoria não pode ser alterado nem apagado pela API
- RN-11 A consulta do histórico de uma reserva é permitida só a Responsável e Administrador; o Solicitante recebe 403
- RN-12 A auditoria é gravada na mesma transação da mudança de estado; um rollback não deixa auditoria órfã
- RN-13 O relatório de utilização é acessível só a Responsável e Administrador (sem token: 401; Solicitante: 403)

## Relatório de utilização (RF-12)
- Rota: `GET /relatorios/utilizacao?inicio=&fim=`
- Apresenta: horas reservadas por recurso; carga horária alocada por professor; quantidade de conflitos evitados (tentativas recusadas por sobreposição no período)
- Casos obrigatórios: período vazio; reserva que cruza a borda do período; reservas canceladas (não contam como uso)
- Os testes semeiam massa pequena e calculam os valores esperados **à mão**, com a conta em comentário no teste (nunca copiados da saída da aplicação)

## Requisitos não funcionais (RNF — todos com número)
- RNF-01 Busca de disponibilidade: p95 ≤ 300 ms com 50 usuários (a validar)
- RNF-02 Cobertura ≥ 80% linhas / 70% branches
- RNF-03 Criação de reserva sob carga responde só 201 ou 409, nunca 500
