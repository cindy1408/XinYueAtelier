package com.xinyue.atelier.model;

import com.xinyue.atelier.PatternOrigin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    private Integer level;
}
