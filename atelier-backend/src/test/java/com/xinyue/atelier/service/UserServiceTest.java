package com.xinyue.atelier.service;

import com.xinyue.atelier.model.User;
import com.xinyue.atelier.repository.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepository;

    @Mock
    private OAuth2User oauthUser;

    @InjectMocks
    private UserService userService;

    // ---------- getCurrentUser ----------

    @Test
    void getCurrentUser_returnsUserWhenEmailMatches() {
        User user = new User();
        user.setEmail("cindy@example.com");

        when(oauthUser.getAttribute("email")).thenReturn("cindy@example.com");
        when(userRepository.findByEmail("cindy@example.com")).thenReturn(Optional.of(user));

        User result = userService.getCurrentUser(oauthUser);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void getCurrentUser_throwsWhenUserNotFound() {
        when(oauthUser.getAttribute("email")).thenReturn("nobody@example.com");
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(oauthUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    // ---------- findOrCreateUser ----------

    @Test
    void findOrCreateUser_returnsExistingUserWhenGoogleIdMatches() {
        User existing = new User();
        existing.setGoogleId("google-123");
        existing.setEmail("cindy@example.com");

        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));

        User result = userService.findOrCreateUser("google-123", "cindy@example.com", "Cindy");

        assertThat(result).isEqualTo(existing);
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateUser_linksGoogleIdWhenExistingEmailFoundWithoutGoogleId() {
        User existing = new User();
        existing.setEmail("cindy@example.com");
        existing.setGoogleId(null);

        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("cindy@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = userService.findOrCreateUser("google-123", "cindy@example.com", "Cindy");

        assertThat(result).isEqualTo(existing);
        assertThat(existing.getGoogleId()).isEqualTo("google-123");
        assertThat(existing.getLastLoginAt()).isNotNull();
        verify(userRepository).save(existing);
    }

    @Test
    void findOrCreateUser_doesNotOverwriteEmailOrNameWhenLinkingExistingAccount() {
        User existing = new User();
        existing.setEmail("cindy@example.com");
        existing.setName("Original Name");
        existing.setGoogleId(null);

        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("cindy@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        userService.findOrCreateUser("google-123", "cindy@example.com", "New Name From Google");

        assertThat(existing.getName()).isEqualTo("Original Name");
    }

    @Test
    void findOrCreateUser_createsNewUserWhenNeitherGoogleIdNorEmailFound() {
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("cindy@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.findOrCreateUser("google-123", "cindy@example.com", "Cindy");

        assertThat(result.getGoogleId()).isEqualTo("google-123");
        assertThat(result.getEmail()).isEqualTo("cindy@example.com");
        assertThat(result.getName()).isEqualTo("Cindy");
        assertThat(result.getRole()).isEqualTo("ROLE_USER");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getLastLoginAt()).isNotNull();
    }

    @Test
    void findOrCreateUser_newUserHasCreatedAtEqualToLastLoginAt() {
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("cindy@example.com")).thenReturn(Optional.empty());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.findOrCreateUser("google-123", "cindy@example.com", "Cindy");

        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getCreatedAt()).isEqualToIgnoringNanos(saved.getLastLoginAt());
    }

    // ---------- updateLastLogin ----------

    @Test
    void updateLastLogin_updatesTimestampAndSavesWhenUserExists() {
        User user = new User();
        user.setEmail("cindy@example.com");
        LocalDateTime before = user.getLastLoginAt();

        when(userRepository.findByEmail("cindy@example.com")).thenReturn(Optional.of(user));

        userService.updateLastLogin("cindy@example.com");

        assertThat(user.getLastLoginAt()).isNotEqualTo(before);
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void updateLastLogin_doesNothingWhenUserDoesNotExist() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        userService.updateLastLogin("nobody@example.com");

        verify(userRepository, never()).save(any());
    }
}