# API Reference

## Authentication

All endpoints except `/api/auth/**` require JWT:
```
Authorization: Bearer <token>
```

---

## Auth APIs

### POST /api/auth/register
Register a new user.

**Request:**
```json
{ "name": "string", "email": "string", "password": "string (min 8)" }
```
**Response:** `AuthResponse { token, userId, name, email, role }`

---

### POST /api/auth/login
Login with credentials.

**Request:**
```json
{ "email": "string", "password": "string" }
```
**Response:** `AuthResponse { token, userId, name, email, role }`

---

### GET /api/auth/me
Get current user profile.

**Response:** `UserProfileDto { id, name, email, role, createdAt }`

---

## Project APIs

### POST /api/projects
Create a project.

**Request:** `{ "name": "string", "description": "string" }`  
**Response:** `ProjectDto { id, name, description, status, repositoryCount }`

---

### GET /api/projects
List all projects for current user.

---

### GET /api/projects/{projectId}
Get a project.

---

### DELETE /api/projects/{projectId}
Soft-delete a project.

---

## Repository APIs

### POST /api/projects/{projectId}/repositories/upload
Upload ZIP file.

**Content-Type:** `multipart/form-data`  
**Parts:** `file` (ZIP), `request` (JSON: `{ "name": "string" }`)

---

### POST /api/projects/{projectId}/repositories/github
Import from GitHub.

**Request:**
```json
{
  "githubUrl": "https://github.com/owner/repo",
  "name": "optional",
  "branch": "optional"
}
```

---

### GET /api/projects/{projectId}/repositories
List repositories.

---

### GET /api/repositories/{repositoryId}
Get repository details.

---

### GET /api/repositories/{repositoryId}/files
List repository files.

---

### GET /api/repositories/{repositoryId}/files/{fileId}
Get file content.

---

## AI APIs (Team Member 3 — Karthik)

### GET /api/ai/health
Check AI engine status.

**Response:**
```json
{ "status": "UP", "llmProvider": "IBM watsonx.ai (...)", "llmAvailable": true }
```

---

### POST /api/ai/ingest
Trigger repository ingestion into vector store.

**Request:** `{ "projectId": "uuid", "repositoryId": "uuid" }`

---

### POST /api/ai/chat
Repository-aware AI chat (RAG-powered).

**Request:**
```json
{
  "projectId": "uuid",
  "repositoryId": "uuid",
  "conversationId": "uuid (optional)",
  "question": "How does authentication work?"
}
```

**Response:**
```json
{
  "conversationId": "uuid",
  "answer": "Authentication is implemented using JWT...",
  "sources": [
    { "filePath": "AuthService.java", "startLine": 25, "endLine": 60 }
  ],
  "modelId": "ibm/granite-13b-chat-v2"
}
```

---

### POST /api/ai/search
Semantic search.

**Request:**
```json
{ "projectId": "uuid", "repositoryId": "uuid", "query": "JWT authentication" }
```

**Response:** `{ "results": [...], "totalResults": 5, "query": "..." }`

---

### POST /api/ai/explain-code
Code explanation.

**Request:**
```json
{
  "projectId": "uuid",
  "repositoryId": "uuid",
  "filePath": "AuthService.java",
  "language": "Java",
  "code": "public class AuthService { ... }"
}
```

**Response:**
```json
{
  "summary": "...",
  "purpose": "...",
  "keyComponents": [...],
  "logic": [...],
  "potentialIssues": [...],
  "suggestions": [...]
}
```

---

### POST /api/ai/generate-readme
Generate README documentation.

**Request:** `{ "projectId": "uuid", "repositoryId": "uuid" }`  
**Response:** `{ "documentationId": "uuid", "content": "# README...", "format": "MARKDOWN" }`

---

### POST /api/ai/generate-api-docs
Generate API documentation.

**Request:** `{ "projectId": "uuid", "repositoryId": "uuid", "language": "Java" }`  
**Response:** `{ "documentationId": "uuid", "content": "# API Docs...", "format": "MARKDOWN" }`

---

## Error Response Format

```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/projects",
  "fieldErrors": [
    { "field": "name", "message": "Project name is required" }
  ]
}
```

---

## Swagger UI

Available at: `http://localhost:8080/swagger-ui.html`
