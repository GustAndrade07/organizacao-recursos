-- RF-12: conflitos evitados = tentativas recusadas por sobreposição registradas dentro da janela do relatório.
-- Não é migração (ver relatorio-horas-por-recurso.sql). Parâmetro único (?): a janela como tstzrange semiaberto
-- [início, fim) (D1), então uma tentativa registrada exatamente no fim da janela não conta.
-- Pendente de confirmação do grupo (o PRD não decide): o "período" é o da hora em que a tentativa foi registrada
-- (registrada_em), não o do intervalo pedido (periodo_pedido).
-- Coluna única: conflitos_evitados (bigint; 0 quando não há tentativa na janela).
SELECT COUNT(*) AS conflitos_evitados
FROM tentativa_recusada t
WHERE t.motivo = 'SOBREPOSICAO'
  AND t.registrada_em <@ ?::tstzrange
