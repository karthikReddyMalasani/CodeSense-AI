package com.codesense.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data flow analysis showing how data moves through the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataFlow {

    /** Data flow diagrams for important flows */
    private List<DataFlowDiagram> flows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DataFlowDiagram {
        /** Name of the flow (e.g., "User Authentication", "Project Upload") */
        private String name;

        /** Description of what happens in this flow */
        private String description;

        /** Sequence of steps */
        private List<FlowStep> steps;

        /** Mermaid sequence diagram */
        private String sequenceDiagram;

        /** Data entities involved */
        private List<String> dataEntities;

        /** External systems involved */
        private List<String> externalSystems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FlowStep {
        /** Step number */
        private Integer stepNumber;

        /** Source component/actor */
        private String from;

        /** Target component/actor */
        private String to;

        /** Action/operation */
        private String action;

        /** Data being passed */
        private String dataType;

        /** Supporting evidence */
        private String evidence;
    }
}
