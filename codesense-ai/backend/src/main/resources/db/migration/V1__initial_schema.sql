-- ============================================================
-- V1__initial_schema.sql
-- CodeSense AI - Initial Database Schema
--
-- Compatible with Neon.tech (PostgreSQL 16 + pgvector)
-- Uses gen_random_uuid() — no uuid-ossp extension needed.
-- Run once in Neon SQL editor BEFORE starting the app:
--   CREATE EXTENSION IF NOT EXISTS vector;
-- ============================================================

-- ─── Users ───────────────────────────────────────────────────────────────────

CREATE TABLE users (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL DEFAULT 'USER',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- ─── Projects ────────────────────────────────────────────────────────────────

CREATE TABLE projects (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_project_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_projects_user_id ON projects(user_id);
CREATE INDEX idx_projects_status  ON projects(status);

-- ─── Repositories ────────────────────────────────────────────────────────────

CREATE TABLE repositories (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    source_type         VARCHAR(50)  NOT NULL,
    github_url          VARCHAR(1000),
    github_owner        VARCHAR(255),
    github_repo         VARCHAR(255),
    default_branch      VARCHAR(255),
    local_path          VARCHAR(1000),
    status              VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    analysis_status     VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    ingestion_status    VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    total_files         INTEGER      DEFAULT 0,
    total_chunks        INTEGER      DEFAULT 0,
    languages           TEXT,
    primary_language    VARCHAR(100),
    error_message       TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_repositories_project_id ON repositories(project_id);
CREATE INDEX idx_repositories_status     ON repositories(status);

-- ─── Repository Files ────────────────────────────────────────────────────────

CREATE TABLE repository_files (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id   UUID          NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    project_id      UUID          NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    file_path       VARCHAR(2000) NOT NULL,
    file_name       VARCHAR(255)  NOT NULL,
    extension       VARCHAR(50),
    language        VARCHAR(100),
    size_bytes      BIGINT        DEFAULT 0,
    line_count      INTEGER       DEFAULT 0,
    content         TEXT,
    content_hash    VARCHAR(64),
    is_binary       BOOLEAN       NOT NULL DEFAULT FALSE,
    is_ignored      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_repo_files_repository_id ON repository_files(repository_id);
CREATE INDEX idx_repo_files_project_id    ON repository_files(project_id);
CREATE INDEX idx_repo_files_language      ON repository_files(language);

-- ─── AI: Repository Chunks (PGVector) ────────────────────────────────────────

CREATE TABLE repository_chunks (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID          NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    repository_id   UUID          NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    file_id         UUID          REFERENCES repository_files(id) ON DELETE SET NULL,
    file_path       VARCHAR(2000) NOT NULL,
    language        VARCHAR(100),
    symbol_name     VARCHAR(500),
    symbol_type     VARCHAR(100),
    chunk_type      VARCHAR(100)  NOT NULL DEFAULT 'TEXT',
    chunk_index     INTEGER       NOT NULL DEFAULT 0,
    start_line      INTEGER,
    end_line        INTEGER,
    content         TEXT          NOT NULL,
    content_hash    VARCHAR(64),
    metadata        JSONB,
    embedding       vector(768),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chunks_project_id    ON repository_chunks(project_id);
CREATE INDEX idx_chunks_repository_id ON repository_chunks(repository_id);
CREATE INDEX idx_chunks_language      ON repository_chunks(language);

-- HNSW index for cosine similarity (works on empty tables, better than ivfflat)
CREATE INDEX idx_chunks_embedding ON repository_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- ─── Conversations ───────────────────────────────────────────────────────────

CREATE TABLE conversations (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id      UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    repository_id   UUID         NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    title           VARCHAR(500),
    status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conversations_user_id       ON conversations(user_id);
CREATE INDEX idx_conversations_repository_id ON conversations(repository_id);

-- ─── Conversation Messages ───────────────────────────────────────────────────

CREATE TABLE conversation_messages (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID         NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role            VARCHAR(50)  NOT NULL,
    content         TEXT         NOT NULL,
    sources         JSONB,
    token_count     INTEGER,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conv_messages_conversation_id ON conversation_messages(conversation_id);

-- ─── Documentation ───────────────────────────────────────────────────────────

CREATE TABLE documentation (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    repository_id   UUID         NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    doc_type        VARCHAR(100) NOT NULL,
    title           VARCHAR(500),
    content         TEXT,
    format          VARCHAR(50)  NOT NULL DEFAULT 'MARKDOWN',
    version         INTEGER      NOT NULL DEFAULT 1,
    status          VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    generated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documentation_project_id    ON documentation(project_id);
CREATE INDEX idx_documentation_repository_id ON documentation(repository_id);

-- ─── AI Requests / Audit ─────────────────────────────────────────────────────

CREATE TABLE ai_requests (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id        UUID         REFERENCES projects(id) ON DELETE SET NULL,
    repository_id     UUID         REFERENCES repositories(id) ON DELETE SET NULL,
    request_type      VARCHAR(100) NOT NULL,
    model_id          VARCHAR(255),
    prompt_tokens     INTEGER,
    completion_tokens INTEGER,
    total_tokens      INTEGER,
    latency_ms        INTEGER,
    status            VARCHAR(50)  NOT NULL DEFAULT 'SUCCESS',
    error_message     TEXT,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_requests_user_id       ON ai_requests(user_id);
CREATE INDEX idx_ai_requests_project_id    ON ai_requests(project_id);
CREATE INDEX idx_ai_requests_repository_id ON ai_requests(repository_id);
