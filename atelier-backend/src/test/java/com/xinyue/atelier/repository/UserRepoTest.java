package com.xinyue.atelier.repository;

import com.xinyue.atelier.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepoTest {

    @Autowired
    private UserRepo userRepo;

    // ---------- findByEmail ----------

    @Test
    void findByEmail_returnsUserWhenEmailExists() {
        User user = newUser("cindy@example.com", "google-123");
        userRepo.save(user);

        Optional<User> result = userRepo.findByEmail("cindy@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("cindy@example.com");
    }

    @Test
    void findByEmail_returnsEmptyWhenEmailDoesNotExist() {
        Optional<User> result = userRepo.findByEmail("nobody@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_isCaseSensitive() {
        User user = newUser("Cindy@Example.com", "google-123");
        userRepo.save(user);

        Optional<User> result = userRepo.findByEmail("cindy@example.com");

        assertThat(result).isEmpty();
    }

    // ---------- findByGoogleId ----------

    @Test
    void findByGoogleId_returnsUserWhenGoogleIdExists() {
        User user = newUser("cindy@example.com", "google-123");
        userRepo.save(user);

        Optional<User> result = userRepo.findByGoogleId("google-123");

        assertThat(result).isPresent();
        assertThat(result.get().getGoogleId()).isEqualTo("google-123");
    }

    @Test
    void findByGoogleId_returnsEmptyWhenGoogleIdDoesNotExist() {
        Optional<User> result = userRepo.findByGoogleId("nonexistent-id");

        assertThat(result).isEmpty();
    }

    @Test
    void findByGoogleId_returnsEmptyWhenGoogleIdIsNull() {
        User user = newUser("cindy@example.com", null);
        userRepo.save(user);

        Optional<User> result = userRepo.findByGoogleId("google-123");

        assertThat(result).isEmpty();
    }

    private User newUser(String email, String googleId) {
        User user = new User();
        user.setEmail(email);
        user.setGoogleId(googleId);
        return user;
    }
}