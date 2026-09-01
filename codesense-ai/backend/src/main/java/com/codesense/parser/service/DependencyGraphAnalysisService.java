package com.codesense.parser.service;

import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import com.codesense.repository.model.RepositoryFile;
import com.codesense.repository.repository.RepositoryFileRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
public class DependencyGraphAnalysisService {
    private static final List<String> STAGES = List.of("Reading project structure", "Detecting project type", "Detecting programming languages", "Reading dependency configuration", "Scanning source files", "Identifying modules/packages", "Identifying classes/components", "Extracting imports", "Extracting exports", "Resolving internal dependencies", "Resolving external dependencies", "Building dependency relationships", "Detecting circular dependencies", "Detecting highly coupled modules", "Calculating dependency metrics", "Building dependency graph", "Organizing graph hierarchy", "Generating dependency analysis", "Rendering interactive graph", "Finalizing report");
    private final RepositoryParserService parserService;
    private final RepositoryFileRepository fileRepository;
    private final Executor executor;
    private final Map<UUID, Job> jobs = new ConcurrentHashMap<>();

    public DependencyGraphAnalysisService(RepositoryParserService parserService,
                                          RepositoryFileRepository fileRepository,
                                          @Qualifier("aiTaskExecutor") Executor executor) {
        this.parserService = parserService;
        this.fileRepository = fileRepository;
        this.executor = executor;
    }

    public View start(UUID repositoryId) { Job existing = jobs.get(repositoryId); if (existing != null && existing.status.isRunning()) return existing.view(); Job job = new Job(UUID.randomUUID(), repositoryId); jobs.put(repositoryId, job); executor.execute(() -> analyze(job)); return job.view(); }
    public View get(UUID repositoryId, UUID jobId) { Job job = jobs.get(repositoryId); if (job == null || !job.id.equals(jobId)) throw new NoSuchElementException("Dependency analysis not found"); return job.view(); }
    private void analyze(Job job) { try { job.status = Status.RUNNING; stage(job, 0); List<RepositoryFile> files = fileRepository.findByRepositoryIdAndIgnoredFalse(job.repositoryId); stage(job, 1); stage(job, 2); Map<String, External> external = detectExternal(files); stage(job, 3); stage(job, 4); ParsedRepositoryDTO parsed = parserService.parseRepository(job.repositoryId); stage(job, 5); stage(job, 6); stage(job, 7); stage(job, 8); Model model = buildModel(parsed, external); stage(job, 9); stage(job, 10); stage(job, 11); model.cycles = findCycles(model.adjacency); stage(job, 12); stage(job, 13); stage(job, 14); stage(job, 15); stage(job, 16); job.result = new Result(model.nodes, model.edges, new ArrayList<>(external.values()), model.cycles, metrics(model.nodes, model.edges, model.cycles)); stage(job, 17); stage(job, 18); stage(job, 19); job.status = Status.COMPLETED; } catch (Exception error) { job.status = Status.FAILED; job.error = error.getMessage() == null ? "Dependency analysis failed" : error.getMessage(); } job.updatedAt = Instant.now(); }
    private void stage(Job job, int index) { job.completed = Math.max(job.completed, index); job.current = STAGES.get(Math.min(index, STAGES.size() - 1)); job.updatedAt = Instant.now(); }

