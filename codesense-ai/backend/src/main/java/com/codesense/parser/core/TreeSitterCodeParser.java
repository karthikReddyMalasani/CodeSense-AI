package com.codesense.parser.core;

import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.CodeMetrics;
import com.codesense.parser.model.CodeRelationship;
import com.codesense.parser.model.ParsedFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tree-sitter-based code parser for multi-language code intelligence.
 * Delegates native AST parsing to TreeSitterNativeEngine and regex parsing to RegexFallbackParser.
 * Follows SOLID Principles (SRP, OCP, LSP, ISP, DIP).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TreeSitterCodeParser implements CodeParser {

    private static final List<String> SUPPORTED_LANGUAGES = List.of(
        "Python", "JavaScript", "TypeScript", "C", "C++", "C#",
        "Go", "Rust", "PHP", "Ruby", "Kotlin", "Swift"
    );

    private final TreeSitterNativeEngine nativeEngine;
    private final RegexFallbackParser fallbackParser;

    @Override
    public List<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    @Override
    public ParsedFile parse(String filePath, String content, String language) {
        if (content == null || content.isBlank()) {
            return ParsedFile.builder()
                .filePath(filePath)
                .language(language != null ? language : "unknown")
                .content(content != null ? content : "")
                .lineCount(0)
                .elements(List.of())
                .relationships(List.of())
                .metadata(Map.of("parser", "empty-content", "status", "EMPTY"))
                .build();
        }

        List<CodeElement> elements = new ArrayList<>();
        List<CodeRelationship> relationships = new ArrayList<>();
        String parserUsed = "regex-fallback";

        try {
            boolean astParsed = nativeEngine.parseAST(filePath, content, language, elements, relationships);
            if (astParsed && !elements.isEmpty()) {
                parserUsed = "tree-sitter-native";
            } else {
                elements.clear();
                relationships.clear();
                fallbackParser.parse(filePath, content, language, elements, relationships);
                parserUsed = "regex-fallback";
            }
        } catch (Throwable ex) {
            log.warn("[TreeSitterCodeParser] Unexpected error parsing {} ({}): {} — using fallback",
                filePath, language, ex.getMessage());
            elements.clear();
            relationships.clear();
            try {
                fallbackParser.parse(filePath, content, language, elements, relationships);
            } catch (Throwable ignored) {
                // Ensure LSP: never throw exceptions to caller
            }
            parserUsed = "safe-fallback";
        }

        return ParsedFile.builder()
            .filePath(filePath)
            .language(language != null ? language : "unknown")
            .content(content)
            .lineCount(countLines(content))
            .elements(elements)
            .relationships(relationships)
            .metadata(Map.of("parser", parserUsed, "language", language != null ? language : "unknown"))
            .build();
    }

    @Override
    public CodeMetrics calculateMetrics(String filePath, String content, String language) {
        if (content == null) content = "";
        String[] lines = content.split("\n", -1);
        int totalLines = lines.length;
        int codeLines = 0;
        int commentLines = 0;
        int blankLines = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                blankLines++;
            } else if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                commentLines++;
            } else {
                codeLines++;
            }
        }

        ParsedFile parsed = parse(filePath, content, language);
        int classCount = (int) parsed.getElements().stream().filter(e -> e.getType() == CodeElement.ElementType.CLASS).count();
        int methodCount = (int) parsed.getElements().stream().filter(e -> e.getType() == CodeElement.ElementType.METHOD || e.getType() == CodeElement.ElementType.FUNCTION).count();

        return CodeMetrics.builder()
            .filePath(filePath)
            .language(language != null ? language : "unknown")
            .totalLines(totalLines)
            .codeLines(codeLines)
            .commentLines(commentLines)
            .blankLines(blankLines)
            .classCount(classCount)
            .methodCount(methodCount)
            .cyclomaticComplexity(Math.max(1, methodCount))
            .build();
    }

    private int countLines(String str) {
        if (str == null || str.isEmpty()) return 0;
        return str.split("\n", -1).length;
    }
}
