package com.codesense.parser.service;

import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import com.codesense.parser.model.*;
import com.codesense.repository.model.RepositoryFile;
import com.codesense.repository.repository.RepositoryFileRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
public class ArchitectureAnalysisService {
    private static final List<String> STAGES = List.of(
        "Reading project structure", "Scanning source files", "Identifying application entry points",
        "Detecting frameworks and technologies", "Analyzing dependencies", "Analyzing frontend architecture",
        "Analyzing backend architecture", "Analyzing API endpoints", "Tracing frontend to backend communication",
        "Analyzing services and business logic", "Analyzing database/entities/repositories",
        "Analyzing authentication and authorization", "Detecting external APIs and third-party services",
        "Analyzing file/data processing", "Analyzing deployment and infrastructure configuration",
        "Building component relationships", "Understanding end-to-end application workflow",
        "Designing system architecture", "Generating architecture diagram", "Finalizing architecture report"
    );

    private final RepositoryParserService parserService;
    private final RepositoryFileRepository fileRepository;
    private final HighLevelDesignGenerator hldGenerator;
    private final LowLevelDesignGenerator lldGenerator;
    private final DatabaseDesignGenerator dbGenerator;
    private final ArchitectureInsightsGenerator insightsGenerator;
    private final Executor analysisExecutor;
    private final Map<UUID, AnalysisJob> jobs = new ConcurrentHashMap<>();

    public ArchitectureAnalysisService(RepositoryParserService parserService,
                                        RepositoryFileRepository fileRepository,
                                        HighLevelDesignGenerator hldGenerator,
                                        LowLevelDesignGenerator lldGenerator,
                                        DatabaseDesignGenerator dbGenerator,
                                        ArchitectureInsightsGenerator insightsGenerator,
                                        @Qualifier("aiTaskExecutor") Executor analysisExecutor) {
        this.parserService = parserService;
        this.fileRepository = fileRepository;
        this.hldGenerator = hldGenerator;
        this.lldGenerator = lldGenerator;
        this.dbGenerator = dbGenerator;
        this.insightsGenerator = insightsGenerator;
        this.analysisExecutor = analysisExecutor;
    }

    public JobView start(UUID repositoryId) {
        AnalysisJob current = jobs.get(repositoryId);
        if (current != null && (current.status == Status.QUEUED || current.status == Status.RUNNING)) return current.view();
        AnalysisJob job = new AnalysisJob(UUID.randomUUID(), repositoryId);
        jobs.put(repositoryId, job);
        analysisExecutor.execute(() -> analyze(job));
        return job.view();
    }

    public JobView get(UUID repositoryId, UUID jobId) {
        AnalysisJob job = jobs.get(repositoryId);
        if (job == null || !job.id.equals(jobId)) throw new NoSuchElementException("Architecture analysis not found");
        return job.view();
    }

    private void analyze(AnalysisJob job) {
        try {
            job.status = Status.RUNNING;
            stage(job, 0);
            List<RepositoryFile> sourceFiles = fileRepository.findByRepositoryIdAndIgnoredFalse(job.repositoryId);
            stage(job, 1);
            ParsedRepositoryDTO parsed = parserService.parseRepository(job.repositoryId);
            stage(job, 2);
            stage(job, 3);

            // Generate comprehensive architecture
            stage(job, 4);
            HighLevelDesign hld = hldGenerator.generate(parsed);
            stage(job, 5);
            LowLevelDesign lld = lldGenerator.generate(parsed);
            stage(job, 6);
            stage(job, 7);
            stage(job, 8);
            DatabaseDesign databaseDesign = dbGenerator.generate(parsed);
            stage(job, 9);
            ArchitectureInsights insights = insightsGenerator.generate(parsed);
            stage(job, 10);

            // Generate legacy data for backward compatibility
            Map<String, Integer> technologies = detectTechnologies(sourceFiles);
            stage(job, 11);
            List<Api> apis = detectApis(parsed);
            stage(job, 12);
            stage(job, 13);
            stage(job, 14);
            stage(job, 15);

            // Build result with all architecture data
            Result result = buildResult(parsed, sourceFiles, technologies, apis, hld, lld, databaseDesign, insights);
            stage(job, 16);
            stage(job, 17);
            stage(job, 18);
            job.result = result;
            stage(job, 19);
            job.status = Status.COMPLETED;
            job.updatedAt = Instant.now();
        } catch (Exception error) {
            job.status = Status.FAILED;
            job.error = error.getMessage() == null ? "Architecture analysis failed" : error.getMessage();
            job.updatedAt = Instant.now();
        }
    }

