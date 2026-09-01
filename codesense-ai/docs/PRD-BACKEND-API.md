# Product Requirements Document (PRD) — Backend API
**Product**: CodeSense AI  
**Version**: 1.0  
**Last Updated**: 2026-09-01  
**Prepared By**: Team Member 3 (Backend Lead)

---

## Executive Summary

CodeSense AI Backend is a Spring Boot 3.x REST API that powers the frontend with comprehensive code intelligence, AI-powered analysis, and repository management services. It provides secure authentication, multi-language code parsing, semantic search via PGVector embeddings, RAG-powered chatbot, code metrics calculation, and architecture visualization. The API supports 13+ programming languages and integrates with multiple LLM providers (IBM Granite, Groq, Gemini, Ollama).

---

## Product Overview

### Vision
Provide robust, scalable REST APIs that enable developers to programmatically access code intelligence and AI analysis capabilities.

### Architecture Pattern
- **Monolithic** with clear package boundaries (can be split into microservices later)
- **Layered**: Controllers → Services → Repositories → Database
- **Security**: JWT-based, role-based access control (RBAC)
- **Data**: PostgreSQL 16 + PGVector for semantic search
- **Async**: Async ingestion tasks with Spring's `@Async`

### Core Services
1. **Auth Service**: JWT token management, user registration/login
2. **Project Service**: CRUD operations on user projects
3. **Repository Service**: Repository management, file storage, language detection
4. **Parser Service**: Multi-language AST parsing, code metrics, dependency analysis
5. **AI Service**: LLM integration, embeddings, RAG pipeline, semantic search
6. **Ingestion Service**: Repository indexing, chunk creation, vector embedding
7. **RAG Service**: Question answering using retrieved context

---

## Detailed API Specifications

### 1. Authentication APIs

#### 1.1 POST `/api/auth/register`
**Purpose**: Register a new user account

**Request Body**:
```json
{
  "name": "string (3-100 chars)",
  "email": "string (valid RFC 5322)",
  "password": "string (min 8 chars, uppercase, lowercase, number)"
}
```

**Response** (201 Created):
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "John Doe",
    "email": "john@example.com",
    "role": "USER",
    "expiresIn": 86400000
  },
  "message": "User registered successfully"
}
```

**Error Responses**:
- 400 Bad Request: Invalid email or weak password
- 409 Conflict: Email already registered
- 422 Unprocessable Entity: Validation failed

**Acceptance Criteria**:
- Email uniqueness enforced via database constraint
- Password hashed using bcrypt (min 10 rounds)
- JWT token generated with 24-hour expiration
- User role defaults to "USER"
- Audit log entry created
- Optional: Send confirmation email (disabled by default)

**Performance**: < 500ms including bcrypt hashing

**Security**:
- Rate limit: 5 attempts per minute per IP
- No password requirements display in response
- Audit log all registrations

---

#### 1.2 POST `/api/auth/login`
**Purpose**: Authenticate user with credentials

**Request Body**:
```json
{
  "email": "string",
  "password": "string"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "John Doe",
    "email": "john@example.com",
    "role": "USER",
    "lastLogin": "2026-09-01T10:30:00Z",
    "expiresIn": 86400000
  }
}
```

**Error Responses**:
- 401 Unauthorized: Invalid credentials
- 429 Too Many Requests: Rate limit exceeded
- 403 Forbidden: Account locked (after 5 failed attempts)

**Acceptance Criteria**:
- Case-insensitive email matching
- Return JWT valid for 24 hours
- Fail safely (don't leak whether email exists)
- Lock account after 5 failed attempts
- Track last login timestamp
- Clear failed login attempts on success

**Performance**: < 300ms including bcrypt verification

**Security**:
- Rate limit: 10 attempts per minute per email
- Log failed login attempts
- Account lockout after 5 failures (30 min window)

---

#### 1.3 POST `/api/auth/social-login`
**Purpose**: OAuth social authentication (Google, GitHub)

**Request Body**:
```json
{
  "provider": "google|github",
  "idToken": "string (OAuth token from provider)",
  "email": "string",
  "name": "string"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "isNewUser": false,
    "expiresIn": 86400000
  }
}
```

**Acceptance Criteria**:
- Support Google and GitHub OAuth
- Verify OAuth token with provider
- Create user if doesn't exist (auto-signup)
- Link social account to existing user (if same email)
- Return standard JWT token
- Handle provider outages gracefully

**Security**:
- Validate OAuth token with provider (no caching)
- Store provider account ID separately
- Never store OAuth token

---

#### 1.4 GET `/api/auth/me`
**Purpose**: Get current authenticated user profile

**Headers**:
```
Authorization: Bearer <token>
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "John Doe",
    "email": "john@example.com",
    "role": "USER",
    "createdAt": "2026-01-15T08:30:00Z",
    "lastLogin": "2026-09-01T10:30:00Z"
  }
}
```

**Error Responses**:
- 401 Unauthorized: Token missing or invalid
- 403 Forbidden: Token expired

**Acceptance Criteria**:
- Validate JWT signature and expiration
- Return user profile with all metadata
- Refresh last-accessed timestamp
- No sensitive data exposure

**Performance**: < 100ms

---

### 2. Project Management APIs

#### 2.1 POST `/api/projects`
**Purpose**: Create a new project

**Request Body**:
```json
{
  "name": "string (3-100 chars)",
  "description": "string (optional, max 500 chars)"
}
```

**Response** (201 Created):
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "My Awesome Project",
    "description": "A collection of my repositories",
    "ownerId": "550e8400-e29b-41d4-a716-446655440001",
    "status": "ACTIVE",
    "repositoryCount": 0,
    "createdAt": "2026-09-01T10:30:00Z",
    "updatedAt": "2026-09-01T10:30:00Z"
  }
}
```

