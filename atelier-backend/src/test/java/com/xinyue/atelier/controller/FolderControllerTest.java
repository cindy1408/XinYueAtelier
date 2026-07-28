package com.xinyue.atelier.controller;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.service.FolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    // ---------- listFolders ----------

    @Test
    void listFolders_returnsListFromService() {
        FolderDto dto1 = new FolderDto(
                UUID.randomUUID(), 1, "Folder One", null,
                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList()
        );
        FolderDto dto2 = new FolderDto(
                UUID.randomUUID(), 2, "Folder Two", null,
                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList()
        );

        when(folderService.listRootFolders()).thenReturn(List.of(dto1, dto2));

        List<FolderDto> result = folderController.listFolders();

        assertThat(result).containsExactly(dto1, dto2);
        verify(folderService).listRootFolders();
    }

    @Test
    void listFolders_returnsEmptyListWhenNoneExist() {
        when(folderService.listRootFolders()).thenReturn(List.of());

        List<FolderDto> result = folderController.listFolders();

        assertThat(result).isEmpty();
        verify(folderService).listRootFolders();
    }

    // ---------- createRootFolder ----------

    @Test
    void createRootFolder_returnsOkWithCreatedFolder() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.png", "image/png", "fake-image-bytes".getBytes()
        );

        FolderDto expectedDto = new FolderDto(
                UUID.randomUUID(), 1, "My Folder", "folders/x/image.png",
                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList()
        );

        when(folderService.createFolder(
                eq(1), eq("My Folder"), eq("COURSE"), eq("DRAFTED"), eq("BEGINNER"), eq(image), isNull()
        )).thenReturn(expectedDto);

        ResponseEntity<FolderDto> response = folderController.createRootFolder(
                1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedDto);
    }

    @Test
    void createRootFolder_passesNullParentIdToService() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.png", "image/png", "fake-image-bytes".getBytes()
        );

        FolderDto dto = new FolderDto(
                UUID.randomUUID(), 1, "My Folder", null,
                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList()
        );

        when(folderService.createFolder(any(), any(), any(), any(), any(), any(), isNull()))
                .thenReturn(dto);

        folderController.createRootFolder(1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image);

        ArgumentCaptor<UUID> parentIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(folderService).createFolder(
                any(), any(), any(), any(), any(), any(), parentIdCaptor.capture()
        );
        assertThat(parentIdCaptor.getValue()).isNull();
    }

    @Test
    void createRootFolder_passesAllParamsThroughToService() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.png", "image/png", "fake-image-bytes".getBytes()
        );
        FolderDto dto = new FolderDto(
                UUID.randomUUID(), 5, "Blazer", null,
                PatternOrigin.DRAFTED, Level.INTERMEDIATE, GarmentType.BLAZER, Collections.emptyList()
        );

        when(folderService.createFolder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(dto);

        folderController.createRootFolder(5, "Blazer", "BLAZER", "DRAFTED", "INTERMEDIATE", image);

        verify(folderService).createFolder(
                eq(5), eq("Blazer"), eq("BLAZER"), eq("DRAFTED"), eq("INTERMEDIATE"), eq(image), isNull()
        );
    }
}