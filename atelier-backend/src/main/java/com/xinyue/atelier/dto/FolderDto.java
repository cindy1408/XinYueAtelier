package com.xinyue.atelier.dto;

import com.xinyue.atelier.model.GarmentType;
import com.xinyue.atelier.model.Level;
import com.xinyue.atelier.model.PatternOrigin;

import java.util.List;
import java.util.UUID;

public record FolderDto(
        UUID id,
        Integer ref,
        String folderName,
        String imagePath,
        PatternOrigin origin,
        Level level,
        GarmentType garmentType,
        List<FolderDto> children
) {}
