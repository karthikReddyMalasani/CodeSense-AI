package com.codesense.parser.core;

import com.codesense.parser.model.ParsedFile;
import com.codesense.parser.model.CodeMetrics;

import java.util.List;

/**
 * Parser interface — Team Member 4 (Prashanthi) owns this interface.
 *
 * Implementations:
 * - JavaParserCodeParser  — for Java source files
 * - TreeSitterCodeParser  — for Python, JavaScript, TypeScript, etc.
 * - GenericCodeParser     — fallback for unsupported languages
 *
 * The parser router selects the appropriate implementation based on language.
 */
public interface CodeParser {

    /**
     * Parse the given source code and return a structured ParsedFile.
     *
     * @param filePath  relative file path (for metadata)
     * @param content   source code content
     * @param language  detected language
     * @return structured parsed representation of the file
     */
    ParsedFile parse(String filePath, String content, String language);

    /**
     * Calculate code metrics for the given content.
     *
     * @param filePath  relative path
     * @param content   source code
     * @param language  detected language
     * @return metrics object
     */
    CodeMetrics calculateMetrics(String filePath, String content, String language);

    /**
     * Return the list of languages this parser supports.
     * Used by the parser router to select the right implementation.
     */
    List<String> getSupportedLanguages();

    /**
     * Whether this parser can handle the given language.
     */
    default boolean supports(String language) {
        return language != null && getSupportedLanguages().contains(language);
    }
}
