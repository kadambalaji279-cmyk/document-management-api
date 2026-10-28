package com.ikaansh.docmanagement.service;

import com.ikaansh.docmanagement.exception.FileStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final Pattern SAFE_EXTENSION = Pattern.compile("\\.[A-Za-z0-9]{1,10}");

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory: " + this.uploadDir, e);
        }
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Cannot store an empty file");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new FileStorageException("File name is missing");
        }

        String cleanedName = StringUtils.cleanPath(originalFileName);
        if (cleanedName.contains("..")) {
            throw new FileStorageException("Invalid file name (path traversal is not allowed): " + originalFileName);
        }

        String safeFileName;
        try {
            // Keep just the last part if a client sends a full path, e.g. C:/fakepath/report.pdf.
            safeFileName = Paths.get(cleanedName).getFileName().toString();
        } catch (InvalidPathException e) {
            throw new FileStorageException("Invalid file name: " + originalFileName, e);
        }

        if (safeFileName.isBlank()) {
            throw new FileStorageException("File name is missing");
        }
        if (safeFileName.length() > 255) {
            throw new FileStorageException("File name is too long (maximum 255 characters)");
        }

        String storedFileName = UUID.randomUUID() + safeExtension(safeFileName);
        Path target = uploadDir.resolve(storedFileName).normalize();

        // Defense in depth: the resolved path must still be inside the upload directory.
        if (!target.startsWith(uploadDir)) {
            throw new FileStorageException("Invalid file path");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + safeFileName, e);
        }

        return new StoredFile(target.toString(), safeFileName);
    }

    public Resource loadAsResource(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new FileStorageException("Document has no uploaded file");
        }

        Path path;
        try {
            path = Paths.get(filePath).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new FileStorageException("Invalid stored file path", e);
        }

        if (!path.startsWith(uploadDir)) {
            throw new FileStorageException("Stored file path is outside the upload directory");
        }
        if (!Files.isRegularFile(path)) {
            throw new FileStorageException("File not found on disk");
        }

        Resource resource = new FileSystemResource(path);
        if (!resource.isReadable()) {
            throw new FileStorageException("File is not readable");
        }
        return resource;
    }

    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            if (!path.startsWith(uploadDir)) {
                log.warn("Refusing to delete a file outside the upload directory: {}", filePath);
                return;
            }

            // Windows can briefly lock a newly written file (for example during an antivirus scan).
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    Files.deleteIfExists(path);
                    return;
                } catch (IOException e) {
                    if (attempt == 3) {
                        // Deleting the database row should not fail just because the file cannot be removed.
                        log.warn("Could not delete file {}: {}", filePath, e.getMessage());
                        return;
                    }
                    Thread.sleep(50);
                }
            }
        } catch (InvalidPathException e) {
            log.warn("Could not delete file {}: {}", filePath, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while deleting file {}", filePath);
        }
    }

    private String safeExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot <= 0) {
            return "";
        }
        String extension = fileName.substring(lastDot).toLowerCase(Locale.ROOT);
        return SAFE_EXTENSION.matcher(extension).matches() ? extension : "";
    }
}
