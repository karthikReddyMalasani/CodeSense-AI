# Setup Guide

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21+ | JDK required |
| Maven | 3.8+ | Or use `./mvnw` wrapper |
| Node.js | 18+ | For frontend |
| npm | 8+ | For frontend |
| PostgreSQL | 15+ | With PGVector extension |
| Git | any | For GitHub clone feature |

---

## PostgreSQL Setup

```bash
# Install PostgreSQL and PGVector
# Ubuntu/Debian:
apt-get install postgresql postgresql-contrib
apt-get install postgresql-16-pgvector

# macOS (Homebrew):
brew install postgresql@16
brew install pgvector

# Create database
psql -U postgres
CREATE USER codesense WITH PASSWORD 'your_password';
CREATE DATABASE codesense OWNER codesense;
GRANT ALL PRIVILEGES ON DATABASE codesense TO codesense;

# Enable PGVector extension
psql -U codesense -d codesense
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

---

## Backend Setup

```bash
cd codesense-ai/backend

# Copy and configure environment
cp ../.env.example ../.env
# Edit .env with your values (DATABASE_URL, JWT_SECRET, etc.)

# Run database migrations
./mvnw flyway:migrate

# Start the backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend starts at: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Frontend Setup

```bash
cd codesense-ai/frontend

# Install dependencies
npm install

# Copy and configure environment
cp .env.example .env
# REACT_APP_API_BASE_URL=http://localhost:8080

# Start the frontend
npm start
```

The frontend starts at: `http://localhost:3000`

---

## Running with IBM watsonx.ai

1. Create an IBM Cloud account at https://cloud.ibm.com
2. Create a Watson Machine Learning service instance
3. Create a project in IBM Watson Studio
4. Generate an API key in IAM settings
5. Configure in `.env`:

```env
IBM_WATSONX_URL=https://us-south.ml.cloud.ibm.com
IBM_WATSONX_API_KEY=your_api_key
IBM_WATSONX_PROJECT_ID=your_project_id
IBM_WATSONX_MODEL_ID=ibm/granite-13b-chat-v2
AI_LLM_PROVIDER=watsonx
AI_EMBEDDING_PROVIDER=watsonx
```

---

## Running in Mock Mode (No IBM Credentials)

The application runs fully in mock mode by default:

```env
AI_LLM_PROVIDER=mock
AI_EMBEDDING_PROVIDER=mock
```

Mock mode provides simulated responses for all AI features.
This is ideal for local development and UI testing.

---

## Testing

```bash
cd codesense-ai/backend

# Run all unit tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=MockLLMServiceTest

# Run with coverage
./mvnw test jacoco:report
```

---

## Common Issues

### PGVector extension not found
```sql
-- Run as superuser
CREATE EXTENSION IF NOT EXISTS vector;
```

### Port 8080 already in use
```bash
# Find and kill the process
lsof -i :8080
kill -9 <PID>
```

### Maven wrapper not executable (Linux/Mac)
```bash
chmod +x backend/mvnw
```

### Frontend proxy errors
Ensure backend is running on port 8080. Check `frontend/package.json` proxy setting.

---

## Team Member 5 Note

This guide covers local development only.  
For production deployment, Docker, CI/CD, and monitoring configuration,  
see Team Member 5 (Vishnu)'s DevOps documentation.
