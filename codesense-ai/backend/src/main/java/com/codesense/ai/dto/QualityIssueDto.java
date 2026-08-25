package com.codesense.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityIssueDto {
    private String type;          // BUG, SECURITY, CODE_SMELL, PERFORMANCE, MAINTAINABILITY
    private String severity;      // CRITICAL, HIGH, MEDIUM, LOW
    private String title;
    private String filePath;
    private Integer line;
    private String description;
    private String explanation;
    private String suggestion;
}
