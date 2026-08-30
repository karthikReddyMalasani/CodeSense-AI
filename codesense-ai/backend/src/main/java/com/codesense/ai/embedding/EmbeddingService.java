package com.codesense.ai.embedding;

import java.util.List;

/**
 * AI Engine — Embedding abstraction.
 * Team Member 3 (Karthik) owns this interface and its implementations.
 *
 * Implementations:
 * - GeminiEmbeddingService: Google Gemini embeddings (production)
 * - MockEmbeddingService: Local development without IBM credentials
 */
public interface EmbeddingService {

    /**
     * Generate an embedding vector for a single text.
     * @param text the text to embed
     * @return float array of embedding dimensions
     */
    float[] generateEmbedding(String text);

    /**
     * Generate embedding vectors for a list of texts.
     * Implementations should batch these for efficiency.
     */
    List<float[]> generateEmbeddings(List<String> texts);

    /**
     * Get the dimensionality of embeddings produced by this service.
     */
    int getDimension();

    /**
     * Get a descriptive name for this embedding provider.
     */
    String getProviderName();
}
