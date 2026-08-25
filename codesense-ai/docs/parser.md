# Parser Architecture — CodeSense AI

## Overview

The parser module (Team Member 4 — Prashanthi) provides multi-language code intelligence.

It consists of two main parsing engines:
1. **JavaParser** — deep Java AST analysis
2. **Regex-based multi-language parser** — Python, JavaScript, TypeScript, and 9 other languages

---

## Supported Languages

| Language | Parser | Extensions |
|---|---|---|
| Java | JavaParser (deep AST) | `.java` |
| Python | Regex-based | `.py` |
| JavaScript | Regex-based | `.js`, `.jsx` |
| TypeScript | Regex-based | `.ts`, `.tsx` |
| C | Regex-based | `.c`, `.h` |
| C++ | Regex-based | `.cpp`, `.cc`, `.cxx`, `.hpp` |
| C# | Regex-based | `.cs` |
| Go | Regex-based | `.go` |
| Rust | Regex-based | `.rs` |
| PHP | Regex-based | `.php` |
| Ruby | Regex-based | `.rb` |
| Kotlin | Regex-based | `.kt`, `.kts` |
| Swift | Regex-based | `.swift` |

---

## Note on Tree-sitter

The original specification calls for Tree-sitter. The current implementation uses a
regex-based parser which provides equivalent functionality for the common cases needed
(class/method/function/import extraction).

**Why not Tree-sitter directly:**
Tree-sitter's Java binding (JTreeSitter) uses Panama FFI (Project Panama) and requires
native grammar files that are not available on Maven Central as stable JARs.

**Future upgrade path:**
The `TreeSitterCodeParser` class is designed as a drop-in replacement. When stable
Maven artifacts for `org.treesitter` become available, the implementation body
of `TreeSitterCodeParser` can be replaced while keeping the same interface.
The `CodeParser` interface ensures this is a localized change.

---

## Architecture

```
ParserRouter
    |
    +─── JavaParserCodeParser   (Java files)
    |
    +─── TreeSitterCodeParser   (Python, JS, TS, C, Go, Rust, ...)
    |
    +─── GenericCodeParser      (fallback)
```

---

## Extracted Elements

For each file, the parser extracts:

| Element Type | Description |
|---|---|
| `CLASS` | Class declarations |
| `INTERFACE` | Interface declarations |
| `ENUM` | Enum types |
| `METHOD` | Class methods |
| `FUNCTION` | Standalone functions |
| `CONSTRUCTOR` | Java constructors |
| `FIELD` | Class fields |
| `PACKAGE` | Package declarations |
| `MODULE` | File-level modules |
| `STRUCT` | C/Rust/Go structs |
| `TRAIT` | Rust traits |

---

## Dependency Graph

Uses JGraphT (`DefaultDirectedGraph`) to build:

1. **Import graph** — which files import which modules
2. **Inheritance graph** — EXTENDS/IMPLEMENTS relationships
3. **Call graph** — method call relationships (Java only, from JavaParser)

Output: Serializable `DependencyGraph` DTO + Mermaid diagram string

---

## UML Diagrams

Generated from parser data only — no hallucination:

- **PlantUML class diagram** — classes, methods, inheritance
- **PlantUML architecture diagram** — packages and components
- **Mermaid class diagram** — same as PlantUML but Mermaid syntax
- **Mermaid architecture flow** — controller/service/repository layers

---

## Integration with AI Engine

Team Member 4's output is consumed by Team Member 3 (Karthik) via:

1. `ParserIntegrationService.ingestParsedRepository(ParsedRepositoryDTO)` 
2. `ParserIntegrationService.ingestParsedFile(ParsedFileDTO)`

When parser integration is active, the AI Engine uses **semantic chunks** (class/method boundaries) instead of text-window chunks, improving RAG quality significantly.
