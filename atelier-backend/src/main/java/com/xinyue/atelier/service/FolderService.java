package com.xinyue.atelier.service;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.model.Pattern;
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
import java.util.ArrayList;
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
        // Enum validation happens before any persistence or S3 call, so an
        // invalid value never leaves partial side effects behind.
        folder.setGarmentType(parseEnum(GarmentType.class, garmentType));
        folder.setOrigin(parseEnum(PatternOrigin.class, origin));
        folder.setLevel(parseEnum(Level.class, level));

        log.debug("Creating folder '{}' with garmentType={}", title, folder.getGarmentType());

        folder = folderRepo.save(folder);

        if (image != null && !image.isEmpty()) {
            String imageKey = buildImageKey(folder, image);
            try {
                storageService.upload(imageKey, image.getBytes(), image.getContentType());
                folder.setImagePath(imageKey);
                folder = folderRepo.save(folder);
            } catch (IOException e) {
                ResponseStatusException failure =
                        new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create folder", e);
                compensateFailedFolderCreate(folder, imageKey, failure);
                throw failure;
            } catch (RuntimeException e) {
                ResponseStatusException failure =
                        new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create folder", e);
                compensateFailedFolderCreate(folder, imageKey, failure);
                throw failure;
            }
        }

        return folderMapper.toDto(folder);
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

        // Validate all enum values up front, before touching S3, so an
        // invalid value can never leave the folder's image in an
        // inconsistent state (old image deleted / new one uploaded but the
        // DB row never saved).
        GarmentType parsedGarmentType = parseEnum(GarmentType.class, garmentType);
        PatternOrigin parsedOrigin = parseEnum(PatternOrigin.class, origin);
        Level parsedLevel = parseEnum(Level.class, level);
        String oldImagePath = folder.getImagePath();
        String newImagePath = null;
        Folder updatedFolder;

        try {
            if (image != null && !image.isEmpty()) {
                newImagePath = buildImageKey(folder, image);
                storageService.upload(newImagePath, image.getBytes(), image.getContentType());
                folder.setImagePath(newImagePath);
            }

            folder.setRef(ref);
            folder.setFolderName(folderName);
            folder.setGarmentType(parsedGarmentType);
            folder.setOrigin(parsedOrigin);
            folder.setLevel(parsedLevel);

            updatedFolder = folderRepo.save(folder);
        } catch (IOException e) {
            ResponseStatusException failure =
                    new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update folder", e);
            if (newImagePath != null) {
                storageService.deleteAfterFailedWrite(newImagePath, failure);
            }
            throw failure;
        } catch (RuntimeException e) {
            ResponseStatusException failure =
                    new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update folder", e);
            if (newImagePath != null) {
                storageService.deleteAfterFailedWrite(newImagePath, failure);
            }
            throw failure;
        }

        if (newImagePath != null && oldImagePath != null) {
            storageService.deleteAfterCommit(List.of(oldImagePath));
        }
        return folderMapper.toDto(updatedFolder);
    }

    @Transactional
    public void deleteFolder(UUID id) {
        Folder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        List<String> objectKeys = collectObjectKeys(folder);
        folderRepo.delete(folder);
        if (!objectKeys.isEmpty()) {
            storageService.deleteAfterCommit(objectKeys);
        }
    }

    // --- Private helpers ---

    private String buildImageKey(Folder folder, MultipartFile image) {
        String extension = FilenameUtils.getExtension(image.getOriginalFilename());
        return "folders/" + folder.getId() + "/" + UUID.randomUUID() + "." + extension;
    }

    private List<String> collectObjectKeys(Folder folder) {
        List<String> objectKeys = new ArrayList<>();
        if (folder.getImagePath() != null) {
            objectKeys.add(folder.getImagePath());
        }
        for (Pattern pattern : folder.getPatterns()) {
            if (pattern.getPdfPath() != null) {
                objectKeys.add(pattern.getPdfPath());
            }
        }
        for (Folder subFolder : folder.getSubFolders()) {
            objectKeys.addAll(collectObjectKeys(subFolder));
        }
        return objectKeys;
    }

    private void compensateFailedFolderCreate(Folder folder, String imageKey, RuntimeException failure) {
        storageService.deleteAfterFailedWrite(imageKey, failure);
        try {
            folderRepo.delete(folder);
        } catch (RuntimeException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
            log.error("Failed to remove folder {} after a failed create operation", folder.getId(), cleanupFailure);
        }
    }

    public Integer getNextAvailableRef(GarmentType garmentType) {
        boolean course = garmentType == GarmentType.COURSE;
        List<Integer> usedRefs = folderRepo.findAllUsedRefsByGroup(course);

        int nextRef = 1;

        for (Integer ref : usedRefs) {
            if (ref == nextRef) {
                nextRef++;
            } else if (ref > nextRef) {
                break;
            }
        }

        return nextRef;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid value for " + enumType.getSimpleName());
        }
    }
}