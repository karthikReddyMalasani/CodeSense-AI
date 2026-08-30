package com.codesense.parser.service;

import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.ParsedFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * UML and Architecture diagram generation service.
 * Team Member 4 (Prashanthi) owns this class.
 *
 * Generates:
 * - PlantUML class diagrams
 * - PlantUML sequence diagrams (basic)
 * - Mermaid class diagrams
 * - Mermaid architecture flow diagrams
 *
 * IMPORTANT: Only generates diagrams from actual parser/dependency data.
 * Never invents relationships.
 */
@Slf4j
@Service
public class UmlDiagramService {

    /**
     * Generate a PlantUML class diagram from parsed files.
     */
    public String generatePlantUmlClassDiagram(List<ParsedFile> parsedFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("!theme plain\n");
        sb.append("skinparam classAttributeIconSize 0\n\n");

        for (ParsedFile file : parsedFiles) {
            Map<String, List<CodeElement>> membersByParent = indexMembers(file.getElements());
            for (CodeElement element : file.getElements()) {
                if (element.getType() == CodeElement.ElementType.CLASS) {
                    sb.append("class ").append(sanitize(element.getName())).append(" {\n");
                    appendClassMembers(sb, element, membersByParent);
                    sb.append("}\n\n");
                } else if (element.getType() == CodeElement.ElementType.INTERFACE) {
                    sb.append("interface ").append(sanitize(element.getName())).append(" {\n");
                    appendClassMembers(sb, element, membersByParent);
                    sb.append("}\n\n");
                } else if (element.getType() == CodeElement.ElementType.ENUM) {
                    sb.append("enum ").append(sanitize(element.getName())).append("\n\n");
                }
            }

            // Relationships
            for (var rel : file.getRelationships()) {
                if (rel.getType() == null) continue;
                switch (rel.getType()) {
                    case EXTENDS ->
                        sb.append(sanitize(rel.getSourceElement())).append(" --|> ")
                          .append(sanitize(rel.getTargetElement())).append("\n");
                    case IMPLEMENTS ->
                        sb.append(sanitize(rel.getSourceElement())).append(" ..|> ")
                          .append(sanitize(rel.getTargetElement())).append("\n");
                    default -> {}
                }
            }
        }

        sb.append("\n@enduml");
        return sb.toString();
    }

    /**
     * Generate a PlantUML component/architecture diagram.
     */
    public String generatePlantUmlArchitectureDiagram(List<ParsedFile> parsedFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("!theme plain\n");
        sb.append("title Architecture Overview\n\n");

        // Group files by package/module
        parsedFiles.stream()
            .collect(Collectors.groupingBy(f -> getTopDirectory(f.getFilePath())))
            .forEach((pkg, files) -> {
                if (pkg.isBlank()) return;
                sb.append("package \"").append(pkg).append("\" {\n");
                files.stream()
                    .flatMap(f -> f.getElements().stream())
                    .filter(e -> e.getType() == CodeElement.ElementType.CLASS
                              || e.getType() == CodeElement.ElementType.INTERFACE)
                    .limit(5)
                    .forEach(e -> sb.append("  [").append(sanitize(e.getName())).append("]\n"));
                sb.append("}\n\n");
            });

        sb.append("@enduml");
        return sb.toString();
    }

    /**
     * Generate a Mermaid class diagram from parsed files.
     */
    public String generateMermaidClassDiagram(List<ParsedFile> parsedFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("classDiagram\n");

        Set<String> addedClasses = new LinkedHashSet<>();

        for (ParsedFile file : parsedFiles) {
            Map<String, List<CodeElement>> membersByParent = indexMembers(file.getElements());
            for (CodeElement element : file.getElements()) {
                if ((element.getType() == CodeElement.ElementType.CLASS
                  || element.getType() == CodeElement.ElementType.INTERFACE)
                  && !addedClasses.contains(element.getName())) {
                    addedClasses.add(element.getName());
                    sb.append("    class ").append(sanitize(element.getName())).append(" {\n");
                    appendMermaidMembers(sb, element, membersByParent);
                    sb.append("    }\n");
                }
            }

            // Inheritance in Mermaid
            for (var rel : file.getRelationships()) {
                if (rel.getType() == null) continue;
                switch (rel.getType()) {
                    case EXTENDS ->
                        sb.append("    ").append(sanitize(rel.getSourceElement()))
                          .append(" --|> ").append(sanitize(rel.getTargetElement())).append("\n");
                    case IMPLEMENTS ->
                        sb.append("    ").append(sanitize(rel.getSourceElement()))
                          .append(" ..|> ").append(sanitize(rel.getTargetElement())).append("\n");
                    default -> {}
                }
            }
        }

        return sb.toString();
    }