**Error Responses**:
- 400 Bad Request: Invalid name or description
- 409 Conflict: Project name already exists for user
- 429 Too Many Requests: User exceeded project limit

**Acceptance Criteria**:
- Project name must be unique per user
- Default status is "ACTIVE"
- Set owner to authenticated user
- Initialize repository count to 0
- Create audit log entry
- Enforce max 50 projects per free user (1000 for paid)

**Performance**: < 200ms

**Security**:
- Only owner can access project
- Soft delete (set status to DELETED)

---

#### 2.2 GET `/api/projects`
**Purpose**: List all projects for authenticated user

**Query Parameters**:
```
?page=0&size=20&sort=createdAt,desc
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "projects": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "name": "My Project",
        "description": "...",
        "status": "ACTIVE",
        "repositoryCount": 5,
        "createdAt": "2026-01-15T08:30:00Z",
        "updatedAt": "2026-09-01T10:30:00Z"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 42,
      "totalPages": 3
    }
  }
}
```

**Acceptance Criteria**:
- Return only authenticated user's projects
- Support pagination (default 20 per page, max 100)
- Support sorting by: createdAt, name, repositoryCount
- Exclude soft-deleted projects
- Include repository count for each project
- Return with consistent timestamp format (ISO 8601)

**Performance**: < 500ms for 100 projects

---

#### 2.3 GET `/api/projects/{projectId}`
**Purpose**: Get project details

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "My Project",
    "description": "...",
    "ownerId": "550e8400-e29b-41d4-a716-446655440001",
    "status": "ACTIVE",
    "repositories": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440002",
        "name": "backend-api",
        "language": "Java",
        "fileCount": 150,
        "totalSize": 2500000,
        "status": "READY",
        "ingestionStatus": "COMPLETED"
      }
    ],
    "createdAt": "2026-01-15T08:30:00Z",
    "updatedAt": "2026-09-01T10:30:00Z"
  }
}
```

**Error Responses**:
- 404 Not Found: Project doesn't exist
- 403 Forbidden: User lacks access to project

**Acceptance Criteria**:
- Include all repositories in project
- Verify user has access before returning
- Include repository details for quick overview
- Return 404 if project deleted or user not owner

**Performance**: < 300ms

---

#### 2.4 DELETE `/api/projects/{projectId}`
**Purpose**: Soft-delete a project

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "message": "Project deleted successfully",
    "projectId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**Error Responses**:
- 404 Not Found: Project doesn't exist
- 403 Forbidden: User not owner

**Acceptance Criteria**:
- Soft delete (mark status as DELETED, don't remove data)
- User can recover deleted project within 30 days
- Cascade soft-delete to repositories
- Create audit log entry
- Don't remove vector embeddings immediately

**Performance**: < 200ms

---

### 3. Repository Management APIs

#### 3.1 POST `/api/projects/{projectId}/repositories/upload`
**Purpose**: Upload a ZIP file as a repository

**Request**:
- Content-Type: multipart/form-data
- Parts:
  - `file`: ZIP file (max 1GB)
  - `name`: Repository name (optional, defaults to filename)

**Response** (202 Accepted):
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "name": "my-repo",
    "projectId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "EXTRACTING",
    "totalFiles": 0,
    "totalSize": 0,
    "progress": {
      "stage": "extracting",
      "percentage": 0
    }
  }
}
```

