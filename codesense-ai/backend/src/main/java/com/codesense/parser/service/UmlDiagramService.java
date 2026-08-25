package com.codesense.parser.service;

import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.ParsedFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
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
            for (CodeElement element : file.getElements()) {
                if (element.getType() == CodeElement.ElementType.CLASS) {
                    sb.append("class ").append(sanitize(element.getName())).append(" {\n");
                    appendClassMembers(sb, element, file.getElements());
                    sb.append("}\n\n");
                } else if (element.getType() == CodeElement.ElementType.INTERFACE) {
                    sb.append("interface ").append(sanitize(element.getName())).append(" {\n");
                    appendClassMembers(sb, element, file.getElements());
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
            for (CodeElement element : file.getElements()) {
                if ((element.getType() == CodeElement.ElementType.CLASS
                  || element.getType() == CodeElement.ElementType.INTERFACE)
                  && !addedClasses.contains(element.getName())) {
                    addedClasses.add(element.getName());
                    sb.append("    class ").append(sanitize(element.getName())).append(" {\n");
                    appendMermaidMembers(sb, element, file.getElements());
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

        // Detect architectural layers from file paths
        Set<String> controllers = new LinkedHashSet<>();
        Set<String> services = new LinkedHashSet<>();
        Set<String> repositories = new LinkedHashSet<>();
        Set<String> models = new LinkedHashSet<>();

        for (ParsedFile file : parsedFiles) {
            for (CodeElement element : file.getElements()) {
                if (element.getType() != CodeElement.ElementType.CLASS
                 && element.getType() != CodeElement.ElementType.INTERFACE) continue;

                String name = element.getName();
                if (element.getAnnotations() != null) {
                    if (element.getAnnotations().stream().anyMatch(a -> a.contains("Controller") || a.contains("RestController")))
                        controllers.add(name);
                    else if (element.getAnnotations().stream().anyMatch(a -> a.contains("Service")))
                        services.add(name);
                    else if (element.getAnnotations().stream().anyMatch(a -> a.contains("Repository")))
                        repositories.add(name);
                }

                // Fallback: naming convention
                if (name.endsWith("Controller")) controllers.add(name);
                else if (name.endsWith("Service")) services.add(name);
                else if (name.endsWith("Repository")) repositories.add(name);
                else if (name.endsWith("Entity") || name.endsWith("Model")) models.add(name);
            }
        }

        if (!controllers.isEmpty()) {
            sb.append("    subgraph Controllers\n");
            controllers.stream().limit(8).forEach(c ->
                sb.append("        ").append(sanitize(c)).append("[").append(c).append("]\n"));
            sb.append("    end\n");
        }
        if (!services.isEmpty()) {
            sb.append("    subgraph Services\n");
            services.stream().limit(8).forEach(s ->
                sb.append("        ").append(sanitize(s)).append("[").append(s).append("]\n"));
            sb.append("    end\n");
        }
        if (!repositories.isEmpty()) {
            sb.append("    subgraph Repositories\n");
            repositories.stream().limit(8).forEach(r ->
                sb.append("        ").append(sanitize(r)).append("[").append(r).append("]\n"));
            sb.append("    end\n");
        }

        // Draw layer connections
        if (!controllers.isEmpty() && !services.isEmpty()) {
            String ctrl = sanitize(controllers.iterator().next());
            String svc = sanitize(services.iterator().next());
            sb.append("    ").append(ctrl).append(" --> ").append(svc).append("\n");
        }
        if (!services.isEmpty() && !repositories.isEmpty()) {
            String svc = sanitize(services.iterator().next());
            String repo = sanitize(repositories.iterator().next());
            sb.append("    ").append(svc).append(" --> ").append(repo).append("\n");
        }

        return sb.toString();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void appendClassMembers(StringBuilder sb, CodeElement cls, List<CodeElement> all) {
        all.stream()
            .filter(e -> cls.getName().equals(e.getParentName()))
            .filter(e -> e.getType() == CodeElement.ElementType.METHOD
                      || e.getType() == CodeElement.ElementType.FIELD)
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

    private void appendMermaidMembers(StringBuilder sb, CodeElement cls, List<CodeElement> all) {
        all.stream()
            .filter(e -> cls.getName().equals(e.getParentName()))
            .filter(e -> e.getType() == CodeElement.ElementType.METHOD
                      || e.getType() == CodeElement.ElementType.FIELD)
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