    /**
     * Generate a Mermaid flowchart showing architecture layers.
     */
    public String generateMermaidArchitectureFlow(List<ParsedFile> parsedFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph TD\n");

        ArchitectureSnapshot snapshot = detectArchitectureLayers(parsedFiles);
        addLayerBlock(sb, "Controllers", snapshot.controllers());
        addLayerBlock(sb, "Services", snapshot.services());
        addLayerBlock(sb, "Repositories", snapshot.repositories());
        addLayerBlock(sb, "Models", snapshot.models());

        addLayerConnection(sb, snapshot.controllers(), snapshot.services());
        addLayerConnection(sb, snapshot.services(), snapshot.repositories());
        addLayerConnection(sb, snapshot.repositories(), snapshot.models());

        return sb.toString();
    }

    private ArchitectureSnapshot detectArchitectureLayers(List<ParsedFile> parsedFiles) {
        Set<String> controllers = new LinkedHashSet<>();
        Set<String> services = new LinkedHashSet<>();
        Set<String> repositories = new LinkedHashSet<>();
        Set<String> models = new LinkedHashSet<>();

        if (parsedFiles == null) {
            return new ArchitectureSnapshot(controllers, services, repositories, models);
        }

        for (ParsedFile file : parsedFiles) {
            if (file == null) {
                continue;
            }

            String filePath = file.getFilePath() == null ? "" : file.getFilePath().replace("\\", "/");
            classifyByFilePath(filePath, controllers, services, repositories, models);

            if (file.getElements() == null) {
                continue;
            }

            for (CodeElement element : file.getElements()) {
                if (element == null) continue;
                if (element.getType() != CodeElement.ElementType.CLASS
                    && element.getType() != CodeElement.ElementType.INTERFACE) {
                    continue;
                }

                String name = element.getName();
                if (name == null || name.isBlank()) {
                    continue;
                }

                if (hasAnnotation(element, "Controller", "RestController")) {
                    controllers.add(name);
                } else if (hasAnnotation(element, "Service")) {
                    services.add(name);
                } else if (hasAnnotation(element, "Repository")) {
                    repositories.add(name);
                }

                if (name.endsWith("Controller")) controllers.add(name);
                else if (name.endsWith("Service")) services.add(name);
                else if (name.endsWith("Repository")) repositories.add(name);
                else if (name.endsWith("Entity") || name.endsWith("Model")) models.add(name);
            }
        }

        return new ArchitectureSnapshot(controllers, services, repositories, models);
    }

    private void classifyByFilePath(String filePath, Set<String> controllers, Set<String> services,
                                   Set<String> repositories, Set<String> models) {
        if (filePath == null || filePath.isBlank()) return;
        String normalized = filePath.toLowerCase();

        if (normalized.contains("/controller/") || normalized.endsWith("controller.java") || normalized.contains("controller")) {
            String canonical = inferNameFromFilePath(filePath, "Controller");
            if (canonical != null) controllers.add(canonical);
        } else if (normalized.contains("/service/") || normalized.endsWith("service.java") || normalized.contains("service")) {
            String canonical = inferNameFromFilePath(filePath, "Service");
            if (canonical != null) services.add(canonical);
        } else if (normalized.contains("/repository/") || normalized.endsWith("repository.java") || normalized.contains("repository")) {
            String canonical = inferNameFromFilePath(filePath, "Repository");
            if (canonical != null) repositories.add(canonical);
        } else if (normalized.contains("/model/") || normalized.contains("/entity/") || normalized.contains("/domain/")) {
            String canonical = inferNameFromFilePath(filePath, "Model");
            if (canonical != null) models.add(canonical);
        }
    }

