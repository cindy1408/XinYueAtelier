package com.xinyue.atelier.service;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.repository.FolderRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

        @Mock
        private FolderRepo folderRepo;

        @Mock
        private FolderMapper folderMapper;

        @Mock
        private S3StorageService storageService;

        private FolderService folderService;

        @BeforeEach
        void setUp() {
                folderService = new FolderService(folderRepo, folderMapper, storageService);
        }

        // ---------- listRootFolders ----------

        @Test
        void listRootFolders_returnsMappedDtosForFoldersWithNoParent() {
                Folder folder1 = new Folder();
                Folder folder2 = new Folder();

                FolderDto dto1 = new FolderDto(
                                UUID.randomUUID(), 1, "Folder One", null,
                                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());
                FolderDto dto2 = new FolderDto(
                                UUID.randomUUID(), 2, "Folder Two", null,
                                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

                when(folderRepo.findByParentFolderIsNull()).thenReturn(List.of(folder1, folder2));
                when(folderMapper.toDto(folder1)).thenReturn(dto1);
                when(folderMapper.toDto(folder2)).thenReturn(dto2);

                List<FolderDto> result = folderService.listRootFolders();

                assertThat(result).containsExactly(dto1, dto2);
                verify(folderRepo).findByParentFolderIsNull();
        }

        @Test
        void listRootFolders_returnsEmptyListWhenNoRootFolders() {
                when(folderRepo.findByParentFolderIsNull()).thenReturn(List.of());

                List<FolderDto> result = folderService.listRootFolders();

                assertThat(result).isEmpty();
        }

        // ---------- createFolder ----------

        @Test
        void createFolder_withValidData_createsFolder() {
                Integer ref = 1;
                String title = "My Folder";
                String garmentType = "COURSE";
                String origin = "DRAFTED";
                String level = "BEGINNER";

                MockMultipartFile image = new MockMultipartFile(
                                "image", "image.png", "image/png", "fake-image".getBytes());

                Folder savedFolder = new Folder();
                savedFolder.setId(UUID.randomUUID());
                savedFolder.setFolderName(title);

                FolderDto dto = new FolderDto(
                                savedFolder.getId(),
                                savedFolder.getRef(),
                                savedFolder.getFolderName(),
                                null,
                                PatternOrigin.DRAFTED,
                                Level.BEGINNER,
                                GarmentType.COURSE,
                                Collections.emptyList());

                when(folderRepo.save(any(Folder.class))).thenReturn(savedFolder);
                when(folderMapper.toDto(savedFolder)).thenReturn(dto);

                FolderDto result = folderService.createFolder(ref, title, garmentType, origin, level, image, null);

                assertThat(result).isNotNull();
                assertThat(result.folderName()).isEqualTo(title);

                ArgumentCaptor<Folder> captor = ArgumentCaptor.forClass(Folder.class);
                verify(folderRepo, times(2)).save(captor.capture());
                assertThat(captor.getValue().getFolderName()).isEqualTo(title);
        }

        @Test
        void createFolder_createsRootFolderWithoutImage() {
                Folder savedFolder = new Folder();
                savedFolder.setId(UUID.randomUUID());

                FolderDto expectedDto = new FolderDto(
                                savedFolder.getId(), 1, "My Folder", null,
                                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

                when(folderRepo.save(any(Folder.class))).thenReturn(savedFolder);
                when(folderMapper.toDto(savedFolder)).thenReturn(expectedDto);

                FolderDto result = folderService.createFolder(
                                1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", null, null);

                assertThat(result).isEqualTo(expectedDto);
                verify(folderRepo, never()).findById(any());
                verify(folderRepo, times(1)).save(any(Folder.class));
                verifyNoInteractions(storageService);
        }

        @Test
        void createFolder_setsParentFolderWhenParentIdProvided() {
                UUID parentId = UUID.randomUUID();
                Folder parentFolder = new Folder();
                Folder savedFolder = new Folder();
                savedFolder.setId(UUID.randomUUID());

                FolderDto dto = new FolderDto(
                                savedFolder.getId(), 1, "Child Folder", null,
                                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

                when(folderRepo.findById(parentId)).thenReturn(Optional.of(parentFolder));
                when(folderRepo.save(any(Folder.class))).thenReturn(savedFolder);
                when(folderMapper.toDto(savedFolder)).thenReturn(dto);

                folderService.createFolder(
                                1, "Child Folder", "COURSE", "DRAFTED", "BEGINNER", null, parentId);

                ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);
                verify(folderRepo).save(folderCaptor.capture());
                assertThat(folderCaptor.getValue().getParentFolder()).isEqualTo(parentFolder);
        }

        @Test
        void createFolder_withNonExistentParent_throwsException() {
                UUID parentId = UUID.randomUUID();
                when(folderRepo.findById(parentId)).thenReturn(Optional.empty());

                ResponseStatusException exception = (ResponseStatusException) assertThatThrownBy(() -> folderService
                                .createFolder(1, "Title", "COURSE", "DRAFTED", "BEGINNER", null, parentId))
                                .isInstanceOf(ResponseStatusException.class)
                                .actual();

                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(exception.getReason()).contains("Parent folder not found");
                verify(folderRepo, never()).save(any());
        }

        @Test
        void createFolder_throwsBadRequestForInvalidGarmentType() {
                assertThatThrownBy(() -> folderService.createFolder(
                                1, "My Folder", "NOT_A_REAL_TYPE", "DRAFTED", "BEGINNER", null, null))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Invalid value for GarmentType");
        }

        @Test
        void createFolder_throwsBadRequestForInvalidOrigin() {
                assertThatThrownBy(() -> folderService.createFolder(
                                1, "My Folder", "COURSE", "NOT_A_REAL_ORIGIN", "BEGINNER", null, null))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Invalid value for PatternOrigin");
        }

        @Test
        void createFolder_throwsBadRequestForInvalidLevel() {
                assertThatThrownBy(() -> folderService.createFolder(
                                1, "My Folder", "COURSE", "DRAFTED", "NOT_A_REAL_LEVEL", null, null))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Invalid value for Level");
        }

        @Test
        void createFolder_isCaseInsensitiveForEnumValues() {
                Folder savedFolder = new Folder();
                savedFolder.setId(UUID.randomUUID());
                FolderDto dto = new FolderDto(
                                savedFolder.getId(), 1, "My Folder", null,
                                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

                when(folderRepo.save(any(Folder.class))).thenReturn(savedFolder);
                when(folderMapper.toDto(savedFolder)).thenReturn(dto);

                folderService.createFolder(
                                1, "My Folder", "course", "drafted", "beginner", null, null);

                verify(folderRepo).save(any(Folder.class));
        }

        @Test
        void createFolder_uploadsImageToS3AndSetsImagePath() {
                Folder savedFolderBeforeImage = new Folder();
                savedFolderBeforeImage.setId(UUID.randomUUID());
                Folder savedFolderAfterImage = new Folder();
                savedFolderAfterImage.setId(savedFolderBeforeImage.getId());

                MockMultipartFile image = new MockMultipartFile(
                                "image", "photo.png", "image/png", "fake-image-bytes".getBytes());

                FolderDto dto = new FolderDto(
                                savedFolderAfterImage.getId(), 1, "My Folder", "folders/x/image.png",
                                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

                when(folderRepo.save(any(Folder.class)))
                                .thenReturn(savedFolderBeforeImage)
                                .thenReturn(savedFolderAfterImage);
                when(folderMapper.toDto(savedFolderAfterImage)).thenReturn(dto);

                folderService.createFolder(
                                1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image, null);

                verify(storageService).upload(argThat(key -> key.contains("folders/") && key.endsWith(".png")), any(),
                                eq("image/png"));

                verify(folderRepo, times(2)).save(any(Folder.class));
        }

        @Test
        void createFolder_doesNotCallS3WhenImageIsEmpty() {
                Folder savedFolder = new Folder();
                savedFolder.setId(UUID.randomUUID());
                FolderDto dto = new FolderDto(
                                savedFolder.getId(), 1, "My Folder", null,
                                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

                MockMultipartFile emptyImage = new MockMultipartFile(
                                "image", "empty.png", "image/png", new byte[0]);

                when(folderRepo.save(any(Folder.class))).thenReturn(savedFolder);
                when(folderMapper.toDto(savedFolder)).thenReturn(dto);

                folderService.createFolder(
                                1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", emptyImage, null);

                verifyNoInteractions(storageService);
                verify(folderRepo, times(1)).save(any(Folder.class));
        }

        @Test
        void deleteFolder_deletesImageFromS3WhenImagePathExists() {
                UUID folderId = UUID.randomUUID();
                Folder folder = new Folder();
                folder.setImagePath("folders/" + folderId + "/image.png");

                when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));

                folderService.deleteFolder(folderId);

                verify(storageService).delete("folders/" + folderId + "/image.png");

                verify(folderRepo).delete(folder);
        }

        @Test
        void deleteFolder_throwsNotFoundWhenFolderDoesNotExist() {
                UUID folderId = UUID.randomUUID();
                when(folderRepo.findById(folderId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> folderService.deleteFolder(folderId))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Folder not found");

                verifyNoInteractions(storageService);
                verify(folderRepo, never()).delete(any());
        }
}