package com.codesense.parser.core;

import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.CodeMetrics;
import com.codesense.parser.model.CodeRelationship;
import com.codesense.parser.model.ParsedFile;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JavaParser-based code parser for Java source files.
 * Team Member 4 (Prashanthi) owns this class.
 *
 * Extracts:
 * - Classes, Interfaces, Enums
 * - Methods, Constructors
 * - Fields
 * - Annotations
 * - Imports / Dependencies
 * - EXTENDS / IMPLEMENTS relationships
 * - Method calls
 */
@Slf4j
@Component
public class JavaParserCodeParser implements CodeParser {

    private final JavaParser javaParser;

    public JavaParserCodeParser() {
        this.javaParser = new JavaParser();
    }

    @Override
    public List<String> getSupportedLanguages() {
        return List.of("Java");
    }

    @Override
    public ParsedFile parse(String filePath, String content, String language) {
        List<CodeElement> elements = new ArrayList<>();
        List<CodeRelationship> relationships = new ArrayList<>();

        if (content == null || content.isBlank()) {
            return ParsedFile.builder()
                .filePath(filePath).language(language)
                .content(content).lineCount(0)
                .elements(elements).relationships(relationships)
                .metadata(Map.of()).build();
        }

        ParseResult<CompilationUnit> result = javaParser.parse(content);
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            log.debug("JavaParser failed for {}: {}", filePath, result.getProblems());
            return ParsedFile.builder()
                .filePath(filePath).language(language)
                .content(content).lineCount(countLines(content))
                .elements(elements).relationships(relationships)
                .metadata(Map.of("parseError", "JavaParser could not parse this file")).build();
        }

        CompilationUnit cu = result.getResult().get();

        // Imports → CodeRelationship(IMPORTS)
        cu.getImports().forEach(imp -> {
            String imported = imp.getNameAsString();
            relationships.add(CodeRelationship.builder()
                .sourceElement(filePath)
                .targetElement(imported)
                .type(CodeRelationship.RelationshipType.IMPORTS)
                .sourceFile(filePath)
                .build());
        });

        // Package element
        cu.getPackageDeclaration().ifPresent(pkg -> elements.add(CodeElement.builder()
            .name(pkg.getNameAsString()).type(CodeElement.ElementType.PACKAGE)
            .language(language).filePath(filePath)
            .startLine(pkg.getBegin().map(p -> p.line).orElse(null))
            .endLine(pkg.getEnd().map(p -> p.line).orElse(null))
            .build()));

        // Top-level types
        cu.getTypes().forEach(type -> extractTypeDeclaration(type, filePath, language, elements, relationships));

