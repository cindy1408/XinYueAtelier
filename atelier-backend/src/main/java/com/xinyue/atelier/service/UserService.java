package com.xinyue.atelier.service;

import com.xinyue.atelier.model.User;
import com.xinyue.atelier.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final Clock clock;
    private final UserRepo userRepository;

    public UserService(Clock clock, UserRepo userRepository) {
        this.clock = clock;
        this.userRepository = userRepository;
    }

    public User getCurrentUser(OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public User findOrCreateUser(String googleId, String email, String name) {
        LocalDateTime now = LocalDateTime.now(clock);

        Optional<User> byGoogleId = userRepository.findByGoogleId(googleId);
        if (byGoogleId.isPresent()) {
            return byGoogleId.get();
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.setGoogleId(googleId);
            existing.setLastLoginAt(now);
            return userRepository.save(existing);
        }

        User newUser = new User();
        newUser.setGoogleId(googleId);
        newUser.setEmail(email);
        newUser.setName(name);
        newUser.setRole("ROLE_USER");
        newUser.setCreatedAt(now);
        newUser.setLastLoginAt(now);
        return userRepository.save(newUser);
    }

    @Transactional
    public void updateLastLogin(String email) {
        userRepository.findByEmail(email)
                .ifPresent(user -> user.setLastLoginAt(LocalDateTime.now(clock)));
        // dirty checking flushes on commit; no explicit save needed
    }
}