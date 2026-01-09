package com.xinyue.atelier.service;

import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.respository.FolderRepo;
import com.xinyue.atelier.respository.PatternRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class PatternService {

    private final PatternRepo patternRepository;
    private final FileStorageService fileStorage;
    private final FolderRepo folderRepo;

    public PatternService(
            PatternRepo patternRepository,
            FolderRepo folderRepo,
            FileStorageService fileStorage) {
        this.patternRepository = patternRepository;
        this.folderRepo = folderRepo;
        this.fileStorage = fileStorage;
    }

    public Pattern create(
            String title,
            MultipartFile pdf,
            UUID folderId) {

        Folder folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        String pdfPath = fileStorage.save(pdf, folder.getFolderName(), title);
        Pattern pattern = new Pattern();
        pattern.setFolderId(folderId);
        pattern.setTitle(title);
        pattern.setPdfPath(pdfPath);

        return patternRepository.save(pattern);
    }
}
