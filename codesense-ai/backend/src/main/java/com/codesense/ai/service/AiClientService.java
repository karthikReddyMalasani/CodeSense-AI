package com.codesense.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

/**
 * Small AI client service that forwards prompts to a Gemini-compatible endpoint.
 *
 * IMPORTANT: Adjust the request body structure below to match the exact Gemini API
 * you are using. Providers differ in expected JSON shape (model/messages vs prompt/input).
 * This implementation sends a very generic JSON {"prompt": <string>, "model": "gemini-1"}
 * and returns the raw parsed JSON response as a Map.
 */
@Service
public class AiClientService {

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    @Value("${GEMINI_ENDPOINT:https://api.openai.com/v1/responses}")
    private String geminiEndpoint;

    private final WebClient client = WebClient.builder().build();

    public Map<String, Object> generateFromPrompt(String prompt, Map<String, Object> extraParams) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key not configured. Set GEMINI_API_KEY in environment.");
        }

        // Build a request body. Modify as required by the Gemini API you use.
        Map<String, Object> body = new HashMap<>();
        // Example fields - many Gemini-compatible endpoints accept either `input` or `prompt` or `messages`.
        body.put("model", extraParams != null && extraParams.containsKey("model") ? extraParams.get("model") : "gemini-1");
        body.put("prompt", prompt);
        if (extraParams != null) body.putAll(extraParams);

        try {
            Map response = client.post()
                .uri(geminiEndpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + geminiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            return response == null ? Map.of() : response;
        } catch (WebClientResponseException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", e.getRawStatusCode());
            err.put("body", e.getResponseBodyAsString());
            return Map.of("error", err);
        }
    }
}
