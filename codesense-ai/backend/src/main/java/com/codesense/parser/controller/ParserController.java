package com.codesense.parser.controller;

import com.codesense.common.dto.ApiResponse;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import com.codesense.parser.service.CodeMetricsService;
import com.codesense.parser.service.DependencyAnalysisService;
import com.codesense.parser.service.RepositoryParserService;
import com.codesense.parser.service.UmlDiagramService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Parser & Code Intelligence REST API.
 * Team Member 4 (Prashanthi) owns this controller.
 *
 * Provides endpoints for:
 * - Repository parsing
 * - Dependency graph generation
 * - UML diagram generation
 * - Code metrics
 * - Architecture diagram generation
 */
@RestController
@RequestMapping("/api/parser")
@RequiredArgsConstructor
@Tag(name = "Parser & Code Intelligence", description = "Multi-language code parsing and analysis (Team Member 4 — Prashanthi)")
@SecurityRequirement(name = "bearerAuth")
public class ParserController {

    private final RepositoryParserService repositoryParserService;
    private final CodeMetricsService codeMetricsService;
    private final DependencyAnalysisService dependencyAnalysisService;
    private final UmlDiagramService umlDiagramService;

    @PostMapping("/repositories/{repositoryId}/parse")
    @Operation(summary = "Parse all files in a repository (JavaParser + Tree-sitter)")
    public ResponseEntity<ApiResponse<ParsedRepositoryDTO>> parseRepository(
            @PathVariable UUID repositoryId) {
        ParsedRepositoryDTO result = repositoryParserService.parseRepository(repositoryId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/repositories/{repositoryId}/metrics")
    @Operation(summary = "Get code metrics for a repository")
    public ResponseEntity<ApiResponse<CodeMetricsService.RepositoryMetricsSummary>> getMetrics(
            @PathVariable UUID repositoryId) {
        CodeMetricsService.RepositoryMetricsSummary metrics =
            codeMetricsService.calculateRepositoryMetrics(repositoryId);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @PostMapping("/repositories/{repositoryId}/dependency-graph")
    @Operation(summary = "Generate dependency graph from parsed repository")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDependencyGraph(
            @PathVariable UUID repositoryId) {
        ParsedRepositoryDTO parsed = repositoryParserService.parseRepository(repositoryId);
        List<com.codesense.parser.model.ParsedFile> parsedFiles = convertToModel(parsed);

        DependencyAnalysisService.DependencyGraph graph =
            dependencyAnalysisService.buildDependencyGraph(parsedFiles);
        String mermaid = dependencyAnalysisService.generateMermaidDependencyDiagram(graph);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "graph", graph,
            "mermaid", mermaid,
            "nodeCount", graph.getNodeCount(),
            "edgeCount", graph.getEdgeCount()
        )));
    }

    @PostMapping("/repositories/{repositoryId}/uml")
    @Operation(summary = "Generate UML class diagram from parsed repository")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateUml(
            @PathVariable UUID repositoryId) {
        ParsedRepositoryDTO parsed = repositoryParserService.parseRepository(repositoryId);
        List<com.codesense.parser.model.ParsedFile> parsedFiles = convertToModel(parsed);

        String plantUml = umlDiagramService.generatePlantUmlClassDiagram(parsedFiles);
        String mermaid = umlDiagramService.generateMermaidClassDiagram(parsedFiles);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "plantUml", plantUml,
            "mermaid", mermaid
        )));
    }

    @PostMapping("/repositories/{repositoryId}/architecture")
    @Operation(summary = "Generate architecture diagram from parsed repository")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateArchitecture(
            @PathVariable UUID repositoryId) {
        ParsedRepositoryDTO parsed = repositoryParserService.parseRepository(repositoryId);
        List<com.codesense.parser.model.ParsedFile> parsedFiles = convertToModel(parsed);

        String plantUml = umlDiagramService.generatePlantUmlArchitectureDiagram(parsedFiles);
        String mermaid = umlDiagramService.generateMermaidArchitectureFlow(parsedFiles);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "plantUml", plantUml,
            "mermaid", mermaid
        )));
    }

    // ─── Helper mappers ──────────────────────────────────────────────────────

    private List<com.codesense.parser.model.ParsedFile> convertToModel(ParsedRepositoryDTO parsed) {
        return parsed.getFiles().stream()
            .map(f -> com.codesense.parser.model.ParsedFile.builder()
                .filePath(f.getFilePath())
                .language(f.getLanguage())
                .content(f.getContent())
                .elements(f.getElements() != null ? f.getElements().stream()
                    .map(e -> com.codesense.parser.model.CodeElement.builder()
                        .name(e.getName()).type(mapElementType(e.getType()))
                        .language(e.getLanguage()).filePath(e.getFilePath())
                        .startLine(e.getStartLine()).endLine(e.getEndLine())
                        .annotations(e.getAnnotations()).returnType(e.getReturnType())
                        .build())
                    .toList() : List.of())
                .relationships(f.getRelationships() != null ? f.getRelationships().stream()
                    .map(r -> com.codesense.parser.model.CodeRelationship.builder()
                        .sourceElement(r.getSourceElement()).targetElement(r.getTargetElement())
                        .type(mapRelType(r.getRelationshipType()))
                        .sourceFile(r.getSourceFile()).build())
                    .toList() : List.of())
                .build())
            .toList();
    }

    private com.codesense.parser.model.CodeElement.ElementType mapElementType(String type) {
        if (type == null) return com.codesense.parser.model.CodeElement.ElementType.MODULE;
        try {
            return com.codesense.parser.model.CodeElement.ElementType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return com.codesense.parser.model.CodeElement.ElementType.MODULE;
        }
    }

    private com.codesense.parser.model.CodeRelationship.RelationshipType mapRelType(String type) {
        if (type == null) return com.codesense.parser.model.CodeRelationship.RelationshipType.DEPENDS_ON;
        try {
            return com.codesense.parser.model.CodeRelationship.RelationshipType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return com.codesense.parser.model.CodeRelationship.RelationshipType.DEPENDS_ON;
        }
    }
}
