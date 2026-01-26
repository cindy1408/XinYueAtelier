package com.xinyue.atelier.service;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class ServiceTest {
    private final PatternService patternService = new PatternService(null, null);
    private static Path TEST_DIR = Paths.get("test-data");

    @BeforeEach
    void setUp() throws IOException {
        if (Files.exists(TEST_DIR)) {
            FileUtils.deleteDirectory(TEST_DIR.toFile());
        }
        Files.createDirectories(TEST_DIR);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(TEST_DIR)) {
            FileUtils.deleteDirectory(TEST_DIR.toFile());
        }
    }

    @Test
    void testSaveFile_createsFileWithSafeName() throws IOException {
        // Arrange
        String originalFileName = "My Pattern File.pdf";
        String title = "Test Pattern 123!";
        byte[] content = "Hello World".getBytes();
        MultipartFile mockFile = new MockMultipartFile("file", originalFileName, "application/pdf", content);

        Path subDir = TEST_DIR.resolve("subfolder");

        // Act
        Path savedPath = patternService.saveFile(mockFile, subDir, title);

        // Assert
        assertTrue(Files.exists(savedPath), "File should exist after saving");

        // Filename should be sanitized
        String expectedFileName = "Test-Pattern-123.pdf";
        assertEquals(expectedFileName, savedPath.getFileName().toString(), "Filename should be sanitized");

        // Content should match
        byte[] savedContent = Files.readAllBytes(savedPath);
        assertArrayEquals(content, savedContent, "File content should match the input");
    }

}
