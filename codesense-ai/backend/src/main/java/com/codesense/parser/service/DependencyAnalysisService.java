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
        List<DependencyEdge> edges = new ArrayList<>();

        for (ParsedFile file : parsedFiles) {
            // Add file as a node
            nodes.add(file.getFilePath());
            graph.addVertex(file.getFilePath());

            // Add class/module nodes
            for (CodeElement element : file.getElements()) {
                if (isTopLevelElement(element)) {
                    String nodeName = element.getName();
                    nodes.add(nodeName);
                    if (!graph.containsVertex(nodeName)) graph.addVertex(nodeName);
                }
            }

            // Add relationship edges
            for (CodeRelationship rel : file.getRelationships()) {
                String src = rel.getSourceElement();
                String tgt = rel.getTargetElement();
                if (src != null && tgt != null) {
                    if (!graph.containsVertex(src)) graph.addVertex(src);
                    if (!graph.containsVertex(tgt)) graph.addVertex(tgt);
                    try {
                        graph.addEdge(src, tgt);
                    } catch (Exception ignored) {}
                    edges.add(DependencyEdge.builder()
                        .source(src).target(tgt)
                        .type(rel.getType() != null ? rel.getType().name() : "DEPENDS_ON")
                        .build());
                }
            }
        }

        // Compute statistics
        Map<String, Integer> inDegree = new HashMap<>();
        for (String node : graph.vertexSet()) {
            inDegree.put(node, graph.inDegreeOf(node));
        }

        List<String> topNodes = inDegree.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        return DependencyGraph.builder()
            .nodes(new ArrayList<>(nodes))
            .edges(edges)
            .topDependencies(topNodes)
            .nodeCount(graph.vertexSet().size())
            .edgeCount(graph.edgeSet().size())
            .build();
    }

    /**
     * Generate a Mermaid diagram from the dependency graph.
     */
    public String generateMermaidDependencyDiagram(DependencyGraph graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph LR\n");

        // Limit to 50 edges for readability
        int limit = Math.min(graph.getEdges().size(), 50);
        Set<String> includedEdges = new LinkedHashSet<>();

        for (int i = 0; i < limit; i++) {
            DependencyEdge edge = graph.getEdges().get(i);
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

        for (ParsedFile file : parsedFiles) {
            List<CodeElement> classes = file.getElements().stream()
                .filter(e -> e.getType() == CodeElement.ElementType.CLASS
                          || e.getType() == CodeElement.ElementType.INTERFACE
                          || e.getType() == CodeElement.ElementType.ENUM)
                .collect(Collectors.toList());

            for (CodeElement cls : classes) {
                sb.append("    class ").append(sanitizeMermaid(cls.getName())).append(" {\n");
                // Add methods
                file.getElements().stream()
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
            for (CodeRelationship rel : file.getRelationships()) {
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

    // ─── DTOs ────────────────────────────────────────────────────────────────

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
