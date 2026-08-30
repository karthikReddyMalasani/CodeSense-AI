# RAG Pipeline — CodeSense AI

**Owner:** Team Member 3 — Karthik (AI Engineer)

---

## Overview

CodeSense AI uses a Retrieval-Augmented Generation (RAG) pipeline to ground every AI response in
actual repository source code. The LLM (IBM Granite via watsonx.ai) never answers from general
knowledge alone — it always receives relevant code chunks as context before generating a response.

---

## End-to-End Flow

```
User Question
      │
      ▼
 ┌─────────────────────────┐
 │  1. Query Processing    │  Sanitise and normalise the question
 └──────────┬──────────────┘
            │
            ▼
 ┌─────────────────────────┐
 │  2. Query Embedding     │  Convert question → float[768] vector
 └──────────┬──────────────┘
            │
            ▼
 ┌─────────────────────────┐
 │  3. Vector Search       │  Cosine similarity search in PGVector
 │  (scoped to project +   │  SELECT … ORDER BY embedding <=> ? LIMIT k
 │   repository IDs)       │
 └──────────┬──────────────┘
            │
            ▼
 ┌─────────────────────────┐
 │  4. Top-K Chunk Retrieval│  Default k = 5 relevant code chunks
 └──────────┬──────────────┘
            │
            ▼
 ┌─────────────────────────┐
 │  5. Context Construction│  Build structured context string from chunks
 │                         │  (file path, language, lines, content)
 └──────────┬──────────────┘
            │
            ▼
 ┌─────────────────────────┐
 │  6. Conversation Memory │  Prepend last N turns of conversation history
 └──────────┬──────────────┘
            │
            ▼
 ┌─────────────────────────┐
 │  7. Prompt Assembly     │  Fill PromptTemplates.REPOSITORY_CHAT template
 └──────────┬──────────────┘
            │
            ▼
 ┌─────────────────────────┐
 │  8. IBM Granite (LLM)   │  POST to watsonx.ai /ml/v1/text/generation
 └──────────┬──────────────┘
            │
            ▼
 ┌─────────────────────────┐
 │  9. Response + Sources  │  Return answer + source references
 └─────────────────────────┘
```

---

## Repository Ingestion Pipeline

Before RAG can work, the repository must be ingested.

```
Repository ZIP / GitHub clone
           │
           ▼
 ┌─────────────────────────┐
 │  File Discovery         │  Walk all files; skip .git, node_modules, etc.
 └──────────┬──────────────┘
            │
            ▼
 ┌─────────────────────────┐
 │  Language Detection     │  LanguageDetectionService (13 languages)
 └──────────┬──────────────┘
            │
            ▼
 ┌─────────────────────────┐
 │  Parser Metadata        │  IF Team Member 4 has provided parsed output,
 │  (optional)             │  use CodeElementDTO / ParsedFileDTO for chunking
 └──────────┬──────────────┘
            │
            ▼
 ┌─────────────────────────┐
 │  Code Chunking          │  ChunkingService
 │  Strategy A (with TM4): │    class-level, method-level, function-level chunks
 │  Strategy B (fallback): │    fixed-window text chunks (512 chars, 50-char overlap)
 └──────────┬──────────────┘
            │
            ▼
 ┌─────────────────────────┐
 │  Embedding Generation   │  EmbeddingService → float[768]
 │  (Mock or Watsonx)      │
 └──────────┬──────────────┘
            │
            ▼
 ┌─────────────────────────┐
 │  PGVector Storage       │  repository_chunks table
 │  (PostgreSQL extension) │  indexed by project_id, repository_id
 └─────────────────────────┘
```

---

## Security Isolation

Every vector search query is **always** filtered by both `project_id` AND `repository_id`:

```sql
SELECT *
FROM   repository_chunks
WHERE  project_id    = :projectId
  AND  repository_id = :repositoryId
ORDER  BY embedding <=> :queryVector
LIMIT  5;
```

This guarantees:
- User A can never retrieve chunks from User B's repository.
- Repository X's chat cannot retrieve content from Repository Y in the same project.

---

## Chunking Strategies

### Strategy A — Parser-Assisted (when Team Member 4 is integrated)

| Element Type | Chunk Boundary |
|---|---|
| `CLASS` | Entire class body |
| `METHOD` | Single method body |
| `FUNCTION` | Single function body |
| `MODULE` | Top-level module/file |

Metadata stored per chunk: `symbolName`, `symbolType`, `startLine`, `endLine`, `language`.

