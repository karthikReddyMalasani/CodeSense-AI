package com.codesense.integration.parser.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Integration contract for Team Member 4's Parser/Analyzer.
 *
 * This DTO is the boundary between the Parser module (Team Member 4)
 * and the AI Engine (Team Member 3).
 *
 * Team Member 4 must populate this DTO and call:
 *   ParserIntegrationService.ingestParsedFile(parsedFileDTO)
 *
 * Team Member 3's AI Engine will consume this DTO without modification.
 *
 * Example JSON:
 * {
 *   "repositoryId": "...",
 *   "filePath": "src/main/java/com/example/AuthService.java",
 *   "language": "Java",
 *   "content": "...",
 *   "elements": [...],
 *   "relationships": [...],
 *   "metadata": {}
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParsedFileDTO {

    /** The repository this file belongs to */
    private String repositoryId;

    /** The project this file belongs to (for security scoping) */
    private String projectId;

    /** Relative path within the repository */
    private String filePath;

    /** Detected programming language */
    private String language;

    /** Full file content */
    private String content;

    /** Parsed code elements (classes, methods, functions, etc.) */
    @Builder.Default
    private List<CodeElementDTO> elements = List.of();

    /** Relationships between code elements (calls, extends, implements, etc.) */
    @Builder.Default
    private List<CodeRelationshipDTO> relationships = List.of();

    /** Additional parser-specific metadata */
    @Builder.Default
    private Map<String, Object> metadata = Map.of();
}
