package com.codesense.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeExplainResponseDto {
    private String summary;
    private String purpose;
    private List<String> keyComponents;
    private List<String> logic;
    private List<String> dependencies;
    private List<String> potentialIssues;
    private List<String> suggestions;
    private String rawExplanation;
    private String modelId;
}
