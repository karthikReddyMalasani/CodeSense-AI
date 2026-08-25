package com.codesense.ai;

import com.codesense.ai.llm.LLMRequest;
import com.codesense.ai.llm.LLMResponse;
import com.codesense.ai.llm.MockLLMService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class MockLLMServiceTest {

    private final MockLLMService service = new MockLLMService();

    @Test
    void generate_returnsSuccessResponse() {
        LLMRequest req = LLMRequest.builder().prompt("How does authentication work?").build();
        LLMResponse response = service.generate(req);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getGeneratedText()).isNotBlank();
        assertThat(response.getModelId()).isEqualTo("mock/dev");
    }

    @Test
    void generate_authQuestion_containsJWT() {
        LLMRequest req = LLMRequest.builder().prompt("explain JWT authentication").build();
        LLMResponse response = service.generate(req);
        assertThat(response.getGeneratedText()).containsIgnoringCase("jwt");
    }

    @Test
    void generate_readmeRequest_containsDocumentation() {
        LLMRequest req = LLMRequest.builder().prompt("generate readme documentation").build();
        LLMResponse response = service.generate(req);
        assertThat(response.getGeneratedText()).isNotBlank();
    }

    @Test
    void generate_nullPrompt_returnsResponse() {
        LLMRequest req = LLMRequest.builder().prompt(null).build();
        LLMResponse response = service.generate(req);
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void isAvailable_alwaysTrue() {
        assertThat(service.isAvailable()).isTrue();
    }

    @Test
    void getProviderName_containsMock() {
        assertThat(service.getProviderName()).containsIgnoringCase("mock");
    }
}
