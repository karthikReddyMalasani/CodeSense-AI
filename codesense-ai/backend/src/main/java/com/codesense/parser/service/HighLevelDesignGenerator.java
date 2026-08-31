package com.codesense.parser.service;

import com.codesense.integration.parser.dto.CodeElementDTO;
import com.codesense.integration.parser.dto.CodeRelationshipDTO;
import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import com.codesense.parser.model.HighLevelDesign;
import com.codesense.parser.model.HighLevelDesign.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates High-Level Design (HLD) from parsed repository data.
 * HLD describes major components, their responsibilities, and interactions.
 */
@Service
public class HighLevelDesignGenerator {

    public HighLevelDesign generate(ParsedRepositoryDTO parsed) {
        HighLevelDesign.HighLevelDesignBuilder builder = HighLevelDesign.builder();

        builder.systemOverview(generateSystemOverview(parsed));
        builder.components(generateComponents(parsed));
        builder.communications(generateCommunications(parsed));
        builder.architectureDiagram(generateArchitectureMermaid(parsed));
        builder.architecturalStyle(inferArchitecturalStyle(parsed));
        builder.externalSystems(detectExternalSystems(parsed));
        builder.deploymentArchitecture(detectDeploymentArchitecture(parsed));

        return builder.build();
    }

    private SystemOverview generateSystemOverview(ParsedRepositoryDTO parsed) {
        String purpose = inferSystemPurpose(parsed);
        List<String> capabilities = inferCapabilities(parsed);
        List<String> technologies = detectMainTechnologies(parsed);

        return SystemOverview.builder()
                .applicationName(parsed.getRepositoryName())
                .purpose(purpose)
                .majorCapabilities(capabilities)
                .mainTechnologies(technologies)
                .description(generateSystemDescription(parsed, purpose, capabilities))
                .build();
    }

    private String inferSystemPurpose(ParsedRepositoryDTO parsed) {
        // Analyze file paths and class names to infer purpose
        Set<String> keywords = new HashSet<>();
        for (ParsedFileDTO file : parsed.getFiles()) {
            String path = safePath(file.getFilePath()).toLowerCase();
            if (path.contains("auth")) keywords.add("authentication");
            if (path.contains("payment") || path.contains("billing")) keywords.add("payment processing");
            if (path.contains("chat") || path.contains("message")) keywords.add("messaging");
            if (path.contains("analytics") || path.contains("metric")) keywords.add("analytics");
            if (path.contains("search")) keywords.add("search capability");
            if (path.contains("ml") || path.contains("ai")) keywords.add("AI/ML features");
            if (path.contains("parser") || path.contains("analysis")) keywords.add("code analysis");
        }

        if (keywords.isEmpty()) {
            return "Multi-purpose application with backend and frontend components";
        }
        return "Application providing: " + String.join(", ", keywords);
    }

