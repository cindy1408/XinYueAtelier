package com.xinyue.atelier.service;

import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.respository.FolderRepo;
import com.xinyue.atelier.respository.PatternRepo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class FolderService {
    private final FolderRepo folderRepo;

    public FolderService(
            FolderRepo folderRepo) {
        this.folderRepo = folderRepo;
    }

    @GetMapping("/{folderId}")
    public Folder getFolder(@PathVariable UUID folderId) {
        return folderRepo.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Folder not found"
                ));
    }
}
