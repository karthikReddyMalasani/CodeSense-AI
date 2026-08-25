# Monitoring Architecture — CodeSense AI

## Stack

| Component | Purpose | Port |
|---|---|---|
| Spring Boot Actuator | Expose health, info, metrics endpoints | `8080/actuator/` |
| Micrometer | Metrics instrumentation library | (embedded) |
| Prometheus | Time-series metrics scraping | `9090` |
| Grafana | Metrics visualization dashboards | `3001` |

---

## Actuator Endpoints

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Application health (UP/DOWN) |
| `GET /actuator/info` | App version and metadata |
| `GET /actuator/metrics` | All metrics names |
| `GET /actuator/prometheus` | Prometheus-formatted metrics |

Configured in `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

## Key Metrics

| Metric | Description |
|---|---|
| `http_server_requests_seconds` | HTTP request latency by endpoint |
| `jvm_memory_used_bytes` | JVM heap/non-heap usage |
| `hikaricp_connections_active` | DB connection pool |
| `spring_data_repository_invocations` | JPA repository calls |
| `process_cpu_usage` | CPU utilization |

---

## Docker Compose Setup

```bash
# Start all monitoring services
docker compose up -d prometheus grafana

# Access Grafana
open http://localhost:3001
# Default: admin / (set GRAFANA_PASSWORD in .env)
```

---

## Prometheus Configuration

`docker/prometheus.yml` scrapes:
- `backend:8080/actuator/prometheus` every 15 seconds

---

## Logging

Structured logging via SLF4J + Logback:

```yaml
logging:
  level:
    com.codesense: DEBUG
    org.springframework.security: WARN
```

**Never logged:** passwords, JWT tokens, API keys, IBM credentials

---

## Health Check URLs

| Service | URL |
|---|---|
| Backend | `http://localhost:8080/actuator/health` |
| AI Engine | `http://localhost:8080/api/ai/health` |
| Frontend | `http://localhost:3000` |
| Prometheus | `http://localhost:9090/-/healthy` |
| Grafana | `http://localhost:3001/api/health` |
