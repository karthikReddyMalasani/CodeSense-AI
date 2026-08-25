package com.codesense.parser.core;

import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.CodeRelationship;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dedicated Regex Fallback Code Parser.
 * Single Responsibility: Performs regex pattern extraction for non-JNI or fallback languages.
 */
@Slf4j
@Component
public class RegexFallbackParser {

    private static final Set<String> KEYWORDS = Set.of(
        "if", "else", "for", "while", "do", "switch", "case", "return",
        "break", "continue", "goto", "typedef", "sizeof", "struct", "union", "enum"
    );

    public void parse(String filePath, String content, String language,
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
            log.warn("[RegexFallbackParser] Parsing error for {} ({}): {}", filePath, language, e.getMessage());
        }
    }

    private void parsePython(String filePath, String content,
                              List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern classPattern = Pattern.compile("^(class)\\s+(\\w+)(?:\\(([^)]+)\\))?\\s*:");
        Pattern funcPattern = Pattern.compile("^(\\s*)(async\\s+)?def\\s+(\\w+)\\s*\\(([^)]*)\\)");
        Pattern importPattern = Pattern.compile("^(?:from\\s+([\\w.]+)\\s+)?import\\s+([\\w., *]+)");
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
                List<String> annotations = pendingDecorator != null ? List.of(pendingDecorator) : List.of();
                pendingDecorator = null;

                elements.add(CodeElement.builder()
                    .name(name).type(CodeElement.ElementType.CLASS).language("Python")
                    .filePath(filePath).startLine(i + 1)
                    .annotations(annotations)
                    .metadata(parent != null ? Map.of("extends", (Object) parent) : Map.of())
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

    private void parseJavaScript(String filePath, String content,
                                  List<CodeElement> elements, List<CodeRelationship> relationships) {
        parseJsTs(filePath, content, "JavaScript", elements, relationships);
    }

    private void parseTypeScript(String filePath, String content,
                                  List<CodeElement> elements, List<CodeRelationship> relationships) {
        parseJsTs(filePath, content, "TypeScript", elements, relationships);

        Pattern interfacePattern = Pattern.compile("(?:export\\s+)?interface\\s+(\\w+)(?:\\s+extends\\s+([\\w, ]+))?\\s*\\{");
        extractMatches(interfacePattern, content, filePath, "TypeScript",
            CodeElement.ElementType.INTERFACE, elements, null, 1);
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
                if (!KEYWORDS.contains(name)) {
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

    private void parseRust(String filePath, String content,
                            List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern structPattern = Pattern.compile("^(?:pub\\s+)?(?:struct|enum)\\s+(\\w+)");
        Pattern traitPattern = Pattern.compile("^(?:pub\\s+)?trait\\s+(\\w+)");
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

    private void parsePHP(String filePath, String content,
                           List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern classPattern = Pattern.compile("(?:abstract\\s+|final\\s+)?class\\s+(\\w+)(?:\\s+extends\\s+(\\w+))?(?:\\s+implements\\s+([\\w, ]+))?");
        Pattern funcPattern = Pattern.compile("(?:public|private|protected|static)?\\s*function\\s+(\\w+)\\s*\\(");

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

    private void parseRuby(String filePath, String content,
                            List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern classPattern = Pattern.compile("^(?:\\s*)class\\s+(\\w+)(?:\\s*<\\s*(\\w+))?");
        Pattern methodPattern = Pattern.compile("^(?:\\s*)def\\s+(\\w+)");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher classMatcher = classPattern.matcher(line);
            if (classMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(classMatcher.group(1)).type(CodeElement.ElementType.CLASS)
                    .language("Ruby").filePath(filePath).startLine(i + 1).build());
                continue;
            }

            Matcher methodMatcher = methodPattern.matcher(line);
            if (methodMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(methodMatcher.group(1)).type(CodeElement.ElementType.METHOD)
                    .language("Ruby").filePath(filePath).startLine(i + 1).build());
            }
        }
    }

    private void parseKotlin(String filePath, String content,
                              List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern classPattern = Pattern.compile("(?:data\\s+|sealed\\s+|abstract\\s+|open\\s+)?(?:class|object|interface)\\s+(\\w+)");
        Pattern funcPattern = Pattern.compile("(?:fun)\\s+(\\w+)\\s*(?:<[^>]+>)?\\s*\\(");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

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

    private void parseSwift(String filePath, String content,
                             List<CodeElement> elements, List<CodeRelationship> relationships) {
        String[] lines = content.split("\n", -1);
        Pattern classPattern = Pattern.compile("(?:public|private|internal|open|final|class|struct|enum|protocol|extension)\\s+(\\w+)");
        Pattern funcPattern = Pattern.compile("(?:func)\\s+(\\w+)\\s*(?:<[^>]+>)?\\s*\\(");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            Matcher classMatcher = classPattern.matcher(line);
            if (classMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(classMatcher.group(1)).type(CodeElement.ElementType.CLASS)
                    .language("Swift").filePath(filePath).startLine(i + 1).build());
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

    private void parseGeneric(String filePath, String content, String language, List<CodeElement> elements) {
        String[] lines = content.split("\n", -1);
        Pattern funcPattern = Pattern.compile("(?:def|function|fn|func|procedure)\\s+(\\w+)");
        Pattern classPattern = Pattern.compile("(?:class|struct|interface|type)\\s+(\\w+)");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            Matcher classMatcher = classPattern.matcher(line);
            if (classMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(classMatcher.group(1)).type(CodeElement.ElementType.CLASS)
                    .language(language != null ? language : "Unknown")
                    .filePath(filePath).startLine(i + 1).build());
                continue;
            }

            Matcher funcMatcher = funcPattern.matcher(line);
            if (funcMatcher.find()) {
                elements.add(CodeElement.builder()
                    .name(funcMatcher.group(1)).type(CodeElement.ElementType.FUNCTION)
                    .language(language != null ? language : "Unknown")
                    .filePath(filePath).startLine(i + 1).build());
            }
        }
    }

    private void extractMatches(Pattern pattern, String content, String filePath, String language,
                                CodeElement.ElementType type, List<CodeElement> elements,
                                String category, int nameGroup) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher m = pattern.matcher(lines[i]);
            if (m.find()) {
                String name = m.group(nameGroup);
                Map<String, Object> meta = category != null ? Map.of("category", (Object) category) : Map.of();
                elements.add(CodeElement.builder()
                    .name(name).type(type).language(language)
                    .filePath(filePath).startLine(i + 1).metadata(meta).build());
            }
        }
    }
}
