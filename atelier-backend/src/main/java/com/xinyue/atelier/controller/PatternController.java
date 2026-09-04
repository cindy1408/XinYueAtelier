package com.xinyue.atelier.controller;

import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.service.PatternService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/patterns")
public class PatternController {
    private final PatternService patternService;

    public PatternController(PatternService patternService) {
        this.patternService = patternService;
    }

    @PostMapping(value = "/{folderId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Pattern> create(
            @RequestParam MultipartFile patternPdf,
            @RequestParam String title,
            @PathVariable UUID folderId) {
        return ResponseEntity.ok(patternService.create(title, patternPdf, folderId));
    }

    @GetMapping("/{folderId}/files")
    public List<Pattern> getFilesByFolderId(@PathVariable UUID folderId) {
        return patternService.getFilesByFolder(folderId);
    }

    @DeleteMapping("/{patternId}")
    public void deletePattern(@PathVariable UUID patternId) {
        patternService.delete(patternId);
    }

    @GetMapping("/download/{patternId}")
    public ResponseEntity<Void> downloadPattern(@PathVariable UUID patternId) {
        String presignedUrl = patternService.getPresignedUrlForPattern(patternId);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", presignedUrl)
                .build();
    }

    @GetMapping("/preview/{patternId}")
    public ResponseEntity<Map<String, String>> previewPattern(@PathVariable UUID patternId) {
        String presignedUrl = patternService.getPresignedUrlForPattern(patternId);
        return ResponseEntity.ok(Map.of("url", presignedUrl));
    }
}