**Error Responses**:
- 400 Bad Request: Invalid file format or size
- 413 Payload Too Large: File exceeds 1GB limit
- 429 Too Many Requests: User exceeded upload quota
- 507 Insufficient Storage: Server storage full

**Acceptance Criteria**:
- Accept ZIP files (tar.gz optional)
- Max 1GB per repository
- Max 10GB total per user
- Extract ZIP asynchronously
- Detect repository language
- Ignore binary files and common exclusions (.git, node_modules, etc.)
- Scan for malware (ClamAV) before extraction
- Support resumable uploads (optional)
- Show upload progress via WebSocket or polling

**Performance**: < 5 seconds to start extraction, full extraction < 5 min for 1GB

**Security**:
- Validate ZIP structure (prevent zip bombs)
- Scan files for viruses/malware
- Isolate extraction in sandbox
- Max file name length 255 chars
- Rate limit: 5 uploads per hour per user

---

#### 3.2 POST `/api/projects/{projectId}/repositories/github`
**Purpose**: Import repository from GitHub

**Request Body**:
```json
{
  "githubUrl": "https://github.com/owner/repo",
  "name": "optional-override",
  "branch": "main"
}
```

**Response** (202 Accepted):
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "name": "repo",
    "projectId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "CLONING",
    "progress": {
      "stage": "cloning",
      "percentage": 15
    }
  }
}
```

**Error Responses**:
- 400 Bad Request: Invalid GitHub URL
- 404 Not Found: Repository not found on GitHub
- 401 Unauthorized: Private repository and no token provided

**Acceptance Criteria**:
- Clone repository asynchronously
- Support public and private repositories (with token)
- Support specific branches
- Store git clone history for updates
- Max 1GB per repository (check before cloning)
- Support GitHub enterprise URLs
- Exclude .git directory from indexing

**Performance**: < 10 seconds to start clone, full clone < 5 min for 1GB

**Security**:
- Validate GitHub URL format
- Support personal access tokens (PAT) for private repos
- Don't log or store tokens in logs
- Rate limit: 10 imports per day per user

---

#### 3.3 GET `/api/projects/{projectId}/repositories`
**Purpose**: List repositories in a project

**Query Parameters**:
```
?page=0&size=20&status=READY&sort=createdAt,desc
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "repositories": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440002",
        "name": "backend-api",
        "projectId": "550e8400-e29b-41d4-a716-446655440000",
        "status": "READY",
        "totalFiles": 150,
        "totalSize": 2500000,
        "languages": ["Java", "SQL"],
        "ingestionStatus": "COMPLETED",
        "totalChunks": 5420,
        "createdAt": "2026-01-15T08:30:00Z"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 5,
      "totalPages": 1
    }
  }
}
```

**Acceptance Criteria**:
- Return repositories for given project
- Filter by status (READY, PROCESSING, FAILED, DELETED)
- Include ingestion status for each repository
- Show detected languages
- Pagination support (default 20)
- Verify user has access to project

**Performance**: < 300ms for 100 repositories

---

#### 3.4 GET `/api/repositories/{repositoryId}`
**Purpose**: Get repository details

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "name": "backend-api",
    "projectId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "READY",
    "source": "UPLOAD|GITHUB",
    "sourceUrl": "https://github.com/owner/repo",
    "totalFiles": 150,
    "analyzedFiles": 145,
    "totalSize": 2500000,
    "languages": ["Java", "SQL", "XML"],
    "ingestionStatus": "COMPLETED",
    "totalChunks": 5420,
    "errorMessage": null,
    "createdAt": "2026-01-15T08:30:00Z",
    "lastAnalyzed": "2026-09-01T10:00:00Z"
  }
}
```

**Error Responses**:
- 404 Not Found: Repository doesn't exist
- 403 Forbidden: User lacks access

**Acceptance Criteria**:
- Return detailed repository metadata
- Include language detection results
- Show ingestion progress and status
- Include error message if analysis failed
- Verify user has access via project ownership

**Performance**: < 200ms

---

#### 3.5 GET `/api/repositories/{repositoryId}/files`
**Purpose**: List all files in repository

**Query Parameters**:
```
?page=0&size=100&ignored=false&language=java
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "files": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440003",
        "repositoryId": "550e8400-e29b-41d4-a716-446655440002",
        "filePath": "src/main/java/com/example/UserController.java",
        "fileName": "UserController.java",
        "language": "Java",
        "size": 2500,
        "lineCount": 85,
        "binary": false,
        "ignored": false
      }
    ],
    "pagination": {
      "page": 0,
      "size": 100,
      "totalElements": 150,
      "totalPages": 2
    }
  }
}
```

