package com.xinyue.atelier.service;

import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.model.Folder;
import org.springframework.stereotype.Service;

@Service
public class FolderMapper {
    public FolderDto toDto(Folder folder) {
        return new FolderDto(
                folder.getId(),
                folder.getFolderName(),
                folder.getImagePath(),
                folder.getOrigin(),
                folder.getLevel(),
                folder.getGarmentType(),
                folder.getSubFolders().stream()
                        .map(this::toDto)
                        .toList()
        );
    }
}