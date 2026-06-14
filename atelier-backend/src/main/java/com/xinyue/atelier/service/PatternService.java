package com.xinyue.atelier.service;

import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.repository.FolderRepo;
import com.xinyue.atelier.repository.PatternRepo;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
public class PatternService {

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final S3Client s3Client;
    private final PatternRepo patternRepo;
    private final FolderRepo folderRepo;
    private final S3Presigner s3Presigner;

    public PatternService(S3Client s3Client, PatternRepo patternRepo, FolderRepo folderRepo, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.patternRepo = patternRepo;
        this.folderRepo = folderRepo;
        this.s3Presigner = s3Presigner;
    }

    public Pattern create(String title, MultipartFile pdf, UUID folderId) {
        Folder folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        try {
            String pdfUrl = uploadPdfToS3(folder.getFolderName(), title, pdf);

            Pattern pattern = new Pattern();
            pattern.setTitle(title);
            pattern.setFolder(folder);
            pattern.setPdfPath(pdfUrl);

            return patternRepo.save(pattern);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create pattern", e);
        }
    }

    public void delete(UUID patternId) {
        Pattern pattern = patternRepo.findById(patternId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pattern not found"));

        if (pattern.getPdfPath() != null) {
            deletePdfFromS3(pattern.getPdfPath());
        }

        patternRepo.delete(pattern);
    }

    // --- Private helpers ---

    private String uploadPdfToS3(String folderName, String title, MultipartFile pdf) throws IOException {
        String extension = FilenameUtils.getExtension(pdf.getOriginalFilename());
        String key = "patterns/" + safeName(folderName) + "/" + safeName(title) + "." + extension;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType("application/pdf")
                        .build(),
                RequestBody.fromBytes(pdf.getBytes())
        );

        return "https://" + bucketName + ".s3.eu-west-2.amazonaws.com/" + key;
    }

    private void deletePdfFromS3(String pdfUrl) {
        String prefix = "https://" + bucketName + ".s3.eu-west-2.amazonaws.com/";
        if (pdfUrl.startsWith(prefix)) {
            String key = pdfUrl.substring(prefix.length());
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

    public String generatePresignedUrl(String pdfUrl) {
        String prefix = "https://" + bucketName + ".s3.eu-west-2.amazonaws.com/";
        String key = pdfUrl.substring(prefix.length());

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(r -> r.bucket(bucketName).key(key))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}