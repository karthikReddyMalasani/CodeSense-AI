package com.codesense.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class IngestRequestDto {
    @NotNull private UUID projectId;
    @NotNull private UUID repositoryId;
    private boolean force = false;
}
