package com.xinyue.atelier.model;

import com.xinyue.atelier.PatternOrigin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
public class Pattern {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String img;

    private String title;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatternOrigin origin;

    private Integer level;
}
