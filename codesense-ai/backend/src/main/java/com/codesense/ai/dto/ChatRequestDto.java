package com.codesense.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatRequestDto {
    @NotNull private UUID projectId;
    @NotNull private UUID repositoryId;
    private UUID conversationId;
    
    @NotBlank(message = "Question cannot be blank")
    @Size(min = 3, max = 5000, message = "Question must be between 3 and 5000 characters")
    private String question;
}
