package com.xinyue.atelier.service;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.respository.FolderRepo;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@Service
public class FolderService {
    private final FolderRepo folderRepo;

    public FolderService(
            FolderRepo folderRepo) {
        this.folderRepo = folderRepo;
    }

    public static final String UPLOAD_DIR = "data";

    @GetMapping("/{folderId}")
    public Folder getFolder(@PathVariable UUID folderId) {
        return folderRepo.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Folder not found"
                ));
    }

    // save file to folder in directory (locally)
    public String save(MultipartFile file, String subDirectory, String title) {
        try {
            Path dirPath = Paths.get(UPLOAD_DIR).resolve(subDirectory);
            Files.createDirectories(dirPath);

            String extension = FilenameUtils.getExtension(file.getOriginalFilename());

            String safeTitle = title
                    .trim()
                    .replaceAll("\\s+", "-")        // spaces → hyphens
                    .replaceAll("[^a-zA-Z0-9-_]", "");

            String filename = safeTitle + "." + extension;

            Path filePath = dirPath.resolve(filename);
            file.transferTo(filePath);

            return filePath.toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    // createFolder creates folder locally and if successful it's also saved in db
    public Folder createFolder(
            String title,
            String garmentType,
            String origin,
            String level,
            MultipartFile image,
            UUID parentId
    ) {
        try {
            Folder folder = new Folder();
            Path uploadRoot = Path.of(UPLOAD_DIR);
            Files.createDirectories(uploadRoot);

            Path basePath = uploadRoot;

            if (parentId != null) {
                Folder parent = folderRepo.findById(parentId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Parent folder not found"
                        ));
                folder.setParentFolder(parent);

                basePath = uploadRoot.resolve(parent.getFolderName());
            }

            Path newFolderPath = basePath.resolve(title);
            Files.createDirectories(newFolderPath);

            String cleanFileName = Objects.requireNonNull(image.getOriginalFilename())
                    .replaceAll("\\s+", "-");

            Path imagePath = newFolderPath.resolve(cleanFileName);
            image.transferTo(imagePath);

            Path relativePath = uploadRoot.relativize(imagePath);

            folder.setFolderName(title);
            folder.setGarmentType(GarmentType.valueOf(garmentType.trim().toUpperCase()));
            folder.setOrigin(PatternOrigin.valueOf(origin));
            folder.setLevel(Level.valueOf(level));
            folder.setImagePath(relativePath.toString());

            return folderRepo.save(folder);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create folder or save image: " + title, e);
        }
    }

    // updateFolder updates folder locally and db
    public Folder updateFolder(
            UUID id,
            String folderName,
            String garmentType,
            String origin,
            String level,
            MultipartFile image
    ) {
        Folder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        try {
            Path uploadPath = Path.of(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            if (image != null && !image.isEmpty()) {
                Path folderPath = uploadPath.resolve(folderName);
                if (!Files.exists(folderPath)) {
                    Files.createDirectories(folderPath);
                }

                String cleanFileName = Objects.requireNonNull(image.getOriginalFilename())
                        .replaceAll("\\s+", "-");

                Path imagePath = folderPath.resolve(cleanFileName);
                image.transferTo(imagePath);

                Path relativePath = uploadPath.relativize(imagePath);
                folder.setImagePath(relativePath.toString());
            }

            folder.setFolderName(folderName);
            folder.setGarmentType(GarmentType.valueOf(garmentType.trim().toUpperCase()));
            folder.setOrigin(PatternOrigin.valueOf(origin.trim().toUpperCase()));
            folder.setLevel(Level.valueOf(level.trim().toUpperCase()));

            return folderRepo.save(folder);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update folder", e);
        }
    }
}
