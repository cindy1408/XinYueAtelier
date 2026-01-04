package com.xinyue.atelier.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {
    String save(MultipartFile file, String subDirectory);
    Path createDirectory(String directoryName);
}