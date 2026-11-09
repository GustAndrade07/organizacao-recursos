-- V1: núcleo da reserva (ADR-0002, ADR-0006, ADR-0007).
-- Regras de esquema: esta migração NÃO se edita depois de mergeada na main; erro vira a próxima versão.
--
-- Decisões que moldam este arquivo (seção 9 do plano de execução):
--   D1  intervalo semiaberto [início, fim): todo período é tstzrange com limite inferior fechado e superior aberto.
--   D2  a tentativa recusada por sobreposição tem tabela própria (tentativa_recusada) e é gravada em transação própria.
--   D5  só SOLICITADA, APROVADA, EM_USO e CONCLUIDA ocupam o horário; CANCELADA, REJEITADA e NAO_COMPARECEU liberam.
--   D6  o professor é um recurso (tipo PROFESSOR) e há UMA constraint de exclusão; o estado da reserva é copiado para
--       reserva_recurso por chave estrangeira composta (ON UPDATE CASCADE), porque o predicado de uma constraint só
--       enxerga colunas da própria linha.
--   D7  trigger de trava por recurso (advisory lock de transação) para o perdedor da corrida receber sempre 23P01.
-- Não há DEFAULT now(): a hora vem do Clock da aplicação (nada de relógio oculto no banco).

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE usuario (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome       text NOT NULL,
    login      text NOT NULL UNIQUE,
    senha_hash text NOT NULL,
    perfil     text NOT NULL,
    CONSTRAINT usuario_perfil_valido CHECK (perfil IN ('SOLICITANTE', 'RESPONSAVEL', 'ADMINISTRADOR'))
);

CREATE TABLE recurso (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo       text NOT NULL,
    nome       text NOT NULL,
    capacidade integer,
    restrito   boolean NOT NULL DEFAULT false,
    CONSTRAINT recurso_tipo_valido CHECK (tipo IN ('SALA', 'MATERIAL', 'PROFESSOR')),
    CONSTRAINT recurso_capacidade_positiva CHECK (capacidade IS NULL OR capacidade > 0)
);

CREATE TABLE manutencao (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recurso_id bigint NOT NULL REFERENCES recurso (id),
    periodo    tstzrange NOT NULL,
    CONSTRAINT manutencao_periodo_semiaberto CHECK (
        NOT isempty(periodo) AND NOT lower_inf(periodo) AND NOT upper_inf(periodo)
        AND lower_inc(periodo) AND NOT upper_inc(periodo))
);
CREATE INDEX manutencao_recurso_periodo_gist ON manutencao USING gist (recurso_id, periodo);

CREATE TABLE reserva (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    solicitante_id bigint NOT NULL REFERENCES usuario (id),
    estado         text NOT NULL,
    criada_em      timestamptz NOT NULL,
    CONSTRAINT reserva_estado_valido CHECK (estado IN
        ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA', 'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU')),
    -- alvo da chave estrangeira composta de reserva_recurso (copia do estado, ver D6)
    CONSTRAINT reserva_id_estado_unico UNIQUE (id, estado)
);
CREATE INDEX reserva_solicitante_idx ON reserva (solicitante_id);

CREATE TABLE reserva_recurso (
    reserva_id bigint NOT NULL,
    estado     text NOT NULL,
    recurso_id bigint NOT NULL REFERENCES recurso (id),
    periodo    tstzrange NOT NULL,
    PRIMARY KEY (reserva_id, recurso_id),
    -- o estado desta linha é sempre o estado da reserva: mudar reserva.estado atualiza as linhas em cascata
    CONSTRAINT reserva_recurso_estado_da_reserva FOREIGN KEY (reserva_id, estado)
        REFERENCES reserva (id, estado) ON UPDATE CASCADE,
    CONSTRAINT reserva_recurso_periodo_semiaberto CHECK (
        NOT isempty(periodo) AND NOT lower_inf(periodo) AND NOT upper_inf(periodo)
        AND lower_inc(periodo) AND NOT upper_inc(periodo)),
    -- A proteção contra reserva dupla vive aqui (RN-02, RN-03, RF-05, RF-06): vale para sala, material e professor,
    -- e só para os estados que ocupam o horário (D5).
    CONSTRAINT reserva_recurso_sem_sobreposicao EXCLUDE USING gist (recurso_id WITH =, periodo WITH &&)
        WHERE (estado IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA'))
);

CREATE TABLE tentativa_recusada (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recurso_id     bigint NOT NULL REFERENCES recurso (id),
    solicitante_id bigint NOT NULL REFERENCES usuario (id),
    periodo_pedido tstzrange NOT NULL,
    motivo         text NOT NULL,
    registrada_em  timestamptz NOT NULL,
    CONSTRAINT tentativa_motivo_valido CHECK (motivo IN ('SOBREPOSICAO')),
    CONSTRAINT tentativa_periodo_semiaberto CHECK (
        NOT isempty(periodo_pedido) AND NOT lower_inf(periodo_pedido) AND NOT upper_inf(periodo_pedido)
        AND lower_inc(periodo_pedido) AND NOT upper_inc(periodo_pedido))
);
CREATE INDEX tentativa_recusada_recurso_data_idx ON tentativa_recusada (recurso_id, registrada_em);
CREATE INDEX tentativa_recusada_solicitante_idx ON tentativa_recusada (solicitante_id);

CREATE TABLE auditoria_reserva (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_id      bigint NOT NULL REFERENCES reserva (id),
    usuario_id      bigint NOT NULL REFERENCES usuario (id),
    instante        timestamptz NOT NULL,
    estado_anterior text,
    estado_novo     text NOT NULL,
    motivo          text,
    CONSTRAINT auditoria_estado_anterior_valido CHECK (estado_anterior IS NULL OR estado_anterior IN
        ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA', 'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU')),
    CONSTRAINT auditoria_estado_novo_valido CHECK (estado_novo IN
        ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA', 'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU'))
);
CREATE INDEX auditoria_reserva_reserva_instante_idx ON auditoria_reserva (reserva_id, instante);
CREATE INDEX auditoria_reserva_usuario_idx ON auditoria_reserva (usuario_id);

-- D7: trava por recurso. Sem ela, pedidos simultâneos para o mesmo intervalo podiam terminar em deadlock (SQLState
-- 40P01) em vez de violação de exclusão (23P01): medido em 06/11/2026, 9 de 40 rodadas de 20 pedidos (PostgreSQL 16.15
-- em Docker, uma máquina só; não generalizar). O trigger põe os escritores do mesmo recurso em fila (lock de
-- transação, liberado no commit ou rollback); quem chega depois espera e então recebe 23P01, que o serviço traduz em
-- 409. A garantia continua sendo a constraint acima; o trigger só ordena a fila.
-- Quem grava uma reserva com vários recursos deve inserir as linhas em ordem crescente de recurso_id, para duas
-- reservas não se travarem em ordem oposta.
CREATE FUNCTION reserva_recurso_trava_recurso() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtextextended('reserva_recurso:' || NEW.recurso_id, 0));
    RETURN NEW;
END;
$$;
CREATE TRIGGER reserva_recurso_trava_recurso BEFORE INSERT OR UPDATE ON reserva_recurso
    FOR EACH ROW EXECUTE FUNCTION reserva_recurso_trava_recurso();
