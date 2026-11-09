-- RF-12: horas reservadas por recurso dentro da janela do relatório.
-- NÃO é migração (fica fora de db/migration, o Flyway não a executa): é consulta de leitura, testada por
-- RelatorioUtilizacaoBancoTest e carregada pelo repositório do back-end.
--
-- Parâmetro único (?): a janela como tstzrange semiaberto [início, fim) (D1), por exemplo
--   '[2026-11-10T00:00:00Z,2026-11-11T00:00:00Z)'.
-- A conversão das datas do endpoint em janela (fuso, fim inclusivo) é do serviço, não desta consulta.
--
-- Regras (pendentes de confirmação do grupo; o PRD não decide):
--   * contam os estados que ocupam horário na D5: SOLICITADA, APROVADA, EM_USO e CONCLUIDA;
--     CANCELADA, REJEITADA e NAO_COMPARECEU não contam;
--   * reserva que cruza a borda conta só a parte dentro da janela (interseção dos intervalos);
--   * recurso sem uso na janela não gera linha (mostrar zero é do serviço).
-- Colunas: recurso_id, nome, tipo, horas (numeric, horas inteiras e frações).
WITH janela AS (SELECT ?::tstzrange AS periodo)
SELECT r.id AS recurso_id,
       r.nome,
       r.tipo,
       SUM(EXTRACT(EPOCH FROM upper(rr.periodo * j.periodo) - lower(rr.periodo * j.periodo))) / 3600.0 AS horas
FROM reserva_recurso rr
JOIN recurso r ON r.id = rr.recurso_id
CROSS JOIN janela j
WHERE rr.estado IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA')
  AND rr.periodo && j.periodo
GROUP BY r.id, r.nome, r.tipo
ORDER BY r.id
