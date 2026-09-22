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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

        @Mock
        private FolderMapper folderMapper;

        @Mock
        private FolderRepo folderRepo;

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

        // ---------- getFolderById ----------

        @Test
        void getFolderById_returnsDtoWhenFolderExists() {
                UUID folderId = UUID.randomUUID();
                Folder folder = new Folder();
                folder.setId(folderId);

                FolderDto dto = new FolderDto(
                        folderId, 1, "My Folder", null,
                        PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

                when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
                when(folderMapper.toDto(folder)).thenReturn(dto);

                Optional<FolderDto> result = folderService.getFolderById(folderId);

                assertThat(result).isPresent();
                assertThat(result.get()).isEqualTo(dto);
        }

        @Test
        void getFolderById_returnsEmptyOptionalWhenFolderDoesNotExist() {
                UUID folderId = UUID.randomUUID();
                when(folderRepo.findById(folderId)).thenReturn(Optional.empty());

                Optional<FolderDto> result = folderService.getFolderById(folderId);

                assertThat(result).isEmpty();
                verifyNoInteractions(folderMapper);
        }

        // ---------- getFolderChildrenById ----------

        @Test
        void getFolderChildrenById_returnsMappedChildren() {
                UUID parentId = UUID.randomUUID();
                Folder child1 = new Folder();
                Folder child2 = new Folder();

                FolderDto dto1 = new FolderDto(
                        UUID.randomUUID(), 1, "Child One", null,
                        PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());
                FolderDto dto2 = new FolderDto(
                        UUID.randomUUID(), 2, "Child Two", null,
                        PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

                when(folderRepo.findByParentFolderId(parentId)).thenReturn(List.of(child1, child2));
                when(folderMapper.toDto(child1)).thenReturn(dto1);
                when(folderMapper.toDto(child2)).thenReturn(dto2);

                List<FolderDto> result = folderService.getFolderChildrenById(parentId);

                assertThat(result).containsExactly(dto1, dto2);
        }

        @Test
        void getFolderChildrenById_returnsEmptyListWhenNoChildrenOrParentDoesNotExist() {
                UUID parentId = UUID.randomUUID();
                when(folderRepo.findByParentFolderId(parentId)).thenReturn(List.of());

                List<FolderDto> result = folderService.getFolderChildrenById(parentId);

                assertThat(result).isEmpty();
                verifyNoInteractions(folderMapper);
        }

        // ---------- createFolder: happy path ----------

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

                assertThatThrownBy(() -> folderService.createFolder(
                        1, "Title", "COURSE", "DRAFTED", "BEGINNER", null, parentId))
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("Parent folder not found");

                verify(folderRepo, never()).save(any());
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

                FolderDto result = folderService.createFolder(
                        1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image, null);

                assertThat(result).isEqualTo(dto);
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

        // ---------- createFolder: exceptions ----------

        @Test
        void createFolder_throwsInternalServerErrorWhenImageReadFails() throws IOException {
                Folder savedFolder = new Folder();
                savedFolder.setId(UUID.randomUUID());

                MultipartFile image = mock(MultipartFile.class);
                when(image.isEmpty()).thenReturn(false);
                when(image.getBytes()).thenThrow(new IOException("disk read error"));

                when(folderRepo.save(any(Folder.class))).thenReturn(savedFolder);

                assertThatThrownBy(() -> folderService.createFolder(
                        1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image, null))
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("Failed to create folder");

                verify(folderRepo, times(1)).save(any(Folder.class));
        }

        @Test
        void createFolder_wrapsRuntimeExceptionFromS3UploadAsInternalServerError() {
                Folder savedFolder = new Folder();
                savedFolder.setId(UUID.randomUUID());

                MockMultipartFile image = new MockMultipartFile(
                        "image", "photo.png", "image/png", "fake-image-bytes".getBytes());

                when(folderRepo.save(any(Folder.class))).thenReturn(savedFolder);
                doThrow(new RuntimeException("S3 unavailable"))
                        .when(storageService).upload(anyString(), any(), anyString());

                assertThatThrownBy(() -> folderService.createFolder(
                        1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image, null))
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("Failed to create folder");
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
        void createFolder_withInvalidEnum_neverCallsSave() {
                assertThatThrownBy(() -> folderService.createFolder(
                        1, "My Folder", "NOT_A_REAL_TYPE", "DRAFTED", "BEGINNER", null, null))
                        .isInstanceOf(ResponseStatusException.class);

                verify(folderRepo, never()).save(any());
                verifyNoInteractions(storageService);
        }

        // ---------- updateFolder: happy path ----------

        @Test
        void updateFolder_withValidData_updatesFolder() {
                UUID folderId = UUID.randomUUID();
                Folder folder = new Folder();
                folder.setId(folderId);

                FolderDto dto = new FolderDto(
                        folderId, 2, "Updated Folder", null,
                        PatternOrigin.ACQUIRED, Level.INTERMEDIATE, GarmentType.DRESS, Collections.emptyList());

                when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
                when(folderRepo.save(folder)).thenReturn(folder);
                when(folderMapper.toDto(folder)).thenReturn(dto);

                FolderDto result = folderService.updateFolder(
                        folderId, 2, "Updated Folder", "DRESS", "ACQUIRED", "INTERMEDIATE", null);

                assertThat(result).isEqualTo(dto);
                assertThat(folder.getFolderName()).isEqualTo("Updated Folder");
                assertThat(folder.getRef()).isEqualTo(2);
                assertThat(folder.getGarmentType()).isEqualTo(GarmentType.DRESS);
                assertThat(folder.getOrigin()).isEqualTo(PatternOrigin.ACQUIRED);
                assertThat(folder.getLevel()).isEqualTo(Level.INTERMEDIATE);
                verify(folderRepo).save(folder);
                verifyNoInteractions(storageService);
        }

        @Test
        void updateFolder_deletesOldImageAndUploadsNewOneWhenImageProvided() {
                UUID folderId = UUID.randomUUID();
                Folder folder = new Folder();
                folder.setId(folderId);
                folder.setImagePath("folders/" + folderId + "/old.png");

                MockMultipartFile image = new MockMultipartFile(
                        "image", "new.png", "image/png", "fake-image-bytes".getBytes());

                FolderDto dto = new FolderDto(
                        folderId, 1, "My Folder", "folders/" + folderId + "/new.png",
                        PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

                when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
                when(folderRepo.save(folder)).thenReturn(folder);
                when(folderMapper.toDto(folder)).thenReturn(dto);

                folderService.updateFolder(
                        folderId, 1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image);

                verify(storageService).delete("folders/" + folderId + "/old.png");
                verify(storageService).upload(argThat(key -> key.contains("folders/") && key.endsWith(".png")), any(),
                        eq("image/png"));
                verify(folderRepo).save(folder);
        }

        @Test
        void updateFolder_uploadsImageWithoutDeletingWhenNoExistingImage() {
                UUID folderId = UUID.randomUUID();
                Folder folder = new Folder();
                folder.setId(folderId);
                folder.setImagePath(null);

                MockMultipartFile image = new MockMultipartFile(
                        "image", "new.png", "image/png", "fake-image-bytes".getBytes());

                FolderDto dto = new FolderDto(
                        folderId, 1, "My Folder", "folders/" + folderId + "/new.png",
                        PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

                when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
                when(folderRepo.save(folder)).thenReturn(folder);
                when(folderMapper.toDto(folder)).thenReturn(dto);

                folderService.updateFolder(
                        folderId, 1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image);

                verify(storageService, never()).delete(any());
                verify(storageService).upload(anyString(), any(), eq("image/png"));
        }

        @Test
        void updateFolder_doesNotCallS3WhenImageIsEmpty() {
                UUID folderId = UUID.randomUUID();
                Folder folder = new Folder();
                folder.setId(folderId);
                folder.setImagePath("folders/" + folderId + "/existing.png");

                MockMultipartFile emptyImage = new MockMultipartFile(
                        "image", "empty.png", "image/png", new byte[0]);

                FolderDto dto = new FolderDto(
                        folderId, 1, "My Folder", "folders/" + folderId + "/existing.png",
                        PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

                when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
                when(folderRepo.save(folder)).thenReturn(folder);
                when(folderMapper.toDto(folder)).thenReturn(dto);

                folderService.updateFolder(
                        folderId, 1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", emptyImage);

                verifyNoInteractions(storageService);
                assertThat(folder.getImagePath()).isEqualTo("folders/" + folderId + "/existing.png");
        }

        // ---------- updateFolder: exceptions ----------

        @Test
        void updateFolder_throwsInternalServerErrorWhenImageReadFails() throws IOException {
                UUID folderId = UUID.randomUUID();
                Folder folder = new Folder();
                folder.setId(folderId);

                MultipartFile image = mock(MultipartFile.class);
                when(image.isEmpty()).thenReturn(false);
                when(image.getBytes()).thenThrow(new IOException("disk read error"));

                when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));

                assertThatThrownBy(() -> folderService.updateFolder(
                        folderId, 1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image))
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("Failed to update folder");

                verify(folderRepo, never()).save(any());
        }

        @Test
        void updateFolder_wrapsRuntimeExceptionFromS3DeleteAsInternalServerError() {
                UUID folderId = UUID.randomUUID();
                Folder folder = new Folder();
                folder.setId(folderId);
                folder.setImagePath("folders/" + folderId + "/old.png");

                MockMultipartFile image = new MockMultipartFile(
                        "image", "new.png", "image/png", "fake-image-bytes".getBytes());

                when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
                doThrow(new RuntimeException("S3 unavailable"))
                        .when(storageService).delete(anyString());

                assertThatThrownBy(() -> folderService.updateFolder(
                        folderId, 1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image))
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("Failed to update folder");

                verify(folderRepo, never()).save(any());
        }

        @Test
        void updateFolder_validatesEnumsBeforeTouchingS3() {
                UUID folderId = UUID.randomUUID();
                Folder folder = new Folder();
                folder.setId(folderId);
                folder.setImagePath("folders/" + folderId + "/old.png");

                MockMultipartFile image = new MockMultipartFile(
                        "image", "new.png", "image/png", "fake-image-bytes".getBytes());

                when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));

                assertThatThrownBy(() -> folderService.updateFolder(
                        folderId, 1, "My Folder", "NOT_A_REAL_TYPE", "DRAFTED", "BEGINNER", image))
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("Invalid value for GarmentType");

                verifyNoInteractions(storageService);
                verify(folderRepo, never()).save(any());
                assertThat(folder.getImagePath()).isEqualTo("folders/" + folderId + "/old.png");
        }

        @Test
        void updateFolder_throwsBadRequestForInvalidGarmentType() {
                UUID folderId = UUID.randomUUID();
                Folder folder = new Folder();
                folder.setId(folderId);

                when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));

                assertThatThrownBy(() -> folderService.updateFolder(
                        folderId, 1, "My Folder", "NOT_A_REAL_TYPE", "DRAFTED", "BEGINNER", null))
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("Invalid value for GarmentType");

                verify(folderRepo, never()).save(any());
        }

        @Test
        void updateFolder_throwsNotFoundWhenFolderDoesNotExist() {
                UUID folderId = UUID.randomUUID();
                when(folderRepo.findById(folderId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> folderService.updateFolder(
                        folderId, 1, "Title", "COURSE", "DRAFTED", "BEGINNER", null))
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("Folder not found");

                verify(folderRepo, never()).save(any());
        }

        @Test
        void updateFolder_throwsNotFoundBeforeTouchingS3WhenFolderDoesNotExist() {
                UUID folderId = UUID.randomUUID();
                MockMultipartFile image = new MockMultipartFile(
                        "image", "new.png", "image/png", "fake-image-bytes".getBytes());

                when(folderRepo.findById(folderId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> folderService.updateFolder(
                        folderId, 1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image))
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("Folder not found");

                verifyNoInteractions(storageService);
                verify(folderRepo, never()).save(any());
        }

        // ---------- deleteFolder: happy path ----------

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
        void deleteFolder_doesNotCallS3WhenImagePathIsNull() {
                UUID folderId = UUID.randomUUID();
                Folder folder = new Folder();
                folder.setImagePath(null);

                when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));

                folderService.deleteFolder(folderId);

                verifyNoInteractions(storageService);
                verify(folderRepo).delete(folder);
        }

        // ---------- deleteFolder: exceptions ----------

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

        @Test
        void deleteFolder_wrapsRuntimeExceptionFromS3DeleteAsInternalServerError() {
                UUID folderId = UUID.randomUUID();
                Folder folder = new Folder();
                folder.setImagePath("folders/" + folderId + "/image.png");

                when(folderRepo.findById(folderId)).thenReturn(Optional.of(folder));
                doThrow(new RuntimeException("S3 unavailable"))
                        .when(storageService).delete(anyString());

                assertThatThrownBy(() -> folderService.deleteFolder(folderId))
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("Failed to delete folder");

                verify(folderRepo, never()).delete(any());
        }
}