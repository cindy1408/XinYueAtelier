package com.xinyue.atelier.service;

import com.xinyue.atelier.model.User;
import com.xinyue.atelier.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepository;

    public User findOrCreateUser(String googleId, String email, String name) {
        // Try to find by googleId first (most reliable)
        return userRepository.findByGoogleId(googleId)
                .orElseGet(() -> {
                    // Check if they signed up with same email before
                    return userRepository.findByEmail(email)
                            .map(existing -> {
                                // Link their Google ID to the existing account
                                existing.setGoogleId(googleId);
                                existing.setLastLoginAt(LocalDateTime.now());
                                return userRepository.save(existing);
                            })
                            .orElseGet(() -> {
                                User newUser = new User();
                                newUser.setGoogleId(googleId);
                                newUser.setEmail(email);
                                newUser.setName(name);
                                newUser.setRole("ROLE_USER");
                                newUser.setCreatedAt(LocalDateTime.now());
                                newUser.setLastLoginAt(LocalDateTime.now());
                                return userRepository.save(newUser);
                            });
                });
    }

    public void updateLastLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }
}