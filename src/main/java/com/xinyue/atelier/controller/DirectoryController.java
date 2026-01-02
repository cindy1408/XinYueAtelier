package com.xinyue.atelier.controller;

import com.xinyue.atelier.service.FileStorageService;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RestController
@RequestMapping("/directory")
@CrossOrigin(origins = "http://localhost:5173", methods = { RequestMethod.GET, RequestMethod.POST,
        RequestMethod.OPTIONS })
public class DirectoryController {
    private final FileStorageService fileStorageService;

    public DirectoryController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/{name}")
    public String createDirectory(@PathVariable String name) {
        Path path = fileStorageService.createDirectory(name);
        return "Directory created at: " + path.toAbsolutePath();
    }
}
