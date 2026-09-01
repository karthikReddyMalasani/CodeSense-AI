# Chat Feature Operations Runbook
**CodeSense AI - Chat Support & Monitoring**  
**Date**: 2026-09-01  
**Target Audience**: DevOps, On-Call Engineers, Support Team

---

## Quick Reference

### Health Check Commands

```bash
# 1. Check backend is running
curl -s http://localhost:8080/api/health | jq '.status'

# 2. Check chat endpoint availability
curl -s http://localhost:8080/api/ai/chat -X OPTIONS

# 3. Check database connectivity
psql -U codesense -d codesense_ai -c "SELECT COUNT(*) FROM repository_chunks;" 

# 4. Check vector index status
psql -U codesense -d codesense_ai -c "SELECT COUNT(*) FROM vector_embeddings;"

# 5. Check LLM connectivity (example for Gemini)
curl -s "https://generativelanguage.googleapis.com/v1beta/models?key=$GEMINI_API_KEY"
```

### Emergency Stop & Restart

```bash
# Stop chat service (without affecting other services)
docker-compose -f docker-compose.prod.yml stop backend

# Restart chat service
docker-compose -f docker-compose.prod.yml up -d backend

# Full stack restart (if needed)
docker-compose -f docker-compose.prod.yml restart

# Monitor logs
docker-compose -f docker-compose.prod.yml logs -f backend | grep -E "chat|RAG"
```

---

## Common Incidents & Resolutions

### Incident 1: "Repository Indexing Not Ready" Error

**Symptom**: Users see "Repository indexing is not ready" when asking questions

**Root Cause**: 
- Ingestion not started
- Ingestion still in progress (INGESTING status)
- Ingestion failed (ERROR status)

**Resolution**:

```bash
# Check repo ingestion status
curl -s http://localhost:8080/api/projects/{projectId}/repositories/{repoId} \
  | jq '.ingestionStatus'

# If PENDING: User needs to click "Ingest AI" button in UI
# If INGESTING: Tell user to wait 5-30 minutes depending on repo size
# If ERROR: Check error log
```

**Prevention**: Add pre-chat validation to show ingestion button if not ready

---

### Incident 2: Chat Timeout (> 120 seconds)

**Symptom**: Chat returns "Request timed out" errors consistently

**Root Causes & Fixes**:

```bash
# Check 1: Is LLM endpoint responding?
curl -s $LLM_ENDPOINT/health
# If failed: Check API key, network connectivity

# Check 2: Is vector search slow?
grep "search completed" logs/backend.log | tail -20
# If > 1000ms: Check database indexes
ANALYZE repository_chunks;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_repo_chunks_repo_id 
  ON repository_chunks(repository_id);

# Check 3: Is LLM service overloaded?
grep "LLM generation completed" logs/backend.log | tail -20
# Extract timing: awk '{print $NF}' to see latency
# If > 30s consistently: Contact LLM provider or reduce model complexity

# Check 4: Is backend resource-constrained?
docker stats backend
# If > 90% memory/CPU: Increase resource limits or optimize queries
```

---

### Incident 3: Vector Search Returns No Results

**Symptom**: Chat answers lack specifics, logging shows "0 chunks found"

**Root Cause**: Vector embeddings not generated or similarity threshold too high

**Resolution**:

```bash
# Check vector embeddings exist
SELECT COUNT(*) FROM vector_embeddings;
# If 0: Ingestion failed, re-run ingestion

# Check embedding quality
SELECT file_path, embedding_model FROM vector_embeddings LIMIT 5;

# Check similarity search manually
SELECT * FROM vector_embeddings 
WHERE similarity(embedding, 'question_as_vector') > 0.7
LIMIT 5;
# If 0 results: Question too different from code, try rephrasing

# Force re-embedding of a repository
DELETE FROM vector_embeddings WHERE repository_id = 'xxx';
-- Then re-run ingestion via UI
```

---

### Incident 4: LLM API Errors

**Symptom**: Chat returns "LLM service error" or generic error messages

**By Provider**:

```bash
# GEMINI
# Error: 429 Too Many Requests
# Fix: Implement rate limiting on frontend, use caching

# GROQ  
# Error: Authentication failed
# Fix: Verify GROQ_API_KEY in environment
echo $GROQ_API_KEY

# IBM GRANITE
# Error: Model not found
# Fix: Check model ID in config, verify account access

# OLLAMA
# Error: Connection refused
# Fix: Verify Ollama running on localhost:11434
curl -s http://localhost:11434/api/tags
```

---

### Incident 5: High Memory Usage After Chat Requests

**Symptom**: Backend memory increases after many chat requests, not released

**Root Cause**: Vector search results or conversation history not garbage collected

**Resolution**:

```bash
# Check memory usage
jstat -gc -h20 $(pgrep -f "java.*backend") 1000

# If sustained high: Possible memory leak
# Restart backend gracefully
docker-compose stop backend
sleep 2
docker-compose up -d backend

# Monitor memory after restart
watch 'docker stats backend --no-stream'
```

---

## Monitoring Setup

### Prometheus Metrics (Add to prometheus.yml)

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'backend'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

### Key Metrics to Track

