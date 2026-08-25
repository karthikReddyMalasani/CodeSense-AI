# Testing Guide — CodeSense AI

## Test Strategy

CodeSense AI uses a multi-layer testing strategy across all five team members:

| Layer | Framework | Owner |
|---|---|---|
| Unit Tests | JUnit 5 + Mockito | Team Member 5 (Vishnu) |
| Integration Tests | Spring Boot Test + Testcontainers | Team Member 5 |
| Frontend Tests | Vite/Jest | Team Member 1 (Prasanna) |

---

## Running Tests

### Backend Unit Tests

```bash
cd backend
./mvnw test
```

Or on Windows:
```cmd
mvn test -B
```

### Skip integration tests (no DB required):

```bash
mvn test -B \
  -Dspring.flyway.enabled=false \
  -Dspring.jpa.hibernate.ddl-auto=none \
  -Dcodesense.ai.llm-provider=mock \
  -Dcodesense.ai.embedding-provider=mock
```

---

## Test Coverage

| Module | Tests | Coverage Area |
|---|---|---|
| `AuthServiceTest` | 3 | Registration, login, duplicate email |
| `ChunkingServiceTest` | 5 | Text chunking, edge cases |
| `MockLLMServiceTest` | 6 | LLM mock responses |
| `MockEmbeddingServiceTest` | 6 | Embedding generation |
| `JavaParserCodeParserTest` | 9 | Java AST extraction |
| `TreeSitterCodeParserTest` | 14 | Multi-language parsing |
| `DependencyAnalysisServiceTest` | 4 | Dependency graph |
| `LanguageDetectionServiceTest` | 31 | All language extensions |
| `ParserIntegrationDTOTest` | 3 | DTO serialization |
| `ProjectServiceTest` | 3 | Project CRUD |

**Total: 84 unit tests, all passing**

---

## Test Data

Sample repositories in `test-data/`:

| Directory | Languages | Purpose |
|---|---|---|
| `java-project/` | Java | Java parsing, controllers, services |
| `python-project/` | Python | Python parsing, Flask API |
| `javascript-project/` | JavaScript | JS class/function extraction |
| `typescript-project/` | TypeScript | TS interface/class extraction |
| `mixed-language-project/` | Java, Python, TypeScript | Cross-language parsing |

---

## Integration Testing with Testcontainers

For full integration tests with a real PostgreSQL database:

```bash
# Requires Docker running
mvn verify -P integration-tests
```

Testcontainers dependencies are already in `pom.xml`.

---

## Security Testing

The CI pipeline checks for:
- Hardcoded credentials in source code
- Proper JWT validation
- Project ownership enforcement
- ZIP path traversal protection
