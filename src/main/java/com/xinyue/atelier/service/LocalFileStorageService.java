package com.xinyue.atelier.service;

import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.respository.FolderRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final FolderRepo folderRepo;

    public LocalFileStorageService(FolderRepo folderRepo) {
        this.folderRepo = folderRepo;
    }

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

    public Folder createDirectory(
            String folderName,
            String title,
            String origin,
            Integer level,
            MultipartFile image
    ) {
        try {
            Path uploadPath = Path.of(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path folderPath = uploadPath.resolve(folderName);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            String imageFileName = image.getOriginalFilename();
            Path imagePath = folderPath.resolve(imageFileName);

            Folder folder = new Folder();
            folder.setFolderName(folderName);
            folder.setFolderName(title);
            folder.setOrigin(PatternOrigin.valueOf(origin));
            folder.setLevel(level);
            folder.setImagePath(imagePath.toString());

            return folderRepo.save(folder);

        } catch (IOException e) {
            throw new RuntimeException("Failed to create folder or save image: " + folderName, e);
        }
    }
}
