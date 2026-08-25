package com.codesense.parser.core;

import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.CodeMetrics;
import com.codesense.parser.model.CodeRelationship;
import com.codesense.parser.model.ParsedFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.treesitter.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tree-sitter-based code parser for multi-language support.
 * Team Member 4 (Prashanthi) owns this class.
 *
 * IMPORTANT NOTE on Tree-sitter Java bindings:
 * ─────────────────────────────────────────────
 * Tree-sitter's official Java binding (JTreeSitter) requires JDK 19+ (Panama FFI).
 * Since this project targets Java 21, the binding is compatible.
 *
 * However, tree-sitter grammar JARs are not available on Maven Central as of the
 * time of implementation. The org.treesitter artifacts (tree-sitter-java, etc.)
 * require native compilation or a JNI bridge.
 *
 * IMPLEMENTATION STRATEGY:
 * This class uses REGEX-BASED parsing as the primary strategy, which:
 * 1. Works cross-platform without native dependencies
 * 2. Handles the most common code patterns reliably
 * 3. Can be replaced with actual Tree-sitter bindings when stable Maven artifacts exist
 *
 * The parser architecture is designed so Team Member 4 can swap in real
 * Tree-sitter bindings by modifying only this class, keeping the interface intact.
 *
 * Supported languages: Python, JavaScript, TypeScript, C, C++, C#, Go, Rust, PHP, Ruby, Kotlin, Swift
 */
@Slf4j
@Component
public class TreeSitterCodeParser implements CodeParser {

    private static final List<String> SUPPORTED = List.of(
        "Python", "JavaScript", "TypeScript", "C", "C++", "C#",
        "Go", "Rust", "PHP", "Ruby", "Kotlin", "Swift"
    );

    // ── Tree-sitter JNI engine ────────────────────────────────────────────────
    // Languages are loaded once at class initialisation. If native loading fails
    // (e.g. unsupported architecture) the boolean flag disables JNI for that
    // language and the existing regex path is used transparently as fallback.

    private static final Map<String, TSLanguage> TS_LANGUAGES = new HashMap<>();
    private static boolean tsAvailable = false;

    static {
        try {
            TS_LANGUAGES.put("Python",     new TreeSitterPython());
            TS_LANGUAGES.put("JavaScript", new TreeSitterJavascript());
            tsAvailable = true;
            log.info("[TreeSitter] Native JNI loaded for: {}", TS_LANGUAGES.keySet());
        } catch (Throwable ex) {
            log.warn("[TreeSitter] Native JNI unavailable — using regex fallback. ({})", ex.getMessage());
        }
    }

    @Override
    public List<String> getSupportedLanguages() {
        return SUPPORTED;
    }

    @Override
    public ParsedFile parse(String filePath, String content, String language) {
        if (content == null || content.isBlank()) {
            return ParsedFile.builder()
                .filePath(filePath).language(language).content(content)
                .lineCount(0).elements(List.of()).relationships(List.of())
                .metadata(Map.of()).build();
        }

        List<CodeElement> elements = new ArrayList<>();
        List<CodeRelationship> relationships = new ArrayList<>();
        String parserUsed = "regex-based";

        // Attempt real tree-sitter AST parse for supported languages
        if (tsAvailable && TS_LANGUAGES.containsKey(language)) {
            try {
                parseWithTreeSitter(filePath, content, language, elements, relationships);
                parserUsed = "tree-sitter";
            } catch (Exception ex) {
                log.warn("[TreeSitter] AST parse failed for {} ({}): {} — falling back to regex",
                    filePath, language, ex.getMessage());
                elements.clear(); relationships.clear();
                parseWithRegex(filePath, content, language, elements, relationships);
                parserUsed = "regex-fallback";
            }
        } else {
            parseWithRegex(filePath, content, language, elements, relationships);
        }

        return ParsedFile.builder()
            .filePath(filePath).language(language)
            .content(content).lineCount(countLines(content))
            .elements(elements).relationships(relationships)
            .metadata(Map.of("parser", parserUsed, "language", language != null ? language : "unknown"))
            .build();
    }

    // ── Real Tree-sitter AST parsing ──────────────────────────────────────────

    private void parseWithTreeSitter(String filePath, String content, String language,
                                      List<CodeElement> elements, List<CodeRelationship> relationships) {
        TSLanguage tsLang = TS_LANGUAGES.get(language);
        try (TSParser parser = new TSParser()) {
            parser.setLanguage(tsLang);
            try (TSTree tree = parser.parseString(null, content)) {
                TSNode root = tree.getRootNode();
                walkNode(root, content, filePath, language, elements, relationships, null);
            }
        }
    }