    private void stage(AnalysisJob job, int index) {
        job.completedStage = Math.max(job.completedStage, index);
        job.currentStage = STAGES.get(Math.min(index, STAGES.size() - 1));
        job.updatedAt = Instant.now();
    }

    private Result buildResult(ParsedRepositoryDTO parsed, List<RepositoryFile> files, Map<String, Integer> technologies, List<Api> apis, HighLevelDesign hld, LowLevelDesign lld, DatabaseDesign databaseDesign, ArchitectureInsights insights) {
        Map<String, LinkedHashSet<String>> layers = new LinkedHashMap<>();
        layers.put("Frontend", new LinkedHashSet<>()); layers.put("API", new LinkedHashSet<>());
        layers.put("Services", new LinkedHashSet<>()); layers.put("Data", new LinkedHashSet<>());
        layers.put("Infrastructure", new LinkedHashSet<>());
        List<Evidence> evidence = new ArrayList<>();
        for (ParsedFileDTO file : parsed.getFiles()) {
            String path = file.getFilePath() == null ? "" : file.getFilePath().replace('\\', '/');
            String lower = path.toLowerCase(Locale.ROOT);
            String layer = lower.contains("frontend") || lower.contains("component") || lower.contains("page") ? "Frontend"
                : lower.contains("controller") || lower.contains("route") ? "API"
                : lower.contains("service") || lower.contains("context") ? "Services"
                : lower.contains("repository") || lower.contains("entity") || lower.contains("model") ? "Data" : null;
            if (layer != null) {
                String name = path.substring(path.lastIndexOf('/') + 1);
                layers.get(layer).add(name);
                evidence.add(new Evidence(path, 1, null, "Path places this file in the " + layer + " area", "INFERRED"));
            }
            if (file.getElements() != null) for (var element : file.getElements()) {
                String type = element.getType() == null ? "" : element.getType().toUpperCase(Locale.ROOT);
                if ("CLASS".equals(type) || "INTERFACE".equals(type)) {
                    String name = element.getName();
                    if (name == null || name.isBlank()) continue;
                    String classified = name.endsWith("Controller") ? "API" : name.endsWith("Service") ? "Services" : name.endsWith("Repository") || name.endsWith("Entity") ? "Data" : null;
                    if (classified != null) layers.get(classified).add(name);
                    evidence.add(new Evidence(path, element.getStartLine(), element.getName(), "Detected " + type + " " + name, "CONFIRMED"));
                }
            }
        }

        List<Component> components = layers.entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).map(entry -> new Component(entry.getKey(), entry.getKey().equals("API") ? "HTTP/API boundary" : entry.getKey() + " code area", technologies.keySet().stream().filter(key -> key.toLowerCase(Locale.ROOT).contains(entry.getKey().toLowerCase(Locale.ROOT))).findFirst().orElse("Detected from source structure"), new ArrayList<>(entry.getValue()).stream().limit(20).toList(), evidence.stream().filter(item -> entry.getValue().contains(item.symbol())).limit(10).toList())).toList();

        List<Flow> flows = detectRelationshipFlows(parsed, layers);
        if (flows.isEmpty()) {
            if (!layers.get("Frontend").isEmpty() && !layers.get("API").isEmpty()) flows.add(new Flow("Frontend", "API", "Detected frontend and API source areas"));
            if (!layers.get("API").isEmpty() && !layers.get("Services").isEmpty()) flows.add(new Flow("API", "Services", "Controller/route and service naming evidence"));
            if (!layers.get("Services").isEmpty() && !layers.get("Data").isEmpty()) flows.add(new Flow("Services", "Data", "Service/repository naming evidence"));
        }

