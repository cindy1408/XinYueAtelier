package com.xinyue.atelier.controller;

import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.service.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value= "/{folderName}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Folder> createDirectory(
            @PathVariable String folderName,
            @RequestParam("title") String title,
            @RequestParam("origin") String origin,
            @RequestParam("level") Integer level,
            @RequestParam("image") MultipartFile image
    ) {
        Folder folder = fileStorageService.createDirectory(folderName, title, origin, level, image);
        return ResponseEntity.ok(folder);
    }
}
