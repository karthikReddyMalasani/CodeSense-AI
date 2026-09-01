package com.codesense.parser.service;

import com.codesense.parser.model.CodeMetrics;
import com.codesense.repository.model.RepositoryFile;
import com.codesense.repository.repository.RepositoryFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Code metrics aggregation service.
 * Team Member 4 (Prashanthi) owns this class.
 *
 * Calculates metrics at file-level and repository-level.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeMetricsService {

    private final ParserRouter parserRouter;
    private final RepositoryFileRepository repositoryFileRepository;

    /**
     * Calculate metrics for all files in the repository.
     * Optimized for large repositories with error resilience and progress tracking.
     */
    public RepositoryMetricsSummary calculateRepositoryMetrics(UUID repositoryId) {
        long startTime = System.currentTimeMillis();
        List<RepositoryFile> files = repositoryFileRepository.findByRepositoryIdAndIgnoredFalse(repositoryId);
        
        log.info("Starting metrics calculation for repository {} with {} total files", repositoryId, files.size());

        List<CodeMetrics> allMetrics = new ArrayList<>();
        Map<String, List<CodeMetrics>> byLanguage = new HashMap<>();
        int skipped = 0;
        int failed = 0;
        int processed = 0;

        for (RepositoryFile file : files) {
            // Skip binary files and empty content
            if (file.isBinary() || file.getContent() == null || file.getContent().isBlank()) {
                skipped++;
                continue;
            }
            
            try {
                // Skip very small files (likely not worth analyzing)
                if (file.getContent().length() < 10) {
                    skipped++;
                    continue;
                }
                
                CodeMetrics metrics = parserRouter.calculateMetrics(
                    file.getFilePath(), file.getContent(), file.getLanguage());
                allMetrics.add(metrics);

                String lang = file.getLanguage() != null ? file.getLanguage() : "Unknown";
                byLanguage.computeIfAbsent(lang, k -> new ArrayList<>()).add(metrics);
                processed++;
                
                // Log progress every 100 files for large repos
                if (processed % 100 == 0) {
                    log.debug("Metrics progress: {}/{} files processed", processed, files.size());
                }
            } catch (Exception e) {
                failed++;
                log.warn("Metrics calculation failed for file {}: {}", file.getFilePath(), e.getMessage());
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Metrics calculation completed for repository {} in {}ms: {} processed, {} skipped, {} failed", 
            repositoryId, duration, processed, skipped, failed);

        return buildSummary(repositoryId, allMetrics, byLanguage, files.size());
    }

    private RepositoryMetricsSummary buildSummary(UUID repositoryId, List<CodeMetrics> all,
                                                    Map<String, List<CodeMetrics>> byLang, int totalFiles) {
        int totalLines = all.stream().mapToInt(CodeMetrics::getTotalLines).sum();
        int codeLines  = all.stream().mapToInt(CodeMetrics::getCodeLines).sum();
        int commentLines = all.stream().mapToInt(CodeMetrics::getCommentLines).sum();
        int classCount = all.stream().mapToInt(CodeMetrics::getClassCount).sum();
        int methodCount = all.stream().mapToInt(m -> m.getMethodCount() + m.getFunctionCount()).sum();
        double avgCyclomatic = all.stream().mapToInt(CodeMetrics::getCyclomaticComplexity)
            .filter(c -> c > 0).average().orElse(0.0);

        List<String> allSmells = all.stream()
            .flatMap(m -> m.getCodeSmells() != null ? m.getCodeSmells().stream() : java.util.stream.Stream.empty())
            .distinct().collect(Collectors.toList());

        Map<String, LanguageMetrics> languageBreakdown = new HashMap<>();
        byLang.forEach((lang, metrics) -> {
            int lines = metrics.stream().mapToInt(CodeMetrics::getTotalLines).sum();
            long fileCount = metrics.size();
            languageBreakdown.put(lang, new LanguageMetrics(lang, (int) fileCount, lines));
        });

        return RepositoryMetricsSummary.builder()
            .repositoryId(repositoryId)
            .totalFiles(totalFiles)
            .analyzedFiles(all.size())
            .totalLines(totalLines)
            .codeLines(codeLines)
            .commentLines(commentLines)
            .classCount(classCount)
            .methodCount(methodCount)
            .averageCyclomaticComplexity(avgCyclomatic)
            .commentRatio(totalLines > 0 ? (double) commentLines / totalLines : 0.0)
            .codeSmells(allSmells)
            .languageBreakdown(languageBreakdown)
            .build();
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RepositoryMetricsSummary {
        private UUID repositoryId;
        private int totalFiles;
        private int analyzedFiles;
        private int totalLines;
        private int codeLines;
        private int commentLines;
        private int classCount;
        private int methodCount;
        private double averageCyclomaticComplexity;
        private double commentRatio;
        private List<String> codeSmells;
        private Map<String, LanguageMetrics> languageBreakdown;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class LanguageMetrics {
        private String language;
        private int fileCount;
        private int totalLines;
    }
}
