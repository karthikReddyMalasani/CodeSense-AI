package com.codesense.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a relationship between two code elements.
 * Team Member 4 (Prashanthi) populates this from parser output.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeRelationship {

    private String sourceElement;
    private String targetElement;
    private RelationshipType type;
    private String sourceFile;
    private String targetFile;
    private Integer sourceLine;

    public enum RelationshipType {
        IMPORTS, CALLS, EXTENDS, IMPLEMENTS, USES,
        REFERENCES, DEPENDS_ON, OVERRIDES, INSTANTIATES
    }
}
