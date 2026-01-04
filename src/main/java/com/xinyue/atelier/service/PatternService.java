package com.xinyue.atelier.service;

import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.respository.PatternRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PatternService {

    private final PatternRepo patternRepository;
    private final FileStorageService fileStorage;

    public PatternService(
            PatternRepo patternRepository,
            FileStorageService fileStorage) {
        this.patternRepository = patternRepository;
        this.fileStorage = fileStorage;
    }

    public Pattern create(
            String title,
            MultipartFile pdf,
            String subDirectory) {
        String pdfPath = fileStorage.save(pdf, subDirectory);

        Pattern pattern = new Pattern();
        pattern.setTitle(title);
        pattern.setPdfPath(pdfPath);

        return patternRepository.save(pattern);
    }
}
