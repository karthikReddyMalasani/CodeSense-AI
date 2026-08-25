package com.codesense.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityAnalysisResponseDto {

    private UUID repositoryId;
    private String repositoryName;

    // Quality score 0-100
    private int qualityScore;
    private String grade;  // A, B, C, D, F

    // Issue counts by type
    private int bugCount;
    private int securityCount;
    private int codeSmellCount;
    private int performanceCount;

    // Issue counts by severity
    private int criticalCount;
    private int highCount;
    private int mediumCount;
    private int lowCount;

    // Metrics
    private int totalFiles;
    private int analyzedFiles;
    private int totalLines;
    private int classCount;
    private int methodCount;
    private double averageComplexity;

    // Language breakdown: language -> fileCount
    private Map<String, Integer> languageBreakdown;

    // Detailed issues list with AI explanations
    private List<QualityIssueDto> issues;

    // Top AI recommendations
    private List<String> aiRecommendations;

    private String modelId;
}
