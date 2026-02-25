package com.xinyue.atelier.controller;

import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.service.FolderService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/folder")
@CrossOrigin(origins = "http://localhost:5173", methods = { RequestMethod.GET, RequestMethod.POST,
        RequestMethod.OPTIONS })
public class FolderController {
    private final FolderService folderService;

    public FolderController( FolderService folderService) {
        this.folderService = folderService;
    }

    @GetMapping
    public List<FolderDto> listFolders() {
        return folderService.listRootFolders();
    }

    @GetMapping("/{parentId}/children")
    public List<FolderDto> getFolderChildren(@PathVariable UUID parentId) {
        return folderService.getFolderChildrenById(parentId);
    }

    @GetMapping("/{folderId}")
    public Optional<FolderDto> getFolderById(@PathVariable UUID folderId) {
        return folderService.getFolderById(folderId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FolderDto> createRootFolder(
            @RequestParam String title,
            @RequestParam String garmentType,
            @RequestParam String origin,
            @RequestParam String level,
            @RequestParam MultipartFile image
    ) {
        return ResponseEntity.ok(
                folderService.createFolder(
                        title, garmentType, origin, level, image, null
                )
        );
    }

    @PostMapping(
            value = "/{parentId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<FolderDto> createChildFolder(
            @PathVariable UUID parentId,
            @RequestParam String title,
            @RequestParam String garmentType,
            @RequestParam String origin,
            @RequestParam String level,
            @RequestParam MultipartFile image
    ) {
        return ResponseEntity.ok(
                folderService.createFolder(
                        title, garmentType, origin, level, image, parentId
                )
        );
    }

    @PutMapping("/{id}")
    public FolderDto updateFolder(
            @PathVariable UUID id,
            @RequestParam String folderName,
            @RequestParam String garmentType,
            @RequestParam String origin,
            @RequestParam String level,
            @RequestParam(required = false) MultipartFile image
    ) {
        return folderService.updateFolder(id, folderName, garmentType, origin, level, image);
    }

    @DeleteMapping("/{id}")
    public void deleteFolder(@PathVariable UUID id) throws IOException {
        folderService.deleteFolder(id);
    }
}