        return ParsedFile.builder()
            .filePath(filePath).language(language)
            .content(content).lineCount(countLines(content))
            .elements(elements).relationships(relationships)
            .metadata(Map.of(
                "packageName", cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse(""),
                "importCount", cu.getImports().size()
            ))
            .build();
    }

    private void extractTypeDeclaration(TypeDeclaration<?> type, String filePath, String language,
                                         List<CodeElement> elements, List<CodeRelationship> relationships) {
        CodeElement.ElementType elemType;
        List<String> annotations = extractAnnotations(type.getAnnotations());

        if (type instanceof ClassOrInterfaceDeclaration cls) {
            elemType = cls.isInterface() ? CodeElement.ElementType.INTERFACE : CodeElement.ElementType.CLASS;

            // EXTENDS relationships
            cls.getExtendedTypes().forEach(ext -> relationships.add(CodeRelationship.builder()
                .sourceElement(type.getNameAsString()).targetElement(ext.getNameAsString())
                .type(CodeRelationship.RelationshipType.EXTENDS)
                .sourceFile(filePath).build()));

            // IMPLEMENTS relationships
            cls.getImplementedTypes().forEach(impl -> relationships.add(CodeRelationship.builder()
                .sourceElement(type.getNameAsString()).targetElement(impl.getNameAsString())
                .type(CodeRelationship.RelationshipType.IMPLEMENTS)
                .sourceFile(filePath).build()));

        } else if (type instanceof EnumDeclaration) {
            elemType = CodeElement.ElementType.ENUM;
        } else {
            elemType = CodeElement.ElementType.CLASS;
        }

        CodeElement classElement = CodeElement.builder()
            .name(type.getNameAsString())
            .type(elemType)
            .language(language)
            .filePath(filePath)
            .startLine(type.getBegin().map(p -> p.line).orElse(null))
            .endLine(type.getEnd().map(p -> p.line).orElse(null))
            .content(type.toString().length() > 2000 ? type.toString().substring(0, 2000) + "..." : type.toString())
            .visibility(type.getAccessSpecifier().asString())
            .annotations(annotations)
            .build();

        elements.add(classElement);

        // Methods
        type.getMethods().forEach(method -> extractMethod(method, type.getNameAsString(), filePath, language, elements, relationships));

        // Constructors
        if (type instanceof ClassOrInterfaceDeclaration cls) {
            cls.getConstructors().forEach(ctor -> {
                List<String> params = ctor.getParameters().stream()
                    .map(p -> p.getTypeAsString() + " " + p.getNameAsString())
                    .collect(Collectors.toList());

                elements.add(CodeElement.builder()
                    .name(ctor.getNameAsString())
                    .type(CodeElement.ElementType.CONSTRUCTOR)
                    .language(language).filePath(filePath)
                    .startLine(ctor.getBegin().map(p -> p.line).orElse(null))
                    .endLine(ctor.getEnd().map(p -> p.line).orElse(null))
                    .visibility(ctor.getAccessSpecifier().asString())
                    .parameters(params)
                    .annotations(extractAnnotations(ctor.getAnnotations()))
                    .parentName(type.getNameAsString())
                    .build());
            });
        }

        // Fields
        type.getFields().forEach(field -> field.getVariables().forEach(variable ->
            elements.add(CodeElement.builder()
                .name(variable.getNameAsString())
                .type(CodeElement.ElementType.FIELD)
                .language(language).filePath(filePath)
                .startLine(field.getBegin().map(p -> p.line).orElse(null))
                .endLine(field.getEnd().map(p -> p.line).orElse(null))
                .returnType(field.getElementType().asString())
                .visibility(field.getAccessSpecifier().asString())
                .parentName(type.getNameAsString())
                .build())
        ));
    }

    private void extractMethod(MethodDeclaration method, String parentClass, String filePath,
                                String language, List<CodeElement> elements,
                                List<CodeRelationship> relationships) {
        List<String> params = method.getParameters().stream()
            .map(p -> p.getTypeAsString() + " " + p.getNameAsString())
            .collect(Collectors.toList());

        List<String> annotations = extractAnnotations(method.getAnnotations());

        String javadoc = method.getJavadocComment()
            .map(jd -> jd.getContent().trim()).orElse(null);

        elements.add(CodeElement.builder()
            .name(method.getNameAsString())
            .type(CodeElement.ElementType.METHOD)
            .language(language).filePath(filePath)
            .startLine(method.getBegin().map(p -> p.line).orElse(null))
            .endLine(method.getEnd().map(p -> p.line).orElse(null))
            .content(method.toString().length() > 1000 ? method.toString().substring(0, 1000) + "..." : method.toString())
            .visibility(method.getAccessSpecifier().asString())
            .returnType(method.getTypeAsString())
            .parameters(params)
            .annotations(annotations)
            .documentation(javadoc)
            .parentName(parentClass)
            .build());

        // Extract method call relationships
        method.findAll(MethodCallExpr.class).forEach(call -> {
            String target = call.getScope()
                .map(scope -> scope.toString() + "." + call.getNameAsString())
                .orElse(call.getNameAsString());
            relationships.add(CodeRelationship.builder()
                .sourceElement(parentClass + "." + method.getNameAsString())
                .targetElement(target)
                .type(CodeRelationship.RelationshipType.CALLS)
                .sourceFile(filePath)
                .sourceLine(call.getBegin().map(p -> p.line).orElse(null))
                .build());
        });
    }

    @Override
    public CodeMetrics calculateMetrics(String filePath, String content, String language) {
        if (content == null || content.isBlank()) {
            return CodeMetrics.builder().filePath(filePath).language(language).build();
        }

        String[] lines = content.split("\n", -1);
        int totalLines = lines.length;
        int commentLines = 0;
        int blankLines = 0;
        int codeLines = 0;

        boolean inBlockComment = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) { blankLines++; continue; }
            if (inBlockComment) {
                commentLines++;
                if (trimmed.contains("*/")) inBlockComment = false;
                continue;
            }
            if (trimmed.startsWith("/*") || trimmed.startsWith("/**")) {
                inBlockComment = true;
                commentLines++;
                if (trimmed.contains("*/")) inBlockComment = false;
                continue;
            }
            if (trimmed.startsWith("//")) { commentLines++; continue; }
            codeLines++;
        }

        int classCount = 0;
        int methodCount = 0;
        int fieldCount = 0;
        int cyclomaticComplexity = 1;
        List<String> smells = new ArrayList<>();

        ParseResult<CompilationUnit> result = javaParser.parse(content);
        if (result.isSuccessful() && result.getResult().isPresent()) {
            CompilationUnit cu = result.getResult().get();
            classCount = cu.getTypes().size();
            for (TypeDeclaration<?> type : cu.getTypes()) {
                methodCount += type.getMethods().size();
                fieldCount += type.getFields().size();
                // Simple cyclomatic: count branching keywords
                long branches = type.getMethods().stream()
                    .mapToLong(m -> countBranches(m.toString())).sum();
                cyclomaticComplexity += (int) branches;
            }

            // Detect simple code smells
            if (totalLines > 500) smells.add("Large file (>500 lines)");
            if (methodCount > 30) smells.add("Too many methods (>30)");
            for (MethodDeclaration m : cu.getTypes().stream()
                    .flatMap(t -> t.getMethods().stream()).collect(Collectors.toList())) {
                int methodLen = m.getEnd().map(e -> e.line).orElse(0)
                    - m.getBegin().map(b -> b.line).orElse(0);
                if (methodLen > 50) smells.add("Long method: " + m.getNameAsString() + " (" + methodLen + " lines)");
            }
        }

        double commentRatio = totalLines > 0 ? (double) commentLines / totalLines : 0.0;
        double avgMethodLength = methodCount > 0 ? (double) codeLines / methodCount : 0.0;

        return CodeMetrics.builder()
            .filePath(filePath).language(language)
            .totalLines(totalLines).codeLines(codeLines)
            .commentLines(commentLines).blankLines(blankLines)
            .classCount(classCount).methodCount(methodCount).fieldCount(fieldCount)
            .cyclomaticComplexity(cyclomaticComplexity)
            .averageMethodLength(avgMethodLength)
            .commentRatio(commentRatio)
            .isLargeFile(totalLines > 500)
            .codeSmells(smells)
            .build();
    }

    private List<String> extractAnnotations(NodeList<AnnotationExpr> annotations) {
        return annotations.stream().map(AnnotationExpr::getNameAsString).collect(Collectors.toList());
    }

    private int countLines(String content) {
        if (content == null) return 0;
        return content.split("\n", -1).length;
    }

    private long countBranches(String code) {
        long count = 0;
        for (String keyword : new String[]{"if ", "else ", "for ", "while ", "case ", "catch ", "&&", "||"}) {
            int idx = 0;
            while ((idx = code.indexOf(keyword, idx)) != -1) { count++; idx += keyword.length(); }
        }
        return count;
    }
}
