# Tree-sitter Architecture — CodeSense AI

> **Owner**: Prashanthi (Team Member 4) — Code Intelligence Module
> **Status**: Implemented as regex-based substitute; interface ready for real Tree-sitter

---

## Overview

CodeSense AI's parser architecture supports 13 programming languages. For languages other than Java, the system uses `TreeSitterCodeParser` — which implements the same `CodeParser` interface that would be used by a real Tree-sitter binding.

---

## Why Not Real Tree-sitter?

The official Java binding for Tree-sitter, **JTreeSitter** (via `io.github.bonede:tree-sitter`), was evaluated:

- JTreeSitter relies on JNI and requires JDK 22+ for the Panama FFI backend
- The published Maven artifacts are experimental and may not build on all platforms
- The project uses **Java 23** — technically compatible, but the published JARs had resolution issues on this workstation

**Decision**: Build a regex-based `TreeSitterCodeParser` that:
1. Implements the exact same `CodeParser` interface
2. Uses language-specific regex patterns to extract code elements
3. Can be swapped for a real Tree-sitter implementation without changing any other code

---

## Interface Design

```java
public interface CodeParser {
    ParsedFile parse(String content, String filePath, String language);
    List<String> getSupportedLanguages();
}
```

All parsers implement this interface. The `ParserRouter` selects the right parser based on language.

---

## Supported Languages (TreeSitterCodeParser)

| Language   | Extension(s)       | Extracted Elements                                    |
|------------|--------------------|-------------------------------------------------------|
| Python     | `.py`              | Classes, functions, imports, decorators               |
| JavaScript | `.js`, `.jsx`      | Classes, functions, arrow functions, imports          |
| TypeScript | `.ts`, `.tsx`      | Classes, interfaces, functions, type aliases, imports |
| C          | `.c`, `.h`         | Functions, structs, includes                          |
| C++        | `.cpp`, `.cc`, `.h`| Classes, functions, structs, includes                |
| C#         | `.cs`              | Classes, interfaces, methods, namespaces, usings      |
| Go         | `.go`              | Structs, functions, methods, imports, packages        |
| Rust       | `.rs`              | Structs, enums, functions, traits, impls, uses        |
| PHP        | `.php`             | Classes, functions, requires                          |
| Ruby       | `.rb`              | Classes, modules, methods, requires                   |
| Kotlin     | `.kt`              | Classes, interfaces, functions, imports               |
| Swift      | `.swift`           | Classes, structs, enums, functions, imports           |

---

## Regex Pattern Examples

Each language defines patterns for its AST node types. Example for Python:

```java
// Python class definition
Pattern classPattern = Pattern.compile(
    "^\\s*class\\s+(\\w+)(?:\\s*\\([^)]*\\))?\\s*:",
    Pattern.MULTILINE
);

// Python function definition
Pattern functionPattern = Pattern.compile(
    "^\\s*def\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:->\\s*[^:]+)?\\s*:",
    Pattern.MULTILINE
);

// Python import
Pattern importPattern = Pattern.compile(
    "^(?:from\\s+\\S+\\s+)?import\\s+(.+)$",
    Pattern.MULTILINE
);
```

---

## Language-Specific Node Types

The patterns are intentionally **not assumed identical across languages**.

Different languages use different constructs:
- Python: `class_definition`, `function_definition`, `import_statement`, `decorator`
- JavaScript: `class_declaration`, `function_declaration`, `arrow_function`, `import_statement`
- TypeScript: adds `interface_declaration`, `type_alias_declaration`
- Go: `func_declaration`, `type_declaration` (struct), `method_declaration`
- Rust: `struct_item`, `fn_item`, `trait_item`, `impl_item`, `enum_item`

---

## Upgrading to Real Tree-sitter

When a stable Java Tree-sitter binding becomes available:

1. Add the Maven dependency to `pom.xml`
2. Create a new class `RealTreeSitterCodeParser implements CodeParser`
3. Register it in `ParserRouter` for the relevant languages
4. Remove or disable `TreeSitterCodeParser` for those languages
5. No changes needed to `RepositoryParserService`, `IngestionService`, `AiService`, or any other class

The interface isolation means the upgrade is a drop-in replacement.

---

## Output: CodeElement

Each parser produces `CodeElement` objects:

```java
public class CodeElement {
    private String id;          // UUID
    private String name;        // e.g., "AuthController"
    private ElementType type;   // CLASS, FUNCTION, IMPORT, etc.
    private String language;    // e.g., "Python"
    private String filePath;
    private int startLine;
    private int endLine;
    private String sourceCode;
    private Map<String, Object> metadata;
}
```

---

## Integration with AI Engine

`RepositoryParserService` aggregates all parsed files into a `ParsedRepositoryDTO`.

This is passed to `IngestionService` via the `ParserIntegrationService` interface:

```
RepositoryParserService → ParsedRepositoryDTO
                                 ↓
                    ParserIntegrationService (interface)
                                 ↓
                    IngestionService (TM3 consumes DTOs)
                                 ↓
                    Code-aware chunking → Embeddings → PGVector
```

---

## Recommended Future Improvement

Replace `TreeSitterCodeParser` with a real Tree-sitter binding using the same `CodeParser` interface:
- `tree-sitter` v0.20+ via Java bindings
- `tree-sitter-java`, `tree-sitter-python`, etc. grammars
- Use Tree-sitter queries (`.scm` files) for pattern-based node extraction
