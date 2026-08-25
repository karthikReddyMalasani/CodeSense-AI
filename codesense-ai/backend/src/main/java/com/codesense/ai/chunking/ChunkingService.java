package com.codesense.ai.chunking;

import com.codesense.ai.vector.RepositoryChunk;
import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.project.model.Project;
import com.codesense.repository.model.Repository;
import com.codesense.repository.model.RepositoryFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI-side code chunking service.
 *
 * Team Member 3 (Karthik) owns this service.
 *
 * IMPORTANT DESIGN BOUNDARY:
 * This service provides a FALLBACK text-based chunker for cases where
 * Team Member 4's parser has not yet been integrated.
 *
 * When Team Member 4's ParsedFileDTO is available, this service will use
 * the parser metadata (class/method/function boundaries) to create semantic chunks.
 *
 * The fallback text chunker is explicitly NOT the final AST parser.
 * It uses sliding-window text chunking with configurable size and overlap.
 */
@Slf4j
@Service
public class ChunkingService {

    @Value("${codesense.ai.ingestion.chunk-size:1000}")
    private int chunkSize;

    @Value("${codesense.ai.ingestion.chunk-overlap:200}")
    private int chunkOverlap;

    /**
     * Create chunks from parsed file metadata (from Team Member 4's parser).
     * This is the PREFERRED path when parser metadata is available.
     */
    public List<RepositoryChunk> chunkFromParserMetadata(
            ParsedFileDTO parsedFile, Repository repository, Project project, RepositoryFile repoFile) {
        List<RepositoryChunk> chunks = new ArrayList<>();

        if (parsedFile.getElements() == null || parsedFile.getElements().isEmpty()) {
            // Fall back to text chunking if no elements
            return chunkByText(parsedFile.getContent(), repository, project, repoFile,
                parsedFile.getFilePath(), parsedFile.getLanguage());
        }

        int chunkIndex = 0;
        for (var element : parsedFile.getElements()) {
            if (element.getContent() == null || element.getContent().isBlank()) continue;

            RepositoryChunk.ChunkType chunkType = mapElementType(element.getType());

            RepositoryChunk chunk = RepositoryChunk.builder()
                .project(project)
                .repository(repository)
                .file(repoFile)
                .filePath(parsedFile.getFilePath())
                .language(parsedFile.getLanguage())
                .symbolName(element.getName())
                .symbolType(element.getType())
                .chunkType(chunkType)
                .chunkIndex(chunkIndex++)
                .startLine(element.getStartLine())
                .endLine(element.getEndLine())
                .content(element.getContent())
                .build();
            chunks.add(chunk);
        }

        log.debug("Parser metadata chunking: {} chunks from {} elements in {}",
            chunks.size(), parsedFile.getElements().size(), parsedFile.getFilePath());
        return chunks;
    }

    /**
     * Fallback text-based chunking using sliding window.
     * Used when Team Member 4's parser is not yet available.
     * This is NOT the final AST parser — it is a temporary fallback.
     */
    public List<RepositoryChunk> chunkByText(
            String content, Repository repository, Project project,
            RepositoryFile repoFile, String filePath, String language) {

        if (content == null || content.isBlank()) return new ArrayList<>();

        List<RepositoryChunk> chunks = new ArrayList<>();
        String[] lines = content.split("\n", -1);
        int totalLines = lines.length;
        int linesPerChunk = estimateLinesPerChunk(content);
        int overlapLines = Math.max(1, (int)(linesPerChunk * 0.2));

        int chunkIndex = 0;
        int startLine = 0;

        while (startLine < totalLines) {
            int endLine = Math.min(startLine + linesPerChunk, totalLines);
            StringBuilder chunkContent = new StringBuilder();

            for (int i = startLine; i < endLine; i++) {
                chunkContent.append(lines[i]).append("\n");
            }

            String chunkText = chunkContent.toString().trim();
            if (!chunkText.isBlank()) {
                RepositoryChunk chunk = RepositoryChunk.builder()
                    .project(project)
                    .repository(repository)
                    .file(repoFile)
                    .filePath(filePath)
                    .language(language)
                    .chunkType(RepositoryChunk.ChunkType.TEXT)
                    .chunkIndex(chunkIndex++)
                    .startLine(startLine + 1)
                    .endLine(endLine)
                    .content(buildChunkWithHeader(filePath, language, startLine + 1, chunkText))
                    .build();
                chunks.add(chunk);
            }

            if (endLine >= totalLines) break;
            startLine = endLine - overlapLines;
        }

        log.debug("Text chunking: {} chunks from {} lines in {}", chunks.size(), totalLines, filePath);
        return chunks;
    }

    private String buildChunkWithHeader(String filePath, String language, int startLine, String content) {
        return String.format("// File: %s | Language: %s | Lines: %d+\n%s",
            filePath, language != null ? language : "Unknown", startLine, content);
    }

    private int estimateLinesPerChunk(String content) {
        int avgLineLength = content.length() / Math.max(1, content.split("\n", -1).length);
        if (avgLineLength == 0) return 50;
        return Math.max(20, chunkSize / avgLineLength);
    }

    private RepositoryChunk.ChunkType mapElementType(String elementType) {
        if (elementType == null) return RepositoryChunk.ChunkType.TEXT;
        return switch (elementType.toUpperCase()) {
            case "CLASS"          -> RepositoryChunk.ChunkType.CLASS;
            case "METHOD"         -> RepositoryChunk.ChunkType.METHOD;
            case "FUNCTION"       -> RepositoryChunk.ChunkType.FUNCTION;
            case "INTERFACE"      -> RepositoryChunk.ChunkType.INTERFACE;
            case "ENUM"           -> RepositoryChunk.ChunkType.ENUM;
            case "MODULE"         -> RepositoryChunk.ChunkType.MODULE;
            case "DOCUMENTATION"  -> RepositoryChunk.ChunkType.DOCUMENTATION;
            default               -> RepositoryChunk.ChunkType.TEXT;
        };
    }
}
