package com.codesense.repository.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for updating repository name and description.
 * Allows users to edit imported or uploaded repository metadata.
 */
@Data
public class UpdateRepositoryRequest {

    @NotBlank(message = "Repository name is required")
    @Size(min = 1, max = 255, message = "Repository name must be between 1 and 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
}
