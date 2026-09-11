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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatternServiceTest {

    @Mock
    private PatternRepo patternRepo;

    @Mock
    private FolderRepo folderRepo;

    @Mock
    private S3StorageService storageService;

    private PatternService patternService;

    @BeforeEach
    void setUp() {
        patternService = new PatternService(patternRepo, folderRepo, storageService);
    }

    // ---------- create ----------

    @Test
    void create_uploadsPdfAndSavesPatternWithFolder() {
        UUID folderId = UUID.randomUUID();
        Folder folder = new Folder();
        folder.setFolderName("My Folder");

        MockMultipartFile pdf = new MockMultipartFile(
                "pdf", "pattern.pdf", "application/pdf", "fake-pdf-bytes".getBytes());

        when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
        when(patternRepo.save(any(Pattern.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pattern result = patternService.create("My Pattern", pdf, folderId);

        assertThat(result.getTitle()).isEqualTo("My Pattern");
        assertThat(result.getFolder()).isEqualTo(folder);
        assertThat(result.getPdfPath())
                .isEqualTo("patterns/My-Folder/My-Pattern.pdf");
    }

    @Test
    void create_sanitizesFolderNameAndTitleForS3Key() {
        UUID folderId = UUID.randomUUID();
        Folder folder = new Folder();
        folder.setFolderName("My Weird Folder!!");

        MockMultipartFile pdf = new MockMultipartFile(
                "pdf", "pattern.pdf", "application/pdf", "fake-pdf-bytes".getBytes());

        when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
        when(patternRepo.save(any(Pattern.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pattern result = patternService.create("A Title: With Spaces & Symbols!", pdf, folderId);

        verify(storageService).upload(eq("patterns/My-Weird-Folder/A-Title-With-Spaces-Symbols.pdf"), any(),
                eq("application/pdf"));

        verify(patternRepo).save(any(Pattern.class));
    }

    @Test
    void create_usesCorrectBucketAndContentType() {
        UUID folderId = UUID.randomUUID();
        Folder folder = new Folder();
        folder.setFolderName("Folder");

        MockMultipartFile pdf = new MockMultipartFile(
                "pdf", "pattern.pdf", "application/pdf", "fake-pdf-bytes".getBytes());

        when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
        when(patternRepo.save(any(Pattern.class))).thenAnswer(invocation -> invocation.getArgument(0));

        patternService.create("Title", pdf, folderId);

        verify(storageService).upload(anyString(), any(), eq("application/pdf"));
    }

    @Test
    void create_throwsNotFoundWhenFolderDoesNotExist() {
        UUID folderId = UUID.randomUUID();
        MockMultipartFile pdf = new MockMultipartFile(
                "pdf", "pattern.pdf", "application/pdf", "fake-pdf-bytes".getBytes());

        when(folderRepo.findById(folderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patternService.create("Title", pdf, folderId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Folder not found");

        verifyNoInteractions(storageService);
        verify(patternRepo, never()).save(any());
    }

    // ---------- delete ----------

    @Test
    void delete_deletesPatternAndS3ObjectWhenPdfPathExists() {
        UUID patternId = UUID.randomUUID();
        Pattern pattern = new Pattern();
        pattern.setPdfPath("patterns/folder/title.pdf");

        when(patternRepo.findById(patternId)).thenReturn(Optional.of(pattern));

        patternService.delete(patternId);

        verify(storageService).delete("patterns/folder/title.pdf");

        verify(patternRepo).delete(pattern);
    }

    @Test
    void delete_skipsS3DeleteWhenPdfPathIsNull() {
        UUID patternId = UUID.randomUUID();
        Pattern pattern = new Pattern();
        pattern.setPdfPath(null);

        when(patternRepo.findById(patternId)).thenReturn(Optional.of(pattern));

        patternService.delete(patternId);

        verifyNoInteractions(storageService);
        verify(patternRepo).delete(pattern);
    }

    @Test
    void delete_throwsNotFoundWhenPatternDoesNotExist() {
        UUID patternId = UUID.randomUUID();
        when(patternRepo.findById(patternId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patternService.delete(patternId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pattern not found");

        verifyNoInteractions(storageService);
        verify(patternRepo, never()).delete(any());
    }

}