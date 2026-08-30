package com.codesense.ai.llm;

import java.util.List;

/**
 * AI Engine — LLM abstraction.
 * Team Member 3 (Karthik) owns this interface and its implementations.
 *
 * Implementations:
 * - GeminiLLMService: production Google Gemini
 * - WatsonxLLMService: IBM watsonx.ai / IBM Granite
 * - MockLLMService: local development without IBM credentials
 */
public interface LLMService {

    /**
     * Generate a completion for the given prompt.
     */
    LLMResponse generate(LLMRequest request);

    /**
     * Generate a response for a simple text prompt using default parameters.
     */
    default String generateText(String prompt) {
        LLMRequest request = LLMRequest.builder().prompt(prompt).build();
        return generate(request).getGeneratedText();
    }

    /**
     * Get a descriptive name for this LLM provider.
     */
    String getProviderName();

    /**
     * Check whether this LLM provider is currently available/healthy.
     */
    boolean isAvailable();
}
