package com.codesense.ai.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock embedding service — default when AI_EMBEDDING_PROVIDER=mock.
 * Produces deterministic pseudo-random embeddings based on text hash.
 * NOT semantically meaningful, but consistent (same text → same vector).
 *
 * Used for local development when a real embedding provider is not configured.
 * PGVector is still populated and can be searched (results are heuristic, not semantic).
 */
@Slf4j
@Service("mockEmbeddingService")
@ConditionalOnProperty(name = "codesense.ai.embedding-provider", havingValue = "mock", matchIfMissing = true)
@Primary
public class MockEmbeddingService implements EmbeddingService {

    @Value("${codesense.ai.embedding-dimension:768}")
    private int dimension;

    @Override
    public float[] generateEmbedding(String text) {
        log.debug("[MockEmbedding] Generating mock embedding ({} chars)", text != null ? text.length() : 0);
        return generateDeterministicEmbedding(text, dimension);
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        List<float[]> embeddings = new ArrayList<>(texts.size());
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        return embeddings;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getProviderName() {
        return "MockEmbedding (Development Only)";
    }

    /**
     * Generates a deterministic embedding from the text's hash code.
     * Same text always produces the same embedding — useful for testing.
     * L2-normalized so cosine similarity works correctly.
     */
    private float[] generateDeterministicEmbedding(String text, int dim) {
        if (text == null || text.isBlank()) return new float[dim];
        long seed = text.hashCode();
        Random rng = new Random(seed);
        float[] embedding = new float[dim];
        double norm = 0;
        for (int i = 0; i < dim; i++) {
            embedding[i] = rng.nextFloat() * 2 - 1;
            norm += embedding[i] * embedding[i];
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dim; i++) {
                embedding[i] /= (float) norm;
            }
        }
        return embedding;
    }
}
