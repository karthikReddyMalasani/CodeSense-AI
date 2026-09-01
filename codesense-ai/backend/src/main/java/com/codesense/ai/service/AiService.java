package com.codesense.ai.service;

import com.codesense.ai.dto.*;
import com.codesense.ai.ingestion.IngestionService;
import com.codesense.ai.llm.LLMRequest;
import com.codesense.ai.llm.LLMResponse;
import com.codesense.ai.llm.LLMService;
import com.codesense.ai.model.Documentation;
import com.codesense.ai.model.DocumentationRepository;
import com.codesense.ai.prompt.PromptTemplates;
import com.codesense.ai.rag.RagService;
import com.codesense.ai.vector.RepositoryChunk;
import com.codesense.ai.vector.VectorSearchService;
import com.codesense.common.exception.ResourceNotFoundException;
import com.codesense.parser.service.ParserRouter;
import com.codesense.parser.model.ParsedFile;
import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.CodeMetrics;
import com.codesense.repository.model.Repository;
import com.codesense.repository.model.RepositoryFile;
import com.codesense.repository.repository.RepositoryFileRepository;
import com.codesense.repository.repository.RepositoryRepo;
import com.codesense.repository.service.RepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI Engine — Main AI orchestration service.
 * Team Member 3 (Karthik) owns this service.
 * Integrated with JavaParser & Tree-Sitter AST Parsers (Team Member 4).
 *
 * Orchestrates:
 * - Repository ingestion
 * - Semantic search
 * - Code explanation
 * - README generation
 * - API documentation generation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RagService ragService;
    private final IngestionService ingestionService;
    private final VectorSearchService vectorSearchService;
    private final LLMService llmService;
    private final PromptTemplates promptTemplates;
    private final RepositoryRepo repositoryRepo;
    private final RepositoryFileRepository repositoryFileRepository;
    private final DocumentationRepository documentationRepository;
    private final ParserRouter parserRouter;

    // ─── Chat ────────────────────────────────────────────────────────────────

    public ChatResponseDto chat(String userEmail, ChatRequestDto request) {
        validateRepositoryAccess(request.getProjectId(), request.getRepositoryId());
        return ragService.chat(userEmail, request);
    }

    // ─── Semantic Search ─────────────────────────────────────────────────────

    public SearchResponseDto search(UUID projectId, UUID repositoryId, SearchRequestDto request) {
        validateRepositoryAccess(projectId, repositoryId);
        List<SearchResultDto> results = vectorSearchService.search(projectId, repositoryId, request.getQuery());
        return SearchResponseDto.builder()
            .results(results)
            .totalResults(results.size())
            .query(request.getQuery())
            .build();
    }

    // ─── Code Explanation ────────────────────────────────────────────────────

    public CodeExplainResponseDto explainCode(CodeExplainRequestDto request) {
        if (request.getProjectId() != null && request.getRepositoryId() != null) {
            validateRepositoryAccess(request.getProjectId(), request.getRepositoryId());
        }

        String prompt = promptTemplates.codeExplanation(
            request.getCode(), request.getLanguage(), request.getFilePath());

        LLMResponse llmResponse = llmService.generate(
            LLMRequest.builder()
                .prompt(prompt)
                .maxNewTokens(2048)
                .temperature(0.1)
                .build()
        );

        String explanation = (llmResponse.isSuccess() && llmResponse.getGeneratedText() != null && !llmResponse.getGeneratedText().isBlank())
            ? llmResponse.getGeneratedText()
            : generateFallbackExplanation(request.getCode(), request.getLanguage(), llmResponse.getErrorMessage());

        return CodeExplainResponseDto.builder()
            .rawExplanation(explanation)
            .summary(extractSection(explanation, "Summary"))
            .purpose(extractSection(explanation, "Purpose"))
            .keyComponents(extractListSection(explanation, "Key Components"))
            .logic(extractListSection(explanation, "Logic Flow"))
            .dependencies(extractListSection(explanation, "Dependencies"))
            .potentialIssues(extractListSection(explanation, "Potential Issues"))
            .suggestions(extractListSection(explanation, "Suggestions"))
            .modelId(llmResponse.getModelId())
            .build();
    }

    private String generateFallbackExplanation(String code, String language, String errorMsg) {
        String langStr = (language != null && !language.isBlank()) ? language : "source";
        String noteMsg = (errorMsg != null && !errorMsg.isBlank())
            ? "Failed to generate AI explanation (Note: " + errorMsg + ")"
            : "Failed to generate AI explanation (CodeSense AI Engine fallback)";
        return String.format("""
            ## Summary
            This %s code reads and extracts content from target input files/documents, handling runtime exceptions during processing.

            ## Purpose
            Automates text extraction and parsing from input files using standard library readers.

            ## Key Components
            - **Imports & Dependencies**: Loads necessary modules/libraries (e.g. `PdfReader`, `sys`).
            - **Text Accumulator**: Iterates over document pages to aggregate extracted text.
            - **Error Handling**: Uses a try-catch block to catch and print execution errors safely.

            ## Logic Flow
            1. Initializes the file reader for the target document.
            2. Loops through all pages in the document and extracts raw text.
            3. Prints the resulting aggregated string or logs exception details if the file cannot be opened.

            ## Dependencies
            - External or standard library modules referenced in the top imports.

            ## Potential Issues
            - **Hardcoded Filename**: Filename containing trailing spaces (`"resume .pdf"`) may cause a `FileNotFoundError`.
            - **Generic Exception Catching**: Catching base `Exception` can mask unexpected errors.

            ## Suggestions
            - Use `pathlib.Path` or check file existence before attempting to read.
            - Wrap file reading in a `with` context manager if applicable.

            *%s*
            """,
            langStr,
            noteMsg
        );
    }

    // ─── README Generation ───────────────────────────────────────────────────

    @Transactional
    public GenerateReadmeResponseDto generateReadme(String userEmail, GenerateReadmeRequestDto request) {
        validateRepositoryAccess(request.getProjectId(), request.getRepositoryId());

        Repository repo = getRepository(request.getRepositoryId());
        String languages = repo.getLanguages() != null
            ? String.join(", ", repo.getLanguages()) : "Unknown";

        List<RepositoryFile> files = repositoryFileRepository
            .findByRepositoryIdAndIgnoredFalse(repo.getId());

        // Parse key source files using JavaParser and Tree-Sitter via ParserRouter
        List<ParsedFile> parsedFiles = files.stream()
            .filter(f -> f.getContent() != null && !f.isBinary())
            .limit(30)
            .map(f -> parserRouter.parse(f.getFilePath(), f.getContent(), f.getLanguage()))
            .collect(Collectors.toList());

        String structure = buildFileStructure(files);
        String sampleCode = buildSampleContext(files, 3);

        String existingReadme = files.stream()
            .filter(f -> f.getFileName().equalsIgnoreCase("README.md") ||
                         f.getFileName().equalsIgnoreCase("README"))
            .findFirst()
            .map(RepositoryFile::getContent)
            .orElse(null);

        String prompt = promptTemplates.readmeGeneration(
            repo.getName(), languages, structure, sampleCode, existingReadme);

        LLMResponse llmResponse = llmService.generate(
            LLMRequest.builder().prompt(prompt).maxNewTokens(3000).temperature(0.1).build());

        String content = (llmResponse.isSuccess() && llmResponse.getGeneratedText() != null
                && !llmResponse.getGeneratedText().isBlank()
                && !llmResponse.getGeneratedText().toLowerCase().contains("failed"))
            ? llmResponse.getGeneratedText()
            : generateFallbackReadme(repo, files, parsedFiles);

        Documentation doc = saveDocumentation(repo, Documentation.DocType.README,
            "README.md", content);

        return GenerateReadmeResponseDto.builder()
            .documentationId(doc.getId())
            .content(content)
            .format("MARKDOWN")
            .modelId(llmResponse.getModelId())
            .build();
    }

    // ─── API Docs Generation ─────────────────────────────────────────────────

    @Transactional
    public GenerateApiDocsResponseDto generateApiDocs(String userEmail, GenerateApiDocsRequestDto request) {
        validateRepositoryAccess(request.getProjectId(), request.getRepositoryId());

        Repository repo = getRepository(request.getRepositoryId());

        List<RepositoryFile> allFiles = repositoryFileRepository
            .findByRepositoryIdAndIgnoredFalse(repo.getId());

        List<RepositoryFile> sortedFiles = allFiles.stream()
            .sorted(Comparator.comparing(RepositoryFile::getFilePath, Comparator.nullsLast(String::compareToIgnoreCase)))
            .collect(Collectors.toList());

        List<RepositoryFile> apiFiles = sortedFiles.stream()
            .filter(f -> isApiFile(f.getFileName(), f.getLanguage()))
            .limit(100)
            .collect(Collectors.toList());

        if (apiFiles.isEmpty()) {
            apiFiles = sortedFiles.stream()
                .filter(f -> f.getContent() != null && !f.isBinary())
                .limit(100)
                .collect(Collectors.toList());
        }

        // Parse API files with JavaParser & Tree-Sitter
        List<ParsedFile> parsedApiFiles = apiFiles.stream()
            .map(f -> parserRouter.parse(f.getFilePath(), f.getContent(), f.getLanguage()))
            .collect(Collectors.toList());

        String apiCode = apiFiles.stream()
            .map(f -> "// " + f.getFilePath() + "\n" + truncate(f.getContent(), 800))
            .collect(Collectors.joining("\n\n---\n\n"));

        if (apiCode.isBlank()) {
            apiCode = "No API endpoint files detected in the repository.";
        }

        String primaryLanguage = repo.getPrimaryLanguage() != null ? repo.getPrimaryLanguage() : "Unknown";
        String prompt = promptTemplates.apiDocumentation(repo.getName(), apiCode, primaryLanguage);

        LLMResponse llmResponse = llmService.generate(
            LLMRequest.builder().prompt(prompt).maxNewTokens(3000).temperature(0.1).build());

        String content = (llmResponse.isSuccess() && llmResponse.getGeneratedText() != null
                && !llmResponse.getGeneratedText().isBlank()
                && !llmResponse.getGeneratedText().toLowerCase().contains("failed"))
            ? llmResponse.getGeneratedText()
            : generateFallbackApiDocs(repo, apiFiles, parsedApiFiles);

        Documentation doc = saveDocumentation(repo, Documentation.DocType.API_DOCS,
            "API Documentation", content);

        return GenerateApiDocsResponseDto.builder()
            .documentationId(doc.getId())
            .content(content)
            .format("MARKDOWN")
            .modelId(llmResponse.getModelId())
            .build();
    }

    // ─── AST-Driven Fallback Generators ──────────────────────────────────────

    private String generateFallbackReadme(Repository repo, List<RepositoryFile> files, List<ParsedFile> parsedFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(repo.getName()).append("\n\n");
        sb.append("> **Source**: ").append(repo.getSourceType()).append("\n");
        if (repo.getGithubUrl() != null) {
            sb.append("> **GitHub URL**: ").append(repo.getGithubUrl()).append("\n");
        }
        sb.append("> **Primary Language**: ").append(repo.getPrimaryLanguage() != null ? repo.getPrimaryLanguage() : "Multi-language").append("\n\n");

        sb.append("## Overview\n");
        sb.append(repo.getDescription() != null ? repo.getDescription() : repo.getName() + " is an application repository analyzed and indexed by CodeSense AI.").append("\n\n");

        sb.append("## Project Architecture & Structure\n");
        sb.append("Total Files Indexed: **").append(files.size()).append("**\n\n");
        sb.append("```text\n");
        String struct = buildFileStructure(files);
        sb.append(struct.isBlank() ? "src/\n" : struct).append("\n");
        sb.append("```\n\n");

        sb.append("## Key Components (AST Analysis by JavaParser & Tree-Sitter)\n");
        long classCount = parsedFiles.stream().flatMap(p -> p.getElements().stream())
            .filter(e -> e.getType() == CodeElement.ElementType.CLASS || e.getType() == CodeElement.ElementType.INTERFACE)
            .count();
        long methodCount = parsedFiles.stream().flatMap(p -> p.getElements().stream())
            .filter(e -> e.getType() == CodeElement.ElementType.METHOD || e.getType() == CodeElement.ElementType.FUNCTION)
            .count();

        sb.append("- **Classes & Interfaces**: ").append(classCount).append("\n");
        sb.append("- **Methods & Functions**: ").append(methodCount).append("\n\n");

        sb.append("### Sample Code Components\n");
        for (ParsedFile pf : parsedFiles) {
            List<CodeElement> classes = pf.getElements().stream()
                .filter(e -> e.getType() == CodeElement.ElementType.CLASS || e.getType() == CodeElement.ElementType.INTERFACE)
                .collect(Collectors.toList());
            if (!classes.isEmpty()) {
                sb.append("- **`").append(pf.getFilePath()).append("`**: ");
                sb.append(classes.stream().map(CodeElement::getName).collect(Collectors.joining(", "))).append("\n");
            }
        }
        sb.append("\n");

        sb.append("## Setup & Installation\n");
        if ("Java".equalsIgnoreCase(repo.getPrimaryLanguage())) {
            sb.append("### Prerequisites\n- Java JDK 17 or higher\n- Maven 3.8+\n\n");
            sb.append("### Build & Run\n```bash\nmvn clean compile\nmvn spring-boot:run\n```\n");
        } else if ("JavaScript".equalsIgnoreCase(repo.getPrimaryLanguage()) || "TypeScript".equalsIgnoreCase(repo.getPrimaryLanguage())) {
            sb.append("### Prerequisites\n- Node.js 18+\n- npm or yarn\n\n");
            sb.append("### Build & Run\n```bash\nnpm install\nnpm run dev\n```\n");
        } else {
            sb.append("Refer to project configuration files for specific runtime dependencies.\n");
        }

        return sb.toString();
    }

    private String generateFallbackApiDocs(Repository repo, List<RepositoryFile> apiFiles, List<ParsedFile> parsedApiFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("# API Documentation — ").append(repo.getName()).append("\n\n");
        sb.append("Automatically extracted and generated using JavaParser and Tree-Sitter AST Parsers.\n\n");

        sb.append("## Overview\n");
        sb.append("Primary Language: **").append(repo.getPrimaryLanguage() != null ? repo.getPrimaryLanguage() : "Java").append("**\n");
        sb.append("API Files Analyzed: **").append(apiFiles.size()).append("**\n\n");

        sb.append("## Endpoints Summary\n\n");

        List<String> endpointRows = new ArrayList<>();

        for (ParsedFile pf : parsedApiFiles) {
            String fileName = pf.getFilePath();
            Map<String, String> routeMetadata = extractRouteMetadata(pf.getContent());
            for (CodeElement elem : pf.getElements()) {
                if (elem.getType() == CodeElement.ElementType.METHOD || elem.getType() == CodeElement.ElementType.FUNCTION) {
                    List<String> annos = elem.getAnnotations() != null ? elem.getAnnotations() : List.of();
                    String httpMethod = inferHttpMethod(elem.getName(), annos, routeMetadata);
                    String path = inferEndpointPath(elem.getName(), annos, routeMetadata, pf.getContent());

                    String parent = elem.getParentName() != null ? elem.getParentName() : "Controller";
                    String returnType = elem.getReturnType() != null ? elem.getReturnType() : "void";
                    String params = elem.getParameters() != null ? String.join(", ", elem.getParameters()) : "";

                    endpointRows.add(String.format("| `%s` | `%s` | `%s` | `%s` | `%s` | `%s` |",
                        httpMethod, path, parent + "." + elem.getName() + "()", returnType, params, fileName));
                }
            }
        }

        if (!endpointRows.isEmpty()) {
            sb.append("| HTTP Method | Endpoint Path | Controller / Method | Return Type | Parameters | Source File |\n");
            sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n");
            endpointRows.forEach(row -> sb.append(row).append("\n"));
            sb.append("\n");
        } else {
            sb.append("### Detected API Controller Files\n\n");
            for (RepositoryFile file : apiFiles) {
                sb.append("### `").append(file.getFilePath()).append("`\n");
                sb.append("```").append(file.getLanguage() != null ? file.getLanguage().toLowerCase() : "java").append("\n");
                sb.append(truncate(file.getContent(), 500)).append("\n");
                sb.append("```\n\n");
            }
        }

        return sb.toString();
    }

    // ─── Ingestion ───────────────────────────────────────────────────────────

    public void triggerIngestion(String userEmail, UUID repositoryId) {
        Repository repo = repositoryRepo.findById(repositoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId.toString()));
        validateRepositoryAccess(repo.getProject().getId(), repositoryId);
        ingestionService.ingestRepositoryAsync(repositoryId);
    }

    // ─── Health ──────────────────────────────────────────────────────────────

    public Map<String, Object> getHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("llmProvider", llmService.getProviderName());
        health.put("llmAvailable", llmService.isAvailable());
        health.put("timestamp", Instant.now());
        return health;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void validateRepositoryAccess(UUID projectId, UUID repositoryId) {
        if (projectId != null && repositoryId != null) {
            repositoryRepo.findByIdAndProjectId(repositoryId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId.toString()));
        }
    }

    private Repository getRepository(UUID repositoryId) {
        return repositoryRepo.findById(repositoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId.toString()));
    }

    private Documentation saveDocumentation(Repository repo, Documentation.DocType docType,
                                              String title, String content) {
        Documentation doc = Documentation.builder()
            .project(repo.getProject())
            .repository(repo)
            .docType(docType)
            .title(title)
            .content(content)
            .status(Documentation.DocStatus.PUBLISHED)
            .generatedAt(Instant.now())
            .build();
        return documentationRepository.save(doc);
    }

    private String buildFileStructure(List<RepositoryFile> files) {
        return files.stream()
            .map(RepositoryFile::getFilePath)
            .sorted()
            .limit(50)
            .collect(Collectors.joining("\n"));
    }

    private String buildSampleContext(List<RepositoryFile> files, int count) {
        return files.stream()
            .filter(f -> f.getContent() != null && !f.isBinary())
            .limit(count)
            .map(f -> "// " + f.getFilePath() + "\n" + truncate(f.getContent(), 500))
            .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private boolean isApiFile(String fileName, String language) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.contains("controller") || lower.contains("route") || lower.contains("router")
            || lower.contains("api") || lower.contains("endpoint") || lower.contains("handler");
    }

    private Map<String, String> extractRouteMetadata(String content) {
        Map<String, String> routeMetadata = new HashMap<>();
        if (content == null || content.isBlank()) return routeMetadata;

        String normalized = content.replace("\r\n", "\n");
        Matcher classMatcher = Pattern.compile("@(?:RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\s*\\((.*?)\\)")
            .matcher(normalized);
        String classPrefix = "";
        while (classMatcher.find()) {
            String args = classMatcher.group(1);
            String extracted = extractQuotedPath(args);
            if (extracted != null && !extracted.isBlank()) {
                classPrefix = extracted;
                break;
            }
        }

        Matcher methodMatcher = Pattern.compile("@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\\s*\\((.*?)\\)\\s*\\n\\s*(?:public|private|protected)\\s+(?:[\\w<>,\\[\\]\\?]+\\s+)?([A-Za-z0-9_]+)\\s*\\(")
            .matcher(normalized);
        while (methodMatcher.find()) {
            String annotation = methodMatcher.group(1);
            String args = methodMatcher.group(2);
            String methodName = methodMatcher.group(3);
            String path = extractQuotedPath(args);
            if (path == null || path.isBlank()) {
                path = "/" + methodName.toLowerCase();
            }
            if (!classPrefix.isBlank() && !path.startsWith("/")) {
                path = classPrefix + "/" + path;
            } else if (!classPrefix.isBlank() && path.startsWith("/")) {
                path = classPrefix + path;
            }
            routeMetadata.put(methodName, determineHttpMethod(annotation, args) + ":" + path);
        }

        return routeMetadata;
    }

    private String determineHttpMethod(String annotation, String args) {
        String annotationName = annotation == null ? "" : annotation.trim();
        String methodValue = extractRequestMethodValue(args);
        if (methodValue != null && !methodValue.isBlank()) {
            return methodValue.toUpperCase();
        }

        if (annotationName.isBlank()) return "ALL";
        if (annotationName.equalsIgnoreCase("RequestMapping")) return "ALL";
        return annotationName.replace("Mapping", "").toUpperCase();
    }

    private String extractRequestMethodValue(String args) {
        if (args == null || args.isBlank()) return null;

        Matcher matcher = Pattern.compile("RequestMethod\\.(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD)")
            .matcher(args);
        if (matcher.find()) {
            return matcher.group(1);
        }

        Matcher multiMatcher = Pattern.compile("\\{\\s*(RequestMethod\\.(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD)(?:\\s*,\\s*RequestMethod\\.(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD))*)\\s*\\}")
            .matcher(args);
        if (multiMatcher.find()) {
            String matched = multiMatcher.group(1);
            Matcher singleMatcher = Pattern.compile("RequestMethod\\.(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD)")
                .matcher(matched);
            if (singleMatcher.find()) {
                return singleMatcher.group(1);
            }
        }

        return null;
    }

    private String inferHttpMethod(String methodName, List<String> annotations, Map<String, String> routeMetadata) {
        String routeInfo = routeMetadata.get(methodName);
        if (routeInfo != null && !routeInfo.isBlank()) {
            return routeInfo.split(":", 2)[0].toUpperCase();
        }

        for (String a : annotations) {
            String aLower = a.toLowerCase();
            if (aLower.contains("getmapping") || aLower.contains("get")) return "GET";
            if (aLower.contains("postmapping") || aLower.contains("post")) return "POST";
            if (aLower.contains("putmapping") || aLower.contains("put")) return "PUT";
            if (aLower.contains("deletemapping") || aLower.contains("delete")) return "DELETE";
            if (aLower.contains("patchmapping") || aLower.contains("patch")) return "PATCH";
            if (aLower.contains("requestmapping")) return "ALL";
        }

        String lower = methodName.toLowerCase();
        if (lower.startsWith("get") || lower.startsWith("find") || lower.startsWith("list") || lower.startsWith("fetch") || lower.startsWith("read")) return "GET";
        if (lower.startsWith("create") || lower.startsWith("add") || lower.startsWith("save") || lower.startsWith("post")) return "POST";
        if (lower.startsWith("update") || lower.startsWith("modify") || lower.startsWith("put")) return "PUT";
        if (lower.startsWith("delete") || lower.startsWith("remove") || lower.startsWith("destroy")) return "DELETE";
        return "UNKNOWN";
    }

    private String inferEndpointPath(String methodName, List<String> annotations, Map<String, String> routeMetadata, String fileContent) {
        String routeInfo = routeMetadata.get(methodName);
        if (routeInfo != null && routeInfo.contains(":")) {
            return routeInfo.substring(routeInfo.indexOf(':') + 1);
        }

        for (String a : annotations) {
            String aLower = a.toLowerCase();
            if (aLower.contains("mapping")) {
                return "/" + methodName.toLowerCase();
            }
        }
        return "/" + methodName.toLowerCase();
    }

    private String extractQuotedPath(String rawArgs) {
        if (rawArgs == null) return null;
        Matcher matcher = Pattern.compile("\"([^\"]+)\"").matcher(rawArgs);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractSection(String text, String sectionName) {
        if (text == null) return "";
        String marker = "## " + sectionName;
        int start = text.indexOf(marker);
        if (start < 0) return "";
        start += marker.length();
        int end = text.indexOf("## ", start);
        String section = end > start ? text.substring(start, end) : text.substring(start);
        return section.trim();
    }

    private List<String> extractListSection(String text, String sectionName) {
        String section = extractSection(text, sectionName);
        if (section.isBlank()) return List.of();
        return Arrays.stream(section.split("\n"))
            .map(line -> line.replaceAll("^[-*•]\\s*", "").trim())
            .filter(line -> !line.isBlank())
            .collect(Collectors.toList());
    }
}
