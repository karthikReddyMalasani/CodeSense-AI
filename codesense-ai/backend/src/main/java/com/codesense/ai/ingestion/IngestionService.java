package com.codesense.ai.ingestion;

import com.codesense.ai.chunking.ChunkingService;
import com.codesense.ai.embedding.EmbeddingService;
import com.codesense.ai.exception.IngestionException;
import com.codesense.ai.vector.RepositoryChunk;
import com.codesense.ai.vector.VectorSearchService;
import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import com.codesense.repository.model.Repository;
import com.codesense.repository.model.IngestionStatus;
import com.codesense.repository.model.RepositoryFile;
import com.codesense.repository.repository.RepositoryFileRepository;
import com.codesense.repository.repository.RepositoryRepo;
import com.codesense.repository.service.LanguageDetectionService;
import com.codesense.repository.service.RepositoryProcessingLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AI Engine — Repository Ingestion Pipeline.
 * Team Member 3 (Karthik) owns this service.
 *
 * Pipeline:
 * Repository Files → Language Detection → Chunking → Embedding → PGVector
 *
 * Two paths:
 * 1. TEXT FALLBACK (current): Text-window chunking when parser unavailable
 * 2. PARSER-BASED (future): AST-based chunking when Team Member 4 integrates
 *
 * Security: All ingested data is scoped to project + repository IDs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final RepositoryRepo repositoryRepo;
    private final RepositoryFileRepository repositoryFileRepository;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;
    private final LanguageDetectionService languageDetectionService;
    private final RepositoryProcessingLock processingLock;

    @Value("${codesense.ai.ingestion.batch-size:50}")
    private int batchSize;

    /**
     * Trigger async ingestion for a repository.
     * Called after ZIP extraction or GitHub clone completes.
     */
    @Async("ingestionTaskExecutor")
    @Transactional
    public void ingestRepositoryAsync(UUID repositoryId) {
        try {
            ingestRepository(repositoryId);
        } catch (Exception e) {
            log.error("Ingestion failed for repository {}: {}", repositoryId, e.getMessage(), e);
            markIngestionFailed(repositoryId, e.getMessage());
        }
    }

    /**
     * Synchronous ingestion entry point.
     */
    @Transactional
    public void ingestRepository(UUID repositoryId) {
        ReentrantLock lock = processingLock.forRepository(repositoryId);
        lock.lock();
        try {
            ingestRepositoryLocked(repositoryId);
        } finally {
            lock.unlock();
        }
    }

    private void ingestRepositoryLocked(UUID repositoryId) {
        Repository repo = repositoryRepo.findByIdForUpdate(repositoryId)
            .orElseThrow(() -> new IngestionException("Repository not found: " + repositoryId));

        log.info("Starting ingestion for repository: {} ({})", repo.getName(), repositoryId);

        vectorSearchService.deleteByRepository(repositoryId);
        repo.setIngestionStatus(IngestionStatus.INGESTING);
        repositoryRepo.save(repo);

        List<RepositoryFile> files = repositoryFileRepository.findByRepositoryIdAndIgnoredFalse(repositoryId);
        log.info("Ingesting {} files for repository: {}", files.size(), repositoryId);

        List<RepositoryChunk> batch = new ArrayList<>();
        int totalChunks = 0;

        for (RepositoryFile file : files) {
            if (shouldSkipFile(file)) {
                continue;
            }

            try {
                totalChunks += processFileChunks(repo, file, batch);
            } catch (Exception e) {
                log.warn("Failed to chunk file {}: {}", file.getFilePath(), e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            totalChunks += embedAndSaveBatch(batch);
        }

        finalizeIngestion(repo, totalChunks);
    }

    private boolean shouldSkipFile(RepositoryFile file) {
        return file == null || file.isBinary() || file.getContent() == null || file.getContent().isBlank()
            || !languageDetectionService.isSupportedSourceLanguage(file.getLanguage());
    }

    private int processFileChunks(Repository repo, RepositoryFile file, List<RepositoryChunk> batch) {
        List<RepositoryChunk> fileChunks = chunkingService.chunkByText(
            file.getContent(),
            repo,
            repo.getProject(),
            file,
            file.getFilePath(),
            file.getLanguage()
        );

        batch.addAll(fileChunks);
        if (batch.size() >= batchSize) {
            int saved = embedAndSaveBatch(batch);
            batch.clear();
            return saved;
        }
        return 0;
    }

    private void finalizeIngestion(Repository repo, int totalChunks) {
        repo.setTotalChunks(totalChunks);
        repo.setIngestionStatus(IngestionStatus.COMPLETED);
        repositoryRepo.save(repo);
        log.info("Ingestion complete for repository {}: {} chunks created", repo.getId(), totalChunks);
    }

    /**
     * Ingest from Team Member 4's parser output.
     * Uses AST-based semantic chunks instead of text windows.
     * Called by ParserIntegrationService implementation.
     */
    @Transactional
    public void ingestFromParserMetadata(ParsedRepositoryDTO parsedRepository) {
        UUID repositoryId = UUID.fromString(parsedRepository.getRepositoryId());
        UUID projectId = UUID.fromString(parsedRepository.getProjectId());

        Repository repo = repositoryRepo.findById(repositoryId)
            .orElseThrow(() -> new IngestionException("Repository not found: " + repositoryId));

        log.info("Parser-based ingestion: {} files, {} elements",
            parsedRepository.getFiles().size(), parsedRepository.getTotalElements());

        // Clear existing text-based chunks and replace with semantic chunks
        vectorSearchService.deleteByRepository(repositoryId);

        List<RepositoryChunk> batch = new ArrayList<>();
        int totalChunks = 0;
        Map<String, RepositoryFile> filesByPath = repositoryFileRepository
            .findByRepositoryIdAndIgnoredFalse(repositoryId)
            .stream()
            .collect(Collectors.toMap(
                RepositoryFile::getFilePath,
                Function.identity(),
                (first, ignored) -> first
            ));

        for (ParsedFileDTO parsedFile : parsedRepository.getFiles()) {
            RepositoryFile repoFile = filesByPath.get(parsedFile.getFilePath());

            List<RepositoryChunk> fileChunks = chunkingService.chunkFromParserMetadata(
                parsedFile, repo, repo.getProject(), repoFile);  // null safe — RepositoryChunk.file is nullable

            batch.addAll(fileChunks);
            if (batch.size() >= batchSize) {
                totalChunks += embedAndSaveBatch(batch);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            totalChunks += embedAndSaveBatch(batch);
        }

        repo.setTotalChunks(totalChunks);
        repo.setIngestionStatus(IngestionStatus.COMPLETED);
        repositoryRepo.save(repo);

        log.info("Parser-based ingestion complete: {} chunks", totalChunks);
    }

    /**
     * Ingest a single file (for incremental updates from parser).
     */
    @Transactional
    public void ingestSingleParsedFile(ParsedFileDTO parsedFile) {
        UUID repositoryId = UUID.fromString(parsedFile.getRepositoryId());
        Repository repo = repositoryRepo.findById(repositoryId)
            .orElseThrow(() -> new IngestionException("Repository not found: " + repositoryId));

        List<RepositoryChunk> chunks = chunkingService.chunkFromParserMetadata(
            parsedFile, repo, repo.getProject(), null);

        if (!chunks.isEmpty()) {
            embedAndSaveBatch(chunks);
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private int embedAndSaveBatch(List<RepositoryChunk> chunks) {
        List<String> texts = chunks.stream().map(RepositoryChunk::getContent).toList();
        List<float[]> embeddings = embeddingService.generateEmbeddings(texts);

        for (int i = 0; i < chunks.size() && i < embeddings.size(); i++) {
            chunks.get(i).setEmbedding(embeddings.get(i));
        }

        vectorSearchService.saveChunks(chunks);
        log.debug("Saved batch of {} chunks", chunks.size());
        return chunks.size();
    }

    private void markIngestionFailed(UUID repositoryId, String error) {
        repositoryRepo.findById(repositoryId).ifPresent(repo -> {
            repo.setIngestionStatus(IngestionStatus.FAILED);
            repo.setErrorMessage("Ingestion failed: " + error);
            repositoryRepo.save(repo);
        });
    }
}
