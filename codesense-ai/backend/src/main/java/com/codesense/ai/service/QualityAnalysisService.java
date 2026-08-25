package com.codesense.ai.service;

import com.codesense.ai.dto.*;
import com.codesense.ai.llm.LLMRequest;
import com.codesense.ai.llm.LLMResponse;
import com.codesense.ai.llm.LLMService;
import com.codesense.common.exception.ResourceNotFoundException;
import com.codesense.parser.service.CodeMetricsService;
import com.codesense.repository.model.Repository;
import com.codesense.repository.model.RepositoryFile;
import com.codesense.repository.repository.RepositoryFileRepository;
import com.codesense.repository.repository.RepositoryRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Code Quality Analysis Service.
 * Combines CodeMetricsService output with AI-powered issue detection
 * to produce a Quality Score + structured issue list + AI recommendations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityAnalysisService {

    private final CodeMetricsService codeMetricsService;
    private final LLMService llmService;
    private final RepositoryRepo repositoryRepo;
    private final RepositoryFileRepository repositoryFileRepository;

    public QualityAnalysisResponseDto analyzeQuality(UUID projectId, UUID repositoryId) {
        Repository repo = repositoryRepo.findByIdAndProjectId(repositoryId, projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId.toString()));

        // Step 1: Get code metrics
        CodeMetricsService.RepositoryMetricsSummary metrics =
            codeMetricsService.calculateRepositoryMetrics(repositoryId);

        // Step 2: Get sample files for AI analysis (top 5 non-binary files)
        List<RepositoryFile> files = repositoryFileRepository
            .findByRepositoryIdAndIgnoredFalse(repositoryId)
            .stream()
            .filter(f -> !f.isBinary() && f.getContent() != null && f.getContent().length() > 50)
            .sorted(Comparator.comparingLong(f -> -f.getSizeBytes()))
            .limit(5)
            .collect(Collectors.toList());

        // Step 3: Build AI prompt for quality analysis
        String codeContext = files.stream()
            .map(f -> "=== " + f.getFilePath() + " ===\n" + truncate(f.getContent(), 600))
            .collect(Collectors.joining("\n\n"));

        String prompt = buildQualityPrompt(repo.getName(), codeContext, metrics);

        // Step 4: Call LLM
        LLMResponse llmResponse = llmService.generate(
            LLMRequest.builder()
                .prompt(prompt)
                .maxNewTokens(2000)
                .temperature(0.1)
                .build()
        );

        // Step 5: Parse LLM output into structured issues
        List<QualityIssueDto> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (llmResponse.isSuccess()) {
            parseAiOutput(llmResponse.getGeneratedText(), issues, recommendations, files);
        }

        // Step 6: Add rule-based issues from metrics (always present)
        addMetricBasedIssues(metrics, issues);

        // Step 7: Compute quality score
        int score = computeQualityScore(metrics, issues);
        String grade = computeGrade(score);

        // Step 8: Build language breakdown
        Map<String, Integer> langBreakdown = new HashMap<>();
        if (metrics.getLanguageBreakdown() != null) {
            metrics.getLanguageBreakdown().forEach((lang, lm) ->
                langBreakdown.put(lang, lm.getFileCount()));
        }

        long bugs = issues.stream().filter(i -> "BUG".equals(i.getType())).count();
        long security = issues.stream().filter(i -> "SECURITY".equals(i.getType())).count();
        long smells = issues.stream().filter(i -> "CODE_SMELL".equals(i.getType())).count();
        long performance = issues.stream().filter(i -> "PERFORMANCE".equals(i.getType())).count();

        long critical = issues.stream().filter(i -> "CRITICAL".equals(i.getSeverity())).count();
        long high = issues.stream().filter(i -> "HIGH".equals(i.getSeverity())).count();
        long medium = issues.stream().filter(i -> "MEDIUM".equals(i.getSeverity())).count();
        long low = issues.stream().filter(i -> "LOW".equals(i.getSeverity())).count();

        if (recommendations.isEmpty()) {
            recommendations.add("Run AI ingestion to enable semantic code search and deeper analysis.");
            recommendations.add("Review files with high cyclomatic complexity first.");
            recommendations.add("Add unit tests for critical business logic.");
        }

        return QualityAnalysisResponseDto.builder()
            .repositoryId(repositoryId)
            .repositoryName(repo.getName())
            .qualityScore(score)
            .grade(grade)
            .bugCount((int) bugs)
            .securityCount((int) security)
            .codeSmellCount((int) smells)
            .performanceCount((int) performance)
            .criticalCount((int) critical)
            .highCount((int) high)
            .mediumCount((int) medium)
            .lowCount((int) low)
            .totalFiles(metrics.getTotalFiles())
            .analyzedFiles(metrics.getAnalyzedFiles())
            .totalLines(metrics.getTotalLines())
            .classCount(metrics.getClassCount())
            .methodCount(metrics.getMethodCount())
            .averageComplexity(metrics.getAverageCyclomaticComplexity())
            .languageBreakdown(langBreakdown)
            .issues(issues)
            .aiRecommendations(recommendations)
            .modelId(llmResponse.getModelId())
            .build();
    }

    // ─── Private Helpers ───────────────────────────────────────────────────────

    private String buildQualityPrompt(String repoName, String codeContext,
                                       CodeMetricsService.RepositoryMetricsSummary metrics) {
        return String.format("""
            You are a senior software engineer performing a code quality review.
            
            Repository: %s
            Total Files: %d | Total Lines: %d | Classes: %d | Methods: %d
            Avg Cyclomatic Complexity: %.2f
            
            Analyze the following source code files and identify concrete issues:
            
            %s
            
            Respond in this exact format for each issue found (up to 8 issues):
            
            ISSUE
            TYPE: BUG|SECURITY|CODE_SMELL|PERFORMANCE|MAINTAINABILITY
            SEVERITY: CRITICAL|HIGH|MEDIUM|LOW
            TITLE: Short title of the issue
            FILE: filename or class name
            DESCRIPTION: One sentence description of the problem
            EXPLANATION: Why this is a problem
            SUGGESTION: How to fix it
            END_ISSUE
            
            After all issues, add:
            RECOMMENDATIONS
            - Recommendation 1
            - Recommendation 2
            - Recommendation 3
            END_RECOMMENDATIONS
            """,
            repoName,
            metrics.getTotalFiles(), metrics.getTotalLines(),
            metrics.getClassCount(), metrics.getMethodCount(),
            metrics.getAverageCyclomaticComplexity(),
            codeContext.isEmpty() ? "No source files available for analysis." : codeContext
        );
    }

    private void parseAiOutput(String text, List<QualityIssueDto> issues,
                                List<String> recommendations, List<RepositoryFile> files) {
        try {
            String[] parts = text.split("ISSUE");
            for (int i = 1; i < parts.length; i++) {
                String block = parts[i];
                if (block.contains("END_ISSUE")) {
                    block = block.substring(0, block.indexOf("END_ISSUE"));
                }
                QualityIssueDto issue = QualityIssueDto.builder()
                    .type(extractField(block, "TYPE", "CODE_SMELL"))
                    .severity(extractField(block, "SEVERITY", "MEDIUM"))
                    .title(extractField(block, "TITLE", "Code Quality Issue"))
                    .filePath(extractField(block, "FILE", "Unknown"))
                    .description(extractField(block, "DESCRIPTION", ""))
                    .explanation(extractField(block, "EXPLANATION", ""))
                    .suggestion(extractField(block, "SUGGESTION", ""))
                    .build();
                issues.add(issue);
            }

            // Parse recommendations
            if (text.contains("RECOMMENDATIONS")) {
                String recBlock = text.substring(text.indexOf("RECOMMENDATIONS") + 15);
                if (recBlock.contains("END_RECOMMENDATIONS")) {
                    recBlock = recBlock.substring(0, recBlock.indexOf("END_RECOMMENDATIONS"));
                }
                Arrays.stream(recBlock.split("\n"))
                    .map(line -> line.replaceAll("^[-*•\\s]+", "").trim())
                    .filter(line -> !line.isBlank())
                    .forEach(recommendations::add);
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI quality output: {}", e.getMessage());
        }
    }

    private String extractField(String block, String field, String defaultValue) {
        String marker = field + ":";
        int start = block.indexOf(marker);
        if (start < 0) return defaultValue;
        start += marker.length();
        int end = block.indexOf("\n", start);
        String value = (end > start ? block.substring(start, end) : block.substring(start)).trim();
        return value.isEmpty() ? defaultValue : value;
    }

    private void addMetricBasedIssues(CodeMetricsService.RepositoryMetricsSummary metrics,
                                       List<QualityIssueDto> issues) {
        // High complexity warning
        if (metrics.getAverageCyclomaticComplexity() > 10) {
            issues.add(QualityIssueDto.builder()
                .type("MAINTAINABILITY").severity("HIGH")
                .title("High Cyclomatic Complexity")
                .filePath("Multiple files")
                .description("Average cyclomatic complexity is " +
                    String.format("%.1f", metrics.getAverageCyclomaticComplexity()) +
                    " (recommended < 10)")
                .explanation("High complexity makes code harder to test, understand, and maintain.")
                .suggestion("Refactor complex methods by extracting smaller, focused functions.")
                .build());
        }

        // Low comment ratio
        if (metrics.getCommentRatio() < 0.05 && metrics.getTotalLines() > 100) {
            issues.add(QualityIssueDto.builder()
                .type("MAINTAINABILITY").severity("LOW")
                .title("Low Documentation Coverage")
                .filePath("Repository-wide")
                .description("Comment ratio is " +
                    String.format("%.1f%%", metrics.getCommentRatio() * 100) +
                    " (recommended > 10%)")
                .explanation("Insufficient comments make the codebase harder to understand and maintain.")
                .suggestion("Add Javadoc/docstrings to public methods and classes.")
                .build());
        }

        // Add code smells from metrics service
        if (metrics.getCodeSmells() != null) {
            for (String smell : metrics.getCodeSmells()) {
                issues.add(QualityIssueDto.builder()
                    .type("CODE_SMELL").severity("MEDIUM")
                    .title(smell)
                    .filePath("Detected by parser")
                    .description(smell)
                    .explanation("Code smell detected by static analysis.")
                    .suggestion("Refactor to improve code quality.")
                    .build());
            }
        }
    }

    private int computeQualityScore(CodeMetricsService.RepositoryMetricsSummary metrics,
                                     List<QualityIssueDto> issues) {
        int score = 100;

        // Deduct for issues by severity
        for (QualityIssueDto issue : issues) {
            switch (issue.getSeverity()) {
                case "CRITICAL" -> score -= 15;
                case "HIGH"     -> score -= 8;
                case "MEDIUM"   -> score -= 4;
                case "LOW"      -> score -= 1;
            }
        }

        // Deduct for high complexity
        if (metrics.getAverageCyclomaticComplexity() > 15) score -= 10;
        else if (metrics.getAverageCyclomaticComplexity() > 10) score -= 5;

        return Math.max(0, Math.min(100, score));
    }

    private String computeGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
