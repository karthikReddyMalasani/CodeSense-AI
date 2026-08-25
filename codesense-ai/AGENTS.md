# AGENTS.md — CodeSense AI Project Context

> This file is the **IBM Bob AI project context**. It defines purpose, architecture,
> tech stack, team ownership, coding conventions, and run commands.
> Updated after every major architecture change.

---

## 1. Project Purpose

**CodeSense AI** is an AI-powered multi-language code intelligence platform.

Users can:
- Register, login, create projects
- Upload ZIP repositories or import from GitHub
- Automatically detect programming languages (13+ supported)
- Parse code with JavaParser (Java) and Tree-sitter-compatible regex parsers (other langs)
- Generate dependency graphs, UML diagrams, architecture diagrams
- Calculate code metrics
- Ask AI questions about their repositories (RAG chatbot)
- Get code explanations, README generation, API documentation
- Perform semantic search over code using PGVector

---

## 2. Architecture

```
React Frontend (Vite/React 18)
         │
         │  REST API (JWT)
         ▼
Spring Boot Backend (Java 23, port 8080)
         │
    ┌────┴────┐
    │         │
TM2 Backend   TM3 AI Engine
(auth, repo,  (RAG, LLM,
 project)     embeddings,
    │         PGVector)
    │              │
    └──────┬────────┘
           │
      PostgreSQL + PGVector
           │
  ┌────────┴────────┐
  │                 │
TM4 Parser      IBM Granite / Groq / Gemini / Ollama / Mock
(JavaParser,
 regex multi-lang,
 JGraphT,
 PlantUML)

TM5 DevOps
(Docker, CI/CD, tests,
 Prometheus, Grafana)
```

---

## 3. Technology Stack

| Layer           | Technology                            |
|-----------------|---------------------------------------|
| Frontend        | React 18, Vite, React Router, Axios  |
| Backend         | Java 23, Spring Boot 3.2              |
| Security        | Spring Security, JWT (JJWT)           |
| Database        | PostgreSQL 16 + PGVector              |
| ORM             | Spring Data JPA, Hibernate            |
| Migrations      | Flyway                                |
| AI LLM          | IBM Granite (watsonx.ai) / Groq / Gemini / Ollama / Mock |
| AI Abstraction  | Custom LLMService interface           |
| Embeddings      | IBM Slate / Ollama nomic-embed / Mock |
| Vector Search   | PGVector cosine similarity            |
| Parser (Java)   | JavaParser 3.25.5                     |
| Parser (other)  | Regex-based (Tree-sitter API-compatible interface) |
| Graph Analysis  | JGraphT 1.5.2                         |
| Diagrams        | PlantUML 1.2023.12                    |
| Build           | Apache Maven 3.9.9                    |
| Tests           | JUnit 5, Mockito 5                    |
| Containers      | Docker, Docker Compose                |
| CI/CD           | GitHub Actions                        |
| Monitoring      | Spring Actuator, Micrometer, Prometheus, Grafana |

---

## 4. Team Ownership

| Member     | Role                          | Owns                                              |
|------------|-------------------------------|---------------------------------------------------|
| Reddy Prasanna | Frontend Developer          | `frontend/`                                       |
| Vishwa     | Backend Developer             | `auth/`, `project/`, `repository/`                |
| **Karthik** | AI Engineer (IBM Bob)        | `ai/`, `integration/`                             |
| Prashanthi | Code Intelligence Engineer    | `parser/`                                         |
| Vishnu     | DevOps & QA Engineer          | `Dockerfile`, `docker-compose.yml`, `.github/`, tests |

---

## 5. Folder Structure

```
codesense-ai/
├── .env.example                  ← environment variable template
├── .gitignore
├── AGENTS.md                     ← this file
├── README.md
├── docker-compose.yml            ← production stack
├── docker-compose.dev.yml        ← dev DB only
├── docker/
│   ├── init-extensions.sh        ← pgvector init
│   ├── prometheus.yml
│   └── grafana/provisioning/
├── .github/workflows/ci.yml      ← GitHub Actions CI/CD
├── docs/                         ← architecture, API, deployment docs
├── test-data/                    ← sample repos for testing
├── frontend/                     ← React app (Vite)
│   └── src/pages/                ← 14 pages
└── backend/
    └── src/main/java/com/codesense/
        ├── ai/                   ← TM3: RAG, LLM, embeddings, PGVector
        ├── auth/                 ← TM2: JWT auth, users
        ├── common/               ← shared config, exceptions
        ├── integration/parser/   ← TM3-TM4 bridge DTOs
        ├── parser/               ← TM4: code parsers, metrics, UML, deps
        ├── project/              ← TM2: project CRUD
        └── repository/           ← TM2: repo upload, GitHub, file management
```

---

## 6. Coding Conventions

