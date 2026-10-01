package com.xinyue.atelier.model.mapper;

import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.service.S3StorageService;
import org.springframework.stereotype.Service;

@Service
public class FolderMapper {
    private final S3StorageService storageService;

    public FolderMapper(S3StorageService storageService) {
        this.storageService = storageService;
    }

    public FolderDto toDto(Folder folder) {
        String imageUrl = null;
        if (folder.getImagePath() != null) {
            imageUrl = storageService.presign(folder.getImagePath());
        }

        return new FolderDto(
                folder.getId(),
                folder.getRef(),
                folder.getFolderName(),
                imageUrl,
                folder.getOrigin(),
                folder.getLevel(),
                folder.getGarmentType(),
                folder.getSubFolders().stream()
                        .map(this::toDto)
                        .toList());
    }
}