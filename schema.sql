--
-- PostgreSQL database dump
--

\restrict jYhLhydJX4ZZaJWW2eercnvvvPHcb1JNx2TePW3lWxwiGlm7UpNC65tsT4uxYGm

-- Dumped from database version 17.4
-- Dumped by pg_dump version 17.7 (Ubuntu 17.7-3.pgdg24.04+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

-- *not* creating schema, since initdb creates it


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS '';


--
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry and geography spatial types and functions';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: coordinators; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.coordinators (
    uuid uuid NOT NULL,
    cognito_sub character varying,
    first_name character varying,
    last_name character varying,
    email character varying,
    registration_date timestamp without time zone
);


--
-- Name: detections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.detections (
    uuid uuid NOT NULL,
    mission_uuid uuid,
    detected_by_drone_uuid uuid,
    detected_at timestamp without time zone,
    location public.geometry(Point,4326),
    image_key character varying
);


--
-- Name: drone_maintenance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.drone_maintenance (
    uuid uuid NOT NULL,
    drone_uuid uuid,
    performed_by uuid,
    maintenance_type character varying,
    description text,
    created_at timestamp without time zone,
    performed_at timestamp without time zone
);


--
-- Name: drones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.drones (
    uuid uuid NOT NULL,
    name character varying,
    group_uuid uuid,
    address character varying,
    manager_version character varying,
    first_discovered timestamp without time zone,
    home_position public.geometry(PointZ,4326),
    model character varying,
    capabilities character varying[]
);


--
-- Name: groups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.groups (
    uuid uuid NOT NULL,
    outpost_uuid uuid,
    name character varying,
    created_at timestamp without time zone
);


--
-- Name: log_files; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_files (
    uuid uuid NOT NULL,
    drone_uuid uuid,
    created_at timestamp without time zone,
    archived boolean DEFAULT false,
    archived_date timestamp without time zone
);


--
-- Name: missions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.missions (
    uuid uuid NOT NULL,
    group_uuid uuid,
    name character varying,
    bundle_url character varying,
    start_time timestamp without time zone,
    created_by uuid
);


--
-- Name: outposts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.outposts (
    uuid uuid NOT NULL,
    name character varying,
    latitude numeric(9,6),
    longitude numeric(9,6),
    area public.geometry(Polygon,4326),
    created_by uuid,
    created_at timestamp without time zone
);


--
-- Name: seat_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.seat_tokens (
    uuid uuid NOT NULL,
    created_by uuid,
    "group" uuid,
    created_at timestamp without time zone
);


--
-- Name: coordinators coordinators_cognito_sub_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coordinators
    ADD CONSTRAINT coordinators_cognito_sub_key UNIQUE (cognito_sub);


--
-- Name: coordinators coordinators_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coordinators
    ADD CONSTRAINT coordinators_email_key UNIQUE (email);


--
-- Name: coordinators coordinators_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coordinators
    ADD CONSTRAINT coordinators_pkey PRIMARY KEY (uuid);


--
-- Name: detections detections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detections
    ADD CONSTRAINT detections_pkey PRIMARY KEY (uuid);


--
-- Name: drone_maintenance drone_maintenance_pkey1; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.drone_maintenance
    ADD CONSTRAINT drone_maintenance_pkey1 PRIMARY KEY (uuid);


--
-- Name: drones drones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.drones
    ADD CONSTRAINT drones_pkey PRIMARY KEY (uuid);


--
-- Name: groups groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT groups_pkey PRIMARY KEY (uuid);


--
-- Name: log_files log_files_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.log_files
    ADD CONSTRAINT log_files_pkey PRIMARY KEY (uuid);


--
-- Name: missions missions_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.missions
    ADD CONSTRAINT missions_name_key UNIQUE (name);


--
-- Name: missions missions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.missions
    ADD CONSTRAINT missions_pkey PRIMARY KEY (uuid);


--
-- Name: outposts outposts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outposts
    ADD CONSTRAINT outposts_pkey PRIMARY KEY (uuid);


--
-- Name: seat_tokens seat_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seat_tokens
    ADD CONSTRAINT seat_tokens_pkey PRIMARY KEY (uuid);


--
-- Name: idx_detections_location; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_detections_location ON public.detections USING gist (location);


--
-- Name: detections fk_detections_drone; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detections
    ADD CONSTRAINT fk_detections_drone FOREIGN KEY (detected_by_drone_uuid) REFERENCES public.drones(uuid) ON DELETE SET NULL;


--
-- Name: detections fk_detections_mission; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detections
    ADD CONSTRAINT fk_detections_mission FOREIGN KEY (mission_uuid) REFERENCES public.missions(uuid) ON DELETE CASCADE;


--
-- Name: drones fk_drones_group; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.drones
    ADD CONSTRAINT fk_drones_group FOREIGN KEY (group_uuid) REFERENCES public.groups(uuid) ON DELETE CASCADE;


--
-- Name: groups fk_groups_outpost; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT fk_groups_outpost FOREIGN KEY (outpost_uuid) REFERENCES public.outposts(uuid) ON DELETE CASCADE;


--
-- Name: log_files fk_log_files_drone; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.log_files
    ADD CONSTRAINT fk_log_files_drone FOREIGN KEY (drone_uuid) REFERENCES public.drones(uuid) ON DELETE SET NULL;


--
-- Name: drone_maintenance fk_maintenance_drone; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.drone_maintenance
    ADD CONSTRAINT fk_maintenance_drone FOREIGN KEY (drone_uuid) REFERENCES public.drones(uuid) ON DELETE SET NULL;


--
-- Name: drone_maintenance fk_maintenance_performed_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.drone_maintenance
    ADD CONSTRAINT fk_maintenance_performed_by FOREIGN KEY (performed_by) REFERENCES public.coordinators(uuid) ON DELETE SET NULL;


--
-- Name: missions fk_missions_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.missions
    ADD CONSTRAINT fk_missions_created_by FOREIGN KEY (created_by) REFERENCES public.coordinators(uuid) ON DELETE SET NULL;


--
-- Name: missions fk_missions_group; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.missions
    ADD CONSTRAINT fk_missions_group FOREIGN KEY (group_uuid) REFERENCES public.groups(uuid) ON DELETE SET NULL;


--
-- Name: outposts fk_outposts_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outposts
    ADD CONSTRAINT fk_outposts_created_by FOREIGN KEY (created_by) REFERENCES public.coordinators(uuid) ON DELETE SET NULL;


--
-- Name: seat_tokens fk_seat_tokens_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seat_tokens
    ADD CONSTRAINT fk_seat_tokens_created_by FOREIGN KEY (created_by) REFERENCES public.coordinators(uuid) ON DELETE SET NULL;


--
-- Name: seat_tokens fk_seat_tokens_group; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.seat_tokens
    ADD CONSTRAINT fk_seat_tokens_group FOREIGN KEY ("group") REFERENCES public.groups(uuid) ON DELETE SET NULL;


--
-- PostgreSQL database dump complete
--

\unrestrict jYhLhydJX4ZZaJWW2eercnvvvPHcb1JNx2TePW3lWxwiGlm7UpNC65tsT4uxYGm