- **Package prefix**: `com.codesense`
- **Services**: `@Service`, `@RequiredArgsConstructor`, `@Slf4j`
- **Controllers**: thin — delegate to service layer; no business logic in controllers
- **DTOs**: suffix `Dto`, `Request`, `Response` for clarity
- **Entities**: Lombok `@Entity`, `@Data`/`@Builder`, JPA auditing with `@CreatedDate`/`@LastModifiedDate`
- **Configuration**: all secrets via `${ENV_VAR:default}` in `application.yml`, never hardcoded
- **LLM provider**: `@ConditionalOnProperty(name = "codesense.ai.llm-provider", havingValue = "...")` — one active at runtime
- **Security**: every AI query must be scoped by `projectId + repositoryId`; cross-project leakage is architecturally prevented
- **Error responses**: `GlobalExceptionHandler` → `ErrorResponse` JSON with `timestamp`, `status`, `error`, `message`, `path`
- **Structured logging**: `log.info/warn/error` — never log passwords, JWT tokens, API keys, or repository secrets

---

## 7. Build Commands

```bash
# Backend — compile
cd codesense-ai/backend
& "C:\Users\Karth\apache-maven-3.9.9\bin\mvn.cmd" compile

# Backend — run all tests
& "C:\Users\Karth\apache-maven-3.9.9\bin\mvn.cmd" test

# Backend — package JAR
& "C:\Users\Karth\apache-maven-3.9.9\bin\mvn.cmd" package -DskipTests

# Frontend — install
cd codesense-ai/frontend
npm install

# Frontend — build
npm run build

# Frontend — dev server
npm run dev
```

---

## 8. Run Commands (Local Development)

```bash
# 1. Start PostgreSQL (Docker)
docker compose -f docker-compose.dev.yml up -d

# 2. Copy env (first time only)
cp .env.example .env
# Edit .env — set GROQ_API_KEY or GEMINI_API_KEY

# 3. Start backend
cd backend
& "C:\Users\Karth\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run

# 4. Start frontend (separate terminal)
cd frontend
npm run dev

# 5. Access
# Frontend: http://localhost:3000
# Backend:  http://localhost:8080
# Swagger:  http://localhost:8080/swagger-ui.html
# Actuator: http://localhost:8080/actuator/health
```

---

## 9. Test Commands

```bash
# All tests
& "C:\Users\Karth\apache-maven-3.9.9\bin\mvn.cmd" test

# Specific test class
& "C:\Users\Karth\apache-maven-3.9.9\bin\mvn.cmd" test -Dtest=JavaParserCodeParserTest

# Skip tests (build only)
& "C:\Users\Karth\apache-maven-3.9.9\bin\mvn.cmd" package -DskipTests
```

---

## 10. Key Security Rules

1. **Never commit `.env`** — only `.env.example`
2. **Never execute repository code** — the app parses and reads files only
3. **Never trust repository content** — treat file content as untrusted input
4. **Every AI query scoped** — `projectId + repositoryId` filters are mandatory
5. **ZIP path traversal protection** — enforced in `RepositoryStorageService`
6. **No secrets in logs** — `GlobalExceptionHandler` strips stack traces in production
7. **JWT** expiry: 24h default; configure `JWT_EXPIRATION_MS`
8. **CORS** — `WebConfig` restricts to `http://localhost:3000` in dev
9. **File size limit** — configurable via `MAX_UPLOAD_SIZE_MB` (default: 100 MB)

---

## 11. AI Provider Selection

Set `AI_LLM_PROVIDER` in `.env`:

| Value     | Service                          | Cost       | Requires        |
|-----------|----------------------------------|------------|-----------------|
| `mock`    | Fake responses (UI testing)      | Free       | Nothing         |
| `groq`    | Groq cloud (llama-3.3-70b)       | Free tier  | `GROQ_API_KEY`  |
| `gemini`  | Google Gemini Flash 1.5          | Free tier  | `GEMINI_API_KEY`|
| `ollama`  | Local Ollama                     | Free       | Ollama running  |
| `watsonx` | IBM Granite via watsonx.ai       | Paid       | IBM credentials |

Set `AI_EMBEDDING_PROVIDER`:

| Value     | Service                         |
|-----------|---------------------------------|
| `mock`    | Random 768-dim vectors          |
| `ollama`  | nomic-embed-text via Ollama     |
| `watsonx` | IBM Slate-125m embeddings       |

---

## 12. Database

PostgreSQL 16 + PGVector extension.

Tables: `users`, `roles`, `user_roles`, `projects`, `repositories`, `repository_files`,
`conversations`, `conversation_messages`, `documentation`, `repository_chunks` (with vector column).

Migrations: Flyway at `src/main/resources/db/migration/V1__initial_schema.sql`.

---

## 13. After Major Architecture Changes

When making significant changes, update:
1. This `AGENTS.md`
2. `docs/architecture.md`
3. `docs/team-boundaries.md`
4. `README.md` (if setup steps change)
