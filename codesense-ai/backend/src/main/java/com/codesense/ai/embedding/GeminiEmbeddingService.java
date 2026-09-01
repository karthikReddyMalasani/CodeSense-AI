package com.codesense.ai.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Semantic embeddings backed by Google's Gemini embedding API. */
@Slf4j
@Service("geminiEmbeddingService")
@ConditionalOnProperty(name = "codesense.ai.embedding-provider", havingValue = "gemini")
@RequiredArgsConstructor
public class GeminiEmbeddingService implements EmbeddingService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.embedding-model:gemini-embedding-001}")
    private String model;

    @Value("${gemini.embedding-dimension:768}")
    private int dimension;

    @Value("${gemini.endpoint:https://generativelanguage.googleapis.com/v1beta/models}")
    private String endpoint;

    @Override
    public float[] generateEmbedding(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini embedding key missing; falling back to deterministic mock embeddings so repository natural-language search still works locally.");
            return fallbackMockEmbedding(text);
        }

        Map<String, Object> body = Map.of(
            "content", Map.of("parts", List.of(Map.of("text", text == null ? "" : text))),
            "outputDimensionality", dimension
        );

        try {
            String responseBody = webClientBuilder.build()
                .post()
                .uri(endpoint + "/" + model + ":embedContent?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode values = objectMapper.readTree(responseBody).path("embedding").path("values");
            if (!values.isArray() || values.size() != dimension) {
                throw new IllegalStateException("Gemini returned an invalid embedding dimension.");
            }

            float[] embedding = new float[dimension];
            for (int i = 0; i < dimension; i++) {
                embedding[i] = (float) values.get(i).asDouble();
            }
            return embedding;
        } catch (WebClientResponseException e) {
            log.error("Gemini embedding API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Gemini embeddings are unavailable. Check GEMINI_API_KEY and GEMINI_EMBEDDING_MODEL.", e);
        } catch (Exception e) {
            log.error("Gemini embedding generation failed: {}", e.getMessage());
            throw new IllegalStateException("Could not generate a Gemini embedding. Please try again.", e);
        }
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
        return "Google Gemini Embeddings — " + model;
    }

    private float[] fallbackMockEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[dimension];
        }

        long seed = text.hashCode();
        java.util.Random rng = new java.util.Random(seed);
        float[] embedding = new float[dimension];
        double norm = 0;

        for (int i = 0; i < dimension; i++) {
            embedding[i] = rng.nextFloat() * 2 - 1;
            norm += embedding[i] * embedding[i];
        }

        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dimension; i++) {
                embedding[i] /= (float) norm;
            }
        }

        return embedding;
    }
}
