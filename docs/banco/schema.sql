-- Esquema consolidado do banco (documentação). NÃO é migração: o Flyway só roda src/main/resources/db/migration.
-- Gerado em 07/11/2026 a partir da migração V1__nucleo_da_reserva.sql, num PostgreSQL 16.15 descartável:
--   1. subir um contêiner postgres:16 só para isso (Docker, banco vazio chamado recursos, apagado depois);
--   2. aplicar a V1 com psql -f V1__nucleo_da_reserva.sql;
--   3. rodar pg_dump --schema-only --no-owner --no-privileges nesse banco.
-- Só estrutura: não há dados, usuários, papéis nem credenciais neste arquivo.
-- (as linhas \restrict e \unrestrict do pg_dump, com token aleatório, foram removidas para o arquivo ser estável)
-- Ao criar a V2, gere de novo. Não edite à mão: a fonte da verdade é a pasta db/migration.
-- Fora da V1 (ficam para a V2): tabela movimentacao_material (RF-09) e colunas de competência e localização de recurso.
--
-- PostgreSQL database dump
--


-- Dumped from database version 16.15 (Debian 16.15-1.pgdg13+2)
-- Dumped by pg_dump version 16.15 (Debian 16.15-1.pgdg13+2)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: btree_gist; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;


--
-- Name: EXTENSION btree_gist; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION btree_gist IS 'support for indexing common datatypes in GiST';


--
-- Name: reserva_recurso_trava_recurso(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.reserva_recurso_trava_recurso() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtextextended('reserva_recurso:' || NEW.recurso_id, 0));
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: auditoria_reserva; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auditoria_reserva (
    id bigint NOT NULL,
    reserva_id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    instante timestamp with time zone NOT NULL,
    estado_anterior text,
    estado_novo text NOT NULL,
    motivo text,
    CONSTRAINT auditoria_estado_anterior_valido CHECK (((estado_anterior IS NULL) OR (estado_anterior = ANY (ARRAY['SOLICITADA'::text, 'APROVADA'::text, 'EM_USO'::text, 'CONCLUIDA'::text, 'REJEITADA'::text, 'CANCELADA'::text, 'NAO_COMPARECEU'::text])))),
    CONSTRAINT auditoria_estado_novo_valido CHECK ((estado_novo = ANY (ARRAY['SOLICITADA'::text, 'APROVADA'::text, 'EM_USO'::text, 'CONCLUIDA'::text, 'REJEITADA'::text, 'CANCELADA'::text, 'NAO_COMPARECEU'::text])))
);


--
-- Name: auditoria_reserva_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.auditoria_reserva ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.auditoria_reserva_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: manutencao; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.manutencao (
    id bigint NOT NULL,
    recurso_id bigint NOT NULL,
    periodo tstzrange NOT NULL,
    CONSTRAINT manutencao_periodo_semiaberto CHECK (((NOT isempty(periodo)) AND (NOT lower_inf(periodo)) AND (NOT upper_inf(periodo)) AND lower_inc(periodo) AND (NOT upper_inc(periodo))))
);


--
-- Name: manutencao_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.manutencao ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.manutencao_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: recurso; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recurso (
    id bigint NOT NULL,
    tipo text NOT NULL,
    nome text NOT NULL,
    capacidade integer,
    restrito boolean DEFAULT false NOT NULL,
    CONSTRAINT recurso_capacidade_positiva CHECK (((capacidade IS NULL) OR (capacidade > 0))),
    CONSTRAINT recurso_tipo_valido CHECK ((tipo = ANY (ARRAY['SALA'::text, 'MATERIAL'::text, 'PROFESSOR'::text])))
);


--
-- Name: recurso_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.recurso ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.recurso_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: reserva; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reserva (
    id bigint NOT NULL,
    solicitante_id bigint NOT NULL,
    estado text NOT NULL,
    criada_em timestamp with time zone NOT NULL,
    CONSTRAINT reserva_estado_valido CHECK ((estado = ANY (ARRAY['SOLICITADA'::text, 'APROVADA'::text, 'EM_USO'::text, 'CONCLUIDA'::text, 'REJEITADA'::text, 'CANCELADA'::text, 'NAO_COMPARECEU'::text])))
);


--
-- Name: reserva_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.reserva ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.reserva_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: reserva_recurso; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reserva_recurso (
    reserva_id bigint NOT NULL,
    estado text NOT NULL,
    recurso_id bigint NOT NULL,
    periodo tstzrange NOT NULL,
    CONSTRAINT reserva_recurso_periodo_semiaberto CHECK (((NOT isempty(periodo)) AND (NOT lower_inf(periodo)) AND (NOT upper_inf(periodo)) AND lower_inc(periodo) AND (NOT upper_inc(periodo))))
);


--
-- Name: tentativa_recusada; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tentativa_recusada (
    id bigint NOT NULL,
    recurso_id bigint NOT NULL,
    solicitante_id bigint NOT NULL,
    periodo_pedido tstzrange NOT NULL,
    motivo text NOT NULL,
    registrada_em timestamp with time zone NOT NULL,
    CONSTRAINT tentativa_motivo_valido CHECK ((motivo = 'SOBREPOSICAO'::text)),
    CONSTRAINT tentativa_periodo_semiaberto CHECK (((NOT isempty(periodo_pedido)) AND (NOT lower_inf(periodo_pedido)) AND (NOT upper_inf(periodo_pedido)) AND lower_inc(periodo_pedido) AND (NOT upper_inc(periodo_pedido))))
);


