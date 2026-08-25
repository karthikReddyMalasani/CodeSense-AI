package com.codesense.parser.core;

import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.CodeMetrics;
import com.codesense.parser.model.CodeRelationship;
import com.codesense.parser.model.ParsedFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Generic code parser — fallback for unsupported languages.
 * Team Member 4 (Prashanthi) owns this class.
 *
 * Provides basic file-level metadata without deep parsing.
 */
@Slf4j
@Component
public class GenericCodeParser implements CodeParser {

    @Override
    public List<String> getSupportedLanguages() {
        return List.of(); // handles everything not claimed by other parsers
    }

    @Override
    public boolean supports(String language) {
        return true; // fallback — supports all languages
    }

    @Override
    public ParsedFile parse(String filePath, String content, String language) {
        String name = filePath.contains("/")
            ? filePath.substring(filePath.lastIndexOf('/') + 1)
            : filePath;

        CodeElement fileElement = CodeElement.builder()
            .name(name)
            .type(CodeElement.ElementType.MODULE)
            .language(language != null ? language : "Unknown")
            .filePath(filePath)
            .startLine(1)
            .endLine(content != null ? content.split("\n", -1).length : 0)
            .build();

        return ParsedFile.builder()
            .filePath(filePath)
            .language(language)
            .content(content)
            .lineCount(content != null ? content.split("\n", -1).length : 0)
            .elements(List.of(fileElement))
            .relationships(List.of())
            .metadata(Map.of("parser", "generic"))
            .build();
    }

    @Override
    public CodeMetrics calculateMetrics(String filePath, String content, String language) {
        if (content == null) return CodeMetrics.builder().filePath(filePath).language(language).build();
        String[] lines = content.split("\n", -1);
        int blank = (int) java.util.Arrays.stream(lines).filter(l -> l.trim().isEmpty()).count();
        return CodeMetrics.builder()
            .filePath(filePath).language(language)
            .totalLines(lines.length).blankLines(blank)
            .codeLines(lines.length - blank)
            .build();
    }
}
