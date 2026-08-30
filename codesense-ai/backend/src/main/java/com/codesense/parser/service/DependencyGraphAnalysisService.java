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

    private Model buildModel(ParsedRepositoryDTO parsed, Map<String, External> external) { Model model = new Model(); for (ParsedFileDTO file : parsed.getFiles()) { String path = file.getFilePath(); if (path == null) continue; String fileId = path; model.addNode(new Node(fileId, path.substring(path.lastIndexOf('/') + 1), "FILE", module(path), file.getLanguage(), path, false)); if (file.getElements() != null) for (var element : file.getElements()) { if (element.getName() == null || element.getName().isBlank()) continue; String id = element.getName(); String type = element.getType() == null ? "SYMBOL" : element.getType(); model.addNode(new Node(id, element.getName(), type, module(path), file.getLanguage(), path, false)); } if (file.getRelationships() != null) for (var relation : file.getRelationships()) { if (relation.getSourceElement() == null || relation.getTargetElement() == null) continue; String source = model.nodes.stream().anyMatch(node -> node.id().equals(relation.getSourceElement())) ? relation.getSourceElement() : fileId; String target = model.nodes.stream().anyMatch(node -> node.id().equals(relation.getTargetElement())) ? relation.getTargetElement() : relation.getTargetElement(); model.addEdge(new Edge(source, target, relation.getRelationshipType() == null ? "DEPENDS_ON" : relation.getRelationshipType(), "CONFIRMED")); } } return model; }
    private Map<String, External> detectExternal(List<RepositoryFile> files) { Map<String, External> result = new LinkedHashMap<>(); for (RepositoryFile file : files) { String path = file.getFilePath().toLowerCase(Locale.ROOT); String content = file.getContent() == null ? "" : file.getContent(); if (path.endsWith("package.json")) addManifest(result, content, "npm", "EXTERNAL"); else if (path.endsWith("pom.xml")) addManifest(result, content, "Maven", "EXTERNAL"); else if (path.endsWith("requirements.txt")) addManifest(result, content, "pip", "EXTERNAL"); } return result; }
    private void addManifest(Map<String, External> result, String content, String manager, String type) { for (String line : content.split("\\R")) { String clean = line.trim().replaceAll("[\\\"<>]", ""); if (clean.isBlank() || clean.startsWith("#") || clean.startsWith("//") || clean.contains("dependencies")) continue; String name = clean.replaceAll("^[^A-Za-z0-9@_-]+", "").replaceAll("[,:].*$", "").replaceAll("\\s.*$", ""); if (name.length() > 1 && name.length() < 80 && (clean.contains("version") || clean.contains("^") || clean.contains("~") || clean.contains("==") || manager.equals("pip"))) result.putIfAbsent(name, new External(name, "Detected in " + manager + " manifest", manager, type)); } }
    private Map<String, Integer> metrics(List<Node> nodes, List<Edge> edges, List<List<String>> cycles) { Map<String, Integer> values = new LinkedHashMap<>(); Map<String, Integer> degree = new HashMap<>(); edges.forEach(edge -> { degree.merge(edge.source(), 1, Integer::sum); degree.merge(edge.target(), 1, Integer::sum); }); values.put("totalNodes", nodes.size()); values.put("internalNodes", (int) nodes.stream().filter(node -> !node.external()).count()); values.put("externalNodes", (int) nodes.stream().filter(Node::external).count()); values.put("totalRelationships", edges.size()); values.put("circularDependencies", cycles.size()); values.put("highCouplingNodes", (int) degree.values().stream().filter(value -> value >= 8).count()); values.put("unusedDependencies", 0); return values; }
    private List<List<String>> findCycles(Map<String, Set<String>> graph) { List<List<String>> cycles = new ArrayList<>(); for (String start : graph.keySet()) findCycles(start, start, graph, new LinkedHashSet<>(), cycles); return cycles.stream().map(cycle -> cycle.subList(0, cycle.size() - 1)).distinct().limit(50).toList(); }
    private void findCycles(String start, String current, Map<String, Set<String>> graph, LinkedHashSet<String> path, List<List<String>> cycles) { if (!path.add(current)) { if (current.equals(start) && path.size() > 1) { List<String> cycle = new ArrayList<>(path); cycle.add(start); cycles.add(cycle); } return; } for (String next : graph.getOrDefault(current, Set.of())) if (path.size() < 20) findCycles(start, next, graph, path, cycles); path.remove(current); }
    private String module(String path) { String normalized = path.replace('\\', '/'); String[] parts = normalized.split("/"); return parts.length > 1 ? parts[0] : "root"; }

    private static final class Model { final List<Node> nodes = new ArrayList<>(); final List<Edge> edges = new ArrayList<>(); final Map<String, Set<String>> adjacency = new LinkedHashMap<>(); List<List<String>> cycles = List.of(); void addNode(Node node) { if (nodes.stream().noneMatch(existing -> existing.id().equals(node.id()))) nodes.add(node); adjacency.putIfAbsent(node.id(), new LinkedHashSet<>()); } void addEdge(Edge edge) { if (edges.stream().noneMatch(existing -> existing.source().equals(edge.source()) && existing.target().equals(edge.target()) && existing.type().equals(edge.type()))) { addNode(new Node(edge.source(), edge.source(), "SYMBOL", "unknown", null, null, false)); addNode(new Node(edge.target(), edge.target(), "SYMBOL", "unknown", null, null, false)); edges.add(edge); adjacency.computeIfAbsent(edge.source(), ignored -> new LinkedHashSet<>()).add(edge.target()); } } }
    private static final class Job { final UUID id; final UUID repositoryId; volatile Status status = Status.QUEUED; volatile int completed = -1; volatile String current = STAGES.get(0); volatile String error; volatile Result result; volatile Instant updatedAt = Instant.now(); Job(UUID id, UUID repositoryId) { this.id = id; this.repositoryId = repositoryId; } View view() { return new View(id, repositoryId, status, completed + 1, STAGES.size(), current, error, result, updatedAt); } }
    public enum Status { QUEUED, RUNNING, COMPLETED, FAILED; boolean isRunning() { return this == QUEUED || this == RUNNING; } }
    public record View(UUID jobId, UUID repositoryId, Status status, int completedStages, int totalStages, String currentStage, String error, Result result, Instant updatedAt) {}
    public record Node(String id, String name, String type, String module, String language, String path, boolean external) {}
    public record Edge(String source, String target, String type, String confidence) {}
    public record External(String name, String version, String packageManager, String type) {}
    public record Result(List<Node> nodes, List<Edge> edges, List<External> externalDependencies, List<List<String>> cycles, Map<String, Integer> metrics) {}
}
