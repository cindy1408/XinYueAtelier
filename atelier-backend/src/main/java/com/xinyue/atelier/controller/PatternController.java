package com.xinyue.atelier.controller;

import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.repository.PatternRepo;
import com.xinyue.atelier.service.PatternService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patterns")
public class PatternController {
    private final PatternService patternService;
    private final PatternRepo patternRepo;

    public PatternController(PatternRepo patternRepo, PatternService patternService) {
        this.patternService = patternService;
        this.patternRepo = patternRepo;
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
        return patternRepo.findAllByFolderId(folderId);
    }

    @DeleteMapping("/{patternId}")
    public void deletePattern(@PathVariable UUID patternId) {
        patternService.delete(patternId);
    }

    @GetMapping("/download/{patternId}")
    public ResponseEntity<Void> downloadPattern(@PathVariable UUID patternId) {
        Pattern pattern = patternRepo.findById(patternId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pattern not found"));

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", pattern.getPdfPath())
                .build();
    }

    @GetMapping("/preview/{patternId}")
    public ResponseEntity<Void> previewPattern(@PathVariable UUID patternId) {
        Pattern pattern = patternRepo.findById(patternId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pattern not found"));

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", pattern.getPdfPath())
                .build();
    }
}