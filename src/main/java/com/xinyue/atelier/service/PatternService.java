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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.xinyue.atelier.service.FolderService.UPLOAD_ROOT;

@Service
public class PatternService {
    private final PatternRepo patternRepo;
    private final FolderRepo folderRepo;

    public PatternService(
            PatternRepo patternRepo,
            FolderRepo folderRepo) {
        this.patternRepo = patternRepo;
        this.folderRepo = folderRepo;
    }

    public Pattern create(String title, MultipartFile pdf, UUID folderId) {

        Folder folder = folderRepo.findById(folderId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        try {
            Path folderPath = UPLOAD_ROOT.resolve(folder.getRelativePath());
            Files.createDirectories(folderPath);

            Path pdfPath = saveFile(pdf, folderPath, title);

            Pattern pattern = new Pattern();
            pattern.setTitle(title);
            pattern.setFolder(folder);
            pattern.setPdfPath(UPLOAD_ROOT.relativize(pdfPath).toString());

            return patternRepo.save(pattern);

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create pattern",
                    e
            );
        }
    }

     Path saveFile(MultipartFile file, Path dir, String title) throws IOException {
        Files.createDirectories(dir);

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        String filename = safeName(title) + "." + extension;

        Path path = dir.resolve(filename);
        file.transferTo(path);

        return path;
    }

    private String safeName(String input) {
        return input
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-zA-Z0-9-_]", "");
    }
}
