# Database Architecture — CodeSense AI

> **Owner**: Vishwa (Team Member 2) — Backend & Repository Management
> **Tech**: PostgreSQL 16 + PGVector extension

---

## Overview

CodeSense AI uses a single PostgreSQL 16 database with the PGVector extension for vector similarity search. Schema versioning is managed by Flyway.

---

## Setup

```sql
-- Required extensions (auto-installed via Docker init script)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";
```

---

## Table Reference

### `users`
User accounts with BCrypt-hashed passwords.

| Column        | Type        | Notes                     |
|---------------|-------------|---------------------------|
| id            | UUID PK     | uuid-ossp default         |
| email         | VARCHAR 255 | UNIQUE, NOT NULL          |
| name          | VARCHAR 255 | NOT NULL                  |
| password_hash | VARCHAR 255 | BCrypt                    |
| enabled       | BOOLEAN     | default true              |
| created_at    | TIMESTAMPTZ | Flyway/JPA auditing       |
| updated_at    | TIMESTAMPTZ |                           |

---

### `roles`
Roles: `ROLE_USER`, `ROLE_ADMIN`.

| Column | Type        |
|--------|-------------|
| id     | UUID PK     |
| name   | VARCHAR 50  |

---

### `user_roles`
Join table: many-to-many users ↔ roles.

| Column  | Type    |
|---------|---------|
| user_id | UUID FK |
| role_id | UUID FK |

---

### `projects`
User-owned projects that group repositories.

| Column      | Type        | Notes                          |
|-------------|-------------|--------------------------------|
| id          | UUID PK     |                                |
| user_id     | UUID FK     | references users(id)           |
| name        | VARCHAR 255 | NOT NULL                       |
| description | TEXT        |                                |
| status      | VARCHAR 50  | ACTIVE, ARCHIVED               |
| created_at  | TIMESTAMPTZ |                                |
| updated_at  | TIMESTAMPTZ |                                |

---

### `repositories`
Repositories within a project. Supports ZIP uploads and GitHub imports.

| Column            | Type        | Notes                          |
|-------------------|-------------|--------------------------------|
| id                | UUID PK     |                                |
| project_id        | UUID FK     | references projects(id)        |
| name              | VARCHAR 255 |                                |
| description       | TEXT        |                                |
| source_type       | VARCHAR 50  | ZIP, GITHUB                    |
| github_url        | VARCHAR 512 | nullable                       |
| github_owner      | VARCHAR 255 | nullable                       |
| github_repo       | VARCHAR 255 | nullable                       |
| github_branch     | VARCHAR 255 | default 'main'                 |
| storage_path      | VARCHAR 512 | local FS path                  |
| status            | VARCHAR 50  | PENDING, READY, ERROR          |
| analysis_status   | VARCHAR 50  | PENDING, PROCESSING, COMPLETE  |
| ingestion_status  | VARCHAR 50  | PENDING, PROCESSING, COMPLETE  |
| primary_language  | VARCHAR 50  |                                |
| languages         | TEXT        | CSV list                       |
| total_files       | INTEGER     |                                |
| total_lines       | BIGINT      |                                |
| created_at        | TIMESTAMPTZ |                                |
| updated_at        | TIMESTAMPTZ |                                |

---

### `repository_files`
Indexed files within a repository.

| Column        | Type        | Notes                     |
|---------------|-------------|---------------------------|
| id            | UUID PK     |                           |
| repository_id | UUID FK     |                           |
| file_path     | VARCHAR 512 | relative to repo root     |
| file_name     | VARCHAR 255 |                           |
| language      | VARCHAR 50  |                           |
| size_bytes    | BIGINT      |                           |
| line_count    | INTEGER     |                           |
| content       | TEXT        | file content              |
| binary        | BOOLEAN     | default false             |
| ignored       | BOOLEAN     | default false             |
| created_at    | TIMESTAMPTZ |                           |

---

### `repository_chunks`
⭐ **The vector table**. Stores text chunks with PGVector embeddings for semantic search.

| Column        | Type               | Notes                           |
|---------------|--------------------|---------------------------------|
| id            | UUID PK            |                                 |
| project_id    | UUID               | NOT NULL — scopes all queries   |
| repository_id | UUID               | NOT NULL — scopes all queries   |
| file_id       | UUID FK            | nullable                        |
| file_path     | VARCHAR 512        |                                 |
| language      | VARCHAR 50         |                                 |
| symbol_name   | VARCHAR 255        | e.g., "AuthService"             |
| symbol_type   | VARCHAR 50         | CLASS, METHOD, FUNCTION, etc.   |
| start_line    | INTEGER            |                                 |
| end_line      | INTEGER            |                                 |
| content       | TEXT               | chunk text                      |
| metadata      | JSONB              | extra structured data           |
| embedding     | vector(768)        | **PGVector column**             |
| created_at    | TIMESTAMPTZ        |                                 |

**Indexes**:
```sql
-- PGVector HNSW index for cosine similarity search
CREATE INDEX idx_chunks_embedding
ON repository_chunks
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Security scoping index
CREATE INDEX idx_chunks_scope
ON repository_chunks (project_id, repository_id);
```

**Security**: The `project_id + repository_id` combination is always included in WHERE clauses. Cross-project access is architecturally impossible through `VectorSearchService`.

---

### `conversations`
Chat conversation sessions per user + repository.

| Column        | Type        |
|---------------|-------------|
| id            | UUID PK     |
| user_id       | UUID FK     |
| project_id    | UUID FK     |
| repository_id | UUID FK     |
| title         | VARCHAR 255 |
| status        | VARCHAR 50  |
| created_at    | TIMESTAMPTZ |
| updated_at    | TIMESTAMPTZ |

---

### `conversation_messages`
Individual messages within a conversation.

| Column          | Type        | Notes              |
|-----------------|-------------|-------------------|
| id              | UUID PK     |                   |
| conversation_id | UUID FK     |                   |
| role            | VARCHAR 20  | USER, ASSISTANT   |
| content         | TEXT        |                   |
| sources         | TEXT        | JSON array        |
| token_count     | INTEGER     |                   |
| created_at      | TIMESTAMPTZ |                   |

---

### `documentation`
AI-generated documentation (README, API docs).

| Column        | Type        | Notes                          |
|---------------|-------------|--------------------------------|
| id            | UUID PK     |                                |
| project_id    | UUID FK     |                                |
| repository_id | UUID FK     |                                |
| doc_type      | VARCHAR 50  | README, API_DOCS               |
| title         | VARCHAR 255 |                                |
| content       | TEXT        | Markdown                       |
| status        | VARCHAR 50  | DRAFT, PUBLISHED               |
| generated_at  | TIMESTAMPTZ |                                |
| created_at    | TIMESTAMPTZ |                                |

---

## Semantic Search Query

The PGVector similarity search uses cosine distance:

```sql
SELECT *,
    1 - (embedding <=> CAST(:queryVector AS vector)) AS similarity
FROM repository_chunks
WHERE project_id = :projectId
  AND repository_id = :repositoryId
ORDER BY embedding <=> CAST(:queryVector AS vector)
LIMIT :topK;
```

The `<=>` operator is the PGVector cosine distance operator. `1 - distance = similarity`.

---

## Flyway Migrations

Location: `backend/src/main/resources/db/migration/`

| File                        | Description             |
|-----------------------------|-------------------------|
| `V1__initial_schema.sql`    | All tables, indexes, extensions |

---

## Connection Pooling

HikariCP with:
- Max pool size: 20
- Min idle: 5
- Connection timeout: 30s
- Max lifetime: 30 min
