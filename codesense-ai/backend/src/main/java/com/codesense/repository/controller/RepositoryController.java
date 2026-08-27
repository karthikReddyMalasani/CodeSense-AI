package com.codesense.repository.controller;

import com.codesense.common.dto.ApiResponse;
import com.codesense.repository.dto.*;
import com.codesense.repository.service.RepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Repositories", description = "Repository management APIs")
@SecurityRequirement(name = "bearerAuth")
public class RepositoryController {

    private final RepositoryService repositoryService;

    @PostMapping(value = "/api/projects/{projectId}/repositories/upload",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a ZIP file as a repository")
    public ResponseEntity<ApiResponse<RepositoryDto>> uploadZip(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") @Valid UploadRepositoryRequest request) {
        RepositoryDto repo = repositoryService.uploadZip(userDetails.getUsername(), projectId, file, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Repository upload started", repo));
    }

    @PostMapping("/api/projects/{projectId}/repositories/github")
    @Operation(summary = "Import a repository from GitHub")
    public ResponseEntity<ApiResponse<RepositoryDto>> importFromGitHub(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId,
            @Valid @RequestBody GitHubImportRequest request) {
        RepositoryDto repo = repositoryService.importFromGitHub(userDetails.getUsername(), projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("GitHub import started", repo));
    }

    @GetMapping("/api/projects/{projectId}/repositories")
    @Operation(summary = "List repositories for a project")
    public ResponseEntity<ApiResponse<List<RepositoryDto>>> getRepositories(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID projectId) {
        List<RepositoryDto> repos = repositoryService.getRepositories(userDetails.getUsername(), projectId);
        return ResponseEntity.ok(ApiResponse.success(repos));
    }

    @GetMapping("/api/repositories/{repositoryId}")
    @Operation(summary = "Get a repository by ID")
    public ResponseEntity<ApiResponse<RepositoryDto>> getRepository(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID repositoryId) {
        RepositoryDto repo = repositoryService.getRepository(userDetails.getUsername(), repositoryId);
        return ResponseEntity.ok(ApiResponse.success(repo));
    }

    @GetMapping("/api/repositories/{repositoryId}/files")
    @Operation(summary = "List files in a repository")
    public ResponseEntity<ApiResponse<List<RepositoryFileDto>>> getFiles(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID repositoryId) {
        List<RepositoryFileDto> files = repositoryService.getFiles(userDetails.getUsername(), repositoryId);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @GetMapping("/api/repositories/{repositoryId}/files/{fileId}")
    @Operation(summary = "Get file content by ID")
    public ResponseEntity<ApiResponse<RepositoryFileDto>> getFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID repositoryId,
            @PathVariable UUID fileId) {
        RepositoryFileDto file = repositoryService.getFile(userDetails.getUsername(), repositoryId, fileId);
        return ResponseEntity.ok(ApiResponse.success(file));
    }

    @PutMapping("/api/repositories/{repositoryId}")
    @Operation(summary = "Update repository name and description")
    public ResponseEntity<ApiResponse<RepositoryDto>> updateRepository(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID repositoryId,
            @Valid @RequestBody UpdateRepositoryRequest request) {
        RepositoryDto repo = repositoryService.updateRepository(userDetails.getUsername(), repositoryId, request);
        return ResponseEntity.ok(ApiResponse.success("Repository updated successfully", repo));
    }

    @DeleteMapping("/api/repositories/{repositoryId}")
    @Operation(summary = "Delete a specific repository")
    public ResponseEntity<ApiResponse<Void>> deleteRepository(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID repositoryId) {
        repositoryService.deleteRepository(userDetails.getUsername(), repositoryId);
        return ResponseEntity.ok(ApiResponse.success("Repository deleted successfully", null));
    }
}
