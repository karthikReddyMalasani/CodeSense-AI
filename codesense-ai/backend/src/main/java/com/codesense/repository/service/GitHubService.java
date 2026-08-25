package com.codesense.repository.service;

import com.codesense.common.exception.BadRequestException;
import com.codesense.repository.dto.GitHubImportRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles GitHub repository cloning.
 * Uses git CLI via ProcessBuilder for actual cloning.
 */
@Slf4j
@Service
public class GitHubService {

    private static final Pattern GITHUB_URL_PATTERN =
        Pattern.compile("^https://github\\.com/([\\w.-]+)/([\\w.-]+?)(\\.git)?$");

    @Value("${codesense.upload.directory:./uploads}")
    private String uploadDirectory;

    @Value("${codesense.github.access-token:}")
    private String accessToken;

    public record GitHubRepoInfo(String owner, String repo, String cloneUrl) {}

    public GitHubRepoInfo parseGitHubUrl(String url) {
        Matcher m = GITHUB_URL_PATTERN.matcher(url.trim());
        if (!m.matches()) {
            throw new BadRequestException("Invalid GitHub URL: " + url);
        }
        String owner = m.group(1);
        String repo = m.group(2);
        String cloneUrl = url.endsWith(".git") ? url : url + ".git";
        return new GitHubRepoInfo(owner, repo, cloneUrl);
    }

    /**
     * Clones a public or private GitHub repository to a local directory.
     * Returns the path of the cloned repository.
     */
    public Path cloneRepository(GitHubImportRequest request, UUID repositoryId) throws IOException {
        GitHubRepoInfo info = parseGitHubUrl(request.getGithubUrl());
        Path targetDir = Paths.get(uploadDirectory, "repositories", repositoryId.toString());
        
        // Clean target directory if exists
        if (Files.exists(targetDir)) {
            FileUtils.deleteDirectory(targetDir.toFile());
        }
        Files.createDirectories(targetDir);

        String cloneUrl = buildCloneUrl(info.cloneUrl());
        String requestedBranch = request.getBranch() != null ? request.getBranch().trim() : "";

        java.util.List<String> command = new java.util.ArrayList<>();
        command.add("git");
        command.add("clone");
        command.add("--depth");
        command.add("1");

        if (!requestedBranch.isEmpty() && !"HEAD".equalsIgnoreCase(requestedBranch)) {
            command.add("--branch");
            command.add(requestedBranch);
        }

        command.add(cloneUrl);
        command.add(targetDir.toAbsolutePath().toString());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("Git clone failed for URL {}: {}", info.cloneUrl(), output);
                throw new BadRequestException("Failed to clone repository. Check if URL is valid and accessible. Error: " + output);
            }

            log.info("Cloned GitHub repo {} to {}", info.cloneUrl(), targetDir);
            return targetDir;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Clone interrupted", e);
        }
    }

    private String buildCloneUrl(String httpsUrl) {
        if (accessToken != null && !accessToken.isBlank()) {
            // Inject token for private repos
            return httpsUrl.replace("https://", "https://oauth2:" + accessToken + "@");
        }
        return httpsUrl;
    }
}
