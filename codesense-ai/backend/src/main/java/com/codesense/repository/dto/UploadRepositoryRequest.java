package com.codesense.repository.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UploadRepositoryRequest {

    @NotBlank(message = "Repository name is required")
    @Size(min = 1, max = 255)
    private String name;

    @Size(max = 2000)
    private String description;
}
