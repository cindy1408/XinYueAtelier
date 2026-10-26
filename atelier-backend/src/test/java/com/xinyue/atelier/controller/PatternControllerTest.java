package com.xinyue.atelier.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.service.PatternService;

class PatternControllerTest {

    private static final MockMultipartFile PDF =
            new MockMultipartFile("patternPdf", "pattern.pdf", "application/pdf", "fake-pdf".getBytes());
    private static final String SIGNED_URL =
            "https://test-bucket.s3.eu-west-2.amazonaws.com/patterns/folder/title.pdf?signed=true";

    private final PatternService service = mock(PatternService.class);
    private final PatternController controller = new PatternController(service);

    @Test
    void createReturnsOkWithCreatedPatternAndPassesArgumentsToService() {
        UUID folderId = UUID.randomUUID();
        Pattern pattern = new Pattern();
        pattern.setTitle("My Pattern");
        when(service.create("My Pattern", PDF, folderId)).thenReturn(pattern);

        ResponseEntity<Pattern> response = controller.create(PDF, "My Pattern", folderId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pattern);
        verify(service).create("My Pattern", PDF, folderId);
    }

    @Test
    void getFilesByFolderIdReturnsPatternsOrEmptyList() {
        UUID folderId = UUID.randomUUID();
        UUID emptyFolderId = UUID.randomUUID();
        Pattern first = new Pattern();
        Pattern second = new Pattern();
        when(service.getFilesByFolder(folderId)).thenReturn(List.of(first, second));
        when(service.getFilesByFolder(emptyFolderId)).thenReturn(List.of());

        assertThat(controller.getFilesByFolderId(folderId)).containsExactly(first, second);
        assertThat(controller.getFilesByFolderId(emptyFolderId)).isEmpty();
    }

    @Test
    void deletePatternCallsServiceAndPropagatesNotFound() {
        UUID id = UUID.randomUUID();
        controller.deletePattern(id);
        verify(service).delete(id);

        UUID missingId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pattern not found"))
                .when(service).delete(missingId);

        assertThatThrownBy(() -> controller.deletePattern(missingId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pattern not found");
    }

    @Test
    void downloadPatternRedirectsToSignedUrl() {
        UUID id = UUID.randomUUID();
        when(service.getPresignedUrlForPattern(id)).thenReturn(SIGNED_URL);

        ResponseEntity<Void> response = controller.downloadPattern(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getFirst("Location")).isEqualTo(SIGNED_URL);
    }

    @Test
    void previewPatternReturnsSignedUrlInBody() {
        UUID id = UUID.randomUUID();
        when(service.getPresignedUrlForPattern(id)).thenReturn(SIGNED_URL);

        ResponseEntity<Map<String, String>> response = controller.previewPattern(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("url", SIGNED_URL);
    }

    @Test
    void downloadAndPreviewPropagateNotFoundWhenPatternIsMissing() {
        UUID id = UUID.randomUUID();
        when(service.getPresignedUrlForPattern(id))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pattern not found"));

        assertThatThrownBy(() -> controller.downloadPattern(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pattern not found");
        assertThatThrownBy(() -> controller.previewPattern(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pattern not found");
    }
}