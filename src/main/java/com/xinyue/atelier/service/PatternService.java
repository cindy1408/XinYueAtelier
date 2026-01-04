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
            PatternOrigin origin,
            MultipartFile pdf,
            MultipartFile image,
            Integer level,
            String subDirectory) {
        String pdfPath = fileStorage.save(pdf, subDirectory);
        String imagePath = fileStorage.save(image, subDirectory);

        Pattern pattern = new Pattern();
        pattern.setTitle(title);
        pattern.setOrigin(origin);
        pattern.setPdfPath(pdfPath);
        pattern.setImagePath(imagePath);
        pattern.setLevel(level);

        return patternRepository.save(pattern);
    }
}
