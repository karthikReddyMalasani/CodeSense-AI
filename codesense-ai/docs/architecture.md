# Architecture

## System Overview

```
React Frontend (port 3000)
         |
         | REST/JSON + JWT
         v
Spring Boot Backend (port 8080)
         |
    +----|---------------------+
    |                          |
    v                          v
Core Backend Module       AI Engine Module
(auth, project,          (rag, embedding,
 repository)              llm, vector, ingestion)
    |                          |
    v                          v
PostgreSQL             PGVector + PostgreSQL
                              |
                              v
                      IBM watsonx.ai
                       (IBM Granite)

         Future:
         Team Member 4 Parser
         (JavaParser + Tree-sitter)
```

---

## Backend Package Structure

```
com.codesense/
├── CodeSenseAiApplication.java
├── auth/
│   ├── controller/   AuthController
│   ├── dto/          RegisterRequest, LoginRequest, AuthResponse
│   ├── model/        User, Role
│   ├── repository/   UserRepository
│   ├── security/     JwtService, JwtAuthFilter, SecurityConfig
│   └── service/      AuthService
│
├── project/
│   ├── controller/   ProjectController
│   ├── dto/          CreateProjectRequest, ProjectDto
│   ├── model/        Project, ProjectStatus
│   ├── repository/   ProjectRepository
│   └── service/      ProjectService
│
├── repository/
│   ├── controller/   RepositoryController
│   ├── dto/          RepositoryDto, UploadRequest, GitHubImportRequest
│   ├── model/        Repository, RepositoryFile, enums
│   ├── repository/   RepositoryRepo, RepositoryFileRepository
│   └── service/      RepositoryService, RepositoryStorageService,
│                     GitHubService, LanguageDetectionService
│
├── ai/  [Team Member 3 — Karthik]
│   ├── controller/   AiController
│   ├── service/      AiService
│   ├── rag/          RagService
│   ├── embedding/    EmbeddingService, MockEmbeddingService,
│   │                 WatsonxEmbeddingService
│   ├── llm/          LLMService, LLMRequest, LLMResponse,
│   │                 WatsonxLLMService, MockLLMService,
│   │                 WatsonxProperties
│   ├── vector/       RepositoryChunk, RepositoryChunkRepository,
│   │                 VectorSearchService
│   ├── ingestion/    IngestionService
│   ├── chunking/     ChunkingService
│   ├── prompt/       PromptTemplates
│   ├── conversation/ Conversation, ConversationMessage, repositories
│   ├── model/        Documentation, DocumentationRepository
│   ├── dto/          ChatRequest/Response, SearchRequest/Response,
│   │                 CodeExplain*, GenerateReadme*, GenerateApiDocs*
│   └── exception/    AiProviderException, EmbeddingException,
│                     IngestionException
│
├── integration/
│   └── parser/       ParserIntegrationService [interface]
│                     ParserIntegrationStub [placeholder]
│                     dto/ ParsedFileDTO, CodeElementDTO,
│                          CodeRelationshipDTO, ParsedRepositoryDTO
│
└── common/
    ├── config/       WebConfig, JpaConfig, AsyncConfig, OpenApiConfig
    ├── dto/          ApiResponse, ErrorResponse
    └── exception/    GlobalExceptionHandler, exceptions
```

---

## Security Architecture

```
Request
  ↓
JwtAuthenticationFilter
  ↓ (valid token)
SecurityContextHolder (UserDetails)
  ↓
Controller (@AuthenticationPrincipal UserDetails)
  ↓
Service (ownership check: user → project → repository)
  ↓
Data Access (filtered by user_id/project_id)
```

**Ownership chain:**
- User owns Projects
- Projects own Repositories
- Repositories own Files + Chunks + Conversations
- AI results are scoped by project_id + repository_id

---

## Database Schema Summary

| Table | Purpose |
|-------|---------|
| `users` | Authentication and user management |
| `projects` | Project containers (scoped to users) |
| `repositories` | Repository metadata and status |
| `repository_files` | Individual file content and metadata |
| `repository_chunks` | AI chunks with PGVector embeddings |
| `conversations` | Chat conversation sessions |
| `conversation_messages` | Individual chat messages |
| `documentation` | AI-generated docs (README, API docs) |
| `ai_requests` | AI usage audit log |
