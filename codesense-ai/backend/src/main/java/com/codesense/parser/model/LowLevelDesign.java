package com.codesense.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Low-Level Design (LLD) of the system.
 * Provides detailed implementation-level view with classes, methods, and relationships.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LowLevelDesign {

    /** Package/module structure */
    private List<PackageStructure> packages;

    /** Major classes and interfaces */
    private List<ClassDesign> classes;

    /** Class relationships and dependencies */
    private List<ClassRelationship> classRelationships;

    /** Service layer details */
    private List<ServiceDesign> services;

    /** Repository/Data access layer details */
    private List<RepositoryDesign> repositories;

    /** Controller/API endpoint details */
    private List<ControllerDesign> controllers;

    /** Database entities */
    private List<EntityDesign> entities;

    /** Sequence diagrams for important workflows */
    private List<SequenceDiagram> sequenceDiagrams;

    /** Mermaid class diagram */
    private String classDiagram;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PackageStructure {
        private String packageName;
        private String path;
        private String purpose;
        private List<String> classes;
        private List<String> subPackages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClassDesign {
        private String name;
        private String type; // CLASS, INTERFACE, ENUM, ABSTRACT, etc.
        private String packageName;
        private String filePath;
        private Integer startLine;
        private Integer endLine;
        private String visibility; // public, private, protected, package
        private List<String> interfaces;
        private String superClass;
        private List<FieldDesign> fields;
        private List<MethodDesign> methods;
        private String documentation;
        private List<String> annotations;
        private String purpose; // Inferred purpose
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldDesign {
        private String name;
        private String type;
        private String visibility;
        private Boolean isStatic;
        private Boolean isFinal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MethodDesign {
        private String name;
        private String returnType;
        private List<String> parameters;
        private String visibility;
        private Boolean isStatic;
        private Boolean isAbstract;
        private Integer startLine;
        private Integer endLine;
        private String documentation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClassRelationship {
        private String sourceClass;
        private String targetClass;
        private String type; // EXTENDS, IMPLEMENTS, USES, DEPENDS_ON, CALLS
        private String sourceFile;
        private Integer sourceLine;
        private String evidence;
        private String confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ServiceDesign {
        private String name;
        private String className;
        private String packageName;
        private String filePath;
        private String purpose;
        private List<String> responsibilities;
        private List<String> methods;
        private List<String> dependencies;
        private String evidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RepositoryDesign {
        private String name;
        private String className;
        private String packageName;
        private String filePath;
        private String entityType; // The entity it manages
        private List<String> methods;
        private List<String> queries;
        private String evidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ControllerDesign {
        private String name;
        private String className;
        private String packageName;
        private String filePath;
        private List<ApiEndpoint> endpoints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiEndpoint {
        private String method; // GET, POST, PUT, DELETE, etc.
        private String path;
        private String methodName;
        private String controllerName;
        private String purpose;
        private List<String> parameters;
        private String responseType;
        private Integer startLine;
        private String sourceFile;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EntityDesign {
        private String name;
        private String packageName;
        private String filePath;
        private String tableName;
        private List<EntityField> fields;
        private List<EntityRelationship> relationships;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EntityField {
        private String name;
        private String type;
        private Boolean isPrimaryKey;
        private Boolean isForeignKey;
        private Boolean isNullable;
        private String columnName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EntityRelationship {
        private String targetEntity;
        private String type; // ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY
        private String foreignKeyColumn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SequenceDiagram {
        private String name; // e.g., "User Login Flow"
        private String mermaidDiagram;
        private String description;
        private List<String> actors;
        private List<String> systems;
    }
}
