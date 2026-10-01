package com.xinyue.atelier.service;

import com.xinyue.atelier.exceptions.FileProcessingException;
import com.xinyue.atelier.exceptions.ResourceNotFoundException;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.repository.FolderRepo;
import com.xinyue.atelier.repository.PatternRepo;
import org.apache.commons.io.FilenameUtils;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    public Pattern create(String title, MultipartFile pdf, UUID folderId) throws Exception {
        Folder folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", folderId));

        byte[] bytes;
        try {
            bytes = pdf.getBytes();
        } catch (IOException e) {
            throw new FileProcessingException("Failed to read uploaded PDF", e);
        }

        if (pdf.isEmpty()) {
            throw new BadRequestException("PDF file is empty");
        }
        if (!"application/pdf".equals(pdf.getContentType())) {
            throw new BadRequestException("File must be a PDF");
        }

        String key = buildPdfKey(folder.getFolderName(), title, pdf);
        storageService.upload(key, bytes, "application/pdf");

        Pattern pattern = new Pattern();
        pattern.setTitle(title);
        pattern.setFolder(folder);
        pattern.setPdfPath(key);
        return patternRepo.save(pattern);
    }

    public List<Pattern> getFilesByFolder(UUID folderId) throws ResourceNotFoundException {
        return patternRepo.findAllByFolderId(folderId);
    }

    @Transactional
    public void delete(UUID patternId) throws ResourceNotFoundException {
        Pattern pattern = findPatternOrThrow(patternId);
        patternRepo.delete(pattern);
        if (pattern.getPdfPath() != null) {
            storageService.delete(pattern.getPdfPath());   // StorageException propagates, row delete rolls back
        }
    }

    /**
     * Presigned URL for downloading/previewing a pattern's PDF.
     * Both use cases need the same lookup + presign, so callers (download and
     * preview endpoints) share this rather than each re-fetching the pattern.
     */
    public String getPresignedUrlForPattern(UUID patternId) throws ResourceNotFoundException {
        Pattern pattern = findPatternOrThrow(patternId);
        return storageService.presign(pattern.getPdfPath());
    }

    // --- Private helpers ---

    private Pattern findPatternOrThrow(UUID patternId) throws ResourceNotFoundException {
        return patternRepo.findById(patternId)
                .orElseThrow(() -> new ResourceNotFoundException("Pattern not found", patternId));
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