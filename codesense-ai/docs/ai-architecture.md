# AI Architecture

## Overview

The CodeSense AI Engine is the intelligence layer built by Team Member 3 (Karthik).
It integrates IBM watsonx.ai / IBM Granite for AI generation, PGVector for semantic search,
and provides a complete RAG pipeline for repository-aware Q&A.

---

## Component Map

```
Frontend
   ↓ REST
AI Controller  (AiController)
   ↓
AI Service  (AiService)
   ↓
   ├── RAG Engine  (RagService)
   │      ├── Vector Search  (VectorSearchService)
   │      │      └── PGVector  (repository_chunks)
   │      └── LLM Service  (LLMService)
   │             ├── WatsonxLLMService  [production]
   │             └── MockLLMService  [development]
   │
   ├── Ingestion Pipeline  (IngestionService)
   │      ├── Chunking Service  (ChunkingService)
   │      │      ├── Text Chunker  [fallback]
   │      │      └── Parser Chunker  [Team Member 4]
   │      └── Embedding Service  (EmbeddingService)
   │             ├── WatsonxEmbeddingService  [production]
   │             └── MockEmbeddingService  [development]
   │
   └── Parser Integration  (ParserIntegrationService)
          └── ParserIntegrationStub  [Team Member 4 placeholder]
```

---

## RAG Pipeline

```
User Question
      ↓
Query Embedding  (EmbeddingService.generateEmbedding)
      ↓
Vector Similarity Search  (PGVector cosine distance <=>)
      ↓ top-5 chunks (configurable)
Context Construction  (RagService.buildContext)
      ↓
Prompt Template  (PromptTemplates.repositoryChat)
      ↓
IBM Granite Generation  (WatsonxLLMService.generate)
      ↓
Answer + Source References
      ↓
Conversation Persistence  (conversation_messages)
```

---

## IBM watsonx.ai Integration

### API Endpoints Used

| Purpose | Endpoint |
|---------|----------|
| Text Generation | `POST {url}/ml/v1/text/generation?version={api-version}` |
| Embeddings | `POST {url}/ml/v1/text/embeddings?version={api-version}` |
| IAM Token | `POST https://iam.cloud.ibm.com/identity/token` |

### Authentication
1. API Key is exchanged for an IAM Bearer token
2. Token is cached for 3000s (tokens expire at ~3600s)
3. Token is automatically refreshed on expiry
4. NEVER stored in logs or responses

### Model Configuration
```yaml
ibm.watsonx.model-id: ibm/granite-13b-chat-v2  # configurable
ibm.watsonx.max-new-tokens: 2048
ibm.watsonx.temperature: 0.1  # near-deterministic for factual answers
ibm.watsonx.repetition-penalty: 1.05
```

---

## Embedding Architecture

### Production (IBM watsonx.ai)
- Model: `ibm/slate-125m-english-rtrvr` (configurable)
- Dimension: 768 (configurable via `EMBEDDING_DIMENSION`)

### Development (Mock)
- Deterministic pseudo-random embeddings
- Same text → same embedding (reproducible tests)
- L2-normalized for correct cosine similarity

---

## PGVector Schema

```sql
CREATE TABLE repository_chunks (
    id              UUID PRIMARY KEY,
    project_id      UUID NOT NULL,   -- SECURITY: always required
    repository_id   UUID NOT NULL,   -- SECURITY: always required
    file_id         UUID,
    file_path       VARCHAR(2000),
    language        VARCHAR(100),
    symbol_name     VARCHAR(500),
    symbol_type     VARCHAR(100),
    chunk_type      VARCHAR(100),    -- TEXT/CLASS/METHOD/FUNCTION/...
    start_line      INTEGER,
    end_line        INTEGER,
    content         TEXT NOT NULL,
    embedding       vector(768),     -- PGVector
    created_at      TIMESTAMP
);

-- Vector index for cosine similarity
CREATE INDEX idx_chunks_embedding ON repository_chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

### Security: Cross-Project Isolation

All vector searches are scoped:
```sql
WHERE project_id = :projectId AND repository_id = :repositoryId
```
Cross-project retrieval is architecturally impossible.

---

## Chunking Strategy

### Current (Fallback Text Chunker)
- Sliding window: `chunk-size=1000 chars`, `overlap=200 chars`
- Adds file path + language header to each chunk
- Configurable via `codesense.ai.ingestion.chunk-size`

### Future (Parser-Based, Team Member 4)
When `ParsedFileDTO` is submitted:
- `CLASS` elements → `ChunkType.CLASS` chunks
- `METHOD` elements → `ChunkType.METHOD` chunks
- `FUNCTION` elements → `ChunkType.FUNCTION` chunks
- Semantic boundaries replace text windows

---

## Prompt Engineering

All prompts are defined in `PromptTemplates.java`:

| Template | Purpose |
|----------|---------|
| `repositoryChat` | Repository Q&A with context |
| `codeExplanation` | Structured code analysis |
| `readmeGeneration` | README documentation |
| `apiDocumentation` | API endpoint documentation |
| `architectureExplanation` | Architecture analysis |
| `errorExplanation` | Debug assistance |

**Anti-hallucination rules in all prompts:**
- "Answer ONLY based on provided context"
- "Do not invent code not in context"
- "If insufficient info: say so explicitly"

---

## Configuration Reference

```yaml
codesense:
  ai:
    llm-provider: mock         # mock | watsonx
    embedding-provider: mock   # mock | watsonx
    embedding-dimension: 768
    rag:
      top-k: 5
      min-similarity: 0.7
      max-context-tokens: 4096
    ingestion:
      chunk-size: 1000
      chunk-overlap: 200
      batch-size: 50
```
