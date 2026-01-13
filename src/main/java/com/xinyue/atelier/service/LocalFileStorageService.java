package com.xinyue.atelier.service;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.respository.FolderRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final FolderRepo folderRepo;

    public LocalFileStorageService(FolderRepo folderRepo) {
        this.folderRepo = folderRepo;
    }

    public static final String UPLOAD_DIR = "data";

    public String save(MultipartFile file, String subDirectory, String title) {
        try {
            Path dirPath = Paths.get(UPLOAD_DIR).resolve(subDirectory);
            Files.createDirectories(dirPath);

            String extension = FilenameUtils.getExtension(file.getOriginalFilename());

            String safeTitle = title.replaceAll("[^a-zA-Z0-9-_ ]", "_");
            String filename = safeTitle + "." + extension;

            Path filePath = dirPath.resolve(filename);

            file.transferTo(filePath);

            return filePath.toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public Folder createDirectory(
            String title,
            String garmentType,
            String origin,
            String level,
            MultipartFile image
    ) {
        try {
            Path uploadPath = Path.of(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path folderPath = uploadPath.resolve(title);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            String cleanFileName = Objects.requireNonNull(image.getOriginalFilename())
                    .replaceAll("\\s+", "-");

            Path imagePath = folderPath.resolve(cleanFileName);

            image.transferTo(imagePath);
            Path relativePath = uploadPath.relativize(imagePath);

            Folder folder = new Folder();
            folder.setFolderName(title);
            folder.setGarmentType(GarmentType.valueOf(garmentType));
            folder.setOrigin(PatternOrigin.valueOf(origin));
            folder.setLevel(Level.valueOf(level));
            folder.setImagePath(relativePath.toString());

            return folderRepo.save(folder);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create folder or save image: " + title, e);
        }
    }
}
