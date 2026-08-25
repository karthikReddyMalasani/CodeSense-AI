package com.codesense.repository.service;

import com.codesense.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Handles ZIP upload, extraction, and local file storage.
 */
@Slf4j
@Service
public class RepositoryStorageService {

    private static final long MAX_ENTRY_SIZE = 100 * 1024 * 1024; // 100MB per entry
    private static final int MAX_ENTRIES = 50_000;

    @Value("${codesense.upload.directory:./uploads}")
    private String uploadDirectory;

    /**
     * Extracts a ZIP file into a unique directory and returns the extraction path.
     */
    public Path extractZip(MultipartFile zipFile, UUID repositoryId) throws IOException {
        validateZip(zipFile);

        Path targetDir = Paths.get(uploadDirectory, "repositories", repositoryId.toString()).toAbsolutePath().normalize();
        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            int entryCount = 0;

            while ((entry = zis.getNextEntry()) != null) {
                if (++entryCount > MAX_ENTRIES) {
                    throw new BadRequestException("ZIP contains too many entries (max " + MAX_ENTRIES + ")");
                }

                String entryName = sanitizeZipEntryName(entry.getName());
                if (entryName == null) {
                    zis.closeEntry();
                    continue;
                }

                Path entryPath = targetDir.resolve(entryName).toAbsolutePath().normalize();
                if (!entryPath.startsWith(targetDir)) {
                    throw new BadRequestException("Zip Slip attempt detected: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    long written = Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    if (written > MAX_ENTRY_SIZE) {
                        Files.deleteIfExists(entryPath);
                        log.warn("Skipped oversized entry: {}", entry.getName());
                    }
                }
                zis.closeEntry();
            }
        }

        log.info("Extracted ZIP to: {}", targetDir);
        return targetDir;
    }

    public Path getRepositoryPath(UUID repositoryId) {
        return Paths.get(uploadDirectory, "repositories", repositoryId.toString()).toAbsolutePath().normalize();
    }

    public void deleteRepository(UUID repositoryId) {
        Path path = getRepositoryPath(repositoryId);
        try {
            FileUtils.deleteDirectory(path.toFile());
            log.info("Deleted repository files: {}", repositoryId);
        } catch (IOException e) {
            log.warn("Failed to delete repository files for {}: {}", repositoryId, e.getMessage());
        }
    }

    /**
     * Walks a repository directory and returns all file paths relative to the root.
     */
    public List<Path> listFiles(Path repositoryRoot) throws IOException {
        List<Path> files = new ArrayList<>();
        if (!Files.exists(repositoryRoot)) return files;

        Files.walkFileTree(repositoryRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
                files.add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs) {
                String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                if (isExcludedDirectory(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return files;
    }

    private boolean isExcludedDirectory(String name) {
        return switch (name) {
            case ".git", "node_modules", "target", "build", "dist",
                 ".idea", ".vscode", "__pycache__", ".pytest_cache",
                 "vendor", ".gradle", "out", "bin" -> true;
            default -> false;
        };
    }

    private void validateZip(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".zip")) {
            throw new BadRequestException("Only ZIP files are supported");
        }
    }

    private String sanitizeZipEntryName(String name) {
        if (name == null || name.isBlank()) return null;
        // Strip leading slashes and resolve ..
        String sanitized = name.replaceAll("^[/\\\\]+", "");
        if (sanitized.contains("..")) return null;
        return sanitized;
    }
}
