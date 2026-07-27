package com.xinyue.atelier.service;

import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.model.Folder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Service
public class FolderMapper {
    private final S3Presigner presigner;
    private final String bucketName;

    public FolderMapper(S3Presigner presigner, @Value("${aws.s3.bucket}") String bucketName) {
        this.presigner = presigner;
        this.bucketName = bucketName;
    }

    public FolderDto toDto(Folder folder) {
        String imageUrl = null;
        if (folder.getImagePath() != null) {
            imageUrl = generatePresignedUrl(folder.getImagePath());
        }

        return new FolderDto(
                folder.getId(),
                folder.getRef(),
                folder.getFolderName(),
                imageUrl,
                folder.getOrigin(),
                folder.getLevel(),
                folder.getGarmentType(),
                folder.getSubFolders().stream()
                        .map(this::toDto)
                        .toList()
        );
    }

    private String generatePresignedUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }
}