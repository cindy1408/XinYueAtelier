package com.xinyue.atelier.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Folder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String folderName;

    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatternOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;

    @Enumerated(EnumType.STRING)
    private GarmentType garmentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parentFolder;

    @OneToMany(mappedBy = "parentFolder", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Folder> subFolders = new ArrayList<>();

    @JsonIgnore
    public String getRelativePath() {
        if (parentFolder == null) {
            return folderName;
        }
        return parentFolder.getRelativePath() + "/" + folderName;
    }
}
