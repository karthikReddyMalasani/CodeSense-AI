package com.codesense.repository.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepositoryDto {
    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private String sourceType;
    private String githubUrl;
    private String status;
    private String analysisStatus;
    private String ingestionStatus;
    private int totalFiles;
    private int totalChunks;
    private List<String> languages;
    private String primaryLanguage;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
}
