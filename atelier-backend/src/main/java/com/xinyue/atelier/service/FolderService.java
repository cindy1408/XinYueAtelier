package com.xinyue.atelier.service;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.repository.FolderRepo;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FolderService {

    private static final Logger log = LoggerFactory.getLogger(FolderService.class);

    private final FolderRepo folderRepo;
    private final FolderMapper folderMapper;
    private final S3StorageService storageService;

    public FolderService(FolderRepo folderRepo, FolderMapper folderMapper, S3StorageService storageService) {
        this.folderRepo = folderRepo;
        this.folderMapper = folderMapper;
        this.storageService = storageService;
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

    @Transactional
    public FolderDto createFolder(
            Integer ref,
            String title,
            String garmentType,
            String origin,
            String level,
            MultipartFile image,
            UUID parentId) {
        Folder folder = new Folder();

        if (parentId != null) {
            Folder parent = folderRepo.findById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found"));
            folder.setParentFolder(parent);
        }

        folder.setRef(ref);
        folder.setFolderName(title);
        folder.setGarmentType(parseEnum(GarmentType.class, garmentType));
        folder.setOrigin(parseEnum(PatternOrigin.class, origin));
        folder.setLevel(parseEnum(Level.class, level));

        log.debug("Creating folder '{}' with garmentType={}", title, folder.getGarmentType());

        folder = folderRepo.save(folder);

        try {
            if (image != null && !image.isEmpty()) {
                String imageKey = uploadImage(folder, image);
                folder.setImagePath(imageKey);
                folder = folderRepo.save(folder);
            }

            return folderMapper.toDto(folder);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create folder", e);
        }
    }

    public FolderDto updateFolder(
            UUID id,
            Integer ref,
            String folderName,
            String garmentType,
            String origin,
            String level,
            MultipartFile image) {
        Folder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        try {
            if (image != null && !image.isEmpty()) {
                if (folder.getImagePath() != null) {
                    storageService.delete(folder.getImagePath());
                }
                String imageKey = uploadImage(folder, image);
                folder.setImagePath(imageKey);
            }

            folder.setRef(ref);
            folder.setFolderName(folderName);
            folder.setGarmentType(parseEnum(GarmentType.class, garmentType));
            folder.setOrigin(parseEnum(PatternOrigin.class, origin));
            folder.setLevel(parseEnum(Level.class, level));

            return folderMapper.toDto(folderRepo.save(folder));

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update folder", e);
        }
    }

    public void deleteFolder(UUID id) {
        Folder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        if (folder.getImagePath() != null) {
            storageService.delete(folder.getImagePath());
        }

        // DB delete cascades to subfolders and patterns via CascadeType.ALL
        folderRepo.delete(folder);
    }

    // --- Private helpers ---

    private String uploadImage(Folder folder, MultipartFile image) throws IOException {
        String extension = FilenameUtils.getExtension(image.getOriginalFilename());
        String key = "folders/" + folder.getId() + "/image." + extension;
        storageService.upload(key, image.getBytes(), image.getContentType());
        return key;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid value for " + enumType.getSimpleName());
        }
    }
}