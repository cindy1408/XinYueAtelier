package com.xinyue.atelier.controller;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.model.User;
import com.xinyue.atelier.service.FolderService;
import com.xinyue.atelier.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    private FolderController folderController;

    @Mock
    private FolderService folderService;

    @Mock
    private UserService userService;

    @Mock
    private OAuth2User oauthUser;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService);
        folderController = new FolderController(folderService);
    }

    @Test
    void getMe_returnsOkWithUserFieldsInBody() {
        User user = new User();
        user.setId(1L);
        user.setEmail("cindy@example.com");
        user.setName("Cindy");
        user.setRole("ROLE_USER");
        LocalDateTime createdAt = LocalDateTime.now();
        user.setCreatedAt(createdAt);

        when(userService.getCurrentUser(oauthUser)).thenReturn(user);

        ResponseEntity<?> response = userController.getMe(oauthUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("id", 1L);
        assertThat(body).containsEntry("email", "cindy@example.com");
        assertThat(body).containsEntry("name", "Cindy");
        assertThat(body).containsEntry("role", "ROLE_USER");
        assertThat(body).containsEntry("createdAt", createdAt);
    }

    @Test
    void getMe_propagatesRuntimeExceptionWhenUserNotFound() {
        when(userService.getCurrentUser(oauthUser))
                .thenThrow(new RuntimeException("User not found"));

        assertThatThrownBy(() -> userController.getMe(oauthUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    // ---------- listFolders ----------

    @Test
    void listFolders_returnsAllRootFolders() {
        FolderDto folder1 = new FolderDto(
                UUID.randomUUID(), 1, "Folder One", null,
                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());
        FolderDto folder2 = new FolderDto(
                UUID.randomUUID(), 2, "Folder Two", null,
                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

        when(folderService.listRootFolders()).thenReturn(List.of(folder1, folder2));

        List<FolderDto> result = folderController.listFolders();

        assertThat(result).containsExactly(folder1, folder2);
        verify(folderService).listRootFolders();
    }

    @Test
    void listFolders_returnsEmptyListWhenNoRootFolders() {
        when(folderService.listRootFolders()).thenReturn(List.of());

        List<FolderDto> result = folderController.listFolders();

        assertThat(result).isEmpty();
    }

// ---------- createRootFolder ----------

    @Test
    void createRootFolder_returnsOkWithCreatedFolder() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.png", "image/png", "fake-image-bytes".getBytes());

        FolderDto dto = new FolderDto(
                UUID.randomUUID(), 1, "My Folder", "folders/x/image.png",
                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());

        when(folderService.createFolder(1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image, null))
                .thenReturn(dto);

        ResponseEntity<FolderDto> response = folderController.createRootFolder(
                1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void createRootFolder_passesNullParentId() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.png", "image/png", "fake-image-bytes".getBytes());

        when(folderService.createFolder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mock(FolderDto.class));

        folderController.createRootFolder(1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", image);

        verify(folderService).createFolder(
                eq(1), eq("My Folder"), eq("COURSE"), eq("DRAFTED"), eq("BEGINNER"), eq(image), isNull());
    }

    @Test
    void createRootFolder_propagatesBadRequestForInvalidEnum() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.png", "image/png", "fake-image-bytes".getBytes());

        when(folderService.createFolder(any(), any(), eq("NOT_A_REAL_TYPE"), any(), any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid value for GarmentType"));

        assertThatThrownBy(() -> folderController.createRootFolder(
                1, "My Folder", "NOT_A_REAL_TYPE", "DRAFTED", "BEGINNER", image))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid value for GarmentType");
    }
}