package com.codesense.ai.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMRequest {

    private String prompt;
    private String systemInstruction;
    private List<ChatMessage> messages;

    @Builder.Default
    private int maxNewTokens = 2048;

    @Builder.Default
    private double temperature = 0.1;

    @Builder.Default
    private double topP = 1.0;

    @Builder.Default
    private int topK = 50;

    @Builder.Default
    private double repetitionPenalty = 1.05;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ChatMessage {
        private String role;   // "user" | "assistant" | "system"
        private String content;
    }
}
