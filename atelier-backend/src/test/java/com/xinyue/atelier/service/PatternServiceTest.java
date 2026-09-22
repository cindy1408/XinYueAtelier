package com.xinyue.atelier.service;

import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.repository.FolderRepo;
import com.xinyue.atelier.repository.PatternRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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

    @Test
    void create_derivesExtensionFromOriginalFilenameNotHardcoded() {
        UUID folderId = UUID.randomUUID();
        Folder folder = new Folder();
        folder.setFolderName("Folder");

        MockMultipartFile pdf = new MockMultipartFile(
                "pdf", "scan.PDF", "application/pdf", "fake-pdf-bytes".getBytes());

        when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
        when(patternRepo.save(any(Pattern.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pattern result = patternService.create("Title", pdf, folderId);

        assertThat(result.getPdfPath()).isEqualTo("patterns/Folder/Title.PDF");
    }

    @Test
    void create_collapsesRepeatedHyphensInSanitizedName() {
        UUID folderId = UUID.randomUUID();
        Folder folder = new Folder();
        folder.setFolderName("Folder");

        MockMultipartFile pdf = new MockMultipartFile(
                "pdf", "pattern.pdf", "application/pdf", "fake-pdf-bytes".getBytes());

        when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
        when(patternRepo.save(any(Pattern.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pattern result = patternService.create("Title   With   Gaps", pdf, folderId);

        assertThat(result.getPdfPath()).isEqualTo("patterns/Folder/Title-With-Gaps.pdf");
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

    // ---------- getFilesByFolder ----------

    @Test
    void getFilesByFolder_returnsPatternsForFolder() {
        UUID folderId = UUID.randomUUID();
        Pattern pattern1 = new Pattern();
        Pattern pattern2 = new Pattern();

        when(patternRepo.findAllByFolderId(folderId)).thenReturn(List.of(pattern1, pattern2));

        List<Pattern> result = patternService.getFilesByFolder(folderId);

        assertThat(result).containsExactly(pattern1, pattern2);
        verify(patternRepo).findAllByFolderId(folderId);
    }

    @Test
    void getFilesByFolder_returnsEmptyListWhenFolderHasNoPatterns() {
        UUID folderId = UUID.randomUUID();
        when(patternRepo.findAllByFolderId(folderId)).thenReturn(List.of());

        List<Pattern> result = patternService.getFilesByFolder(folderId);

        assertThat(result).isEmpty();
    }

// ---------- getPresignedUrlForPattern ----------

    @Test
    void getPresignedUrlForPattern_returnsPresignedUrlWhenPatternExists() {
        UUID patternId = UUID.randomUUID();
        Pattern pattern = new Pattern();
        pattern.setPdfPath("patterns/folder/title.pdf");

        when(patternRepo.findById(patternId)).thenReturn(Optional.of(pattern));
        when(storageService.presign("patterns/folder/title.pdf")).thenReturn("https://example.com/signed-url");

        String result = patternService.getPresignedUrlForPattern(patternId);

        assertThat(result).isEqualTo("https://example.com/signed-url");
        verify(storageService).presign("patterns/folder/title.pdf");
    }

    @Test
    void getPresignedUrlForPattern_throwsNotFoundWhenPatternDoesNotExist() {
        UUID patternId = UUID.randomUUID();
        when(patternRepo.findById(patternId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patternService.getPresignedUrlForPattern(patternId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pattern not found");

        verifyNoInteractions(storageService);
    }


}