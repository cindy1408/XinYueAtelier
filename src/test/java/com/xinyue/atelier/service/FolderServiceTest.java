package com.xinyue.atelier.service;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.repository.FolderRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FolderServiceTest {

    private FolderService folderService;
    private FolderRepo folderRepo;
    private FolderMapper folderMapper;

    @BeforeEach
    void setUp() {
        folderRepo = mock(FolderRepo.class);
        folderMapper = mock(FolderMapper.class);

        folderService = new FolderService(folderRepo, folderMapper);
    }

    @Test
    void getFolderById_happyPath() {

        UUID id = UUID.randomUUID();

        Folder folder = new Folder();
        folder.setId(id);
        folder.setFolderName("Folder");
        folder.setOrigin(PatternOrigin.DRAFTED);
        folder.setLevel(Level.BEGINNER);
        folder.setGarmentType(GarmentType.COURSE);


        FolderDto dto = new FolderDto(
                id,
                "Folder",
                null,
                PatternOrigin.DRAFTED,
                Level.BEGINNER,
                GarmentType.COURSE,
                java.util.Collections.emptyList()
        );

        // Mock repository and mapper
        when(folderRepo.findById(id)).thenReturn(Optional.of(folder));
        when(folderMapper.toDto(folder)).thenReturn(dto);

        // Act
        Optional<FolderDto> result = folderService.getFolderById(id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(dto, result.get());
    }

    @Test
    void getFolderById_notFound_returnsEmpty() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Mock repository to return empty
        when(folderRepo.findById(id)).thenReturn(Optional.empty());

        // Act
        Optional<FolderDto> result = folderService.getFolderById(id);

        // Assert
        assertTrue(result.isEmpty());
    }


    @Test
    void createFolder_withValidData_createsFolder() throws Exception {
        // Arrange
        String title = "My Folder";
        String garmentType = "COURSE";
        String origin = "DRAFTED";
        String level = "BEGINNER";

        MockMultipartFile image = new MockMultipartFile(
                "image", "image.png", "image/png", "fake-image".getBytes()
        );

        Folder savedFolder = new Folder();
        savedFolder.setId(UUID.randomUUID());
        savedFolder.setFolderName(title);

        FolderDto dto = new FolderDto(
                savedFolder.getId(),
                savedFolder.getFolderName(),
                null,
                PatternOrigin.DRAFTED,
                Level.BEGINNER,
                GarmentType.COURSE,
                java.util.Collections.emptyList()
        );

        // Mock repository and mapper
        when(folderRepo.save(any(Folder.class))).thenReturn(savedFolder);
        when(folderMapper.toDto(savedFolder)).thenReturn(dto);

        // Act
        FolderDto result = folderService.createFolder(title, garmentType, origin, level, image, null);

        // Assert
        assertNotNull(result);
        assertEquals(title, result.folderName());

        // Verify folder was saved
        ArgumentCaptor<Folder> captor = ArgumentCaptor.forClass(Folder.class);
        verify(folderRepo, times(1)).save(captor.capture());
        assertEquals(title, captor.getValue().getFolderName());
    }

    @Test
    void createFolder_withNonExistentParent_throwsException() {
        // Arrange
        UUID parentId = UUID.randomUUID();
        when(folderRepo.findById(parentId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                folderService.createFolder(
                        "Title", "COURSE", "DRAFTED", "BEGINNER", null, parentId
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Parent folder not found"));
    }
}
