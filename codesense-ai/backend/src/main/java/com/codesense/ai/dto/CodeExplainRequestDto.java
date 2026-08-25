package com.codesense.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CodeExplainRequestDto {
    @NotNull private UUID projectId;
    @NotNull private UUID repositoryId;
    private String filePath;
    private String language;
    @NotBlank private String code;
}
