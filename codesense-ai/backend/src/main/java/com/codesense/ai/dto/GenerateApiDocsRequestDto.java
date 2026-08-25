package com.codesense.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class GenerateApiDocsRequestDto {
    @NotNull private UUID projectId;
    @NotNull private UUID repositoryId;
    private String language;
    private boolean regenerate = false;
}
