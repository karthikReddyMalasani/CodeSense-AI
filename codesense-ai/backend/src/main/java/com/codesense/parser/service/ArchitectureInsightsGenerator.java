package com.codesense.parser.service;

import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import com.codesense.parser.model.ArchitectureInsights;
import com.codesense.parser.model.ArchitectureInsights.Insight;
import com.codesense.parser.model.ArchitectureInsights.ArchitectureWarning;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates architecture insights and warnings from parsed repository analysis.
 */
@Service
public class ArchitectureInsightsGenerator {

    public ArchitectureInsights generate(ParsedRepositoryDTO parsed) {
        List<Insight> insights = generateInsights(parsed);
        List<ArchitectureWarning> warnings = generateWarnings(parsed);

        return new ArchitectureInsights(insights, warnings);
    }

    private List<Insight> generateInsights(ParsedRepositoryDTO parsed) {
        List<Insight> insights = new ArrayList<>();

        // Insight 1: Layered architecture detection
        if (hasLayeredArchitecture(parsed)) {
            insights.add(Insight.builder()
                    .category("Layering")
                    .severity("IMPORTANT")
                    .description("Application follows a clear layered architecture with controller, service, and repository patterns")
                    .evidence("Detected multiple controller, service, and repository classes")
                    .affectedComponents(List.of("API", "Services", "Data Access"))
                    .build());
        }

        // Insight 2: Modularity assessment
        int packageCount = countPackages(parsed);
        if (packageCount > 10) {
            insights.add(Insight.builder()
                    .category("Modularity")
                    .severity("NOTICE")
                    .description("Application has good modular organization with " + packageCount + " distinct packages")
                    .evidence(packageCount + " packages detected across the codebase")
                    .build());
        }

        // Insight 3: Frontend-Backend separation
        if (hasFrontendBackendSeparation(parsed)) {
            insights.add(Insight.builder()
                    .category("Separation of Concerns")
                    .severity("IMPORTANT")
                    .description("Frontend and backend are properly separated, suggesting a scalable architecture")
                    .evidence("Distinct frontend/ and backend/ directories detected")
                    .affectedComponents(List.of("Frontend", "Backend"))
                    .build());
        }

        // Insight 4: Service layer analysis
        long serviceCount = countClasses(parsed, "Service");
        if (serviceCount > 5) {
            insights.add(Insight.builder()
                    .category("Service Layer")
                    .severity("NOTICE")
                    .description("Rich service layer with " + serviceCount + " service classes for business logic")
                    .evidence(serviceCount + " service classes detected")
                    .build());
        }

        // Insight 5: Database access pattern
        long repositoryCount = countClasses(parsed, "Repository");
        if (repositoryCount > 0) {
            insights.add(Insight.builder()
                    .category("Data Access")
                    .severity("NOTICE")
                    .description("Uses repository pattern for data access with " + repositoryCount + " repositories")
                    .evidence("Repository/DAO pattern detected in codebase")
                    .affectedComponents(List.of("Data Access", "Database"))
                    .build());
        }

        // Insight 6: Integration patterns
        if (hasIntegrationPatterns(parsed)) {
            insights.add(Insight.builder()
                    .category("Integration")
                    .severity("IMPORTANT")
                    .description("Application integrates with external systems (AI providers, authentication, storage)")
                    .evidence("External service integration code detected")
                    .build());
        }

        return insights;
    }

    private List<ArchitectureWarning> generateWarnings(ParsedRepositoryDTO parsed) {
        List<ArchitectureWarning> warnings = new ArrayList<>();

        // Warning 1: Large classes
        List<String> largeClasses = findLargeClasses(parsed);
        if (!largeClasses.isEmpty()) {
            warnings.add(ArchitectureWarning.builder()
                    .type("LARGE_CLASS")
                    .severity("MEDIUM")
                    .description("Found " + largeClasses.size() + " classes with potential high complexity")
                    .affectedComponents(largeClasses.stream().limit(3).collect(Collectors.toList()))
                    .evidence("Classes with " + (400 + " lines detected"))
                    .recommendation("Consider breaking down large classes into smaller, more focused classes")
                    .build());
        }

        // Warning 2: Potential circular dependencies
        if (mightHaveCircularDependencies(parsed)) {
            warnings.add(ArchitectureWarning.builder()
                    .type("CIRCULAR_DEPENDENCY")
                    .severity("HIGH")
                    .description("Potential circular dependencies detected between components")
                    .evidence("Mutual dependencies between service and repository layers")
                    .recommendation("Review and refactor dependencies to maintain clear layering")
                    .build());
        }

        // Warning 3: Missing authentication checks
        long servicesWithoutAuth = countClassesWithout(parsed, "Service", "@Secured|@PreAuthorize");
        if (servicesWithoutAuth > 5) {
            warnings.add(ArchitectureWarning.builder()
                    .type("SECURITY_CONCERN")
                    .severity("HIGH")
                    .description(servicesWithoutAuth + " service methods may lack security annotations")
                    .evidence("Limited @PreAuthorize or @Secured annotations found")
                    .recommendation("Add appropriate security annotations to service methods")
                    .build());
        }

        // Warning 4: Missing error handling
        long filesWithoutExceptionHandling = countFilesWithoutFeature(parsed, "Exception|try-catch|@ExceptionHandler");
        if (filesWithoutExceptionHandling > parsed.getFiles().size() * 0.5) {
            warnings.add(ArchitectureWarning.builder()
                    .type("ERROR_HANDLING")
                    .severity("MEDIUM")
                    .description("Significant portion of codebase may lack robust error handling")
                    .evidence("Limited exception handling detected")
                    .recommendation("Implement comprehensive error handling and custom exception classes")
                    .build());
        }

        // Warning 5: Tight coupling indicators
        long highCoupling = countHighCouplingIndicators(parsed);
        if (highCoupling > 10) {
            warnings.add(ArchitectureWarning.builder()
                    .type("HIGH_COUPLING")
                    .severity("MEDIUM")
                    .description("Multiple indicators of tight coupling between components")
                    .evidence(highCoupling + " coupling indicators detected")
                    .recommendation("Introduce interfaces and dependency injection to reduce coupling")
                    .build());
        }

        return warnings;
    }

