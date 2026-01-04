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

    public static final String UPLOAD_DIR = "uploads";

    @Override
    public String save(MultipartFile file, String subDirectory) {
        try {
            Path dirPath = Paths.get(UPLOAD_DIR).resolve(subDirectory);
            Files.createDirectories(dirPath);

            Path filePath = dirPath.resolve(file.getOriginalFilename());
            file.transferTo(filePath);

            return filePath.toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public Path createDirectory(String folderName) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);
            Path dirPath = uploadPath.resolve(folderName);
            return  Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory", e);
        }
    }
}
