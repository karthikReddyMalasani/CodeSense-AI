package com.codesense.integration.parser.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a parsed code element (class, method, function, interface, enum, etc.)
 *
 * Team Member 4 (Prashanthi) populates this from JavaParser / Tree-sitter output.
 *
 * Example (Java class):
 * {
 *   "name": "AuthService",
 *   "type": "CLASS",
 *   "language": "Java",
 *   "filePath": "src/main/java/AuthService.java",
 *   "startLine": 1,
 *   "endLine": 100,
 *   "content": "public class AuthService { ... }"
 * }
 *
 * Example (Python function):
 * {
 *   "name": "authenticate_user",
 *   "type": "FUNCTION",
 *   "language": "Python",
 *   "filePath": "auth.py",
 *   "startLine": 15,
 *   "endLine": 40
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeElementDTO {

    /** Element name (class name, method name, function name) */
    private String name;

    /**
     * Element type. Supported values:
     * CLASS, METHOD, FUNCTION, INTERFACE, ENUM, CONSTRUCTOR,
     * FIELD, MODULE, PACKAGE, NAMESPACE, STRUCT, TRAIT
     */
    private String type;

    /** Programming language */
    private String language;

    /** File path relative to repository root */
    private String filePath;

    /** Start line (1-based, inclusive) */
    private Integer startLine;

    /** End line (1-based, inclusive) */
    private Integer endLine;

    /** Source code content of this element */
    private String content;

    /** Visibility/access modifier (public, private, protected) */
    private String visibility;

    /** Return type (for methods/functions) */
    private String returnType;

    /** Parameters (for methods/functions) */
    private List<String> parameters;

    /** Annotations or decorators */
    private List<String> annotations;

    /** Docstring or JavaDoc */
    private String documentation;

    /** Additional parser metadata */
    private Map<String, Object> metadata;
}
