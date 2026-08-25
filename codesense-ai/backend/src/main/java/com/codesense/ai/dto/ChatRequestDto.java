package com.codesense.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatRequestDto {
    @NotNull private UUID projectId;
    @NotNull private UUID repositoryId;
    private UUID conversationId;
    @NotBlank private String question;
}