        String mermaid = buildMermaid(flows);
        String summary = "This layered modular architecture is derived from " + parsed.getFiles().size() + " parsed files and " + parsed.getTotalRelationships() + " parser relationships. Confirmed findings use parser metadata; path-based areas are marked inferred.";
        return new Result(hld, lld, databaseDesign, insights, "Evidence-backed layered application architecture", summary, components, flows, apis, technologies.keySet().stream().toList(), evidence.stream().limit(60).toList(), mermaid);
    }

    private List<Flow> detectRelationshipFlows(ParsedRepositoryDTO parsed, Map<String, LinkedHashSet<String>> layers) {
        Map<String, LinkedHashSet<String>> relationshipGraph = new LinkedHashMap<>();

        for (ParsedFileDTO file : parsed.getFiles()) {
            if (file == null || file.getRelationships() == null) continue;
            for (var relationship : file.getRelationships()) {
                if (relationship == null) continue;

                String source = normalizeRelationshipNode(relationship.getSourceElement(), relationship.getSourceFile());
                String target = normalizeRelationshipNode(relationship.getTargetElement(), relationship.getTargetFile());
                if (source == null || target == null || source.isBlank() || target.isBlank()) continue;
                if (isLowLevelArchitectureNoise(source) || isLowLevelArchitectureNoise(target)) continue;

                String sourceLayer = classifyArchitectureComponent(relationship.getSourceFile(), source);
                String targetLayer = classifyArchitectureComponent(relationship.getTargetFile(), target);

                String sourceNode = sourceLayer != null ? sourceLayer : source;
                String targetNode = targetLayer != null ? targetLayer : target;

                if (sourceLayer == null && targetLayer == null) continue;
                if (sourceNode.equalsIgnoreCase(targetNode)) continue;

                relationshipGraph.computeIfAbsent(sourceNode, key -> new LinkedHashSet<>()).add(targetNode);
                if (sourceLayer != null && layers.get(sourceLayer) != null) layers.get(sourceLayer).add(source);
                if (targetLayer != null && layers.get(targetLayer) != null) layers.get(targetLayer).add(target);
            }
        }

        if (relationshipGraph.isEmpty()) return List.of();

        List<Flow> flows = new ArrayList<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : relationshipGraph.entrySet()) {
            for (String target : entry.getValue()) {
                flows.add(new Flow(entry.getKey(), target, "Detected source-backed component flow"));
            }
        }
        return flows.stream().distinct().limit(40).toList();
    }

    private String normalizeRelationshipNode(String value, String filePath) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isBlank()) return null;
        int dot = trimmed.lastIndexOf('.');
        String node = dot >= 0 ? trimmed.substring(dot + 1) : trimmed;
        if (filePath != null && !filePath.isBlank()) {
            String normalizedFile = filePath.replace('\\', '/');
            String fileName = normalizedFile.substring(normalizedFile.lastIndexOf('/') + 1);
            if (fileName.endsWith(".java") || fileName.endsWith(".ts") || fileName.endsWith(".tsx") || fileName.endsWith(".js") || fileName.endsWith(".jsx") || fileName.endsWith(".py")) {
                String base = fileName.substring(0, fileName.lastIndexOf('.'));
                if (node.equalsIgnoreCase(base) || node.equalsIgnoreCase(base + "Controller") || node.equalsIgnoreCase(base + "Service") || node.equalsIgnoreCase(base + "Repository")) {
                    return node;
                }
            }
        }
        return node;
    }

    private String classifyArchitectureComponent(String filePath, String symbol) {
        String text = ((filePath == null ? "" : filePath) + " " + (symbol == null ? "" : symbol)).toLowerCase(Locale.ROOT);
        if (text.contains("frontend") || text.contains("page") || text.contains("component") || text.contains("view") || text.contains("ui/")) return "Frontend";
        if (text.contains("controller") || text.contains("route") || text.contains("api") || text.contains("endpoint") || text.contains("handler")) return "API";
        if (text.contains("service") || text.contains("manager") || text.contains("context") || text.contains("usecase") || text.contains("orchestrator")) return "Services";
        if (text.contains("repository") || text.contains("entity") || text.contains("model") || text.contains("dto") || text.contains("mapper") || text.contains("schema")) return "Data";
        if (text.contains("config") || text.contains("docker") || text.contains("kubernetes") || text.contains("deployment") || text.contains("security") || text.contains("auth") || text.contains("gateway") || text.contains("infra")) return "Infrastructure";
        return null;
    }

    private boolean isLowLevelArchitectureNoise(String value) {
        if (value == null) return true;
        String normalized = value.trim();
        if (normalized.isBlank()) return true;
        String lower = normalized.toLowerCase(Locale.ROOT);
        Set<String> noise = Set.of(
            "uuid", "list", "arraylist", "hashmap", "map", "set", "queue", "optional", "string", "stringbuilder",
            "object", "ioexception", "runtimeexception", "exception", "illegalargumentexception", "nullpointerexception",
            "logger", "console", "jsonnode", "httpclient", "resttemplate", "httprequest", "responseentity"
        );
        return noise.contains(lower) || lower.startsWith("java.") || lower.startsWith("javax.") || lower.startsWith("org.") || lower.startsWith("com.") || lower.startsWith("net.") || lower.startsWith("commons.");
    }

    private String buildMermaid(List<Flow> flows) {
        StringBuilder builder = new StringBuilder("graph LR\n");
        if (flows == null || flows.isEmpty()) {
            builder.append("  repo((Repository))\n");
            return builder.toString();
        }
        Set<String> seen = new LinkedHashSet<>();
        for (Flow flow : flows) {
            String from = id(flow.from());
            String to = id(flow.to());
            if (from == null || to == null || from.isBlank() || to.isBlank()) continue;
            String key = from + "->" + to;
            if (!seen.add(key)) continue;
            builder.append("  ").append(from).append("[").append(flow.from()).append("] --> ").append(to).append("[").append(flow.to()).append("]\n");
        }
        return builder.toString();
    }

    private List<Api> detectApis(ParsedRepositoryDTO parsed) {
        List<Api> result = new ArrayList<>();
        for (ParsedFileDTO file : parsed.getFiles()) if (file.getElements() != null) for (var element : file.getElements()) {
            String annotations = String.valueOf(element.getAnnotations());
            if (annotations.contains("GetMapping") || annotations.contains("PostMapping") || annotations.contains("PutMapping") || annotations.contains("DeleteMapping")) result.add(new Api(annotations.replaceAll(".*?(GetMapping|PostMapping|PutMapping|DeleteMapping).*", "$1"), "detected in " + file.getFilePath(), element.getName(), file.getFilePath()));
        }
        return result.stream().limit(100).toList();
    }

    private Map<String, Integer> detectTechnologies(List<RepositoryFile> files) {
        Map<String, Integer> found = new LinkedHashMap<>();
        for (RepositoryFile file : files) {
            String path = file.getFilePath().toLowerCase(Locale.ROOT);
            if (path.endsWith("package.json")) found.merge("Node.js / package.json", 1, Integer::sum);
            if (path.endsWith("pom.xml")) found.merge("Java / Maven", 1, Integer::sum);
            if (path.contains("docker")) found.merge("Docker", 1, Integer::sum);
            if (path.endsWith(".yml") || path.endsWith(".yaml")) found.merge("YAML configuration", 1, Integer::sum);
            if (path.contains("security") || path.contains("auth")) found.merge("Authentication code", 1, Integer::sum);
            if (path.contains("supabase") || path.contains("postgres") || path.contains("migration")) found.merge("PostgreSQL / persistence", 1, Integer::sum);
            if (path.contains("gemini") || path.contains("openai") || path.contains("groq")) found.merge("AI provider integration", 1, Integer::sum);
        }
        return found;
    }

    private String id(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }

    public enum Status { QUEUED, RUNNING, COMPLETED, FAILED }
    private static final class AnalysisJob {
        final UUID id; final UUID repositoryId; final Instant createdAt = Instant.now();
        volatile Instant updatedAt = createdAt; volatile Status status = Status.QUEUED; volatile int completedStage = -1; volatile String currentStage = STAGES.get(0); volatile String error; volatile Result result;
        AnalysisJob(UUID id, UUID repositoryId) { this.id = id; this.repositoryId = repositoryId; }
        JobView view() { return new JobView(id, repositoryId, status, completedStage + 1, STAGES.size(), currentStage, error, result, createdAt, updatedAt); }
    }
    public record JobView(UUID jobId, UUID repositoryId, Status status, int completedStages, int totalStages, String currentStage, String error, Result result, Instant createdAt, Instant updatedAt) {}
    public record Result(HighLevelDesign hld, LowLevelDesign lld, DatabaseDesign databaseDesign, ArchitectureInsights insights, String architectureStyle, String summary, List<Component> components, List<Flow> flows, List<Api> apis, List<String> technologies, List<Evidence> evidence, String mermaid) {}
    public record Component(String name, String purpose, String technology, List<String> files, List<Evidence> evidence) {}
    public record Flow(String from, String to, String evidence) {}
    public record Api(String method, String endpoint, String controller, String sourceFile) {}
    public record Evidence(String filePath, Integer line, String symbol, String reason, String confidence) {}
}