    private Model buildModel(ParsedRepositoryDTO parsed, Map<String, External> external) {
        Model model = new Model();
        String repositoryKey = parsed != null && parsed.getRepositoryId() != null ? parsed.getRepositoryId() + ":" : "";

        for (ParsedFileDTO file : parsed == null || parsed.getFiles() == null ? List.<ParsedFileDTO>of() : parsed.getFiles()) {
            if (file == null) continue;
            String normalizedPath = normalizeRepositoryPath(file.getFilePath());
            if (normalizedPath == null || normalizedPath.isBlank() || !isSemanticFile(normalizedPath)) continue;

            String moduleName = inferModule(normalizedPath);
            if (file.getElements() != null) {
                for (var element : file.getElements()) {
                    if (element == null || element.getName() == null || element.getName().isBlank()) continue;
                    String symbol = normalizeSymbolName(element.getName());
                    if (symbol == null || symbol.isBlank() || isLowLevelNoise(symbol)) continue;
                    String type = classifyElementType(element.getType(), element.getName(), normalizedPath);
                    String nodeId = repositoryKey + moduleName + "#" + normalizedPath + "#" + symbol;
                    model.addNode(new Node(nodeId, symbol, type, moduleName, file.getLanguage(), normalizedPath, false));
                }
            }

            if (file.getRelationships() != null) {
                for (var relation : file.getRelationships()) {
                    if (relation == null) continue;
                    String source = normalizeSymbolName(relation.getSourceElement());
                    String target = normalizeSymbolName(relation.getTargetElement());
                    if (source == null || target == null || source.isBlank() || target.isBlank()) continue;
                    if (isLowLevelNoise(source) || isLowLevelNoise(target)) continue;

                    String sourceId = resolveNodeId(model, normalizedPath, source, moduleName, repositoryKey);
                    String targetId = resolveNodeId(model, normalizedPath, target, moduleName, repositoryKey);
                    if (sourceId == null || targetId == null) {
                        sourceId = repositoryKey + moduleName + "#" + normalizedPath + "#" + source;
                        targetId = repositoryKey + moduleName + "#" + normalizedPath + "#" + target;
                    }

                    String relationshipType = classifyRelationshipType(relation.getRelationshipType(), source, target);
                    if (sourceId.equals(targetId)) continue;

                    String sourceLabel = source;
                    String targetLabel = target;
                    model.addNode(new Node(sourceId, sourceLabel, "CLASS", moduleName, file.getLanguage(), normalizedPath, false));
                    model.addNode(new Node(targetId, targetLabel, "CLASS", moduleName, file.getLanguage(), normalizedPath, false));
                    model.addEdge(new Edge(sourceId, targetId, relationshipType, "CONFIRMED"));
                }
            }
        }

        model.pruneNoise();
        return model;
    }

    private String resolveNodeId(Model model, String filePath, String symbol, String moduleName, String repositoryKey) {
        String normalizedSymbol = normalizeSymbolName(symbol);
        if (normalizedSymbol == null || normalizedSymbol.isBlank()) return null;

        List<Node> sameFileMatches = model.nodes.stream()
            .filter(node -> node.path() != null && node.path().equalsIgnoreCase(filePath))
            .filter(node -> node.name().equalsIgnoreCase(normalizedSymbol))
            .toList();
        if (sameFileMatches.size() == 1) return sameFileMatches.get(0).id();

        List<Node> globalMatches = model.nodes.stream()
            .filter(node -> node.name().equalsIgnoreCase(normalizedSymbol))
            .toList();
        if (globalMatches.size() == 1) return globalMatches.get(0).id();

        return repositoryKey + moduleName + "#" + filePath + "#" + normalizedSymbol;
    }

