package com.codesense.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * High-Level Design (HLD) of the system.
 * Describes major components, architectural patterns, and system-level interactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HighLevelDesign {

    /** System overview and purpose */
    private SystemOverview systemOverview;

    /** Major architectural components */
    private List<ArchitectureComponent> components;

    /** Communication patterns between components */
    private List<ComponentCommunication> communications;

    /** Mermaid diagram showing the architecture */
    private String architectureDiagram;

    /** Overall architectural style (e.g., "Layered", "Microservices", "Monolith") */
    private String architecturalStyle;

    /** External systems and integrations */
    private List<ExternalSystem> externalSystems;

    /** Deployment architecture description */
    private DeploymentArchitecture deploymentArchitecture;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SystemOverview {
        private String applicationName;
        private String purpose;
        private List<String> majorCapabilities;
        private List<String> mainTechnologies;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ArchitectureComponent {
        private String name;
        private String type; // FRONTEND, BACKEND, DATABASE, CACHE, AI_PROVIDER, etc.
        private String description;
        private List<String> responsibilities;
        private String technology;
        private String confidence; // CONFIRMED, INFERRED, UNKNOWN
        private String evidence; // Source evidence
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ComponentCommunication {
        private String from;
        private String to;
        private String protocol; // REST, gRPC, Event, Direct, etc.
        private String description;
        private String evidence;
        private String confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExternalSystem {
        private String name;
        private String type; // AUTH_PROVIDER, AI_PROVIDER, STORAGE, etc.
        private String purpose;
        private String technology;
        private String evidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeploymentArchitecture {
        private String description;
        private List<DeploymentComponent> components;
        private String containerization; // Docker, Kubernetes, etc.
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeploymentComponent {
        private String name;
        private String hostingType; // Cloud, On-premise, Container, etc.
        private String technology;
        private String evidence;
    }
}
