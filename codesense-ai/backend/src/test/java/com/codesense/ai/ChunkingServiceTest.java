package com.codesense.ai;

import com.codesense.ai.chunking.ChunkingService;
import com.codesense.ai.vector.RepositoryChunk;
import com.codesense.project.model.Project;
import com.codesense.repository.model.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ChunkingService.
 * Team Member 5 (Vishnu) — Testing.
 */
class ChunkingServiceTest {

    private ChunkingService chunkingService;
    private Repository mockRepository;
    private Project mockProject;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService();
        ReflectionTestUtils.setField(chunkingService, "chunkSize", 500);
        ReflectionTestUtils.setField(chunkingService, "chunkOverlap", 100);

        mockProject = Project.builder().name("test-project").build();
        mockRepository = Repository.builder()
            .id(UUID.randomUUID())
            .name("test-repo")
            .project(mockProject)
            .build();
    }

    @Test
    void chunkByText_shortContent_createsSingleChunk() {
        String content = "public class Hello {\n    public void greet() {\n        System.out.println(\"Hello\");\n    }\n}";
        List<RepositoryChunk> chunks = chunkingService.chunkByText(
            content, mockRepository, mockProject, null, "Hello.java", "Java");

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getLanguage()).isEqualTo("Java");
        assertThat(chunks.get(0).getFilePath()).isEqualTo("Hello.java");
    }

    @Test
    void chunkByText_longContent_createsMultipleChunks() {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            content.append("// Line ").append(i).append("\n");
            content.append("public void method").append(i).append("() {}\n");
        }

        List<RepositoryChunk> chunks = chunkingService.chunkByText(
            content.toString(), mockRepository, mockProject, null, "Large.java", "Java");

        assertThat(chunks).isNotEmpty();
        chunks.forEach(c -> assertThat(c.getContent()).isNotBlank());
    }

    @Test
    void chunkByText_emptyContent_returnsEmptyList() {
        List<RepositoryChunk> chunks = chunkingService.chunkByText(
            "", mockRepository, mockProject, null, "Empty.java", "Java");

        assertThat(chunks).isEmpty();
    }

    @Test
    void chunkByText_nullContent_returnsEmptyList() {
        List<RepositoryChunk> chunks = chunkingService.chunkByText(
            null, mockRepository, mockProject, null, "Null.java", "Java");

        assertThat(chunks).isEmpty();
    }

    @Test
    void chunkByText_assignsRepositoryAndProject() {
        String content = "public class Test {}";
        List<RepositoryChunk> chunks = chunkingService.chunkByText(
            content, mockRepository, mockProject, null, "Test.java", "Java");

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getRepository()).isEqualTo(mockRepository);
        assertThat(chunks.get(0).getProject()).isEqualTo(mockProject);
    }
}
