package com.codesense.repository.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Detects programming language from file extension.
 * All extension mappings live here — do NOT scatter them throughout the application.
 */
@Service
public class LanguageDetectionService {

    private static final Map<String, String> EXTENSION_TO_LANGUAGE = Map.ofEntries(
        Map.entry(".java",   "Java"),
        Map.entry(".py",     "Python"),
        Map.entry(".js",     "JavaScript"),
        Map.entry(".jsx",    "JavaScript"),
        Map.entry(".ts",     "TypeScript"),
        Map.entry(".tsx",    "TypeScript"),
        Map.entry(".c",      "C"),
        Map.entry(".h",      "C"),
        Map.entry(".cpp",    "C++"),
        Map.entry(".cc",     "C++"),
        Map.entry(".cxx",    "C++"),
        Map.entry(".hpp",    "C++"),
        Map.entry(".cs",     "C#"),
        Map.entry(".go",     "Go"),
        Map.entry(".rs",     "Rust"),
        Map.entry(".php",    "PHP"),
        Map.entry(".rb",     "Ruby"),
        Map.entry(".kt",     "Kotlin"),
        Map.entry(".kts",    "Kotlin"),
        Map.entry(".swift",  "Swift"),
        Map.entry(".scala",  "Scala"),
        Map.entry(".r",      "R"),
        Map.entry(".R",      "R"),
        Map.entry(".sh",     "Shell"),
        Map.entry(".bash",   "Shell"),
        Map.entry(".zsh",    "Shell"),
        Map.entry(".yml",    "YAML"),
        Map.entry(".yaml",   "YAML"),
        Map.entry(".json",   "JSON"),
        Map.entry(".xml",    "XML"),
        Map.entry(".html",   "HTML"),
        Map.entry(".htm",    "HTML"),
        Map.entry(".css",    "CSS"),
        Map.entry(".scss",   "SCSS"),
        Map.entry(".sass",   "SASS"),
        Map.entry(".sql",    "SQL"),
        Map.entry(".md",     "Markdown"),
        Map.entry(".txt",    "Text"),
        Map.entry(".gradle", "Gradle"),
        Map.entry(".tf",     "Terraform"),
        Map.entry(".proto",  "Protobuf")
    );

    public Optional<String> detectLanguage(String fileName) {
        if (fileName == null) return Optional.empty();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) return Optional.empty();
        String extension = fileName.substring(dotIndex).toLowerCase();
        return Optional.ofNullable(EXTENSION_TO_LANGUAGE.get(extension));
    }

    public boolean isSupportedSourceLanguage(String language) {
        return language != null && switch (language) {
            case "Java", "Python", "JavaScript", "TypeScript",
                 "C", "C++", "C#", "Go", "Rust", "PHP",
                 "Ruby", "Kotlin", "Swift" -> true;
            default -> false;
        };
    }

    public boolean isBinaryExtension(String fileName) {
        if (fileName == null) return false;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) return false;
        String ext = fileName.substring(dotIndex).toLowerCase();
        return switch (ext) {
            case ".class", ".jar", ".war", ".ear", ".zip", ".tar", ".gz",
                 ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".pdf",
                 ".bin", ".exe", ".dll", ".so", ".dylib", ".a", ".lib",
                 ".mp3", ".mp4", ".avi", ".mov", ".woff", ".woff2", ".ttf",
                 ".eot", ".dat", ".db", ".sqlite" -> true;
            default -> false;
        };
    }
}
