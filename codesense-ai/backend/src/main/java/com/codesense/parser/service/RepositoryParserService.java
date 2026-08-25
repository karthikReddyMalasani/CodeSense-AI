package com.codesense.parser.service;

import com.codesense.integration.parser.dto.CodeElementDTO;
import com.codesense.integration.parser.dto.CodeRelationshipDTO;
import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.CodeRelationship;
import com.codesense.parser.model.ParsedFile;
import com.codesense.repository.model.Repository;
import com.codesense.repository.model.RepositoryFile;
import com.codesense.repository.repository.RepositoryFileRepository;
import com.codesense.repository.repository.RepositoryRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository-level parsing service.
 * Team Member 4 (Prashanthi) owns this class.
 *
 * Orchestrates parsing for all files in a repository,
 * then returns ParsedRepositoryDTO for consumption by the AI Engine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryParserService {

    private final ParserRouter parserRouter;
    private final RepositoryRepo repositoryRepo;
    private final RepositoryFileRepository repositoryFileRepository;

    /**
     * Parse all files in the repository and return a complete ParsedRepositoryDTO.
     */
    public ParsedRepositoryDTO parseRepository(UUID repositoryId) {
        Repository repo = repositoryRepo.findById(repositoryId)
            .orElseThrow(() -> new RuntimeException("Repository not found: " + repositoryId));

        List<RepositoryFile> files = repositoryFileRepository.findByRepositoryIdAndIgnoredFalse(repositoryId);
        log.info("Parsing {} files for repository: {}", files.size(), repo.getName());

        List<ParsedFileDTO> parsedFiles = new ArrayList<>();
        int totalElements = 0;
        int totalRelationships = 0;
        Set<String> languages = new LinkedHashSet<>();

        for (RepositoryFile file : files) {
            if (file.isBinary() || file.getContent() == null || file.getContent().isBlank()) {
                continue;
            }
            try {
                ParsedFile parsed = parserRouter.parse(
                    file.getFilePath(), file.getContent(), file.getLanguage());

                ParsedFileDTO dto = toDTO(parsed, repositoryId.toString(), repo.getProject().getId().toString());
                parsedFiles.add(dto);
                totalElements += parsed.getElements().size();
                totalRelationships += parsed.getRelationships().size();

                if (file.getLanguage() != null) {
                    languages.add(file.getLanguage());
                }
            } catch (Exception e) {
                log.warn("Failed to parse file {}: {}", file.getFilePath(), e.getMessage());
            }
        }

        log.info("Parsed {} files: {} elements, {} relationships",
            parsedFiles.size(), totalElements, totalRelationships);

        return ParsedRepositoryDTO.builder()
            .repositoryId(repositoryId.toString())
            .projectId(repo.getProject().getId().toString())
            .repositoryName(repo.getName())
            .files(parsedFiles)
            .totalElements(totalElements)
            .totalRelationships(totalRelationships)
            .languages(new ArrayList<>(languages))
            .parserVersion("1.0.0")
            .build();
    }

    /**
     * Parse a single file (for incremental updates).
     */
    public ParsedFileDTO parseSingleFile(UUID repositoryId, UUID fileId) {
        Repository repo = repositoryRepo.findById(repositoryId)
            .orElseThrow(() -> new RuntimeException("Repository not found: " + repositoryId));

        RepositoryFile file = repositoryFileRepository.findByIdAndRepositoryId(fileId, repositoryId)
            .orElseThrow(() -> new RuntimeException("File not found: " + fileId));

        ParsedFile parsed = parserRouter.parse(file.getFilePath(), file.getContent(), file.getLanguage());
        return toDTO(parsed, repositoryId.toString(), repo.getProject().getId().toString());
    }

    // ─── Mapper ──────────────────────────────────────────────────────────────

    private ParsedFileDTO toDTO(ParsedFile parsed, String repositoryId, String projectId) {
        List<CodeElementDTO> elementDTOs = parsed.getElements().stream()
            .map(this::toElementDTO).collect(Collectors.toList());

        List<CodeRelationshipDTO> relDTOs = parsed.getRelationships().stream()
            .map(this::toRelationshipDTO).collect(Collectors.toList());

        return ParsedFileDTO.builder()
            .repositoryId(repositoryId)
            .projectId(projectId)
            .filePath(parsed.getFilePath())
            .language(parsed.getLanguage())
            .content(parsed.getContent())
            .elements(elementDTOs)
            .relationships(relDTOs)
            .metadata(parsed.getMetadata() != null ? parsed.getMetadata() : Map.of())
            .build();
    }

    private CodeElementDTO toElementDTO(CodeElement e) {
        return CodeElementDTO.builder()
            .name(e.getName())
            .type(e.getType() != null ? e.getType().name() : "UNKNOWN")
            .language(e.getLanguage())
            .filePath(e.getFilePath())
            .startLine(e.getStartLine())
            .endLine(e.getEndLine())
            .content(e.getContent())
            .visibility(e.getVisibility())
            .returnType(e.getReturnType())
            .parameters(e.getParameters())
            .annotations(e.getAnnotations())
            .documentation(e.getDocumentation())
            .metadata(e.getMetadata() != null ? new HashMap<>(e.getMetadata()) : null)
            .build();
    }

    private CodeRelationshipDTO toRelationshipDTO(CodeRelationship r) {
        return CodeRelationshipDTO.builder()
            .sourceElement(r.getSourceElement())
            .targetElement(r.getTargetElement())
            .relationshipType(r.getType() != null ? r.getType().name() : "USES")
            .sourceFile(r.getSourceFile())
            .targetFile(r.getTargetFile())
            .sourceLine(r.getSourceLine())
            .build();
    }
}