    private String inferNameFromFilePath(String filePath, String suffix) {
        String fileName = filePath.replace("\\", "/");
        int idx = fileName.lastIndexOf('/');
        String name = idx >= 0 ? fileName.substring(idx + 1) : fileName;
        String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        return base.endsWith(suffix) ? base : null;
    }

    private boolean hasAnnotation(CodeElement element, String... annotationNames) {
        if (element.getAnnotations() == null || element.getAnnotations().isEmpty()) return false;
        return element.getAnnotations().stream()
            .filter(a -> a != null)
            .anyMatch(annotation -> java.util.Arrays.stream(annotationNames)
                .anyMatch(name -> annotation.contains(name) || annotation.contains(name.toLowerCase())));
    }

    private void addLayerBlock(StringBuilder sb, String layerName, Set<String> nodes) {
        if (nodes == null || nodes.isEmpty()) return;

        sb.append("    subgraph ").append(layerName).append("\n");
        nodes.stream().limit(8).forEach(node ->
            sb.append("        ").append(sanitize(node)).append("[").append(node).append("]\n"));
        sb.append("    end\n");
    }

    private void addLayerConnection(StringBuilder sb, Set<String> sourceNodes, Set<String> targetNodes) {
        if (sourceNodes == null || targetNodes == null || sourceNodes.isEmpty() || targetNodes.isEmpty()) return;
        String source = sanitize(sourceNodes.iterator().next());
        String target = sanitize(targetNodes.iterator().next());
        sb.append("    ").append(source).append(" --> ").append(target).append("\n");
    }

    private record ArchitectureSnapshot(Set<String> controllers, Set<String> services,
                                       Set<String> repositories, Set<String> models) {}

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void appendClassMembers(StringBuilder sb, CodeElement cls,
                                    Map<String, List<CodeElement>> membersByParent) {
        membersByParent.getOrDefault(cls.getName(), List.of()).stream()
            .limit(10)
            .forEach(m -> {
                if (m.getType() == CodeElement.ElementType.FIELD) {
                    sb.append("  ").append(m.getName()).append(" : ")
                      .append(m.getReturnType() != null ? m.getReturnType() : "Object").append("\n");
                } else {
                    sb.append("  +").append(m.getName()).append("()\n");
                }
            });
    }

    private void appendMermaidMembers(StringBuilder sb, CodeElement cls,
                                      Map<String, List<CodeElement>> membersByParent) {
        membersByParent.getOrDefault(cls.getName(), List.of()).stream()
            .limit(8)
            .forEach(m -> {
                if (m.getType() == CodeElement.ElementType.FIELD) {
                    sb.append("        ").append(m.getReturnType() != null ? m.getReturnType() : "Object")
                      .append(" ").append(m.getName()).append("\n");
                } else {
                    sb.append("        +").append(m.getName()).append("()\n");
                }
            });
    }

    private Map<String, List<CodeElement>> indexMembers(List<CodeElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return Map.of();
        }

        return elements.stream()
            .filter(element -> element.getParentName() != null)
            .filter(element -> element.getType() == CodeElement.ElementType.METHOD
                            || element.getType() == CodeElement.ElementType.FIELD)
            .collect(Collectors.groupingBy(
                CodeElement::getParentName,
                java.util.LinkedHashMap::new,
                Collectors.toCollection(ArrayList::new)
            ));
    }

    private String sanitize(String name) {
        if (name == null) return "Unknown";
        return name.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private String getTopDirectory(String filePath) {
        if (filePath == null) return "";
        String[] parts = filePath.replace("\\", "/").split("/");
        return parts.length > 1 ? parts[0] : "";
    }
}
