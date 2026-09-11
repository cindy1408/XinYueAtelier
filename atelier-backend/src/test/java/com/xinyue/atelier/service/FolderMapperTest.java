package com.xinyue.atelier.service;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.model.Folder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderMapperTest {

    @Mock
    private S3StorageService storageService;

    private FolderMapper folderMapper;

    @BeforeEach
    void setUp() {
        folderMapper = new FolderMapper(storageService);
    }

    @Test
    void toDto_mapsBasicFieldsCorrectly() {
        Folder folder = newFolder("My Folder", null);

        FolderDto dto = folderMapper.toDto(folder);

        assertThat(dto.id()).isEqualTo(folder.getId());
        assertThat(dto.folderName()).isEqualTo("My Folder");
        assertThat(dto.origin()).isEqualTo(PatternOrigin.DRAFTED);
        assertThat(dto.level()).isEqualTo(Level.BEGINNER);
        assertThat(dto.garmentType()).isEqualTo(GarmentType.COURSE);
        verifyNoInteractions(storageService);
    }

    @Test
    void toDto_returnsNullImagePathWhenNoImagePath() {
        Folder folder = newFolder("No Image Folder", null);
        folder.setImagePath(null);

        FolderDto dto = folderMapper.toDto(folder);

        assertThat(dto.imagePath()).isNull();
        verifyNoInteractions(storageService);
    }

    @Test
    void toDto_generatesPresignedUrlWhenImagePathExists() {
        Folder folder = newFolder("Image Folder", null);
        folder.setImagePath("folders/123/image.png");

        String presignedUrl = "https://test-bucket.s3.amazonaws.com/folders/123/image.png?signed=true";
        when(storageService.presign("folders/123/image.png")).thenReturn(presignedUrl);

        FolderDto dto = folderMapper.toDto(folder);

        assertThat(dto.imagePath()).isEqualTo(presignedUrl);
    }

    @Test
    void toDto_usesCorrectKeyForPresignedUrl() {
        Folder folder = newFolder("Image Folder", null);
        folder.setImagePath("folders/123/image.png");

        when(storageService.presign("folders/123/image.png")).thenReturn("https://example.com/signed");

        folderMapper.toDto(folder);

        verify(storageService).presign("folders/123/image.png");
    }

    @Test
    void toDto_mapsEmptySubFoldersToEmptyList() {
        Folder folder = newFolder("Parent", null);
        folder.setSubFolders(List.of());

        FolderDto dto = folderMapper.toDto(folder);

        assertThat(dto.children()).isEmpty();
    }

    @Test
    void toDto_recursivelyMapsSubFolders() {
        Folder parent = newFolder("Parent", null);
        Folder child1 = newFolder("Child One", parent);
        Folder child2 = newFolder("Child Two", parent);
        parent.setSubFolders(List.of(child1, child2));

        FolderDto dto = folderMapper.toDto(parent);

        assertThat(dto.children()).hasSize(2);
        assertThat(dto.children())
                .extracting(FolderDto::folderName)
                .containsExactlyInAnyOrder("Child One", "Child Two");
    }

    @Test
    void toDto_recursivelyMapsNestedSubFolderImages() {
        Folder parent = newFolder("Parent", null);
        Folder child = newFolder("Child", parent);
        child.setImagePath("folders/child/image.png");
        parent.setSubFolders(List.of(child));

        String presignedUrl = "https://test-bucket.s3.amazonaws.com/folders/child/image.png?signed=true";
        when(storageService.presign("folders/child/image.png")).thenReturn(presignedUrl);

        FolderDto dto = folderMapper.toDto(parent);

        assertThat(dto.children().get(0).imagePath()).isEqualTo(presignedUrl);
    }

    private Folder newFolder(String name, Folder parent) {
        Folder folder = new Folder();
        folder.setId(UUID.randomUUID());
        folder.setRef(1);
        folder.setFolderName(name);
        folder.setOrigin(PatternOrigin.DRAFTED);
        folder.setLevel(Level.BEGINNER);
        folder.setGarmentType(GarmentType.COURSE);
        folder.setParentFolder(parent);
        folder.setSubFolders(List.of());
        return folder;
    }
}