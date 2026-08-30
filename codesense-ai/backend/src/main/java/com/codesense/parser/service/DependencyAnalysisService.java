package com.codesense.parser.service;

import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.CodeRelationship;
import com.codesense.parser.model.ParsedFile;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dependency graph and relationship analysis service.
 * Team Member 4 (Prashanthi) owns this class.
 *
 * Uses JGraphT for graph construction.
 * Generates:
 * - Import dependency graph
 * - Class inheritance graph
 * - Method call graph
 * - Module/package relationships
 */
@Slf4j
@Service
public class DependencyAnalysisService {

    /**
     * Build a dependency graph from all parsed files.
     * Returns serializable graph metadata (not the JGraphT object directly).
     */
    public DependencyGraph buildDependencyGraph(List<ParsedFile> parsedFiles) {
        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Set<String> nodes = new LinkedHashSet<>();
        Set<String> seenEdges = new HashSet<>();
        List<DependencyEdge> edges = new ArrayList<>();

        if (parsedFiles == null || parsedFiles.isEmpty()) {
            return emptyGraph();
        }

        for (ParsedFile file : parsedFiles) {
            if (file == null) continue;
            addFileNode(nodes, graph, file.getFilePath());
            addTopLevelElementNodes(nodes, graph, file.getElements());
            addRelationshipEdges(nodes, graph, edges, seenEdges, file.getRelationships());
        }

        return DependencyGraph.builder()
            .nodes(new ArrayList<>(nodes))
            .edges(edges)
            .topDependencies(buildTopDependencies(graph))
            .nodeCount(graph.vertexSet().size())
            .edgeCount(graph.edgeSet().size())
            .build();
    }

    private DependencyGraph emptyGraph() {
        return DependencyGraph.builder()
            .nodes(List.of())
            .edges(List.of())
            .topDependencies(List.of())
            .nodeCount(0)
            .edgeCount(0)
            .build();
    }

    private void addFileNode(Set<String> nodes, Graph<String, DefaultEdge> graph, String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        nodes.add(filePath);
        graph.addVertex(filePath);
    }

    private void addTopLevelElementNodes(Set<String> nodes, Graph<String, DefaultEdge> graph, List<CodeElement> fileElements) {
        if (fileElements == null) return;
        for (CodeElement element : fileElements) {
            if (element == null || !isTopLevelElement(element)) continue;
            String nodeName = element.getName();
            if (nodeName == null || nodeName.isBlank()) continue;
            nodes.add(nodeName);
            if (!graph.containsVertex(nodeName)) graph.addVertex(nodeName);
        }
    }

    private void addRelationshipEdges(Set<String> nodes, Graph<String, DefaultEdge> graph,
                                     List<DependencyEdge> edges, Set<String> seenEdges,
                                     List<CodeRelationship> fileRelationships) {
        if (fileRelationships == null) return;

        for (CodeRelationship rel : fileRelationships) {
            if (rel == null) continue;
            String src = rel.getSourceElement();
            String tgt = rel.getTargetElement();
            if (src == null || src.isBlank() || tgt == null || tgt.isBlank()) continue;

            nodes.add(src);
            nodes.add(tgt);
            if (!graph.containsVertex(src)) graph.addVertex(src);
            if (!graph.containsVertex(tgt)) graph.addVertex(tgt);

            String edgeKey = src + "->" + tgt + "::" + (rel.getType() != null ? rel.getType().name() : "DEPENDS_ON");
            if (seenEdges.contains(edgeKey)) continue;

            try {
                DefaultEdge added = graph.addEdge(src, tgt);
                if (added != null) {
                    edges.add(DependencyEdge.builder()
                        .source(src)
                        .target(tgt)
                        .type(rel.getType() != null ? rel.getType().name() : "DEPENDS_ON")
                        .build());
                    seenEdges.add(edgeKey);
                }
            } catch (IllegalArgumentException ex) {
                log.debug("Skipping invalid dependency edge {} -> {}: {}", src, tgt, ex.getMessage());
            }
        }
    }

