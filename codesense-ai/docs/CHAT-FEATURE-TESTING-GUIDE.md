# Chat Feature Testing & Usage Guide
**CodeSense AI - Chat Component**  
**Date**: 2026-09-01  
**Version**: 1.0

---

## Quick Start

### Prerequisites
1. ✅ Backend running on `http://localhost:8080`
2. ✅ Frontend running on `http://localhost:3000`
3. ✅ PostgreSQL + PGVector running
4. ✅ LLM provider configured (Gemini, Groq, IBM, or Ollama)

### Basic Flow
1. **Create Project** → Upload/Import Repository → **Ingest AI** → **Ask Questions** → Get Answers

---

## Feature Requirements Checklist

### ✅ Backend Requirements

| Requirement | Status | Details |
|---|---|---|
| Accept ANY question type | ✅ | Architecture, security, performance, business logic, etc. |
| Size-independent performance | ✅ | Works for 10-file to 10k+ file repos |
| Input validation (3-5000 chars) | ✅ | ChatRequestDto enforces @Size constraints |
| Retry mechanism | ✅ | Frontend retries up to 2 times on failure |
| Timeout handling (120s) | ✅ | Frontend timeout + backend processing time |
| Error handling | ✅ | Graceful fallback if search or LLM fails |
| Source citations | ✅ | Returns file paths + line numbers |
| Conversation history | ✅ | Maintains multi-turn context |
| Rate limiting | ✅ | Max 1000 requests/min per user |

### ✅ Frontend Requirements

| Requirement | Status | Details |
|---|---|---|
| Accept any natural language question | ✅ | No limitations on question content |
| Input validation | ✅ | Shows error if < 3 or > 5000 chars |
| Retry on timeout | ✅ | Auto-retries 2x with user feedback |
| Error messages | ✅ | Context-aware suggestions |
| Source display | ✅ | Shows files and line numbers |
| Conversation history | ✅ | Full history with previous messages |
| Ingestion status | ✅ | Shows real-time indexing status |
| Markdown rendering | ✅ | Bold, code blocks, lists, etc. |

---

## Testing Scenarios

### 1. Basic Chat Flow

**Test Case 1.1**: Simple Architecture Question
```
Repository: Java Spring Boot project
Question: "Explain the system architecture"
Expected: 
- Response within 5-30 seconds
- Shows controller, service, repository layers
- Cites source files (e.g., UserController.java:10-35)
- Markdown formatted with sections
```

**Test Case 1.2**: Security Question
```
Repository: Node.js/Express app
Question: "How are passwords stored and validated?"
Expected:
- Finds authentication code
- Shows bcrypt/hashing strategy
- Cites relevant files from auth module
- Explains security approach
```

**Test Case 1.3**: Performance Question
```
Repository: Python Django app
Question: "Which database queries are performance bottlenecks?"
Expected:
- Identifies slow queries
- Shows N+1 problem if exists
- Suggests optimization
- Cites ORM usage patterns
```

---

### 2. Error Handling

**Test Case 2.1**: Repository Not Ingested
```
Setup: Uploaded repo without running "Ingest AI"
Action: Ask question
Expected:
- Message: "Repository indexing is not ready"
- Shows current status (PENDING, INGESTING, etc.)
- Offers "Ingest AI" button
```

**Test Case 2.2**: Invalid Input - Too Short
```
Input: "hi"
Expected:
- Frontend error: "Please ask a longer question (at least 3 characters)"
- User can retry
```

**Test Case 2.3**: Invalid Input - Too Long
```
Input: 5001+ characters
Expected:
- Frontend error: "Question is too long (max 5000 characters)"
- User can clear and retry
```

**Test Case 2.4**: LLM Provider Failure
```
Setup: LLM API key invalid/expired
Action: Ask question
Expected:
- Response shows fallback message
- "AI service is temporarily unavailable"
- Suggests retrying
- No crash, graceful degradation
```

**Test Case 2.5**: Vector Search Failure
```
Setup: Vector database temporarily down
Action: Ask question
Expected:
- Continues without context
- LLM generates answer (may indicate insufficient context)
- Log shows "Vector search failed, proceeding without context"
- User gets helpful response
```

---

### 3. Performance Testing

**Test Case 3.1**: Small Repository (10 files)
```
Repository: Flask app with 10 files, ~1000 LOC
Expected:
- First token: < 2 seconds
- Full response: < 5 seconds
- Instant search completion (< 100ms)
```

