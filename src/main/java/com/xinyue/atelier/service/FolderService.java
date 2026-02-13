package com.xinyue.atelier.service;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.repository.FolderRepo;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FolderService {
    public static final Path UPLOAD_ROOT = Path.of("data");

    private final FolderRepo folderRepo;
    private final FolderMapper folderMapper;

    public FolderService(
            FolderRepo folderRepo, FolderMapper folderMapper) {
        this.folderRepo = folderRepo;
        this.folderMapper = folderMapper;
    }

    public List<FolderDto> listRootFolders() {
        return folderRepo.findByParentFolderIsNull()
                .stream()
                .map(folderMapper::toDto)
                .toList();
    }

    public List<FolderDto> getFolderChildrenById(UUID parentId) {
        return folderRepo.findByParentFolderId(parentId)
                .stream()
                .map(folderMapper::toDto)
                .toList();
    }

    public Optional<FolderDto> getFolderById(UUID id) {
        return folderRepo.findById(id)
                .map(folderMapper::toDto);
    }


    public FolderDto createFolder(
            String title,
            String garmentType,
            String origin,
            String level,
            MultipartFile image,
            UUID parentId
    ) {
        try {
            Files.createDirectories(UPLOAD_ROOT);
            Folder folder = new Folder();

            if (parentId != null) {
                Folder parent = folderRepo.findById(parentId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Parent folder not found"
                        ));
                folder.setParentFolder(parent);
            }

            String safeFolderName = safeName(title);
            Path folderPath = UPLOAD_ROOT.resolve(safeFolderName);
            Files.createDirectories(folderPath);


            if (image != null && !image.isEmpty()) {
                Path imagePath = saveFile(image, folderPath, title);
                folder.setImagePath(UPLOAD_ROOT.relativize(imagePath).toString());
            }

            folder.setFolderName(title);
            folder.setGarmentType(parseEnum(GarmentType.class, garmentType));
            folder.setOrigin(parseEnum(PatternOrigin.class, origin));
            folder.setLevel(parseEnum(Level.class, level));

            Folder savedFolder = folderRepo.save(folder);
            return folderMapper.toDto(savedFolder);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create folder",
                    e
            );
        }
    }

    // updateFolder updates folder locally and db
    public FolderDto updateFolder(
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
            Files.createDirectories(UPLOAD_ROOT);

            String safeFolderName = safeName(folderName);
            Path folderPath = UPLOAD_ROOT.resolve(safeFolderName);
            Files.createDirectories(folderPath);

            if (image != null && !image.isEmpty()) {
                Path imagePath = saveFile(image, folderPath, folderName);
                folder.setImagePath(UPLOAD_ROOT.relativize(imagePath).toString());
            }

            folder.setFolderName(folderName);
            folder.setGarmentType(parseEnum(GarmentType.class, garmentType));
            folder.setOrigin(parseEnum(PatternOrigin.class, origin));
            folder.setLevel(parseEnum(Level.class, level));

            Folder savedFolder = folderRepo.save(folder);
            return folderMapper.toDto(savedFolder);

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update folder",
                    e
            );
        }
    }


    public void deleteFolder(UUID id) throws IOException {
        Folder folder = folderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        Path folderPath = UPLOAD_ROOT.resolve(folder.getRelativePath());

        // Delete from file system
        if (Files.exists(folderPath)) {
            Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    System.out.println("Deleted file: " + file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    System.out.println("Deleted folder: " + dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        // Delete folder record (and optionally children) from DB
        folderRepo.delete(folder);
    }

    private String safeName(String input) {
        return input
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-zA-Z0-9-_]", "");
    }

    private Path saveFile(MultipartFile file, Path dir, String ignoredTitle) throws IOException {
        Files.createDirectories(dir);

        String originalName = FilenameUtils.getBaseName(file.getOriginalFilename());
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        String safeBase = safeName(originalName);
        String filename = safeBase + "." + extension;

        Path path = dir.resolve(filename);
        file.transferTo(path);

        return path;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid value for " + enumType.getSimpleName()
            );
        }
    }

}
