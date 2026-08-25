package com.codesense.parser.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Unified model for a single parsed source file.
 * Team Member 4 (Prashanthi) uses this internally before converting to DTOs.
 */
@Data
@Builder
public class ParsedFile {

    private String filePath;
    private String language;
    private String content;
    private int lineCount;
    private List<CodeElement> elements;
    private List<CodeRelationship> relationships;
    private Map<String, Object> metadata;
}