**Test Case 3.2**: Medium Repository (100 files)
```
Repository: Spring Boot microservice, ~20k LOC
Expected:
- First token: < 3-5 seconds
- Full response: < 15 seconds
- Search: < 500ms
```

**Test Case 3.3**: Large Repository (1000+ files)
```
Repository: Enterprise monolith, 100k+ LOC
Expected:
- First token: < 5 seconds (still responsive)
- Full response: < 30-60 seconds
- Search: < 1-2 seconds
- NO timeouts (adaptive context window)
```

---

### 4. Conversation Continuity

**Test Case 4.1**: Multi-turn Conversation
```
Q1: "Explain authentication flow"
Q2: "Where are tokens validated?"
Q3: "How long do tokens expire?"

Expected:
- Each question references previous context
- System maintains conversation history
- Related answers build on each other
- Conversation ID persists across turns
```

**Test Case 4.2**: New Conversation
```
Action: Click "New Chat" button
Expected:
- Messages cleared
- Conversation ID reset to null
- Input field ready for new question
- Suggested questions appear
```

**Test Case 4.3**: Repository Switch
```
Action: Switch to different repository
Expected:
- Previous conversation cleared
- New conversation starts for new repo
- Ingestion status checked for new repo
- Suggested questions update context
```

---

### 5. Edge Cases

**Test Case 5.1**: Empty Question
```
Input: Whitespace only
Expected:
- Send button disabled (grayed out)
- No API call made
```

**Test Case 5.2**: Question with Special Characters
```
Question: "What's @Component & @Service in Spring?"
Expected:
- Handles special chars correctly
- Proper markdown escaping
- No injection issues
```

**Test Case 5.3**: Very Long Response
```
Question: "List all classes and their methods"
Expected:
- Response streams in chunks
- "Thinking..." indicator shows progress
- No timeout (even if > 30s)
- Can be scrolled and copied
```

**Test Case 5.4**: Simultaneous Questions
```
Action: Rapidly send 2-3 questions
Expected:
- Queue system prevents overlap
- Loading state prevents duplicate submissions
- Responses returned in order
```

---

### 6. Retry Mechanism Testing

**Test Case 6.1**: Network Failure (First Attempt)
```
Setup: Simulate network error on first call
Expected:
- Message: "Request timed out. Retrying..."
- 1-2 second delay
- Retry succeeds
- Answer returned normally
```

**Test Case 6.2**: Timeout Recovery
```
Setup: LLM takes 180+ seconds (timeout at 120s)
Expected:
- After 120s: "Request took too long..."
- Automatic retry
- If still fails: "Try a simpler question"
- No permanent hang
```

**Test Case 6.3**: Max Retries Exceeded
```
Setup: LLM consistently errors
Expected:
- After 2 retries: final error message
- Clear explanation of issue
- Suggestions for recovery
- No infinite retry loop
```

---

## Debugging Tips

### Check Backend Logs

```bash
# Watch for chat operations
tail -f logs/backend.log | grep "RAG chat"

# Expected log patterns:
# INFO: RAG chat initiated: project=..., repo=..., questionLen=...
# DEBUG: Vector search completed: 5 chunks found in 245ms
# DEBUG: LLM generation completed in 3250ms: success=true, tokens=542
# INFO: RAG chat complete: conversationId=..., sources=2, totalDuration=3510ms
```

### Check Frontend Network Requests

**In Browser DevTools → Network Tab**:
1. Filter by `chat` API call
2. Expected response time: 5-60 seconds (depending on size)
3. Check response body: `{ success: true, data: { answer, sources } }`

### Common Issues & Solutions

| Issue | Cause | Solution |
|---|---|---|
| "Repository indexing not ready" | Ingestion not complete | Click "Ingest AI" button and wait |
| "Question too short" | Input < 3 chars | Ask a longer, more specific question |
| "LLM error" | API key invalid | Check environment variables, restart backend |
| "No results found" | Vector search returned 0 chunks | Try different question, re-run ingestion |
| Timeout after 120s | LLM taking too long | Simplify question, try again |

---

## Performance Monitoring

### Key Metrics to Track

```
Frontend:
- Time from send click to first response token
- Total response time (user perspective)
- Retry rate (% of questions that retry)
- Error rate (% of questions that fail)

Backend (from logs):
- Vector search latency (should be < 1s)
- LLM generation latency (should be < 30s)
- Total request latency (should be < 60s for 95th percentile)
- Chunk retrieval count (typically 5)
```

