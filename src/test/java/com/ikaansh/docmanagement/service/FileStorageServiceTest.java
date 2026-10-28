package com.ikaansh.docmanagement.service;

import com.ikaansh.docmanagement.exception.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void store_shouldSaveFileWithUuidPrefixedName() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.txt",
                "text/plain",
                "hello world".getBytes(StandardCharsets.UTF_8)
        );

        StoredFile storedFile = fileStorageService.store(file);

        Path storedPath = Paths.get(storedFile.getPath());
        assertTrue(Files.exists(storedPath));
        assertTrue(storedPath.startsWith(tempDir));
        assertEquals("report.txt", storedFile.getFileName());
        assertNotEquals("report.txt", storedPath.getFileName().toString());
        assertTrue(storedPath.getFileName().toString().endsWith(".txt"));
        assertEquals("hello world", Files.readString(storedPath));
    }

    @Test
    void store_shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        FileStorageException exception = assertThrows(
                FileStorageException.class,
                () -> fileStorageService.store(file)
        );

        assertEquals("Cannot store an empty file", exception.getMessage());
    }

    @Test
    void store_shouldRejectPathTraversalName() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../evil.txt",
                "text/plain",
                "not allowed".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(FileStorageException.class, () -> fileStorageService.store(file));
        assertFalse(Files.exists(tempDir.getParent().resolve("evil.txt")));
    }

    @Test
    void store_shouldRejectBackslashPathTraversalName() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "..\\evil.txt",
                "text/plain",
                "not allowed".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(FileStorageException.class, () -> fileStorageService.store(file));
    }

    @Test
    void deleteFile_shouldRemoveStoredFile() throws IOException {
        StoredFile storedFile = fileStorageService.store(new MockMultipartFile(
                "file",
                "report.txt",
                "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8)
        ));

        fileStorageService.deleteFile(storedFile.getPath());

        assertFalse(Files.exists(Paths.get(storedFile.getPath())));
    }
}
