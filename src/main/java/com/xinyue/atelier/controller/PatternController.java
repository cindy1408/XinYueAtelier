package com.xinyue.atelier.controller;

import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.repository.PatternRepo;
import com.xinyue.atelier.service.PatternService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static com.xinyue.atelier.service.FolderService.UPLOAD_ROOT;

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

        @DeleteMapping("/{patternId}")
        public void deletePatternById(@PathVariable UUID patternId) {
            patternRepo.deleteById(patternId);
        }

        @GetMapping("/download/{patternId}")
        public ResponseEntity<Resource> downloadPattern(@PathVariable UUID patternId) {

                Pattern pattern = patternRepo.findById(patternId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Pattern not found"
                        ));

                Path filePath = Path.of(UPLOAD_ROOT.toUri()).resolve(pattern.getPdfPath());

                System.out.println("Trying to download: " + filePath.toAbsolutePath());

                if (!Files.exists(filePath)) {
                        throw new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "File not found on disk: " + filePath
                        );
                }

                try {
                        Resource resource = new UrlResource(filePath.toUri());

                        return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_PDF)
                                .header(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        "attachment; filename=\"" + filePath.getFileName() + "\""
                                )
                                .body(resource);

                } catch (MalformedURLException e) {
                        throw new RuntimeException("Invalid file path", e);
                }
        }


        @GetMapping("/preview/{patternId}")
        public ResponseEntity<Resource> previewPattern(@PathVariable UUID patternId) {

                System.out.println("uuid: "+ patternId);
                Pattern pattern = patternRepo.findById(patternId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Pattern not found"
                        ));

//                Path filePath = Path.of(UPLOAD_ROOT.toUri()).resolve(pattern.getPdfPath());
                Path filePath = Path.of(pattern.getPdfPath());

                if (!filePath.isAbsolute()) {
                        filePath = UPLOAD_ROOT.resolve(filePath);
                }

                if (!Files.exists(filePath)) {
                        throw new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "File not found on disk"
                        );
                }

                try {
                        Resource resource = new UrlResource(filePath.toUri());

                        return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_PDF)
                                .header(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        "inline; filename=\"" + filePath.getFileName() + "\""
                                )
                                .body(resource);

                } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                }
        }

}
