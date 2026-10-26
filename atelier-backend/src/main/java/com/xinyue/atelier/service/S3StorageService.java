package com.xinyue.atelier.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Single place that owns S3 upload/delete/presign behavior for anything
 * stored under the app's bucket (pattern PDFs, folder images, etc).
 * Callers deal in keys and bytes; they shouldn't need to know how
 * presigning or bucket config works.
 */
@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);
    private static final Duration DEFAULT_PRESIGN_DURATION = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner, @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    public void upload(String key, byte[] content, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content));
    }

    public void delete(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
    }

    public void deleteAfterCommit(Collection<String> keys) {
        Set<String> uniqueKeys = new LinkedHashSet<>(keys);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteBestEffort(uniqueKeys);
                }
            });
        } else {
            deleteBestEffort(uniqueKeys);
        }
    }

    public void deleteAfterFailedWrite(String key, RuntimeException failure) {
        try {
            delete(key);
        } catch (RuntimeException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
            log.error("Failed to clean up S3 object {} after a failed database write", key, cleanupFailure);
        }
    }

    public String presign(String key) {
        return presign(key, DEFAULT_PRESIGN_DURATION);
    }

    public String presign(String key, Duration duration) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(r -> r.bucket(bucketName).key(key))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private void deleteBestEffort(Collection<String> keys) {
        for (String key : keys) {
            try {
                delete(key);
            } catch (RuntimeException cleanupFailure) {
                log.error("Failed to clean up S3 object {} after the database change committed", key,
                        cleanupFailure);
            }
        }
    }
}