    private List<String> buildTopDependencies(Graph<String, DefaultEdge> graph) {
        Map<String, Integer> inDegree = new HashMap<>();
        for (String node : graph.vertexSet()) {
            inDegree.put(node, graph.inDegreeOf(node));
        }

        return inDegree.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Generate a Mermaid diagram from the dependency graph.
     */
    public String generateMermaidDependencyDiagram(DependencyGraph graph) {
        return generateMermaidDependencyDiagram(graph, "LR");
    }

    /**
     * Generate a Mermaid diagram from the dependency graph using the provided direction.
     * Supported directions: LR (left->right), TB (top->bottom), RL (right->left), BT (bottom->top)
     */
    public String generateMermaidDependencyDiagram(DependencyGraph graph, String direction) {
        String dir = (direction == null || direction.isBlank()) ? "LR" : direction.toUpperCase();
        if (!Set.of("LR", "TB", "RL", "BT").contains(dir)) {
            dir = "LR"; // fallback
        }

        if (graph == null || graph.getEdges() == null || graph.getEdges().isEmpty()) {
            return "graph " + dir + "\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("graph ").append(dir).append("\n");

        // Limit to 50 edges for readability
        int limit = Math.min(graph.getEdges().size(), 50);
        Set<String> includedEdges = new LinkedHashSet<>();

        for (int i = 0; i < limit; i++) {
            DependencyEdge edge = graph.getEdges().get(i);
            if (edge == null) continue;
            String src = sanitizeMermaid(edge.getSource());
            String tgt = sanitizeMermaid(edge.getTarget());
            String key = src + "-->" + tgt;
            if (!includedEdges.contains(key)) {
                sb.append("    ").append(src).append(" --> ").append(tgt).append("\n");
                includedEdges.add(key);
            }
        }

        return sb.toString();
    }

    /**
     * Generate a Mermaid class diagram from parsed files.
     */
    public String generateMermaidClassDiagram(List<ParsedFile> parsedFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("classDiagram\n");

        if (parsedFiles == null || parsedFiles.isEmpty()) {
            return sb.toString();
        }

        for (ParsedFile file : parsedFiles) {
            if (file == null) continue;
            List<CodeElement> fileElements = file.getElements() != null ? file.getElements() : List.of();
            List<CodeRelationship> fileRelationships = file.getRelationships() != null ? file.getRelationships() : List.of();

            List<CodeElement> classes = fileElements.stream()
                .filter(e -> e.getType() == CodeElement.ElementType.CLASS
                          || e.getType() == CodeElement.ElementType.INTERFACE
                          || e.getType() == CodeElement.ElementType.ENUM)
                .collect(Collectors.toList());

            for (CodeElement cls : classes) {
                if (cls.getName() == null || cls.getName().isBlank()) {
                    continue;
                }
                sb.append("    class ").append(sanitizeMermaid(cls.getName())).append(" {\n");
                // Add methods
                fileElements.stream()
                    .filter(e -> cls.getName().equals(e.getParentName())
                             && (e.getType() == CodeElement.ElementType.METHOD
                                 || e.getType() == CodeElement.ElementType.CONSTRUCTOR))
                    .limit(10)
                    .forEach(m -> {
                        String ret = m.getReturnType() != null ? m.getReturnType() : "void";
                        sb.append("        +").append(m.getName()).append("() ").append(ret).append("\n");
                    });
                sb.append("    }\n");
            }

            // Add inheritance relationships
            for (CodeRelationship rel : fileRelationships) {
                if (rel.getType() == CodeRelationship.RelationshipType.EXTENDS) {
                    sb.append("    ").append(sanitizeMermaid(rel.getSourceElement()))
                      .append(" --|> ").append(sanitizeMermaid(rel.getTargetElement())).append("\n");
                } else if (rel.getType() == CodeRelationship.RelationshipType.IMPLEMENTS) {
                    sb.append("    ").append(sanitizeMermaid(rel.getSourceElement()))
                      .append(" ..|> ").append(sanitizeMermaid(rel.getTargetElement())).append("\n");
                }
            }
        }

        return sb.toString();
    }

    private boolean isTopLevelElement(CodeElement element) {
        return element.getType() == CodeElement.ElementType.CLASS
            || element.getType() == CodeElement.ElementType.INTERFACE
            || element.getType() == CodeElement.ElementType.ENUM
            || element.getType() == CodeElement.ElementType.MODULE;
    }

    private String sanitizeMermaid(String name) {
        if (name == null) return "unknown";
        // Mermaid node IDs cannot have certain characters
        return name.replaceAll("[^A-Za-z0-9_]", "_");
    }

    // ─── DTOs ─────────────────────────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DependencyGraph {
        private List<String> nodes;
        private List<DependencyEdge> edges;
        private List<String> topDependencies;
        private int nodeCount;
        private int edgeCount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DependencyEdge {
        private String source;
        private String target;
        private String type;
    }
}