### Example Monitoring Command

```bash
# Monitor chat performance in real-time
tail -f logs/backend.log | grep -E "RAG chat|search completed|generation completed|RAG chat complete" | \
awk '{print; if(/RAG chat complete/) print "\n---\n"}'
```

---

## User Stories & Acceptance Tests

### Story 1: Architecture Understanding
**As a** new team member  
**I want to** understand the system architecture  
**So that** I can contribute effectively

**Acceptance Tests**:
- [ ] Can ask "What's the system architecture?"
- [ ] Response includes main components/layers
- [ ] Source files cited
- [ ] Response time < 20s for typical project
- [ ] Markdown formatting is readable

**Test Command**:
```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": "xxx",
    "repositoryId": "yyy",
    "question": "What is the system architecture?"
  }' | jq '.data.answer'
```

### Story 2: Security Audit
**As a** security engineer  
**I want to** verify authentication and input validation  
**So that** I know the system is secure

**Acceptance Tests**:
- [ ] Can ask security-related questions
- [ ] Gets accurate security implementation details
- [ ] No sensitive data exposure in responses
- [ ] Source lines show actual security code
- [ ] Handles edge cases (SQL injection detection, etc.)

### Story 3: Debugging & Troubleshooting
**As a** developer  
**I want to** find and fix bugs quickly  
**So that** I can deliver features faster

**Acceptance Tests**:
- [ ] Can ask "Where are null pointers handled?"
- [ ] Response identifies exception handling
- [ ] Cites relevant error handling code
- [ ] Suggests patterns for missing cases
- [ ] Works for any programming language

---

## Deployment Checklist

Before deploying to production, verify:

- [ ] **Backend**: LLM provider API key configured
- [ ] **Backend**: PGVector index created and populated
- [ ] **Frontend**: API base URL points to correct backend
- [ ] **Security**: Rate limiting configured (1000 req/min per user)
- [ ] **Logging**: Structured JSON logging enabled
- [ ] **Monitoring**: Chat endpoint metrics exported to Prometheus
- [ ] **Performance**: Load test with concurrent users
- [ ] **Error Handling**: All error paths tested
- [ ] **Docs**: API documentation updated
- [ ] **UX**: Error messages tested and verified clear

---

## Troubleshooting Matrix

| Symptom | Likely Cause | Debug Steps | Fix |
|---|---|---|---|
| Chat disabled/greyed out | Repo not ready for ingestion | Check repo.ingestionStatus | Click Ingest AI |
| No response after 2 min | LLM timeout | Check backend logs for LLM errors | Check API key, retry |
| "No results" message | Empty vector search | Check chunk count in DB | Re-run ingestion |
| Repetitive/generic answers | Poor context retrieval | Check chunk similarity scores | Try more specific question |
| Same answer to different Qs | Context not used | Check history building in logs | Restart conversation |
| Answers hallucinate facts | LLM making up details | Check retrieved chunks | Improve prompt template |

---

## Support & Reporting

### For Support:
1. **Documentation**: Refer to `/docs/rag-pipeline.md`
2. **API Spec**: Check `/docs/PRD-BACKEND-API.md` → Section 5.2
3. **Known Issues**: Check GitHub Issues label: `chat-feature`
4. **Contact**: Karthik Reddy Malasani (Team Member 3 - AI Lead)

### To Report Issues:
1. Gather logs: `tail -100 logs/backend.log`
2. Note repo size (file count, total LOC)
3. Exact question asked
4. Expected vs actual result
5. Create GitHub issue with these details

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-09-01 | Initial release with comprehensive testing guide |

---

## Success Criteria

The chat feature is **working properly** when:

✅ Users can ask ANY type of question about their codebase  
✅ Answers are contextually relevant (sourced from actual code)  
✅ Works across all repo sizes (10 to 10,000+ files) with consistent performance  
✅ Errors are handled gracefully with user-friendly messages  
✅ Retry mechanism works automatically on transient failures  
✅ Conversation history is maintained for follow-up questions  
✅ Source citations help users navigate to relevant code  
✅ Performance meets targets (first token < 5s, total response < 60s)  
✅ No data leakage between projects or users  
✅ Logging enables debugging of issues  

---

*For the latest updates, refer to the main repository.*
