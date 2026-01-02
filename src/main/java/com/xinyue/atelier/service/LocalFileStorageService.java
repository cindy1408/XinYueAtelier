package com.xinyue.atelier.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final String UPLOAD_DIR = "uploads";

    @Override
    public String save(MultipartFile file) {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));

            String filename =
                    UUID.randomUUID() + "_" + file.getOriginalFilename();

            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename);

            Files.copy(file.getInputStream(), filePath);

            return filePath.toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public Path createDirectory(String directoryName) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            Path dirPath = uploadPath.resolve(directoryName);
            return  Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory", e);
        }
    }
}
