package com.codesense.ai.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMResponse {

    private String generatedText;
    private String modelId;
    private String stopReason;
    private int promptTokens;
    private int generatedTokens;
    private int totalTokens;
    private long latencyMs;
    private boolean success;
    private String errorMessage;

    public static LLMResponse error(String message) {
        return LLMResponse.builder()
            .success(false)
            .errorMessage(message)
            .generatedText("")
            .build();
    }
}
