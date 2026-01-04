package com.xinyue.atelier.controller;

import com.xinyue.atelier.service.FileStorageService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static com.xinyue.atelier.service.LocalFileStorageService.UPLOAD_DIR;

@RestController
@RequestMapping("/directory")
@CrossOrigin(origins = "http://localhost:5173", methods = { RequestMethod.GET, RequestMethod.POST,
        RequestMethod.OPTIONS })
public class DirectoryController {
    private final FileStorageService fileStorageService;

    public DirectoryController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public List<String> listDirectories() {
        try {
            Path basePath = Path.of(UPLOAD_DIR);

            if (!Files.exists(basePath)) {
                return List.of(); // no folders yet
            }

            // list only directories, not files
            try (Stream<Path> paths = Files.list(basePath)) {
                return paths
                        .filter(Files::isDirectory)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .toList();
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to list directories", e);
        }
    }

    @GetMapping("/{folderName}/files")
    public List<String> listFiles(@PathVariable String folderName) {
        System.out.println("LIST FILES FOR: " + folderName);
        try {
            Path folderPath = Path.of(UPLOAD_DIR, folderName);

            if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                return List.of();
            }

            try (Stream<Path> paths = Files.list(folderPath)) {
                return paths
                        .filter(Files::isRegularFile)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .toList();
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to list files", e);
        }
    }

    @PostMapping("/{folderName}")
    public String createDirectory(@PathVariable String folderName) {
        Path path = fileStorageService.createDirectory(folderName);
        return "Directory created at: " + path.toAbsolutePath();
    }
}
