package com.xinyue.atelier.controller;

import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.service.PatternService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/patterns")
@CrossOrigin(origins = "http://localhost:5173", methods = { RequestMethod.GET, RequestMethod.POST,
                RequestMethod.OPTIONS })
public class PatternController {
        private final PatternService patternService;

        public PatternController(PatternService patternService) {
                this.patternService = patternService;
        }

        @PostMapping(value = "/{folderName}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<Pattern> create(
                        @RequestParam MultipartFile patternPdf,
                        @RequestParam String title,
                        @PathVariable String folderName) {
                return ResponseEntity.ok(
                                patternService.create(title, patternPdf, folderName));
        }
}
