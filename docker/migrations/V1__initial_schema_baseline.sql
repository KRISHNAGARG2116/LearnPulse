--
-- PostgreSQL database dump
--

\restrict PUQnes8kKQuDGEaMgw3WGd0cp37s7CgNTX0rmJmp9odCfAuxh98KvPogfZMVuaz

-- Dumped from database version 16.14 (Debian 16.14-1.pgdg12+1)
-- Dumped by pg_dump version 16.14 (Debian 16.14-1.pgdg12+1)

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
-- Name: hstore; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS hstore WITH SCHEMA public;


--
-- Name: EXTENSION hstore; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION hstore IS 'data type for storing sets of (key, value) pairs';


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: vector; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;


--
-- Name: EXTENSION vector; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION vector IS 'vector data type and ivfflat and hnsw access methods';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ai_chat_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ai_chat_history (
    id uuid NOT NULL,
    ai_response text NOT NULL,
    conversation_id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    user_message text NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.ai_chat_history OWNER TO postgres;

--
-- Name: ai_telemetry_records; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ai_telemetry_records (
    id uuid NOT NULL,
    completion_time timestamp(6) with time zone NOT NULL,
    completion_tokens integer,
    created_at timestamp(6) with time zone NOT NULL,
    error_message character varying(500),
    estimated_cost_usd double precision,
    execution_latency_ms bigint NOT NULL,
    feature character varying(255) NOT NULL,
    model character varying(255) NOT NULL,
    operation_id character varying(255) NOT NULL,
    prompt_tokens integer,
    provider character varying(255) NOT NULL,
    start_time timestamp(6) with time zone NOT NULL,
    status character varying(255) NOT NULL,
    total_tokens integer,
    usage_provenance character varying(255) NOT NULL,
    user_email character varying(255),
    user_id uuid
);


ALTER TABLE public.ai_telemetry_records OWNER TO postgres;

--
-- Name: chapters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.chapters (
    id uuid NOT NULL,
    chapter_number integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    description character varying(1000),
    title character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    subject_id uuid NOT NULL
);


ALTER TABLE public.chapters OWNER TO postgres;

--
-- Name: infrastructure_checks; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.infrastructure_checks (
    id bigint NOT NULL,
    checked_at timestamp(6) with time zone NOT NULL,
    component_name character varying(255) NOT NULL,
    status character varying(255) NOT NULL
);


ALTER TABLE public.infrastructure_checks OWNER TO postgres;

--
-- Name: infrastructure_checks_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.infrastructure_checks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.infrastructure_checks_id_seq OWNER TO postgres;

--
-- Name: infrastructure_checks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.infrastructure_checks_id_seq OWNED BY public.infrastructure_checks.id;


--
-- Name: notes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notes (
    id uuid NOT NULL,
    content text,
    created_at timestamp(6) with time zone NOT NULL,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    chapter_id uuid,
    document_id uuid,
    subject_id uuid,
    teacher_id uuid NOT NULL
);


ALTER TABLE public.notes OWNER TO postgres;

--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.password_reset_tokens (
    id uuid NOT NULL,
    consumed boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by_admin_id uuid,
    expiry_date timestamp(6) with time zone NOT NULL,
    token_hash character varying(255) NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.password_reset_tokens OWNER TO postgres;

--
-- Name: questions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.questions (
    id uuid NOT NULL,
    correct_answer character varying(1) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    explanation text,
    marks integer NOT NULL,
    option_a character varying(255) NOT NULL,
    option_b character varying(255) NOT NULL,
    option_c character varying(255) NOT NULL,
    option_d character varying(255) NOT NULL,
    question_text text NOT NULL,
    source character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    quiz_id uuid NOT NULL,
    CONSTRAINT questions_source_check CHECK (((source)::text = ANY ((ARRAY['TEACHER_MANUAL'::character varying, 'TEACHER_UPLOADED'::character varying, 'AI_GENERATED'::character varying])::text[])))
);


ALTER TABLE public.questions OWNER TO postgres;

--
-- Name: quizzes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.quizzes (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    custom_instructions text,
    description text,
    is_published boolean NOT NULL,
    passing_score_percentage double precision NOT NULL,
    quiz_type character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    teacher_priorities text,
    title character varying(255) NOT NULL,
    total_marks integer NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    chapter_id uuid,
    created_by uuid NOT NULL,
    subject_id uuid,
    CONSTRAINT quizzes_quiz_type_check CHECK (((quiz_type)::text = ANY ((ARRAY['CHAPTER_QUIZ'::character varying, 'FINAL_COURSE_QUIZ'::character varying])::text[]))),
    CONSTRAINT quizzes_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying])::text[])))
);


