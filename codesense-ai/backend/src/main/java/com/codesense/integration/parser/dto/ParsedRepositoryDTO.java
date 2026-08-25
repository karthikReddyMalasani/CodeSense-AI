package com.codesense.integration.parser.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Container for a fully parsed repository.
 * Team Member 4 submits this to the AI Engine after parsing a complete repository.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParsedRepositoryDTO {

    private String repositoryId;
    private String projectId;
    private String repositoryName;

    /** All parsed files in this repository */
    private List<ParsedFileDTO> files;

    /** Total number of code elements extracted */
    private int totalElements;

    /** Total number of relationships extracted */
    private int totalRelationships;

    /** Languages detected in this repository */
    private List<String> languages;

    /** Parser version used */
    private String parserVersion;
}
