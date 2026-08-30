# CodeSense AI

An AI-powered multi-language code intelligence and documentation platform.

## Overview

CodeSense AI lets you upload repositories, analyze source code across 13 programming languages, and use **Google Gemini** for intelligent code understanding, RAG-based Q&A, documentation generation, and semantic search.

## Team Structure

| Member | Role | Ownership |
|--------|------|-----------|
| Reddy Prasanna | Frontend Developer / UI-UX | React Frontend |
| Vishwa | Backend Developer / Spring Boot | Backend APIs & Repository Management |
| **Karthik (Bob)** | **AI Engineer** | **AI Engine — RAG, Embeddings, LLM** |
| Prashanthi | Code Intelligence Engineer | Parser / Analyzer |
| Vishnu | DevOps & QA | Testing / CI-CD / Docker |

## Features

- 🔐 JWT-based authentication
- 📁 Project & repository management
- 📦 ZIP repository upload + GitHub import
- 🤖 AI chatbot per repository (RAG pipeline)
- 🔍 Semantic search with PGVector
- 💡 Code explanation, README & API docs generation
- 🌐 13 languages: Java, Python, JS, TS, C, C++, C#, Go, Rust, PHP, Ruby, Kotlin, Swift
- 📊 Code metrics, dependency graphs, UML/architecture diagrams

## Quick Start (5 minutes)

### Prerequisites

- Java 23 (or 21+)
- Node.js 18+
- Maven at `C:\Users\Karth\apache-maven-3.9.9\bin\mvn.cmd` (or on PATH)

### Step 1 — Database (pick one)

**Option A — Neon.tech (free cloud, recommended):**
1. Sign up at https://neon.tech (GitHub login, no credit card)
2. Create a project, copy the connection string
3. In the SQL editor run: `CREATE EXTENSION IF NOT EXISTS vector;`

**Option B — Local Docker:**
```bash
docker compose -f docker-compose.dev.yml up -d
```

### Step 2 — Get a Gemini API key

1. Go to https://aistudio.google.com/apikey
2. Create an API key for the Gemini API
3. Copy the key securely

### Step 3 — Configure environment

```powershell
Copy-Item .env.example .env
# Open .env and set:
#   GEMINI_API_KEY=your_gemini_key_here
#   DATABASE_URL=jdbc:postgresql://...  (Neon URL or localhost)
#   DATABASE_USERNAME=...
#   DATABASE_PASSWORD=...
#   JWT_SECRET=<run: [Convert]::ToBase64String((1..64|%{[byte](Get-Random -Max 256)}))>
```

### Step 4 — Start the backend

```powershell
cd backend
& "C:\Users\Karth\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
```

Backend: http://localhost:8080  
Swagger: http://localhost:8080/swagger-ui.html

### Step 5 — Start the frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend: http://localhost:3000

### Step 6 — Try the app

1. Register → Login → Create Project
2. Upload a ZIP or import a GitHub repo
3. Go to **AI Chat** → ask *"How does authentication work?"*
4. Try **Code Explanation**, **README Generator**, **Search**

---

## AI Provider

| Variable | Value | Description |
|----------|-------|-------------|
| `AI_LLM_PROVIDER` | `gemini` | Real AI answers via Google Gemini |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Gemini model used for generation |
| `AI_EMBEDDING_PROVIDER` | `gemini` | Gemini embeddings for semantic search |
| `AI_LLM_PROVIDER` | `gemini` | Gemini responses |

> Gemini API quotas and billing depend on your Google AI Studio project.

---

## Running Tests

```powershell
cd backend
& "C:\Users\Karth\apache-maven-3.9.9\bin\mvn.cmd" test
# Expected: Tests run: 97, Failures: 0, Errors: 0
```

---

## Docker (full stack)

```bash
docker compose up --build
# Frontend: http://localhost:3000
# Backend:  http://localhost:8080
# Prometheus: http://localhost:9090
# Grafana:    http://localhost:3001
```

---

## Project Structure

```
codesense-ai/
├── backend/          # Spring Boot — TM2 (auth, project, repo) + TM3 (AI)
│   └── src/main/java/com/codesense/
│       ├── ai/       # RAG, LLM (Gemini/Mock), embeddings, PGVector
│       ├── auth/     # JWT, Spring Security
│       ├── parser/   # JavaParser, regex multi-lang, JGraphT, PlantUML
│       ├── project/  # Project CRUD
│       └── repository/ # ZIP upload, GitHub clone, file indexing
├── frontend/         # React 18 + Vite — 14 pages
├── docs/             # Architecture, API, RAG, parser, deployment docs
├── test-data/        # Sample repos for testing
├── docker/           # Postgres init, Prometheus, Grafana configs
├── .github/          # CI/CD pipeline
├── .env.example      # Environment variable template
└── docker-compose.yml
```

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `DATABASE_URL` | ✅ | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | ✅ | DB username |
| `DATABASE_PASSWORD` | ✅ | DB password |
| `JWT_SECRET` | ✅ | 64+ char base64 secret |
| `GEMINI_API_KEY` | ✅ (for AI) | From Google AI Studio |
| `GEMINI_MODEL` | optional | default: `gemini-2.5-flash` |
| `GEMINI_EMBEDDING_MODEL` | optional | default: `gemini-embedding-001` |
| `EMBEDDING_DIMENSION` | optional | default: `768` |
| `GITHUB_ACCESS_TOKEN` | optional | For private GitHub repos |
| `UPLOAD_DIR` | optional | default: `./uploads` |
| `MAX_UPLOAD_SIZE_MB` | optional | default: `100` |

---

## Documentation

| Doc | Description |
|-----|-------------|
| [docs/architecture.md](docs/architecture.md) | System architecture |
| [docs/api.md](docs/api.md) | REST API reference |
| [docs/ai-architecture.md](docs/ai-architecture.md) | AI engine design |
| [docs/rag-pipeline.md](docs/rag-pipeline.md) | RAG pipeline |
| [docs/parser.md](docs/parser.md) | Parser module |
| [docs/tree-sitter.md](docs/tree-sitter.md) | Multi-language parsing |
| [docs/database.md](docs/database.md) | Database schema |
| [docs/deployment.md](docs/deployment.md) | Deployment guide |
| [docs/testing.md](docs/testing.md) | Testing strategy |
| [docs/monitoring.md](docs/monitoring.md) | Monitoring setup |
| [AGENTS.md](AGENTS.md) | AI project context (IBM Bob) |