    private boolean isSemanticFile(String filePath) {
        if (filePath == null || filePath.isBlank()) return false;
        String normalized = filePath.replace('\\', '/').toLowerCase(Locale.ROOT);
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);
        Set<String> excludedNames = Set.of(
            ".gitignore", ".gitattributes", ".editorconfig", ".project", ".classpath",
            "readme.md", "readme.txt", "dockerfile", "docker-compose.yml", "docker-compose.yaml",
            "package-lock.json", "pom.xml", "build.gradle", "settings.gradle", "gradlew",
            "mvnw", "mvnw.cmd", "npm-shrinkwrap.json", "yarn.lock", "pnpm-lock.yaml",
            "vercel.json", "nginx.conf", "application.yml", "application-dev.yml", "application-prod.yml",
            "requirements.txt", "pyproject.toml", "tsconfig.json", "vite.config.js", "vite.config.ts"
        );
        if (excludedNames.contains(name) || normalized.contains("/.git/") || normalized.contains("/node_modules/")) {
            return false;
        }
        return normalized.endsWith(".java") || normalized.endsWith(".kt") || normalized.endsWith(".js")
            || normalized.endsWith(".jsx") || normalized.endsWith(".ts") || normalized.endsWith(".tsx")
            || normalized.endsWith(".py") || normalized.endsWith(".go") || normalized.endsWith(".cs")
            || normalized.endsWith(".rb") || normalized.endsWith(".php") || normalized.endsWith(".rs")
            || normalized.endsWith(".cpp") || normalized.endsWith(".c") || normalized.endsWith(".h")
            || normalized.endsWith(".scala");
    }

    private String normalizeRepositoryPath(String value) {
        if (value == null) return null;
        String normalized = value.replace('\\', '/').replaceAll("^\\./+", "");
        normalized = normalized.replaceAll("/+", "/");
        normalized = normalized.replaceAll("(?i)^.*?(?:src|backend|frontend|app|project)/", "");
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.matches(".*?/(?:src|backend|frontend|app|project)/.*")) {
            int idx = normalized.indexOf("/src/");
            if (idx >= 0) normalized = normalized.substring(idx + 1);
        }
        return normalized.isBlank() ? null : normalized;
    }

    private String inferModule(String path) {
        if (path == null || path.isBlank()) return "root";
        String normalized = path.replace('\\', '/');
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("/frontend/")) return "frontend";
        if (lower.contains("/backend/")) return "backend";
        if (lower.contains("/src/main/java/")) {
            String afterPackage = normalized.substring(normalized.indexOf("/src/main/java/") + "/src/main/java/".length());
            String[] segments = afterPackage.split("/");
            if (segments.length > 0 && !segments[0].isBlank() && !segments[0].equals("com") && !segments[0].equals("codesense")) return segments[0];
            if (segments.length > 1 && !segments[1].isBlank()) return segments[1];
        }
        String[] parts = normalized.split("/");
        for (String part : parts) {
            if (!part.isBlank() && !part.equals("src") && !part.equals("main") && !part.equals("java") && !part.equals("resources") && !part.equals("test") && !part.equals("frontend") && !part.equals("backend")) return part;
        }
        return "root";
    }

    private String displayFileName(String path) {
        if (path == null) return "unknown";
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private String normalizeSymbolName(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isBlank()) return null;
        int dot = trimmed.lastIndexOf('.');
        String normalized = dot >= 0 ? trimmed.substring(dot + 1) : trimmed;
        normalized = normalized.replaceAll("\\s+", "");
        return normalized.isBlank() ? null : normalized;
    }

    private String classifyElementType(String declaredType, String elementName, String path) {
        String type = declaredType == null ? "CLASS" : declaredType.toUpperCase(Locale.ROOT);
        String signature = (elementName == null ? "" : elementName).toLowerCase(Locale.ROOT);
        if (signature.contains("controller")) return "CONTROLLER";
        if (signature.contains("service")) return "SERVICE";
        if (signature.contains("repository")) return "REPOSITORY";
        if (signature.contains("entity")) return "ENTITY";
        if (signature.contains("mapper")) return "MAPPER";
        if (type.equals("INTERFACE")) return "INTERFACE";
        if (type.equals("CLASS")) return "CLASS";
        if (type.equals("METHOD") || type.equals("FUNCTION")) return "METHOD";
        return "CLASS";
    }

    private String classifyRelationshipType(String relationshipType, String source, String target) {
        String type = relationshipType == null ? "DEPENDS_ON" : relationshipType.trim().toUpperCase(Locale.ROOT);
        if (type.contains("IMPORT") || type.contains("PACKAGE")) return "IMPORTS";
        if (type.contains("CALL") || type.contains("INVOKE")) return "CALLS";
        if (type.contains("EXTEND")) return "EXTENDS";
        if (type.contains("IMPLEMENT")) return "IMPLEMENTS";
        if (type.contains("USE")) return "USES";
        if (source != null && source.toLowerCase(Locale.ROOT).contains("controller")) return "DEPENDS_ON";
        if (target != null && target.toLowerCase(Locale.ROOT).contains("repository")) return "DEPENDS_ON";
        return "DEPENDS_ON";
    }

    private boolean isLowLevelNoise(String value) {
        if (value == null) return true;
        String normalized = value.trim();
        if (normalized.isBlank()) return true;
        String lower = normalized.toLowerCase(Locale.ROOT);
        Set<String> noise = Set.of(
            "uuid", "list", "arraylist", "hashmap", "hashset", "set", "queue",
            "optional", "string", "integer", "long", "boolean", "double", "float",
            "object", "ioexception", "runtimeexception", "exception", "illegalargumentexception",
            "nullpointerexception", "logger", "console", "tostring", "stream", "filter",
            "collect", "log", "info", "warn", "error", "json", "responseentity", "modelmapper",
            "readme", "gitignore", "dockerfile", "nginx", "application", "pom", "package"
        );
        if (noise.contains(lower)) return true;
        return lower.startsWith("java.") || lower.startsWith("javax.") || lower.startsWith("org.") || lower.startsWith("com.") || lower.startsWith("net.") || lower.startsWith("spring.") || lower.startsWith("jakarta.");
    }

    private Map<String, External> detectExternal(List<RepositoryFile> files) {
        Map<String, External> result = new LinkedHashMap<>();
        for (RepositoryFile file : files) {
            if (file == null) continue;
            String path = file.getFilePath() == null ? "" : file.getFilePath().toLowerCase(Locale.ROOT);
            String content = file.getContent() == null ? "" : file.getContent();
            String contentLower = content.toLowerCase(Locale.ROOT);
            if (path.endsWith("package.json")) addManifest(result, content, "npm", "EXTERNAL");
            else if (path.endsWith("pom.xml")) addManifest(result, content, "Maven", "EXTERNAL");
            else if (path.endsWith("requirements.txt")) addManifest(result, content, "pip", "EXTERNAL");
            else if (path.endsWith("docker-compose.yml") || path.endsWith("docker-compose.yaml") || path.contains("docker")) {
                if (contentLower.contains("postgres") || contentLower.contains("postgresql")) result.putIfAbsent("PostgreSQL", new External("PostgreSQL", "Detected in Docker config", "Docker", "DATABASE"));
                if (contentLower.contains("redis")) result.putIfAbsent("Redis", new External("Redis", "Detected in Docker config", "Docker", "DATABASE"));
            }
            if (contentLower.contains("gemini") || contentLower.contains("openai") || contentLower.contains("groq")) {
                result.putIfAbsent("AI Provider", new External("AI Provider", "Detected in source configuration", "SDK", "AI_PROVIDER"));
            }
            if (contentLower.contains("spring-boot") || contentLower.contains("springframework")) {
                result.putIfAbsent("Spring Boot", new External("Spring Boot", "Detected in Java dependency config", "Maven", "FRAMEWORK"));
            }
            if (contentLower.contains("react") && (path.endsWith("package.json") || path.endsWith(".jsx") || path.endsWith(".tsx"))) {
                result.putIfAbsent("React", new External("React", "Detected in frontend source", "npm", "FRAMEWORK"));
            }
        }
        return result;
    }

    private void addManifest(Map<String, External> result, String content, String manager, String type) {
        for (String line : content.split("\\R")) {
            String clean = line.trim().replaceAll("[\\\"<>]", "");
            if (clean.isBlank() || clean.startsWith("#") || clean.startsWith("//") || clean.contains("dependencies") || clean.contains("<project") || clean.contains("<dependencies>") || clean.contains("<dependency")) {
                if (manager.equals("Maven") && clean.contains("<groupId>")) {
                    String name = clean.replaceAll(".*<groupId>", "").replaceAll("</groupId>.*", "");
                    if (name.length() > 1) result.putIfAbsent(name, new External(name, "Detected in Maven manifest", manager, type));
                }
                continue;
            }
            String name = clean.replaceAll("^[^A-Za-z0-9@_.-]+", "").replaceAll("[,:].*$", "").replaceAll("\\s.*$", "");
            if (name.length() > 1 && name.length() < 80 && (clean.contains("version") || clean.contains("^") || clean.contains("~") || clean.contains("==") || manager.equals("pip"))) {
                result.putIfAbsent(name, new External(name, "Detected in " + manager + " manifest", manager, type));
            }
        }
    }

    private Map<String, Integer> metrics(List<Node> nodes, List<Edge> edges, List<List<String>> cycles) {
        Map<String, Integer> values = new LinkedHashMap<>();
        Map<String, Integer> degree = new HashMap<>();
        edges.forEach(edge -> {
            degree.merge(edge.source(), 1, Integer::sum);
            degree.merge(edge.target(), 1, Integer::sum);
        });
        values.put("totalNodes", nodes.size());
        values.put("internalNodes", (int) nodes.stream().filter(node -> !node.external()).count());
        values.put("externalNodes", (int) nodes.stream().filter(Node::external).count());
        values.put("totalRelationships", edges.size());
        values.put("circularDependencies", cycles.size());
        values.put("highCouplingNodes", (int) degree.values().stream().filter(value -> value >= 8).count());
        values.put("unusedDependencies", 0);
        return values;
    }

    private List<List<String>> findCycles(Map<String, Set<String>> graph) {
        List<List<String>> cycles = new ArrayList<>();
        for (String start : graph.keySet()) findCycles(start, start, graph, new LinkedHashSet<>(), cycles);
        return cycles.stream().map(cycle -> cycle.subList(0, cycle.size() - 1)).distinct().limit(50).toList();
    }

    private void findCycles(String start, String current, Map<String, Set<String>> graph, LinkedHashSet<String> path, List<List<String>> cycles) {
        if (!path.add(current)) {
            if (current.equals(start) && path.size() > 1) {
                List<String> cycle = new ArrayList<>(path);
                cycle.add(start);
                cycles.add(cycle);
            }
            return;
        }
        for (String next : graph.getOrDefault(current, Set.of())) if (path.size() < 20) findCycles(start, next, graph, path, cycles);
        path.remove(current);
    }

    private String module(String path) {
        return inferModule(path);
    }

    private static final class Model {
        final List<Node> nodes = new ArrayList<>();
        final List<Edge> edges = new ArrayList<>();
        final Map<String, Set<String>> adjacency = new LinkedHashMap<>();
        List<List<String>> cycles = List.of();

        void addNode(Node node) {
            if (nodes.stream().noneMatch(existing -> existing.id().equals(node.id()))) nodes.add(node);
            adjacency.putIfAbsent(node.id(), new LinkedHashSet<>());
        }

        void addEdge(Edge edge) {
            if (edges.stream().noneMatch(existing -> existing.source().equals(edge.source()) && existing.target().equals(edge.target()) && existing.type().equals(edge.type()))) {
                addNode(new Node(edge.source(), edge.source(), "CLASS", "unknown", null, null, false));
                addNode(new Node(edge.target(), edge.target(), "CLASS", "unknown", null, null, false));
                edges.add(edge);
                adjacency.computeIfAbsent(edge.source(), ignored -> new LinkedHashSet<>()).add(edge.target());
            }
        }

        void pruneNoise() {
            List<Edge> filtered = edges.stream()
                .filter(edge -> edge.type() != null)
                .filter(edge -> !edge.type().equals("CONTAINS"))
                .filter(edge -> edge.source() != null && edge.target() != null)
                .filter(edge -> !edge.source().equals(edge.target()))
                .toList();
            edges.clear();
            edges.addAll(filtered);
        }
    }
    private static final class Job { final UUID id; final UUID repositoryId; volatile Status status = Status.QUEUED; volatile int completed = -1; volatile String current = STAGES.get(0); volatile String error; volatile Result result; volatile Instant updatedAt = Instant.now(); Job(UUID id, UUID repositoryId) { this.id = id; this.repositoryId = repositoryId; } View view() { return new View(id, repositoryId, status, completed + 1, STAGES.size(), current, error, result, updatedAt); } }
    public enum Status { QUEUED, RUNNING, COMPLETED, FAILED; boolean isRunning() { return this == QUEUED || this == RUNNING; } }
    public record View(UUID jobId, UUID repositoryId, Status status, int completedStages, int totalStages, String currentStage, String error, Result result, Instant updatedAt) {}
    public record Node(String id, String name, String type, String module, String language, String path, boolean external) {}
    public record Edge(String source, String target, String type, String confidence) {}
    public record External(String name, String version, String packageManager, String type) {}
    public record Result(List<Node> nodes, List<Edge> edges, List<External> externalDependencies, List<List<String>> cycles, Map<String, Integer> metrics) {}
}
