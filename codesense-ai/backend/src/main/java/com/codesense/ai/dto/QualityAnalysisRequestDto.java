package com.codesense.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class QualityAnalysisRequestDto {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotNull(message = "Repository ID is required")
    private UUID repositoryId;
}