    /**
     * Recursively walk tree-sitter AST nodes and emit CodeElements / CodeRelationships.
     */
    private void walkNode(TSNode node, String src, String filePath, String language,
                          List<CodeElement> elements, List<CodeRelationship> relationships,
                          String currentClass) {
        if (node == null || node.isNull()) return;
        String type = node.getType();
        String nextClass = currentClass;

        switch (type) {
            // ── Class-like ────────────────────────────────────────────────────
            case "class_definition",         // Python
                 "class_declaration",         // JS/TS
                 "abstract_class_declaration",
                 "struct_type",               // Go
                 "struct_item" -> {           // Rust
                String name = childValue(node, src, "name", "identifier");
                if (name != null) {
                    nextClass = name;
                    elements.add(CodeElement.builder()
                        .name(name).type(CodeElement.ElementType.CLASS)
                        .language(language).filePath(filePath)
                        .startLine(node.getStartPoint().getRow() + 1)
                        .endLine(node.getEndPoint().getRow() + 1).build());
                    // superclass / base types
                    String parent = childValue(node, src, "superclasses", "base_class",
                        "extends_type_list", "extends_clause");
                    if (parent != null) {
                        relationships.add(CodeRelationship.builder()
                            .sourceElement(name).targetElement(parent)
                            .type(CodeRelationship.RelationshipType.EXTENDS)
                            .sourceFile(filePath).sourceLine(node.getStartPoint().getRow() + 1).build());
                    }
                }
            }

            // ── Interface ──────────────────────────────────────────────────────
            case "interface_declaration" -> {  // TS
                String name = childValue(node, src, "name", "identifier");
                if (name != null) {
                    elements.add(CodeElement.builder()
                        .name(name).type(CodeElement.ElementType.INTERFACE)
                        .language(language).filePath(filePath)
                        .startLine(node.getStartPoint().getRow() + 1).build());
                }
            }

            // ── Function / Method ───────────────────────────────────────────────
            case "function_definition",         // Python / Rust
                 "function_declaration",         // JS / TS / Go
                 "function_item",                // Rust
                 "method_definition",            // JS class methods
                 "method_declaration" -> {        // TS class methods
                String name = childValue(node, src, "name", "identifier");
                if (name != null) {
                    boolean isMethod = currentClass != null;
                    elements.add(CodeElement.builder()
                        .name(name)
                        .type(isMethod ? CodeElement.ElementType.METHOD : CodeElement.ElementType.FUNCTION)
                        .language(language).filePath(filePath)
                        .startLine(node.getStartPoint().getRow() + 1)
                        .endLine(node.getEndPoint().getRow() + 1)
                        .parentName(currentClass).build());
                }
            }

            // ── Arrow function (const foo = () => {}) ─────────────────────────
            case "lexical_declaration", "variable_declaration" -> {
                String name = childValue(node, src, "name", "identifier");
                // Only emit if right-hand side is an arrow_function
                if (name != null && nodeBodyContains(node, "arrow_function")) {
                    elements.add(CodeElement.builder()
                        .name(name).type(CodeElement.ElementType.FUNCTION)
                        .language(language).filePath(filePath)
                        .startLine(node.getStartPoint().getRow() + 1).build());
                }
            }

            // ── Imports ──────────────────────────────────────────────────────
            case "import_statement",      // Python
                 "import_from_statement", // Python
                 "import_declaration" -> { // JS/TS
                String target = importTarget(node, src);
                if (target != null) {
                    relationships.add(CodeRelationship.builder()
                        .sourceElement(filePath).targetElement(target)
                        .type(CodeRelationship.RelationshipType.IMPORTS)
                        .sourceFile(filePath).sourceLine(node.getStartPoint().getRow() + 1).build());
                }
            }

            // ── Go: function declaration form ─────────────────────────────────
            case "source_file",           // root — just recurse
                 "type_declaration",      // Go type ...
                 "program",              // JS root
                 "module" -> { /* fall through, recurse */ }

            default -> { /* ignore leaf / noise nodes */ }
        }

        // Recurse into children
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            TSNode child = node.getChild(i);
            walkNode(child, src, filePath, language, elements, relationships, nextClass);
        }
    }

    /** Extract text of the first child node whose type matches any of the given types. */
    private String childValue(TSNode node, String src, String... childTypes) {
        Set<String> set = new HashSet<>(Arrays.asList(childTypes));
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            TSNode child = node.getChild(i);
            if (set.contains(child.getType())) {
                return nodeText(child, src);
            }
        }
        return null;
    }

    /** Check whether any direct child has the given type. */
    private boolean nodeBodyContains(TSNode node, String nodeType) {
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            if (nodeType.equals(node.getChild(i).getType())) return true;
        }
        return false;
    }

    /** Extract the import module name from an import node. */
    private String importTarget(TSNode node, String src) {
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            TSNode child = node.getChild(i);
            String t = child.getType();
            if (t.equals("string") || t.equals("dotted_name") || t.equals("relative_import")
                    || t.equals("module_name") || t.equals("identifier")) {
                String text = nodeText(child, src);
                if (text != null) return text.replace("'", "").replace("\"", "");
            }
        }
        return null;
    }

    /** Slice the source text for a given node using [startByte, endByte). */
    private String nodeText(TSNode node, String src) {
        try {
            int start = node.getStartByte();
            int end   = node.getEndByte();
            if (start < 0 || end > src.length() || start >= end) return null;
            return src.substring(start, end).trim();
        } catch (Exception e) {
            return null;
        }
    }

    // ── Regex-based dispatch (original implementation, preserved as fallback) ─

    private void parseWithRegex(String filePath, String content, String language,
                                List<CodeElement> elements, List<CodeRelationship> relationships) {
        try {
            switch (language != null ? language : "") {
                case "Python"     -> parsePython(filePath, content, elements, relationships);
                case "JavaScript" -> parseJavaScript(filePath, content, elements, relationships);
                case "TypeScript" -> parseTypeScript(filePath, content, elements, relationships);
                case "C", "C++"  -> parseC(filePath, content, language, elements, relationships);
                case "C#"         -> parseCSharp(filePath, content, elements, relationships);
                case "Go"         -> parseGo(filePath, content, elements, relationships);
                case "Rust"       -> parseRust(filePath, content, elements, relationships);
                case "PHP"        -> parsePHP(filePath, content, elements, relationships);
                case "Ruby"       -> parseRuby(filePath, content, elements, relationships);
                case "Kotlin"     -> parseKotlin(filePath, content, elements, relationships);
                case "Swift"      -> parseSwift(filePath, content, elements, relationships);
                default           -> parseGeneric(filePath, content, language, elements);
            }
        } catch (Exception e) {
            log.warn("Regex parser error for {} ({}): {}", filePath, language, e.getMessage());
        }
    }

    // ─── Python ──────────────────────────────────────────────────────────────

    private void parsePython(String filePath, String content,
                              List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);

        // class_definition
        Pattern classPattern = Pattern.compile("^(class)\\s+(\\w+)(?:\\(([^)]+)\\))?\\s*:");
        // function_definition
        Pattern funcPattern = Pattern.compile("^(\\s*)(async\\s+)?def\\s+(\\w+)\\s*\\(([^)]*)\\)");
        // import_statement
        Pattern importPattern = Pattern.compile("^(?:from\\s+([\\w.]+)\\s+)?import\\s+([\\w., *]+)");
        // decorator
        Pattern decoratorPattern = Pattern.compile("^\\s*@(\\w+)");

        String currentClass = null;
        String pendingDecorator = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String stripped = line.trim();

            Matcher decorMatcher = decoratorPattern.matcher(stripped);
            if (decorMatcher.find()) {
                pendingDecorator = decorMatcher.group(1);
                continue;
            }

            Matcher classMatcher = classPattern.matcher(stripped);
            if (classMatcher.find()) {
                String name = classMatcher.group(2);
                currentClass = name;
                String parent = classMatcher.group(3);
                CodeElement.ElementType type = CodeElement.ElementType.CLASS;
                List<String> annotations = pendingDecorator != null ? List.of(pendingDecorator) : List.of();
                pendingDecorator = null;

                elements.add(CodeElement.builder()
                    .name(name).type(type).language("Python")
                    .filePath(filePath).startLine(i + 1)
                    .annotations(annotations)
                    .metadata(parent != null ? Map.of("extends", parent) : Map.of())
                    .build());

                if (parent != null) {
                    for (String p : parent.split(",")) {
                        relationships.add(CodeRelationship.builder()
                            .sourceElement(name).targetElement(p.trim())
                            .type(CodeRelationship.RelationshipType.EXTENDS)
                            .sourceFile(filePath).sourceLine(i + 1).build());
                    }
                }
                continue;
            }

            Matcher funcMatcher = funcPattern.matcher(line);
            if (funcMatcher.find()) {
                String indent = funcMatcher.group(1);
                String name = funcMatcher.group(3);
                boolean isMethod = indent.length() > 0 && currentClass != null;
                List<String> annotations = pendingDecorator != null ? List.of(pendingDecorator) : List.of();
                pendingDecorator = null;

                elements.add(CodeElement.builder()
                    .name(name)
                    .type(isMethod ? CodeElement.ElementType.METHOD : CodeElement.ElementType.FUNCTION)
                    .language("Python").filePath(filePath).startLine(i + 1)
                    .parentName(isMethod ? currentClass : null)
                    .annotations(annotations)
                    .build());
                continue;
            }

            Matcher importMatcher = importPattern.matcher(stripped);
            if (importMatcher.find()) {
                String module = importMatcher.group(1) != null ? importMatcher.group(1) : importMatcher.group(2).split(",")[0].trim();
                relationships.add(CodeRelationship.builder()
                    .sourceElement(filePath).targetElement(module)
                    .type(CodeRelationship.RelationshipType.IMPORTS)
                    .sourceFile(filePath).sourceLine(i + 1).build());
            }
            pendingDecorator = null;
        }
    }

    // ─── JavaScript ──────────────────────────────────────────────────────────

    private void parseJavaScript(String filePath, String content,
                                  List<CodeElement> elements, List<CodeRelationship> relationships) {
        parseJsTs(filePath, content, "JavaScript", elements, relationships);
    }

    private void parseTypeScript(String filePath, String content,
                                  List<CodeElement> elements, List<CodeRelationship> relationships) {
        parseJsTs(filePath, content, "TypeScript", elements, relationships);

        // TypeScript interfaces
        Pattern interfacePattern = Pattern.compile("(?:export\\s+)?interface\\s+(\\w+)(?:\\s+extends\\s+([\\w, ]+))?\\s*\\{");
        extractMatches(interfacePattern, content, filePath, "TypeScript",
            CodeElement.ElementType.INTERFACE, elements, null, 1);

        // TypeScript type aliases
        Pattern typePattern = Pattern.compile("(?:export\\s+)?type\\s+(\\w+)\\s*=");
        extractMatches(typePattern, content, filePath, "TypeScript",
            CodeElement.ElementType.CLASS, elements, "TypeAlias", 1);
    }

    private void parseJsTs(String filePath, String content, String language,
                            List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);

        Pattern classPattern = Pattern.compile("(?:export\\s+)?(?:default\\s+)?class\\s+(\\w+)(?:\\s+extends\\s+(\\w+))?");
        Pattern funcPattern = Pattern.compile("(?:export\\s+)?(?:default\\s+)?(?:async\\s+)?function(?:\\*)?\\s+(\\w+)\\s*\\(");
        Pattern arrowFuncPattern = Pattern.compile("(?:export\\s+)?(?:const|let|var)\\s+(\\w+)\\s*=\\s*(?:async\\s+)?(?:\\([^)]*\\)|\\w+)\\s*=>");
        Pattern methodPattern = Pattern.compile("^\\s+(?:async\\s+)?(?:static\\s+)?(?:get\\s+|set\\s+)?(\\w+)\\s*\\([^)]*\\)\\s*\\{");
        Pattern importPattern = Pattern.compile("import\\s+(?:[^;]+from\\s+)?['\"]([^'\"]+)['\"]");

        String currentClass = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher classMatcher = classPattern.matcher(line);
            if (classMatcher.find()) {
                currentClass = classMatcher.group(1);
                String parent = classMatcher.groupCount() >= 2 ? classMatcher.group(2) : null;
                elements.add(CodeElement.builder()
                    .name(currentClass).type(CodeElement.ElementType.CLASS)
                    .language(language).filePath(filePath).startLine(i + 1).build());
                if (parent != null) {
                    relationships.add(CodeRelationship.builder()
                        .sourceElement(currentClass).targetElement(parent)
                        .type(CodeRelationship.RelationshipType.EXTENDS)
                        .sourceFile(filePath).sourceLine(i + 1).build());
                }
                continue;
            }

            Matcher funcMatcher = funcPattern.matcher(line);
            if (funcMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(funcMatcher.group(1)).type(CodeElement.ElementType.FUNCTION)
                    .language(language).filePath(filePath).startLine(i + 1).build());
                continue;
            }

            Matcher arrowMatcher = arrowFuncPattern.matcher(line);
            if (arrowMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(arrowMatcher.group(1)).type(CodeElement.ElementType.FUNCTION)
                    .language(language).filePath(filePath).startLine(i + 1).build());
                continue;
            }

            Matcher methodMatcher = methodPattern.matcher(line);
            if (currentClass != null && methodMatcher.find()) {
                String name = methodMatcher.group(1);
                if (!name.equals("constructor") && !name.equals("if") && !name.equals("for") && !name.equals("while")) {
                    elements.add(CodeElement.builder()
                        .name(name).type(CodeElement.ElementType.METHOD)
                        .language(language).filePath(filePath).startLine(i + 1)
                        .parentName(currentClass).build());
                }
            }

            Matcher importMatcher = importPattern.matcher(line);
            if (importMatcher.find()) {
                relationships.add(CodeRelationship.builder()
                    .sourceElement(filePath).targetElement(importMatcher.group(1))
                    .type(CodeRelationship.RelationshipType.IMPORTS)
                    .sourceFile(filePath).sourceLine(i + 1).build());
            }
        }
    }

    // ─── C / C++ ─────────────────────────────────────────────────────────────

    private void parseC(String filePath, String content, String language,
                         List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern structPattern = Pattern.compile("(?:typedef\\s+)?(?:struct|union|enum)\\s+(\\w+)");
        Pattern funcPattern = Pattern.compile("^(?:(?:static|inline|extern|virtual|override)\\s+)*(?:\\w+[\\w*&<>\\s]+)\\s+(\\w+)\\s*\\([^)]*\\)\\s*(?:const\\s*)?\\{?\\s*$");
        Pattern includePattern = Pattern.compile("#include\\s*[<\"]([^>\"]+)[>\"]");
        Pattern classPattern = Pattern.compile("class\\s+(\\w+)(?:\\s*:\\s*(?:public|private|protected)\\s+(\\w+))?");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher classMatcher = classPattern.matcher(line);
            if (classMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(classMatcher.group(1)).type(CodeElement.ElementType.CLASS)
                    .language(language).filePath(filePath).startLine(i + 1).build());
                if (classMatcher.group(2) != null) {
                    relationships.add(CodeRelationship.builder()
                        .sourceElement(classMatcher.group(1)).targetElement(classMatcher.group(2))
                        .type(CodeRelationship.RelationshipType.EXTENDS)
                        .sourceFile(filePath).sourceLine(i + 1).build());
                }
                continue;
            }

            Matcher structMatcher = structPattern.matcher(line);
            if (structMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(structMatcher.group(1)).type(CodeElement.ElementType.STRUCT)
                    .language(language).filePath(filePath).startLine(i + 1).build());
                continue;
            }

            Matcher funcMatcher = funcPattern.matcher(line);
            if (funcMatcher.find()) {
                String name = funcMatcher.group(1);
                if (!isKeyword(name)) {
                    elements.add(CodeElement.builder()
                        .name(name).type(CodeElement.ElementType.FUNCTION)
                        .language(language).filePath(filePath).startLine(i + 1).build());
                }
            }

            Matcher includeMatcher = includePattern.matcher(line);
            if (includeMatcher.find()) {
                relationships.add(CodeRelationship.builder()
                    .sourceElement(filePath).targetElement(includeMatcher.group(1))
                    .type(CodeRelationship.RelationshipType.IMPORTS)
                    .sourceFile(filePath).sourceLine(i + 1).build());
            }
        }
    }

    // ─── C# ──────────────────────────────────────────────────────────────────

    private void parseCSharp(String filePath, String content,
                              List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern classPattern = Pattern.compile("(?:public|private|protected|internal|static|abstract|sealed|partial)?\\s*(?:class|interface|enum|struct)\\s+(\\w+)(?:\\s*:\\s*([\\w, ]+))?");
        Pattern methodPattern = Pattern.compile("(?:public|private|protected|internal|static|virtual|override|async)\\s+(?:[\\w<>\\[\\]]+\\s+)+(\\w+)\\s*\\([^)]*\\)");
        Pattern usingPattern = Pattern.compile("^using\\s+([\\w.]+);");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            Matcher usingMatcher = usingPattern.matcher(line);
            if (usingMatcher.find()) {
                relationships.add(CodeRelationship.builder()
                    .sourceElement(filePath).targetElement(usingMatcher.group(1))
                    .type(CodeRelationship.RelationshipType.IMPORTS)
                    .sourceFile(filePath).sourceLine(i + 1).build());
                continue;
            }

            Matcher classMatcher = classPattern.matcher(line);
            if (classMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(classMatcher.group(1))
                    .type(line.contains("interface") ? CodeElement.ElementType.INTERFACE : CodeElement.ElementType.CLASS)
                    .language("C#").filePath(filePath).startLine(i + 1).build());
                continue;
            }

            Matcher methodMatcher = methodPattern.matcher(line);
            if (methodMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(methodMatcher.group(1)).type(CodeElement.ElementType.METHOD)
                    .language("C#").filePath(filePath).startLine(i + 1).build());
            }
        }
    }

    // ─── Go ──────────────────────────────────────────────────────────────────

    private void parseGo(String filePath, String content,
                          List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern funcPattern = Pattern.compile("^func\\s+(?:\\([^)]+\\)\\s+)?(\\w+)\\s*\\(");
        Pattern typePattern = Pattern.compile("^type\\s+(\\w+)\\s+(struct|interface)");
        Pattern importPattern = Pattern.compile("\"([\\w./]+)\"");

        boolean inImportBlock = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.startsWith("import (")) { inImportBlock = true; continue; }
            if (inImportBlock && line.equals(")")) { inImportBlock = false; continue; }

            if (inImportBlock || line.startsWith("import ")) {
                Matcher m = importPattern.matcher(line);
                if (m.find()) {
                    relationships.add(CodeRelationship.builder()
                        .sourceElement(filePath).targetElement(m.group(1))
                        .type(CodeRelationship.RelationshipType.IMPORTS)
                        .sourceFile(filePath).sourceLine(i + 1).build());
                }
                continue;
            }

            Matcher funcMatcher = funcPattern.matcher(line);
            if (funcMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(funcMatcher.group(1)).type(CodeElement.ElementType.FUNCTION)
                    .language("Go").filePath(filePath).startLine(i + 1).build());
                continue;
            }

            Matcher typeMatcher = typePattern.matcher(line);
            if (typeMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(typeMatcher.group(1))
                    .type(typeMatcher.group(2).equals("interface") ? CodeElement.ElementType.INTERFACE : CodeElement.ElementType.STRUCT)
                    .language("Go").filePath(filePath).startLine(i + 1).build());
            }
        }
    }

    // ─── Rust ────────────────────────────────────────────────────────────────

    private void parseRust(String filePath, String content,
                            List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern structPattern = Pattern.compile("^(?:pub\\s+)?(?:struct|enum)\\s+(\\w+)");
        Pattern traitPattern = Pattern.compile("^(?:pub\\s+)?trait\\s+(\\w+)");
        Pattern implPattern = Pattern.compile("^(?:pub\\s+)?impl(?:<[^>]+>)?\\s+(?:(\\w+)\\s+for\\s+)?(\\w+)");
        Pattern fnPattern = Pattern.compile("^\\s*(?:pub\\s+)?(?:async\\s+)?fn\\s+(\\w+)\\s*(?:<[^>]+>)?\\s*\\(");
        Pattern usePattern = Pattern.compile("^use\\s+([\\w::{}, ]+);");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher useMatcher = usePattern.matcher(line.trim());
            if (useMatcher.find()) {
                relationships.add(CodeRelationship.builder()
                    .sourceElement(filePath).targetElement(useMatcher.group(1))
                    .type(CodeRelationship.RelationshipType.IMPORTS)
                    .sourceFile(filePath).sourceLine(i + 1).build());
                continue;
            }

            Matcher traitMatcher = traitPattern.matcher(line.trim());
            if (traitMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(traitMatcher.group(1)).type(CodeElement.ElementType.TRAIT)
                    .language("Rust").filePath(filePath).startLine(i + 1).build());
                continue;
            }

            Matcher structMatcher = structPattern.matcher(line.trim());
            if (structMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(structMatcher.group(1)).type(CodeElement.ElementType.STRUCT)
                    .language("Rust").filePath(filePath).startLine(i + 1).build());
                continue;
            }

            Matcher fnMatcher = fnPattern.matcher(line);
            if (fnMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(fnMatcher.group(1)).type(CodeElement.ElementType.FUNCTION)
                    .language("Rust").filePath(filePath).startLine(i + 1).build());
            }
        }
    }

    // ─── PHP ─────────────────────────────────────────────────────────────────

    private void parsePHP(String filePath, String content,
                           List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern classPattern = Pattern.compile("(?:abstract\\s+|final\\s+)?class\\s+(\\w+)(?:\\s+extends\\s+(\\w+))?(?:\\s+implements\\s+([\\w, ]+))?");
        Pattern interfacePattern = Pattern.compile("interface\\s+(\\w+)");
        Pattern funcPattern = Pattern.compile("(?:public|private|protected|static)?\\s*function\\s+(\\w+)\\s*\\(");
        Pattern requirePattern = Pattern.compile("(?:require|include)(?:_once)?\\s*['\"]([^'\"]+)['\"]");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            Matcher classMatcher = classPattern.matcher(line);
            if (classMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(classMatcher.group(1)).type(CodeElement.ElementType.CLASS)
                    .language("PHP").filePath(filePath).startLine(i + 1).build());
                continue;
            }

            Matcher funcMatcher = funcPattern.matcher(line);
            if (funcMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(funcMatcher.group(1)).type(CodeElement.ElementType.FUNCTION)
                    .language("PHP").filePath(filePath).startLine(i + 1).build());
            }
        }
    }

    // ─── Ruby ────────────────────────────────────────────────────────────────

    private void parseRuby(String filePath, String content,
                            List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern classPattern = Pattern.compile("^(?:\\s*)class\\s+(\\w+)(?:\\s*<\\s*(\\w+))?");
        Pattern modulePattern = Pattern.compile("^(?:\\s*)module\\s+(\\w+)");
        Pattern methodPattern = Pattern.compile("^(?:\\s*)def\\s+(\\w+)");
        Pattern requirePattern = Pattern.compile("require(?:_relative)?\\s+['\"]([^'\"]+)['\"]");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher classMatcher = classPattern.matcher(line);
            if (classMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(classMatcher.group(1)).type(CodeElement.ElementType.CLASS)
                    .language("Ruby").filePath(filePath).startLine(i + 1).build());
                if (classMatcher.group(2) != null) {
                    relationships.add(CodeRelationship.builder()
                        .sourceElement(classMatcher.group(1)).targetElement(classMatcher.group(2))
                        .type(CodeRelationship.RelationshipType.EXTENDS)
                        .sourceFile(filePath).sourceLine(i + 1).build());
                }
                continue;
            }

            Matcher moduleMatcher = modulePattern.matcher(line);
            if (moduleMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(moduleMatcher.group(1)).type(CodeElement.ElementType.MODULE)
                    .language("Ruby").filePath(filePath).startLine(i + 1).build());
                continue;
            }

            Matcher methodMatcher = methodPattern.matcher(line);
            if (methodMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(methodMatcher.group(1)).type(CodeElement.ElementType.METHOD)
                    .language("Ruby").filePath(filePath).startLine(i + 1).build());
            }

            Matcher requireMatcher = requirePattern.matcher(line);
            if (requireMatcher.find()) {
                relationships.add(CodeRelationship.builder()
                    .sourceElement(filePath).targetElement(requireMatcher.group(1))
                    .type(CodeRelationship.RelationshipType.IMPORTS)
                    .sourceFile(filePath).sourceLine(i + 1).build());
            }
        }
    }

    // ─── Kotlin ──────────────────────────────────────────────────────────────

    private void parseKotlin(String filePath, String content,
                              List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern classPattern = Pattern.compile("(?:data\\s+|sealed\\s+|abstract\\s+|open\\s+)?(?:class|object|interface)\\s+(\\w+)");
        Pattern funcPattern = Pattern.compile("(?:fun)\\s+(\\w+)\\s*(?:<[^>]+>)?\\s*\\(");
        Pattern importPattern = Pattern.compile("^import\\s+([\\w.]+)");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            Matcher importMatcher = importPattern.matcher(line);
            if (importMatcher.find()) {
                relationships.add(CodeRelationship.builder()
                    .sourceElement(filePath).targetElement(importMatcher.group(1))
                    .type(CodeRelationship.RelationshipType.IMPORTS)
                    .sourceFile(filePath).sourceLine(i + 1).build());
                continue;
            }

            Matcher classMatcher = classPattern.matcher(line);
            if (classMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(classMatcher.group(1))
                    .type(line.contains("interface") ? CodeElement.ElementType.INTERFACE : CodeElement.ElementType.CLASS)
                    .language("Kotlin").filePath(filePath).startLine(i + 1).build());
                continue;
            }

            Matcher funcMatcher = funcPattern.matcher(line);
            if (funcMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(funcMatcher.group(1)).type(CodeElement.ElementType.FUNCTION)
                    .language("Kotlin").filePath(filePath).startLine(i + 1).build());
            }
        }
    }

    // ─── Swift ───────────────────────────────────────────────────────────────

    private void parseSwift(String filePath, String content,
                             List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern classPattern = Pattern.compile("(?:public|private|internal|open|final|class|struct|enum|protocol|extension)\\s+(\\w+)");
        Pattern funcPattern = Pattern.compile("(?:func)\\s+(\\w+)\\s*(?:<[^>]+>)?\\s*\\(");
        Pattern importPattern = Pattern.compile("^import\\s+(\\w+)");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            Matcher importMatcher = importPattern.matcher(line);
            if (importMatcher.find()) {
                relationships.add(CodeRelationship.builder()
                    .sourceElement(filePath).targetElement(importMatcher.group(1))
                    .type(CodeRelationship.RelationshipType.IMPORTS)
                    .sourceFile(filePath).sourceLine(i + 1).build());
                continue;
            }

            Matcher classMatcher = classPattern.matcher(line);
            if (classMatcher.find()) {
                String name = classMatcher.group(1);
                if (!isSwiftKeyword(name)) {
                    elements.add(CodeElement.builder()
                        .name(name)
                        .type(line.contains("protocol") ? CodeElement.ElementType.INTERFACE : CodeElement.ElementType.CLASS)
                        .language("Swift").filePath(filePath).startLine(i + 1).build());
                }
                continue;
            }

            Matcher funcMatcher = funcPattern.matcher(line);
            if (funcMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(funcMatcher.group(1)).type(CodeElement.ElementType.FUNCTION)
                    .language("Swift").filePath(filePath).startLine(i + 1).build());
            }
        }
    }

    // ─── Generic fallback ────────────────────────────────────────────────────

    private void parseGeneric(String filePath, String content, String language,
                               List<CodeElement> elements) {
        // For totally unknown languages: just note the file exists
        elements.add(CodeElement.builder()
            .name(filePath.contains("/") ? filePath.substring(filePath.lastIndexOf('/') + 1) : filePath)
            .type(CodeElement.ElementType.MODULE)
            .language(language != null ? language : "Unknown")
            .filePath(filePath).startLine(1).build());
    }

    private void extractMatches(Pattern pattern, String content, String filePath, String language,
                                  CodeElement.ElementType type, List<CodeElement> elements,
                                  String subtype, int nameGroup) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher m = pattern.matcher(lines[i]);
            if (m.find()) {
                Map<String, Object> meta = subtype != null ? Map.of("subtype", subtype) : Map.of();
                elements.add(CodeElement.builder()
                    .name(m.group(nameGroup)).type(type)
                    .language(language).filePath(filePath)
                    .startLine(i + 1).metadata(meta).build());
            }
        }
    }

    @Override
    public CodeMetrics calculateMetrics(String filePath, String content, String language) {
        if (content == null || content.isBlank()) {
            return CodeMetrics.builder().filePath(filePath).language(language).build();
        }
        String[] lines = content.split("\n", -1);
        int total = lines.length;
        int blank = 0, comment = 0;
        String commentStart = getCommentStart(language);

        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) blank++;
            else if (commentStart != null && t.startsWith(commentStart)) comment++;
        }

        int code = total - blank - comment;
        ParsedFile parsed = parse(filePath, content, language);
        long classCount = parsed.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.CLASS || e.getType() == CodeElement.ElementType.INTERFACE).count();
        long funcCount = parsed.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.FUNCTION
                || e.getType() == CodeElement.ElementType.METHOD).count();

        List<String> smells = new ArrayList<>();
        if (total > 500) smells.add("Large file (>500 lines)");
        if (funcCount > 30) smells.add("Too many functions/methods (>" + funcCount + ")");

        return CodeMetrics.builder()
            .filePath(filePath).language(language)
            .totalLines(total).codeLines(code)
            .commentLines(comment).blankLines(blank)
            .classCount((int) classCount)
            .functionCount((int) funcCount)
            .commentRatio(total > 0 ? (double) comment / total : 0.0)
            .isLargeFile(total > 500)
            .codeSmells(smells)
            .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private int countLines(String content) {
        if (content == null) return 0;
        return content.split("\n", -1).length;
    }

    private boolean isKeyword(String word) {
        return List.of("if", "else", "for", "while", "return", "switch", "case", "do", "break", "continue")
            .contains(word);
    }

    private boolean isSwiftKeyword(String word) {
        return List.of("public", "private", "internal", "open", "final", "class", "struct",
            "enum", "protocol", "extension", "static", "var", "let", "func").contains(word);
    }

    private String getCommentStart(String language) {
        if (language == null) return null;
        return switch (language) {
            case "Python", "Ruby", "Shell" -> "#";
            case "JavaScript", "TypeScript", "Java", "C", "C++", "C#", "Go", "Kotlin", "Swift", "Rust", "PHP" -> "//";
            default -> null;
        };
    }
}
