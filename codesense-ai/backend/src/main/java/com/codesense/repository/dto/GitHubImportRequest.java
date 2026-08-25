package com.codesense.repository.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GitHubImportRequest {

    @NotBlank(message = "GitHub URL is required")
    @Pattern(regexp = "^https://github\\.com/[\\w.-]+/[\\w.-]+(\\.git)?$",
             message = "Must be a valid GitHub repository URL")
    private String githubUrl;

    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    private String branch;
}
