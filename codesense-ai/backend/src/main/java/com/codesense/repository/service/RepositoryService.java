package com.codesense.repository.service;

import com.codesense.ai.ingestion.IngestionService;
import com.codesense.auth.model.User;
import com.codesense.auth.repository.UserRepository;
import com.codesense.common.exception.BadRequestException;
import com.codesense.common.exception.ResourceNotFoundException;
import com.codesense.project.model.Project;
import com.codesense.project.service.ProjectService;
import com.codesense.repository.dto.*;
import com.codesense.repository.model.*;
import com.codesense.repository.model.Repository;
import com.codesense.repository.repository.RepositoryFileRepository;
import com.codesense.repository.repository.RepositoryRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.codesense.ai.vector.RepositoryChunkRepository;
import com.codesense.ai.model.DocumentationRepository;
import com.codesense.ai.conversation.ConversationRepository;
import org.springframework.util.FileSystemUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final RepositoryRepo repositoryRepo;
    private final RepositoryFileRepository repositoryFileRepository;
    private final RepositoryChunkRepository repositoryChunkRepository;
    private final DocumentationRepository documentationRepository;
    private final ConversationRepository conversationRepository;
    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final RepositoryStorageService storageService;
    private final GitHubService gitHubService;
    private final LanguageDetectionService languageDetectionService;
    private final IngestionService ingestionService;

    @Transactional
    public RepositoryDto uploadZip(String email, UUID projectId, MultipartFile file, UploadRepositoryRequest request) {
        Project project = projectService.getProjectForUser(email, projectId);

        Repository repository = Repository.builder()
            .project(project)
            .name(request.getName())
            .description(request.getDescription())
            .sourceType(SourceType.ZIP)
            .status(RepositoryStatus.PROCESSING)
            .build();
        repository = repositoryRepo.save(repository);

        extractAndIndexZipAsync(repository.getId(), file);

        return toDto(repository);
    }

    @Transactional
    public RepositoryDto importFromGitHub(String email, UUID projectId, GitHubImportRequest request) {
        Project project = projectService.getProjectForUser(email, projectId);
        GitHubService.GitHubRepoInfo info = gitHubService.parseGitHubUrl(request.getGithubUrl());

        String name = request.getName() != null ? request.getName() : info.repo();

        Repository repository = Repository.builder()
            .project(project)
            .name(name)
            .description(request.getDescription())
            .sourceType(SourceType.GITHUB)
            .githubUrl(request.getGithubUrl())
            .githubOwner(info.owner())
            .githubRepo(info.repo())
            .defaultBranch(request.getBranch())
            .status(RepositoryStatus.PROCESSING)
            .build();
        repository = repositoryRepo.save(repository);

        cloneAndIndexGitHubAsync(repository.getId(), request);

        return toDto(repository);
    }

    public List<RepositoryDto> getRepositories(String email, UUID projectId) {
        projectService.getProjectForUser(email, projectId); // ownership check
        return repositoryRepo.findByProjectIdOrderByCreatedAtDesc(projectId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    // public RepositoryDto getRepository(String email, UUID repositoryId) {
    //     Repository repo = repositoryRepo.findById(repositoryId)
    //         .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId.toString()));
    //     projectService.getProjectForUser(email, repo.getProject().getId()); // ownership
    //     return toDto(repo);
    // }

    public RepositoryDto getRepository(String email, UUID repositoryId) {
    Repository repo = getRepositoryEntityWithOwnerCheck(email, repositoryId);
    return toDto(repo);
}
    public List<RepositoryFileDto> getFiles(String email, UUID repositoryId) {
        Repository repo = getRepositoryEntityWithOwnerCheck(email, repositoryId);
        return repositoryFileRepository.findByRepositoryIdAndIgnoredFalse(repo.getId())
            .stream().map(this::toFileDto).collect(Collectors.toList());
    }

    public RepositoryFileDto getFile(String email, UUID repositoryId, UUID fileId) {
        getRepositoryEntityWithOwnerCheck(email, repositoryId);
        RepositoryFile file = repositoryFileRepository.findByIdAndRepositoryId(fileId, repositoryId)
            .orElseThrow(() -> new ResourceNotFoundException("File", fileId.toString()));
        return toFileDto(file);
    }

    @Transactional
    public void deleteRepository(String email, UUID repositoryId) {
        Repository repo = getRepositoryEntityWithOwnerCheck(email, repositoryId);

        // 1. Delete associated vector chunks
        try {
            repositoryChunkRepository.deleteByRepositoryId(repo.getId());
        } catch (Exception e) {
            log.warn("Could not delete repository chunks for repo {}: {}", repo.getId(), e.getMessage());
        }

        // 2. Delete file metadata
        try {
            repositoryFileRepository.deleteByRepositoryId(repo.getId());
        } catch (Exception e) {
            log.warn("Could not delete repository files for repo {}: {}", repo.getId(), e.getMessage());
        }

        // 3. Delete documentation records
        try {
            documentationRepository.deleteByRepositoryId(repo.getId());
        } catch (Exception e) {
            log.warn("Could not delete documentation for repo {}: {}", repo.getId(), e.getMessage());
        }

        // 4. Delete conversations records
        try {
            conversationRepository.deleteByRepositoryId(repo.getId());
        } catch (Exception e) {
            log.warn("Could not delete conversations for repo {}: {}", repo.getId(), e.getMessage());
        }

        // 5. Delete physical files from disk
        if (repo.getLocalPath() != null) {
            try {
                Path path = Paths.get(repo.getLocalPath());
                if (Files.exists(path)) {
                    FileSystemUtils.deleteRecursively(path);
                }
            } catch (Exception e) {
                log.warn("Failed to delete local files for repo {}: {}", repo.getId(), e.getMessage());
            }
        }

        // 6. Delete repository record
        repositoryRepo.delete(repo);
        log.info("Successfully deleted repository {} from project {}", repositoryId, repo.getProject().getId());
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    @Async("ingestionTaskExecutor")
    public void extractAndIndexZipAsync(UUID repositoryId, MultipartFile file) {
        try {
            Repository repo = repositoryRepo.findById(repositoryId).orElseThrow();
            Path extracted = storageService.extractZip(file, repositoryId);
            repo.setLocalPath(extracted.toAbsolutePath().toString());
            indexRepositoryFiles(repo, extracted);
        } catch (Exception e) {
            log.error("Failed to process ZIP for repository {}: {}", repositoryId, e.getMessage(), e);
            markFailed(repositoryId, e.getMessage());
        }
    }

    @Async("ingestionTaskExecutor")
    public void cloneAndIndexGitHubAsync(UUID repositoryId, GitHubImportRequest request) {
        try {
            Repository repo = repositoryRepo.findById(repositoryId).orElseThrow();
            Path cloned = gitHubService.cloneRepository(request, repositoryId);
            repo.setLocalPath(cloned.toAbsolutePath().toString());
            indexRepositoryFiles(repo, cloned);
        } catch (Exception e) {
            log.error("Failed to clone GitHub repo for repository {}: {}", repositoryId, e.getMessage(), e);
            markFailed(repositoryId, e.getMessage());
        }
    }

    @Async("ingestionTaskExecutor")
    public void processRepositoryAsync(UUID repositoryId) {
        // Placeholder: will be triggered after file indexing completes
    }

    @Transactional
    public void indexRepositoryFiles(Repository repo, Path rootPath) throws IOException {
        List<Path> files = storageService.listFiles(rootPath);
        List<RepositoryFile> repoFiles = new ArrayList<>();
        List<String> languages = new ArrayList<>();

        for (Path filePath : files) {
            try {
                String relativePath = rootPath.relativize(filePath).toString().replace("\\", "/");
                String fileName = filePath.getFileName().toString();

                if (languageDetectionService.isBinaryExtension(fileName)) {
                    repoFiles.add(buildFile(repo, filePath, relativePath, null, true));
                    continue;
                }

                if (Files.size(filePath) > 1024 * 1024) { // skip files > 1MB for content storage
                    String lang = languageDetectionService.detectLanguage(fileName).orElse(null);
                    repoFiles.add(buildFile(repo, filePath, relativePath, lang, false));
                    if (lang != null && !languages.contains(lang)) languages.add(lang);
                    continue;
                }

                String content = Files.readString(filePath, StandardCharsets.UTF_8);
                String lang = languageDetectionService.detectLanguage(fileName).orElse(null);
                String hash = DigestUtils.sha256Hex(content);

                RepositoryFile rf = RepositoryFile.builder()
                    .repository(repo)
                    .project(repo.getProject())
                    .filePath(relativePath)
                    .fileName(fileName)
                    .extension(getExtension(fileName))
                    .language(lang)
                    .sizeBytes(Files.size(filePath))
                    .lineCount(content.split("\n", -1).length)
                    .content(content)
                    .contentHash(hash)
                    .build();
                repoFiles.add(rf);

                if (lang != null && !languages.contains(lang)) {
                    languages.add(lang);
                }
            } catch (Exception e) {
                log.debug("Skipped file {}: {}", filePath, e.getMessage());
            }
        }

        repositoryFileRepository.saveAll(repoFiles);

        // Update repository metadata
        String primaryLanguage = repoFiles.stream()
            .map(RepositoryFile::getLanguage)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(l -> l, Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        repo.setTotalFiles(repoFiles.size());
        repo.setLanguages(languages);
        repo.setPrimaryLanguage(primaryLanguage);
        repo.setStatus(RepositoryStatus.READY);
        repo.setAnalysisStatus(AnalysisStatus.COMPLETED);
        repositoryRepo.save(repo);

        log.info("Indexed {} files for repository {}", repoFiles.size(), repo.getId());

        // Automatically trigger RAG Ingestion (Chunking + Embedding + PGVector persistence)
        try {
            log.info("Triggering background RAG ingestion for repository {}", repo.getId());
            ingestionService.ingestRepositoryAsync(repo.getId());
        } catch (Exception e) {
            log.error("Failed to trigger ingestion for repository {}: {}", repo.getId(), e.getMessage());
        }
    }

    // public Repository getRepositoryEntityWithOwnerCheck(String email, UUID repositoryId) {
    //     Repository repo = repositoryRepo.findById(repositoryId)
    //         .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId.toString()));
    //     projectService.getProjectForUser(email, repo.getProject().getId());
    //     return repo;
    // }

    public Repository getRepositoryEntityWithOwnerCheck(
        String email,
        UUID repositoryId) {

    User user = userRepository.findByEmail(email)
            .orElseThrow(() ->
                    new UsernameNotFoundException(
                            "User not found: " + email
                    ));

    return repositoryRepo.findByIdAndProjectUserId(
            repositoryId,
            user.getId()
    ).orElseThrow(() ->
            new ResourceNotFoundException(
                    "Repository",
                    repositoryId.toString()
            ));
}

    public Repository getRepositoryEntity(UUID repositoryId) {
        return repositoryRepo.findById(repositoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Repository", repositoryId.toString()));
    }

    private void markFailed(UUID repositoryId, String errorMessage) {
        repositoryRepo.findById(repositoryId).ifPresent(repo -> {
            repo.setStatus(RepositoryStatus.FAILED);
            repo.setErrorMessage(errorMessage);
            repositoryRepo.save(repo);
        });
    }

    private RepositoryFile buildFile(Repository repo, Path filePath, String relativePath,
                                      String language, boolean binary) throws IOException {
        return RepositoryFile.builder()
            .repository(repo)
            .project(repo.getProject())
            .filePath(relativePath)
            .fileName(filePath.getFileName().toString())
            .extension(getExtension(filePath.getFileName().toString()))
            .language(language)
            .sizeBytes(Files.size(filePath))
            .binary(binary)
            .build();
    }

    private String getExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        return i >= 0 ? fileName.substring(i) : "";
    }

    private RepositoryDto toDto(Repository repo) {
        if (repo == null) return null;
        return RepositoryDto.builder()
            .id(repo.getId())
            .projectId(repo.getProject() != null ? repo.getProject().getId() : null)
            .name(repo.getName())
            .description(repo.getDescription())
            .sourceType(repo.getSourceType() != null ? repo.getSourceType().name() : "ZIP")
            .githubUrl(repo.getGithubUrl())
            .status(repo.getStatus() != null ? repo.getStatus().name() : "READY")
            .analysisStatus(repo.getAnalysisStatus() != null ? repo.getAnalysisStatus().name() : "PENDING")
            .ingestionStatus(repo.getIngestionStatus() != null ? repo.getIngestionStatus().name() : "PENDING")
            .totalFiles(repo.getTotalFiles())
            .totalChunks(repo.getTotalChunks())
            .languages(repo.getLanguages() != null ? repo.getLanguages() : List.of())
            .primaryLanguage(repo.getPrimaryLanguage() != null ? repo.getPrimaryLanguage() : "Text")
            .errorMessage(repo.getErrorMessage())
            .createdAt(repo.getCreatedAt())
            .updatedAt(repo.getUpdatedAt())
            .build();
    }

    private RepositoryFileDto toFileDto(RepositoryFile f) {
        return RepositoryFileDto.builder()
            .id(f.getId())
            .filePath(f.getFilePath())
            .fileName(f.getFileName())
            .extension(f.getExtension())
            .language(f.getLanguage())
            .sizeBytes(f.getSizeBytes())
            .lineCount(f.getLineCount())
            .content(f.getContent())
            .binary(f.isBinary())
            .build();
    }
}
