package com.codesense.ai;

import com.codesense.ai.chunking.ChunkingService;
import com.codesense.ai.embedding.EmbeddingService;
import com.codesense.ai.exception.IngestionException;
import com.codesense.ai.ingestion.IngestionService;
import com.codesense.ai.vector.*;
import com.codesense.project.model.Project;
import com.codesense.repository.model.*;
import com.codesense.repository.repository.*;
import com.codesense.repository.service.LanguageDetectionService;
import com.codesense.repository.service.RepositoryProcessingLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

/**
 * Unit tests for IngestionService.
 * Team Member 3 (Karthik) — ingestion pipeline tests.
 */
@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock RepositoryRepo repositoryRepo;
    @Mock RepositoryFileRepository repositoryFileRepository;
    @Mock ChunkingService chunkingService;
    @Mock EmbeddingService embeddingService;
    @Mock VectorSearchService vectorSearchService;
    @Mock LanguageDetectionService languageDetectionService;
    @Mock RepositoryProcessingLock processingLock;

    @InjectMocks IngestionService ingestionService;

    private UUID repositoryId;
    private Repository mockRepo;

    @BeforeEach
    void setUp() {
        repositoryId = UUID.randomUUID();
        when(processingLock.forRepository(any())).thenReturn(new java.util.concurrent.locks.ReentrantLock());

        Project p = new Project();
        p.setId(UUID.randomUUID());

        mockRepo = new Repository();
        mockRepo.setId(repositoryId);
        mockRepo.setName("test-repo");
        mockRepo.setIngestionStatus(IngestionStatus.PENDING);
        mockRepo.setProject(p);
    }

    @Test
    void ingestRepository_skipsWhenNoFiles() {
        when(repositoryRepo.findByIdForUpdate(repositoryId)).thenReturn(Optional.of(mockRepo));
        when(repositoryRepo.save(any())).thenReturn(mockRepo);
        doNothing().when(vectorSearchService).deleteByRepository(repositoryId);
        when(repositoryFileRepository.findByRepositoryIdAndIgnoredFalse(repositoryId))
            .thenReturn(List.of());

        assertThatCode(() -> ingestionService.ingestRepository(repositoryId))
            .doesNotThrowAnyException();

        // No embeddings should be generated when there are no files
        verify(embeddingService, never()).generateEmbedding(any());
    }

    @Test
    void ingestRepository_generatesEmbeddingsForTextFiles() {
        when(repositoryRepo.findByIdForUpdate(repositoryId)).thenReturn(Optional.of(mockRepo));
        when(repositoryRepo.save(any())).thenReturn(mockRepo);
        doNothing().when(vectorSearchService).deleteByRepository(repositoryId);

        RepositoryFile file = new RepositoryFile();
        file.setId(UUID.randomUUID());
        file.setFilePath("src/Main.java");
        file.setLanguage("Java");
        file.setBinary(false);
        file.setContent("public class Main { public static void main(String[] args) {} }");
        file.setSizeBytes(64L);
        file.setIgnored(false);

        when(repositoryFileRepository.findByRepositoryIdAndIgnoredFalse(repositoryId))
            .thenReturn(List.of(file));
        when(languageDetectionService.isSupportedSourceLanguage("Java")).thenReturn(true);

        // chunkByText returns a list of RepositoryChunk objects
        RepositoryChunk chunk = new RepositoryChunk();
        chunk.setContent("public class Main {}");
        chunk.setFilePath("src/Main.java");
        when(chunkingService.chunkByText(anyString(), any(), any(), any(), anyString(), anyString()))
            .thenReturn(List.of(chunk));

        // embedAndSaveBatch uses generateEmbeddings (batch) + saveChunks (batch)
        when(embeddingService.generateEmbeddings(anyList())).thenReturn(List.of(new float[]{0.1f, 0.2f}));
        when(vectorSearchService.saveChunks(anyList())).thenReturn(List.of(chunk));

        assertThatCode(() -> ingestionService.ingestRepository(repositoryId))
            .doesNotThrowAnyException();

        verify(embeddingService, atLeastOnce()).generateEmbeddings(anyList());
    }

    @Test
    void ingestRepository_throwsWhenRepoNotFound() {
        when(repositoryRepo.findByIdForUpdate(repositoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingestionService.ingestRepository(repositoryId))
            .isInstanceOf(IngestionException.class);
    }
}
