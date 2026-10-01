package com.xinyue.atelier.controller;

import com.xinyue.atelier.exceptions.FileProcessingException;
import com.xinyue.atelier.exceptions.ResourceNotFoundException;
import com.xinyue.atelier.model.GarmentType;
import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.service.FolderService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/folder")
public class FolderController {
    private final FolderService folderService;

    public FolderController( FolderService folderService) {
        this.folderService = folderService;
    }

    @GetMapping
    public List<FolderDto> listFolders() {
        return folderService.listRootFolders();
    }

//    TODO: Validate request parameters and path variables
   @GetMapping("/{parentId}/children")
   public List<FolderDto> getFolderChildren(@PathVariable UUID parentId) throws ResourceNotFoundException {
       return folderService.getFolderChildrenById(parentId);
   }

    // Controller
    @GetMapping("/{folderId}")
    public FolderDto getFolderById(@PathVariable UUID folderId) throws ResourceNotFoundException {
        return folderService.getFolderById(folderId);
    }

    @GetMapping("/next-ref")
    public Integer getNextAvailableRef(GarmentType garmentType) {
        return folderService.getNextAvailableRef(garmentType);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FolderDto> createRootFolder(
            @RequestParam(required = false) Integer ref,
            @RequestParam String title,
            @RequestParam Enum garmentType,
            @RequestParam String origin,
            @RequestParam String level,
            @RequestParam MultipartFile image
    ) throws FileProcessingException {
        return ResponseEntity.ok(
                folderService.createFolder(
                        ref, title, garmentType, origin, level, image, null
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
           @RequestParam MultipartFile image
   ) throws FileProcessingException {
       return ResponseEntity.ok(
               folderService.createFolder(
                       null, title, null, null, null, image, parentId
               )
       );
   }

   @PutMapping("/{id}")
   public FolderDto updateFolder(
           @PathVariable UUID id,
           @RequestParam Integer ref,
           @RequestParam String folderName,
           @RequestParam String garmentType,
           @RequestParam String origin,
           @RequestParam String level,
           @RequestParam(required = false) MultipartFile image
   ) throws ResourceNotFoundException {
       return folderService.updateFolder(id, ref, folderName, garmentType, origin, level, image);
   }

    @DeleteMapping("/{id}")
    public void deleteFolder(@PathVariable UUID id) throws FileProcessingException {
        folderService.deleteFolder(id);
    }
}
