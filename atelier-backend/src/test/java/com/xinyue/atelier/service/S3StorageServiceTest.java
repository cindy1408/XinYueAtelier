package com.xinyue.atelier.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    private static final String BUCKET_NAME = "test-bucket";

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new S3StorageService(s3Client, s3Presigner, BUCKET_NAME);
    }

    // ---------- upload ----------

    @Test
    void upload_putsObjectWithCorrectBucketKeyAndContentType() {
        byte[] content = "fake-file-bytes".getBytes();

        storageService.upload("folders/abc/image.png", content, "image/png");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo(BUCKET_NAME);
        assertThat(capturedRequest.key()).isEqualTo("folders/abc/image.png");
        assertThat(capturedRequest.contentType()).isEqualTo("image/png");
    }

    @Test
    void upload_propagatesAwsServiceExceptionFromClient() {
        byte[] content = "fake-file-bytes".getBytes();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(AwsServiceException.builder().message("Access Denied").build());

        assertThatThrownBy(() -> storageService.upload("folders/abc/image.png", content, "image/png"))
                .isInstanceOf(AwsServiceException.class)
                .hasMessageContaining("Access Denied");
    }

    @Test
    void upload_propagatesSdkClientExceptionOnNetworkFailure() {
        byte[] content = "fake-file-bytes".getBytes();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("Unable to connect to S3"));

        assertThatThrownBy(() -> storageService.upload("folders/abc/image.png", content, "image/png"))
                .isInstanceOf(SdkClientException.class)
                .hasMessageContaining("Unable to connect to S3");
    }

    // ---------- delete ----------

    @Test
    void delete_deletesObjectWithCorrectBucketAndKey() {
        storageService.delete("patterns/folder/title.pdf");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo(BUCKET_NAME);
        assertThat(capturedRequest.key()).isEqualTo("patterns/folder/title.pdf");
    }

    @Test
    void delete_propagatesNoSuchKeyExceptionFromClient() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("The specified key does not exist").build());

        assertThatThrownBy(() -> storageService.delete("patterns/folder/missing.pdf"))
                .isInstanceOf(NoSuchKeyException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void delete_propagatesSdkClientExceptionOnNetworkFailure() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(SdkClientException.create("Unable to connect to S3"));

        assertThatThrownBy(() -> storageService.delete("patterns/folder/title.pdf"))
                .isInstanceOf(SdkClientException.class);
    }

    // ---------- presign (default duration) ----------

    @Test
    void presign_usesDefaultFifteenMinuteDurationAndReturnsUrl() throws Exception {
        URL fakeUrl = URI.create("https://test-bucket.s3.eu-west-2.amazonaws.com/patterns/folder/title.pdf?signed=true").toURL();

        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedGetObjectRequest);
        when(presignedGetObjectRequest.url()).thenReturn(fakeUrl);

        String result = storageService.presign("patterns/folder/title.pdf");

        assertThat(result).isEqualTo(fakeUrl.toString());

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());

        GetObjectPresignRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.signatureDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(capturedRequest.getObjectRequest().bucket()).isEqualTo(BUCKET_NAME);
        assertThat(capturedRequest.getObjectRequest().key()).isEqualTo("patterns/folder/title.pdf");
    }

    // ---------- presign (custom duration) ----------

    @Test
    void presign_withCustomDuration_usesGivenDuration() throws Exception {
        URL fakeUrl = URI.create("https://test-bucket.s3.eu-west-2.amazonaws.com/patterns/folder/title.pdf?signed=true").toURL();

        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedGetObjectRequest);
        when(presignedGetObjectRequest.url()).thenReturn(fakeUrl);

        String result = storageService.presign("patterns/folder/title.pdf", Duration.ofHours(1));

        assertThat(result).isEqualTo(fakeUrl.toString());

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());

        assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void presign_propagatesExceptionWhenPresignerFails() {
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(SdkClientException.create("Unable to generate presigned URL"));

        assertThatThrownBy(() -> storageService.presign("patterns/folder/title.pdf"))
                .isInstanceOf(SdkClientException.class)
                .hasMessageContaining("Unable to generate presigned URL");
    }
}