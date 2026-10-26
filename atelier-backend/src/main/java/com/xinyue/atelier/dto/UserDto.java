package com.xinyue.atelier.dto;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String email,
        String name,
        String role,
        LocalDateTime createdAt
) {}
