package com.xinyue.atelier.dto;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;

import java.util.List;
import java.util.UUID;

public record FolderDto(
        UUID id,
        String folderName,
        String imagePath,
        PatternOrigin origin,
        Level level,
        GarmentType garmentType,
        List<FolderDto> children
) {}