```
# Chat success rate
rate(chat_requests_total{status="success"}[5m])

# Chat latency P95
histogram_quantile(0.95, chat_request_duration_seconds)

# Vector search latency
histogram_quantile(0.95, vector_search_duration_seconds)

# LLM generation latency  
histogram_quantile(0.95, llm_generation_duration_seconds)

# Ingestion status distribution
count(repository_ingestion_status)

# Error rate by type
rate(chat_errors_total[5m])
```

### Grafana Dashboard JSON

Add dashboard with panels:
1. Chat Success Rate (time series)
2. Chat Latency P50/P95/P99 (heatmap)
3. Vector Search Performance (bar chart)
4. LLM Provider Latency (box plot)
5. Error Count by Type (pie chart)
6. Backend Resource Usage (gauges)

---

## Performance Baselines

Reference values for healthy chat service:

| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| Vector Search | < 500ms | > 1s | > 3s |
| LLM Generation | < 15s | > 30s | > 60s |
| Total Response | < 30s | > 60s | > 120s |
| Chat Success Rate | > 99% | < 95% | < 90% |
| Error Rate | < 0.1% | > 1% | > 5% |
| Backend Memory | < 1GB | > 1.5GB | > 2GB |
| Database Connections | < 10 | > 20 | > 40 |

---

## Scheduled Maintenance

### Daily
- [ ] Monitor chat success rate (> 99%)
- [ ] Check error logs for patterns
- [ ] Verify LLM API health

### Weekly
- [ ] Run database VACUUM and ANALYZE
- [ ] Check vector index fragmentation
- [ ] Review slow query logs
- [ ] Verify backup completeness

### Monthly
- [ ] Performance review (compare baselines)
- [ ] Update LLM model versions if needed
- [ ] Review and archive old logs
- [ ] Security audit of API access patterns

---

## Escalation Procedures

### Level 1: Self-Service (User)
**Issue**: "Chat not working"  
**Troubleshooting**:
- [ ] Is repository ingested? (Check UI status)
- [ ] Is question 3-5000 characters? (Check validation)
- [ ] Try refreshing browser
- [ ] Try different, simpler question

### Level 2: Support Team (< 30 min response)
**Tools**: Backend logs, Docker health, basic DB queries  
**Process**:
1. Reproduce issue
2. Check backend logs: `grep ERROR logs/backend.log`
3. Check repo ingestion status in DB
4. Verify LLM provider API key

### Level 3: Development Team (< 1 hour response)
**Tools**: Full code access, performance profiling  
**Process**:
1. Code review of recent changes
2. Database query optimization
3. LLM prompt engineering
4. Post-mortem analysis

### Level 4: Emergency Response (Immediate)
**Trigger**: > 50% users affected  
**Actions**:
1. Disable chat feature (comment out ChatPage import)
2. Restart backend service
3. Verify database/LLM connectivity
4. Escalate to management

---

## Rollback Procedure

If chat improvements cause issues:

```bash
# Find previous stable commit
git log --oneline | head -20

# Rollback to previous version
git revert HEAD

# Rebuild backend
mvn clean package -DskipTests

# Rebuild frontend
npm run build

# Restart services
docker-compose restart backend frontend

# Verify chat works
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"projectId": "test", ...}'
```

---

## Contact & Escalation

**Chat Feature Owner**: Karthik Reddy Malasani (AI Lead)  
**On-Call Schedule**: Check Pagerduty (codesense-ai-oncall)  
**Slack Channels**:
- #codesense-incidents (critical issues)
- #codesense-chat-feature (feature discussion)
- #codesense-support (user issues)

**External Escalation**:
- **Gemini**: Contact Google Cloud support (gcp-support@google.com)
- **Groq**: Contact Groq support (support@groq.com)
- **IBM Granite**: Contact IBM support (ibm-support@ibm.com)

---

## Appendix: Log Examples

### Successful Chat Request
```
2026-09-01 10:15:32 INFO  RagService - RAG chat initiated: project=proj-001, repo=repo-001, questionLen=42
2026-09-01 10:15:32 DEBUG RagService - Starting vector search: topK=5
2026-09-01 10:15:32 DEBUG RagService - Vector search returned 5 chunks in 245ms
2026-09-01 10:15:32 DEBUG RagService - Invoking LLM with context
2026-09-01 10:15:35 DEBUG RagService - LLM generation completed in 3250ms: tokens=542
2026-09-01 10:15:35 INFO  RagService - RAG chat complete: conversationId=conv-123, sources=3, totalDuration=3510ms
```

### Failed Chat Request (Retry Succeeds)
```
2026-09-01 10:16:00 INFO  RagService - RAG chat initiated: project=proj-001, repo=repo-001
2026-09-01 10:16:00 ERROR VectorSearchService - Vector search failed: Connection timeout
2026-09-01 10:16:00 INFO  RagService - Vector search failed, proceeding without context
2026-09-01 10:16:05 INFO  RagService - RAG chat complete: conversationId=conv-124, sources=0, totalDuration=5120ms
2026-09-01 10:16:05 DEBUG RagService - Chat completed with fallback (no vector context)
```

---

*Last Updated: 2026-09-01*  
*Next Review: 2026-10-01*
