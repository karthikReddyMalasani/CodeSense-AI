package com.codesense.parser.service;

import com.codesense.parser.core.CodeParser;
import com.codesense.parser.core.GenericCodeParser;
import com.codesense.parser.core.JavaParserCodeParser;
import com.codesense.parser.core.TreeSitterCodeParser;
import com.codesense.parser.model.CodeMetrics;
import com.codesense.parser.model.ParsedFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Parser router — selects the appropriate CodeParser implementation based on language.
 * Team Member 4 (Prashanthi) owns this class.
 *
 * Routing rules:
 * Java          → JavaParserCodeParser
 * All other supported languages → TreeSitterCodeParser
 * Everything else → GenericCodeParser
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParserRouter {

    private final JavaParserCodeParser javaParser;
    private final TreeSitterCodeParser treeSitterParser;
    private final GenericCodeParser genericParser;

    /**
     * Select parser by language and parse the given file content.
     */
    public ParsedFile parse(String filePath, String content, String language) {
        CodeParser parser = selectParser(language);
        log.debug("Parsing {} ({}) with {}", filePath, language, parser.getClass().getSimpleName());
        return parser.parse(filePath, content, language);
    }

    /**
     * Calculate metrics for a file using the appropriate parser.
     */
    public CodeMetrics calculateMetrics(String filePath, String content, String language) {
        CodeParser parser = selectParser(language);
        return parser.calculateMetrics(filePath, content, language);
    }

    private CodeParser selectParser(String language) {
        if (language == null) return genericParser;
        if (javaParser.supports(language)) return javaParser;
        if (treeSitterParser.supports(language)) return treeSitterParser;
        return genericParser;
    }

    public List<String> getAllSupportedLanguages() {
        return List.of(
            "Java", "Python", "JavaScript", "TypeScript",
            "C", "C++", "C#", "Go", "Rust", "PHP",
            "Ruby", "Kotlin", "Swift"
        );
    }
}
