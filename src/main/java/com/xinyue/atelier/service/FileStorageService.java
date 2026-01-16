package com.xinyue.atelier.service;

import com.xinyue.atelier.model.Folder;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;


public interface FileStorageService {
    String save(MultipartFile file, String subDirectory, String title);
    Folder createDirectory(String title, String garmentType, String origin, String level, MultipartFile image, UUID parentId);
    Folder updateDirectory(UUID id, String folderName, String garmentType, String origin, String level,MultipartFile image);
}