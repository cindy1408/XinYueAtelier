package com.xinyue.atelier.dto;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FolderUpdateRequest {

    @NotBlank
    private String folderName;

    @NotNull
    private GarmentType garmentType;

    @NotNull
    private Level level;

    @NotNull
    private PatternOrigin origin;

    /**
     * REQUIRED for optimistic locking
     */
    @NotNull
    private Long version;

    // getters & setters
    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public GarmentType getGarmentType() {
        return garmentType;
    }

    public void setGarmentType(GarmentType garmentType) {
        this.garmentType = garmentType;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public PatternOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(PatternOrigin origin) {
        this.origin = origin;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
