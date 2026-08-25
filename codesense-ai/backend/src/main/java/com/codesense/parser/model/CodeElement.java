package com.codesense.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a code element extracted by the parser.
 * Team Member 4 (Prashanthi) populates this from JavaParser or Tree-sitter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeElement {

    private String name;
    private ElementType type;
    private String language;
    private String filePath;
    private Integer startLine;
    private Integer endLine;
    private String content;
    private String visibility;
    private String returnType;
    private List<String> parameters;
    private List<String> annotations;
    private String documentation;
    private String parentName;
    private Map<String, Object> metadata;

    public enum ElementType {
        CLASS, INTERFACE, ENUM, METHOD, FUNCTION, CONSTRUCTOR,
        FIELD, VARIABLE, MODULE, PACKAGE, NAMESPACE, IMPORT,
        ANNOTATION, DECORATOR, STRUCT, TRAIT
    }
}
