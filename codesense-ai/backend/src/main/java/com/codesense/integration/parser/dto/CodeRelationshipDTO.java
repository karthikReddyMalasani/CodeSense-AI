package com.codesense.integration.parser.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a relationship between two code elements.
 *
 * Team Member 4 (Prashanthi) populates this from dependency graph analysis.
 *
 * Example:
 * {
 *   "sourceElement": "AuthController",
 *   "targetElement": "AuthService",
 *   "relationshipType": "CALLS",
 *   "sourceFile": "AuthController.java",
 *   "targetFile": "AuthService.java"
 * }
 *
 * Relationship types:
 * - CALLS: method/function invocation
 * - EXTENDS: class inheritance
 * - IMPLEMENTS: interface implementation
 * - IMPORTS: module import
 * - USES: general usage/dependency
 * - DEPENDS_ON: general dependency
 * - OVERRIDES: method override
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeRelationshipDTO {

    /** Source element name (caller, subclass, etc.) */
    private String sourceElement;

    /** Target element name (callee, superclass, etc.) */
    private String targetElement;

    /**
     * Relationship type.
     * CALLS | EXTENDS | IMPLEMENTS | IMPORTS | USES | DEPENDS_ON | OVERRIDES
     */
    private String relationshipType;

    /** File containing the source element */
    private String sourceFile;

    /** File containing the target element */
    private String targetFile;

    /** Line number of the relationship (e.g., where the call occurs) */
    private Integer sourceLine;
}
