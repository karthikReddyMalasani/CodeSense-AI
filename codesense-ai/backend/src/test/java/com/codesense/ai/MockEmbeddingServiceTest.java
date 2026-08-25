package com.codesense.ai;

import com.codesense.ai.embedding.MockEmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class MockEmbeddingServiceTest {

    @Test
    void generateEmbedding_returnsDimensionArray() {
        MockEmbeddingService service = new MockEmbeddingService();
        ReflectionTestUtils.setField(service, "dimension", 768);

        float[] embedding = service.generateEmbedding("hello world");

        assertThat(embedding).hasSize(768);
    }

    @Test
    void generateEmbedding_isDeterministic() {
        MockEmbeddingService service = new MockEmbeddingService();
        ReflectionTestUtils.setField(service, "dimension", 768);

        float[] e1 = service.generateEmbedding("authentication service");
        float[] e2 = service.generateEmbedding("authentication service");

        assertThat(e1).isEqualTo(e2);
    }

    @Test
    void generateEmbedding_differentTexts_differentEmbeddings() {
        MockEmbeddingService service = new MockEmbeddingService();
        ReflectionTestUtils.setField(service, "dimension", 768);

        float[] e1 = service.generateEmbedding("hello world");
        float[] e2 = service.generateEmbedding("foo bar baz");

        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    void generateEmbeddings_batch() {
        MockEmbeddingService service = new MockEmbeddingService();
        ReflectionTestUtils.setField(service, "dimension", 768);

        List<float[]> embeddings = service.generateEmbeddings(
            List.of("text1", "text2", "text3"));

        assertThat(embeddings).hasSize(3);
        embeddings.forEach(e -> assertThat(e).hasSize(768));
    }

    @Test
    void generateEmbedding_nullText_returnsZeroVector() {
        MockEmbeddingService service = new MockEmbeddingService();
        ReflectionTestUtils.setField(service, "dimension", 768);

        float[] embedding = service.generateEmbedding(null);
        assertThat(embedding).hasSize(768);
    }

    @Test
    void generateEmbedding_isL2Normalized() {
        MockEmbeddingService service = new MockEmbeddingService();
        ReflectionTestUtils.setField(service, "dimension", 768);

        float[] embedding = service.generateEmbedding("test normalization");
        double norm = 0;
        for (float v : embedding) norm += v * v;
        assertThat(Math.sqrt(norm)).isCloseTo(1.0, within(0.001));
    }
}
