# Deployment Guide — CodeSense AI

## Overview

This document covers deploying CodeSense AI to a production environment.

## Prerequisites

- Docker 24+ and Docker Compose v2
- PostgreSQL 15+ with PGVector extension (or use the bundled Docker service)
- 4 GB RAM minimum (8 GB recommended)
- IBM Cloud account with watsonx.ai access (optional — mock mode works for development)

---

## Environment Variables

Copy `.env.example` to `.env` and fill in all required values:

```bash
cp .env.example .env
```

### Required Variables

| Variable | Description |
|---|---|
| `POSTGRES_PASSWORD` | Strong PostgreSQL password |
| `JWT_SECRET` | Random 64+ char secret: `openssl rand -base64 64` |

### IBM watsonx.ai (for real AI)

| Variable | Description |
|---|---|
| `IBM_WATSONX_URL` | `https://us-south.ml.cloud.ibm.com` |
| `IBM_WATSONX_API_KEY` | IBM Cloud API key |
| `IBM_WATSONX_PROJECT_ID` | watsonx.ai project ID |
| `IBM_WATSONX_MODEL_ID` | e.g. `ibm/granite-13b-chat-v2` |
| `AI_LLM_PROVIDER` | `watsonx` (or `mock` for local dev) |
| `AI_EMBEDDING_PROVIDER` | `watsonx` (or `mock` for local dev) |

---

## Docker Deployment

### 1. Build and Start All Services

```bash
# From the codesense-ai/ directory:
docker compose up -d
```

This starts:
- `postgres` — PostgreSQL 16 with PGVector
- `backend` — Spring Boot on port 8080
- `frontend` — React/Nginx on port 3000
- `prometheus` — Metrics on port 9090
- `grafana` — Dashboard on port 3001

### 2. Verify Health

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:3000
```

### 3. View Logs

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

---

## Database Setup

The PostgreSQL extensions (PGVector, uuid-ossp) are installed automatically on first startup via `docker/init-extensions.sh`.

Flyway migrations run automatically on backend startup:
- `V1__initial_schema.sql` — all tables, indexes, PGVector index

---

## Build Commands

```bash
# Backend only
cd backend
./mvnw package -DskipTests

# Frontend only
cd frontend
npm install && npm run build

# All via Docker
docker compose build
```

---

## Health Checks

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Spring Boot health |
| `GET /api/ai/health` | AI Engine status |
| `GET /actuator/prometheus` | Metrics for Prometheus |

---

## Monitoring

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001` (default admin password from `GRAFANA_PASSWORD`)

---

## Rollback

1. Stop services: `docker compose down`
2. Restore database from backup
3. Deploy previous image version
4. Start: `docker compose up -d`

---

## Known Limitations

- PGVector `ivfflat` index requires at least 100 vectors before it's useful — use exact search for small repositories
- IBM watsonx.ai requires internet access and valid credentials
- ZIP upload limited to `MAX_UPLOAD_SIZE_MB` (default 100MB)
- GitHub integration uses public API rate limits unless `GITHUB_ACCESS_TOKEN` is set
