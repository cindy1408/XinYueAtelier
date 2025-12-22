package com.xinyue.atelier.dto;

import com.xinyue.atelier.PatternOrigin;

public record PatternUploadRequest(
        String title,
        String img,
        String location,
        PatternOrigin origin,
        Integer level
) {
}


