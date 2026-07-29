package com.xinyue.atelier.service;

import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.repository.FolderRepo;
import com.xinyue.atelier.repository.PatternRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatternServiceTest {

    private static final String BUCKET_NAME = "test-bucket";

    @Mock
    private S3Client s3Client;

    @Mock
    private PatternRepo patternRepo;

    @Mock
    private FolderRepo folderRepo;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedGetObjectRequest presignedRequest;

    private PatternService patternService;

    @BeforeEach
    void setUp() {
        patternService = new PatternService(s3Client, patternRepo, folderRepo, s3Presigner);
        ReflectionTestUtils.setField(patternService, "bucketName", BUCKET_NAME);
    }

    // ---------- create ----------

    @Test
    void create_uploadsPdfAndSavesPatternWithFolder() {
        UUID folderId = UUID.randomUUID();
        Folder folder = new Folder();
        folder.setFolderName("My Folder");

        MockMultipartFile pdf = new MockMultipartFile(
                "pdf", "pattern.pdf", "application/pdf", "fake-pdf-bytes".getBytes()
        );

        when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
        when(patternRepo.save(any(Pattern.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pattern result = patternService.create("My Pattern", pdf, folderId);

        assertThat(result.getTitle()).isEqualTo("My Pattern");
        assertThat(result.getFolder()).isEqualTo(folder);
        assertThat(result.getPdfPath())
                .isEqualTo("https://" + BUCKET_NAME + ".s3.eu-west-2.amazonaws.com/patterns/My-Folder/My-Pattern.pdf");
    }

    @Test
    void create_sanitizesFolderNameAndTitleForS3Key() {
        UUID folderId = UUID.randomUUID();
        Folder folder = new Folder();
        folder.setFolderName("My Weird Folder!!");

        MockMultipartFile pdf = new MockMultipartFile(
                "pdf", "pattern.pdf", "application/pdf", "fake-pdf-bytes".getBytes()
        );

        when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
        when(patternRepo.save(any(Pattern.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pattern result = patternService.create("A Title: With Spaces & Symbols!", pdf, folderId);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), (RequestBody) any());

        assertThat(captor.getValue().key()).isEqualTo("patterns/My-Weird-Folder/A-Title-With-Spaces-Symbols.pdf");
    }

    @Test
    void create_usesCorrectBucketAndContentType() {
        UUID folderId = UUID.randomUUID();
        Folder folder = new Folder();
        folder.setFolderName("Folder");

        MockMultipartFile pdf = new MockMultipartFile(
                "pdf", "pattern.pdf", "application/pdf", "fake-pdf-bytes".getBytes()
        );

        when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
        when(patternRepo.save(any(Pattern.class))).thenAnswer(invocation -> invocation.getArgument(0));

        patternService.create("Title", pdf, folderId);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), (RequestBody) any());

        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET_NAME);
        assertThat(captor.getValue().contentType()).isEqualTo("application/pdf");
    }

    @Test
    void create_throwsNotFoundWhenFolderDoesNotExist() {
        UUID folderId = UUID.randomUUID();
        MockMultipartFile pdf = new MockMultipartFile(
                "pdf", "pattern.pdf", "application/pdf", "fake-pdf-bytes".getBytes()
        );

        when(folderRepo.findById(folderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patternService.create("Title", pdf, folderId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Folder not found");

        verifyNoInteractions(s3Client);
        verify(patternRepo, never()).save(any());
    }

    // ---------- delete ----------

    @Test
    void delete_deletesPatternAndS3ObjectWhenPdfPathExists() {
        UUID patternId = UUID.randomUUID();
        Pattern pattern = new Pattern();
        pattern.setPdfPath("https://" + BUCKET_NAME + ".s3.eu-west-2.amazonaws.com/patterns/folder/title.pdf");

        when(patternRepo.findById(patternId)).thenReturn(Optional.of(pattern));

        patternService.delete(patternId);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET_NAME);
        assertThat(captor.getValue().key()).isEqualTo("patterns/folder/title.pdf");

        verify(patternRepo).delete(pattern);
    }

    @Test
    void delete_skipsS3DeleteWhenPdfPathIsNull() {
        UUID patternId = UUID.randomUUID();
        Pattern pattern = new Pattern();
        pattern.setPdfPath(null);

        when(patternRepo.findById(patternId)).thenReturn(Optional.of(pattern));

        patternService.delete(patternId);

        verifyNoInteractions(s3Client);
        verify(patternRepo).delete(pattern);
    }

    @Test
    void delete_skipsS3DeleteWhenPdfPathDoesNotMatchExpectedPrefix() {
        UUID patternId = UUID.randomUUID();
        Pattern pattern = new Pattern();
        pattern.setPdfPath("https://some-other-bucket.s3.eu-west-2.amazonaws.com/patterns/folder/title.pdf");

        when(patternRepo.findById(patternId)).thenReturn(Optional.of(pattern));

        patternService.delete(patternId);

        verifyNoInteractions(s3Client);
        verify(patternRepo).delete(pattern);
    }

    @Test
    void delete_throwsNotFoundWhenPatternDoesNotExist() {
        UUID patternId = UUID.randomUUID();
        when(patternRepo.findById(patternId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patternService.delete(patternId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pattern not found");

        verifyNoInteractions(s3Client);
        verify(patternRepo, never()).delete(any());
    }

    // ---------- generatePresignedUrl ----------

    @Test
    void generatePresignedUrl_extractsKeyAndReturnsSignedUrl() throws MalformedURLException {
        String pdfUrl = "https://" + BUCKET_NAME + ".s3.eu-west-2.amazonaws.com/patterns/folder/title.pdf";
        URL signedUrl = new URL("https://" + BUCKET_NAME + ".s3.eu-west-2.amazonaws.com/patterns/folder/title.pdf?signed=true");

        when(presignedRequest.url()).thenReturn(signedUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        String result = patternService.generatePresignedUrl(pdfUrl);

        assertThat(result).isEqualTo(signedUrl.toString());
    }

    @Test
    void generatePresignedUrl_usesCorrectSignatureDuration() throws MalformedURLException {
        String pdfUrl = "https://" + BUCKET_NAME + ".s3.eu-west-2.amazonaws.com/patterns/folder/title.pdf";
        URL signedUrl = new URL("https://example.com/signed");

        when(presignedRequest.url()).thenReturn(signedUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        patternService.generatePresignedUrl(pdfUrl);

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());

        assertThat(captor.getValue().signatureDuration()).isEqualTo(java.time.Duration.ofMinutes(15));
    }
}