**Acceptance Criteria**:
- List all files in repository
- Filter by language, binary status, ignored status
- Support pagination
- Include file metadata (size, line count)
- Sort by name, size, or date modified
- Exclude .gitignore-ignored files by default

**Performance**: < 500ms for 1000 files

---

#### 3.6 GET `/api/repositories/{repositoryId}/files/{fileId}`
**Purpose**: Get file content

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "filePath": "src/main/java/com/example/UserController.java",
    "language": "Java",
    "size": 2500,
    "lineCount": 85,
    "content": "package com.example;\n...",
    "encoding": "UTF-8"
  }
}
```

**Error Responses**:
- 404 Not Found: File doesn't exist
- 403 Forbidden: User lacks access
- 413 Payload Too Large: File > 10MB

**Acceptance Criteria**:
- Return file content with metadata
- Max 10MB files (stream larger files)
- Detect and return file encoding
- Support byte range requests for large files
- Don't return binary files (error instead)

**Performance**: < 500ms for 1MB file, streaming for larger

---

#### 3.7 DELETE `/api/repositories/{repositoryId}`
**Purpose**: Delete a repository

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "message": "Repository deleted",
    "repositoryId": "550e8400-e29b-41d4-a716-446655440002"
  }
}
```

**Error Responses**:
- 404 Not Found: Repository doesn't exist
- 403 Forbidden: User not project owner

**Acceptance Criteria**:
- Soft delete repository
- Cascade delete all files
- Remove all vector embeddings from PGVector
- Create audit log entry
- Can recover within 30 days

**Performance**: < 500ms

**Security**:
- Only project owner can delete

---

### 4. Parser & Code Analysis APIs

#### 4.1 POST `/api/parser/repositories/{repositoryId}/parse`
**Purpose**: Parse all files in repository

**Response** (202 Accepted):
```json
{
  "success": true,
  "data": {
    "repositoryId": "550e8400-e29b-41d4-a716-446655440002",
    "filesQueued": 150,
    "estimatedDuration": "45s",
    "status": "QUEUED"
  }
}
```

**Acceptance Criteria**:
- Parse all non-binary files asynchronously
- Support Java, Python, JavaScript, TypeScript, Go, Rust, C#, C++, C, PHP, Ruby, Kotlin, Scala
- Use JavaParser for Java, regex-based for others
- Extract: classes, methods, functions, interfaces, imports
- Calculate complexity metrics
- Build dependency graph
- Timeout after 10 minutes per repository
- Store parsed metadata in database

**Performance**: < 1 minute for 1000 files

---

#### 4.2 GET `/api/parser/repositories/{repositoryId}/metrics`
**Purpose**: Get code quality metrics

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "repositoryId": "550e8400-e29b-41d4-a716-446655440002",
    "totalFiles": 150,
    "analyzedFiles": 145,
    "totalLines": 45320,
    "codeLines": 38200,
    "commentLines": 5120,
    "classCount": 42,
    "methodCount": 520,
    "averageCyclomaticComplexity": 3.2,
    "commentRatio": 0.113,
    "codeSmells": [
      "Long method detected in UserController.java:25",
      "High cyclomatic complexity in PaymentService.java:120"
    ],
    "languageBreakdown": {
      "Java": {"fileCount": 95, "totalLines": 38200},
      "SQL": {"fileCount": 30, "totalLines": 5120},
      "XML": {"fileCount": 20, "totalLines": 2000}
    }
  }
}
```

**Error Responses**:
- 404 Not Found: Repository doesn't exist
- 202 Accepted: Metrics not ready, try later
- 408 Request Timeout: Analysis taking too long

**Acceptance Criteria**:
- Calculate metrics for analyzed files
- Detect code smells (long methods, high complexity)
- Support incremental updates (don't recalculate all)
- Cache results (1 hour TTL)
- Return 202 if not yet calculated
- Handle large repos (10k+ files) with streaming/pagination

**Performance**: < 5s to return cached, < 3 min for fresh calculation on 10k files

---

#### 4.3 POST `/api/parser/repositories/{repositoryId}/dependency-graph`
**Purpose**: Generate dependency graph

**Query Parameters**:
```
?direction=incoming|outgoing|all
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "graph": {
      "nodes": [
        {"id": "com.example.UserController", "type": "class", "language": "Java"},
        {"id": "com.example.UserService", "type": "class", "language": "Java"}
      ],
      "edges": [
        {"source": "com.example.UserController", "target": "com.example.UserService", "type": "USES"}
      ]
    },
    "mermaid": "graph TB\n...",
    "nodeCount": 42,
    "edgeCount": 127,
    "circularDependencies": 2
  }
}
```

**Acceptance Criteria**:
- Generate directed dependency graph
- Show node types (class, interface, module, package)
- Show edge types (EXTENDS, IMPLEMENTS, USES, IMPORTS)
- Detect circular dependencies
- Generate Mermaid diagram representation
- Support direction filter (incoming, outgoing, all)
- Max 500 nodes per visualization (paginate if larger)
- Cache for 1 hour

**Performance**: < 2s for 500 nodes, < 10s for 5000 nodes

---

#### 4.4 GET `/api/parser/repositories/{repositoryId}/metrics` (repeated for clarity)
See section 4.2 above

---

#### 4.5 POST `/api/parser/repositories/{repositoryId}/uml`
**Purpose**: Generate UML class diagram

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "plantUml": "@startuml\n...",
    "mermaid": "classDiagram\n...",
    "classCount": 42,
    "relationshipCount": 127
  }
}
```