    private List<String> inferCapabilities(ParsedRepositoryDTO parsed) {
        List<String> capabilities = new ArrayList<>();

        // Check for API/REST capabilities
        long controllers = parsed.getFiles().stream()
                .flatMap(f -> f.getElements().stream())
                .filter(e -> e.getType() != null && e.getType().contains("Controller"))
                .count();
        if (controllers > 0) capabilities.add("REST API endpoints");

        // Check for authentication
        long authClasses = parsed.getFiles().stream()
            .filter(f -> safePath(f.getFilePath()).toLowerCase().contains("auth"))
                .count();
        if (authClasses > 0) capabilities.add("Authentication and Authorization");

        // Check for persistence
        long entities = parsed.getFiles().stream()
                .flatMap(f -> f.getElements().stream())
                .filter(e -> e.getType() != null && (e.getType().contains("Entity") || e.getName().endsWith("Entity")))
                .count();
        if (entities > 0) capabilities.add("Data persistence and modeling");

        // Check for frontend
        if (parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).contains("frontend") || safePath(f.getFilePath()).endsWith(".jsx") || safePath(f.getFilePath()).endsWith(".tsx"))) {
            capabilities.add("Web user interface");
        }

        // Check for business logic services
        long services = parsed.getFiles().stream()
                .flatMap(f -> f.getElements().stream())
                .filter(e -> e.getType() != null && e.getType().contains("Service"))
                .count();
        if (services > 0) capabilities.add("Complex business logic and orchestration");

        return capabilities.isEmpty() ? List.of("Code processing and analysis") : capabilities;
    }

    private List<String> detectMainTechnologies(ParsedRepositoryDTO parsed) {
        Set<String> techs = new LinkedHashSet<>();

        if (parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).endsWith(".java"))) {
            techs.add("Java");
        }
        if (parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).endsWith(".jsx") || safePath(f.getFilePath()).endsWith(".tsx"))) {
            techs.add("React");
        }
        if (parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).contains("frontend"))) {
            techs.add("Web Frontend");
        }
        if (parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).endsWith("pom.xml"))) {
            techs.add("Maven");
        }
        if (parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).endsWith("package.json"))) {
            techs.add("Node.js/npm");
        }
        if (parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).contains("docker"))) {
            techs.add("Docker");
        }
        if (parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).contains("postgres") || safePath(f.getFilePath()).contains("database"))) {
            techs.add("PostgreSQL");
        }
        if (parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).contains("redis"))) {
            techs.add("Redis");
        }

        return new ArrayList<>(techs);
    }

    private String generateSystemDescription(ParsedRepositoryDTO parsed, String purpose, List<String> capabilities) {
        return String.format("A comprehensive system with %d parsed files and %d detected relationships. %s. Major capabilities: %s.",
                parsed.getFiles().size(),
                parsed.getTotalRelationships(),
                purpose,
                String.join(", ", capabilities));
    }

    private List<ArchitectureComponent> generateComponents(ParsedRepositoryDTO parsed) {
        List<ArchitectureComponent> components = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Map<String, Integer> componentCounts = new HashMap<>();
        Map<String, String> componentEvidence = new HashMap<>();

        // Detect components based on file paths and class names
        for (ParsedFileDTO file : parsed.getFiles()) {
            String type = detectComponentType(file.getFilePath());
            if (type != null && !seen.contains(type)) {
                componentCounts.merge(type, 1, Integer::sum);
                componentEvidence.putIfAbsent(type, file.getFilePath());
            }

            if (file.getElements() != null) {
                for (CodeElementDTO element : file.getElements()) {
                    String elemType = classifyElement(element, file.getFilePath());
                    if (elemType != null && !seen.contains(elemType)) {
                        componentCounts.merge(elemType, 1, Integer::sum);
                        componentEvidence.putIfAbsent(elemType, file.getFilePath() + " (class: " + element.getName() + ")");
                    }
                }
            }
        }

        // Create components
        String[] componentTypes = {"Frontend", "API/Controller", "Services", "Repository/Data Access", "Database", "Authentication", "Cache", "AI/ML Provider", "External APIs", "Infrastructure"};
        Map<String, String> typeDescriptions = Map.ofEntries(
                Map.entry("Frontend", "User interface and client-side components"),
                Map.entry("API/Controller", "HTTP endpoints and request handlers"),
                Map.entry("Services", "Business logic and domain services"),
                Map.entry("Repository/Data Access", "Data access objects and repositories"),
                Map.entry("Database", "Data persistence layer"),
                Map.entry("Authentication", "Authentication and authorization"),
                Map.entry("Cache", "Caching layer for performance"),
                Map.entry("AI/ML Provider", "AI/ML integration and services"),
                Map.entry("External APIs", "External service integrations"),
                Map.entry("Infrastructure", "Infrastructure and configuration")
        );

        for (String compType : componentTypes) {
            if (componentCounts.containsKey(compType)) {
                ArchitectureComponent component = ArchitectureComponent.builder()
                        .name(compType)
                        .type(compType)
                        .description(typeDescriptions.getOrDefault(compType, ""))
                        .technology(inferComponentTechnology(compType, parsed))
                        .evidence(componentEvidence.getOrDefault(compType, "Detected from source structure"))
                        .confidence("CONFIRMED")
                        .build();
                components.add(component);
                seen.add(compType);
            }
        }

        return components;
    }

    private String detectComponentType(String filePath) {
        String lower = safePath(filePath).toLowerCase();
        if (lower.contains("frontend") || lower.contains("component") || lower.contains("page") || lower.contains("ui/")) return "Frontend";
        if (lower.contains("controller") || lower.contains("route") || lower.contains("api")) return "API/Controller";
        if (lower.contains("service") || lower.contains("manager") || lower.contains("orchestrat")) return "Services";
        if (lower.contains("repository") || lower.contains("dao") || lower.contains("mapper")) return "Repository/Data Access";
        if (lower.contains("entity") || lower.contains("model")) return "Database";
        if (lower.contains("auth") || lower.contains("security")) return "Authentication";
        if (lower.contains("cache") || lower.contains("redis")) return "Cache";
        if (lower.contains("ai") || lower.contains("ml") || lower.contains("gemini") || lower.contains("openai")) return "AI/ML Provider";
        if (lower.contains("external") || lower.contains("integration")) return "External APIs";
        if (lower.contains("config") || lower.contains("docker") || lower.contains("deploy")) return "Infrastructure";
        return null;
    }

    private String classifyElement(CodeElementDTO element, String filePath) {
        if (element.getName() == null || element.getType() == null) return null;

        String name = element.getName();
        String type = element.getType();
        String lower = (name + " " + type + " " + filePath).toLowerCase();

        if (lower.contains("controller")) return "API/Controller";
        if (lower.contains("service")) return "Services";
        if (lower.contains("repository")) return "Repository/Data Access";
        if (lower.contains("entity")) return "Database";
        if (lower.contains("component") || lower.contains("page")) return "Frontend";

        return null;
    }

    private String safePath(String filePath) {
        return filePath == null ? "" : filePath;
    }

    private String inferComponentTechnology(String componentType, ParsedRepositoryDTO parsed) {
        return switch (componentType) {
            case "Frontend" -> "React/Vue/Angular";
            case "API/Controller" -> "Spring Boot/Express/FastAPI";
            case "Services" -> "Business Logic Layer";
            case "Repository/Data Access" -> "ORM/JDBC/SQL";
            case "Database" -> "PostgreSQL/MySQL";
            case "Authentication" -> "OAuth2/JWT";
            case "Cache" -> "Redis";
            case "AI/ML Provider" -> "Gemini/OpenAI/Groq";
            default -> "Detected from source";
        };
    }

    private List<ComponentCommunication> generateCommunications(ParsedRepositoryDTO parsed) {
        List<ComponentCommunication> communications = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Detect communications from relationships
        for (ParsedFileDTO file : parsed.getFiles()) {
            if (file.getRelationships() == null) continue;

            for (CodeRelationshipDTO rel : file.getRelationships()) {
                String fromComp = detectComponentType(rel.getSourceFile());
                String toComp = detectComponentType(rel.getTargetFile());

                if (fromComp != null && toComp != null && !fromComp.equals(toComp)) {
                    String key = fromComp + "->" + toComp;
                    if (!seen.contains(key)) {
                        String protocol = inferProtocol(rel.getRelationshipType());
                        communications.add(ComponentCommunication.builder()
                                .from(fromComp)
                                .to(toComp)
                                .protocol(protocol)
                                .description(generateCommunicationDescription(fromComp, toComp, rel.getRelationshipType()))
                                .evidence("Detected from " + rel.getSourceFile() + " → " + rel.getTargetFile())
                                .confidence("CONFIRMED")
                                .build());
                        seen.add(key);
                    }
                }
            }
        }

        // If no relationships found, infer standard communication patterns
        if (communications.isEmpty()) {
            communications.addAll(inferStandardCommunications(parsed));
        }

        return communications;
    }

    private String inferProtocol(String relationshipType) {
        return switch (relationshipType != null ? relationshipType : "") {
            case "CALLS" -> "Direct method call";
            case "IMPORTS", "DEPENDS_ON", "USES" -> "Dependency injection";
            case "IMPLEMENTS", "EXTENDS" -> "Interface/Abstract";
            default -> "Unspecified";
        };
    }

    private String generateCommunicationDescription(String from, String to, String relationshipType) {
        return String.format("%s calls/uses %s", from, to);
    }

    private List<ComponentCommunication> inferStandardCommunications(ParsedRepositoryDTO parsed) {
        List<ComponentCommunication> comms = new ArrayList<>();
        boolean hasFrontend = parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).contains("frontend"));
        boolean hasApi = parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).contains("controller"));
        boolean hasService = parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).contains("service"));
        boolean hasRepository = parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).contains("repository"));
        boolean hasEntity = parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).contains("entity"));

        if (hasFrontend && hasApi) {
            comms.add(ComponentCommunication.builder()
                    .from("Frontend").to("API/Controller").protocol("REST/HTTP")
                    .description("Frontend sends HTTP requests to API endpoints")
                    .confidence("INFERRED").build());
        }
        if (hasApi && hasService) {
            comms.add(ComponentCommunication.builder()
                    .from("API/Controller").to("Services").protocol("Direct method call")
                    .description("Controllers delegate to service layer")
                    .confidence("INFERRED").build());
        }
        if (hasService && hasRepository) {
            comms.add(ComponentCommunication.builder()
                    .from("Services").to("Repository/Data Access").protocol("Direct method call")
                    .description("Services use repositories for data access")
                    .confidence("INFERRED").build());
        }
        if (hasRepository && hasEntity) {
            comms.add(ComponentCommunication.builder()
                    .from("Repository/Data Access").to("Database").protocol("SQL/ORM")
                    .description("Repositories execute database queries")
                    .confidence("INFERRED").build());
        }

        return comms;
    }

    private String generateArchitectureMermaid(ParsedRepositoryDTO parsed) {
        StringBuilder sb = new StringBuilder("graph TB\n");
        List<ComponentCommunication> comms = generateCommunications(parsed);

        Set<String> components = new HashSet<>();
        for (ComponentCommunication comm : comms) {
            components.add(comm.getFrom());
            components.add(comm.getTo());
        }

        // Add component nodes
        for (String comp : components) {
            String id = comp.replace(" ", "").replace("/", "");
            sb.append("  ").append(id).append("[\"").append(comp).append("\"]\n");
        }

        // Add communications
        for (ComponentCommunication comm : comms) {
            String fromId = comm.getFrom().replace(" ", "").replace("/", "");
            String toId = comm.getTo().replace(" ", "").replace("/", "");
            sb.append("  ").append(fromId).append(" -->|").append(comm.getProtocol()).append("| ").append(toId).append("\n");
        }

        return sb.toString();
    }

    private String inferArchitecturalStyle(ParsedRepositoryDTO parsed) {
        // Detect if it's layered, microservices, etc.
        long frontendFiles = parsed.getFiles().stream().filter(f -> safePath(f.getFilePath()).contains("frontend")).count();
        long backendFiles = parsed.getFiles().stream().filter(f -> safePath(f.getFilePath()).contains("backend")).count();
        long serviceCount = parsed.getFiles().stream()
                .flatMap(f -> f.getElements().stream())
                .filter(e -> e.getName() != null && e.getName().endsWith("Service"))
                .count();

        if (frontendFiles > 0 && backendFiles > 0) {
            return "Layered Monolith (Frontend + Backend)";
        } else if (serviceCount > 10) {
            return "Service-Oriented Architecture";
        } else if (parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).contains("docker"))) {
            return "Containerized Application";
        }
        return "Modular Layered Architecture";
    }

    private List<ExternalSystem> detectExternalSystems(ParsedRepositoryDTO parsed) {
        List<ExternalSystem> systems = new ArrayList<>();
        Set<String> detected = new HashSet<>();

        for (ParsedFileDTO file : parsed.getFiles()) {
            String lower = (file.getFilePath() + " " + (file.getContent() != null ? file.getContent().substring(0, Math.min(500, file.getContent().length())) : "")).toLowerCase();

            if (lower.contains("oauth") || lower.contains("auth0")) {
                if (detected.add("OAuth2")) {
                    systems.add(ExternalSystem.builder().name("OAuth2/Auth0").type("AUTH_PROVIDER")
                            .purpose("User authentication and identity management")
                            .technology("OAuth2").evidence(file.getFilePath()).build());
                }
            }
            if (lower.contains("gemini") || lower.contains("openai") || lower.contains("groq")) {
                if (detected.add("AI Provider")) {
                    systems.add(ExternalSystem.builder().name("AI Provider (Gemini/OpenAI/Groq)").type("AI_PROVIDER")
                            .purpose("Large language model and embeddings").technology("Gemini/OpenAI/Groq").evidence(file.getFilePath()).build());
                }
            }
            if (lower.contains("stripe") || lower.contains("payment")) {
                if (detected.add("Payment")) {
                    systems.add(ExternalSystem.builder().name("Payment Gateway").type("PAYMENT_PROVIDER")
                            .purpose("Payment processing").technology("Stripe/Razorpay").evidence(file.getFilePath()).build());
                }
            }
            if (lower.contains("supabase") || lower.contains("firebase")) {
                if (detected.add("Backend Service")) {
                    systems.add(ExternalSystem.builder().name("Backend Service").type("BaaS")
                            .purpose("Backend-as-a-Service").technology("Supabase/Firebase").evidence(file.getFilePath()).build());
                }
            }
            if (lower.contains("email") || lower.contains("smtp") || lower.contains("mailgun")) {
                if (detected.add("Email Service")) {
                    systems.add(ExternalSystem.builder().name("Email Service").type("COMMUNICATION")
                            .purpose("Email delivery").technology("SMTP/Mailgun").evidence(file.getFilePath()).build());
                }
            }
        }

        return systems;
    }

    private DeploymentArchitecture detectDeploymentArchitecture(ParsedRepositoryDTO parsed) {
        List<DeploymentComponent> components = new ArrayList<>();

        if (parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).contains("dockerfile") || safePath(f.getFilePath()).contains("docker-compose"))) {
            components.add(DeploymentComponent.builder()
                    .name("Containerized Services").hostingType("Docker Containers")
                    .technology("Docker & Docker Compose").evidence("Dockerfile/docker-compose.yml detected").build());
        }

        if (parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).contains("kubernetes") || safePath(f.getFilePath()).contains("k8s"))) {
            components.add(DeploymentComponent.builder()
                    .name("Kubernetes Orchestration").hostingType("Kubernetes")
                    .technology("Kubernetes").evidence("K8s manifest files detected").build());
        }

        if (parsed.getFiles().stream().anyMatch(f -> safePath(f.getFilePath()).endsWith("vercel.json") || safePath(f.getFilePath()).endsWith("netlify.toml"))) {
            components.add(DeploymentComponent.builder()
                    .name("Frontend Hosting").hostingType("Cloud")
                    .technology("Vercel/Netlify").evidence("Cloud deployment config detected").build());
        }

        if (components.isEmpty()) {
            return DeploymentArchitecture.builder()
                    .description("Not detected from repository.")
                    .components(List.of())
                    .build();
        }

        return DeploymentArchitecture.builder()
                .description("The application uses containerization for deployment")
                .components(components)
                .containerization(components.stream().map(DeploymentComponent::getTechnology).collect(Collectors.joining(", ")))
                .build();
    }
}