ALTER TABLE public.quizzes OWNER TO postgres;

--
-- Name: student_enrollments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.student_enrollments (
    id uuid NOT NULL,
    enrolled_at timestamp(6) with time zone NOT NULL,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    student_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    CONSTRAINT student_enrollments_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


ALTER TABLE public.student_enrollments OWNER TO postgres;

--
-- Name: student_quiz_results; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.student_quiz_results (
    id uuid NOT NULL,
    attempted_at timestamp(6) with time zone NOT NULL,
    correct_answers integer NOT NULL,
    is_passed boolean NOT NULL,
    percentage double precision NOT NULL,
    score integer NOT NULL,
    total_marks integer NOT NULL,
    wrong_answers integer NOT NULL,
    quiz_id uuid NOT NULL,
    student_id uuid NOT NULL
);


ALTER TABLE public.student_quiz_results OWNER TO postgres;

--
-- Name: subjects; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.subjects (
    id uuid NOT NULL,
    code character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    description character varying(1000),
    name character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL
);


ALTER TABLE public.subjects OWNER TO postgres;

--
-- Name: teacher_subjects; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.teacher_subjects (
    id uuid NOT NULL,
    assigned_at timestamp(6) with time zone NOT NULL,
    subject_id uuid NOT NULL,
    teacher_id uuid NOT NULL
);


ALTER TABLE public.teacher_subjects OWNER TO postgres;

--
-- Name: uploaded_documents; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.uploaded_documents (
    id uuid NOT NULL,
    active boolean NOT NULL,
    content_type character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    extracted_text text,
    file_path character varying(255) NOT NULL,
    file_size bigint NOT NULL,
    original_file_name character varying(255) NOT NULL,
    processing_status character varying(255) NOT NULL,
    stored_file_name character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    chapter_id uuid,
    subject_id uuid,
    teacher_id uuid NOT NULL,
    CONSTRAINT uploaded_documents_processing_status_check CHECK (((processing_status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSED'::character varying, 'FAILED'::character varying])::text[])))
);


ALTER TABLE public.uploaded_documents OWNER TO postgres;

--
-- Name: user_profiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_profiles (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    department character varying(255),
    enrollment_number character varying(255),
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    rejection_reason character varying(1000),
    teacher_verification_status character varying(255),
    updated_at timestamp(6) with time zone NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT user_profiles_teacher_verification_status_check CHECK (((teacher_verification_status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


ALTER TABLE public.user_profiles OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    account_non_expired boolean NOT NULL,
    account_non_locked boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    credentials_non_expired boolean NOT NULL,
    email character varying(255) NOT NULL,
    enabled boolean NOT NULL,
    password character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'TEACHER'::character varying, 'STUDENT'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: vector_store; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.vector_store (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    content text,
    metadata json,
    embedding public.vector(768)
);


ALTER TABLE public.vector_store OWNER TO postgres;

--
-- Name: infrastructure_checks id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.infrastructure_checks ALTER COLUMN id SET DEFAULT nextval('public.infrastructure_checks_id_seq'::regclass);


--
-- Name: ai_chat_history ai_chat_history_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_chat_history
    ADD CONSTRAINT ai_chat_history_pkey PRIMARY KEY (id);


--
-- Name: ai_telemetry_records ai_telemetry_records_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_telemetry_records
    ADD CONSTRAINT ai_telemetry_records_pkey PRIMARY KEY (id);


--
-- Name: chapters chapters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chapters
    ADD CONSTRAINT chapters_pkey PRIMARY KEY (id);


--
-- Name: infrastructure_checks infrastructure_checks_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.infrastructure_checks
    ADD CONSTRAINT infrastructure_checks_pkey PRIMARY KEY (id);


--
-- Name: notes notes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notes
    ADD CONSTRAINT notes_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- Name: questions questions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.questions
    ADD CONSTRAINT questions_pkey PRIMARY KEY (id);


--
-- Name: quizzes quizzes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.quizzes
    ADD CONSTRAINT quizzes_pkey PRIMARY KEY (id);


--
-- Name: student_enrollments student_enrollments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_enrollments
    ADD CONSTRAINT student_enrollments_pkey PRIMARY KEY (id);


--
-- Name: student_quiz_results student_quiz_results_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_quiz_results
    ADD CONSTRAINT student_quiz_results_pkey PRIMARY KEY (id);


--
-- Name: subjects subjects_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.subjects
    ADD CONSTRAINT subjects_pkey PRIMARY KEY (id);


--
-- Name: teacher_subjects teacher_subjects_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teacher_subjects
    ADD CONSTRAINT teacher_subjects_pkey PRIMARY KEY (id);


--
-- Name: users uk_6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: uploaded_documents uk_8t002mtki5hn1rt5wfunv4bij; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.uploaded_documents
    ADD CONSTRAINT uk_8t002mtki5hn1rt5wfunv4bij UNIQUE (stored_file_name);


--
-- Name: password_reset_tokens uk_ajre85ybxavf1tt4omkrs5p6g; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT uk_ajre85ybxavf1tt4omkrs5p6g UNIQUE (token_hash);


--
-- Name: subjects uk_aodt3utnw0lsov4k9ta88dbpr; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.subjects
    ADD CONSTRAINT uk_aodt3utnw0lsov4k9ta88dbpr UNIQUE (name);


--
-- Name: user_profiles uk_e5h89rk3ijvdmaiig4srogdc6; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT uk_e5h89rk3ijvdmaiig4srogdc6 UNIQUE (user_id);


--
-- Name: subjects uk_rg7x1lyii7kdyycw98d45vep5; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.subjects
    ADD CONSTRAINT uk_rg7x1lyii7kdyycw98d45vep5 UNIQUE (code);


--
-- Name: student_enrollments uk_student_subject; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_enrollments
    ADD CONSTRAINT uk_student_subject UNIQUE (student_id, subject_id);


--
-- Name: chapters uk_subject_chapter_number; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chapters
    ADD CONSTRAINT uk_subject_chapter_number UNIQUE (subject_id, chapter_number);


--
-- Name: teacher_subjects uk_teacher_subject; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teacher_subjects
    ADD CONSTRAINT uk_teacher_subject UNIQUE (teacher_id, subject_id);


--
-- Name: uploaded_documents uploaded_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.uploaded_documents
    ADD CONSTRAINT uploaded_documents_pkey PRIMARY KEY (id);


--
-- Name: user_profiles user_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: vector_store vector_store_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vector_store
    ADD CONSTRAINT vector_store_pkey PRIMARY KEY (id);


--
-- Name: idx_ai_telemetry_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_telemetry_created_at ON public.ai_telemetry_records USING btree (created_at);


--
-- Name: idx_ai_telemetry_feature_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_telemetry_feature_status ON public.ai_telemetry_records USING btree (feature, status);


--
-- Name: idx_ai_telemetry_user_feature; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_telemetry_user_feature ON public.ai_telemetry_records USING btree (user_id, feature);


--
-- Name: idx_chat_history_created; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chat_history_created ON public.ai_chat_history USING btree (created_at);


--
-- Name: idx_chat_history_user_conv; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chat_history_user_conv ON public.ai_chat_history USING btree (user_id, conversation_id);


--
-- Name: idx_doc_subject_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doc_subject_active ON public.uploaded_documents USING btree (subject_id, active);


--
-- Name: idx_doc_teacher_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doc_teacher_active ON public.uploaded_documents USING btree (teacher_id, active);


--
-- Name: idx_quiz_result_quiz_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_quiz_result_quiz_id ON public.student_quiz_results USING btree (quiz_id);


--
-- Name: idx_quiz_result_student_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_quiz_result_student_time ON public.student_quiz_results USING btree (student_id, attempted_at);


--
-- Name: idx_user_profile_teacher_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_profile_teacher_status ON public.user_profiles USING btree (user_id, teacher_verification_status);


--
-- Name: vector_store_embedding_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX vector_store_embedding_idx ON public.vector_store USING hnsw (embedding public.vector_cosine_ops);


--
-- Name: quizzes fk1qg4klxor82vbamctkjechwe2; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.quizzes
    ADD CONSTRAINT fk1qg4klxor82vbamctkjechwe2 FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: uploaded_documents fk3lovj5wsckc5f5dkj1wafa52; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.uploaded_documents
    ADD CONSTRAINT fk3lovj5wsckc5f5dkj1wafa52 FOREIGN KEY (chapter_id) REFERENCES public.chapters(id);


--
-- Name: chapters fk3rm6snrkx0k8xyqn7017b0v41; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.chapters
    ADD CONSTRAINT fk3rm6snrkx0k8xyqn7017b0v41 FOREIGN KEY (subject_id) REFERENCES public.subjects(id);


--
-- Name: ai_chat_history fk417akj34rdt7i992m0379lhif; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_chat_history
    ADD CONSTRAINT fk417akj34rdt7i992m0379lhif FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: uploaded_documents fk6es735l1qf5kq7xxfjw9hbxxq; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.uploaded_documents
    ADD CONSTRAINT fk6es735l1qf5kq7xxfjw9hbxxq FOREIGN KEY (teacher_id) REFERENCES public.users(id);


--
-- Name: notes fk75iwfx76ppss2db35erg9tb94; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notes
    ADD CONSTRAINT fk75iwfx76ppss2db35erg9tb94 FOREIGN KEY (chapter_id) REFERENCES public.chapters(id);


--
-- Name: quizzes fkbfcxv33pl1gl32wie5nobns7r; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.quizzes
    ADD CONSTRAINT fkbfcxv33pl1gl32wie5nobns7r FOREIGN KEY (chapter_id) REFERENCES public.chapters(id);


--
-- Name: quizzes fkcblaekfu02utjnygfc3eqsgxy; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.quizzes
    ADD CONSTRAINT fkcblaekfu02utjnygfc3eqsgxy FOREIGN KEY (subject_id) REFERENCES public.subjects(id);


--
-- Name: teacher_subjects fkdweqkwxroox2u7pbmksehx04i; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teacher_subjects
    ADD CONSTRAINT fkdweqkwxroox2u7pbmksehx04i FOREIGN KEY (subject_id) REFERENCES public.subjects(id);


--
-- Name: notes fkg1fqdmt8tr67gx64gpyo45tb8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notes
    ADD CONSTRAINT fkg1fqdmt8tr67gx64gpyo45tb8 FOREIGN KEY (document_id) REFERENCES public.uploaded_documents(id);


--
-- Name: notes fkh4rhmcyry4jlp1f6fht2xvag3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notes
    ADD CONSTRAINT fkh4rhmcyry4jlp1f6fht2xvag3 FOREIGN KEY (subject_id) REFERENCES public.subjects(id);


--
-- Name: user_profiles fkjcad5nfve11khsnpwj1mv8frj; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT fkjcad5nfve11khsnpwj1mv8frj FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: notes fkjo4xqndcpxe1j1q27vufmwbh0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notes
    ADD CONSTRAINT fkjo4xqndcpxe1j1q27vufmwbh0 FOREIGN KEY (teacher_id) REFERENCES public.users(id);


--
-- Name: password_reset_tokens fkk3ndxg5xp6v7wd4gjyusp15gq; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT fkk3ndxg5xp6v7wd4gjyusp15gq FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: uploaded_documents fkld5lcu68kwvtj90qofyo0xl47; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.uploaded_documents
    ADD CONSTRAINT fkld5lcu68kwvtj90qofyo0xl47 FOREIGN KEY (subject_id) REFERENCES public.subjects(id);


--
-- Name: questions fkn3gvco4b0kewxc0bywf1igfms; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.questions
    ADD CONSTRAINT fkn3gvco4b0kewxc0bywf1igfms FOREIGN KEY (quiz_id) REFERENCES public.quizzes(id);


--
-- Name: student_quiz_results fknip8ybyo1l3wt6n7ox5plb2n4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_quiz_results
    ADD CONSTRAINT fknip8ybyo1l3wt6n7ox5plb2n4 FOREIGN KEY (quiz_id) REFERENCES public.quizzes(id);


--
-- Name: student_enrollments fkouh272eabmw474wrli0ov4pmh; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_enrollments
    ADD CONSTRAINT fkouh272eabmw474wrli0ov4pmh FOREIGN KEY (subject_id) REFERENCES public.subjects(id);


--
-- Name: teacher_subjects fkq4e6w8v6kqcd1ua0eaui9jho; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.teacher_subjects
    ADD CONSTRAINT fkq4e6w8v6kqcd1ua0eaui9jho FOREIGN KEY (teacher_id) REFERENCES public.users(id);


--
-- Name: student_quiz_results fkql2ensua332myuh4q08nc4loy; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_quiz_results
    ADD CONSTRAINT fkql2ensua332myuh4q08nc4loy FOREIGN KEY (student_id) REFERENCES public.users(id);


--
-- Name: student_enrollments fkt00l40y9sd2v6vsijfovuna61; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.student_enrollments
    ADD CONSTRAINT fkt00l40y9sd2v6vsijfovuna61 FOREIGN KEY (student_id) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--

\unrestrict PUQnes8kKQuDGEaMgw3WGd0cp37s7CgNTX0rmJmp9odCfAuxh98KvPogfZMVuaz

