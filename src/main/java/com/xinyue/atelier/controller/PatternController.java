package com.xinyue.atelier.controller;

import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.respository.FolderRepo;
import com.xinyue.atelier.respository.PatternRepo;
import com.xinyue.atelier.service.PatternService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patterns")
@CrossOrigin(origins = "http://localhost:5173", methods = { RequestMethod.GET, RequestMethod.POST,
                RequestMethod.OPTIONS })
public class PatternController {
        private final PatternService patternService;
        private final PatternRepo patternRepo;

        public PatternController(PatternRepo patternRepo, PatternService patternService) {
                this.patternService = patternService;
                this.patternRepo = patternRepo;
        }

        @PostMapping(value = "/{folderId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<Pattern> create(
                @RequestParam MultipartFile patternPdf,
                @RequestParam String title,
                @PathVariable UUID folderId) {
                return ResponseEntity.ok(
                        patternService.create(title, patternPdf, folderId));
        }


        @GetMapping("/{folderId}/files")
        public List<Pattern> getFilesFolderById(@PathVariable UUID folderId) {
                return patternRepo.findAllByFolderId(folderId);
        }
}
