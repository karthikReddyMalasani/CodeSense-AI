package com.codesense.ai.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * Groq LLM provider — FREE cloud API, no credit card required.
 *
 * Groq offers free API access (with generous rate limits) to:
 * - llama-3.3-70b-versatile  (best quality, very fast)
 * - llama3-8b-8192           (lightweight, fastest)
 * - gemma2-9b-it             (Google Gemma 2)
 * - mixtral-8x7b-32768       (Mistral MoE)
 *
 * HOW TO GET FREE GROQ API KEY (no credit card):
 * 1. Go to https://console.groq.com
 * 2. Sign up with GitHub or Google (free, no card)
 * 3. Click "API Keys" → "Create API Key"
 * 4. Copy key → set GROQ_API_KEY in .env
 * 5. Set AI_LLM_PROVIDER=groq
 *
 * Free tier limits: ~14,400 requests/day, 6000 tokens/min
 *
 * Activated when: AI_LLM_PROVIDER=groq
 */
@Slf4j
@Service("groqLLMService")
@ConditionalOnProperty(name = "codesense.ai.llm-provider", havingValue = "groq")
@RequiredArgsConstructor
public class GroqLLMService implements LLMService {

    private final WebClient.Builder webClientBuilder;

    @Value("${groq.api-key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.1-8b-instant}")
    private String model;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    @Override
    public LLMResponse generate(LLMRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            return LLMResponse.error(
                "GROQ_API_KEY not set. Get a free key at https://console.groq.com (no credit card)");
        }

        long start = System.currentTimeMillis();
        try {
            // Groq uses OpenAI-compatible API format
            GroqRequest body = GroqRequest.builder()
                .model(model)
                .messages(List.of(
                    new GroqRequest.Message("user", request.getPrompt())
                ))
                .maxTokens(request.getMaxNewTokens())
                .temperature(request.getTemperature())
                .build();

            GroqResponse response = webClientBuilder.build()
                .post()
                .uri(GROQ_API_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GroqResponse.class)
                .block();

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                return LLMResponse.error("Empty response from Groq");
            }

            String text = response.getChoices().get(0).getMessage().getContent();
            long latency = System.currentTimeMillis() - start;

            log.debug("Groq response: model={}, tokens={}, latency={}ms",
                model, response.getUsage() != null ? response.getUsage().getTotalTokens() : 0, latency);

            return LLMResponse.builder()
                .generatedText(text != null ? text.trim() : "")
                .modelId("groq/" + model)
                .stopReason(response.getChoices().get(0).getFinishReason())
                .promptTokens(response.getUsage() != null ? response.getUsage().getPromptTokens() : 0)
                .generatedTokens(response.getUsage() != null ? response.getUsage().getCompletionTokens() : 0)
                .totalTokens(response.getUsage() != null ? response.getUsage().getTotalTokens() : 0)
                .latencyMs(latency)
                .success(true)
                .build();

        } catch (WebClientResponseException e) {
            log.error("Groq API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 429) {
                return LLMResponse.error("Groq rate limit reached. Wait a moment and try again.");
            }
            return LLMResponse.error("Groq API error: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Groq call failed: {}", e.getMessage());
            return LLMResponse.error("Groq error: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "Groq (free) — " + model;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    // ─── Request / Response DTOs ─────────────────────────────────────────────

    @Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    private static class GroqRequest {
        private String model;
        private List<Message> messages;
        @JsonProperty("max_tokens")  private int maxTokens;
        private double temperature;

        @Data @lombok.AllArgsConstructor @lombok.NoArgsConstructor
        static class Message {
            private String role;
            private String content;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GroqResponse {
        private List<Choice> choices;
        private Usage usage;

        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        static class Choice {
            private Message message;
            @JsonProperty("finish_reason") private String finishReason;
        }
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        static class Message {
            private String role;
            private String content;
        }
        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        static class Usage {
            @JsonProperty("prompt_tokens")     private int promptTokens;
            @JsonProperty("completion_tokens") private int completionTokens;
            @JsonProperty("total_tokens")      private int totalTokens;
        }
    }
}
