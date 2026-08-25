package com.codesense.ai.conversation;

import com.codesense.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for conversation history.
 * Team Member 3 (Karthik) owns this controller.
 *
 * Exposes:
 * - GET  /api/repositories/{repositoryId}/conversations        — list conversations
 * - GET  /api/conversations/{conversationId}/messages          — get messages for a conversation
 * - DELETE /api/conversations/{conversationId}                 — delete a conversation
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Conversation history APIs")
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/api/repositories/{repositoryId}/conversations")
    @Operation(summary = "List conversations for a repository")
    public ResponseEntity<ApiResponse<List<ConversationSummaryDto>>> listConversations(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID repositoryId) {
        List<ConversationSummaryDto> conversations =
            conversationService.getConversations(userDetails.getUsername(), repositoryId);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/api/conversations/{conversationId}/messages")
    @Operation(summary = "Get messages for a conversation")
    public ResponseEntity<ApiResponse<ConversationDetailDto>> getConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID conversationId) {
        ConversationDetailDto detail =
            conversationService.getConversationDetail(userDetails.getUsername(), conversationId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @DeleteMapping("/api/conversations/{conversationId}")
    @Operation(summary = "Delete a conversation")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID conversationId) {
        conversationService.deleteConversation(userDetails.getUsername(), conversationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
