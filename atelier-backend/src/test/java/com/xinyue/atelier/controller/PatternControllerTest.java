package com.xinyue.atelier.controller;

import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.repository.PatternRepo;
import com.xinyue.atelier.service.PatternService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatternControllerTest {

    @Mock
    private PatternService patternService;

    @Mock
    private PatternRepo patternRepo;

    private PatternController patternController;

    @BeforeEach
    void setUp() {
        patternController = new PatternController(patternRepo, patternService);
    }

    // ---------- create ----------

    @Test
    void create_returnsOkWithCreatedPattern() {
        UUID folderId = UUID.randomUUID();
        MockMultipartFile pdf = new MockMultipartFile(
                "patternPdf", "pattern.pdf", "application/pdf", "fake-pdf".getBytes()
        );
        Pattern pattern = new Pattern();
        pattern.setTitle("My Pattern");

        when(patternService.create("My Pattern", pdf, folderId)).thenReturn(pattern);

        ResponseEntity<Pattern> response = patternController.create(pdf, "My Pattern", folderId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pattern);
    }

    @Test
    void create_passesCorrectArgumentsToService() {
        UUID folderId = UUID.randomUUID();
        MockMultipartFile pdf = new MockMultipartFile(
                "patternPdf", "pattern.pdf", "application/pdf", "fake-pdf".getBytes()
        );

        when(patternService.create(any(), any(), any())).thenReturn(new Pattern());

        patternController.create(pdf, "My Pattern", folderId);

        verify(patternService).create(eq("My Pattern"), eq(pdf), eq(folderId));
    }

    // ---------- getFilesByFolderId ----------

    @Test
    void getFilesByFolderId_returnsPatternsForFolder() {
        UUID folderId = UUID.randomUUID();
        Pattern pattern1 = new Pattern();
        Pattern pattern2 = new Pattern();

        when(patternRepo.findAllByFolderId(folderId)).thenReturn(List.of(pattern1, pattern2));

        List<Pattern> result = patternController.getFilesByFolderId(folderId);

        assertThat(result).containsExactly(pattern1, pattern2);
        verify(patternRepo).findAllByFolderId(folderId);
    }

    @Test
    void getFilesByFolderId_returnsEmptyListWhenNoPatterns() {
        UUID folderId = UUID.randomUUID();
        when(patternRepo.findAllByFolderId(folderId)).thenReturn(List.of());

        List<Pattern> result = patternController.getFilesByFolderId(folderId);

        assertThat(result).isEmpty();
    }

    // ---------- deletePattern ----------

    @Test
    void deletePattern_callsServiceDeleteWithCorrectId() {
        UUID patternId = UUID.randomUUID();

        patternController.deletePattern(patternId);

        verify(patternService).delete(patternId);
    }

    @Test
    void deletePattern_propagatesNotFoundFromService() {
        UUID patternId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pattern not found"))
                .when(patternService).delete(patternId);

        assertThatThrownBy(() -> patternController.deletePattern(patternId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pattern not found");
    }

    // ---------- downloadPattern ----------

    @Test
    void downloadPattern_returnsRedirectWithLocationHeader() {
        UUID patternId = UUID.randomUUID();
        Pattern pattern = new Pattern();
        pattern.setPdfPath("patterns/folder/title.pdf");

        String signedUrl = "https://test-bucket.s3.eu-west-2.amazonaws.com/patterns/folder/title.pdf?signed=true";

        when(patternRepo.findById(patternId)).thenReturn(Optional.of(pattern));
        when(patternService.generatePresignedUrl("patterns/folder/title.pdf")).thenReturn(signedUrl);

        ResponseEntity<Void> response = patternController.downloadPattern(patternId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getFirst("Location")).isEqualTo(signedUrl);
    }
    @Test
    void downloadPattern_throwsNotFoundWhenPatternDoesNotExist() {
        UUID patternId = UUID.randomUUID();
        when(patternRepo.findById(patternId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patternController.downloadPattern(patternId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pattern not found");
    }

    // ---------- previewPattern ----------

    @Test
    void previewPattern_returnsPresignedUrlInBody() {
        UUID patternId = UUID.randomUUID();
        Pattern pattern = new Pattern();
        pattern.setPdfPath("https://test-bucket.s3.eu-west-2.amazonaws.com/patterns/folder/title.pdf");

        when(patternRepo.findById(patternId)).thenReturn(Optional.of(pattern));
        when(patternService.generatePresignedUrl(pattern.getPdfPath()))
                .thenReturn("https://test-bucket.s3.eu-west-2.amazonaws.com/patterns/folder/title.pdf?signed=true");

        ResponseEntity<Map<String, String>> response = patternController.previewPattern(patternId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("url", "https://test-bucket.s3.eu-west-2.amazonaws.com/patterns/folder/title.pdf?signed=true");
    }

    @Test
    void previewPattern_throwsNotFoundWhenPatternDoesNotExist() {
        UUID patternId = UUID.randomUUID();
        when(patternRepo.findById(patternId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patternController.previewPattern(patternId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pattern not found");

        verifyNoInteractions(patternService);
    }
}