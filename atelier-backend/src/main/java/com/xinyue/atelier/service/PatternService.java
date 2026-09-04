package com.xinyue.atelier.service;

import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.repository.FolderRepo;
import com.xinyue.atelier.repository.PatternRepo;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class PatternService {

    private final PatternRepo patternRepo;
    private final FolderRepo folderRepo;
    private final S3StorageService storageService;

    public PatternService(PatternRepo patternRepo, FolderRepo folderRepo, S3StorageService storageService) {
        this.patternRepo = patternRepo;
        this.folderRepo = folderRepo;
        this.storageService = storageService;
    }

    public Pattern create(String title, MultipartFile pdf, UUID folderId) {
        Folder folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        try {
            String key = buildPdfKey(folder.getFolderName(), title, pdf);
            storageService.upload(key, pdf.getBytes(), "application/pdf");

            Pattern pattern = new Pattern();
            pattern.setTitle(title);
            pattern.setFolder(folder);
            pattern.setPdfPath(key);

            return patternRepo.save(pattern);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create pattern", e);
        }
    }

    public List<Pattern> getFilesByFolder(UUID folderId) {
        return patternRepo.findAllByFolderId(folderId);
    }

    public void delete(UUID patternId) {
        Pattern pattern = findPatternOrThrow(patternId);

        if (pattern.getPdfPath() != null) {
            storageService.delete(pattern.getPdfPath());
        }

        patternRepo.delete(pattern);
    }

    /**
     * Presigned URL for downloading/previewing a pattern's PDF.
     * Both use cases need the same lookup + presign, so callers (download and
     * preview endpoints) share this rather than each re-fetching the pattern.
     */
    public String getPresignedUrlForPattern(UUID patternId) {
        Pattern pattern = findPatternOrThrow(patternId);
        return storageService.presign(pattern.getPdfPath());
    }

    // --- Private helpers ---

    private Pattern findPatternOrThrow(UUID patternId) {
        return patternRepo.findById(patternId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pattern not found"));
    }

    private String buildPdfKey(String folderName, String title, MultipartFile pdf) {
        String extension = FilenameUtils.getExtension(pdf.getOriginalFilename());
        return "patterns/" + safeName(folderName) + "/" + safeName(title) + "." + extension;
    }

    private String safeName(String input) {
        return input
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-zA-Z0-9-_]", "")
                .replaceAll("-{2,}", "-");
    }
}