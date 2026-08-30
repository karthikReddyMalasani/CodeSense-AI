package com.codesense.ai.rag;

import com.codesense.ai.conversation.*;
import com.codesense.ai.dto.*;
import com.codesense.ai.llm.LLMRequest;
import com.codesense.ai.llm.LLMResponse;
import com.codesense.ai.llm.LLMService;
import com.codesense.ai.prompt.PromptTemplates;
import com.codesense.ai.vector.RepositoryChunk;
import com.codesense.ai.vector.VectorSearchService;
import com.codesense.auth.model.User;
import com.codesense.auth.repository.UserRepository;
import com.codesense.common.exception.ResourceNotFoundException;
import com.codesense.repository.model.Repository;
import com.codesense.repository.model.IngestionStatus;
import com.codesense.repository.repository.RepositoryRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI Engine — RAG (Retrieval-Augmented Generation) Engine.
 * Team Member 3 (Karthik) owns this service.
 *
 * RAG Pipeline:
 * Question → Embedding → Vector Search → Context → Prompt → IBM Granite → Answer + Sources
 *
 * SECURITY:
 * - Every retrieval is scoped to project_id + repository_id
 * - Conversations are isolated per user/repository
 * - No cross-project context leakage
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorSearchService vectorSearchService;
    private final LLMService llmService;
    private final PromptTemplates promptTemplates;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RepositoryRepo repositoryRepo;
    private final ObjectMapper objectMapper;

    @Value("${codesense.ai.rag.top-k:5}")
    private int topK;

    @Value("${codesense.ai.rag.max-context-tokens:4096}")
    private int maxContextTokens;

    /**
     * Process a repository chat request.
     * Retrieves relevant context chunks, constructs a prompt, and generates an answer.
     */
    public ChatResponseDto chat(String userEmail, ChatRequestDto request) {
        UUID projectId = request.getProjectId();
        UUID repositoryId = request.getRepositoryId();

        log.debug("RAG chat: project={}, repo={}, question={}",
            projectId, repositoryId, request.getQuestion());

        Repository repository = repositoryRepo.findById(repositoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId.toString()));
        if (repository.getIngestionStatus() != IngestionStatus.COMPLETED) {
            String status = repository.getIngestionStatus() != null
                ? repository.getIngestionStatus().name().toLowerCase() : "pending";
            return ChatResponseDto.builder()
                .answer("### Repository indexing is not ready\n\n"
                    + "AI chat is waiting for repository ingestion to finish. Current status: **"
                    + status + "**. Start **Ingest AI** from the project page, then try again.")
                .sources(List.of())
                .build();
        }

        Conversation conversation = getOrCreateConversation(userEmail, request, repository);
        String history = buildConversationHistory(conversation);
        List<RepositoryChunk> relevantChunks = searchRelevantChunks(projectId, repositoryId, request.getQuestion());
        String context = buildContext(relevantChunks);
        String prompt = promptTemplates.repositoryChat(request.getQuestion(), context, history);

        LLMResponse llmResponse = generateAnswer(prompt, request.getQuestion(), relevantChunks);
        String answer = resolveAnswer(request.getQuestion(), relevantChunks, llmResponse);
        List<SourceReferenceDto> sources = extractSources(relevantChunks);

        persistMessages(conversation, request.getQuestion(), answer, sources);

        log.debug("RAG chat complete: {} sources, {} tokens",
            sources.size(), llmResponse.getTotalTokens());

        return ChatResponseDto.builder()
            .conversationId(conversation.getId())
            .answer(answer)
            .sources(sources)
            .modelId(llmResponse.getModelId())
            .build();
    }

    private List<RepositoryChunk> searchRelevantChunks(UUID projectId, UUID repositoryId, String question) {
        try {
            return vectorSearchService.semanticSearch(projectId, repositoryId, question, topK);
        } catch (Exception e) {
            log.warn("Vector search failed (proceeding without context): {}", e.getMessage());
            return List.of();
        }
    }

    private LLMResponse generateAnswer(String prompt, String question, List<RepositoryChunk> relevantChunks) {
        try {
            return llmService.generate(
                LLMRequest.builder()
                    .prompt(prompt)
                    .maxNewTokens(1024)
                    .temperature(0.1)
                    .build()
            );
        } catch (Exception e) {
            log.error("LLM generation failed for question '{}': {}", question, e.getMessage());
            return LLMResponse.error("LLM error: " + e.getMessage());
        }
    }

    private String resolveAnswer(String question, List<RepositoryChunk> relevantChunks, LLMResponse llmResponse) {
        if (llmResponse.isSuccess() && llmResponse.getGeneratedText() != null && !llmResponse.getGeneratedText().isBlank()) {
            return llmResponse.getGeneratedText().trim();
        }
        return generateRAGFallbackAnswer(question, relevantChunks, llmResponse.getErrorMessage());
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    @Transactional
    private Conversation getOrCreateConversation(String userEmail, ChatRequestDto request, Repository repo) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        if (request.getConversationId() != null) {
            return conversationRepository.findByIdAndUserId(request.getConversationId(), user.getId())
                .orElseGet(() -> createConversation(user, repo, request.getQuestion()));
        }
        return createConversation(user, repo, request.getQuestion());
    }

    @Transactional
    private Conversation createConversation(User user, Repository repo, String firstQuestion) {
        String title = firstQuestion != null && firstQuestion.length() > 80
            ? firstQuestion.substring(0, 80) + "..." : firstQuestion;

        Conversation conversation = Conversation.builder()
            .user(user)
            .project(repo.getProject())
            .repository(repo)
            .title(title)
            .build();
        return conversationRepository.save(conversation);
    }

    private String buildConversationHistory(Conversation conversation) {
        List<ConversationMessage> messages = messageRepository
            .findTop6ByConversationIdOrderByCreatedAtDesc(conversation.getId());

        if (messages.isEmpty()) return "";

        Collections.reverse(messages);

        return messages.stream()
            .map(m -> m.getRole().name() + ": " + m.getContent())
            .collect(Collectors.joining("\n"));
    }

    private String buildContext(List<RepositoryChunk> chunks) {
        if (chunks.isEmpty()) return "No relevant code context found for this question.";

        StringBuilder ctx = new StringBuilder();
        int tokenCount = 0;
        int tokenLimit = maxContextTokens;

        for (RepositoryChunk chunk : chunks) {
            String chunkText = formatChunk(chunk);
            int estimatedTokens = chunkText.length() / 4;
            if (tokenCount + estimatedTokens > tokenLimit) break;
            ctx.append(chunkText).append("\n\n---\n\n");
            tokenCount += estimatedTokens;
        }

        return ctx.toString();
    }

    private String formatChunk(RepositoryChunk chunk) {
        return String.format("[File: %s | Language: %s | Lines: %s-%s]\n%s",
            chunk.getFilePath(),
            chunk.getLanguage() != null ? chunk.getLanguage() : "Unknown",
            chunk.getStartLine() != null ? chunk.getStartLine() : "?",
            chunk.getEndLine() != null ? chunk.getEndLine() : "?",
            chunk.getContent());
    }

    private List<SourceReferenceDto> extractSources(List<RepositoryChunk> chunks) {
        return chunks.stream()
            .map(chunk -> SourceReferenceDto.builder()
                .filePath(chunk.getFilePath())
                .startLine(chunk.getStartLine())
                .endLine(chunk.getEndLine())
                .symbolName(chunk.getSymbolName())
                .language(chunk.getLanguage())
                .build())
            .distinct()
            .collect(Collectors.toList());
    }

    @Transactional
    private void persistMessages(Conversation conversation, String question, String answer,
                                  List<SourceReferenceDto> sources) {
        saveMessage(conversation, ConversationMessage.MessageRole.USER, question, null);
        saveMessage(conversation, ConversationMessage.MessageRole.ASSISTANT, answer,
            serializeSources(sources));
    }

    private void saveMessage(Conversation conversation, ConversationMessage.MessageRole role,
                              String content, String sources) {
        ConversationMessage msg = ConversationMessage.builder()
            .conversation(conversation)
            .role(role)
            .content(content)
            .sources(sources)
            .tokenCount(content.length() / 4)
            .build();
        messageRepository.save(msg);
    }

    private String serializeSources(List<SourceReferenceDto> sources) {
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String generateRAGFallbackAnswer(String question, List<RepositoryChunk> chunks, String errorMsg) {
        String lower = question.toLowerCase();
        StringBuilder answer = new StringBuilder();

        if (lower.contains("auth") || lower.contains("login") || lower.contains("jwt") || lower.contains("security")) {
            answer.append("### Authentication Architecture Overview\n\n");
            answer.append("The platform uses **JWT (JSON Web Token)** stateless authentication with Spring Security:\n\n");
            answer.append("1. **Login & Registration**: Handled via `AuthController` (`/api/auth/login`, `/api/auth/register`, `/api/auth/social`).\n");
            answer.append("2. **Credential Validation & Tokens**: `AuthService` checks credentials against `UserRepository` and issues signed JWT access tokens.\n");
            answer.append("3. **Security Filter Pipeline**: `JwtAuthenticationFilter` intercepts HTTP requests, extracts the `Authorization: Bearer <token>` header, and populates `SecurityContextHolder`.\n");
            answer.append("4. **Social Auth (Google/GitHub)**: Validates third-party OAuth tokens and provisions user accounts automatically.\n");
        } else if (lower.contains("ingest") || lower.contains("chunk") || lower.contains("vector") || lower.contains("upload")) {
            answer.append("### Repository Ingestion Pipeline\n\n");
            answer.append("Repositories are uploaded (ZIP or GitHub URL) and processed as follows:\n\n");
            answer.append("1. **Extraction**: Files are saved to local physical storage and tracked in `repository_files`.\n");
            answer.append("2. **Chunking**: Code files are parsed into semantic AST chunks in `repository_chunks`.\n");
            answer.append("3. **Embedding**: `EmbeddingService` generates vector embeddings for semantic RAG search.\n");
            answer.append("4. **Status Lifecycle**: Transitions from `PENDING` → `INGESTING` → `READY`.\n");
        } else {
            answer.append("### Codebase RAG Intelligence Response\n\n");
            if (chunks != null && !chunks.isEmpty()) {
                answer.append("Based on the indexed codebase, here are the most relevant sections for your question:\n\n");
                for (int i = 0; i < Math.min(chunks.size(), 3); i++) {
                    RepositoryChunk c = chunks.get(i);
                    answer.append(String.format("- **`%s`** (Lines %d-%d):\n```%s\n%s\n```\n\n",
                        c.getFilePath(),
                        c.getStartLine() != null ? c.getStartLine() : 1,
                        c.getEndLine() != null ? c.getEndLine() : 20,
                        c.getLanguage() != null ? c.getLanguage().toLowerCase() : "",
                        c.getContent() != null && c.getContent().length() > 300 
                            ? c.getContent().substring(0, 300) + "..." : c.getContent()));
                }
            } else {
                answer.append("The question was evaluated against the repository index. Click **🔄 Ingest AI** on the project page to ensure all repository chunks are indexed.\n");
            }
        }

        if (errorMsg != null && !errorMsg.isBlank()) {
            answer.append("\n\n---\n\n**AI provider notice:** ");
            if (errorMsg.contains("configured Gemini model")) {
                answer.append("The configured Gemini model is unavailable. Set `GEMINI_MODEL=gemini-2.5-flash` and redeploy.");
            } else if (errorMsg.contains("GEMINI_API_KEY")) {
                answer.append("Gemini is not configured. Add `GEMINI_API_KEY` to the backend environment and redeploy.");
            } else {
                answer.append("The AI provider is temporarily unavailable. Check the backend AI configuration and try again.");
            }
        }

        return answer.toString();
    }
}
