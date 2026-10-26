package com.xinyue.atelier.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.dto.FolderDto;
import com.xinyue.atelier.dto.UserDto;
import com.xinyue.atelier.model.User;
import com.xinyue.atelier.service.FolderService;
import com.xinyue.atelier.service.UserService;

class UserInterviewTests {

    private static final User USER = new User();

    static {
        USER.setId(1L);
        USER.setEmail("cindy@example.com");
        USER.setName("Cindy");
        USER.setRole("ROLE_USER");
        USER.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
    }

    private static final MockMultipartFile IMAGE =
            new MockMultipartFile("image", "photo.png", "image/png", "fake-image-bytes".getBytes());

    private final UserService userService = mock(UserService.class);
    private final FolderService folderService = mock(FolderService.class);
    private final OAuth2User oauthUser = mock(OAuth2User.class);
    private final UserController userController = new UserController(userService);
    private final FolderController folderController = new FolderController(folderService);

    private static FolderDto folder(int ref, String title) {
        return new FolderDto(UUID.randomUUID(), ref, title, null,
                PatternOrigin.DRAFTED, Level.BEGINNER, GarmentType.COURSE, Collections.emptyList());
    }

    @Test
    void getMeReturnsOkWithUserFieldsInBody() {
        when(userService.getCurrentUser(oauthUser)).thenReturn(USER);

        ResponseEntity<UserDto> response = userController.getMe(oauthUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new UserDto(
                USER.getId(),
                USER.getEmail(),
                USER.getName(),
                USER.getRole(),
                USER.getCreatedAt()));
    }

    @Test
    void getMeReportsAdminRoleForAdmins() {
        USER.setRole("ROLE_ADMIN");
        when(userService.getCurrentUser(oauthUser)).thenReturn(USER);

        UserDto body = userController.getMe(oauthUser).getBody();

        assertThat(body).isNotNull();
        assertThat(body.role()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void getMePropagatesExceptionWhenUserNotFound() {
        when(userService.getCurrentUser(oauthUser)).thenThrow(new RuntimeException("User not found"));

        try {
            userController.getMe(oauthUser);
        } catch (RuntimeException e) {
            assertThat(e).hasMessage("User not found");
        }
    }

    @Test
    void listFoldersReturnsRootFoldersOrEmptyList() {
        FolderDto one = folder(1, "Folder One");
        FolderDto two = folder(2, "Folder Two");
        when(folderService.listRootFolders()).thenReturn(List.of(one, two));
        assertThat(folderController.listFolders()).containsExactly(one, two);

        when(folderService.listRootFolders()).thenReturn(List.of());
        assertThat(folderController.listFolders()).isEmpty();
    }

    @Test
    void createRootFolderReturnsOkAndPassesNullParentId() {
        FolderDto dto = folder(1, "My Folder");
        when(folderService.createFolder(1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", IMAGE, null))
                .thenReturn(dto);

        ResponseEntity<FolderDto> response =
                folderController.createRootFolder(1, "My Folder", "COURSE", "DRAFTED", "BEGINNER", IMAGE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
        verify(folderService).createFolder(
                eq(1), eq("My Folder"), eq("COURSE"), eq("DRAFTED"), eq("BEGINNER"), eq(IMAGE), isNull());
    }

    @Test
    void createRootFolderPropagatesBadRequestForInvalidEnum() {
        when(folderService.createFolder(any(), any(), eq("NOT_A_REAL_TYPE"), any(), any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid value for GarmentType"));

        assertThatThrownBy(() -> folderController.createRootFolder(
                1, "My Folder", "NOT_A_REAL_TYPE", "DRAFTED", "BEGINNER", IMAGE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid value for GarmentType");
    }
}