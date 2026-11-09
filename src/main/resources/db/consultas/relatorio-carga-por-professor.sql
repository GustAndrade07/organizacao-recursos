-- RF-12: carga horária alocada por professor dentro da janela do relatório.
-- Não é migração (ver relatorio-horas-por-recurso.sql); mesmas regras e mesmo parâmetro único (?), a janela como
-- tstzrange semiaberto [início, fim). O professor é um recurso do tipo PROFESSOR (D6), então a carga é o cálculo de
-- horas por recurso restrito a esse tipo. Professor sem carga na janela não gera linha.
-- Colunas: recurso_id, nome, horas.
WITH janela AS (SELECT ?::tstzrange AS periodo)
SELECT r.id AS recurso_id,
       r.nome,
       SUM(EXTRACT(EPOCH FROM upper(rr.periodo * j.periodo) - lower(rr.periodo * j.periodo))) / 3600.0 AS horas
FROM reserva_recurso rr
JOIN recurso r ON r.id = rr.recurso_id
CROSS JOIN janela j
WHERE r.tipo = 'PROFESSOR'
  AND rr.estado IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA')
  AND rr.periodo && j.periodo
GROUP BY r.id, r.nome
ORDER BY r.id
