package com.xinyue.atelier.controller;

import com.xinyue.atelier.dto.PatternUploadRequest;
import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.service.PatternService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patterns")
public class PatternController {
    private final PatternService patternService;

    public PatternController(PatternService patternService) {this.patternService = patternService;}

    @PostMapping
    public ResponseEntity<Pattern> create(@RequestBody PatternUploadRequest request) {
        Pattern pattern = new Pattern();
        pattern.setImg(request.img());
        pattern.setTitle(request.title());
        pattern.setOrigin(request.origin());
        pattern.setLocation(request.location());
        pattern.setLevel(request.level());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(patternService.create(pattern));
    }
}