--
-- Name: tentativa_recusada_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tentativa_recusada ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tentativa_recusada_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: usuario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuario (
    id bigint NOT NULL,
    nome text NOT NULL,
    login text NOT NULL,
    senha_hash text NOT NULL,
    perfil text NOT NULL,
    CONSTRAINT usuario_perfil_valido CHECK ((perfil = ANY (ARRAY['SOLICITANTE'::text, 'RESPONSAVEL'::text, 'ADMINISTRADOR'::text])))
);


--
-- Name: usuario_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.usuario ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.usuario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: auditoria_reserva auditoria_reserva_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_reserva
    ADD CONSTRAINT auditoria_reserva_pkey PRIMARY KEY (id);


--
-- Name: manutencao manutencao_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manutencao
    ADD CONSTRAINT manutencao_pkey PRIMARY KEY (id);


--
-- Name: recurso recurso_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recurso
    ADD CONSTRAINT recurso_pkey PRIMARY KEY (id);


--
-- Name: reserva reserva_id_estado_unico; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reserva
    ADD CONSTRAINT reserva_id_estado_unico UNIQUE (id, estado);


--
-- Name: reserva reserva_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reserva
    ADD CONSTRAINT reserva_pkey PRIMARY KEY (id);


--
-- Name: reserva_recurso reserva_recurso_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reserva_recurso
    ADD CONSTRAINT reserva_recurso_pkey PRIMARY KEY (reserva_id, recurso_id);


--
-- Name: reserva_recurso reserva_recurso_sem_sobreposicao; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reserva_recurso
    ADD CONSTRAINT reserva_recurso_sem_sobreposicao EXCLUDE USING gist (recurso_id WITH =, periodo WITH &&) WHERE ((estado = ANY (ARRAY['SOLICITADA'::text, 'APROVADA'::text, 'EM_USO'::text, 'CONCLUIDA'::text])));


--
-- Name: tentativa_recusada tentativa_recusada_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tentativa_recusada
    ADD CONSTRAINT tentativa_recusada_pkey PRIMARY KEY (id);


--
-- Name: usuario usuario_login_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_login_key UNIQUE (login);


--
-- Name: usuario usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (id);


--
-- Name: auditoria_reserva_reserva_instante_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auditoria_reserva_reserva_instante_idx ON public.auditoria_reserva USING btree (reserva_id, instante);


--
-- Name: auditoria_reserva_usuario_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auditoria_reserva_usuario_idx ON public.auditoria_reserva USING btree (usuario_id);


--
-- Name: manutencao_recurso_periodo_gist; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX manutencao_recurso_periodo_gist ON public.manutencao USING gist (recurso_id, periodo);


--
-- Name: reserva_solicitante_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX reserva_solicitante_idx ON public.reserva USING btree (solicitante_id);


--
-- Name: tentativa_recusada_recurso_data_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX tentativa_recusada_recurso_data_idx ON public.tentativa_recusada USING btree (recurso_id, registrada_em);


--
-- Name: tentativa_recusada_solicitante_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX tentativa_recusada_solicitante_idx ON public.tentativa_recusada USING btree (solicitante_id);


--
-- Name: reserva_recurso reserva_recurso_trava_recurso; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER reserva_recurso_trava_recurso BEFORE INSERT OR UPDATE ON public.reserva_recurso FOR EACH ROW EXECUTE FUNCTION public.reserva_recurso_trava_recurso();


--
-- Name: auditoria_reserva auditoria_reserva_reserva_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_reserva
    ADD CONSTRAINT auditoria_reserva_reserva_id_fkey FOREIGN KEY (reserva_id) REFERENCES public.reserva(id);


--
-- Name: auditoria_reserva auditoria_reserva_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_reserva
    ADD CONSTRAINT auditoria_reserva_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuario(id);


--
-- Name: manutencao manutencao_recurso_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manutencao
    ADD CONSTRAINT manutencao_recurso_id_fkey FOREIGN KEY (recurso_id) REFERENCES public.recurso(id);


--
-- Name: reserva_recurso reserva_recurso_estado_da_reserva; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reserva_recurso
    ADD CONSTRAINT reserva_recurso_estado_da_reserva FOREIGN KEY (reserva_id, estado) REFERENCES public.reserva(id, estado) ON UPDATE CASCADE;


--
-- Name: reserva_recurso reserva_recurso_recurso_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reserva_recurso
    ADD CONSTRAINT reserva_recurso_recurso_id_fkey FOREIGN KEY (recurso_id) REFERENCES public.recurso(id);


--
-- Name: reserva reserva_solicitante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reserva
    ADD CONSTRAINT reserva_solicitante_id_fkey FOREIGN KEY (solicitante_id) REFERENCES public.usuario(id);


--
-- Name: tentativa_recusada tentativa_recusada_recurso_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tentativa_recusada
    ADD CONSTRAINT tentativa_recusada_recurso_id_fkey FOREIGN KEY (recurso_id) REFERENCES public.recurso(id);


--
-- Name: tentativa_recusada tentativa_recusada_solicitante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tentativa_recusada
    ADD CONSTRAINT tentativa_recusada_solicitante_id_fkey FOREIGN KEY (solicitante_id) REFERENCES public.usuario(id);


--
-- PostgreSQL database dump complete
--


