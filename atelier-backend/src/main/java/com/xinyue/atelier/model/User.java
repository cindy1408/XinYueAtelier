package com.xinyue.atelier.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    private String googleId;       // Google's unique ID for this user ("sub" claim)

    private String role;

    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}