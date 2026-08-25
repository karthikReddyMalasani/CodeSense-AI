# Team Ownership — CodeSense AI

## Module Boundaries

Each team member owns a clearly defined module. No member should duplicate another's core implementation.

---

## Team Member 1 — Reddy Prasanna
**Role:** Frontend Developer / UI-UX Engineer

**Owns:**
- `frontend/src/` — All React pages and components
- `frontend/src/pages/` — All 14 pages
- `frontend/src/components/` — Reusable UI components
- `frontend/src/services/api.js` — API client
- `frontend/src/context/AuthContext.jsx` — Auth state

**Key pages:**
- Login, Register, Dashboard, Projects, Repository Browser
- AI Chat, Semantic Search, Code Explanation
- README, API Docs, Architecture, Metrics, Dependencies

---

## Team Member 2 — Vishwa
**Role:** Backend Developer / Spring Boot Engineer

**Owns:**
- `backend/src/main/java/com/codesense/auth/` — JWT auth, registration, login
- `backend/src/main/java/com/codesense/project/` — Project CRUD
- `backend/src/main/java/com/codesense/repository/` — Repository upload, GitHub clone, file management
- `backend/src/main/resources/db/migration/` — Flyway SQL schemas
- `backend/src/main/resources/application*.yml` — Spring Boot config

**Key APIs:**
- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
- `CRUD /api/projects/**`
- `POST /api/projects/{id}/repositories/upload` (ZIP upload)
- `POST /api/projects/{id}/repositories/github` (GitHub import)
- `GET /api/repositories/{id}/files/**`

---

## Team Member 3 — Karthik
**Role:** AI Engineer / IBM watsonx.ai Specialist

**Owns:**
- `backend/src/main/java/com/codesense/ai/` — All AI components
- `ai/rag/RagService.java` — RAG pipeline
- `ai/ingestion/IngestionService.java` — Repository ingestion
- `ai/vector/VectorSearchService.java` — PGVector semantic search
- `ai/embedding/` — Embedding services (Mock + watsonx)
- `ai/llm/` — LLM services (Mock + watsonx)
- `ai/prompt/PromptTemplates.java` — All AI prompts
- `ai/chunking/ChunkingService.java` — Code chunking
- `ai/conversation/` — Conversation memory
- `integration/parser/` — Parser integration interface + stub

**Key APIs:**
- `POST /api/ai/chat` — Repository chatbot (RAG)
- `POST /api/ai/search` — Semantic search
- `POST /api/ai/explain-code` — Code explanation
- `POST /api/ai/generate-readme` — README generation
- `POST /api/ai/generate-api-docs` — API docs generation
- `POST /api/ai/ingest` — Trigger ingestion

---

## Team Member 4 — Prashanthi
**Role:** Code Intelligence Engineer

**Owns:**
- `backend/src/main/java/com/codesense/parser/` — All parser components
- `parser/core/JavaParserCodeParser.java` — JavaParser integration
- `parser/core/TreeSitterCodeParser.java` — Multi-language parsing
- `parser/core/GenericCodeParser.java` — Fallback parser
- `parser/service/ParserRouter.java` — Language-based parser selection
- `parser/service/RepositoryParserService.java` — Repository-level parsing
- `parser/service/CodeMetricsService.java` — Code metrics
- `parser/service/DependencyAnalysisService.java` — JGraphT dependency graphs
- `parser/service/UmlDiagramService.java` — PlantUML/Mermaid diagrams
- `parser/controller/ParserController.java` — Parser REST API

**Key APIs:**
- `POST /api/parser/repositories/{id}/parse`
- `GET /api/parser/repositories/{id}/metrics`
- `POST /api/parser/repositories/{id}/dependency-graph`
- `POST /api/parser/repositories/{id}/uml`
- `POST /api/parser/repositories/{id}/architecture`

**Integration with Team Member 3:**
- Implements `ParserIntegrationService` interface
- Populates `ParsedRepositoryDTO` and `ParsedFileDTO`
- Team Member 3's `IngestionService` consumes these DTOs

---

## Team Member 5 — Vishnu
**Role:** DevOps & QA Engineer

**Owns:**
- `backend/src/test/` — All backend tests (84 unit tests)
- `backend/Dockerfile` — Backend container
- `frontend/Dockerfile` — Frontend container
- `frontend/nginx.conf` — Nginx SPA config
- `docker-compose.yml` — Production compose
- `docker-compose.dev.yml` — Development compose
- `docker/` — Init scripts, Prometheus, Grafana configs
- `.github/workflows/ci.yml` — CI/CD pipeline
- `test-data/` — Sample repositories
- `docs/deployment.md`, `docs/testing.md`

**Monitoring stack:**
- Spring Boot Actuator + Micrometer
- Prometheus (metrics scraping)
- Grafana (dashboards)

---

## Cross-Team Integration Points

| Integration | From | To |
|---|---|---|
| Parser metadata → AI ingestion | Team 4 | Team 3 |
| Repository files → Parser | Team 2 | Team 4 |
| AI APIs → Frontend | Team 3 | Team 1 |
| Backend APIs → Frontend | Team 2 | Team 1 |
| Parser APIs → Frontend | Team 4 | Team 1 |
