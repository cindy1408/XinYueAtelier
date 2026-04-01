package com.xinyue.atelier.service;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.repository.FolderRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FolderService {

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final S3Client s3Client;
    private final FolderRepo folderRepo;
    private final FolderMapper folderMapper;

    public FolderService(S3Client s3Client, FolderRepo folderRepo, FolderMapper folderMapper) {
        this.s3Client = s3Client;
        this.folderRepo = folderRepo;
        this.folderMapper = folderMapper;
    }

    public List<FolderDto> listRootFolders() {
        return folderRepo.findByParentFolderIsNull()
                .stream()
                .map(folderMapper::toDto)
                .toList();
    }

    public List<FolderDto> getFolderChildrenById(UUID parentId) {
        return folderRepo.findByParentFolderId(parentId)
                .stream()
                .map(folderMapper::toDto)
                .toList();
    }

    public Optional<FolderDto> getFolderById(UUID id) {
        return folderRepo.findById(id)
                .map(folderMapper::toDto);
    }

    public FolderDto createFolder(
            String title,
            String garmentType,
            String origin,
            String level,
            MultipartFile image,
            UUID parentId
    ) {
        Folder folder = new Folder();

        if (parentId != null) {
            Folder parent = folderRepo.findById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found"));
            folder.setParentFolder(parent);
        }

        folder.setFolderName(title);
        folder.setGarmentType(parseEnum(GarmentType.class, garmentType));
        folder.setOrigin(parseEnum(PatternOrigin.class, origin));
        folder.setLevel(parseEnum(Level.class, level));

        try {
            if (image != null && !image.isEmpty()) {
                String imageUrl = uploadImageToS3(safeName(title), image);
                folder.setImagePath(imageUrl);
            }

            return folderMapper.toDto(folderRepo.save(folder));

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create folder", e);
        }
    }

    public FolderDto updateFolder(
            UUID id,
            String folderName,
            String garmentType,
            String origin,
            String level,
            MultipartFile image
    ) {
        Folder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        try {
            if (image != null && !image.isEmpty()) {
                // Delete old image from S3 if there is one
                if (folder.getImagePath() != null) {
                    deleteImageFromS3(folder.getImagePath());
                }
                String imageUrl = uploadImageToS3(safeName(folderName), image);
                folder.setImagePath(imageUrl);
            }

            folder.setFolderName(folderName);
            folder.setGarmentType(parseEnum(GarmentType.class, garmentType));
            folder.setOrigin(parseEnum(PatternOrigin.class, origin));
            folder.setLevel(parseEnum(Level.class, level));

            return folderMapper.toDto(folderRepo.save(folder));

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update folder", e);
        }
    }

    public void deleteFolder(UUID id) {
        Folder folder = folderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        // Delete image from S3 if present
        if (folder.getImagePath() != null) {
            deleteImageFromS3(folder.getImagePath());
        }

        // DB delete cascades to subfolders and patterns via CascadeType.ALL
        folderRepo.delete(folder);
    }

    // --- Private helpers ---

    private String uploadImageToS3(String safeFolderName, MultipartFile image) throws IOException {
        String key = "folders/" + safeFolderName + "/" + image.getOriginalFilename();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(image.getContentType())
                        .build(),
                RequestBody.fromBytes(image.getBytes())
        );

        return "https://" + bucketName + ".s3.eu-west-2.amazonaws.com/" + key;
    }

    private void deleteImageFromS3(String imageUrl) {
        // Extract the S3 key from the full URL
        String prefix = "https://" + bucketName + ".s3.eu-west-2.amazonaws.com/";
        if (imageUrl.startsWith(prefix)) {
            String key = imageUrl.substring(prefix.length());
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build()
            );
        }
    }

    private String safeName(String input) {
        return input
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-zA-Z0-9-_]", "");
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid value for " + enumType.getSimpleName()
            );
        }
    }
}