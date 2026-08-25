package com.codesense.parser.language;

/**
 * Enumeration of all languages supported by the parser module.
 * Team Member 4 (Prashanthi) owns this enum.
 *
 * JavaParser handles: JAVA
 * Tree-sitter handles: all other languages
 * Generic fallback handles: anything else
 */
public enum SupportedLanguage {

    JAVA("Java", ".java", ParserType.JAVAPARSER),
    PYTHON("Python", ".py", ParserType.TREE_SITTER),
    JAVASCRIPT("JavaScript", ".js", ParserType.TREE_SITTER),
    TYPESCRIPT("TypeScript", ".ts", ParserType.TREE_SITTER),
    C("C", ".c", ParserType.TREE_SITTER),
    CPP("C++", ".cpp", ParserType.TREE_SITTER),
    CSHARP("C#", ".cs", ParserType.TREE_SITTER),
    GO("Go", ".go", ParserType.TREE_SITTER),
    RUST("Rust", ".rs", ParserType.TREE_SITTER),
    PHP("PHP", ".php", ParserType.TREE_SITTER),
    RUBY("Ruby", ".rb", ParserType.TREE_SITTER),
    KOTLIN("Kotlin", ".kt", ParserType.TREE_SITTER),
    SWIFT("Swift", ".swift", ParserType.TREE_SITTER);

    private final String displayName;
    private final String primaryExtension;
    private final ParserType parserType;

    SupportedLanguage(String displayName, String primaryExtension, ParserType parserType) {
        this.displayName = displayName;
        this.primaryExtension = primaryExtension;
        this.parserType = parserType;
    }

    public String getDisplayName() { return displayName; }
    public String getPrimaryExtension() { return primaryExtension; }
    public ParserType getParserType() { return parserType; }

    public static SupportedLanguage fromDisplayName(String name) {
        if (name == null) return null;
        for (SupportedLanguage lang : values()) {
            if (lang.displayName.equalsIgnoreCase(name)) return lang;
        }
        return null;
    }

    public enum ParserType {
        JAVAPARSER, TREE_SITTER, GENERIC
    }
}
