package com.xinyue.atelier.controller;

import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.respository.FolderRepo;
import com.xinyue.atelier.respository.PatternRepo;
import com.xinyue.atelier.service.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/directory")
@CrossOrigin(origins = "http://localhost:5173", methods = { RequestMethod.GET, RequestMethod.POST,
        RequestMethod.OPTIONS })
public class DirectoryController {
    private final FileStorageService fileStorageService;
    private final FolderRepo folderRepo;
    private final PatternRepo patternRepo;

    public DirectoryController(FileStorageService fileStorageService, FolderRepo folderRepo, PatternRepo patternRepo) {
        this.fileStorageService = fileStorageService;
        this.folderRepo = folderRepo;
        this.patternRepo = patternRepo;
    }

    @GetMapping
    public List<String> listDirectories() {
        return folderRepo.findAllFolderNames();
    }

    @GetMapping("/{folderId}/files")
    public List<Pattern> listFiles(@PathVariable UUID folderId) {
        return patternRepo.findByFolderId(folderId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Folder> createDirectory(
            @RequestParam("title") String title,
            @RequestParam("origin") String origin,
            @RequestParam("level") Integer level,
            @RequestParam("image") MultipartFile image
    ) {
        Folder folder = fileStorageService.createDirectory(title, origin, level, image);
        return ResponseEntity.ok(folder);
    }
}
