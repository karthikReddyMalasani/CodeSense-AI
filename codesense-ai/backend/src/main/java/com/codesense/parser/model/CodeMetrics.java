package com.codesense.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Code metrics for a single file or the whole repository.
 * Team Member 4 (Prashanthi) populates this.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeMetrics {

    private String filePath;
    private String language;

    // File-level metrics
    private int totalLines;
    private int codeLines;
    private int commentLines;
    private int blankLines;

    // Structure metrics
    private int classCount;
    private int methodCount;
    private int functionCount;
    private int fieldCount;
    private int importCount;

    // Complexity
    private int cyclomaticComplexity;
    private double averageMethodLength;
    private int maxMethodLength;
    private int longestMethodLines;

    // Quality indicators
    private double commentRatio;
    private int dependencyCount;
    private boolean isLargeFile;
    private List<String> codeSmells;

    // Aggregate metadata
    private Map<String, Object> extras;
}
