package com.codesense.ai.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Mock LLM — used by tests and when no AI_LLM_PROVIDER is set.
 *
 * For real AI responses, set in .env:
 *   AI_LLM_PROVIDER=groq
 *   GROQ_API_KEY=gsk_...   (free at console.groq.com)
 */
@Slf4j
@Service("mockLLMService")
@ConditionalOnProperty(name = "codesense.ai.llm-provider", havingValue = "mock", matchIfMissing = true)
@Primary
public class MockLLMService implements LLMService {

    @Override
    public LLMResponse generate(LLMRequest request) {
        log.info("[MockLLM] Generating mock response (prompt {} chars)",
            request.getPrompt() != null ? request.getPrompt().length() : 0);

        String mockResponse = buildMockResponse(request.getPrompt());

        return LLMResponse.builder()
            .generatedText(mockResponse)
            .modelId("mock/dev")
            .stopReason("eos_token")
            .promptTokens(estimateTokens(request.getPrompt()))
            .generatedTokens(estimateTokens(mockResponse))
            .totalTokens(estimateTokens(request.getPrompt()) + estimateTokens(mockResponse))
            .latencyMs(50L)
            .success(true)
            .build();
    }

    @Override
    public String getProviderName() {
        return "MockLLM (set AI_LLM_PROVIDER=groq for real responses)";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private String buildMockResponse(String prompt) {
        if (prompt == null) return "I need a prompt to generate a response.";
        String lower = prompt.toLowerCase();

        if (lower.contains("authentication") || lower.contains("auth") || lower.contains("jwt")) {
            return """
                ## Authentication Overview

                The application uses **JWT (JSON Web Token)** based authentication.

                **Flow:**
                1. User submits credentials via `POST /api/auth/login`
                2. Server validates credentials against the database
                3. Server issues a signed JWT token
                4. Client includes `Authorization: Bearer <token>` in subsequent requests
                5. `JwtAuthenticationFilter` validates the token on every protected request

                **Key classes:**
                - `JwtService` — token generation and validation
                - `JwtAuthenticationFilter` — Spring Security filter
                - `SecurityConfig` — security chain configuration
                - `AuthService` — credential validation and user management

                *[Mock response — set AI_LLM_PROVIDER=groq in .env for real AI answers]*
                """;
        }

        if (lower.contains("readme") || lower.contains("documentation")) {
            return """
                # Project Documentation

                ## Overview
                This is an AI-generated README based on repository analysis.

                ## Getting Started
                Follow the setup instructions in the configuration files.

                ## Architecture
                The project follows a clean layered architecture pattern.

                *[Mock response — set AI_LLM_PROVIDER=groq in .env for real AI answers]*
                """;
        }

        if (lower.contains("explain") || lower.contains("code")) {
            return """
                ## Code Explanation

                This code snippet implements a well-structured component following best practices.

                **Summary:** The code defines a service/class responsible for handling a specific domain concern.

                **Key Components:**
                - Input validation
                - Business logic processing
                - Output transformation

                **Logic Flow:**
                1. Receives input parameters
                2. Validates and processes them
                3. Returns structured output

                *[Mock response — set AI_LLM_PROVIDER=groq in .env for real AI answers]*
                """;
        }

        return """
            Thank you for your question about the repository.

            Based on the context provided, I found relevant information in the repository.
            The repository appears to contain a well-structured application with standard patterns.

            For more detailed analysis, ensure your repository has been properly ingested.

            *[Mock response — set AI_LLM_PROVIDER=groq and GROQ_API_KEY in .env for real AI answers]*
            """;
    }

    private int estimateTokens(String text) {
        if (text == null) return 0;
        return (int) Math.ceil(text.length() / 4.0);
    }
}
