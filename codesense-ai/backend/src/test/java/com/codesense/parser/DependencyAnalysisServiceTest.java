package com.codesense.parser;

import com.codesense.parser.service.DependencyAnalysisService;
import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.CodeRelationship;
import com.codesense.parser.model.ParsedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for dependency graph analysis.
 * Team Member 5 (Vishnu) — Testing.
 */
class DependencyAnalysisServiceTest {

    private DependencyAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new DependencyAnalysisService();
    }

    @Test
    void buildDependencyGraph_emptyList_returnsEmptyGraph() {
        DependencyAnalysisService.DependencyGraph graph = service.buildDependencyGraph(List.of());
        assertThat(graph).isNotNull();
        assertThat(graph.getNodes()).isEmpty();
        assertThat(graph.getEdges()).isEmpty();
    }

    @Test
    void buildDependencyGraph_withRelationships_createsEdges() {
        ParsedFile file1 = ParsedFile.builder()
            .filePath("AuthController.java")
            .language("Java")
            .elements(List.of(
                CodeElement.builder()
                    .name("AuthController")
                    .type(CodeElement.ElementType.CLASS)
                    .filePath("AuthController.java").build()
            ))
            .relationships(List.of(
                CodeRelationship.builder()
                    .sourceElement("AuthController")
                    .targetElement("AuthService")
                    .type(CodeRelationship.RelationshipType.CALLS)
                    .sourceFile("AuthController.java").build()
            ))
            .build();

        DependencyAnalysisService.DependencyGraph graph = service.buildDependencyGraph(List.of(file1));

        assertThat(graph.getNodeCount()).isGreaterThan(0);
        assertThat(graph.getEdgeCount()).isGreaterThan(0);
        assertThat(graph.getEdges()).anyMatch(e ->
            e.getSource().equals("AuthController") && e.getTarget().equals("AuthService"));
    }

    @Test
    void generateMermaidDependencyDiagram_returnsValidMermaid() {
        DependencyAnalysisService.DependencyGraph graph = DependencyAnalysisService.DependencyGraph.builder()
            .nodes(List.of("A", "B", "C"))
            .edges(List.of(
                new DependencyAnalysisService.DependencyEdge("A", "B", "CALLS"),
                new DependencyAnalysisService.DependencyEdge("B", "C", "IMPORTS")
            ))
            .nodeCount(3).edgeCount(2).build();

        String mermaid = service.generateMermaidDependencyDiagram(graph);

        assertThat(mermaid).startsWith("graph LR");
        assertThat(mermaid).contains("-->");
    }

    @Test
    void generateMermaidClassDiagram_returnsClassDiagramSyntax() {
        ParsedFile file = ParsedFile.builder()
            .filePath("User.java")
            .language("Java")
            .elements(List.of(
                CodeElement.builder().name("User").type(CodeElement.ElementType.CLASS)
                    .filePath("User.java").build(),
                CodeElement.builder().name("getId").type(CodeElement.ElementType.METHOD)
                    .parentName("User").filePath("User.java").build()
            ))
            .relationships(List.of())
            .build();

        String diagram = service.generateMermaidClassDiagram(List.of(file));

        assertThat(diagram).contains("classDiagram");
        assertThat(diagram).contains("User");
    }
}
