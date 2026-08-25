# CodeSense AI Backend

## Spring Boot 3.3 + Java 21

### Quick Start

```bash
# Ensure PostgreSQL is running with PGVector extension
# Copy and configure .env.example → .env

# Run migrations and start
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**API**: `http://localhost:8080`  
**Swagger**: `http://localhost:8080/swagger-ui.html`

### Package Structure

```
com.codesense/
├── auth/       — JWT authentication, user management
├── project/    — Project CRUD
├── repository/ — Repository upload, GitHub import, file management
├── ai/         — AI Engine (Team Member 3 — Karthik)
│   ├── llm/    — IBM watsonx.ai / Granite LLM abstraction
│   ├── embedding/ — Embedding service
│   ├── vector/    — PGVector semantic search
│   ├── rag/       — RAG engine
│   ├── ingestion/ — Repository ingestion pipeline
│   ├── chunking/  — Code chunking service
│   └── prompt/    — Prompt templates
├── integration/parser/ — Team Member 4 handoff interface
└── common/     — Cross-cutting: config, DTOs, exceptions
```

### Test

```bash
./mvnw test
```
