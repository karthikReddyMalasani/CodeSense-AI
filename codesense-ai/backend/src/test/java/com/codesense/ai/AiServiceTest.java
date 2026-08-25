package com.codesense.ai;

import com.codesense.ai.dto.*;
import com.codesense.ai.ingestion.IngestionService;
import com.codesense.ai.llm.*;
import com.codesense.ai.model.Documentation;
import com.codesense.ai.model.DocumentationRepository;
import com.codesense.ai.prompt.PromptTemplates;
import com.codesense.ai.rag.RagService;
import com.codesense.ai.service.AiService;
import com.codesense.ai.vector.VectorSearchService;
import com.codesense.common.exception.ResourceNotFoundException;
import com.codesense.repository.model.Repository;
import com.codesense.repository.model.RepositoryFile;
import com.codesense.repository.repository.RepositoryFileRepository;
import com.codesense.repository.repository.RepositoryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AiService.
 * Team Member 3 (Karthik) — AI Engine tests.
 */
@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock RagService ragService;
    @Mock IngestionService ingestionService;
    @Mock VectorSearchService vectorSearchService;
    @Mock LLMService llmService;
    @Mock PromptTemplates promptTemplates;
    @Mock RepositoryRepo repositoryRepo;
    @Mock RepositoryFileRepository repositoryFileRepository;
    @Mock DocumentationRepository documentationRepository;

    @InjectMocks AiService aiService;

    private UUID projectId;
    private UUID repositoryId;
    private Repository mockRepo;

    @BeforeEach
    void setUp() {
        projectId    = UUID.randomUUID();
        repositoryId = UUID.randomUUID();
        mockRepo = new Repository();
        mockRepo.setId(repositoryId);
        mockRepo.setName("test-repo");
    }

    // ─── health ──────────────────────────────────────────────────────────────

    @Test
    void health_returnsStatusUp() {
        when(llmService.getProviderName()).thenReturn("Mock LLM");
        when(llmService.isAvailable()).thenReturn(true);

        Map<String, Object> health = aiService.getHealth();

        assertThat(health.get("status")).isEqualTo("UP");
        assertThat(health.get("llmProvider")).isEqualTo("Mock LLM");
        assertThat(health.get("llmAvailable")).isEqualTo(true);
    }

    // ─── code explanation ────────────────────────────────────────────────────

    @Test
    void explainCode_delegatestoLLM_andReturnsExplanation() {
        when(repositoryRepo.findByIdAndProjectId(repositoryId, projectId))
            .thenReturn(Optional.of(mockRepo));
        when(promptTemplates.codeExplanation(any(), any(), any())).thenReturn("prompt");
        when(llmService.generate(any())).thenReturn(
            LLMResponse.builder().success(true).generatedText("## Summary\nThis is auth code.").modelId("mock").build());

        CodeExplainRequestDto req = new CodeExplainRequestDto();
        req.setProjectId(projectId);
        req.setRepositoryId(repositoryId);
        req.setCode("public class AuthController {}");
        req.setLanguage("Java");

        CodeExplainResponseDto resp = aiService.explainCode(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getSummary()).isNotBlank();
    }

    @Test
    void explainCode_whenLLMFails_returnsFallbackMessage() {
        when(repositoryRepo.findByIdAndProjectId(repositoryId, projectId))
            .thenReturn(Optional.of(mockRepo));
        when(promptTemplates.codeExplanation(any(), any(), any())).thenReturn("p");
        when(llmService.generate(any())).thenReturn(
            LLMResponse.builder().success(false).errorMessage("timeout").build());

        CodeExplainRequestDto req = new CodeExplainRequestDto();
        req.setProjectId(projectId);
        req.setRepositoryId(repositoryId);
        req.setCode("code");
        req.setLanguage("Java");

        CodeExplainResponseDto resp = aiService.explainCode(req);
        assertThat(resp.getRawExplanation()).contains("Failed");
    }

    @Test
    void explainCode_throwsWhenRepositoryNotFound() {
        when(repositoryRepo.findByIdAndProjectId(any(), any())).thenReturn(Optional.empty());

        CodeExplainRequestDto req = new CodeExplainRequestDto();
        req.setProjectId(projectId);
        req.setRepositoryId(repositoryId);
        req.setCode("code");
        req.setLanguage("Java");

        assertThatThrownBy(() -> aiService.explainCode(req))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
