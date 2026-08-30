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
import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.ParsedFile;
import com.codesense.parser.service.ParserRouter;
import com.codesense.project.model.Project;
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
    @Mock ParserRouter parserRouter;

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

    @Test
    void generateApiDocs_whenLLMFails_usesDetectedRoutesFromFileContent() {
        Project project = new Project();
        project.setId(projectId);
        mockRepo.setProject(project);
        mockRepo.setPrimaryLanguage("Java");

        RepositoryFile authFile = RepositoryFile.builder()
            .filePath("src/main/java/com/example/AuthController.java")
            .fileName("AuthController.java")
            .language("Java")
            .content("""
                @RestController
                @RequestMapping(\"/api/auth\")
                public class AuthController {
                    @PostMapping(\"/register\")
                    public String register() { return \"ok\"; }

                    @DeleteMapping(\"/logout\")
                    public String logout() { return \"ok\"; }
                }
                """)
            .build();

        RepositoryFile userFile = RepositoryFile.builder()
            .filePath("src/main/java/com/example/UserController.java")
            .fileName("UserController.java")
            .language("Java")
            .content("""
                @RestController
                public class UserController {
                    @GetMapping(\"/users\")
                    public List<String> listUsers() { return List.of(); }
                }
                """)
            .build();

        when(repositoryRepo.findById(repositoryId)).thenReturn(Optional.of(mockRepo));
        when(repositoryRepo.findByIdAndProjectId(repositoryId, projectId)).thenReturn(Optional.of(mockRepo));
        when(repositoryFileRepository.findByRepositoryIdAndIgnoredFalse(repositoryId)).thenReturn(List.of(userFile, authFile));
        when(parserRouter.parse(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            String filePath = invocation.getArgument(0);
            String content = invocation.getArgument(1);
            String language = invocation.getArgument(2);

            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            List<CodeElement> elements = new ArrayList<>();
            if (fileName.contains("AuthController")) {
                elements.add(CodeElement.builder().name("register").type(CodeElement.ElementType.METHOD).annotations(List.of()).build());
                elements.add(CodeElement.builder().name("logout").type(CodeElement.ElementType.METHOD).annotations(List.of()).build());
            } else {
                elements.add(CodeElement.builder().name("listUsers").type(CodeElement.ElementType.METHOD).annotations(List.of()).build());
            }
            return ParsedFile.builder().filePath(filePath).language(language).content(content).elements(elements).relationships(List.of()).build();
        });

        when(llmService.generate(any())).thenReturn(LLMResponse.builder().success(false).errorMessage("timeout").build());
        when(documentationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GenerateApiDocsRequestDto req = new GenerateApiDocsRequestDto();
        req.setProjectId(projectId);
        req.setRepositoryId(repositoryId);

        GenerateApiDocsResponseDto resp = aiService.generateApiDocs("user@test.com", req);

        assertThat(resp.getContent()).contains("POST").contains("DELETE").contains("GET");
        assertThat(resp.getContent()).doesNotContain("| `GET` | `/register` |");
    }
}
