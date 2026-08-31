package com.codesense.parser.service;

import com.codesense.integration.parser.dto.CodeElementDTO;
import com.codesense.integration.parser.dto.CodeRelationshipDTO;
import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import com.codesense.parser.model.LowLevelDesign;
import com.codesense.parser.model.LowLevelDesign.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generates Low-Level Design (LLD) from parsed repository data.
 * LLD provides detailed class, method, and relationship information.
 */
@Service
public class LowLevelDesignGenerator {

    public LowLevelDesign generate(ParsedRepositoryDTO parsed) {
        LowLevelDesign.LowLevelDesignBuilder builder = LowLevelDesign.builder();

        builder.packages(generatePackages(parsed));
        builder.classes(generateClasses(parsed));
        builder.classRelationships(generateClassRelationships(parsed));
        builder.services(generateServices(parsed));
        builder.repositories(generateRepositories(parsed));
        builder.controllers(generateControllers(parsed));
        builder.entities(generateEntities(parsed));
        builder.sequenceDiagrams(generateSequenceDiagrams(parsed));
        builder.classDiagram(generateClassDiagram(parsed));

        return builder.build();
    }

    private List<PackageStructure> generatePackages(ParsedRepositoryDTO parsed) {
        Map<String, List<String>> packageClasses = new LinkedHashMap<>();

        for (ParsedFileDTO file : parsed.getFiles()) {
            if (file.getElements() == null) continue;

            String packageName = extractPackageName(file.getFilePath());
            if (packageName == null || packageName.isBlank()) continue;

            packageClasses.computeIfAbsent(packageName, pkg -> new ArrayList<>()).addAll(
                    file.getElements().stream()
                            .filter(e -> e.getName() != null)
                            .map(CodeElementDTO::getName)
                            .collect(Collectors.toList())
            );
        }

        return packageClasses.entrySet().stream()
                .map(entry -> PackageStructure.builder()
                        .packageName(entry.getKey())
                        .path(extractPackagePath(entry.getKey()))
                        .purpose(inferPackagePurpose(entry.getKey()))
                        .classes(entry.getValue())
                        .subPackages(new ArrayList<>())
                        .build())
                .limit(50)
                .collect(Collectors.toList());
    }

    private String extractPackageName(String filePath) {
        // For src/main/java/com/example/service/AuthService.java -> com.example.service
        if (filePath == null || !filePath.contains("java")) return null;

        String[] parts = filePath.split("[/\\\\]");
        int javaIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if ("java".equals(parts[i])) javaIndex = i;
        }
        if (javaIndex < 0 || javaIndex >= parts.length - 1) return null;

