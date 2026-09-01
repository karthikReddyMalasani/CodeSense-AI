package com.codesense.ai.controller;

import com.codesense.ai.dto.*;
import com.codesense.ai.service.AiService;
import com.codesense.ai.service.QualityAnalysisService;
import com.codesense.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI Engine — REST API Controller.
 * Team Member 3 (Karthik) owns this controller.
 *
 * All AI endpoints are secured with JWT authentication.
 * Project/repository ownership is validated on every request.
 * Comprehensive error handling and validation for all inputs.
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Engine", description = "AI-powered code intelligence APIs (Team Member 3 — Karthik)")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiService aiService;
    private final QualityAnalysisService qualityAnalysisService;

    @GetMapping("/health")
    @Operation(summary = "Check AI Engine health and provider status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(aiService.getHealth()));
    }

    @PostMapping("/ingest")
    @Operation(summary = "Trigger repository ingestion into vector store")
    public ResponseEntity<ApiResponse<String>> ingest(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody IngestRequestDto request) {
        aiService.triggerIngestion(userDetails.getUsername(), request.getRepositoryId());
        return ResponseEntity.ok(ApiResponse.success(
                "Ingestion started for repository: " + request.getRepositoryId()));
    }

    @PostMapping("/chat")
    @Operation(summary = "Repository-aware AI chatbot (RAG-powered)")
    public ResponseEntity<ApiResponse<ChatResponseDto>> chat(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChatRequestDto request) {
        ChatResponseDto response = aiService.chat(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/search")
    @Operation(summary = "Semantic search over repository source code")
    public ResponseEntity<ApiResponse<SearchResponseDto>> search(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SearchRequestDto request) {
        SearchResponseDto response = aiService.search(
                request.getProjectId(), request.getRepositoryId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/explain-code")
    @Operation(summary = "AI-powered code explanation")
    public ResponseEntity<ApiResponse<CodeExplainResponseDto>> explainCode(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CodeExplainRequestDto request) {
        CodeExplainResponseDto response = aiService.explainCode(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/generate-readme")
    @Operation(summary = "Generate README documentation from repository analysis")
    public ResponseEntity<ApiResponse<GenerateReadmeResponseDto>> generateReadme(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GenerateReadmeRequestDto request) {
        GenerateReadmeResponseDto response = aiService.generateReadme(
                userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/generate-api-docs")
    @Operation(summary = "Generate API documentation from repository analysis")
    public ResponseEntity<ApiResponse<GenerateApiDocsResponseDto>> generateApiDocs(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GenerateApiDocsRequestDto request) {
        GenerateApiDocsResponseDto response = aiService.generateApiDocs(
                userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/analyze-quality")
    @Operation(summary = "Run full code quality analysis with AI-powered issue detection and scoring")
    public ResponseEntity<ApiResponse<QualityAnalysisResponseDto>> analyzeQuality(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody QualityAnalysisRequestDto request) {
        QualityAnalysisResponseDto response = qualityAnalysisService.analyzeQuality(
                request.getProjectId(), request.getRepositoryId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── Global Exception Handlers ────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
        
        log.warn("Validation error: {}", errors);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception ex) {
        log.error("Unexpected error in AI controller: {}", ex.getMessage(), ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }
}
