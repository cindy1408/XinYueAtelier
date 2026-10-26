package com.xinyue.atelier.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.service.FolderService;

class FolderControllerTest {

    private static final MockMultipartFile IMAGE =
            new MockMultipartFile("image", "photo.png", "image/png", "fake-image-bytes".getBytes());

    private final FolderService service = mock(FolderService.class);
    private final FolderController controller = new FolderController(service);

    private static FolderDto folder(UUID id) {
        return new FolderDto(id, 1, "My Folder", null,
                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());
    }

    @Test
    void getFolderChildrenReturnsChildrenOrEmptyList() {
        UUID parentId = UUID.randomUUID();
        UUID emptyParentId = UUID.randomUUID();
        FolderDto child = folder(UUID.randomUUID());
        when(service.getFolderChildrenById(parentId)).thenReturn(List.of(child));
        when(service.getFolderChildrenById(emptyParentId)).thenReturn(List.of());

        assertThat(controller.getFolderChildren(parentId)).containsExactly(child);
        assertThat(controller.getFolderChildren(emptyParentId)).isEmpty();
    }

    /**
     * Returning Optional straight from a controller means Spring serialises Optional.empty()
     * as a null body with HTTP 200, not a 404, so callers can't tell "not found" from "empty".
     */
    @Test
    void getFolderByIdReturnsOptionalAsIs() {
        UUID id = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        FolderDto dto = folder(id);
        when(service.getFolderById(id)).thenReturn(Optional.of(dto));
        when(service.getFolderById(missingId)).thenReturn(Optional.empty());

        assertThat(controller.getFolderById(id)).contains(dto);
        assertThat(controller.getFolderById(missingId)).isEmpty();
    }

    /** Documents the current contract: child folders always get null ref, garmentType, origin and level. */
    @Test
    void createChildFolderReturnsOkAndPassesNullMetadata() {
        UUID parentId = UUID.randomUUID();
        FolderDto dto = new FolderDto(UUID.randomUUID(), null, "Child Folder", "folders/x/image.png",
                null, null, null, Collections.emptyList());
        when(service.createFolder(isNull(), eq("Child Folder"), isNull(), isNull(), isNull(), eq(IMAGE), eq(parentId)))
                .thenReturn(dto);

        ResponseEntity<FolderDto> response = controller.createChildFolder(parentId, "Child Folder", IMAGE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
        verify(service).createFolder(isNull(), eq("Child Folder"), isNull(), isNull(), isNull(), eq(IMAGE), eq(parentId));
    }

    @Test
    void createChildFolderPropagatesNotFoundWhenParentIsMissing() {
        UUID parentId = UUID.randomUUID();
        when(service.createFolder(any(), any(), any(), any(), any(), any(), eq(parentId)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found"));

        assertThatThrownBy(() -> controller.createChildFolder(parentId, "Child Folder", IMAGE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Parent folder not found");
    }

    @Test
    void updateFolderReturnsUpdatedFolderWithOrWithoutImage() {
        UUID id = UUID.randomUUID();
        FolderDto withImage = new FolderDto(id, 2, "Updated Folder", "folders/x/new.png",
                PatternOrigin.ACQUIRED, Level.INTERMEDIATE, GarmentType.DRESS, Collections.emptyList());
        FolderDto withoutImage = folder(id);
        when(service.updateFolder(id, 2, "Updated Folder", "DRESS", "ACQUIRED", "INTERMEDIATE", IMAGE))
                .thenReturn(withImage);
        when(service.updateFolder(id, 1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", null))
                .thenReturn(withoutImage);

        assertThat(controller.updateFolder(id, 2, "Updated Folder", "DRESS", "ACQUIRED", "INTERMEDIATE", IMAGE))
                .isEqualTo(withImage);
        assertThat(controller.updateFolder(id, 1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", null))
                .isEqualTo(withoutImage);
    }

    @Test
    void updateFolderPropagatesServiceErrors() {
        UUID id = UUID.randomUUID();
        for (HttpStatus status : new HttpStatus[] { HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST }) {
            doThrow(new ResponseStatusException(status, "boom")).when(service)
                    .updateFolder(eq(id), any(), any(), any(), any(), any(), any());

            assertThatThrownBy(() -> controller.updateFolder(id, 1, "Title", "NOT_A_REAL_TYPE", "DRAFTED", "BEGINNER", null))
                    .as("should propagate %s", status)
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(status));
        }
    }

    @Test
    void deleteFolderCallsServiceAndPropagatesNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        controller.deleteFolder(id);
        verify(service).deleteFolder(id);

        UUID missingId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"))
                .when(service).deleteFolder(missingId);

        assertThatThrownBy(() -> controller.deleteFolder(missingId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Folder not found");
    }
}