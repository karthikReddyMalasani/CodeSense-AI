# Parser Integration Guide

## For Team Member 4 (Prashanthi)

This document describes how to integrate JavaParser and Tree-sitter with the CodeSense AI Engine.

---

## Overview

The AI Engine (Team Member 3) has defined a clean integration interface.
Team Member 4 only needs to implement one interface and submit DTOs.
**No changes to the AI Engine are required.**

---

## Integration Interface

```java
// Package: com.codesense.integration.parser
public interface ParserIntegrationService {
    void ingestParsedRepository(ParsedRepositoryDTO parsedRepository);
    void ingestParsedFile(ParsedFileDTO parsedFile);
    boolean isParserAvailable();
}
```

---

## Step 1: Create Your Implementation

```java
@Service
@Primary  // Replaces the stub
public class JavaParserTreeSitterIntegration implements ParserIntegrationService {

    private final IngestionService ingestionService;

    @Override
    public void ingestParsedRepository(ParsedRepositoryDTO parsedRepository) {
        ingestionService.ingestFromParserMetadata(parsedRepository);
    }

    @Override
    public void ingestParsedFile(ParsedFileDTO parsedFile) {
        ingestionService.ingestSingleParsedFile(parsedFile);
    }

    @Override
    public boolean isParserAvailable() {
        return true;
    }
}
```

---

## Step 2: Parse Files

### Java files → JavaParser

```java
ParsedFileDTO parseJavaFile(String filePath, String content) {
    CompilationUnit cu = StaticJavaParser.parse(content);
    List<CodeElementDTO> elements = new ArrayList<>();

    cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
        elements.add(CodeElementDTO.builder()
            .name(cls.getNameAsString())
            .type("CLASS")
            .language("Java")
            .filePath(filePath)
            .startLine(cls.getBegin().map(p -> p.line).orElse(0))
            .endLine(cls.getEnd().map(p -> p.line).orElse(0))
            .content(cls.toString())
            .build());

        cls.getMethods().forEach(method -> {
            elements.add(CodeElementDTO.builder()
                .name(method.getNameAsString())
                .type("METHOD")
                .language("Java")
                .filePath(filePath)
                .startLine(method.getBegin().map(p -> p.line).orElse(0))
                .endLine(method.getEnd().map(p -> p.line).orElse(0))
                .content(method.toString())
                .build());
        });
    });

    return ParsedFileDTO.builder()
        .filePath(filePath)
        .language("Java")
        .content(content)
        .elements(elements)
        .build();
}
```

### Other languages → Tree-sitter

```java
ParsedFileDTO parseWithTreeSitter(String filePath, String language, String content) {
    // Use the tree-sitter Java binding to parse
    // Map tree-sitter node types to CodeElementDTO types
    // Return ParsedFileDTO with extracted elements
}
```

---

## Step 3: Extract Relationships

```java
// From JavaParser
cu.findAll(MethodCallExpr.class).forEach(call -> {
    relationships.add(CodeRelationshipDTO.builder()
        .source(callerClass)
        .target(call.getNameAsString())
        .type("CALLS")
        .lineNumber(call.getBegin().map(p -> p.line).orElse(0))
        .build());
});
```

---

## DTO Reference

### ParsedFileDTO
```json
{
  "repositoryId": "uuid",
  "projectId": "uuid",
  "filePath": "src/main/java/AuthService.java",
  "language": "Java",
  "content": "public class AuthService { ... }",
  "elements": [
    {
      "name": "AuthService",
      "type": "CLASS",
      "language": "Java",
      "filePath": "src/main/java/AuthService.java",
      "startLine": 1,
      "endLine": 100,
      "content": "public class AuthService { ... }"
    },
    {
      "name": "authenticate",
      "type": "METHOD",
      "language": "Java",
      "filePath": "src/main/java/AuthService.java",
      "startLine": 25,
      "endLine": 45
    }
  ],
  "relationships": [
    {
      "source": "AuthController",
      "target": "AuthService",
      "type": "CALLS",
      "sourceFile": "AuthController.java",
      "targetFile": "AuthService.java"
    }
  ]
}
```

### Supported Element Types
- `CLASS` — Java/Python/TS class
- `METHOD` — Instance method
- `FUNCTION` — Standalone function (Python, JS, Go, Rust)
- `INTERFACE` — Interface/trait/protocol
- `ENUM` — Enum type
- `CONSTRUCTOR` — Constructor method
- `MODULE` — Python module/package
- `NAMESPACE` — C++/C# namespace

### Supported Relationship Types
- `CALLS` — Method/function invocation
- `EXTENDS` — Class inheritance
- `IMPLEMENTS` — Interface implementation
- `IMPORTS` — Module import
- `USES` — General dependency
- `CREATES` — Object instantiation
- `THROWS` — Exception dependency

---

## Language → Parser Mapping

| Language   | Extension | Parser       |
|------------|-----------|--------------|
| Java       | .java     | JavaParser   |
| Python     | .py       | Tree-sitter  |
| JavaScript | .js, .jsx | Tree-sitter  |
| TypeScript | .ts, .tsx | Tree-sitter  |
| C          | .c, .h    | Tree-sitter  |
| C++        | .cpp, .cc | Tree-sitter  |
| C#         | .cs       | Tree-sitter  |
| Go         | .go       | Tree-sitter  |
| Rust       | .rs       | Tree-sitter  |
| PHP        | .php      | Tree-sitter  |
| Ruby       | .rb       | Tree-sitter  |
| Kotlin     | .kt       | Tree-sitter  |
| Swift      | .swift    | Tree-sitter  |

---

## What Happens After Integration

When Team Member 4 submits `ParsedRepositoryDTO`:

1. `ParserIntegrationService.ingestParsedRepository()` is called
2. `IngestionService.ingestFromParserMetadata()` creates semantic chunks by class/method/function
3. Each element becomes a `RepositoryChunk` with `ChunkType = CLASS/METHOD/FUNCTION`
4. Chunks are embedded and stored in PGVector
5. RAG queries now return semantically meaningful results (class-level, method-level)
6. The AI can cite specific classes and methods in answers

This replaces the text-window fallback chunking with AST-based semantic chunking.
