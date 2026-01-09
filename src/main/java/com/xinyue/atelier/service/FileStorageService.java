package com.xinyue.atelier.service;

import com.xinyue.atelier.model.Folder;
import org.springframework.web.multipart.MultipartFile;


public interface FileStorageService {
    String save(MultipartFile file, String subDirectory);
    Folder createDirectory(String title, String origin, Integer level, MultipartFile image);
}