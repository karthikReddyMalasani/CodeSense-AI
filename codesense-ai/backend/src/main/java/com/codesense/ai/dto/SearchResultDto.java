package com.codesense.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResultDto {
    private UUID chunkId;
    private String filePath;
    private String language;
    private String symbolName;
    private String symbolType;
    private Integer startLine;
    private Integer endLine;
    private String content;
    private String chunkType;
    private Double similarityScore;
}
