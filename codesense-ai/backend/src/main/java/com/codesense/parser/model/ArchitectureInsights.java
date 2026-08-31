package com.codesense.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Architecture insights and warnings derived from source code analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchitectureInsights {

    /** Architectural observations and insights */
    private List<Insight> insights;

    /** Architectural warnings and issues */
    private List<ArchitectureWarning> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Insight {
        /** Category of insight (e.g., "Coupling", "Cohesion", "Layering", "Modularity") */
        private String category;

        /** Severity level (INFO, NOTICE, IMPORTANT) */
        private String severity;

        /** The insight description */
        private String description;

        /** Supporting evidence */
        private String evidence;

        /** Affected components */
        private List<String> affectedComponents;

        /** Source files supporting this insight */
        private List<String> sourceFiles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ArchitectureWarning {
        /** Type of warning */
        private String type; // HIGH_COUPLING, CIRCULAR_DEPENDENCY, LARGE_CLASS, LAYER_VIOLATION, etc.

        /** Severity (CRITICAL, HIGH, MEDIUM, LOW) */
        private String severity;

        /** Human-readable description */
        private String description;

        /** Affected component(s) */
        private List<String> affectedComponents;

        /** Supporting evidence/metrics */
        private String evidence;

        /** Recommended action */
        private String recommendation;

        /** Source files involved */
        private List<String> sourceFiles;

        /** Line numbers if applicable */
        private List<Integer> lineNumbers;
    }
}
