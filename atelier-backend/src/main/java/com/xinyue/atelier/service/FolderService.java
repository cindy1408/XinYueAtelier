package com.xinyue.atelier.service;

import com.xinyue.atelier.exceptions.FileProcessingException;
import com.xinyue.atelier.exceptions.ResourceNotFoundException;
import com.xinyue.atelier.model.GarmentType;
import com.xinyue.atelier.model.Level;
import com.xinyue.atelier.model.PatternOrigin;
import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.model.mapper.FolderMapper;
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
import java.nio.file.FileSystemAlreadyExistsException;
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

    public FolderDto getFolderById(UUID id) throws ResourceNotFoundException {
        return folderRepo.findById(id)
                .map(folderMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", id));
    }

    @Transactional
    public FolderDto createFolder(
            Integer ref,
            String title,
            Enum garmentType,
            String origin,
            String level,
            MultipartFile image,
            UUID parentId) throws FileProcessingException {
        Folder folder = new Folder();

        if (parentId != null) {
            Folder parent = folderRepo.findById(parentId)
                    .orElseThrow(() -> new FileSystemAlreadyExistsException("Parent folder not found"));
            folder.setParentFolder(parent);
        }

        folder.setRef(ref);
        folder.setFolderName(title);
        // Enum validation happens before any persistence or S3 call, so an
        // invalid value never leaves partial side effects behind.
        folder.setGarmentType((GarmentType) garmentType);
        folder.setOrigin(parseEnum(PatternOrigin.class, origin));
        folder.setLevel(parseEnum(Level.class, level));

        log.debug("Creating folder '{}' with garmentType={}", title, folder.getGarmentType());

        folder = folderRepo.save(folder);

        if (image != null && !image.isEmpty()) {
            try {
                String imageKey = uploadImage(folder, image);
                folder.setImagePath(imageKey);
                folder = folderRepo.save(folder);
            } catch (IOException e) {
                throw new FileSystemAlreadyExistsException("Failed to create folder");
            } catch (RuntimeException e) {
                // Covers unchecked failures from the storage layer (e.g. AWS
                // SDK exceptions), which otherwise propagated to callers
                // unwrapped.
                throw new FileSystemAlreadyExistsException("Failed to create folder");
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
            MultipartFile image) throws ResourceNotFoundException {
        Folder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found", id));

        // Validate all enum values up front, before touching S3, so an
        // invalid value can never leave the folder's image in an
        // inconsistent state (old image deleted / new one uploaded but the
        // DB row never saved).
        GarmentType parsedGarmentType = parseEnum(GarmentType.class, garmentType);
        PatternOrigin parsedOrigin = parseEnum(PatternOrigin.class, origin);
        Level parsedLevel = parseEnum(Level.class, level);

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
            folder.setGarmentType(parsedGarmentType);
            folder.setOrigin(parsedOrigin);
            folder.setLevel(parsedLevel);

            return folderMapper.toDto(folderRepo.save(folder));

        } catch (IOException | RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update folder", e);
        }
    }

    public void deleteFolder(UUID id) throws FileProcessingException {
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