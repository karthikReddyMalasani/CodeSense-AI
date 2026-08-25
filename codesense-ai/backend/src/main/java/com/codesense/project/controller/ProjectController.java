package com.codesense.project.controller;

import com.codesense.common.dto.ApiResponse;
import com.codesense.project.dto.CreateProjectRequest;
import com.codesense.project.dto.ProjectDto;
import com.codesense.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ApiResponse<ProjectDto>> createProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectDto project = projectService.createProject(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Project created", project));
    }

    @GetMapping
    @Operation(summary = "List all projects for current user")
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getProjects(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProjectDto> projects = projectService.getProjects(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get a project by ID")
    public ResponseEntity<ApiResponse<ProjectDto>> getProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId) {
        ProjectDto project = projectService.getProject(userDetails.getUsername(), projectId);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete a project")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId) {
        projectService.deleteProject(userDetails.getUsername(), projectId);
        return ResponseEntity.ok(ApiResponse.success("Project deleted", null));
    }
}
