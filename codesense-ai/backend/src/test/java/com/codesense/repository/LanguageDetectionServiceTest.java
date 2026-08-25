package com.codesense.repository;

import com.codesense.repository.service.LanguageDetectionService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class LanguageDetectionServiceTest {

    private final LanguageDetectionService service = new LanguageDetectionService();

    @ParameterizedTest
    @CsvSource({
        "AuthService.java,       Java",
        "app.py,                 Python",
        "index.js,               JavaScript",
        "App.jsx,                JavaScript",
        "main.ts,                TypeScript",
        "App.tsx,                TypeScript",
        "main.c,                 C",
        "util.cpp,               C++",
        "Program.cs,             C#",
        "main.go,                Go",
        "lib.rs,                 Rust",
        "index.php,              PHP",
        "app.rb,                 Ruby",
        "Main.kt,                Kotlin",
        "App.swift,              Swift"
    })
    void detectLanguage_knownExtensions(String fileName, String expectedLanguage) {
        var result = service.detectLanguage(fileName.trim());
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedLanguage.trim());
    }

    @ParameterizedTest
    @ValueSource(strings = {"file.class", "archive.jar", "image.png", "doc.pdf", "binary.exe"})
    void isBinaryExtension_returnsTrue(String fileName) {
        assertThat(service.isBinaryExtension(fileName)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Main.java", "app.py", "index.js"})
    void isBinaryExtension_sourceFiles_returnsFalse(String fileName) {
        assertThat(service.isBinaryExtension(fileName)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Java", "Python", "JavaScript", "TypeScript", "C", "C++", "Go", "Rust"})
    void isSupportedSourceLanguage_returnsTrue(String language) {
        assertThat(service.isSupportedSourceLanguage(language)).isTrue();
    }
}
