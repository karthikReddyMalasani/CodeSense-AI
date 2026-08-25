package com.codesense.parser.core;

import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.CodeRelationship;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.treesitter.*;

import java.util.*;

/**
 * Dedicated Native Tree-Sitter AST Parsing Engine.
 * Single Responsibility: Manages JNI binding lifecycles, AST tree generation,
 * and recursive AST node walking. Fully isolates native crashes/exceptions.
 */
@Slf4j
@Component
public class TreeSitterNativeEngine {

    private final Map<String, TSLanguage> tsLanguages = new HashMap<>();
    private boolean tsAvailable = false;

    public TreeSitterNativeEngine() {
        initNativeLanguages();
    }

    private void initNativeLanguages() {
        try {
            tsLanguages.put("Python", new TreeSitterPython());
            tsLanguages.put("JavaScript", new TreeSitterJavascript());
            tsAvailable = true;
            log.info("[TreeSitterNativeEngine] Native JNI loaded successfully for: {}", tsLanguages.keySet());
        } catch (Throwable ex) {
            tsAvailable = false;
            log.warn("[TreeSitterNativeEngine] Native JNI unavailable, using regex fallback. ({})", ex.getMessage());
        }
    }

    public boolean isNativeAvailable(String language) {
        return tsAvailable && language != null && tsLanguages.containsKey(language);
    }

    public boolean parseAST(String filePath, String content, String language,
                            List<CodeElement> elements, List<CodeRelationship> relationships) {
        if (!isNativeAvailable(language)) {
            return false;
        }

        TSLanguage tsLang = tsLanguages.get(language);
        if (tsLang == null) {
            return false;
        }

        try (TSParser parser = new TSParser()) {
            parser.setLanguage(tsLang);
            try (TSTree tree = parser.parseString(null, content)) {
                if (tree == null) {
                    return false;
                }
                TSNode root = tree.getRootNode();
                if (root == null || root.isNull()) {
                    return false;
                }
                walkNode(root, content, filePath, language, elements, relationships, null);
                return true;
            }
        } catch (Throwable ex) {
            log.warn("[TreeSitterNativeEngine] AST parse failed for {} ({}): {}", filePath, language, ex.getMessage());
            return false;
        }
    }

    private void walkNode(TSNode node, String src, String filePath, String language,
                          List<CodeElement> elements, List<CodeRelationship> relationships,
                          String currentClass) {
        if (node == null || node.isNull()) return;

        String type;
        try {
            type = node.getType();
        } catch (Throwable t) {
            return;
        }

        String nextClass = currentClass;

        try {
            switch (type) {
                case "class_definition", "class_declaration", "abstract_class_declaration",
                     "struct_type", "struct_item" -> {
                    String name = childValue(node, src, "name", "identifier");
                    if (name != null) {
                        nextClass = name;
                        elements.add(CodeElement.builder()
                            .name(name).type(CodeElement.ElementType.CLASS)
                            .language(language).filePath(filePath)
                            .startLine(safeRow(node.getStartPoint()) + 1)
                            .endLine(safeRow(node.getEndPoint()) + 1).build());

                        String parent = childValue(node, src, "superclasses", "base_class",
                            "extends_type_list", "extends_clause");
                        if (parent != null) {
                            relationships.add(CodeRelationship.builder()
                                .sourceElement(name).targetElement(parent)
                                .type(CodeRelationship.RelationshipType.EXTENDS)
                                .sourceFile(filePath).sourceLine(safeRow(node.getStartPoint()) + 1).build());
                        }
                    }
                }

                case "interface_declaration" -> {
                    String name = childValue(node, src, "name", "identifier");
                    if (name != null) {
                        elements.add(CodeElement.builder()
                            .name(name).type(CodeElement.ElementType.INTERFACE)
                            .language(language).filePath(filePath)
                            .startLine(safeRow(node.getStartPoint()) + 1).build());
                    }
                }

                case "function_definition", "function_declaration", "function_item",
                     "method_definition", "method_declaration" -> {
                    String name = childValue(node, src, "name", "identifier");
                    if (name != null) {
                        boolean isMethod = currentClass != null;
                        elements.add(CodeElement.builder()
                            .name(name)
                            .type(isMethod ? CodeElement.ElementType.METHOD : CodeElement.ElementType.FUNCTION)
                            .language(language).filePath(filePath)
                            .startLine(safeRow(node.getStartPoint()) + 1)
                            .endLine(safeRow(node.getEndPoint()) + 1)
                            .parentName(currentClass).build());
                    }
                }

                case "lexical_declaration", "variable_declaration" -> {
                    String name = childValue(node, src, "name", "identifier");
                    if (name != null && nodeBodyContains(node, "arrow_function")) {
                        elements.add(CodeElement.builder()
                            .name(name).type(CodeElement.ElementType.FUNCTION)
                            .language(language).filePath(filePath)
                            .startLine(safeRow(node.getStartPoint()) + 1).build());
                    }
                }

                case "import_statement", "import_from_statement", "import_declaration" -> {
                    String target = importTarget(node, src);
                    if (target != null) {
                        relationships.add(CodeRelationship.builder()
                            .sourceElement(filePath).targetElement(target)
                            .type(CodeRelationship.RelationshipType.IMPORTS)
                            .sourceFile(filePath).sourceLine(safeRow(node.getStartPoint()) + 1).build());
                    }
                }

                default -> {}
            }
        } catch (Throwable ex) {
            log.trace("[TreeSitterNativeEngine] Ignored exception while walking node: {}", ex.getMessage());
        }

        try {
            int count = node.getChildCount();
            for (int i = 0; i < count; i++) {
                TSNode child = node.getChild(i);
                walkNode(child, src, filePath, language, elements, relationships, nextClass);
            }
        } catch (Throwable t) {
            // Safe exit if child iteration fails
        }
    }

    private int safeRow(TSPoint point) {
        return point != null ? point.getRow() : 0;
    }

    private String childValue(TSNode node, String src, String... childTypes) {
        Set<String> set = new HashSet<>(Arrays.asList(childTypes));
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            TSNode child = node.getChild(i);
            if (child != null && set.contains(child.getType())) {
                return nodeText(child, src);
            }
        }
        return null;
    }

    private boolean nodeBodyContains(TSNode node, String nodeType) {
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            TSNode child = node.getChild(i);
            if (child != null && nodeType.equals(child.getType())) return true;
        }
        return false;
    }

    private String importTarget(TSNode node, String src) {
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            TSNode child = node.getChild(i);
            if (child == null) continue;
            String t = child.getType();
            if ("string".equals(t) || "dotted_name".equals(t) || "relative_import".equals(t)
                    || "module_name".equals(t) || "identifier".equals(t)) {
                String text = nodeText(child, src);
                if (text != null) return text.replace("'", "").replace("\"", "");
            }
        }
        return null;
    }

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
}
