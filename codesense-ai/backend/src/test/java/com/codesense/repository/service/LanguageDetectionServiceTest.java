package com.codesense.repository.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageDetectionServiceTest {

    private final LanguageDetectionService service = new LanguageDetectionService();

    @Test
    void detectLanguage_recognizesMarkdownReadme() {
        assertThat(service.detectLanguage("README.md")).contains("Markdown");
    }

    @Test
    void isSupportedSourceLanguage_allowsDocumentationAndConfigFiles() {
        assertThat(service.isSupportedSourceLanguage("Markdown")).isTrue();
        assertThat(service.isSupportedSourceLanguage("YAML")).isTrue();
        assertThat(service.isSupportedSourceLanguage("JSON")).isTrue();
    }
}
