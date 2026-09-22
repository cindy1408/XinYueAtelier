package com.xinyue.atelier.controller;

import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.service.FolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderControllerTest {

    @Mock
    private FolderService folderService;

    private FolderController folderController;

    @BeforeEach
    void setUp() {
        folderController = new FolderController(folderService);
    }

    // ---------- getFolderChildren ----------

    @Test
    void getFolderChildren_returnsChildrenForParent() {
        UUID parentId = UUID.randomUUID();
        FolderDto child = new FolderDto(
                UUID.randomUUID(), 1, "Child", null,
                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

        when(folderService.getFolderChildrenById(parentId)).thenReturn(List.of(child));

        List<FolderDto> result = folderController.getFolderChildren(parentId);

        assertThat(result).containsExactly(child);
        verify(folderService).getFolderChildrenById(parentId);
    }

    @Test
    void getFolderChildren_returnsEmptyListWhenNoChildren() {
        UUID parentId = UUID.randomUUID();
        when(folderService.getFolderChildrenById(parentId)).thenReturn(List.of());

        List<FolderDto> result = folderController.getFolderChildren(parentId);

        assertThat(result).isEmpty();
    }

    // ---------- getFolderById ----------

    @Test
    void getFolderById_returnsPresentOptionalWhenFolderExists() {
        UUID folderId = UUID.randomUUID();
        FolderDto dto = new FolderDto(
                folderId, 1, "My Folder", null,
                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

        when(folderService.getFolderById(folderId)).thenReturn(Optional.of(dto));

        Optional<FolderDto> result = folderController.getFolderById(folderId);

        assertThat(result).contains(dto);
    }

    @Test
    void getFolderById_returnsEmptyOptionalWhenFolderDoesNotExist() {
        UUID folderId = UUID.randomUUID();
        when(folderService.getFolderById(folderId)).thenReturn(Optional.empty());

        Optional<FolderDto> result = folderController.getFolderById(folderId);

        // NOTE: returning Optional directly from a controller means Spring
        // serializes Optional.empty() as `null` with an HTTP 200 status,
        // not a 404. A caller can't distinguish "folder not found" from
        // "found a folder with no useful content" without inspecting the
        // body. See note below.
        assertThat(result).isEmpty();
    }

    // ---------- createChildFolder ----------

    @Test
    void createChildFolder_returnsOkWithCreatedFolder() {
        UUID parentId = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.png", "image/png", "fake-image-bytes".getBytes());

        FolderDto dto = new FolderDto(
                UUID.randomUUID(), null, "Child Folder", "folders/x/image.png",
                null, null, null, Collections.emptyList());

        when(folderService.createFolder(isNull(), eq("Child Folder"), isNull(), isNull(), isNull(), eq(image), eq(parentId)))
                .thenReturn(dto);

        ResponseEntity<FolderDto> response = folderController.createChildFolder(parentId, "Child Folder", image);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void createChildFolder_passesNullForRefGarmentTypeOriginAndLevel() {
        UUID parentId = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.png", "image/png", "fake-image-bytes".getBytes());

        when(folderService.createFolder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mock(FolderDto.class));

        folderController.createChildFolder(parentId, "Child Folder", image);

        // Documents the current contract: a child folder created through
        // this endpoint always gets null ref/garmentType/origin/level,
        // regardless of what the caller might want to set. Worth confirming
        // this is intentional and not a missing set of @RequestParams.
        verify(folderService).createFolder(
                isNull(), eq("Child Folder"), isNull(), isNull(), isNull(), eq(image), eq(parentId));
    }

    @Test
    void createChildFolder_propagatesNotFoundWhenParentDoesNotExist() {
        UUID parentId = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.png", "image/png", "fake-image-bytes".getBytes());

        when(folderService.createFolder(any(), any(), any(), any(), any(), any(), eq(parentId)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found"));

        assertThatThrownBy(() -> folderController.createChildFolder(parentId, "Child Folder", image))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Parent folder not found");
    }

    // ---------- updateFolder ----------

    @Test
    void updateFolder_returnsUpdatedFolder() {
        UUID folderId = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile(
                "image", "new.png", "image/png", "fake-image-bytes".getBytes());

        FolderDto dto = new FolderDto(
                folderId, 2, "Updated Folder", "folders/x/new.png",
                PatternOrigin.ACQUIRED, Level.INTERMEDIATE, GarmentType.DRESS, Collections.emptyList());

        when(folderService.updateFolder(folderId, 2, "Updated Folder", "DRESS", "ACQUIRED", "INTERMEDIATE", image))
                .thenReturn(dto);

        FolderDto result = folderController.updateFolder(
                folderId, 2, "Updated Folder", "DRESS", "ACQUIRED", "INTERMEDIATE", image);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void updateFolder_worksWithNullImage() {
        UUID folderId = UUID.randomUUID();
        FolderDto dto = new FolderDto(
                folderId, 1, "My Folder", null,
                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

        when(folderService.updateFolder(folderId, 1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", null))
                .thenReturn(dto);

        FolderDto result = folderController.updateFolder(
                folderId, 1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", null);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void updateFolder_propagatesNotFoundWhenFolderDoesNotExist() {
        UUID folderId = UUID.randomUUID();

        when(folderService.updateFolder(eq(folderId), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        assertThatThrownBy(() -> folderController.updateFolder(
                folderId, 1, "Title", "COURSE", "DRAFTED", "BEGINNER", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Folder not found");
    }

    @Test
    void updateFolder_propagatesBadRequestForInvalidEnum() {
        UUID folderId = UUID.randomUUID();

        when(folderService.updateFolder(eq(folderId), any(), any(), eq("NOT_A_REAL_TYPE"), any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid value for GarmentType"));

        assertThatThrownBy(() -> folderController.updateFolder(
                folderId, 1, "Title", "NOT_A_REAL_TYPE", "DRAFTED", "BEGINNER", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid value for GarmentType");
    }

    // ---------- deleteFolder ----------

    @Test
    void deleteFolder_callsServiceDeleteWithCorrectId() throws Exception {
        UUID folderId = UUID.randomUUID();

        folderController.deleteFolder(folderId);

        verify(folderService).deleteFolder(folderId);
    }

    @Test
    void deleteFolder_propagatesNotFoundFromService() {
        UUID folderId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"))
                .when(folderService).deleteFolder(folderId);

        assertThatThrownBy(() -> folderController.deleteFolder(folderId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Folder not found");
    }
}