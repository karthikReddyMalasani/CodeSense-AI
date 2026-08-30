package com.codesense.ai.vector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Repository for PGVector-based semantic search on repository chunks.
 *
 * SECURITY CRITICAL:
 * Every search query MUST filter by project_id AND repository_id.
 * Never allow cross-project vector retrieval.
 */
@Repository
public interface RepositoryChunkRepository extends JpaRepository<RepositoryChunk, UUID> {

    /**
     * Semantic similarity search using cosine distance (<=> operator).
     * Returns top-k chunks ordered by similarity.
     */
    @Query(value = """
        SELECT rc.*
        FROM repository_chunks rc
        WHERE rc.project_id = :projectId
          AND rc.repository_id = :repositoryId
          AND rc.embedding IS NOT NULL
        ORDER BY rc.embedding <=> CAST(:embedding AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<RepositoryChunk> findSimilarChunks(
        @Param("projectId") UUID projectId,
        @Param("repositoryId") UUID repositoryId,
        @Param("embedding") String embedding,
        @Param("topK") int topK
    );

    /**
     * Semantic similarity search with minimum similarity threshold.
     */
    @Query(value = """
        SELECT rc.*
        FROM repository_chunks rc
        WHERE rc.project_id = :projectId
          AND rc.repository_id = :repositoryId
          AND rc.embedding IS NOT NULL
          AND (1 - (rc.embedding <=> CAST(:embedding AS vector))) >= :minSimilarity
        ORDER BY rc.embedding <=> CAST(:embedding AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<RepositoryChunk> findSimilarChunksAboveThreshold(
        @Param("projectId") UUID projectId,
        @Param("repositoryId") UUID repositoryId,
        @Param("embedding") String embedding,
        @Param("minSimilarity") double minSimilarity,
        @Param("topK") int topK
    );

    /**
     * Semantic search filtered by language.
     */
    @Query(value = """
        SELECT rc.*
        FROM repository_chunks rc
        WHERE rc.project_id = :projectId
          AND rc.repository_id = :repositoryId
          AND rc.language = :language
          AND rc.embedding IS NOT NULL
        ORDER BY rc.embedding <=> CAST(:embedding AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<RepositoryChunk> findSimilarChunksByLanguage(
        @Param("projectId") UUID projectId,
        @Param("repositoryId") UUID repositoryId,
        @Param("embedding") String embedding,
        @Param("language") String language,
        @Param("topK") int topK
    );

    long countByRepositoryId(UUID repositoryId);

    @Modifying
    @Transactional
    @Query("delete from RepositoryChunk chunk where chunk.repository.id = :repositoryId")
    void deleteByRepositoryId(@Param("repositoryId") UUID repositoryId);

    @Modifying
    @Transactional
    void deleteByProjectId(UUID projectId);

    boolean existsByRepositoryId(UUID repositoryId);
}
