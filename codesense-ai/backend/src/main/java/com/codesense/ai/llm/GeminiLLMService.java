package com.codesense.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** LLM provider backed by Google's Gemini generateContent API. */
@Slf4j
@Service("geminiLLMService")
@ConditionalOnProperty(name = "codesense.ai.llm-provider", havingValue = "gemini")
@RequiredArgsConstructor
public class GeminiLLMService implements LLMService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.endpoint:https://generativelanguage.googleapis.com/v1beta/models}")
    private String endpoint;

    @Override
    public LLMResponse generate(LLMRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            return LLMResponse.error("Gemini is not configured. Add GEMINI_API_KEY to the backend environment and redeploy.");
        }

        long start = System.currentTimeMillis();
        try {
            Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", request.getPrompt()))
                )),
                "generationConfig", Map.of(
                    "temperature", request.getTemperature(),
                    "maxOutputTokens", request.getMaxNewTokens()
                )
            );

            String responseBody = webClientBuilder.build()
                .post()
                .uri(endpoint + "/" + model + ":generateContent?key=" + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode response = objectMapper.readTree(responseBody);
            String text = response.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text").asText("").trim();
            if (text.isBlank()) {
                return LLMResponse.error("Gemini returned an empty response. Try asking a more specific question.");
            }

            int totalTokens = response.path("usageMetadata").path("totalTokenCount").asInt(0);
            return LLMResponse.builder()
                .generatedText(text)
                .modelId("gemini/" + model)
                .totalTokens(totalTokens)
                .latencyMs(System.currentTimeMillis() - start)
                .success(true)
                .build();
        } catch (WebClientResponseException e) {
            log.error("Gemini API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                return LLMResponse.error("Gemini authentication failed. Check GEMINI_API_KEY in the backend environment.");
            }
            if (e.getStatusCode().value() == 404) {
                return LLMResponse.error("The configured Gemini model is unavailable. Set GEMINI_MODEL to gemini-2.5-flash and redeploy.");
            }
            if (e.getStatusCode().value() == 429) {
                return LLMResponse.error("Gemini rate limit reached. Please wait a moment and try again.");
            }
            return LLMResponse.error("Gemini is temporarily unavailable. Please try again shortly.");
        } catch (Exception e) {
            log.error("Gemini call failed: {}", e.getMessage());
            return LLMResponse.error("The AI provider could not be reached. Check the Gemini configuration and try again.");
        }
    }

    @Override
    public String getProviderName() {
        return "Google Gemini — " + model;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }
}