**Acceptance Criteria**:
- Generate UML class diagram
- Support PlantUML and Mermaid formats
- Show inheritance, interfaces, associations
- Limit to top 100 classes (most used)
- Include relationships (inheritance, composition)
- Cache for 1 hour

**Performance**: < 3s for 100 classes

---

#### 4.6 POST `/api/parser/repositories/{repositoryId}/architecture`
**Purpose**: Generate architecture diagram

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "plantUml": "@startuml\n...",
    "mermaid": "graph TB\n...",
    "layers": [
      {"name": "Presentation", "modules": ["Controller", "View"]},
      {"name": "Business Logic", "modules": ["Service", "Manager"]},
      {"name": "Data Access", "modules": ["Repository", "DAO"]},
      {"name": "Database", "modules": ["PostgreSQL"]}
    ],
    "patterns": ["MVC", "Service Layer", "Repository Pattern"]
  }
}
```

**Acceptance Criteria**:
- Detect architectural layers (if MVC, layered, etc.)
- Generate high-level component diagram
- Identify architectural patterns
- Show data flow between layers
- Support multiple output formats (PlantUML, Mermaid)
- Cache for 1 hour

**Performance**: < 5s

---

### 5. AI Engine APIs

#### 5.1 POST `/api/ai/ingest`
**Purpose**: Trigger repository ingestion into vector store

**Request Body**:
```json
{
  "repositoryId": "550e8400-e29b-41d4-a716-446655440002",
  "projectId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response** (202 Accepted):
```json
{
  "success": true,
  "data": {
    "repositoryId": "550e8400-e29b-41d4-a716-446655440002",
    "status": "INGESTING",
    "message": "Ingestion started. Check back later for completion."
  }
}
```

**Error Responses**:
- 404 Not Found: Repository doesn't exist
- 409 Conflict: Ingestion already in progress
- 503 Service Unavailable: AI service temporarily down

**Acceptance Criteria**:
- Start asynchronous ingestion task
- Return immediately (202 Accepted)
- Process files in batches
- Generate embeddings for code chunks
- Store in PGVector with project/repository isolation
- Update ingestion status in database
- Handle failures gracefully (retry up to 3 times)
- Max 1 ingestion per repository concurrently

**Performance**: Start < 1s, completion < 10 min for 10k files

---

#### 5.2 POST `/api/ai/chat`
**Purpose**: Repository-aware chatbot using RAG

**Request Body**:
```json
{
  "repositoryId": "550e8400-e29b-41d4-a716-446655440002",
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "question": "How does user authentication work?",
  "conversationId": "optional-uuid"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "conversationId": "550e8400-e29b-41d4-a716-446655440004",
    "answer": "User authentication is handled through...",
    "sources": [
      {"filePath": "src/auth/UserController.java", "startLine": 42, "endLine": 65},
      {"filePath": "src/auth/AuthService.java", "startLine": 10, "endLine": 30}
    ],
    "modelId": "ibm/granite-13b-chat-v2",
    "tokensUsed": 542
  }
}
```

**Error Responses**:
- 400 Bad Request: Question too short (< 5 chars)
- 404 Not Found: Repository not found
- 503 Service Unavailable: LLM provider error
- 504 Gateway Timeout: Response took > 2 minutes

**Acceptance Criteria**:
- **Natural Language Questions**: Accept ANY type of question about the imported project in natural language
  - Architecture & design patterns questions (e.g., "What's the overall architecture?" "Is this using MVC pattern?")
  - Implementation & code logic questions (e.g., "How does user authentication work?" "Explain the payment flow")
  - Debugging & troubleshooting questions (e.g., "Where are null pointer exceptions handled?" "Find database query issues")
  - Performance & optimization questions (e.g., "Which methods have high complexity?" "What operations are slow?")
  - Security questions (e.g., "Where is input validation done?" "How are passwords stored?")
  - Business logic questions (e.g., "What's the order processing workflow?" "How are discounts applied?")
  - Dependency & integration questions (e.g., "What libraries are used?" "Which external APIs are called?")
  - Testing & quality questions (e.g., "Are error cases handled?" "What's test coverage?")
- **Size-Independent**: Handle questions effectively regardless of application size (small 10-file repos to massive 10k+ file codebases)
- Retrieve relevant code chunks (top 5, adaptive based on question complexity)
- Send context to LLM with prompt template
- Stream response (optional, for long answers)
- Track conversation for follow-ups
- Extract source citations from context with file paths and line numbers
- Support multiple LLM providers (IBM, Groq, Gemini, Ollama)
- Fail gracefully if ingestion incomplete (suggest running ingestion)
- Max 10 conversations per repository (users can delete old ones to create new)
- Max 100 messages per conversation (users can start new conversation if needed)
- Adaptive context window: Use fewer chunks for small repos, more for large repos

**Performance**: First token < 5s, full response < 2 min (consistent across repo sizes)

**Question Examples** (all should work):
- "Explain the authentication flow" → Architecture question
- "Find where we handle user input validation" → Security question
- "What's the database schema for orders?" → Data structure question
- "Which methods have cyclomatic complexity > 10?" → Quality question
- "How does the payment module integrate with Stripe?" → Integration question
- "What error handling exists in the login service?" → Robustness question
- "Is there caching implemented? Where?" → Performance question
- "Show me all HTTP endpoints and their responsibilities" → API documentation question
- "Are there any circular dependencies?" → Dependency question
- "What's the deployment process?" → DevOps question

**Security**:
- Verify repository access via project ownership
- Don't expose raw embeddings in response
- Log conversation for audit
- Sanitize code snippets if they contain sensitive data (API keys, credentials)

---

#### 5.3 POST `/api/ai/search`
**Purpose**: Semantic search over repository

**Request Body**:
```json
{
  "repositoryId": "550e8400-e29b-41d4-a716-446655440002",
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "query": "JWT authentication",
  "topK": 10,
  "limit": 10
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "results": [
      {
        "filePath": "src/auth/UserController.java",
        "language": "Java",
        "content": "public class UserController {\n  @PostMapping(\"/login\")\n...",
        "startLine": 10,
        "endLine": 35,
        "score": 0.92,
        "symbolName": "UserController.login",
        "symbolType": "METHOD"
      }
    ],
    "totalResults": 23,
    "query": "JWT authentication"
  }
}
```

**Error Responses**:
- 400 Bad Request: Query empty or too short (< 3 chars)
- 404 Not Found: Repository not found
- 503 Service Unavailable: Search service down

**Acceptance Criteria**:
- Generate embedding for query using same model as ingestion
- Search PGVector for cosine-similar chunks
- Return top K results with similarity scores (0-1)
- Include file path, language, snippet, line numbers
- Support symbol name and type (if parser data available)
- Filter by language (optional query param)
- Timeout after 30s
- Return empty results if ingestion incomplete

**Performance**: < 3s for 50k+ lines

---

#### 5.4 POST `/api/ai/explain-code`
**Purpose**: AI-powered code explanation

**Request Body**:
```json
{
  "code": "public void saveUser(User user) { ... }",
  "language": "Java",
  "filePath": "optional/UserService.java"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "rawExplanation": "...",
    "summary": "Persists a user object to the database",
    "purpose": "Store user data for registration or profile updates",
    "keyComponents": [
      "User entity model",
      "Database persistence layer",
      "Validation checks"
    ],
    "logic": ["Validate user input", "Hash password", "Save to DB"],
    "dependencies": ["UserRepository", "PasswordEncoder"],
    "potentialIssues": ["No null check for user parameter"],
    "suggestions": ["Add @NotNull validation", "Consider async save"],
    "modelId": "ibm/granite-13b-chat-v2"
  }
}
```

**Error Responses**:
- 400 Bad Request: Code empty or > 10k lines
- 503 Service Unavailable: LLM provider error

**Acceptance Criteria**:
- Support 50+ programming languages
- Use prompt template for consistent output
- Extract structured sections from LLM response
- Provide raw explanation and parsed sections
- Max 10k lines of code per request
- Timeout after 30s
- Handle syntax errors gracefully

**Performance**: < 10s for code < 1000 lines

---

#### 5.5 POST `/api/ai/generate-readme`
**Purpose**: Auto-generate project README

**Request Body**:
```json
{
  "repositoryId": "550e8400-e29b-41d4-a716-446655440002",
  "projectId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "readme": "# My Project\n\n## Overview\n...",
    "format": "markdown",
    "generatedAt": "2026-09-01T10:30:00Z"
  }
}
```

**Acceptance Criteria**:
- Analyze repository structure and code
- Generate README with sections:
  - Title and description
  - Features
  - Installation instructions
  - Usage examples
  - Architecture overview
  - Contributing guidelines
  - License
- Use project metadata (name, description)
- Include detected technologies/languages
- Generate markdown format
- Support custom templates (optional)
- Timeout after 60s

**Performance**: < 20s for 10k files

---

#### 5.6 POST `/api/ai/generate-api-docs`
**Purpose**: Auto-generate API documentation

**Request Body**:
```json
{
  "repositoryId": "550e8400-e29b-41d4-a716-446655440002",
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "format": "markdown|openapi|swagger"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "docs": "# API Documentation\n\n## Endpoints\n...",
    "format": "markdown",
    "endpointCount": 15,
    "generatedAt": "2026-09-01T10:30:00Z"
  }
}
```

**Acceptance Criteria**:
- Detect REST endpoints (@GetMapping, @PostMapping, etc.)
- Extract path, method, parameters
- Generate documentation in multiple formats
- Include request/response examples
- Support OpenAPI/Swagger format (machine-readable)
- Generate curl examples
- Group by resource/controller
- Handle non-REST projects gracefully
- Timeout after 60s

**Performance**: < 20s for 10k files

---

### 6. Conversation Management APIs

#### 6.1 GET `/api/repositories/{repositoryId}/conversations`
**Purpose**: List conversation history

**Query Parameters**:
```
?projectId=xxx&repositoryId=yyy&page=0&size=20
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "conversations": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440004",
        "repositoryId": "550e8400-e29b-41d4-a716-446655440002",
        "title": "Authentication flow",
        "messageCount": 5,
        "createdAt": "2026-09-01T10:00:00Z",
        "updatedAt": "2026-09-01T10:30:00Z"
      }
    ],
    "pagination": {"page": 0, "size": 20, "totalElements": 12}
  }
}
```

**Acceptance Criteria**:
- List conversations for repository
- Show message count and last updated
- Sort by date (newest first)
- Pagination support
- Verify user has repository access

**Performance**: < 500ms for 100 conversations

---

#### 6.2 DELETE `/api/conversations/{conversationId}`
**Purpose**: Delete conversation

**Response** (200 OK):
```json
{
  "success": true,
  "data": {"message": "Conversation deleted"}
}
```

**Acceptance Criteria**:
- Soft delete conversation
- Preserve conversation history for 30 days
- Verify user has access

**Performance**: < 200ms

---

## Non-Functional Requirements

### Performance
| Endpoint | Target | Percentile |
|----------|--------|-----------|
| Auth (login) | 300ms | 95% |
| Project CRUD | 200ms | 95% |
| Metrics calculation | 3min | 95% (for 10k files) |
| Chat response | 5s (first token) | 50% |
| Search | 3s | 95% |
| File retrieval | 500ms | 95% |

### Scalability
- **Concurrent Users**: Support 1000 concurrent requests
- **Database**: PostgreSQL connection pool size 20-50
- **Async Tasks**: Ingestion queue max depth 100
- **Rate Limiting**:
  - Auth: 10 req/min per IP
  - API: 1000 req/min per user
  - Ingestion: 5 per hour per user
  - LLM: 100 requests/day per user (free tier)

### Reliability
- **Uptime**: 99.5% SLA
- **Error Handling**: Graceful fallbacks for LLM provider failures
- **Retry Logic**: 
  - Failed ingestion: retry 3 times with exponential backoff
  - Failed LLM call: immediate retry, then fallback response
- **Data Validation**: All inputs sanitized, type-checked
- **Backup**: Daily database backups retained 30 days

### Security
- **Authentication**: JWT with HS256 signing
- **HTTPS**: Enforced in production
- **Injection Prevention**: Parameterized queries, input sanitization
- **CORS**: Restrict to frontend domain
- **Rate Limiting**: Prevent brute force and abuse
- **Audit Logging**: All sensitive operations logged
- **Secret Management**: Environment variables, no hardcoding
- **File Scanning**: ClamAV for uploaded ZIP files
- **Access Control**: Project/repository ownership verification

### Monitoring & Observability
- **Logging**: Structured JSON logs (SLF4J)
- **Metrics**: Prometheus endpoints exposed
- **Health Checks**: /actuator/health, /actuator/prometheus
- **Tracing**: Optional OpenTelemetry integration
- **Alerting**: Configure alerts for:
  - 500 errors
  - LLM provider failures
  - Database connection pool exhaustion
  - Ingestion timeouts

---

## Technology Stack

### Core
- **Framework**: Spring Boot 3.x
- **Language**: Java 21
- **Build**: Maven 3.8+
- **Database**: PostgreSQL 16 + PGVector extension
- **Caching**: Redis (optional, for session/embedding cache)

### Dependencies
- **Security**: Spring Security 6, JWT (jjwt)
- **ORM**: Hibernate JPA
- **Schema Migration**: Flyway
- **Parsing**: JavaParser 3.x, Regex-based for others
- **AI/LLM**: 
  - IBM watsonx.ai (primary)
  - Groq (free)
  - Google Gemini (free)
  - Ollama (self-hosted)
- **Embeddings**: Same as LLM providers
- **Vector DB**: PGVector (PostgreSQL extension)
- **Graph**: JGraphT for dependency analysis
- **Monitoring**: Micrometer, Prometheus
- **Testing**: JUnit 5, Mockito, TestContainers

### DevOps
- **Containerization**: Docker
- **Orchestration**: Docker Compose
- **CI/CD**: GitHub Actions
- **Registry**: GitHub Container Registry (GHCR)

---

## Deployment & Operations

### Environment Variables
```env
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/codesense
DATABASE_USERNAME=codesense
DATABASE_PASSWORD=****

# JWT
JWT_SECRET=very-long-random-secret-min-64-chars
JWT_EXPIRATION_MS=86400000

# AI Provider (choose one)
AI_LLM_PROVIDER=gemini|groq|ibm|ollama|mock
AI_EMBEDDING_PROVIDER=gemini|groq|ibm|mock

# LLM API Keys
GEMINI_API_KEY=****
GROQ_API_KEY=****
IBM_WATSONX_API_KEY=****

# Server
SERVER_PORT=8080
MAX_UPLOAD_SIZE_MB=1000
```

### Database Schema
- Users, Projects, Repositories (relational)
- RepositoryFiles, RepositoryChunks (with vectors)
- Conversations, Messages
- Audit logs

### Health Checks
- GET `/actuator/health` — Application health
- GET `/api/ai/health` — LLM provider status
- GET `/actuator/prometheus` — Metrics

---

## Acceptance Criteria Checklist

- [ ] All endpoints documented in Swagger/OpenAPI
- [ ] Error responses consistent (400/401/403/404/500)
- [ ] JWT authentication enforced (except auth endpoints)
- [ ] Rate limiting implemented and tested
- [ ] Input validation on all endpoints
- [ ] Pagination implemented for list endpoints
- [ ] Async tasks properly handled (202 responses)
- [ ] Database transactions atomic
- [ ] Audit logging for sensitive operations
- [ ] CORS properly configured
- [ ] All secrets in environment variables
- [ ] Graceful degradation for LLM provider failures
- [ ] Integration tests for all major flows
- [ ] Load tested to 1000 concurrent users
- [ ] Security scan (OWASP Top 10)

---

## Roadmap (Future Phases)

### Phase 2 (Q1 2026)
- WebSocket support for real-time chat streaming
- GraphQL API endpoint
- Webhook support (GitHub, Slack)
- Batch operations (process multiple repos simultaneously)

### Phase 3 (Q2 2026)
- Code generation from requirements
- CI/CD integration (trigger analysis on push)
- Custom LLM fine-tuning for organization-specific patterns
- Programmatic API client SDKs (Python, JavaScript, Java)

### Phase 4 (Q3 2026)
- Multi-repository analysis (cross-repo dependencies)
- ML model for code quality scoring
- Security scanning integration (SAST, dependency analysis)
- Enterprise SSO (SAML, LDAP)

---

**Document Approval**:
- Backend Lead: ___________
- Product Manager: ___________
- DevOps Lead: ___________
- Date Approved: ___________
