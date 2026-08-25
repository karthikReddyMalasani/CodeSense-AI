package com.codesense.ai.vector;

import com.codesense.ai.dto.SearchResultDto;
import com.codesense.ai.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Vector-based semantic search over repository chunks.
 *
 * SECURITY: All search methods require projectId AND repositoryId.
 * Cross-project access is architecturally impossible through this service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final RepositoryChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;

    @Value("${codesense.ai.rag.top-k:5}")
    private int defaultTopK;

    @Value("${codesense.ai.rag.min-similarity:0.7}")
    private double defaultMinSimilarity;

    /**
     * Search for chunks semantically similar to the query.
     */
    public List<RepositoryChunk> semanticSearch(UUID projectId, UUID repositoryId, String query, int topK) {
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        String pgVectorStr = toPgVectorString(queryEmbedding);
        log.debug("Semantic search: project={}, repo={}, queryLen={}, topK={}", projectId, repositoryId, query.length(), topK);
        return chunkRepository.findSimilarChunks(projectId, repositoryId, pgVectorStr, topK);
    }

    public List<RepositoryChunk> semanticSearchWithThreshold(
            UUID projectId, UUID repositoryId, String query, int topK, double minSimilarity) {
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        String pgVectorStr = toPgVectorString(queryEmbedding);
        return chunkRepository.findSimilarChunksAboveThreshold(projectId, repositoryId, pgVectorStr, minSimilarity, topK);
    }

    public List<SearchResultDto> search(UUID projectId, UUID repositoryId, String query) {
        List<RepositoryChunk> chunks = semanticSearch(projectId, repositoryId, query, defaultTopK);
        return chunks.stream().map(this::toSearchResult).collect(Collectors.toList());
    }

    @Transactional
    public RepositoryChunk saveChunk(RepositoryChunk chunk, float[] embedding) {
        chunk.setEmbedding(embedding);
        if (chunk.getContent() != null) {
            chunk.setContentHash(DigestUtils.sha256Hex(chunk.getContent()));
        }
        return chunkRepository.save(chunk);
    }

    @Transactional
    public List<RepositoryChunk> saveChunks(List<RepositoryChunk> chunks) {
        return chunkRepository.saveAll(chunks);
    }

    @Transactional
    public void deleteByRepository(UUID repositoryId) {
        chunkRepository.deleteByRepositoryId(repositoryId);
        log.info("Deleted all chunks for repository: {}", repositoryId);
    }

    public long countChunks(UUID repositoryId) {
        return chunkRepository.countByRepositoryId(repositoryId);
    }

    /**
     * Convert float[] to PGVector string format: "[0.1,0.2,...]"
     */
    public String toPgVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private SearchResultDto toSearchResult(RepositoryChunk chunk) {
        return SearchResultDto.builder()
            .chunkId(chunk.getId())
            .filePath(chunk.getFilePath())
            .language(chunk.getLanguage())
            .symbolName(chunk.getSymbolName())
            .symbolType(chunk.getSymbolType())
            .startLine(chunk.getStartLine())
            .endLine(chunk.getEndLine())
            .content(chunk.getContent())
            .chunkType(chunk.getChunkType() != null ? chunk.getChunkType().name() : "TEXT")
            .build();
    }
}