        StringBuilder pkg = new StringBuilder();
        for (int i = javaIndex + 1; i < parts.length - 1; i++) {
            if (pkg.length() > 0) pkg.append(".");
            pkg.append(parts[i]);
        }
        return pkg.toString();
    }

    private String extractPackagePath(String packageName) {
        return "src/main/java/" + packageName.replace(".", "/");
    }

    private String inferPackagePurpose(String packageName) {
        String lower = packageName.toLowerCase();
        if (lower.contains("service")) return "Business logic and domain services";
        if (lower.contains("controller") || lower.contains("api")) return "REST API endpoints";
        if (lower.contains("repository") || lower.contains("dao")) return "Data access layer";
        if (lower.contains("entity") || lower.contains("model")) return "Domain entities and models";
        if (lower.contains("dto")) return "Data transfer objects";
        if (lower.contains("config")) return "Application configuration";
        if (lower.contains("security") || lower.contains("auth")) return "Security and authentication";
        if (lower.contains("util")) return "Utility and helper functions";
        if (lower.contains("exception")) return "Exception handling";
        return "Application logic";
    }

    private List<ClassDesign> generateClasses(ParsedRepositoryDTO parsed) {
        List<ClassDesign> classes = new ArrayList<>();

        for (ParsedFileDTO file : parsed.getFiles()) {
            if (file.getElements() == null) continue;

            for (CodeElementDTO element : file.getElements()) {
                if (element.getType() == null || (!element.getType().equals("CLASS") && !element.getType().equals("INTERFACE"))) {
                    continue;
                }

                ClassDesign classDesign = ClassDesign.builder()
                        .name(element.getName())
                        .type(element.getType())
                        .packageName(extractPackageName(file.getFilePath()))
                        .filePath(file.getFilePath())
                        .startLine(element.getStartLine())
                        .endLine(element.getEndLine())
                        .visibility(element.getVisibility() != null ? element.getVisibility() : "public")
                        .documentation(element.getDocumentation())
                        .annotations(element.getAnnotations() != null ? element.getAnnotations() : new ArrayList<>())
                        .purpose(inferClassPurpose(element.getName(), file.getFilePath()))
                        .fields(new ArrayList<>())
                        .methods(new ArrayList<>())
                        .build();

                classes.add(classDesign);
            }
        }

        return classes.stream().limit(100).collect(Collectors.toList());
    }

    private String inferClassPurpose(String className, String filePath) {
        if (className.endsWith("Controller")) return "REST API endpoint controller";
        if (className.endsWith("Service")) return "Business logic service";
        if (className.endsWith("Repository")) return "Data access repository";
        if (className.endsWith("Entity")) return "Database entity";
        if (className.endsWith("DTO")) return "Data transfer object";
        if (className.endsWith("Config")) return "Configuration";
        if (className.endsWith("Exception")) return "Exception handling";
        if (className.endsWith("Util")) return "Utility class";

        String lower = safePath(filePath).toLowerCase();
        if (lower.contains("service")) return "Service class";
        if (lower.contains("controller")) return "Controller class";
        if (lower.contains("repository")) return "Repository class";
        return "Domain class";
    }

    private String safePath(String filePath) {
        return filePath == null ? "" : filePath;
    }

    private List<ClassRelationship> generateClassRelationships(ParsedRepositoryDTO parsed) {
        List<ClassRelationship> relationships = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (ParsedFileDTO file : parsed.getFiles()) {
            if (file.getRelationships() == null) continue;

            for (CodeRelationshipDTO rel : file.getRelationships()) {
                if (rel.getSourceElement() == null || rel.getTargetElement() == null) continue;

                String key = rel.getSourceElement() + "-" + rel.getTargetElement() + "-" + rel.getRelationshipType();
                if (seen.contains(key)) continue;

                ClassRelationship classRel = ClassRelationship.builder()
                        .sourceClass(rel.getSourceElement())
                        .targetClass(rel.getTargetElement())
                        .type(rel.getRelationshipType())
                        .sourceFile(rel.getSourceFile())
                        .sourceLine(rel.getSourceLine())
                        .evidence("Source: " + rel.getSourceFile() + " (line " + rel.getSourceLine() + ")")
                        .confidence("CONFIRMED")
                        .build();

                relationships.add(classRel);
                seen.add(key);
            }
        }

        return relationships.stream().limit(100).collect(Collectors.toList());
    }

    private List<ServiceDesign> generateServices(ParsedRepositoryDTO parsed) {
        List<ServiceDesign> services = new ArrayList<>();

        for (ParsedFileDTO file : parsed.getFiles()) {
            if (file.getElements() == null) continue;

            for (CodeElementDTO element : file.getElements()) {
                if (element.getName() != null && element.getName().endsWith("Service") && "CLASS".equals(element.getType())) {
                    ServiceDesign service = ServiceDesign.builder()
                            .name(element.getName())
                            .className(element.getName())
                            .packageName(extractPackageName(file.getFilePath()))
                            .filePath(file.getFilePath())
                            .purpose(element.getDocumentation() != null ? element.getDocumentation() : "Service class")
                            .responsibilities(inferServiceResponsibilities(element.getName()))
                            .methods(new ArrayList<>())
                            .dependencies(new ArrayList<>())
                            .evidence("Detected in " + file.getFilePath())
                            .build();

                    services.add(service);
                }
            }
        }

        return services.stream().limit(50).collect(Collectors.toList());
    }

    private List<String> inferServiceResponsibilities(String serviceName) {
        List<String> responsibilities = new ArrayList<>();
        String lower = serviceName.toLowerCase();

        if (lower.contains("auth")) {
            responsibilities.addAll(List.of("User authentication", "Authorization checks", "Token management"));
        } else if (lower.contains("user")) {
            responsibilities.addAll(List.of("User management", "Profile operations", "User queries"));
        } else if (lower.contains("project")) {
            responsibilities.addAll(List.of("Project operations", "Project queries", "Project validation"));
        } else if (lower.contains("repository") || lower.contains("parser")) {
            responsibilities.addAll(List.of("Repository operations", "File parsing", "Dependency analysis"));
        } else if (lower.contains("embedding") || lower.contains("ai") || lower.contains("vector")) {
            responsibilities.addAll(List.of("Embedding generation", "Vector operations", "AI integration"));
        } else {
            responsibilities.add("Core business logic");
            responsibilities.add("Data processing");
        }

        return responsibilities;
    }

    private List<RepositoryDesign> generateRepositories(ParsedRepositoryDTO parsed) {
        List<RepositoryDesign> repositories = new ArrayList<>();

        for (ParsedFileDTO file : parsed.getFiles()) {
            if (file.getElements() == null) continue;

            for (CodeElementDTO element : file.getElements()) {
                if (element.getName() != null && element.getName().endsWith("Repository") && "INTERFACE".equals(element.getType())) {
                    String entityType = extractEntityType(element.getName());
                    RepositoryDesign repo = RepositoryDesign.builder()
                            .name(element.getName())
                            .className(element.getName())
                            .packageName(extractPackageName(file.getFilePath()))
                            .filePath(file.getFilePath())
                            .entityType(entityType)
                            .methods(inferRepositoryMethods(element.getName()))
                            .queries(new ArrayList<>())
                            .evidence("Detected in " + file.getFilePath())
                            .build();

                    repositories.add(repo);
                }
            }
        }

        return repositories.stream().limit(50).collect(Collectors.toList());
    }

    private String extractEntityType(String repositoryName) {
        // UserRepository -> User
        return repositoryName.replace("Repository", "").replace("Impl", "");
    }

    private List<String> inferRepositoryMethods(String repositoryName) {
        return List.of("findById()", "findAll()", "save()", "update()", "delete()", "findByQuery()");
    }

    private List<ControllerDesign> generateControllers(ParsedRepositoryDTO parsed) {
        List<ControllerDesign> controllers = new ArrayList<>();

        for (ParsedFileDTO file : parsed.getFiles()) {
            if (file.getElements() == null) continue;

            for (CodeElementDTO element : file.getElements()) {
                if (element.getName() != null && element.getName().endsWith("Controller") && "CLASS".equals(element.getType())) {
                    List<ApiEndpoint> endpoints = inferApiEndpoints(element.getName(), file.getFilePath());

                    ControllerDesign controller = ControllerDesign.builder()
                            .name(element.getName())
                            .className(element.getName())
                            .packageName(extractPackageName(file.getFilePath()))
                            .filePath(file.getFilePath())
                            .endpoints(endpoints)
                            .build();

                    controllers.add(controller);
                }
            }
        }

        return controllers.stream().limit(30).collect(Collectors.toList());
    }

    private List<ApiEndpoint> inferApiEndpoints(String controllerName, String filePath) {
        List<ApiEndpoint> endpoints = new ArrayList<>();
        String resource = controllerName.replace("Controller", "").toLowerCase();

        endpoints.add(ApiEndpoint.builder()
                .method("GET")
                .path("/api/" + resource)
                .methodName("getAll")
                .controllerName(controllerName)
                .purpose("Retrieve all " + resource)
                .sourceFile(filePath)
                .build());

        endpoints.add(ApiEndpoint.builder()
                .method("GET")
                .path("/api/" + resource + "/{id}")
                .methodName("getById")
                .controllerName(controllerName)
                .purpose("Retrieve " + resource + " by ID")
                .sourceFile(filePath)
                .build());

        endpoints.add(ApiEndpoint.builder()
                .method("POST")
                .path("/api/" + resource)
                .methodName("create")
                .controllerName(controllerName)
                .purpose("Create new " + resource)
                .sourceFile(filePath)
                .build());

        return endpoints;
    }

    private List<EntityDesign> generateEntities(ParsedRepositoryDTO parsed) {
        List<EntityDesign> entities = new ArrayList<>();

        for (ParsedFileDTO file : parsed.getFiles()) {
            if (file.getElements() == null) continue;

            for (CodeElementDTO element : file.getElements()) {
                if (element.getName() != null && element.getName().endsWith("Entity") && "CLASS".equals(element.getType())) {
                    EntityDesign entity = EntityDesign.builder()
                            .name(element.getName())
                            .packageName(extractPackageName(file.getFilePath()))
                            .filePath(file.getFilePath())
                            .tableName(camelToSnakeCase(element.getName().replace("Entity", "")))
                            .fields(inferEntityFields(element.getName()))
                            .relationships(new ArrayList<>())
                            .build();

                    entities.add(entity);
                }
            }
        }

        return entities.stream().limit(30).collect(Collectors.toList());
    }

    private String camelToSnakeCase(String str) {
        return str.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    private List<EntityField> inferEntityFields(String entityName) {
        List<EntityField> fields = new ArrayList<>();

        fields.add(EntityField.builder()
                .name("id")
                .type("UUID")
                .isPrimaryKey(true)
                .isForeignKey(false)
                .columnName("id")
                .build());

        fields.add(EntityField.builder()
                .name("createdAt")
                .type("Instant")
                .columnName("created_at")
                .build());

        fields.add(EntityField.builder()
                .name("updatedAt")
                .type("Instant")
                .columnName("updated_at")
                .build());

        return fields;
    }

    private List<SequenceDiagram> generateSequenceDiagrams(ParsedRepositoryDTO parsed) {
        List<SequenceDiagram> diagrams = new ArrayList<>();

        // Generate common workflow sequences
        if (containsAuthElements(parsed)) {
            diagrams.add(generateAuthenticationSequence());
        }

        if (containsControllers(parsed) && containsServices(parsed)) {
            diagrams.add(generateRequestSequence());
        }

        return diagrams;
    }

    private boolean containsAuthElements(ParsedRepositoryDTO parsed) {
        return parsed.getFiles().stream()
                .anyMatch(f -> safePath(f.getFilePath()).toLowerCase().contains("auth") ||
                        (f.getElements() != null && f.getElements().stream().anyMatch(e -> e.getName() != null && e.getName().contains("Auth"))));
    }

    private boolean containsControllers(ParsedRepositoryDTO parsed) {
        return parsed.getFiles().stream()
                .flatMap(f -> f.getElements().stream())
                .anyMatch(e -> e.getName() != null && e.getName().endsWith("Controller"));
    }

    private boolean containsServices(ParsedRepositoryDTO parsed) {
        return parsed.getFiles().stream()
                .flatMap(f -> f.getElements().stream())
                .anyMatch(e -> e.getName() != null && e.getName().endsWith("Service"));
    }

    private SequenceDiagram generateAuthenticationSequence() {
        String mermaid = "sequenceDiagram\n" +
                "  participant User\n" +
                "  participant Frontend\n" +
                "  participant AuthController\n" +
                "  participant AuthService\n" +
                "  participant UserRepository\n" +
                "  participant Database\n\n" +
                "  User->>Frontend: Login request\n" +
                "  Frontend->>AuthController: POST /auth/login\n" +
                "  AuthController->>AuthService: authenticate(credentials)\n" +
                "  AuthService->>UserRepository: findByEmail()\n" +
                "  UserRepository->>Database: SELECT * FROM users\n" +
                "  Database-->>UserRepository: User record\n" +
                "  UserRepository-->>AuthService: User object\n" +
                "  AuthService->>AuthService: validatePassword()\n" +
                "  AuthService->>AuthService: generateJWT()\n" +
                "  AuthService-->>AuthController: JWT token\n" +
                "  AuthController-->>Frontend: {token, user}\n" +
                "  Frontend-->>User: Login successful";

        return SequenceDiagram.builder()
                .name("User Authentication Flow")
                .description("Sequence diagram showing user login process")
                .mermaidDiagram(mermaid)
                .actors(List.of("User", "Frontend", "Backend"))
                .systems(List.of("Authentication", "User Management", "Database"))
                .build();
    }

    private SequenceDiagram generateRequestSequence() {
        String mermaid = "sequenceDiagram\n" +
                "  participant Client\n" +
                "  participant Controller\n" +
                "  participant Service\n" +
                "  participant Repository\n" +
                "  participant Database\n\n" +
                "  Client->>Controller: HTTP Request\n" +
                "  Controller->>Service: Method call\n" +
                "  Service->>Service: Business logic\n" +
                "  Service->>Repository: Data operation\n" +
                "  Repository->>Database: SQL Query\n" +
                "  Database-->>Repository: Result\n" +
                "  Repository-->>Service: Data object\n" +
                "  Service->>Service: Process result\n" +
                "  Service-->>Controller: Response\n" +
                "  Controller->>Controller: Map to DTO\n" +
                "  Controller-->>Client: HTTP Response";

        return SequenceDiagram.builder()
                .name("Request Processing Flow")
                .description("Standard request lifecycle from client to database")
                .mermaidDiagram(mermaid)
                .actors(List.of("Client", "Backend"))
                .systems(List.of("API", "Business Logic", "Database"))
                .build();
    }

    private String generateClassDiagram(ParsedRepositoryDTO parsed) {
        StringBuilder sb = new StringBuilder("classDiagram\n");

        List<ClassDesign> classes = generateClasses(parsed);
        List<ClassRelationship> rels = generateClassRelationships(parsed);

        // Add classes
        for (ClassDesign cls : classes.stream().limit(20).collect(Collectors.toList())) {
            sb.append("  class ").append(cls.getName()).append(" {\n");
            if (cls.getFields() != null) {
                for (FieldDesign field : cls.getFields().stream().limit(5).collect(Collectors.toList())) {
                    sb.append("    ").append(field.getType()).append(" ").append(field.getName()).append("\n");
                }
            }
            if (cls.getMethods() != null) {
                for (MethodDesign method : cls.getMethods().stream().limit(3).collect(Collectors.toList())) {
                    sb.append("    ").append(method.getReturnType()).append(" ").append(method.getName()).append("()\n");
                }
            }
            sb.append("  }\n");
        }

        // Add relationships
        Set<String> seen = new HashSet<>();
        for (ClassRelationship rel : rels.stream().limit(20).collect(Collectors.toList())) {
            String key = rel.getSourceClass() + rel.getType() + rel.getTargetClass();
            if (seen.add(key)) {
                String arrow = switch (rel.getType()) {
                    case "EXTENDS" -> "<|--";
                    case "IMPLEMENTS" -> "<|..";
                    case "USES", "DEPENDS_ON" -> "-->";
                    default -> "-->";
                };
                sb.append("  ").append(rel.getSourceClass()).append(" ").append(arrow).append(" ").append(rel.getTargetClass()).append("\n");
            }
        }

        return sb.toString();
    }
}