### Strategy B — Text Fallback (current default)

- Window size: **512 characters**
- Overlap: **50 characters**
- Chunks are labelled with file path and language
- Used when no parser metadata is available

The fallback is clearly isolated in [`ChunkingService.chunkTextFallback()`](../backend/src/main/java/com/codesense/ai/chunking/ChunkingService.java).
Team Member 4 integration will replace this path via `chunkWithParserMetadata()`.

---

## Prompt Template (Repository Chat)

```
You are an expert code assistant analysing a software repository.

Repository: {repositoryName}
Language(s): {languages}

RELEVANT CODE CONTEXT:
{context}

CONVERSATION HISTORY:
{history}

USER QUESTION:
{question}

INSTRUCTIONS:
- Answer based ONLY on the code context provided above.
- Always cite source files (filePath, startLine, endLine).
- If the context is insufficient, say: "I could not find enough information 
  in the repository to answer this confidently."
- Do NOT hallucinate class names, method names, or file paths.
- Distinguish between facts from the code and your inferences.
```

---

## Embedding Architecture

| Property | Value |
|---|---|
| Provider (mock) | `MockEmbeddingService` — deterministic SHA-256 seeded, L2-normalised |
| Provider (prod) | `GeminiEmbeddingService` — Google Gemini embedding API |
| Dimension | 768 |
| Storage | `pgvector` extension, `vector(768)` column |
| Similarity | Cosine (`<=>` operator) |
| Batch support | `generateEmbeddings(List<String>)` |

Switch provider via environment variable:
```
AI_EMBEDDING_PROVIDER=mock      # local development, no AI credentials needed
AI_EMBEDDING_PROVIDER=gemini    # production
```

---

## Conversation Memory

Conversations are stored in PostgreSQL in the `conversations` and `conversation_messages` tables.

- Each conversation is scoped to a `(userId, projectId, repositoryId)` triple.
- The last **5 messages** (configurable) are injected into the RAG prompt as history.
- A conversation from Repository A **never** influences Repository B.

---

## AI Response Format

```json
{
  "answer": "JWT authentication is handled by JwtService which...",
  "conversationId": "uuid",
  "sources": [
    {
      "filePath": "src/auth/JwtService.java",
      "startLine": 12,
      "endLine": 85,
      "symbolName": "JwtService",
      "language": "JAVA",
      "relevanceScore": 0.94
    }
  ]
}
```

Source references come **only** from retrieved chunks — never invented.

---

## Configuration

| Environment Variable | Description | Default |
|---|---|---|
| `AI_LLM_PROVIDER` | `mock` or `gemini` | `gemini` |
| `AI_EMBEDDING_PROVIDER` | `mock` or `gemini` | `gemini` |
| `AI_RAG_TOP_K` | Number of chunks to retrieve | `5` |
| `AI_CHUNK_SIZE` | Text fallback chunk size (chars) | `512` |
| `AI_CHUNK_OVERLAP` | Text fallback overlap (chars) | `50` |
| `GEMINI_API_KEY` | Google AI Studio API key | — |
| `GEMINI_MODEL` | Gemini generation model | `gemini-2.5-flash` |
| `GEMINI_EMBEDDING_MODEL` | Gemini embedding model | `gemini-embedding-001` |
| `EMBEDDING_DIMENSION` | pgvector embedding dimension | `768` |

---

## Key Classes

| Class | Location | Role |
|---|---|---|
| `RagService` | `ai/rag/` | Orchestrates the full RAG flow |
| `IngestionService` | `ai/ingestion/` | Async ingestion pipeline |
| `ChunkingService` | `ai/chunking/` | Text chunking (fallback + parser-assisted) |
| `VectorSearchService` | `ai/vector/` | Cosine similarity search via PGVector |
| `EmbeddingService` | `ai/embedding/` | Embedding generation interface |
| `LLMService` | `ai/llm/` | LLM generation interface |
| `PromptTemplates` | `ai/prompt/` | All prompt templates |
| `RepositoryChunkRepository` | `ai/vector/` | PGVector JPA queries |

---

## Future Improvements (Post-TM4 Integration)

1. Replace text-fallback chunking with AST-aware chunking using `ParsedFileDTO`.
2. Add semantic deduplication of chunks at ingestion time.
3. Support streaming responses via `LLMService.stream()`.
4. Add re-ranking of retrieved chunks (cross-encoder).
5. Support hybrid search (vector + keyword BM25).