    private boolean hasLayeredArchitecture(ParsedRepositoryDTO parsed) {
        long controllers = countClasses(parsed, "Controller");
        long services = countClasses(parsed, "Service");
        long repositories = countClasses(parsed, "Repository");
        
        return controllers > 0 && services > 0 && repositories > 0;
    }

    private int countPackages(ParsedRepositoryDTO parsed) {
        return (int) parsed.getFiles().stream()
                .map(f -> extractPackageName(f.getFilePath()))
                .filter(p -> p != null && !p.isEmpty())
                .distinct()
                .count();
    }

    private String extractPackageName(String filePath) {
        if (filePath == null || !filePath.contains("java")) return "";
        String[] parts = filePath.split("[/\\\\]");
        int javaIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if ("java".equals(parts[i])) javaIndex = i;
        }
        if (javaIndex < 0 || javaIndex >= parts.length - 1) return "";
        StringBuilder pkg = new StringBuilder();
        for (int i = javaIndex + 1; i < parts.length - 1; i++) {
            if (pkg.length() > 0) pkg.append(".");
            pkg.append(parts[i]);
        }
        return pkg.toString();
    }

    private boolean hasFrontendBackendSeparation(ParsedRepositoryDTO parsed) {
        return parsed.getFiles().stream().anyMatch(f -> f.getFilePath().contains("frontend")) &&
               parsed.getFiles().stream().anyMatch(f -> f.getFilePath().contains("backend"));
    }

    private long countClasses(ParsedRepositoryDTO parsed, String pattern) {
        return parsed.getFiles().stream()
                .flatMap(f -> f.getElements().stream())
                .filter(e -> e.getName() != null && e.getName().endsWith(pattern))
                .count();
    }

    private boolean hasIntegrationPatterns(ParsedRepositoryDTO parsed) {
        return parsed.getFiles().stream()
                .anyMatch(f -> f.getFilePath().toLowerCase().contains("ai") ||
                        f.getFilePath().toLowerCase().contains("gemini") ||
                        f.getFilePath().toLowerCase().contains("openai") ||
                        f.getFilePath().toLowerCase().contains("auth") ||
                        f.getFilePath().toLowerCase().contains("oauth"));
    }

    private List<String> findLargeClasses(ParsedRepositoryDTO parsed) {
        return parsed.getFiles().stream()
                .flatMap(f -> f.getElements().stream()
                        .filter(e -> e.getEndLine() != null && e.getStartLine() != null && 
                                (e.getEndLine() - e.getStartLine()) > 400)
                        .map(e -> e.getName()))
                .collect(Collectors.toList());
    }

    private boolean mightHaveCircularDependencies(ParsedRepositoryDTO parsed) {
        // Simple heuristic: if there are many bidirectional relationships
        return parsed.getTotalRelationships() > 100;
    }

    private long countClassesWithout(ParsedRepositoryDTO parsed, String pattern, String missingPattern) {
        return parsed.getFiles().stream()
                .flatMap(f -> f.getElements().stream())
                .filter(e -> e.getName() != null && e.getName().endsWith(pattern) &&
                        (e.getAnnotations() == null || e.getAnnotations().stream()
                                .noneMatch(a -> a.matches(".*" + missingPattern + ".*"))))
                .count();
    }

    private long countFilesWithoutFeature(ParsedRepositoryDTO parsed, String pattern) {
        return parsed.getFiles().stream()
                .filter(f -> f.getContent() == null || !f.getContent().matches(".*" + pattern + ".*"))
                .count();
    }

    private long countHighCouplingIndicators(ParsedRepositoryDTO parsed) {
        // Count direct constructor dependencies and field injections
        return parsed.getFiles().stream()
                .flatMap(f -> f.getElements().stream())
                .filter(e -> e.getAnnotations() != null &&
                        e.getAnnotations().stream().anyMatch(a -> a.contains("Autowired") || a.contains("Inject")))
                .count();
    }
}
