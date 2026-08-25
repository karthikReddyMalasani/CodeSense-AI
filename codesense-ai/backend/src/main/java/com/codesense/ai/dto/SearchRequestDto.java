package com.codesense.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SearchRequestDto {
    @NotNull private UUID projectId;
    @NotNull private UUID repositoryId;
    @NotBlank private String query;
    private int topK = 5;
}
