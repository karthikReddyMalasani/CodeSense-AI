package com.codesense.ai;

import com.codesense.ai.conversation.*;
import com.codesense.ai.dto.*;
import com.codesense.ai.llm.*;
import com.codesense.ai.prompt.PromptTemplates;
import com.codesense.ai.rag.RagService;
import com.codesense.ai.vector.*;
import com.codesense.auth.model.User;
import com.codesense.auth.repository.UserRepository;
import com.codesense.project.model.Project;
import com.codesense.repository.model.Repository;
import com.codesense.repository.model.IngestionStatus;
import com.codesense.repository.repository.RepositoryRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RagService.
 * Team Member 3 (Karthik) — RAG pipeline tests.
 */
@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock VectorSearchService vectorSearchService;
    @Mock LLMService llmService;
    @Mock PromptTemplates promptTemplates;
    @Mock ConversationRepository conversationRepository;
    @Mock ConversationMessageRepository messageRepository;
    @Mock UserRepository userRepository;
    @Mock RepositoryRepo repositoryRepo;

    @Spy ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks RagService ragService;

    private UUID projectId;
    private UUID repositoryId;
    private User mockUser;
    private Repository mockRepo;
    private Project mockProject;

    @BeforeEach
    void setUp() {
        projectId    = UUID.randomUUID();
        repositoryId = UUID.randomUUID();

        mockProject = new Project();
        mockProject.setId(projectId);

        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("test@example.com");

        mockRepo = new Repository();
        mockRepo.setId(repositoryId);
        mockRepo.setName("test-repo");
        mockRepo.setProject(mockProject);
        mockRepo.setIngestionStatus(IngestionStatus.COMPLETED);
    }

    @Test
    void chat_returnsAnswerAndSources() {
        // Arrange
        Conversation savedConv = Conversation.builder()
            .id(UUID.randomUUID()).user(mockUser).repository(mockRepo).project(mockProject)
            .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(repositoryRepo.findById(repositoryId)).thenReturn(Optional.of(mockRepo));
        when(conversationRepository.save(any())).thenReturn(savedConv);
        when(messageRepository.findTop6ByConversationIdOrderByCreatedAtDesc(any())).thenReturn(new ArrayList<>());

        RepositoryChunk chunk = new RepositoryChunk();
        chunk.setFilePath("src/Auth.java");
        chunk.setLanguage("Java");
        chunk.setStartLine(1);
        chunk.setEndLine(50);
        chunk.setContent("JWT auth logic");
        when(vectorSearchService.semanticSearch(eq(projectId), eq(repositoryId), anyString(), anyInt()))
            .thenReturn(List.of(chunk));

        when(promptTemplates.repositoryChat(any(), any(), any())).thenReturn("full prompt");
        when(llmService.generate(any())).thenReturn(
            LLMResponse.builder().success(true)
                .generatedText("Authentication uses JWT tokens.").modelId("mock-llm").totalTokens(50).build());
        when(messageRepository.save(any())).thenReturn(new ConversationMessage());

        ChatRequestDto request = new ChatRequestDto();
        request.setProjectId(projectId);
        request.setRepositoryId(repositoryId);
        request.setQuestion("How does auth work?");

        // Act
        ChatResponseDto response = ragService.chat("test@example.com", request);

        // Assert
        assertThat(response.getAnswer()).contains("JWT tokens");
        assertThat(response.getSources()).hasSize(1);
        assertThat(response.getSources().get(0).getFilePath()).isEqualTo("src/Auth.java");
        verify(llmService).generate(any());
    }

    @Test
    void chat_whenNoChunks_returnsNoContextMessage() {
        Conversation savedConv = Conversation.builder()
            .id(UUID.randomUUID()).user(mockUser).repository(mockRepo).project(mockProject)
            .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(repositoryRepo.findById(repositoryId)).thenReturn(Optional.of(mockRepo));
        when(conversationRepository.save(any())).thenReturn(savedConv);
        when(messageRepository.findTop6ByConversationIdOrderByCreatedAtDesc(any())).thenReturn(new ArrayList<>());
        when(vectorSearchService.semanticSearch(any(), any(), any(), anyInt())).thenReturn(List.of());
        when(promptTemplates.repositoryChat(any(), any(), any())).thenReturn("prompt");
        when(llmService.generate(any())).thenReturn(
            LLMResponse.builder().success(true).generatedText("I could not find enough information.").modelId("mock").build());
        when(messageRepository.save(any())).thenReturn(new ConversationMessage());

        ChatRequestDto request = new ChatRequestDto();
        request.setProjectId(projectId);
        request.setRepositoryId(repositoryId);
        request.setQuestion("What is the answer?");

        ChatResponseDto response = ragService.chat("test@example.com", request);
        assertThat(response.getSources()).isEmpty();
    }
}
