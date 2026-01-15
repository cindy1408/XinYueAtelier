package com.xinyue.atelier.controller;

import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.respository.FolderRepo;
import com.xinyue.atelier.service.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/directory")
@CrossOrigin(origins = "http://localhost:5173", methods = { RequestMethod.GET, RequestMethod.POST,
        RequestMethod.OPTIONS })
public class DirectoryController {
    private final FileStorageService fileStorageService;
    private final FolderRepo folderRepo;

    public DirectoryController(FileStorageService fileStorageService, FolderRepo folderRepo) {
        this.fileStorageService = fileStorageService;
        this.folderRepo = folderRepo;
    }

    @GetMapping
    public List<Folder> listDirectories() {
        return folderRepo.findAll();
    }

    @GetMapping("/{folderId}")
    public Optional<Folder> getFolderById(@PathVariable UUID folderId) {
        return folderRepo.findById(folderId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Folder> createDirectory(
            @RequestParam("title") String title,
            @RequestParam("garmentType") String garmentType,
            @RequestParam("origin") String origin,
            @RequestParam("level") String level,
            @RequestParam("image") MultipartFile image
    ) {
        Folder folder = fileStorageService.createDirectory(title,garmentType, origin, level, image);
        return ResponseEntity.ok(folder);
    }

    @PutMapping("/{id}")
    public Folder updateFolder(
            @PathVariable UUID id,
            @RequestParam String folderName,
            @RequestParam String garmentType,
            @RequestParam String origin,
            @RequestParam String level,
            @RequestParam(required = false) MultipartFile image
    ) {
        return fileStorageService.updateDirectory(id, folderName, garmentType, origin, level, image);
    }

}
